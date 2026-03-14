package com.sonara.app.autoeq

data class HeadphoneProfile(
    val name: String,
    val correctionBands: FloatArray = FloatArray(10),
    val matchConfidence: Float = 0f,
    val source: String = "built-in"
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is HeadphoneProfile) return false
        return name == other.name && correctionBands.contentEquals(other.correctionBands)
    }
    override fun hashCode() = name.hashCode()
}
