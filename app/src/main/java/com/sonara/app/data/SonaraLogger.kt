package com.sonara.app.data

import android.content.Context
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.io.File
import java.io.FileWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class LogLevel(val tag: String, val emoji: String) {
    DEBUG("DEBUG", "D"), INFO("INFO", "I"), WARN("WARN", "W"), ERROR("ERROR", "E"),
    EQ("EQ", "EQ"), AI("AI", "AI"), BT("BT", "BT"), MEDIA("MEDIA", "M"), NET("NET", "N"), UI("UI", "U")
}

data class LogEntry(
    val timestamp: Long = System.currentTimeMillis(),
    val level: LogLevel, val tag: String, val message: String
) {
    private val fmt = SimpleDateFormat("HH:mm:ss.SSS", Locale.US)
    val timeString: String get() = fmt.format(Date(timestamp))
    val displayText: String get() = "${level.emoji} $timeString [$tag] $message"
}

object SonaraLogger {
    private const val MAX_MEMORY_ENTRIES = 5000
    private const val MAX_FILE_SIZE = 10 * 1024 * 1024L // 10 MB
    private const val LOG_FILE = "sonara_log.txt"
    private const val LOG_FILE_OLD = "sonara_log_old.txt"

    private val _logs = MutableStateFlow<List<LogEntry>>(emptyList())
    val logs: StateFlow<List<LogEntry>> = _logs.asStateFlow()
    private val _isEnabled = MutableStateFlow(true)
    val isEnabled: StateFlow<Boolean> = _isEnabled.asStateFlow()

    private var logFile: File? = null
    private var fileWriter: FileWriter? = null
    private val fileFmt = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.US)

    fun init(context: Context) {
        try {
            val dir = context.filesDir
            logFile = File(dir, LOG_FILE)
            // Rotate if too big
            if (logFile!!.exists() && logFile!!.length() > MAX_FILE_SIZE) {
                val old = File(dir, LOG_FILE_OLD)
                if (old.exists()) old.delete()
                logFile!!.renameTo(old)
                logFile = File(dir, LOG_FILE)
            }
            fileWriter = FileWriter(logFile, true) // append mode
            fileWriter?.write("\n═══ Session ${fileFmt.format(Date())} ═══\n")
            fileWriter?.flush()
        } catch (e: Exception) {
            Log.e("SonaraLogger", "File init failed: ${e.message}")
        }
    }

    fun setEnabled(enabled: Boolean) { _isEnabled.value = enabled }

    fun d(tag: String, msg: String) = log(LogLevel.DEBUG, tag, msg)
    fun i(tag: String, msg: String) = log(LogLevel.INFO, tag, msg)
    fun w(tag: String, msg: String) = log(LogLevel.WARN, tag, msg)
    fun e(tag: String, msg: String) = log(LogLevel.ERROR, tag, msg)
    fun eq(msg: String) = log(LogLevel.EQ, "AudioEngine", msg)
    fun ai(msg: String) = log(LogLevel.AI, "AI", msg)
    fun bt(msg: String) = log(LogLevel.BT, "Bluetooth", msg)
    fun media(msg: String) = log(LogLevel.MEDIA, "Media", msg)
    fun net(msg: String) = log(LogLevel.NET, "Network", msg)
    fun ui(msg: String) = log(LogLevel.UI, "UI", msg)

    private fun log(level: LogLevel, tag: String, msg: String) {
        if (!_isEnabled.value) return

        when (level) {
            LogLevel.ERROR -> Log.e("Sonara.$tag", msg)
            LogLevel.WARN -> Log.w("Sonara.$tag", msg)
            else -> Log.d("Sonara.$tag", msg)
        }

        val entry = LogEntry(level = level, tag = tag, message = msg)

        // Memory buffer (5000 entries)
        _logs.update { current ->
            val updated = current + entry
            if (updated.size > MAX_MEMORY_ENTRIES) updated.drop(updated.size - MAX_MEMORY_ENTRIES) else updated
        }

        // File logging (survives app restart)
        try {
            fileWriter?.write("${fileFmt.format(Date(entry.timestamp))} [${level.tag}] [$tag] $msg\n")
            fileWriter?.flush()
        } catch (_: Exception) {}
    }

    fun exportAsText(): String {
        val header = buildString {
            appendLine("Sonara Debug Log")
            appendLine("Exported: ${fileFmt.format(Date())}")
            appendLine("Memory entries: ${_logs.value.size}")
            appendLine("Log file: ${logFile?.absolutePath ?: "none"}")
            appendLine("File size: ${logFile?.length()?.let { "${it / 1024} KB" } ?: "0"}")
            appendLine("---")
            appendLine()
        }
        return header + _logs.value.joinToString("\n") { it.displayText }
    }

    /** Export full file log (all sessions) */
    fun exportFileLog(): String {
        return try { logFile?.readText() ?: "No log file" } catch (_: Exception) { "Read error" }
    }

    fun clear() {
        _logs.update { emptyList() }
        try { logFile?.writeText(""); fileWriter = FileWriter(logFile, true) } catch (_: Exception) {}
    }

    fun getStats(): Map<LogLevel, Int> = _logs.value.groupBy { it.level }.mapValues { it.value.size }
    fun getFileSize(): Long = logFile?.length() ?: 0
    fun release() { try { fileWriter?.close() } catch (_: Exception) {} }
}
