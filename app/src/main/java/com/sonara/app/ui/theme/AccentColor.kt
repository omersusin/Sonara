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

package com.sonara.app.ui.theme
import androidx.compose.ui.graphics.Color
enum class AccentColor(val displayName: String, val primary: Color, val primaryLight: Color, val primaryDark: Color) {
    Auto("Auto", Color(0xFF0061A4), Color(0xFFD1E4FF), Color(0xFF003258)),
    Amber("Amber", Color(0xFFFFB900), Color(0xFFFFE082), Color(0xFFC78D00)),
    Rose("Rose", Color(0xFFFF5252), Color(0xFFFF8A80), Color(0xFFD32F2F)),
    Ocean("Ocean", Color(0xFF00B0FF), Color(0xFF80D8FF), Color(0xFF0091EA)),
    Sage("Sage", Color(0xFF00E676), Color(0xFFB9F6CA), Color(0xFF00C853)),
    Lavender("Lavender", Color(0xFF7C4DFF), Color(0xFFB388FF), Color(0xFF651FFF)),
    Coral("Coral", Color(0xFFFF6E40), Color(0xFFFF9E80), Color(0xFFFF3D00)),
    Ice("Ice", Color(0xFF18FFFF), Color(0xFF84FFFF), Color(0xFF00E5FF)),
    Pearl("Pearl", Color(0xFFECEFF1), Color(0xFFFFFFFF), Color(0xFFB0BEC5))
}
