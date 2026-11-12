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
import androidx.compose.foundation.Image
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Report
import androidx.compose.material.icons.filled.Help
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.RateReview
import androidx.compose.material.icons.filled.SupportAgent
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Person
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
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
    var emailSubject by remember { mutableStateOf("") }
    var emailBody by remember { mutableStateOf("") }

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
        if ((step == 0 || step == 1) && !showSettings) {
            val stepBackground = if (step == 0) R.drawable.step_one_bg else R.drawable.step_two_bg
            Image(
                painter = painterResource(stepBackground),
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
            Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.24f)))
        } else {
            AnimatedBackdrop(useRedTheme = !showSettings && step in 2..7)
        }
        if (showSettings) {
            SettingsScreen(onBack = { showSettings = false }) {
                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://t.me/K_J6k")))
            }
        } else {
            Column(
                Modifier.fillMaxSize().padding(horizontal = 22.dp, vertical = 24.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
                horizontalAlignment = Alignment.End
            ) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End, verticalAlignment = Alignment.CenterVertically) {
                    OutlinedButton(onClick = { showSettings = true }) {
                        Icon(Icons.Filled.Settings, contentDescription = "الإعدادات")
                        Spacer(Modifier.width(8.dp))
                        Text("الإعدادات")
                    }
                }

                Column(Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()), horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        Text("الخطوة ${(step + 1).coerceAtMost(8)} من 8", color = Mint, fontSize = 12.sp)
                    }
                    Box(Modifier.fillMaxWidth().height(4.dp).background(Color.White.copy(alpha = 0.10f), RoundedCornerShape(8.dp))) {
                        Box(Modifier.fillMaxWidth((step + 1) / 8f).height(4.dp).background(Mint, RoundedCornerShape(8.dp)))
                    }
                    val borderMotion = rememberInfiniteTransition(label = "rainbow-border")
                    val borderPhase by borderMotion.animateFloat(
                        initialValue = 0f,
                        targetValue = 1f,
                        animationSpec = infiniteRepeatable(animation = tween(5200, easing = LinearEasing)),
                        label = "border-phase"
                    )
                    val borderColors = listOf(
                        Color(0xFFFF4D8D), Color(0xFFFFC857), Color(0xFF6DFFB8),
                        Color(0xFF45D9FF), Color(0xFF8C70FF), Color(0xFFFF4D8D)
                    )
                    val movingStops = borderColors.mapIndexed { index, color ->
                        ((index.toFloat() / borderColors.size + borderPhase) % 1f) to color
                    }.sortedBy { it.first }.toTypedArray()
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        border = BorderStroke(2.dp, Brush.sweepGradient(*movingStops)),
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
                                    StepHeading("01", "رقم الهاتف المحظور", "اكتب رقم واتساب المرتبط بالحساب الذي تريد فك حظره.")
                                    OutlinedTextField(
                                        value = phone,
                                        onValueChange = { value -> phone = value.filter { it in '0'..'9' }.take(11); error = "" },
                                        modifier = Modifier.fillMaxWidth(),
                                        label = { Text("رقم الهاتف") },
                                        leadingIcon = { Icon(Icons.Filled.PhoneAndroid, contentDescription = null, tint = Mint) },
                                        placeholder = { Text("01xxxxxxxxx") },
                                        singleLine = true,
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone)
                                    )
                                }
                                1 -> {
                                    StepHeading("02", "صورة إشعار الحظر", "اختار لقطة شاشة واضحة من داخل واتساب. الفحص بيتم محليًا.")
                                    Button(
                                        onClick = { imagePicker.launch("image/*") },
                                        enabled = !busy,
                                        modifier = Modifier.fillMaxWidth()
                                     ) {
                                        Icon(Icons.Filled.PhotoLibrary, contentDescription = null)
                                        Spacer(Modifier.width(8.dp))
                                        Text(if (busy) "جاري فحص الصورة…" else "اختيار صورة من الهاتف")
                                    }
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
                                    StepHeading("06", "اكتب اسمك عشان يظهر في الخطاب", "اكتب الاسم الذي تريد ظهوره في الخطاب. (اختياري)")
                                    OutlinedTextField(
                                        value = fullName,
                                        onValueChange = { fullName = it.take(60) },
                                        modifier = Modifier.fillMaxWidth(),
                                        label = { Text("اكتب اسمك") },
                                        leadingIcon = { Icon(Icons.Filled.Person, contentDescription = null, tint = Mint) },
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
                                    Text("معاينة الرسالة قبل فتح البريد", color = Mint, fontWeight = FontWeight.Bold)
                                    Text("الموضوع: $emailSubject", color = Color.White, fontSize = 13.sp, textAlign = TextAlign.End)
                                    Text(emailBody.ifBlank { "نص الطلب سيظهر هنا قبل فتح تطبيق البريد." }, color = Muted, fontSize = 12.sp, textAlign = TextAlign.End)
                                    Text("هنفتح تطبيق البريد بالعنوان والنص كاملين. راجع الرسالة واضغط إرسال بنفسك؛ التطبيق لا يرسل دون إذنك.", color = Muted, fontSize = 11.sp, textAlign = TextAlign.End)
                                }
                            }
                            if (error.isNotBlank()) Text(error, color = Color(0xFFFF8D8D), textAlign = TextAlign.End)
                        }
                    }
                }

                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    if (step > 0) {
                        OutlinedButton(onClick = { step--; error = "" }, modifier = Modifier.weight(1f)) {
                            Icon(Icons.Filled.ArrowBack, contentDescription = null)
                            Spacer(Modifier.width(6.dp))
                            Text("رجوع")
                        }
                    }
                    Button(
                        onClick = {
                            error = when (step) {
                                0 -> if (AppealTemplates.validatePhone(phone)) "" else "رقم الهاتف غير صحيح. راجع الأرقام وحاول مرة أخرى."
                                1 -> if (imageAccepted) "" else "لازم تفحص صورة إشعار الحظر الصحيحة قبل المتابعة."
                                else -> ""
                            }
                            if (error.isBlank()) {
                                if (step < 7) {
                                    if (step == 6) {
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
                                        emailSubject = when {
                                            firstLine.startsWith("Subject:") -> firstLine.removePrefix("Subject:").trim()
                                            firstLine.startsWith("الموضوع:") -> firstLine.removePrefix("الموضوع:").trim()
                                            else -> "WhatsApp account review request - $phone"
                                        }
                                        emailBody = if (firstLine.startsWith("Subject:") || firstLine.startsWith("الموضوع:")) {
                                            message.substringAfter("\n\n", message)
                                        } else message
                                        if (message.startsWith("تعذر")) error = message
                                    }
                                    if (error.isBlank()) step++
                                } else {
                                    val emailIntent = Intent(Intent.ACTION_SEND).apply {
                                        type = "message/rfc822"
                                        putExtra(Intent.EXTRA_EMAIL, arrayOf("support@whatsapp.com"))
                                        putExtra(Intent.EXTRA_SUBJECT, emailSubject)
                                        putExtra(Intent.EXTRA_TEXT, emailBody)
                                    }
                                    try {
                                        context.startActivity(Intent.createChooser(emailIntent, "إرسال طلب مراجعة واتساب"))
                                    } catch (_: Exception) {
                                        error = "لم يتم العثور على تطبيق بريد. ثبّت Gmail أو تطبيق بريد ثم حاول مرة أخرى."
                                    }
                                }
                            }
                        },
                        modifier = Modifier.weight(2f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (step == 7) Color.Black else Color.Transparent,
                            contentColor = Mint
                        )
                     ) {
                        if (step == 7) {
                            Icon(Icons.Filled.Send, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text("فتح البريد وتجهيز الطلب")
                        } else {
                            Text("متابعة", fontWeight = FontWeight.Bold)
                            Spacer(Modifier.width(8.dp))
                            Icon(Icons.Filled.ArrowForward, contentDescription = null)
                        }
                    }
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
    val headingIcon = when (number) {
        "01" -> Icons.Filled.PhoneAndroid
        "02" -> Icons.Filled.PhotoLibrary
        "03" -> Icons.Filled.Block
        "04" -> Icons.Filled.Help
        "05" -> Icons.Filled.RateReview
        "06" -> Icons.Filled.Person
        "07" -> Icons.Filled.Edit
        else -> Icons.Filled.CheckCircle
    }
    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(7.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(number, color = Mint, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            Icon(headingIcon, contentDescription = null, tint = Mint, modifier = Modifier.width(18.dp))
        }
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
            val optionIcon = choiceIcon(value)
            if (active) {
                Button(
                    onClick = { onSelect(value) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Black, contentColor = Mint)
                 ) {
                    Text(label, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.width(8.dp))
                    Icon(Icons.Filled.CheckCircle, contentDescription = "محدد")
                    Spacer(Modifier.width(8.dp))
                    Icon(optionIcon, contentDescription = null)
                }
            } else {
                OutlinedButton(onClick = { onSelect(value) }, modifier = Modifier.fillMaxWidth()) {
                    Text(label)
                    Spacer(Modifier.width(8.dp))
                    Icon(optionIcon, contentDescription = null, tint = Mint)
                }
            }
        }
    }
}

