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

package com.sonara.app.ui.screens.onboarding

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.MusicOff
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.sonara.app.ui.theme.*

@Composable
fun HearTheDifferenceScreen(isPlaying: Boolean = false, onContinue: () -> Unit, onSetEqEnabled: ((Boolean) -> Unit)? = null) {
    var isOriginalMode by remember { mutableStateOf(false) }
    val haptic = LocalHapticFeedback.current
    val p = MaterialTheme.colorScheme.primary
    val origColor by animateColorAsState(if (isOriginalMode) p.copy(alpha = 0.25f) else SonaraCard, tween(300), label = "orig")
    val enhColor by animateColorAsState(if (!isOriginalMode) p.copy(alpha = 0.25f) else SonaraCard, tween(300), label = "enh")

    LaunchedEffect(isOriginalMode) { onSetEqEnabled?.invoke(!isOriginalMode) }

    Column(Modifier.fillMaxSize().background(SonaraBackground).padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Icon(Icons.Rounded.GraphicEq, null, Modifier.size(64.dp), tint = p)
        Spacer(Modifier.height(20.dp))
        Text("Hear the Difference", style = MaterialTheme.typography.headlineMedium, textAlign = TextAlign.Center, color = SonaraTextPrimary)
        Spacer(Modifier.height(8.dp))
        Text(if (isPlaying) "Hold the button to hear original sound" else "Play a song to compare",
            style = MaterialTheme.typography.bodyLarge, textAlign = TextAlign.Center, color = SonaraTextSecondary)
        Spacer(Modifier.height(36.dp))

        if (isPlaying) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Card(Modifier.weight(1f).height(130.dp), colors = CardDefaults.cardColors(containerColor = origColor), shape = RoundedCornerShape(20.dp)) {
                    Column(Modifier.fillMaxSize().padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                        Icon(Icons.Rounded.MusicOff, null, Modifier.size(36.dp), tint = if (isOriginalMode) p else SonaraTextTertiary)
                        Spacer(Modifier.height(10.dp))
                        Text("Original", style = MaterialTheme.typography.titleMedium, color = if (isOriginalMode) SonaraTextPrimary else SonaraTextTertiary)
                        if (isOriginalMode) { Spacer(Modifier.height(4.dp)); Text("No EQ", style = MaterialTheme.typography.bodySmall, color = SonaraTextSecondary) }
                    }
                }
                Card(Modifier.weight(1f).height(130.dp), colors = CardDefaults.cardColors(containerColor = enhColor), shape = RoundedCornerShape(20.dp)) {
                    Column(Modifier.fillMaxSize().padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                        Icon(Icons.Rounded.MusicNote, null, Modifier.size(36.dp), tint = if (!isOriginalMode) p else SonaraTextTertiary)
                        Spacer(Modifier.height(10.dp))
                        Text("Enhanced", style = MaterialTheme.typography.titleMedium, color = if (!isOriginalMode) SonaraTextPrimary else SonaraTextTertiary)
                        if (!isOriginalMode) { Spacer(Modifier.height(4.dp)); Text("Sonara EQ", style = MaterialTheme.typography.bodySmall, color = SonaraTextSecondary) }
                    }
                }
            }
            Spacer(Modifier.height(28.dp))
            Box(Modifier.fillMaxWidth().height(60.dp).clip(RoundedCornerShape(30.dp))
                .background(if (isOriginalMode) SonaraWarning.copy(0.25f) else p.copy(0.12f))
                .pointerInput(Unit) { detectTapGestures(onPress = {
                    isOriginalMode = true; haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    try { awaitRelease() } finally { isOriginalMode = false; haptic.performHapticFeedback(HapticFeedbackType.LongPress) }
                }) }, contentAlignment = Alignment.Center) {
                Text(if (isOriginalMode) "Original — Release for Enhanced" else "Hold to hear Original sound",
                    style = MaterialTheme.typography.titleSmall, color = SonaraTextPrimary)
            }
        } else {
            Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = SonaraCard), shape = RoundedCornerShape(20.dp)) {
                Column(Modifier.padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Rounded.PlayArrow, null, Modifier.size(48.dp), tint = SonaraTextTertiary)
                    Spacer(Modifier.height(12.dp))
                    Text("Start playing a song\nto compare the difference", style = MaterialTheme.typography.bodyLarge, color = SonaraTextSecondary, textAlign = TextAlign.Center)
                }
            }
        }

        Spacer(Modifier.height(32.dp))
        FilledTonalButton(onClick = { onSetEqEnabled?.invoke(true); onContinue() }, Modifier.fillMaxWidth().height(50.dp),
            shape = RoundedCornerShape(25.dp), colors = ButtonDefaults.filledTonalButtonColors(containerColor = p, contentColor = SonaraBackground)) { Text("Get Started") }
    }
}
