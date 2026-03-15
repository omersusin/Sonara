package com.sonara.app.data.models

data class EqProfile(
    val bands: FloatArray = FloatArray(10),
    val preamp: Float = 0f,
    val bassBoost: Int = 0,
    val virtualizer: Int = 0,
    val loudness: Int = 0
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is EqProfile) return false
        return bands.contentEquals(other.bands) && preamp == other.preamp &&
            bassBoost == other.bassBoost && virtualizer == other.virtualizer &&
            loudness == other.loudness
    }
    override fun hashCode(): Int {
        var result = bands.contentHashCode()
        result = 31 * result + preamp.hashCode()
        result = 31 * result + bassBoost
        result = 31 * result + virtualizer
        result = 31 * result + loudness
        return result
    }
}