private fun choiceIcon(value: String): ImageVector = when (value) {
    "standard" -> Icons.Filled.Block
    "temporary" -> Icons.Filled.Schedule
    "severe" -> Icons.Filled.Warning
    "mistake" -> Icons.Filled.Report
    "unusual" -> Icons.Filled.Bolt
    "reports" -> Icons.Filled.Notifications
    "content" -> Icons.Filled.Info
    "unknown" -> Icons.Filled.Help
    "other" -> Icons.Filled.Edit
    "review" -> Icons.Filled.RateReview
    "human" -> Icons.Filled.SupportAgent
    "urgent" -> Icons.Filled.Bolt
    "explain" -> Icons.Filled.Info
    "long" -> Icons.Filled.Edit
    "short" -> Icons.Filled.CheckCircle
    "arabic" -> Icons.Filled.Translate
    "followup" -> Icons.Filled.History
    "escalation" -> Icons.Filled.TrendingUp
    "final" -> Icons.Filled.Notifications
    else -> Icons.Filled.CheckCircle
}

@androidx.compose.runtime.Composable
private fun AnimatedBackdrop(useRedTheme: Boolean = false) {
    Canvas(Modifier.fillMaxSize()) {
        drawRect(brush = Brush.linearGradient(
            colors = if (useRedTheme) listOf(Color(0xFF020203), Color(0xFF16070B), Color(0xFF350B16), Color(0xFF080609)) else listOf(Color(0xFF07131F), Color(0xFF102A38), Color(0xFF11152B)),
            start = androidx.compose.ui.geometry.Offset(0f, 0f),
            end = androidx.compose.ui.geometry.Offset(size.width, size.height)
        ))
        if (useRedTheme) {
            drawCircle(Color(0x35B51632), radius = size.minDimension * 0.52f,
                center = androidx.compose.ui.geometry.Offset(size.width * 0.04f, size.height * 0.20f))
            drawCircle(Color(0x258C1028), radius = size.minDimension * 0.42f,
                center = androidx.compose.ui.geometry.Offset(size.width * 0.96f, size.height * 0.86f))
        } else {
            drawCircle(Color(0x2638E8C0), radius = size.minDimension * 0.48f,
                center = androidx.compose.ui.geometry.Offset(size.width * 0.08f, size.height * 0.18f))
            drawCircle(Color(0x263D8BFF), radius = size.minDimension * 0.40f,
                center = androidx.compose.ui.geometry.Offset(size.width * 0.94f, size.height * 0.84f))
        }
    }
}

