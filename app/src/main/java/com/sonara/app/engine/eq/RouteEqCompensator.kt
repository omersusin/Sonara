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

class RouteEqCompensator {
    companion object {
        private val SPEAKER = shortArrayOf(250, 100, 0, -50, -100)
        private val BT = shortArrayOf(150, 50, 50, 100, 50)
        private val WIRED = shortArrayOf(0, 0, 0, 0, 0)
    }
    fun apply(bands: ShortArray, route: EqSessionController.AudioRoute): ShortArray {
        val offset = when (route) { EqSessionController.AudioRoute.SPEAKER -> SPEAKER; EqSessionController.AudioRoute.BLUETOOTH -> BT; EqSessionController.AudioRoute.WIRED -> WIRED }
        return ShortArray(bands.size) { i -> (bands[i] + offset.getOrElse(i) { 0 }).toShort().coerceIn(-1500, 1500) }
    }
    fun neutralize(bands: ShortArray, sourceRoute: EqSessionController.AudioRoute): ShortArray {
        val offset = when (sourceRoute) { EqSessionController.AudioRoute.SPEAKER -> SPEAKER; EqSessionController.AudioRoute.BLUETOOTH -> BT; EqSessionController.AudioRoute.WIRED -> WIRED }
        return ShortArray(bands.size) { i -> (bands[i] - offset.getOrElse(i) { 0 }).toShort().coerceIn(-1500, 1500) }
    }
}
