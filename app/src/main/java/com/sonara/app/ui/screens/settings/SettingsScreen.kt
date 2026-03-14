package com.sonara.app.ui.screens.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sonara.app.ui.components.ChipStatus
import com.sonara.app.ui.components.FluentCard
import com.sonara.app.ui.components.StatusChip
import com.sonara.app.ui.theme.*

@Composable
fun SettingsScreen() {
    val viewModel: SettingsViewModel = viewModel()
    val state by viewModel.uiState.collectAsState()

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text("Settings", style = MaterialTheme.typography.headlineLarge, modifier = Modifier.padding(vertical = 8.dp))
        }

        item { SectionHeader("Last.fm Integration") }
        item { LastFmCard(state, viewModel) }

        item { SectionHeader("Appearance") }
        item { AccentColorCard(state.accentColor) { viewModel.setAccentColor(it) } }

        item { SectionHeader("Sound Engine") }
        item { SoundEngineCard(state, viewModel) }

        item { SectionHeader("About") }
        item { AboutCard() }

        item { Spacer(Modifier.height(16.dp)) }
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        title.uppercase(),
        style = MaterialTheme.typography.labelSmall,
        color = SonaraTextTertiary,
        modifier = Modifier.padding(start = 4.dp, top = 8.dp, bottom = 2.dp)
    )
}

@Composable
private fun LastFmCard(state: SettingsUiState, viewModel: SettingsViewModel) {
    FluentCard {
        KeyInputSection(
            title = "API Key",
            isSet = state.isApiKeySet,
            description = if (state.isApiKeySet) "Genre detection via Last.fm is active." else "Enter your Last.fm API key to enable genre detection.",
            inputValue = state.apiKeyInput,
            onInputChange = { viewModel.updateApiKeyInput(it) },
            placeholder = "Enter API key...",
            buttonText = "Save API Key",
            onSave = { viewModel.saveApiKey() }
        )

        HorizontalDivider(
            modifier = Modifier.padding(vertical = 16.dp),
            thickness = 0.5.dp,
            color = SonaraDivider.copy(alpha = 0.5f)
        )

        KeyInputSection(
            title = "Shared Secret",
            isSet = state.isSharedSecretSet,
            description = if (state.isSharedSecretSet) "Scrobbling is ready." else "Required for Last.fm scrobbling.",
            inputValue = state.sharedSecretInput,
            onInputChange = { viewModel.updateSharedSecretInput(it) },
            placeholder = "Enter shared secret...",
            buttonText = "Save Secret",
            onSave = { viewModel.saveSharedSecret() }
        )
    }
}

@Composable
private fun KeyInputSection(
    title: String,
    isSet: Boolean,
    description: String,
    inputValue: String,
    onInputChange: (String) -> Unit,
    placeholder: String,
    buttonText: String,
    onSave: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(title, style = MaterialTheme.typography.titleMedium)
        StatusChip(if (isSet) "Set" else "Not Set", if (isSet) ChipStatus.Active else ChipStatus.Inactive)
    }
    Spacer(Modifier.height(4.dp))
    Text(description, style = MaterialTheme.typography.bodySmall, color = SonaraTextSecondary)
    Spacer(Modifier.height(10.dp))
    OutlinedTextField(
        value = inputValue,
        onValueChange = onInputChange,
        placeholder = { Text(placeholder, color = SonaraTextTertiary) },
        visualTransformation = PasswordVisualTransformation(),
        modifier = Modifier.fillMaxWidth(),
        singleLine = true,
        shape = MaterialTheme.shapes.small,
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = MaterialTheme.colorScheme.primary,
            unfocusedBorderColor = SonaraDivider,
            focusedContainerColor = SonaraCardElevated,
            unfocusedContainerColor = SonaraCardElevated,
            cursorColor = MaterialTheme.colorScheme.primary,
            focusedTextColor = SonaraTextPrimary,
            unfocusedTextColor = SonaraTextPrimary
        )
    )
    Spacer(Modifier.height(10.dp))
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
        OutlinedButton(
            onClick = onSave,
            enabled = inputValue.isNotBlank(),
            shape = MaterialTheme.shapes.extraLarge,
            border = ButtonDefaults.outlinedButtonBorder(enabled = inputValue.isNotBlank()),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.primary)
        ) { Text(buttonText) }
    }
}

@Composable
private fun AccentColorCard(selected: AccentColor, onSelect: (AccentColor) -> Unit) {
    FluentCard {
        Text("Accent Color", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(4.dp))
        Text(selected.displayName, style = MaterialTheme.typography.bodySmall, color = SonaraTextSecondary)
        Spacer(Modifier.height(16.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            AccentColor.entries.forEach { color ->
                val isSelected = color == selected
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(color.primary)
                        .then(
                            if (isSelected) Modifier.border(2.5.dp, SonaraTextPrimary, CircleShape)
                            else Modifier.border(1.dp, SonaraDivider.copy(alpha = 0.3f), CircleShape)
                        )
                        .clickable { onSelect(color) },
                    contentAlignment = Alignment.Center
                ) {
                    if (isSelected) {
                        Icon(Icons.Rounded.Check, contentDescription = null, tint = SonaraBackground, modifier = Modifier.size(18.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun SoundEngineCard(state: SettingsUiState, viewModel: SettingsViewModel) {
    FluentCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text("AI Auto-adjust", style = MaterialTheme.typography.titleMedium)
                Text("Adjust EQ based on detected genre and mood", style = MaterialTheme.typography.bodySmall, color = SonaraTextSecondary)
            }
            Switch(
                checked = state.aiEnabled,
                onCheckedChange = { viewModel.setAiEnabled(it) },
                colors = SwitchDefaults.colors(
                    checkedThumbColor = MaterialTheme.colorScheme.primary,
                    checkedTrackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f),
                    uncheckedThumbColor = SonaraTextTertiary,
                    uncheckedTrackColor = SonaraCardElevated
                )
            )
        }

        HorizontalDivider(
            modifier = Modifier.padding(vertical = 14.dp),
            thickness = 0.5.dp,
            color = SonaraDivider.copy(alpha = 0.5f)
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text("AutoEQ", style = MaterialTheme.typography.titleMedium)
                Text("Apply headphone correction automatically", style = MaterialTheme.typography.bodySmall, color = SonaraTextSecondary)
            }
            Switch(
                checked = state.autoEqEnabled,
                onCheckedChange = { viewModel.setAutoEqEnabled(it) },
                colors = SwitchDefaults.colors(
                    checkedThumbColor = MaterialTheme.colorScheme.primary,
                    checkedTrackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f),
                    uncheckedThumbColor = SonaraTextTertiary,
                    uncheckedTrackColor = SonaraCardElevated
                )
            )
        }
    }
}

@Composable
private fun AboutCard() {
    FluentCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text("Sonara", style = MaterialTheme.typography.titleMedium)
                Text("Personal Sound Engine", style = MaterialTheme.typography.bodySmall, color = SonaraTextSecondary)
            }
            Text("v1.0.0", style = MaterialTheme.typography.labelLarge, color = SonaraTextTertiary)
        }
    }
}
