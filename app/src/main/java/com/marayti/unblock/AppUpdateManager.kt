package com.marayti.unblock

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.FileProvider
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest
import java.util.Locale
import java.util.concurrent.TimeUnit

data class UpdateStatus(
    val currentVersion: String,
    val currentCode: Int,
    val latestVersion: String?,
    val message: String,
    val downloaded: Boolean
)

object UpdateScheduler {
    private const val PERIODIC_NAME = "unblock-periodic-update-check"
    private const val IMMEDIATE_NAME = "unblock-immediate-update-check"

    fun schedule(context: Context) {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()
        val periodic = PeriodicWorkRequestBuilder<AppUpdateWorker>(6, TimeUnit.HOURS)
            .setConstraints(constraints)
            .build()
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            PERIODIC_NAME, ExistingPeriodicWorkPolicy.KEEP, periodic
        )
        checkNow(context)
    }

    fun checkNow(context: Context) {
        val request = OneTimeWorkRequestBuilder<AppUpdateWorker>()
            .setConstraints(
                Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build()
            )
            .build()
        WorkManager.getInstance(context).enqueueUniqueWork(
            IMMEDIATE_NAME, ExistingWorkPolicy.KEEP, request
        )
    }
}

object UpdateManager {
    private const val PREFS = "unblock_update_state"
    private const val KEY_LATEST_VERSION = "latest_version"
    private const val KEY_LATEST_CODE = "latest_code"
    private const val KEY_MESSAGE = "message"
    private const val KEY_APK_PATH = "apk_path"

    internal fun preferences(context: Context) =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun readStatus(context: Context): UpdateStatus {
        val prefs = preferences(context)
        val path = prefs.getString(KEY_APK_PATH, null)
        val downloaded = !path.isNullOrBlank() && File(path).isFile
        val latestCode = prefs.getInt(KEY_LATEST_CODE, 0)
        val latestVersion = prefs.getString(KEY_LATEST_VERSION, null)
        val message = when {
            downloaded -> "التحديث الجديد جاهز للتثبيت."
            latestCode > BuildConfig.VERSION_CODE -> prefs.getString(KEY_MESSAGE, "يتوفر تحديث جديد.") ?: "يتوفر تحديث جديد."
            prefs.contains(KEY_MESSAGE) -> prefs.getString(KEY_MESSAGE, "لم يتم التحقق بعد.") ?: "لم يتم التحقق بعد."
            else -> "سيتم فحص التحديثات عند توفر الإنترنت."
        }
        return UpdateStatus(
            BuildConfig.VERSION_NAME,
            BuildConfig.VERSION_CODE,
            latestVersion,
            message,
            downloaded
        )
    }

    internal fun saveNoUpdate(context: Context, latestVersion: String, latestCode: Int) {
        preferences(context).edit()
            .putString(KEY_LATEST_VERSION, latestVersion)
            .putInt(KEY_LATEST_CODE, latestCode)
            .putString(KEY_MESSAGE, "أنت تستخدم أحدث إصدار.")
            .remove(KEY_APK_PATH)
            .apply()
    }

    internal fun saveAvailable(context: Context, version: String, code: Int, apk: File) {
        preferences(context).edit()
            .putString(KEY_LATEST_VERSION, version)
            .putInt(KEY_LATEST_CODE, code)
            .putString(KEY_MESSAGE, "تم تنزيل التحديث والتحقق من سلامته. أصبح جاهزًا للتثبيت.")
            .putString(KEY_APK_PATH, apk.absolutePath)
            .apply()
    }

    internal fun saveFailure(context: Context, message: String) {
        preferences(context).edit().putString(KEY_MESSAGE, message).apply()
    }

    internal fun apkFile(context: Context): File? {
        val path = preferences(context).getString(KEY_APK_PATH, null) ?: return null
        return File(path).takeIf { it.isFile && it.length() > 0L }
    }
}

class AppUpdateWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            val release = getJson(RELEASE_API)
            val tag = release.optString("tag_name")
            val parsed = Regex("^unblock-v(.+)-(\\d+)$").matchEntire(tag)
                ?: throw IllegalStateException("صيغة إصدار التحديث غير صحيحة.")
            val versionName = parsed.groupValues[1]
            val versionCode = parsed.groupValues[2].toInt()
            if (versionCode <= BuildConfig.VERSION_CODE) {
                UpdateManager.saveNoUpdate(applicationContext, versionName, versionCode)
                return@withContext Result.success()
            }

            val assets = release.getJSONArray("assets")
            var apkUrl: String? = null
            var checksumUrl: String? = null
            for (index in 0 until assets.length()) {
                val asset = assets.getJSONObject(index)
                when (asset.optString("name")) {
                    APK_ASSET -> apkUrl = asset.optString("browser_download_url")
                    CHECKSUM_ASSET -> checksumUrl = asset.optString("browser_download_url")
                }
            }
            val apk = apkUrl ?: throw IllegalStateException("ملف APK غير موجود في الإصدار.")
            val checksum = checksumUrl ?: throw IllegalStateException("ملف التحقق من سلامة APK غير موجود.")
            val targetDir = File(applicationContext.filesDir, "updates").apply { mkdirs() }
            val temporary = File(targetDir, "update.apk.part")
            val target = File(targetDir, "update.apk")
            temporary.delete()
            download(apk, temporary)
            val expectedHash = URL(checksum).openStream().bufferedReader().use { it.readText() }
                .trim().substringBefore(" ").lowercase(Locale.US)
            val actualHash = sha256(temporary)
            if (!expectedHash.matches(Regex("^[a-f0-9]{64}$")) || expectedHash != actualHash) {
                temporary.delete()
                throw SecurityException("فشل التحقق من بصمة التحديث؛ تم رفض الملف.")
            }
            if (target.exists()) target.delete()
            if (!temporary.renameTo(target)) throw IllegalStateException("تعذر حفظ ملف التحديث.")
            UpdateManager.saveAvailable(applicationContext, versionName, versionCode, target)
            notifyReady(versionName)
            Result.success()
        } catch (error: Exception) {
            UpdateManager.saveFailure(
                applicationContext,
                "تعذر فحص أو تنزيل التحديث: ${error.message ?: "خطأ اتصال"}"
            )
            if (runAttemptCount < 3) Result.retry() else Result.failure()
        }
    }

    private fun getJson(address: String): JSONObject {
        val connection = (URL(address).openConnection() as HttpURLConnection).apply {
            connectTimeout = 15000
            readTimeout = 20000
            setRequestProperty("Accept", "application/vnd.github+json")
            setRequestProperty("User-Agent", "UNBLOCK-Android-Updater")
        }
        return try {
            if (connection.responseCode !in 200..299) {
                throw IllegalStateException("خادم التحديث أعاد رمز ${connection.responseCode}")
            }
            JSONObject(connection.inputStream.bufferedReader().use { it.readText() })
        } finally {
            connection.disconnect()
        }
    }

    private fun download(address: String, destination: File) {
        val connection = (URL(address).openConnection() as HttpURLConnection).apply {
            connectTimeout = 20000
            readTimeout = 30000
            instanceFollowRedirects = true
            setRequestProperty("User-Agent", "UNBLOCK-Android-Updater")
        }
        try {
            if (connection.responseCode !in 200..299) {
                throw IllegalStateException("تعذر تنزيل التحديث: HTTP ${connection.responseCode}")
            }
            connection.inputStream.use { input ->
                destination.outputStream().buffered().use { output ->
                    val buffer = ByteArray(64 * 1024)
                    var total = 0L
                    while (true) {
                        val count = input.read(buffer)
                        if (count < 0) break
                        total += count
                        if (total > 200L * 1024L * 1024L) {
                            throw IllegalStateException("حجم ملف التحديث أكبر من الحد المسموح.")
                        }
                        output.write(buffer, 0, count)
                    }
                }
            }
            if (destination.length() < 1_000_000L) {
                throw IllegalStateException("ملف التحديث غير مكتمل.")
            }
        } finally {
            connection.disconnect()
        }
    }

    private fun sha256(file: File): String {
        val digest = MessageDigest.getInstance("SHA-256")
        file.inputStream().buffered().use { input ->
            val buffer = ByteArray(64 * 1024)
            while (true) {
                val count = input.read(buffer)
                if (count < 0) break
                digest.update(buffer, 0, count)
            }
        }
        return digest.digest().joinToString("") { "%02x".format(it) }
    }

    private fun notifyReady(version: String) {
        val manager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= 26) {
            manager.createNotificationChannel(
                NotificationChannel(CHANNEL_ID, "تحديثات UNBLOCK", NotificationManager.IMPORTANCE_HIGH)
            )
        }
        val openApp = Intent(applicationContext, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("open_update_settings", true)
        }
        val pending = android.app.PendingIntent.getActivity(
            applicationContext, 7302, openApp,
            android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE
        )
        val notification = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_sys_download_done)
            .setContentTitle("تحديث جديد جاهز - UNBLOCK")
            .setContentText("الإصدار $version جاهز. اضغط لفتح التطبيق وتثبيته.")
            .setStyle(NotificationCompat.BigTextStyle().bigText("تم تنزيل الإصدار $version والتحقق من سلامته. افتح التطبيق واضغط تثبيت التحديث لإكمال التثبيت."))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pending)
            .build()
        try {
            NotificationManagerCompat.from(applicationContext).notify(NOTIFICATION_ID, notification)
        } catch (_: SecurityException) {
            // Notifications may be denied; the update remains available in Settings.
        }
    }

    companion object {
        private const val RELEASE_API = "https://api.github.com/repos/atemmokhtar2-blip/Unblock-application-/releases/latest"
        private const val APK_ASSET = "unblock-release.apk"
        private const val CHECKSUM_ASSET = "unblock-release.apk.sha256"
        private const val CHANNEL_ID = "unblock_updates"
        private const val NOTIFICATION_ID = 7303
    }
}

object UpdateInstaller {
    fun install(context: Context): Boolean {
        val apk = UpdateManager.apkFile(context) ?: return false
        if (Build.VERSION.SDK_INT >= 26 && !context.packageManager.canRequestPackageInstalls()) {
            val settingsIntent = Intent(
                Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                Uri.parse("package:${context.packageName}")
            ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(settingsIntent)
            return false
        }
        val uri = FileProvider.getUriForFile(
            context, "${context.packageName}.fileprovider", apk
        )
        val installIntent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/vnd.android.package-archive")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(installIntent)
        return true
    }
}
