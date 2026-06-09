package com.musicstream.ui.screens.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.musicstream.data.local.AudioQuality
import com.musicstream.ui.viewmodel.SettingsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    vm: SettingsViewModel = hiltViewModel()
) {
    val state by vm.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.Filled.ArrowBack, "Back") }
                }
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(bottom = 16.dp)
        ) {
            // Appearance
            item { SettingsGroupHeader("Appearance") }
            item {
                SwitchPreference(
                    title = "Dark Mode",
                    subtitle = "Use dark theme",
                    icon = Icons.Filled.DarkMode,
                    checked = state.darkMode,
                    onCheckedChange = vm::setDarkMode
                )
            }
            item {
                SwitchPreference(
                    title = "Dynamic Colors",
                    subtitle = "Use system accent colors (Android 12+)",
                    icon = Icons.Filled.Palette,
                    checked = state.dynamicColors,
                    onCheckedChange = vm::setDynamicColors
                )
            }

            // Playback
            item { SettingsGroupHeader("Playback") }
            item {
                ListPreference(
                    title = "Audio Quality",
                    subtitle = "Streaming quality",
                    icon = Icons.Filled.GraphicEq,
                    currentValue = state.audioQuality.label,
                    options = AudioQuality.values().map { it.label },
                    onSelected = { label ->
                        val quality = AudioQuality.values()
                            .firstOrNull { it.label == label }
                            ?: AudioQuality.AUTO
                        vm.setAudioQuality(quality)
                    }
                )
            }
            item {
                SwitchPreference(
                    title = "Show Lyrics",
                    subtitle = "Display lyrics on player screen",
                    icon = Icons.Filled.Lyrics,
                    checked = state.showLyrics,
                    onCheckedChange = vm::setShowLyrics
                )
            }

            // Downloads
            item { SettingsGroupHeader("Downloads") }
            item {
                SwitchPreference(
                    title = "Wi-Fi Only",
                    subtitle = "Download only on Wi-Fi",
                    icon = Icons.Filled.Wifi,
                    checked = state.downloadWifiOnly,
                    onCheckedChange = vm::setDownloadWifiOnly
                )
            }
            item {
                ListItem(
                    headlineContent = { Text("Clear Cache") },
                    supportingContent = { Text("Free up storage") },
                    leadingContent = { Icon(Icons.Filled.DeleteSweep, null) },
                    modifier = Modifier.clickable { vm.clearCache() }
                )
            }

            // About
            item { SettingsGroupHeader("About") }
            item {
                ListItem(
                    headlineContent = { Text("MusicStream") },
                    supportingContent = { Text("Version 1.0.0") },
                    leadingContent = { Icon(Icons.Filled.Info, null) }
                )
            }
        }
    }
}

@Composable
fun SettingsGroupHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = 16.dp, top = 16.dp, bottom = 4.dp)
    )
}

@Composable
fun SwitchPreference(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    ListItem(
        headlineContent = { Text(title) },
        supportingContent = {
            Text(subtitle, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
        },
        leadingContent = { Icon(icon, null) },
        trailingContent = {
            Switch(checked = checked, onCheckedChange = onCheckedChange)
        }
    )
}

@Composable
fun ListPreference(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    currentValue: String,
    options: List<String>,
    onSelected: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        ListItem(
            headlineContent = { Text(title) },
            supportingContent = {
                Text(currentValue, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
            },
            leadingContent = { Icon(icon, null) },
            modifier = Modifier.clickable { expanded = true }
        )
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option) },
                    onClick = { onSelected(option); expanded = false }
                )
            }
        }
    }
}
