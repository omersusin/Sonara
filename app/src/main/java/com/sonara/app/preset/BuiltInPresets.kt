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

package com.sonara.app.preset

object BuiltInPresets {
    val ALL: List<Preset> = listOf(
        // ── Neutral ──
        p("Flat", floatArrayOf(0f,0f,0f,0f,0f,0f,0f,0f,0f,0f), "neutral"),
        // ── Bass ──
        p("Bass Boost", floatArrayOf(5f,4.5f,3.5f,2f,0.5f,0f,0f,0f,0f,0f), "bass"),
        p("Bass Heavy", floatArrayOf(7f,6f,5f,3f,1f,0f,-1f,-1f,0f,0f), "bass"),
        p("Deep Bass", floatArrayOf(10f,0f,-9.4f,-9f,-3.5f,-6.1f,-1.5f,-5f,0.6f,3f), "bass"),
        p("Sub Bass", floatArrayOf(9.4f,8.5f,4.5f,1.5f,0f,0f,0f,0f,0f,0f), "bass"),
        // ── Treble ──
        p("Treble Boost", floatArrayOf(0f,0f,0f,0f,0.5f,1.5f,3f,4f,5f,5.5f), "treble"),
        p("Clarity", floatArrayOf(4.5f,6.5f,8.8f,6.5f,3f,1.3f,6f,9f,10.5f,9f), "treble"),
        // ── Shape ──
        p("V-Shape", floatArrayOf(5f,4f,2f,0f,-2f,-2f,0f,2f,4f,5f), "fun"),
        p("Loudness", floatArrayOf(4f,3f,1f,0f,0f,0f,0f,1f,3f,4f), "fun"),
        p("Volume Boost", floatArrayOf(-1.8f,-3f,-1.8f,1.5f,3.5f,3.5f,2.5f,1.5f,0f,-1.5f), "fun"),
        // ── Vocal ──
        p("Vocal", floatArrayOf(-1f,-0.5f,0f,2f,4f,4f,3f,1f,0f,-1f), "vocal"),
        p("Podcast", floatArrayOf(-2f,-1f,0f,3f,5f,5f,3f,1f,-1f,-2f), "vocal"),
        // ── Genre ──
        p("Rock", floatArrayOf(0f,0f,3f,-10f,-2.5f,0.8f,3f,3f,3f,3f), "genre"),
        p("Pop", floatArrayOf(0f,0f,0f,1.3f,2.3f,5f,-1.8f,-3f,-3f,-3f), "genre"),
        p("Hip-Hop", floatArrayOf(4.4f,4f,2f,3f,-1.3f,-1.5f,0.8f,-1f,0.8f,3f), "genre"),
        p("Jazz", floatArrayOf(0f,0f,3f,5.9f,-5.2f,-2.5f,1.8f,-0.8f,-0.8f,-0.8f), "genre"),
        p("Classical", floatArrayOf(0f,-3.5f,-5f,0f,2f,0f,0f,4.4f,9f,9f), "genre"),
        p("Electronic", floatArrayOf(4f,3.5f,0.5f,-0.5f,-1f,2f,0f,1f,3.5f,4.5f), "genre"),
        p("R&B", floatArrayOf(3f,7f,5.3f,1.5f,-1.8f,-1.5f,2.3f,3f,3.7f,4f), "genre"),
        p("Acoustic", floatArrayOf(4.8f,4f,2.5f,1f,1.5f,2f,3.3f,4f,3.4f,3f), "genre"),
        p("Metal", floatArrayOf(10.5f,7.5f,1f,5.5f,0f,0f,3.1f,0f,8.1f,12f), "genre"),
        p("Dubstep", floatArrayOf(11f,0.5f,-2f,-5f,-4.9f,-4.5f,-1.8f,0f,-2.5f,0f), "genre"),
        p("Hardstyle", floatArrayOf(6.6f,12f,0.6f,-12f,0.3f,6.5f,-1.1f,-4.5f,-7.7f,-10f), "genre"),
        // ── Mood ──
        p("Late Night", floatArrayOf(1f,1.5f,1f,0f,-1f,-2f,-1f,0f,0.5f,0.5f), "mood"),
        p("Morning", floatArrayOf(0f,0f,1f,2f,2.5f,2f,1.5f,2f,2.5f,2f), "mood"),
        p("Workout", floatArrayOf(5f,4f,2f,1f,0f,1f,2f,3f,3.5f,4f), "mood"),
        p("Chill", floatArrayOf(1f,1.5f,1f,0f,-0.5f,0f,0.5f,1f,1.5f,1f), "mood"),
        p("Cinema", floatArrayOf(3f,6.1f,8.8f,7f,6.1f,5f,5.8f,3.5f,9f,8f), "mood"),
        // ── Rhythm ──
        p("Rhythm Cut", floatArrayOf(-5.3f,-4.5f,-3.9f,-3f,-1f,0f,0f,0f,0f,0f), "fun"),
        // ── Device ──
        p("Small Speaker", floatArrayOf(6f,5f,4f,2f,1f,0f,0f,1f,2f,2f), "device"),
        p("Headphone", floatArrayOf(2f,1f,0f,0f,-0.5f,0f,0.5f,1f,2f,2.5f), "device"),
        p("Earbud Fix", floatArrayOf(3f,2.5f,1f,0f,-1f,0f,1f,0f,-1f,-2f), "device")
    )

    private fun p(name: String, bands: FloatArray, cat: String) = Preset(
        name = name,
        bands = Preset.fromArray(bands),
        isBuiltIn = true,
        category = cat
    )

    val CATEGORIES = mapOf(
        "neutral" to "Neutral",
        "bass" to "Bass",
        "treble" to "Treble",
        "vocal" to "Vocal",
        "genre" to "Genre",
        "mood" to "Mood",
        "fun" to "Fun",
        "device" to "Device",
        "custom" to "Custom"
    )
}
