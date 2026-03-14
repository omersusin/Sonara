package com.sonara.app.ui.screens.presets

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.sonara.app.SonaraApp
import com.sonara.app.preset.BuiltInPresets
import com.sonara.app.preset.Preset
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class PresetsUiState(
    val presets: List<Preset> = emptyList(),
    val selectedFilter: String = "all",
    val selectedPresetId: Long? = null
)

class PresetsViewModel(application: Application) : AndroidViewModel(application) {
    private val repo = (application as SonaraApp).presetRepository
    private val _uiState = MutableStateFlow(PresetsUiState())
    val uiState: StateFlow<PresetsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            repo.allPresets().collect { list ->
                _uiState.update { it.copy(presets = list) }
            }
        }
    }

    fun setFilter(filter: String) { _uiState.update { it.copy(selectedFilter = filter) } }

    fun selectPreset(id: Long) {
        _uiState.update { it.copy(selectedPresetId = id) }
        viewModelScope.launch { repo.markUsed(id) }
    }

    fun toggleFavorite(preset: Preset) {
        viewModelScope.launch { repo.toggleFavorite(preset.id, preset.isFavorite) }
    }

    fun duplicatePreset(preset: Preset) {
        viewModelScope.launch { repo.duplicate(preset) }
    }

    fun deletePreset(preset: Preset) {
        viewModelScope.launch { repo.delete(preset) }
    }

    fun filteredPresets(): List<Preset> {
        val all = _uiState.value.presets
        return when (_uiState.value.selectedFilter) {
            "all" -> all
            "favorites" -> all.filter { it.isFavorite }
            "custom" -> all.filter { !it.isBuiltIn }
            else -> all.filter { it.category == _uiState.value.selectedFilter }
        }
    }

    val filterTabs: List<Pair<String, String>> = listOf(
        "all" to "All",
        "favorites" to "Favorites",
        "custom" to "Custom"
    ) + BuiltInPresets.CATEGORIES.filter { it.key != "custom" }.map { it.key to it.value }
}
