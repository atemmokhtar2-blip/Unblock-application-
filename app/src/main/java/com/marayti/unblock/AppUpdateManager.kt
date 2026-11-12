package com.marayti.unblock

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
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
import java.io.RandomAccessFile
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
    val downloaded: Boolean,
    val checking: Boolean,
    val progress: Int
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
        val connectivity = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val activeNetwork = connectivity.activeNetwork
        val capabilities = activeNetwork?.let { connectivity.getNetworkCapabilities(it) }
        val hasInternet = capabilities?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true &&
            capabilities?.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED) == true
        if (hasInternet) {
            UpdateManager.saveChecking(context)
        } else {
            UpdateManager.saveWaitingForNetwork(context)
        }
        val request = OneTimeWorkRequestBuilder<AppUpdateWorker>()
            .setConstraints(
                Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build()
            )
            .build()
        // A manual check must always start a fresh worker. KEEP can silently
        // preserve a stale/failed request and make the Settings button appear broken.
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
    private const val KEY_CHECKING = "checking"
    private const val KEY_PROGRESS = "progress"

    internal fun preferences(context: Context) =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun readStatus(context: Context): UpdateStatus {
        val prefs = preferences(context)
        val path = prefs.getString(KEY_APK_PATH, null)
        val downloaded = !path.isNullOrBlank() && File(path).isFile
        val latestCode = prefs.getInt(KEY_LATEST_CODE, 0)
        val latestVersion = prefs.getString(KEY_LATEST_VERSION, null)
        val checking = prefs.getBoolean(KEY_CHECKING, false)
        val progress = prefs.getInt(KEY_PROGRESS, 0)
        val message = when {
            checking -> prefs.getString(KEY_MESSAGE, "جارٍ فحص التحديثات...") ?: "جارٍ فحص التحديثات..."
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
            downloaded,
            checking,
            progress
        )
    }

    internal fun hasDownloadedVersion(context: Context, versionCode: Int): Boolean {
        val prefs = preferences(context)
        val path = prefs.getString(KEY_APK_PATH, null) ?: return false
        return prefs.getInt(KEY_LATEST_CODE, 0) == versionCode && File(path).isFile && File(path).length() > 0L
    }

    internal fun saveWaitingForNetwork(context: Context) {
        preferences(context).edit()
            .putBoolean(KEY_CHECKING, false)
            .putInt(KEY_PROGRESS, 0)
            .putString(KEY_MESSAGE, "لا يوجد اتصال بالإنترنت. سيتم فحص التحديثات تلقائيًا عند عودة الاتصال.")
            .apply()
    }

    internal fun saveChecking(context: Context) {
        // Preserve the visible progress if a worker is already downloading.
        preferences(context).edit()
            .putBoolean(KEY_CHECKING, true)
            .putString(KEY_MESSAGE, "جارٍ الاتصال بخادم التحديث...")
            .apply()
    }

    internal fun resetProgress(context: Context, message: String) {
        preferences(context).edit()
            .putBoolean(KEY_CHECKING, true)
            .putInt(KEY_PROGRESS, 0)
            .putString(KEY_MESSAGE, message)
            .apply()
    }

    internal fun saveProgress(context: Context, message: String, progress: Int) {
        val prefs = preferences(context)
        val requested = progress.coerceIn(0, 100)
        val previous = prefs.getInt(KEY_PROGRESS, 0)
        prefs.edit()
            .putBoolean(KEY_CHECKING, true)
            .putInt(KEY_PROGRESS, maxOf(previous, requested))
            .putString(KEY_MESSAGE, message)
            .apply()
    }

    internal fun saveNoUpdate(context: Context, latestVersion: String, latestCode: Int) {
        preferences(context).edit()
            .putBoolean(KEY_CHECKING, false)
            .putInt(KEY_PROGRESS, 100)
            .putString(KEY_LATEST_VERSION, latestVersion)
            .putInt(KEY_LATEST_CODE, latestCode)
            .putString(KEY_MESSAGE, "أنت تستخدم أحدث إصدار.")
            .remove(KEY_APK_PATH)
            .apply()
    }

    internal fun saveAvailable(context: Context, version: String, code: Int, apk: File) {
        preferences(context).edit()
            .putBoolean(KEY_CHECKING, false)
            .putInt(KEY_PROGRESS, 100)
            .putString(KEY_LATEST_VERSION, version)
            .putInt(KEY_LATEST_CODE, code)
            .putString(KEY_MESSAGE, "تم تنزيل التحديث والتحقق من سلامته. أصبح جاهزًا للتثبيت.")
            .putString(KEY_APK_PATH, apk.absolutePath)
            .apply()
    }

    internal fun saveFailure(context: Context, message: String) {
        preferences(context).edit()
            .putBoolean(KEY_CHECKING, false)
            .putInt(KEY_PROGRESS, 0)
            .putString(KEY_MESSAGE, message)
            .apply()
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
        val lockDirectory = File(applicationContext.filesDir, "updates").apply { mkdirs() }
        val lockFile = File(lockDirectory, "update-worker.lock")
        RandomAccessFile(lockFile, "rw").channel.use { channel ->
            channel.lock().use { performUpdate() }
        }
    }

    private fun performUpdate(): Result {
        try {
            UpdateManager.resetProgress(applicationContext, "جارٍ الاتصال بخادم التحديث...")
            val manifest = getJson(UPDATE_MANIFEST_URL)
            UpdateManager.saveProgress(applicationContext, "تم الاتصال. جارٍ فحص رقم الإصدار...", 0)
            val versionName = manifest.getString("versionName")
            val versionCode = manifest.getInt("versionCode")
            val lastKnownCode = UpdateManager.preferences(applicationContext).getInt("latest_code", 0)
            if (versionCode <= BuildConfig.VERSION_CODE) {
                UpdateManager.saveNoUpdate(applicationContext, versionName, versionCode)
                return Result.success()
            }
            if (versionCode < lastKnownCode) {
                throw SecurityException("رفض إصدار أقدم من آخر إصدار موثوق.")
            }
            if (UpdateManager.hasDownloadedVersion(applicationContext, versionCode)) {
                UpdateManager.apkFile(applicationContext)?.let { existingApk ->
                    UpdateManager.saveAvailable(applicationContext, versionName, versionCode, existingApk)
                }
                return Result.success()
            }

            val apk = manifest.optString("apkUrl")
                .takeIf { it.startsWith("https://") }
                ?: throw IllegalStateException("رابط ملف APK غير موجود في ملف التحديث.")
            val expectedHashFromManifest = manifest.optString("sha256")
                .takeIf { it.matches(Regex("^[a-fA-F0-9]{64}$")) }
            val checksum = manifest.optString("checksumUrl").takeIf { it.startsWith("https://") }
            UpdateManager.saveProgress(applicationContext, "يوجد تحديث جديد. جارٍ تنزيل ملف التطبيق...", 0)
            val targetDir = File(applicationContext.filesDir, "updates").apply { mkdirs() }
            val temporary = File(targetDir, "update-$versionCode.apk.part")
            val target = File(targetDir, "update-$versionCode.apk")
            temporary.delete()
            UpdateManager.preferences(applicationContext).edit().remove("apk_path").apply()
            download(apk, temporary) { percent ->
                UpdateManager.saveProgress(applicationContext, "جارٍ تنزيل التحديث... $percent%", percent)
            }
            UpdateManager.saveProgress(applicationContext, "اكتمل التنزيل. جارٍ التحقق من سلامة الملف...", 100)
            val expectedHash = (expectedHashFromManifest ?: checksum?.let { readChecksum(it) }
                ?.trim()?.split(Regex("\\s+"))?.firstOrNull())
                ?.lowercase(Locale.US)
                ?: throw SecurityException("بصمة SHA-256 غير موجودة أو غير صالحة في بيانات التحديث.")
            val actualHash = sha256(temporary)
            if (expectedHash != actualHash) {
                temporary.delete()
                throw SecurityException("فشل التحقق من بصمة التحديث؛ تم رفض الملف.")
            }
            val archiveInfo = if (Build.VERSION.SDK_INT >= 33) {
                applicationContext.packageManager.getPackageArchiveInfo(
                    temporary.absolutePath, PackageManager.PackageInfoFlags.of(0)
                )
            } else {
                @Suppress("DEPRECATION")
                applicationContext.packageManager.getPackageArchiveInfo(temporary.absolutePath, 0)
            } ?: throw SecurityException("ملف التحديث ليس تطبيق Android صالحًا.")
            val archiveVersionCode = if (Build.VERSION.SDK_INT >= 28) archiveInfo.longVersionCode else {
                @Suppress("DEPRECATION")
                archiveInfo.versionCode.toLong()
            }
            if (archiveInfo.packageName != applicationContext.packageName ||
                archiveVersionCode != versionCode.toLong()) {
                throw SecurityException("بيانات ملف APK لا تطابق الإصدار المعلن؛ تم رفض الملف.")
            }
            if (target.exists()) target.delete()
            if (!temporary.renameTo(target)) throw IllegalStateException("تعذر حفظ ملف التحديث.")
            UpdateManager.saveAvailable(applicationContext, versionName, versionCode, target)
            notifyReady(versionName)
            Result.success()
        } catch (error: Exception) {
            UpdateManager.saveFailure(
                applicationContext,
                "فشل التحديث: ${error.javaClass.simpleName} — ${error.message ?: "تحقق من اتصال الإنترنت وحاول مرة أخرى."}"
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
            setRequestProperty("Cache-Control", "no-cache")
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

    private fun download(address: String, destination: File, onProgress: (Int) -> Unit) {
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
                    val expectedLength = connection.contentLengthLong
                    var lastReported = -1
                    while (true) {
                        val count = input.read(buffer)
                        if (count < 0) break
                        total += count
                        if (total > 200L * 1024L * 1024L) {
                            throw IllegalStateException("حجم ملف التحديث أكبر من الحد المسموح.")
                        }
                        output.write(buffer, 0, count)
                        if (expectedLength > 0L) {
                            val percent = ((total * 100L) / expectedLength).toInt().coerceIn(0, 100)
                            if (percent != lastReported) {
                                lastReported = percent
                                onProgress(percent)
                            }
                        }
                    }
                }
            }
            if (expectedLength > 0L && total != expectedLength) {
                destination.delete()
                throw IllegalStateException("اكتمل الاتصال لكن حجم الملف لا يطابق الحجم المعلن؛ أُعيد رفض التنزيل.")
            }
            onProgress(100)
            if (destination.length() < 1_000_000L) {
                destination.delete()
                throw IllegalStateException("ملف التحديث غير مكتمل.")
            }
        } finally {
            connection.disconnect()
        }
    }

    private fun readChecksum(address: String): String {
        val connection = (URL(address).openConnection() as HttpURLConnection).apply {
            connectTimeout = 15000
            readTimeout = 20000
            instanceFollowRedirects = true
            setRequestProperty("User-Agent", "UNBLOCK-Android-Updater")
        }
        return try {
            if (connection.responseCode !in 200..299) {
                throw IllegalStateException("تعذر تنزيل ملف التحقق: HTTP ${connection.responseCode}")
            }
            connection.inputStream.bufferedReader().use { it.readText() }
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
        private const val UPDATE_MANIFEST_URL = "https://raw.githubusercontent.com/atemmokhtar2-blip/Unblock-application-/main/update.json"
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
