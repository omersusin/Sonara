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

package com.sonara.app.engine.eq

import android.media.audiofx.Equalizer
import android.util.Log

class SafeEqualizer private constructor(private val equalizer: Equalizer) {
    companion object {
        private const val TAG = "SafeEq"
        fun create(priority: Int, sessionId: Int): SafeEqualizer? {
            return try { val eq = Equalizer(priority, sessionId); eq.enabled = true; SafeEqualizer(eq) }
            catch (e: Exception) { Log.e(TAG, "Create($sessionId): ${e.message}"); null }
        }
    }

    val bandCount: Int get() = try { equalizer.numberOfBands.toInt() } catch (_: Exception) { 0 }

    fun setBands(bandsMb: IntArray) {
        val hw = bandCount; if (hw == 0) return
        val mapped = if (bandsMb.size == hw) bandsMb else interpolate(bandsMb, hw)
        val range = try { equalizer.bandLevelRange } catch (_: Exception) { shortArrayOf(-1500, 1500) }
        for (i in 0 until hw) { try { equalizer.setBandLevel(i.toShort(), mapped[i].toShort().coerceIn(range[0], range[1])) } catch (_: Exception) {} }
    }

    fun setEnabled(enabled: Boolean) { try { equalizer.enabled = enabled } catch (_: Exception) {} }
    fun release() { try { equalizer.enabled = false; equalizer.release() } catch (_: Exception) {} }

    private fun interpolate(src: IntArray, tgt: Int): IntArray {
        if (src.isEmpty()) return IntArray(tgt)
        return IntArray(tgt) { i -> val p = i.toFloat() * (src.size - 1) / (tgt - 1).coerceAtLeast(1); val lo = p.toInt().coerceIn(0, src.size - 1); val hi = (lo + 1).coerceIn(0, src.size - 1); val f = p - lo; (src[lo] * (1 - f) + src[hi] * f).toInt() }
    }
}