@androidx.compose.runtime.Composable
private fun SettingsScreen(onBack: () -> Unit, onOpenTelegram: () -> Unit) {
    Column(
        Modifier.fillMaxSize().padding(22.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
        horizontalAlignment = Alignment.End
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Icon(Icons.Filled.Settings, contentDescription = null, tint = Mint)
            Text("الإعدادات", color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.Bold)
        }
        Card(colors = CardDefaults.cardColors(containerColor = Panel), shape = RoundedCornerShape(24.dp)) {
            Column(Modifier.fillMaxWidth().padding(20.dp), horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Filled.Info, contentDescription = null, tint = Mint)
                    Text("عن التطبيق", color = Mint, fontSize = 19.sp, fontWeight = FontWeight.Bold)
                }
                Text("فحص محلي لصور إشعارات حظر واتساب وتجهيز طلب مراجعة. لا يتم رفع الصور إلى أي خادم.", color = Muted, textAlign = TextAlign.End)
            }
        }
        Card(colors = CardDefaults.cardColors(containerColor = Panel), shape = RoundedCornerShape(24.dp)) {
            Column(Modifier.fillMaxWidth().padding(20.dp), horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Filled.Send, contentDescription = null, tint = Mint)
                    Text("المطور والدعم", color = Mint, fontSize = 19.sp, fontWeight = FontWeight.Bold)
                }
                Button(onClick = onOpenTelegram, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Filled.Send, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("قناة المطور على تيليجرام")
                }
                OutlinedButton(onClick = onOpenTelegram, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Filled.Send, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("تواصل مع المطور")
                }
                Text("@K_J6k", color = Muted)
            }
        }
        Spacer(Modifier.weight(1f))
        OutlinedButton(onClick = onBack, modifier = Modifier.fillMaxWidth()) {
            Icon(Icons.Filled.ArrowBack, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text("رجوع")
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
    val lightning = rememberInfiniteTransition(label = "hatem-lightning")
    val flash by lightning.animateFloat(
        initialValue = 0.16f,
        targetValue = 0.92f,
        animationSpec = infiniteRepeatable(animation = tween(1250)),
        label = "lightning-flash"
    )
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
        Text("ϟ", color = Color.White.copy(alpha = flash), fontSize = 23.sp, fontWeight = FontWeight.Black)
        Spacer(Modifier.height(1.dp))
        Text("H A T E M", color = Mint.copy(alpha = flash), fontWeight = FontWeight.ExtraBold, letterSpacing = 2.sp)
        Text("ϟ", color = Color.White.copy(alpha = flash), fontSize = 23.sp, fontWeight = FontWeight.Black)
    }
}
