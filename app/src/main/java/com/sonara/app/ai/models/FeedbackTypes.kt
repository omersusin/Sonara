package com.sonara.app.ai.models

enum class FeedbackType(
    val id: String,
    val label: String,
    val emoji: String
) {
    TOO_BASSY("too_bassy", "Too Bassy", "\uD83D\uDD0A"),
    TOO_BRIGHT("too_bright", "Too Bright", "✨"),
    TOO_THIN("too_thin", "Too Thin", "\uD83D\uDD07"),
    TOO_HARSH("too_harsh", "Too Harsh", "⚡"),
    TOO_FLAT("too_flat", "Too Flat", "➖"),
    PREFER_WARMER("prefer_warmer", "Warmer", "\uD83D\uDD25"),
    PREFER_CLEARER("prefer_clearer", "Clearer", "\uD83D\uDC8E"),
    PERFECT("perfect", "Perfect!", "✅");

    companion object {
        val quickOptions = listOf(TOO_BASSY, TOO_BRIGHT, TOO_THIN, PREFER_WARMER, PERFECT)
        val allOptions = values().toList()
    }
}
