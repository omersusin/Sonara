package com.sonara.app.ui.theme

import com.materialkolor.PaletteStyle

enum class SonaraPaletteStyle(val displayName: String) {
    EXPRESSIVE("Expressive"),
    TONAL_SPOT("Tonal Spot"),
    VIBRANT("Vibrant"),
    RAINBOW("Rainbow"),
    FRUIT_SALAD("Fruit Salad"),
    NEUTRAL("Neutral"),
    MONOCHROME("Monochrome"),
    FIDELITY("Fidelity"),
    CONTENT("Content");

    fun toMaterialKolor(): PaletteStyle = when (this) {
        EXPRESSIVE  -> PaletteStyle.Expressive
        TONAL_SPOT  -> PaletteStyle.TonalSpot
        VIBRANT     -> PaletteStyle.Vibrant
        RAINBOW     -> PaletteStyle.Rainbow
        FRUIT_SALAD -> PaletteStyle.FruitSalad
        NEUTRAL     -> PaletteStyle.Neutral
        MONOCHROME  -> PaletteStyle.Monochrome
        FIDELITY    -> PaletteStyle.Fidelity
        CONTENT     -> PaletteStyle.Content
    }

    companion object {
        fun fromId(id: String) = entries.find { it.name == id } ?: EXPRESSIVE
    }
}
