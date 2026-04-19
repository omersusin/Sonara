package com.sonara.app.audio.engine

import kotlinx.coroutines.delay

class SmoothTransitionEngine {
    companion object {
        const val TRANSITION_STEPS = 20
        const val STEP_DELAY_MS = 50L
        const val TOTAL_DURATION_MS = TRANSITION_STEPS * STEP_DELAY_MS
    }

    suspend fun transition(
        from: FloatArray,
        to: FloatArray,
        onStep: (FloatArray) -> Unit
    ) {
        if (from.size != to.size) {
            onStep(to)
            return
        }
        for (step in 1..TRANSITION_STEPS) {
            val progress = step.toFloat() / TRANSITION_STEPS
            val smoothProgress = smoothStep(progress)
            val current = FloatArray(from.size) { i ->
                from[i] + (to[i] - from[i]) * smoothProgress
            }
            onStep(current)
            delay(STEP_DELAY_MS)
        }
    }

    suspend fun transitionFloat(from: Float, to: Float, onStep: (Float) -> Unit) {
        for (step in 1..TRANSITION_STEPS) {
            val progress = smoothStep(step.toFloat() / TRANSITION_STEPS)
            onStep(from + (to - from) * progress)
            delay(STEP_DELAY_MS)
        }
    }

    /**
     * Interpolates 10-band EQ and effect strengths (BassBoost / Virtualizer / Loudness)
     * in lock-step so a track change doesn't have a mismatched sonic jump between the
     * spectral curve and the effects that shape it.
     *
     * Reverb uses named presets (0-6) so it can't be interpolated — it snaps at the
     * midpoint of the transition to avoid a sudden cut at the very start.
     */
    suspend fun transitionFull(
        fromBands: FloatArray,
        toBands: FloatArray,
        fromBass: Int,
        toBass: Int,
        fromVirt: Int,
        toVirt: Int,
        fromLoud: Int,
        toLoud: Int,
        fromReverb: Int = 0,
        toReverb: Int = 0,
        onBandStep: (FloatArray) -> Unit,
        onEffectStep: (Int, Int, Int, Int) -> Unit
    ) {
        if (fromBands.size != toBands.size) {
            onBandStep(toBands)
            onEffectStep(toBass, toVirt, toLoud, toReverb)
            return
        }
        val midStep = TRANSITION_STEPS / 2
        var reverbApplied = fromReverb
        for (step in 1..TRANSITION_STEPS) {
            val progress = step.toFloat() / TRANSITION_STEPS
            val s = smoothStep(progress)
            val bands = FloatArray(fromBands.size) { i -> fromBands[i] + (toBands[i] - fromBands[i]) * s }
            // Snap reverb at midpoint so it doesn't cut in jarring at start/end
            if (step == midStep && toReverb != fromReverb) reverbApplied = toReverb
            onBandStep(bands)
            onEffectStep(
                (fromBass + (toBass - fromBass) * s).toInt(),
                (fromVirt + (toVirt - fromVirt) * s).toInt(),
                (fromLoud + (toLoud - fromLoud) * s).toInt(),
                reverbApplied
            )
            delay(STEP_DELAY_MS)
        }
    }

    private fun smoothStep(t: Float): Float = t * t * (3f - 2f * t)
}
