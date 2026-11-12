package com.marayti.unblock

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.LinearEasing
import androidx.compose.foundation.Canvas
import androidx.compose.ui.draw.drawBehind
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private val Ink = Color(0xFF07131F)
private val Panel = Color(0xFF142431)
private val Mint = Color(0xFF65E0C0)
private val Muted = Color(0xFFA7B7C3)
private val DisplayFont = FontFamily(Font(R.font.alexandria_variable))

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme(colorScheme = darkColorScheme(
                primary = Mint, onPrimary = Ink, background = Ink,
                surface = Panel, onSurface = Color.White
            ), typography = MaterialTheme.typography.copy(
                headlineLarge = MaterialTheme.typography.headlineLarge.copy(fontFamily = DisplayFont),
                headlineMedium = MaterialTheme.typography.headlineMedium.copy(fontFamily = DisplayFont),
                titleLarge = MaterialTheme.typography.titleLarge.copy(fontFamily = DisplayFont),
                titleMedium = MaterialTheme.typography.titleMedium.copy(fontFamily = DisplayFont),
                bodyLarge = MaterialTheme.typography.bodyLarge.copy(fontFamily = DisplayFont),
                bodyMedium = MaterialTheme.typography.bodyMedium.copy(fontFamily = DisplayFont),
                labelLarge = MaterialTheme.typography.labelLarge.copy(fontFamily = DisplayFont)
            )) {
                Surface(Modifier.fillMaxSize(), color = Ink) {
                    UnblockFlow(
                        onReadImage = { uri ->
                            contentResolver.openInputStream(uri)?.use { input ->
                                val output = java.io.ByteArrayOutputStream()
                                val buffer = ByteArray(8192)
                                var total = 0
                                while (true) {
                                    val count = input.read(buffer)
                                    if (count < 0) break
                                    total += count
                                    if (total > 5 * 1024 * 1024) return@use null
                                    output.write(buffer, 0, count)
                                }
                                output.toByteArray()
                            }
                        }
                    )
                }
            }
        }
    }
}

