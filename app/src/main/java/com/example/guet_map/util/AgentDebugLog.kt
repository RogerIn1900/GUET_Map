package com.example.guet_map.util

import android.content.Context
import android.util.Log
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.Executors

object AgentDebugLog {
    private const val TAG = "AgentDebug"
    private const val SESSION = "4f6b92"
    private const val INGEST = "http://10.0.2.2:7803/ingest/b84c0b04-84ba-4a94-bc14-562094bf1fd3"
    private const val LOG_FILE = "debug-4f6b92.log"
    private val executor = Executors.newSingleThreadExecutor()

    @Volatile
    var appContext: Context? = null

    fun log(
        hypothesisId: String,
        location: String,
        message: String,
        data: Map<String, Any?> = emptyMap(),
        runId: String = "pre-fix"
    ) {
        val payload = JSONObject().apply {
            put("sessionId", SESSION)
            put("hypothesisId", hypothesisId)
            put("location", location)
            put("message", message)
            put("timestamp", System.currentTimeMillis())
            put("runId", runId)
            put("data", JSONObject(data))
        }
        Log.d(TAG, "[$runId] $hypothesisId $location $message $data")
        executor.execute {
            appContext?.let { ctx ->
                try {
                    File(ctx.filesDir, LOG_FILE).appendText(payload.toString() + "\n")
                } catch (_: Exception) {
                }
            }
            try {
                val conn = (URL(INGEST).openConnection() as HttpURLConnection).apply {
                    requestMethod = "POST"
                    setRequestProperty("Content-Type", "application/json")
                    setRequestProperty("X-Debug-Session-Id", SESSION)
                    doOutput = true
                    connectTimeout = 2000
                    readTimeout = 2000
                }
                conn.outputStream.use { it.write(payload.toString().toByteArray()) }
                conn.responseCode
                conn.disconnect()
            } catch (_: Exception) {
                // emulator/host ingest may be unavailable
            }
        }
    }
}
