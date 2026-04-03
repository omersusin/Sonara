package com.sonara.app.data.models

data class SharedEqState(
    val bands: FloatArray = FloatArray(10),
    val preamp: Float = 0f,
    val bassBoost: Int = 0,
    val virtualizer: Int = 0,
    val loudness: Int = 0,
    val presetName: String = "Flat",
    val isManualPreset: Boolean = false,
    val isEnabled: Boolean = true
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is SharedEqState) return false
        return bands.contentEquals(other.bands) && preamp == other.preamp &&
            bassBoost == other.bassBoost && virtualizer == other.virtualizer &&
            loudness == other.loudness && presetName == other.presetName &&
            isManualPreset == other.isManualPreset && isEnabled == other.isEnabled
    }
    override fun hashCode() = bands.contentHashCode()
}
