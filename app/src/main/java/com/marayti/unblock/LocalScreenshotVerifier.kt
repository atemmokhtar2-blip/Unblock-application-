package com.marayti.unblock

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import com.googlecode.tesseract.android.TessBaseAPI
import java.io.File
import java.io.IOException
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

/** Offline screenshot verification. Language data is copied and checked before OCR starts. */
class LocalScreenshotVerifier(private val context: Context) {
    fun verify(bytes: ByteArray): VerificationResult {
        if (bytes.size < 300) return reject("ملف الصورة صغير أو غير صالح")
        if (bytes.size > 5 * 1024 * 1024) return reject("حجم الصورة أكبر من 5 ميجابايت")

        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
        if (bounds.outWidth < 600 || bounds.outHeight < 600) return reject("دقة الصورة أقل من الحد المطلوب")
        if (bounds.outWidth > 8_000 || bounds.outHeight > 8_000 ||
            bounds.outWidth.toLong() * bounds.outHeight.toLong() > 24_000_000L
        ) return reject("أبعاد الصورة كبيرة جدًا للفحص الآمن")
        val ratio = bounds.outWidth.toFloat() / bounds.outHeight.toFloat()
        if (ratio < 0.3f || ratio > 3.5f) return reject("أبعاد الصورة غير مناسبة")

        val decodeOptions = BitmapFactory.Options()
        var sampleSize = 1
        while (bounds.outWidth / sampleSize > 3_000 || bounds.outHeight / sampleSize > 3_000) {
            sampleSize *= 2
        }
        decodeOptions.inSampleSize = sampleSize
        val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size, decodeOptions)
            ?: return reject("تعذر فتح ملف الصورة")

