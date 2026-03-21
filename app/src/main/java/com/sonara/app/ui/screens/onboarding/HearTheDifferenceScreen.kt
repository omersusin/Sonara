package com.sonara.app.ui.screens.onboarding

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.MusicOff
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.sonara.app.SonaraApp
import com.sonara.app.ui.theme.*

/**
 * Madde 10: "Hear the Difference" onboarding ekranı.
 * Basılı tut = Original (EQ kapalı)
 * Bırak = Sonara Enhanced (EQ açık)
 * Smoothing ile ani volume/clipping olmamalı.
 */
@Composable
fun HearTheDifferenceScreen(
    isPlaying: Boolean = false,
    onContinue: () -> Unit
) {
    var isOriginalMode by remember { mutableStateOf(false) }
    val p = MaterialTheme.colorScheme.primary

    Column(
        Modifier.fillMaxSize().background(SonaraBackground).padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(Icons.Rounded.GraphicEq, null, Modifier.size(72.dp), tint = p)
        Spacer(Modifier.height(24.dp))
        Text("Hear the Difference", style = MaterialTheme.typography.headlineMedium, textAlign = TextAlign.Center, color = SonaraTextPrimary)
        Spacer(Modifier.height(12.dp))
        Text("Play a song and compare Original vs Sonara", style = MaterialTheme.typography.bodyLarge, textAlign = TextAlign.Center, color = SonaraTextSecondary)

        Spacer(Modifier.height(40.dp))

        if (isPlaying) {
            // A/B Compare kartları
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Original kart
                Card(
                    Modifier.weight(1f).height(120.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isOriginalMode) p.copy(0.2f) else SonaraCard
                    ),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        Modifier.fillMaxSize().padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(Icons.Rounded.MusicOff, null, Modifier.size(32.dp),
                            tint = if (isOriginalMode) p else SonaraTextTertiary)
                        Spacer(Modifier.height(8.dp))
                        Text("Original", style = MaterialTheme.typography.titleMedium,
                            color = if (isOriginalMode) SonaraTextPrimary else SonaraTextTertiary)
                    }
                }

                // Sonara Enhanced kart
                Card(
                    Modifier.weight(1f).height(120.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (!isOriginalMode) p.copy(0.2f) else SonaraCard
                    ),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        Modifier.fillMaxSize().padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(Icons.Rounded.MusicNote, null, Modifier.size(32.dp),
                            tint = if (!isOriginalMode) p else SonaraTextTertiary)
                        Spacer(Modifier.height(8.dp))
                        Text("Sonara Enhanced", style = MaterialTheme.typography.titleMedium,
                            color = if (!isOriginalMode) SonaraTextPrimary else SonaraTextTertiary)
                    }
                }
            }

            Spacer(Modifier.height(24.dp))

            // Basılı tut butonu
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .clip(RoundedCornerShape(28.dp))
                    .background(if (isOriginalMode) SonaraWarning.copy(0.3f) else p.copy(0.15f))
                    .pointerInput(Unit) {
                        detectTapGestures(
                            onPress = {
                                // Basılı tut = Original
                                isOriginalMode = true
                                SonaraApp.instance.setEqEnabled(false)
                                try { awaitRelease() } finally {
                                    // Bırak = Sonara
                                    isOriginalMode = false
                                    SonaraApp.instance.setEqEnabled(true)
                                }
                            }
                        )
                    },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    if (isOriginalMode) "Original — Release for Sonara" else "Hold for Original Sound",
                    style = MaterialTheme.typography.titleMedium,
                    color = SonaraTextPrimary
                )
            }
        } else {
            Card(
                Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = SonaraCard),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Start playing a song to compare", style = MaterialTheme.typography.bodyLarge, color = SonaraTextSecondary, textAlign = TextAlign.Center)
                }
            }
        }

        Spacer(Modifier.height(32.dp))

        FilledTonalButton(
            onClick = onContinue,
            Modifier.fillMaxWidth().height(48.dp),
            shape = RoundedCornerShape(24.dp),
            colors = ButtonDefaults.filledTonalButtonColors(containerColor = p, contentColor = SonaraBackground)
        ) { Text("Continue") }
    }
}
