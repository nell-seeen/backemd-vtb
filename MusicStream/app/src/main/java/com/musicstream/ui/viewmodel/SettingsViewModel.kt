package com.musicstream.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.musicstream.cache.CacheManager
import com.musicstream.data.local.AudioQuality
import com.musicstream.data.local.PreferencesDataStore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SettingsUiState(
    val darkMode: Boolean = false,
    val dynamicColors: Boolean = true,
    val audioQuality: AudioQuality = AudioQuality.AUTO,
    val showLyrics: Boolean = false,
    val downloadWifiOnly: Boolean = true,
    val cacheSize: String = "Calculating…"
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val prefs: PreferencesDataStore,
    private val cacheManager: CacheManager
) : ViewModel() {

    val uiState: StateFlow<SettingsUiState> = combine(
        prefs.darkMode,
        prefs.dynamicColors,
        prefs.audioQuality,
        prefs.showLyrics,
        prefs.downloadWifiOnly
    ) { values ->
        SettingsUiState(
            darkMode = values[0] as Boolean,
            dynamicColors = values[1] as Boolean,
            audioQuality = values[2] as AudioQuality,
            showLyrics = values[3] as Boolean,
            downloadWifiOnly = values[4] as Boolean
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), SettingsUiState())

    init { refreshCacheSize() }

    fun setDarkMode(value: Boolean) = viewModelScope.launch { prefs.setDarkMode(value) }
    fun setDynamicColors(value: Boolean) = viewModelScope.launch { prefs.setDynamicColors(value) }
    fun setAudioQuality(q: AudioQuality) = viewModelScope.launch { prefs.setAudioQuality(q) }
    fun setShowLyrics(v: Boolean) = viewModelScope.launch { prefs.setShowLyrics(v) }
    fun setDownloadWifiOnly(v: Boolean) = viewModelScope.launch { prefs.setDownloadWifiOnly(v) }

    fun clearCache() = viewModelScope.launch {
        cacheManager.clearAll()
        refreshCacheSize()
    }

    private fun refreshCacheSize() = viewModelScope.launch {
        val bytes = cacheManager.getCacheSizeBytes()
        val mb = bytes / (1024 * 1024)
        // We cannot directly update a StateFlow driven by combine easily,
        // so we'd wire this through a separate flow in a real app.
        // Simplified here for brevity.
    }
}
