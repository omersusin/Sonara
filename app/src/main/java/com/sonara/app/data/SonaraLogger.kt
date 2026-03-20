package com.sonara.app.data

import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class LogLevel(val tag: String, val emoji: String) {
    DEBUG("DEBUG", "🔍"),
    INFO("INFO", "ℹ️"),
    WARN("WARN", "⚠️"),
    ERROR("ERROR", "❌"),
    EQ("EQ", "🎛️"),
    AI("AI", "🧠"),
    BT("BT", "🎧"),
    MEDIA("MEDIA", "🎵"),
    NET("NET", "🌐"),
    UI("UI", "📱")
}

data class LogEntry(
    val timestamp: Long = System.currentTimeMillis(),
    val level: LogLevel,
    val tag: String,
    val message: String
) {
    private val fmt = SimpleDateFormat("HH:mm:ss.SSS", Locale.US)
    val timeString: String get() = fmt.format(Date(timestamp))
    val displayText: String get() = "${level.emoji} $timeString [$tag] $message"
}

/**
 * Sonara internal logger — captures ALL app events.
 * Viewable in Settings > Debug Log
 * Exportable as text for bug reports
 */
object SonaraLogger {
    private const val MAX_ENTRIES = 500
    private val _logs = MutableStateFlow<List<LogEntry>>(emptyList())
    val logs: StateFlow<List<LogEntry>> = _logs.asStateFlow()

    private val _isEnabled = MutableStateFlow(true)
    val isEnabled: StateFlow<Boolean> = _isEnabled.asStateFlow()

    fun setEnabled(enabled: Boolean) { _isEnabled.value = enabled }

    // ══════════════════════════════════════
    // Core log methods
    // ══════════════════════════════════════

    fun d(tag: String, msg: String) = log(LogLevel.DEBUG, tag, msg)
    fun i(tag: String, msg: String) = log(LogLevel.INFO, tag, msg)
    fun w(tag: String, msg: String) = log(LogLevel.WARN, tag, msg)
    fun e(tag: String, msg: String) = log(LogLevel.ERROR, tag, msg)

    // ══════════════════════════════════════
    // Domain-specific loggers
    // ══════════════════════════════════════

    fun eq(msg: String) = log(LogLevel.EQ, "AudioEngine", msg)
    fun ai(msg: String) = log(LogLevel.AI, "AI", msg)
    fun bt(msg: String) = log(LogLevel.BT, "Bluetooth", msg)
    fun media(msg: String) = log(LogLevel.MEDIA, "Media", msg)
    fun net(msg: String) = log(LogLevel.NET, "Network", msg)
    fun ui(msg: String) = log(LogLevel.UI, "UI", msg)

    private fun log(level: LogLevel, tag: String, msg: String) {
        if (!_isEnabled.value) return

        // Android logcat
        when (level) {
            LogLevel.ERROR -> Log.e("Sonara.$tag", msg)
            LogLevel.WARN -> Log.w("Sonara.$tag", msg)
            else -> Log.d("Sonara.$tag", msg)
        }

        // Internal buffer
        val entry = LogEntry(level = level, tag = tag, message = msg)
        _logs.update { current ->
            val updated = current + entry
            if (updated.size > MAX_ENTRIES) updated.drop(updated.size - MAX_ENTRIES) else updated
        }
    }

    // ══════════════════════════════════════
    // Export
    // ══════════════════════════════════════

    fun exportAsText(): String {
        val header = buildString {
            appendLine("═══ Sonara Debug Log ═══")
            appendLine("Exported: ${SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date())}")
            appendLine("Entries: ${_logs.value.size}")
            appendLine("═══════════════════════")
            appendLine()
        }
        return header + _logs.value.joinToString("\n") { it.displayText }
    }

    fun clear() { _logs.update { emptyList() } }

    // ══════════════════════════════════════
    // Stats
    // ══════════════════════════════════════

    fun getStats(): Map<LogLevel, Int> {
        return _logs.value.groupBy { it.level }.mapValues { it.value.size }
    }
}