@androidx.compose.runtime.Composable
private fun UnblockFlow(onReadImage: (Uri) -> ByteArray?) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var showIntro by remember { mutableStateOf(true) }
    var introText by remember { mutableStateOf("") }
    var step by remember { mutableStateOf(0) }
    var phone by remember { mutableStateOf("") }
    var fullName by remember { mutableStateOf("") }
    var severity by remember { mutableStateOf("standard") }
    var reason by remember { mutableStateOf("unknown") }
    var requestType by remember { mutableStateOf("review") }
    var outputType by remember { mutableStateOf("long") }
    var imageStatus by remember { mutableStateOf("لم يتم اختيار صورة إشعار الحظر بعد") }
    var imageAccepted by remember { mutableStateOf(false) }
    var evidence by remember { mutableStateOf(VerificationResult(false, "")) }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf("") }
    var showSettings by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        val message = "UNBLOCK"
        for (index in 1..message.length) {
            introText = message.take(index)
            delay(420)
        }
        delay(700)
        showIntro = false
    }

    val imagePicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            busy = true
            imageAccepted = false
            evidence = VerificationResult(false, "")
            imageStatus = "جاري فحص الصورة على هاتفك…"
            scope.launch {
                val result = try {
                    withContext(Dispatchers.IO) {
                        val bytes = onReadImage(uri)
                        if (bytes == null) VerificationResult(false, "تعذر قراءة الصورة أو أن حجمها أكبر من 5 ميجابايت")
                        else LocalScreenshotVerifier(context).verify(bytes)
                    }
                } catch (exception: Exception) {
                    VerificationResult(false, "فشل فحص الصورة: ${exception.message ?: "خطأ غير معروف"}")
                } finally {
                    busy = false
                }
                evidence = result
                imageAccepted = result.accepted
                imageStatus = result.message
            }
        }
    }

    Box(Modifier.fillMaxSize()) {
        AnimatedBackdrop()
        if (showSettings) {
            SettingsScreen(onBack = { showSettings = false }) {
                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://t.me/K_J6k")))
            }
        } else {
            Column(
                Modifier.fillMaxSize().padding(horizontal = 22.dp, vertical = 24.dp),
                verticalArrangement = Arrangement.SpaceBetween,
                horizontalAlignment = Alignment.End
            ) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("UNBLOCK  /  WHATSAPP", color = Mint, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    OutlinedButton(onClick = { showSettings = true }) { Text("الإعدادات") }
                }

                Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Text("مساعد مراجعة الحساب", color = Color.White, fontSize = 26.sp, fontWeight = FontWeight.Bold)
                    Text("هنمشي معاك خطوة بخطوة. بياناتك وصورك بتفضل على جهازك.", color = Muted, fontSize = 13.sp, textAlign = TextAlign.End)
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        Text("الخطوة ${(step + 1).coerceAtMost(8)} من 8", color = Mint, fontSize = 12.sp)
                    }
                    Box(Modifier.fillMaxWidth().height(4.dp).background(Color.White.copy(alpha = 0.10f), RoundedCornerShape(8.dp))) {
                        Box(Modifier.fillMaxWidth((step + 1) / 8f).height(4.dp).background(Mint, RoundedCornerShape(8.dp)))
                    }
                    val borderMotion = rememberInfiniteTransition(label = "rainbow-border")
                    val borderAngle by borderMotion.animateFloat(
                        initialValue = 0f,
                        targetValue = 360f,
                        animationSpec = infiniteRepeatable(animation = tween(4200, easing = LinearEasing)),
                        label = "border-angle"
                    )
                    Card(
                        modifier = Modifier.fillMaxWidth().drawBehind {
                            rotate(borderAngle, center) {
                            drawRoundRect(
                                brush = Brush.sweepGradient(
                                    listOf(
                                        Color(0xFFFF4D8D), Color(0xFFFFC857), Color(0xFF6DFFB8),
                                        Color(0xFF45D9FF), Color(0xFF8C70FF), Color(0xFFFF4D8D)
                                    )
                                ),
                                cornerRadius = CornerRadius(28.dp.toPx()),
                                style = Stroke(width = 2.5.dp.toPx())
                            )
                            }
                        },
                        colors = CardDefaults.cardColors(containerColor = Panel.copy(alpha = 0.97f)),
                        shape = RoundedCornerShape(28.dp)
                    ) {
                        Column(
                            Modifier.fillMaxWidth().padding(22.dp),
                            horizontalAlignment = Alignment.End,
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            when (step) {
                                0 -> {
                                    StepHeading("01", "رقم الهاتف المحظور", "اكتب رقم واتساب المرتبط بالحساب الذي تريد مراجعته.")
                                    OutlinedTextField(
                                        value = phone,
                                        onValueChange = { value -> phone = value.filter { it in '0'..'9' }.take(11); error = "" },
                                        modifier = Modifier.fillMaxWidth(),
                                        label = { Text("رقم الهاتف المصري") },
                                        placeholder = { Text("01xxxxxxxxx") },
                                        singleLine = true,
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone)
                                    )
                                    Text("11 رقمًا ويبدأ بـ 010 أو 011 أو 012 أو 015", color = Muted, fontSize = 11.sp)
                                }
                                1 -> {
                                    StepHeading("02", "صورة إشعار الحظر", "اختار لقطة شاشة واضحة من داخل واتساب. الفحص بيتم محليًا.")
                                    Button(
                                        onClick = { imagePicker.launch("image/*") },
                                        enabled = !busy,
                                        modifier = Modifier.fillMaxWidth()
                                    ) { Text(if (busy) "جاري فحص الصورة…" else "اختيار صورة من الهاتف") }
                                    Text(imageStatus, color = if (imageAccepted) Mint else Muted, textAlign = TextAlign.End)
                                    if (imageAccepted) Text("تم التحقق من الإشعار بنجاح", color = Mint, fontWeight = FontWeight.Bold)
                                }
                                2 -> {
                                    StepHeading("03", "نوع الحظر", "اختار أقرب وصف للحالة الظاهرة عندك.")
                                    ChoiceRow(listOf("حظر عادي" to "standard", "مؤقت أو متكرر" to "temporary", "حظر مشدد" to "severe"), severity) { severity = it }
                                }
                                3 -> {
                                    StepHeading("04", "سبب الحظر", "لو مش متأكد من السبب، اختار لا أعرف.")
                                    ChoiceRow(listOf("أعتقد أنه خطأ" to "mistake", "نشاط غير معتاد" to "unusual", "بلاغات أو شكاوى" to "reports", "مشكلة محتوى" to "content", "لا أعرف السبب" to "unknown", "سبب آخر" to "other"), reason) { reason = it }
                                }
                                4 -> {
                                    StepHeading("05", "نوع المراجعة", "اختار الطريقة الأقرب لطلبك.")
                                    ChoiceRow(listOf("مراجعة عادية" to "review", "مراجعة بشرية" to "human", "طلب عاجل" to "urgent", "توضيح السبب" to "explain"), requestType) { requestType = it }
                                }
                                5 -> {
                                    StepHeading("06", "اسم صاحب الحساب", "اكتب اسمك كما تحب أن يظهر في خطاب المراجعة. (اختياري)")
                                    OutlinedTextField(
                                        value = fullName,
                                        onValueChange = { fullName = it.take(60) },
                                        modifier = Modifier.fillMaxWidth(),
                                        label = { Text("الاسم الكامل") },
                                        singleLine = true
                                    )
                                }
                                6 -> {
                                    StepHeading("07", "صيغة الرسالة", "اختار شكل الخطاب الذي تريد إرساله.")
                                    ChoiceRow(listOf("خطاب كامل" to "long", "رسالة قصيرة" to "short", "النسخة العربية" to "arabic", "متابعة بعد 48 ساعة" to "followup", "تصعيد الطلب" to "escalation", "إشعار أخير" to "final"), outputType) { outputType = it }
                                }
                                else -> {
                                    StepHeading("08", "راجع بياناتك", "اضغط الزر لتجهيز رسالة المراجعة في تطبيق البريد.")
                                    SummaryLine("رقم الهاتف", phone)
                                    SummaryLine("نوع الحظر", severityLabel(severity))
                                    SummaryLine("السبب", reasonLabel(reason))
                                    SummaryLine("نوع المراجعة", requestLabel(requestType))
                                    SummaryLine("صيغة الرسالة", outputLabel(outputType))
                                    SummaryLine("فحص الصورة", if (imageAccepted) "تم بنجاح" else "غير مكتمل")
                                    Text("هنفتح تطبيق البريد برسالة جاهزة. راجعها واضغط إرسال بنفسك؛ التطبيق مش بيبعت رسائل من غير إذنك.", color = Muted, fontSize = 12.sp, textAlign = TextAlign.End)
                                }
                            }
                            if (error.isNotBlank()) Text(error, color = Color(0xFFFF8D8D), textAlign = TextAlign.End)
                        }
                    }
                }

                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    if (step > 0) {
                        OutlinedButton(onClick = { step--; error = "" }, modifier = Modifier.weight(1f)) { Text("رجوع") }
                    }
                    Button(
                        onClick = {
                            error = when (step) {
                                0 -> if (AppealTemplates.validatePhone(phone)) "" else "رقم الهاتف غير صحيح. راجع الأرقام وحاول مرة أخرى."
                                1 -> if (imageAccepted) "" else "لازم تفحص صورة إشعار الحظر الصحيحة قبل المتابعة."
                                else -> ""
                            }
                            if (error.isBlank()) {
                                if (step < 7) step++ else {
                                    val message = runCatching {
                                        when (outputType) {
                                            "short" -> AppealTemplates.buildShort(severity, phone, reason, requestType, evidence)
                                            "arabic" -> AppealTemplates.buildArabic(severity, phone, reason, requestType, evidence, fullName)
                                            "followup" -> AppealTemplates.buildFollowUp(severity, phone, reason, requestType, evidence, fullName)
                                            "escalation" -> AppealTemplates.buildEscalation(severity, phone, reason, requestType, evidence, fullName)
                                            "final" -> AppealTemplates.buildFinalNotice(severity, phone, reason, requestType, evidence, fullName)
                                            else -> AppealTemplates.build(severity, phone, reason, requestType, evidence, fullName)
                                        }
                                    }.getOrElse { it.message ?: "تعذر تجهيز الرسالة" }
                                    val firstLine = message.lineSequence().firstOrNull().orEmpty()
                                    val hasSubject = firstLine.startsWith("Subject:") || firstLine.startsWith("الموضوع:")
                                    val subject = when {
                                        firstLine.startsWith("Subject:") -> firstLine.removePrefix("Subject:").trim()
                                        firstLine.startsWith("الموضوع:") -> firstLine.removePrefix("الموضوع:").trim()
                                        else -> "WhatsApp account review request - $phone"
                                    }
                                    val body = if (hasSubject) message.substringAfter("\n\n", "") else message
                                    val emailIntent = Intent(Intent.ACTION_SENDTO).apply {
                                        data = Uri.parse("mailto:support@whatsapp.com")
                                        putExtra(Intent.EXTRA_SUBJECT, subject)
                                        putExtra(Intent.EXTRA_TEXT, body)
                                    }
                                    try {
                                        context.startActivity(emailIntent)
                                    } catch (_: Exception) {
                                        error = "مفيش تطبيق بريد مثبت. ثبّت Gmail أو تطبيق بريد ثم حاول تاني."
                                    }
                                }
                            }
                        },
                        modifier = Modifier.weight(2f),
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Black, contentColor = Mint)
                    ) { if (step == 7) Text("فتح البريد وتجهيز الطلب") else HatemAnimatedLabel() }
                }
            }
        }
        AnimatedVisibility(
            visible = showIntro,
            enter = fadeIn(tween(80)),
            exit = fadeOut(tween(520)),
            modifier = Modifier.fillMaxSize()
        ) {
            Box(
                Modifier.fillMaxSize().background(Color.Black),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(introText, color = Color.White, fontSize = 42.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 5.sp)
                    Text("استعد لاستعادة حسابك", color = Mint.copy(alpha = 0.85f), fontSize = 14.sp)
                }
            }
        }
    }
}

