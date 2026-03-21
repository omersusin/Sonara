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

data class PresetsUiState(val presets: List<Preset> = emptyList(), val selectedFilter: String = "all", val activePresetName: String = "Flat")

class PresetsViewModel(application: Application) : AndroidViewModel(application) {
    private val app = application as SonaraApp
    private val repo = app.presetRepository
    private val _uiState = MutableStateFlow(PresetsUiState())
    val uiState: StateFlow<PresetsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch { repo.allPresets().collect { l -> _uiState.update { it.copy(presets = l) } } }
        viewModelScope.launch { app.eqState.collect { eq -> _uiState.update { it.copy(activePresetName = eq.presetName) } } }
    }

    fun setFilter(f: String) { _uiState.update { it.copy(selectedFilter = f) } }
    fun applyPreset(p: Preset) { app.applyEq(p.bandsArray(), p.name, true, p.bassBoost, p.virtualizer, p.loudness, p.preamp); viewModelScope.launch { repo.markUsed(p.id) } }
    fun toggleFavorite(p: Preset) { viewModelScope.launch { repo.toggleFavorite(p.id, p.isFavorite) } }
    fun duplicatePreset(p: Preset) { viewModelScope.launch { repo.duplicate(p) } }
    fun deletePreset(p: Preset) { viewModelScope.launch { repo.delete(p) } }

    fun filteredPresets(): List<Preset> {
        val all = _uiState.value.presets
        return when (_uiState.value.selectedFilter) {
            "all" -> all; "favorites" -> all.filter { it.isFavorite }; "custom" -> all.filter { !it.isBuiltIn }
            else -> all.filter { it.category == _uiState.value.selectedFilter }
        }
    }

    val filterTabs = listOf("all" to "All", "favorites" to "Favorites", "custom" to "Custom") +
        BuiltInPresets.CATEGORIES.filter { it.key != "custom" }.map { it.key to it.value }
}
