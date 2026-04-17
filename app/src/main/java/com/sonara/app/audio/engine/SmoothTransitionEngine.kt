/*
 * Sonara - AI-powered audio equalizer
 * Copyright (C) 2024-2026 Sonara
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

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

    private fun smoothStep(t: Float): Float = t * t * (3f - 2f * t)
}
