package com.marayti.unblock

import android.content.Context
import android.graphics.BitmapFactory
import com.googlecode.tesseract.android.TessBaseAPI
import java.io.File
import java.util.Locale

data class VerificationResult(
    val accepted: Boolean,
    val message: String,
    val recognizedText: String = "",
    val confidence: Int = 0,
    val date: String = "",
    val phone: String = "",
    val restrictionType: String = "",
    val restrictionReason: String = ""
)

/** Strict, offline screenshot verification. Fails closed if OCR is unavailable. */
class LocalScreenshotVerifier(private val context: Context) {
    fun verify(bytes: ByteArray): VerificationResult {
        if (bytes.size < 300) return reject("ملف الصورة صغير أو غير صالح")
        if (bytes.size > 5 * 1024 * 1024) return reject("حجم الصورة أكبر من 5 ميجابايت")

        val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options)
        if (options.outWidth < 600 || options.outHeight < 600) return reject("دقة الصورة أقل من الحد المطلوب")
        if (options.outWidth > 8_000 || options.outHeight > 8_000 ||
            options.outWidth.toLong() * options.outHeight.toLong() > 40_000_000L
        ) return reject("أبعاد الصورة كبيرة جدًا للفحص الآمن")
        val ratio = options.outWidth.toFloat() / options.outHeight.toFloat()
        if (ratio < 0.3f || ratio > 3.5f) return reject("أبعاد الصورة غير مناسبة")

