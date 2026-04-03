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
    TOO_MUDDY("too_muddy", "Muddy", "\uD83C\uDF2B\uFE0F"),
    PREFER_WARMER("prefer_warmer", "Warmer", "\uD83D\uDD25"),
    PREFER_CLEARER("prefer_clearer", "Clearer", "\uD83D\uDC8E"),
    MORE_BASS("more_bass", "More Bass", "\uD83E\uDD41"),
    MORE_VOCAL("more_vocal", "More Vocal", "\uD83C\uDFA4"),
    PERFECT("perfect", "Perfect!", "✅"),
    WRONG_GENRE("wrong_genre", "Wrong Genre", "❌");

    companion object {
        val quickOptions = listOf(PERFECT, TOO_BASSY, TOO_BRIGHT)
        val allOptions = entries.toList()
    }
}
