package com.sonara.app.ui.screens.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.TextUnitType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sonara.app.ui.components.FluentCard
import com.sonara.app.ui.theme.SonaraDivider
import com.sonara.app.ui.theme.SonaraTextPrimary
import com.sonara.app.ui.theme.SonaraTextSecondary
import com.sonara.app.ui.theme.SonaraTextTertiary
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LyricsSettingsScreen(onBack: () -> Unit = {}) {
    val vm: SettingsViewModel = viewModel()
    val s by vm.uiState.collectAsState()
    val p = MaterialTheme.colorScheme.primary

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Lyrics") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Rounded.ArrowBack, "Back") } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                FluentCard {
                    Text("Animation Style", style = MaterialTheme.typography.titleMedium, color = SonaraTextPrimary)
                    Spacer(Modifier.height(4.dp))
                    Text("Choose how synced lyrics are displayed", style = MaterialTheme.typography.bodySmall, color = SonaraTextSecondary)
                    Spacer(Modifier.height(12.dp))
                    Column(Modifier.selectableGroup()) {
                        LyricsAnimationStyle.entries.forEach { style ->
                            val selected = s.lyricsAnimationStyle == style
                            val desc = when (style) {
                                LyricsAnimationStyle.NONE      -> "No animation, plain text"
                                LyricsAnimationStyle.FADE      -> "Lines fade in and out"
                                LyricsAnimationStyle.GLOW      -> "Active line glows with color"
                                LyricsAnimationStyle.SLIDE     -> "Lines slide in from the side"
                                LyricsAnimationStyle.KARAOKE   -> "Word-by-word fill effect"
                                LyricsAnimationStyle.APPLE     -> "Apple Music style scroll"
                                LyricsAnimationStyle.APPLE_V2  -> "Per-letter timing (requires TTML)"
                                LyricsAnimationStyle.VIVIMUSIC -> "Fluid bounce animation"
                                LyricsAnimationStyle.LYRICS_V2 -> "Smooth flowing highlight"
                                LyricsAnimationStyle.METRO     -> "Metro tile-style animation"
                            }
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .selectable(selected = selected, role = Role.RadioButton, onClick = { vm.setLyricsAnimationStyle(style) })
                                    .padding(vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                RadioButton(selected = selected, onClick = null, colors = RadioButtonDefaults.colors(selectedColor = p))
                                Column {
                                    Text(style.displayName, style = MaterialTheme.typography.bodyMedium, color = if (selected) p else SonaraTextPrimary)
                                    Text(desc, style = MaterialTheme.typography.labelSmall, color = SonaraTextTertiary)
                                }
                            }
                            if (style != LyricsAnimationStyle.entries.last()) {
                                HorizontalDivider(thickness = 0.5.dp, color = SonaraDivider.copy(alpha = 0.3f))
                            }
                        }
                    }
                }
            }

            item {
                FluentCard {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text("Text Size", style = MaterialTheme.typography.titleMedium, color = SonaraTextPrimary)
                        Text("${s.lyricsTextSize.roundToInt()} sp", style = MaterialTheme.typography.labelLarge, color = p)
                    }
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "♪ Sample lyric line",
                        style = MaterialTheme.typography.bodyMedium.copy(fontSize = TextUnit(s.lyricsTextSize, TextUnitType.Sp)),
                        color = SonaraTextSecondary,
                        modifier = Modifier.padding(vertical = 6.dp)
                    )
                    Slider(
                        value = s.lyricsTextSize,
                        onValueChange = { vm.setLyricsTextSize(it) },
                        valueRange = 14f..36f,
                        steps = 21,
                        colors = SliderDefaults.colors(thumbColor = p, activeTrackColor = p)
                    )
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Small", style = MaterialTheme.typography.labelSmall, color = SonaraTextTertiary)
                        Text("Large", style = MaterialTheme.typography.labelSmall, color = SonaraTextTertiary)
                    }
                }
            }

            item {
                FluentCard {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text("Sync Offset", style = MaterialTheme.typography.titleMedium, color = SonaraTextPrimary)
                            Text("Nudge lyrics ±500 ms if they feel early or late", style = MaterialTheme.typography.bodySmall, color = SonaraTextSecondary)
                        }
                        Text(
                            "${if (s.lyricsSyncOffsetMs >= 0) "+" else ""}${s.lyricsSyncOffsetMs} ms",
                            style = MaterialTheme.typography.labelLarge,
                            color = p
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                    Slider(
                        value = s.lyricsSyncOffsetMs.toFloat(),
                        onValueChange = { vm.setLyricsSyncOffset(it.roundToInt()) },
                        valueRange = -500f..500f,
                        colors = SliderDefaults.colors(thumbColor = p, activeTrackColor = p)
                    )
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("-500 ms", style = MaterialTheme.typography.labelSmall, color = SonaraTextTertiary)
                        Text("+500 ms", style = MaterialTheme.typography.labelSmall, color = SonaraTextTertiary)
                    }
                }
            }

            item {
                FluentCard {
                    SwitchRow(
                        title = "Show translated lyrics",
                        desc = "Display translated version when available",
                        checked = s.lyricsShowTranslated,
                        onChange = { vm.setLyricsShowTranslated(it) }
                    )
                }
            }

            item { Spacer(Modifier.height(16.dp)) }
        }
    }
}
