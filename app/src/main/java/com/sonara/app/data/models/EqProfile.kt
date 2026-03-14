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
        return bands.contentEquals(other.bands) && preamp == other.preamp
    }
    override fun hashCode() = bands.contentHashCode()
}
