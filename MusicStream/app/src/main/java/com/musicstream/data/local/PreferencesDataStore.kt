package com.musicstream.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import com.musicstream.domain.model.RepeatMode
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "music_stream_prefs")

@Singleton
class PreferencesDataStore @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val store = context.dataStore

    object Keys {
        val REPEAT_MODE = stringPreferencesKey("repeat_mode")
        val SHUFFLE_ENABLED = booleanPreferencesKey("shuffle_enabled")
        val AUDIO_QUALITY = stringPreferencesKey("audio_quality")
        val DARK_MODE = booleanPreferencesKey("dark_mode")
        val DYNAMIC_COLORS = booleanPreferencesKey("dynamic_colors")
        val DOWNLOAD_WIFI_ONLY = booleanPreferencesKey("download_wifi_only")
        val LAST_PLAYED_SONG_ID = stringPreferencesKey("last_played_song_id")
        val LAST_POSITION_MS = longPreferencesKey("last_position_ms")
        val SHOW_LYRICS = booleanPreferencesKey("show_lyrics")
        val EQUALIZER_ENABLED = booleanPreferencesKey("equalizer_enabled")
    }

    // ── Getters ──

    val repeatMode: Flow<RepeatMode> = store.data
        .catch { emit(emptyPreferences()) }
        .map { prefs ->
            RepeatMode.valueOf(prefs[Keys.REPEAT_MODE] ?: RepeatMode.OFF.name)
        }

    val shuffleEnabled: Flow<Boolean> = store.data
        .catch { emit(emptyPreferences()) }
        .map { it[Keys.SHUFFLE_ENABLED] ?: false }

    val darkMode: Flow<Boolean> = store.data
        .catch { emit(emptyPreferences()) }
        .map { it[Keys.DARK_MODE] ?: false }

    val dynamicColors: Flow<Boolean> = store.data
        .catch { emit(emptyPreferences()) }
        .map { it[Keys.DYNAMIC_COLORS] ?: true }

    val downloadWifiOnly: Flow<Boolean> = store.data
        .catch { emit(emptyPreferences()) }
        .map { it[Keys.DOWNLOAD_WIFI_ONLY] ?: true }

    val audioQuality: Flow<AudioQuality> = store.data
        .catch { emit(emptyPreferences()) }
        .map { AudioQuality.valueOf(it[Keys.AUDIO_QUALITY] ?: AudioQuality.AUTO.name) }

    val showLyrics: Flow<Boolean> = store.data
        .catch { emit(emptyPreferences()) }
        .map { it[Keys.SHOW_LYRICS] ?: false }

    // ── Setters ──

    suspend fun setRepeatMode(mode: RepeatMode) = store.edit { it[Keys.REPEAT_MODE] = mode.name }

    suspend fun setShuffleEnabled(enabled: Boolean) = store.edit { it[Keys.SHUFFLE_ENABLED] = enabled }

    suspend fun setDarkMode(enabled: Boolean) = store.edit { it[Keys.DARK_MODE] = enabled }

    suspend fun setDynamicColors(enabled: Boolean) = store.edit { it[Keys.DYNAMIC_COLORS] = enabled }

    suspend fun setDownloadWifiOnly(enabled: Boolean) = store.edit { it[Keys.DOWNLOAD_WIFI_ONLY] = enabled }

    suspend fun setAudioQuality(quality: AudioQuality) = store.edit { it[Keys.AUDIO_QUALITY] = quality.name }

    suspend fun setShowLyrics(show: Boolean) = store.edit { it[Keys.SHOW_LYRICS] = show }

    suspend fun saveLastPlayed(songId: String, positionMs: Long) = store.edit {
        it[Keys.LAST_PLAYED_SONG_ID] = songId
        it[Keys.LAST_POSITION_MS] = positionMs
    }
}

enum class AudioQuality(val label: String, val minBitrateKbps: Int) {
    LOW("Low (64 kbps)", 64),
    MEDIUM("Medium (128 kbps)", 128),
    HIGH("High (256 kbps)", 256),
    AUTO("Auto", 0)
}
