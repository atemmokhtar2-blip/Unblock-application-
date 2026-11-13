package com.marayti.unblock

import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.Executors

/** Sends only one aggregate app_open event. No identifiers or device fields are transmitted. */
internal object AppUsageMetrics {
    private const val ENDPOINT = "https://wa-unban-bot.vercel.app/app/metrics"
    private val executor = Executors.newSingleThreadExecutor()

    fun recordOpen() {
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
                connection.outputStream.use { output ->
                    output.write("""{"event":"app_open"}""".toByteArray(Charsets.UTF_8))
                }
                // Consume the small response so the connection can close cleanly.
                val stream = if (connection.responseCode in 200..299) connection.inputStream else connection.errorStream
                stream?.use { it.readBytes() }
            } catch (_: Exception) {
                // Usage metrics are optional and must never affect app functionality.
            } finally {
                connection?.disconnect()
            }
        }
    }
}
