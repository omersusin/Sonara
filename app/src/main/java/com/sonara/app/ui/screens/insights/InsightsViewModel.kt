package com.sonara.app.ui.screens.insights

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class InsightEntry(
    val label: String,
    val value: String,
    val detail: String = "",
    val confidence: Float = 0f
)

data class InsightsUiState(
    val trackTitle: String = "",
    val trackArtist: String = "",
    val dataSource: String = "None",
    val genre: String = "Unknown",
    val mood: String = "Unknown",
    val energy: Float = 0.5f,
    val confidence: Float = 0f,
    val autoEqActive: Boolean = false,
    val headphoneName: String = "",
    val activePreset: String = "Flat",
    val aiAdjustment: String = "None",
    val entries: List<InsightEntry> = emptyList()
)

class InsightsViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(InsightsUiState())
    val uiState: StateFlow<InsightsUiState> = _uiState.asStateFlow()
}
