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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sonara.app.ui.components.FluentCard
import com.sonara.app.ui.theme.SonaraTextPrimary
import com.sonara.app.ui.theme.SonaraTextSecondary
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
                    Spacer(Modifier.height(8.dp))
                    LyricsAnimationStyle.entries.forEach { style ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = s.lyricsAnimationStyle == style,
                                onClick = { vm.setLyricsAnimationStyle(style) },
                                colors = RadioButtonDefaults.colors(selectedColor = p)
                            )
                            Column {
                                Text(style.label, style = MaterialTheme.typography.bodyMedium, color = SonaraTextPrimary)
                            }
                        }
                    }
                }
            }

            item {
                FluentCard {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Text Size", style = MaterialTheme.typography.titleMedium, color = SonaraTextPrimary)
                        Text("${s.lyricsTextSize.roundToInt()} sp", style = MaterialTheme.typography.bodyMedium, color = p)
                    }
                    Spacer(Modifier.height(4.dp))
                    Slider(
                        value = s.lyricsTextSize,
                        onValueChange = { vm.setLyricsTextSize(it) },
                        valueRange = 10f..24f,
                        steps = 13,
                        colors = SliderDefaults.colors(thumbColor = p, activeTrackColor = p)
                    )
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("10 sp", style = MaterialTheme.typography.labelSmall, color = SonaraTextSecondary)
                        Text("24 sp", style = MaterialTheme.typography.labelSmall, color = SonaraTextSecondary)
                    }
                }
            }

            item {
                FluentCard {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Sync Offset", style = MaterialTheme.typography.titleMedium, color = SonaraTextPrimary)
                        Text("${s.lyricsSyncOffsetMs} ms", style = MaterialTheme.typography.bodyMedium, color = p)
                    }
                    Text(
                        "Adjust if lyrics feel early or late",
                        style = MaterialTheme.typography.bodySmall,
                        color = SonaraTextSecondary
                    )
                    Spacer(Modifier.height(4.dp))
                    Slider(
                        value = s.lyricsSyncOffsetMs.toFloat(),
                        onValueChange = { vm.setLyricsSyncOffset(it.roundToInt()) },
                        valueRange = -2000f..2000f,
                        steps = 39,
                        colors = SliderDefaults.colors(thumbColor = p, activeTrackColor = p)
                    )
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("-2000 ms", style = MaterialTheme.typography.labelSmall, color = SonaraTextSecondary)
                        Text("+2000 ms", style = MaterialTheme.typography.labelSmall, color = SonaraTextSecondary)
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
