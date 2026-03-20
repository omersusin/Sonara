package com.sonara.app.engine.audio

data class AudioFeatureVector(
    val spectralCentroid: Float = 0f,
    val spectralRolloff: Float = 0f,
    val spectralFlux: Float = 0f,
    val spectralFlatness: Float = 0f,
    val subBassEnergy: Float = 0f,
    val bassEnergy: Float = 0f,
    val lowMidEnergy: Float = 0f,
    val midEnergy: Float = 0f,
    val highMidEnergy: Float = 0f,
    val presenceEnergy: Float = 0f,
    val brillianceEnergy: Float = 0f,
    val rmsEnergy: Float = 0f,
    val zeroCrossingRate: Float = 0f,
    val peakAmplitude: Float = 0f,
    val dynamicRange: Float = 0f,
    val tempo: Float = 0f,
    val sampleCount: Int = 0,
    val durationMs: Long = 0,
    val isValid: Boolean = false
) {
    fun toFloatArray(): FloatArray = floatArrayOf(
        spectralCentroid / 8000f, spectralRolloff / 12000f, spectralFlux, spectralFlatness,
        subBassEnergy, bassEnergy, lowMidEnergy, midEnergy, highMidEnergy, presenceEnergy, brillianceEnergy,
        rmsEnergy, zeroCrossingRate, dynamicRange / 60f, (tempo / 200f).coerceIn(0f, 1f)
    )
    companion object { const val FEATURE_SIZE = 15; fun empty() = AudioFeatureVector() }
}