        return try {
            if (!hasImageContent(bitmap)) return reject("الصورة فارغة أو غير واضحة")
            val dataRoot = File(context.filesDir, "tesseract")
            val tessdata = File(dataRoot, "tessdata")
            if (!tessdata.exists() && !tessdata.mkdirs()) {
                throw IOException("تعذر إنشاء مجلد بيانات محرك القراءة")
            }
            ensureLanguageData(tessdata, "eng")
            ensureLanguageData(tessdata, "ara")

            val tess = TessBaseAPI()
            try {
                val initialized = tess.init(dataRoot.absolutePath + File.separator, "eng+ara", TessBaseAPI.OEM_LSTM_ONLY)
                if (!initialized) {
                    return reject("لم يتمكن محرك القراءة من تحميل اللغتين العربية والإنجليزية. أعد تثبيت التطبيق ثم حاول مجددًا.")
                }
                tess.setPageSegMode(TessBaseAPI.PageSegMode.PSM_AUTO)
                tess.setImage(bitmap)
                val recognized = tess.getUTF8Text().orEmpty()
                if (recognized.isBlank()) return reject("الصورة واضحة لكن لم يتم استخراج نص منها. جرّب لقطة شاشة كاملة.")
                evaluate(recognized, tess.meanConfidence())
            } finally {
                try { tess.recycle() } catch (_: Exception) { }
            }
        } catch (exception: Exception) {
            reject(
                "تعذر تشغيل محرك فحص الصور محليًا (${exception.javaClass.simpleName}). " +
                    "تأكد من تثبيت النسخة كاملة ثم أعد المحاولة."
            )
        } finally {
            bitmap.recycle()
        }
    }

    private fun ensureLanguageData(tessdata: File, language: String) {
        val target = File(tessdata, "$language.traineddata")
        if (target.exists() && target.length() >= 100_000L) return
        val temporary = File(tessdata, "$language.traineddata.part")
        try {
            context.assets.open("tessdata/$language.traineddata").use { input ->
                temporary.outputStream().use { output -> input.copyTo(output) }
            }
            if (temporary.length() < 100_000L) {
                throw IOException("ملف اللغة $language غير مكتمل داخل التطبيق")
            }
            if (target.exists() && !target.delete()) throw IOException("تعذر استبدال ملف اللغة $language")
            if (!temporary.renameTo(target)) throw IOException("تعذر تجهيز ملف اللغة $language")
        } finally {
            if (temporary.exists()) temporary.delete()
        }
    }

    internal fun verifyRecognizedTextForTest(raw: String, confidence: Int = 90): VerificationResult = evaluate(raw, confidence)

    private fun evaluate(raw: String, confidence: Int): VerificationResult {
        val text = normalize(raw)
        val tokens = Regex("[\\p{L}\\p{N}]+").findAll(text).map { it.value }.toList()
        val arabic = Regex("[\\u0600-\\u06FF]").containsMatchIn(text)
        val signals = if (arabic) {
            Triple(
                listOf("واتساب"),
                listOf("حظر", "محظور", "تم حظر", "حظر مؤقت", "حظر دائم", "تعليق", "معلق", "ايقاف", "تقييد", "مقيد", "غير مسموح", "ممنوع", "موقوف", "حسابك محظور", "لا يسمح", "لا يمكن لهذا الحساب استخدام واتساب", "لا يمكن استخدام واتساب", "هذا الحساب لا يمكنه استخدام واتساب", "لا يستطيع هذا الحساب استخدام واتساب"),
                listOf("شروط الخدمه", "يخالف شروط الخدمه", "مخالف لشروط الخدمه", "الشروط", "سياسه", "سياسات", "مخالفه", "انتهاك", "مراجعه", "طلب مراجعه", "مركز المساعده", "تعرف على المزيد", "الدعم", "نشاط غير معتاد", "ارشادات", "اساءه")
            )
        } else {
            Triple(
                listOf("whatsapp", "whats app"),
                listOf("temporarily banned", "permanently banned", "banned", "bans", "banning", "ban", "suspended", "suspension", "this account is not allowed", "no longer allowed", "not allowed to use", "cannot use whatsapp", "account has been banned", "account is banned", "account was banned", "account has been disabled", "account has been suspended", "temporarily blocked", "deactivated", "terminated", "disabled", "restricted", "restriction", "account locked", "your phone number is banned", "account banned"),
                listOf("terms of service", "terms and conditions", "terms", "term", "policy", "policies", "violation", "violated", "violating", "violate", "request a review", "review", "appeal", "help center", "learn more", "spam", "automated", "automation", "bulk", "illegal", "unsolicited", "guidelines", "account activity", "suspicious activity", "suspicious", "community standards", "contact us", "support", "misuse", "abuse")
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
                restrictionType = if (listOf("permanent", "permanently", "دائم", "نهائي").any { text.contains(it) }) "permanent" else if (listOf("temporary", "temporarily", "مؤقت").any { text.contains(it) }) "temporary" else "",
                restrictionReason = extractReason(text, arabic)
            )
        } else {
            VerificationResult(false, "لم يتم قبول الصورة: " + failures.joinToString("، "), raw, confidence)
        }
    }

    private fun hasImageContent(bitmap: Bitmap): Boolean {
        val stepX = (bitmap.width / 64).coerceAtLeast(1)
        val stepY = (bitmap.height / 64).coerceAtLeast(1)
        var count = 0
        var sum = 0.0
        var sumSquares = 0.0
        var y = 0
        while (y < bitmap.height) {
            var x = 0
            while (x < bitmap.width) {
                val color = bitmap.getPixel(x, y)
                val gray = (android.graphics.Color.red(color) * 0.299 +
                    android.graphics.Color.green(color) * 0.587 +
                    android.graphics.Color.blue(color) * 0.114)
                sum += gray
                sumSquares += gray * gray
                count++
                x += stepX
            }
            y += stepY
        }
        if (count == 0) return false
        val mean = sum / count
        val variance = (sumSquares / count) - mean * mean
        return kotlin.math.sqrt(variance.coerceAtLeast(0.0)) >= 10.0
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
            Regex("\\b\\d{1,2}[/.-]\\d{1,2}[/.-]\\d{2,4}\\b"),
            Regex("\\b(?:jan|feb|mar|apr|may|jun|jul|aug|sep|oct|nov|dec)[a-z]*\\.?\\s+\\d{1,2},?\\s+\\d{4}\\b", RegexOption.IGNORE_CASE)
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
        val mapping = if (arabic) listOf(
            "رسائل جماعيه" to "bulk/automated", "مزعج" to "spam", "ازعاج" to "spam",
            "انتحال" to "impersonation", "احتيال" to "fraud", "غير قانوني" to "illegal",
            "مخالفه" to "terms violation"
        ) else listOf(
            "spam" to "spam", "bulk" to "bulk/automated", "automated" to "bulk/automated",
            "automation" to "bulk/automated", "unsolicited" to "bulk/automated",
            "illegal" to "illegal", "unauthorized" to "illegal", "impersonat" to "impersonation",
            "fraud" to "fraud", "abuse" to "abuse"
        )
        return mapping.firstOrNull { text.contains(it.first) }?.second.orEmpty()
    }

    private fun reject(message: String) = VerificationResult(false, message)
}
