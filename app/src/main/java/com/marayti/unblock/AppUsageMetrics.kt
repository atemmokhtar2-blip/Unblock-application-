package com.marayti.unblock

import android.content.Context
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest
import java.util.UUID
import java.util.concurrent.Executors

/**
 * Sends only a SHA-256 hash of a random per-install UUID.
 * No Android ID, hardware identifier, phone number, account, or device details are collected.
 */
internal object AppUsageMetrics {
    private const val ENDPOINT = "https://wa-unban-bot.vercel.app/app/metrics"
    private const val PREFS = "unblock_usage_metrics"
    private const val INSTALL_ID_KEY = "installation_uuid"
    private val executor = Executors.newSingleThreadExecutor()

    private fun installationToken(context: Context): String {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val installId = prefs.getString(INSTALL_ID_KEY, null) ?: UUID.randomUUID().toString().also {
            prefs.edit().putString(INSTALL_ID_KEY, it).apply()
        }
        val digest = MessageDigest.getInstance("SHA-256")
            .digest(installId.toByteArray(Charsets.UTF_8))
        return digest.joinToString("") { "%02x".format(it) }
    }

    fun recordOpen(context: Context) {
        val token = try {
            installationToken(context.applicationContext)
        } catch (_: Exception) {
            return
        }
        executor.execute {
            var connection: HttpURLConnection? = null
            try {
                connection = (URL(ENDPOINT).openConnection() as HttpURLConnection).apply {
                    requestMethod = "POST"
                    connectTimeout = 5000
                    readTimeout = 5000
                    doOutput = true
                    setRequestProperty("Content-Type", "application/json; charset=utf-8")
                    setRequestProperty("Accept", "application/json")
                }
                val payload = """{"event":"app_open","install_id":"$token"}"""
                connection.outputStream.use { output ->
                    output.write(payload.toByteArray(Charsets.UTF_8))
                }
                val stream = if (connection.responseCode in 200..299) connection.inputStream else connection.errorStream
                stream?.use { it.readBytes() }
            } catch (_: Exception) {
                // Analytics are optional and must never affect the app's normal behavior.
            } finally {
                connection?.disconnect()
            }
        }
    }
}