        val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
            ?: return reject("تعذر فتح ملف الصورة")
        val tess = TessBaseAPI()
        return try {
            val dataRoot = File(context.filesDir, "tesseract")
            val tessdata = File(dataRoot, "tessdata")
            if (!tessdata.exists()) tessdata.mkdirs()
            listOf("eng", "ara").forEach { language ->
                val target = File(tessdata, "$language.traineddata")
                if (!target.exists() || target.length() < 100_000L) {
                    context.assets.open("tessdata/$language.traineddata").use { input ->
                        target.outputStream().use { output -> input.copyTo(output) }
                    }
                }
            }
            if (!tess.init(dataRoot.absolutePath, "eng+ara")) {
                return reject("محرك قراءة الصور غير جاهز على هذا الجهاز")
            }
            tess.setImage(bitmap)
            val text = tess.getUTF8Text().orEmpty()
            val confidence = tess.meanConfidence()
            evaluate(text, confidence)
        } catch (_: Exception) {
            reject("تعذر تشغيل فحص الصور محليًا. أعد المحاولة بصورة أوضح.")
        } finally {
            try { tess.end() } catch (_: Exception) { }
            bitmap.recycle()
        }
    }

    private fun evaluate(raw: String, confidence: Int): VerificationResult {
        val text = normalize(raw)
        val tokens = Regex("[\\p{L}\\p{N}]+").findAll(text).map { it.value }.toList()
        val arabic = Regex("[\\u0600-\\u06FF]").containsMatchIn(text)
        val signals = if (arabic) {
            Triple(
                listOf("واتساب"),
                listOf("حظر", "محظور", "تم حظر", "حظر مؤقت", "حظر دائم", "تعليق", "معلق", "ايقاف", "تقييد", "مقيد", "غير مسموح", "ممنوع", "موقوف", "حسابك محظور", "لا يسمح"),
                listOf("شروط الخدمه", "الشروط", "سياسه", "سياسات", "مخالفه", "انتهاك", "مراجعه", "طلب مراجعه", "مركز المساعده", "تعرف على المزيد", "الدعم", "نشاط غير معتاد", "ارشادات", "اساءه")
            )
        } else {
            Triple(
                listOf("whatsapp", "whats app"),
                listOf("temporarily banned", "permanently banned", "banned", "suspended", "suspension", "this account is not allowed", "no longer allowed", "not allowed to use", "cannot use whatsapp", "temporarily blocked", "deactivated", "terminated", "disabled", "restricted", "restriction", "account locked", "your phone number is banned", "account banned"),
                listOf("terms of service", "terms and conditions", "terms", "policy", "policies", "violation", "violated", "violating", "violate", "request a review", "review", "appeal", "help center", "learn more", "spam", "automated", "automation", "bulk", "illegal", "unsolicited", "guidelines", "account activity", "suspicious activity", "suspicious", "community standards", "contact us", "support", "misuse", "abuse")
            )
        }
        val foreign = if (arabic) listOf("تيليجرام", "تليجرام", "فيسبوك", "انستجرام", "تويتر", "سناب", "تيك توك", "ديسكورد", "سكايب", "فايبر")
            else listOf("telegram", "discord", "viber", "tiktok", "snapchat", "wechat", "twitter", "linkedin", "skype", "kik")
        val failures = mutableListOf<String>()
        if (tokens.size < 8 || tokens.sumOf { it.length } < 50 || confidence < 50) failures += "وضوح النص"
        if (tokens.distinct().size < 5) failures += "كلمات غير كافية"
        if (signals.first.none { containsSignal(text, tokens, it) }) failures += "اسم واتساب غير ظاهر"
        if (signals.second.none { containsSignal(text, tokens, it) }) failures += "لا يوجد نص يدل على الحظر"
        if (signals.third.none { containsSignal(text, tokens, it) }) failures += "لا يوجد سبب أو إشارة إلى سياسة المراجعة"
        if (foreign.any { text.contains(it) }) failures += "الصورة تشير إلى منصة أخرى"

        return if (failures.isEmpty()) {
            VerificationResult(
                accepted = true,
                message = "تم التحقق: إشعار حظر واتساب يبدو صحيحًا ✓",
                recognizedText = raw,
                confidence = confidence,
                date = extractDate(text),
                phone = extractPhone(text),
                restrictionType = if (listOf("permanent", "دائم").any { text.contains(it) }) "permanent" else if (listOf("temporary", "مؤقت").any { text.contains(it) }) "temporary" else "standard",
                restrictionReason = extractReason(text, arabic)
            )
        } else {
            VerificationResult(false, "لم يتم قبول الصورة: " + failures.joinToString("، "), raw, confidence)
        }
    }

    private fun containsSignal(text: String, tokens: List<String>, phrase: String): Boolean {
        if (!phrase.contains(' ')) return phrase in tokens || (phrase.length >= 5 && text.replace(" ", "").contains(phrase))
        return text.contains(phrase) || text.replace(" ", "").contains(phrase.replace(" ", ""))
    }

    private fun normalize(value: String): String {
        val map = mapOf('أ' to 'ا', 'إ' to 'ا', 'آ' to 'ا', 'ٱ' to 'ا', 'ى' to 'ي', 'ئ' to 'ي', 'ؤ' to 'و', 'ة' to 'ه', 'ۀ' to 'ه')
        return value.lowercase(Locale.ROOT)
            .replace(Regex("[\\u0610-\\u061A\\u064B-\\u065F\\u0670\\u06D6-\\u06EDـ]"), "")
            .map { map[it] ?: it }.joinToString("")
            .replace(Regex("\\s+"), " ").trim()
    }


    private fun extractDate(text: String): String {
        val patterns = listOf(
            Regex("\\b\\d{4}-\\d{1,2}-\\d{1,2}\\b"),
            Regex("\\b\\d{1,2}[/.-]\\d{1,2}[/.-]\\d{2,4}\\b")
        )
        return patterns.firstNotNullOfOrNull { it.find(text)?.value }.orEmpty()
    }

    private fun extractPhone(text: String): String {
        val pattern = Regex("(?<![\\d])\\+?\\(?\\d[\\d\\s().-]{7,18}\\d(?![\\d])")
        return pattern.findAll(text).map { it.value }.firstOrNull { candidate ->
            candidate.count(Char::isDigit) >= 9 && !Regex("\\d{1,4}[/.-]\\d{1,2}[/.-]\\d{2,4}").containsMatchIn(candidate)
        }.orEmpty()
    }

    private fun extractReason(text: String, arabic: Boolean): String {
        val candidates = if (arabic) listOf("شروط الخدمه", "سياسه", "مخالفه", "انتهاك", "نشاط غير معتاد", "بلاغات")
            else listOf("terms of service", "policy", "violation", "suspicious activity", "spam", "reports")
        return candidates.firstOrNull { text.contains(it) }.orEmpty()
    }

    private fun reject(message: String) = VerificationResult(false, message)
}