@androidx.compose.runtime.Composable
private fun StepHeading(number: String, title: String, description: String) {
    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(7.dp)) {
        Text(number, color = Mint, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        Text(title, color = Color.White, fontSize = 23.sp, fontWeight = FontWeight.Bold)
        Text(description, color = Muted, fontSize = 13.sp, textAlign = TextAlign.End)
    }
}

@androidx.compose.runtime.Composable
private fun SummaryLine(label: String, value: String) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(value, color = Color.White, fontSize = 12.sp)
        Text(label, color = Muted, fontSize = 12.sp)
    }
}

@androidx.compose.runtime.Composable
private fun ChoiceRow(options: List<Pair<String, String>>, selected: String, onSelect: (String) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
        options.forEach { (label, value) ->
            val active = selected == value
            if (active) {
                Button(
                    onClick = { onSelect(value) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Black, contentColor = Mint)
                ) { Text(label, fontWeight = FontWeight.SemiBold) }
            } else {
                OutlinedButton(onClick = { onSelect(value) }, modifier = Modifier.fillMaxWidth()) { Text(label) }
            }
        }
    }
}

@androidx.compose.runtime.Composable
private fun AnimatedBackdrop() {
    Canvas(Modifier.fillMaxSize()) {
        drawRect(brush = Brush.linearGradient(
            colors = listOf(Color(0xFF07131F), Color(0xFF102A38), Color(0xFF11152B)),
            start = androidx.compose.ui.geometry.Offset(0f, 0f),
            end = androidx.compose.ui.geometry.Offset(size.width, size.height)
        ))
        drawCircle(Color(0x2638E8C0), radius = size.minDimension * 0.48f,
            center = androidx.compose.ui.geometry.Offset(size.width * 0.08f, size.height * 0.18f))
        drawCircle(Color(0x263D8BFF), radius = size.minDimension * 0.40f,
            center = androidx.compose.ui.geometry.Offset(size.width * 0.94f, size.height * 0.84f))
    }
}

