package com.marayti.unblock

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private val Ink = Color(0xFF101923)
private val Panel = Color(0xFF1A2732)
private val Mint = Color(0xFF65E0C0)
private val Muted = Color(0xFF9AABB8)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme(colorScheme = darkColorScheme(
                primary = Mint, onPrimary = Ink, background = Ink,
                surface = Panel, onSurface = Color.White
            )) {
                Surface(Modifier.fillMaxSize(), color = Ink) {
                    UnblockHome(
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
                        },
                        onShare = { text ->
                            val send = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_TEXT, text)
                            }
                            startActivity(Intent.createChooser(send, "مشاركة طلب المراجعة"))
                        }
                    )
                }
            }
        }
    }
}

@androidx.compose.runtime.Composable
private fun UnblockHome(onReadImage: (Uri) -> ByteArray?, onShare: (String) -> Unit) {
    val context = LocalContext.current
    var severity by remember { mutableStateOf("standard") }
    var reason by remember { mutableStateOf("unknown") }
    var requestType by remember { mutableStateOf("review") }
    var phone by remember { mutableStateOf("") }
    var imageStatus by remember { mutableStateOf("لم يتم فحص صورة بعد") }
    var verdictAccepted by remember { mutableStateOf(false) }
    var draft by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    val imagePicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            busy = true
            verdictAccepted = false
            draft = ""
            imageStatus = "جاري فحص الصورة محليًا…"
            scope.launch {
                val result = withContext(Dispatchers.IO) {
                    val bytes = onReadImage(uri)
                    if (bytes == null) VerificationResult(false, "تعذر قراءة الصورة من الهاتف")
                    else LocalScreenshotVerifier(context).verify(bytes)
                }
                verdictAccepted = result.accepted
                imageStatus = result.message
                busy = false
            }
        }
    }

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.End
    ) {
        Text("UNBLOCK  ·  WHATSAPP", color = Mint, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        Text("جهّز طلب مراجعتك بثقة", color = Color.White, fontSize = 29.sp, fontWeight = FontWeight.ExtraBold)
        Text("أدواتك تعمل على هاتفك. صورك لا تُرفع إلى أي خادم.", color = Muted, fontSize = 14.sp)
        Card(colors = CardDefaults.cardColors(containerColor = Panel), shape = RoundedCornerShape(22.dp)) {
            Column(Modifier.fillMaxWidth().padding(18.dp), horizontalAlignment = Alignment.End) {
                Text("01  ·  صورة إشعار الحظر", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                Spacer(Modifier.height(8.dp))
                Text(imageStatus, color = if (verdictAccepted) Mint else Muted, textAlign = TextAlign.End)
                Spacer(Modifier.height(12.dp))
                Button(onClick = { imagePicker.launch("image/*") }, enabled = !busy, modifier = Modifier.fillMaxWidth()) {
                    Text(if (busy) "جاري الفحص…" else "اختيار صورة من الهاتف")
                }
            }
        }
        ChoiceCard("02  ·  نوع الحالة", listOf("حظر عادي" to "standard", "حظر مؤقت أو متكرر" to "temporary", "حظر مشدد" to "severe"), severity) { severity = it }
        ChoiceCard("03  ·  السبب الأقرب", listOf("أعتقد أنه خطأ" to "mistake", "نشاط غير معتاد" to "unusual", "بلاغات أو شكاوى" to "reports", "مشكلة محتوى" to "content", "لا أعرف السبب" to "unknown", "سبب آخر" to "other"), reason) { reason = it }
        ChoiceCard("04  ·  نوع الطلب", listOf("مراجعة عادية" to "review", "مراجعة بشرية" to "human", "طلب عاجل" to "urgent", "توضيح السبب" to "explain"), requestType) { requestType = it }
        OutlinedTextField(
            value = phone,
            onValueChange = { phone = it.filter(Char::isDigit).take(11) },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("رقم واتساب المصري") },
            placeholder = { Text("01xxxxxxxxx") },
            singleLine = true
        )
        Button(onClick = {
            draft = when {
                !verdictAccepted -> "افحص أولًا صورة إشعار حظر واتساب الصحيحة."
                !phone.matches(Regex("01[0125][0-9]{8}")) -> "راجع الرقم: يجب أن يتكون من 11 رقمًا ويبدأ بـ 010 أو 011 أو 012 أو 015."
                else -> AppealTemplates.build(severity, phone, reason, requestType)
            }
        }, modifier = Modifier.fillMaxWidth()) { Text("إنشاء طلب المراجعة") }
        if (draft.isNotBlank()) {
            Card(colors = CardDefaults.cardColors(containerColor = Panel), shape = RoundedCornerShape(20.dp)) {
                Column(Modifier.padding(16.dp), horizontalAlignment = Alignment.End) {
                    Text("مسودة الطلب", color = Mint, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(8.dp))
                    Text(draft, color = Color.White, textAlign = TextAlign.End)
                    Spacer(Modifier.height(12.dp))
                    OutlinedButton(onClick = { onShare(draft) }, modifier = Modifier.fillMaxWidth()) {
                        Text("مشاركة / فتح البريد")
                    }
                }
            }
        }
        Text("خصوصيتك أولًا · الفحص محلي على جهازك", color = Muted, fontSize = 12.sp, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
    }
}

@androidx.compose.runtime.Composable
private fun ChoiceCard(title: String, choices: List<Pair<String, String>>, selected: String, onSelect: (String) -> Unit) {
    Card(colors = CardDefaults.cardColors(containerColor = Panel), shape = RoundedCornerShape(22.dp)) {
        Column(Modifier.fillMaxWidth().padding(16.dp), horizontalAlignment = Alignment.End) {
            Text(title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 17.sp)
            Spacer(Modifier.height(10.dp))
            choices.chunked(2).forEach { pair ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End)) {
                    pair.forEach { (label, value) ->
                        if (selected == value) {
                            Button(onClick = { onSelect(value) }, colors = ButtonDefaults.buttonColors(containerColor = Mint, contentColor = Ink)) { Text(label) }
                        } else {
                            OutlinedButton(onClick = { onSelect(value) }) { Text(label) }
                        }
                    }
                }
            }
        }
    }
}