@androidx.compose.runtime.Composable
private fun SettingsScreen(onBack: () -> Unit, onOpenTelegram: () -> Unit) {
    Column(
        Modifier.fillMaxSize().padding(22.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
        horizontalAlignment = Alignment.End
    ) {
        OutlinedButton(onClick = onBack, modifier = Modifier.fillMaxWidth()) { Text("رجوع") }
        Text("الإعدادات", color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.Bold)
        Card(colors = CardDefaults.cardColors(containerColor = Panel), shape = RoundedCornerShape(24.dp)) {
            Column(Modifier.fillMaxWidth().padding(20.dp), horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("عن التطبيق", color = Mint, fontSize = 19.sp, fontWeight = FontWeight.Bold)
                Text("فحص محلي لصور إشعارات حظر واتساب وتجهيز طلب مراجعة. لا يتم رفع الصور إلى أي خادم.", color = Muted, textAlign = TextAlign.End)
            }
        }
        Card(colors = CardDefaults.cardColors(containerColor = Panel), shape = RoundedCornerShape(24.dp)) {
            Column(Modifier.fillMaxWidth().padding(20.dp), horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("المطور والدعم", color = Mint, fontSize = 19.sp, fontWeight = FontWeight.Bold)
                Button(onClick = onOpenTelegram, modifier = Modifier.fillMaxWidth()) { Text("قناة المطور على تيليجرام") }
                OutlinedButton(onClick = onOpenTelegram, modifier = Modifier.fillMaxWidth()) { Text("تواصل مع المطور") }
                Text("@K_J6k", color = Muted)
            }
        }
    }
}

private fun severityLabel(value: String) = when (value) { "temporary" -> "مؤقت أو متكرر"; "severe" -> "مشدد"; else -> "عادي" }
private fun reasonLabel(value: String) = when (value) {
    "mistake" -> "أعتقد أنه خطأ"; "unusual" -> "نشاط غير معتاد"; "reports" -> "بلاغات أو شكاوى"
    "content" -> "مشكلة محتوى"; "other" -> "سبب آخر"; else -> "لا أعرف السبب"
}
private fun requestLabel(value: String) = when (value) { "human" -> "مراجعة بشرية"; "urgent" -> "طلب عاجل"; "explain" -> "توضيح السبب"; else -> "مراجعة عادية" }
private fun outputLabel(value: String) = when (value) { "short" -> "رسالة قصيرة"; "arabic" -> "النسخة العربية"; "followup" -> "متابعة"; "escalation" -> "تصعيد"; "final" -> "إشعار أخير"; else -> "خطاب كامل" }

@androidx.compose.runtime.Composable
private fun HatemAnimatedLabel() {
    var visibleLetters by remember { mutableStateOf(0) }
    LaunchedEffect(Unit) {
        while (true) {
            for (count in 1..5) { visibleLetters = count; delay(260) }
            delay(900)
            for (count in 4 downTo 0) { visibleLetters = count; delay(150) }
            delay(250)
        }
    }
    Text("HATEM".take(visibleLetters).toCharArray().joinToString(" "), color = Mint, fontWeight = FontWeight.ExtraBold, letterSpacing = 3.sp)
}
