package com.musicstream.ui.navigation

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.*
import androidx.navigation.navArgument
import com.musicstream.ui.screens.album.AlbumScreen
import com.musicstream.ui.screens.artist.ArtistScreen
import com.musicstream.ui.screens.home.HomeScreen
import com.musicstream.ui.screens.library.LibraryScreen
import com.musicstream.ui.screens.lyrics.LyricsScreen
import com.musicstream.ui.screens.player.MiniPlayerBar
import com.musicstream.ui.screens.player.PlayerScreen
import com.musicstream.ui.screens.playlist.PlaylistScreen
import com.musicstream.ui.screens.queue.QueueScreen
import com.musicstream.ui.screens.search.SearchScreen
import com.musicstream.ui.screens.settings.SettingsScreen
import com.musicstream.ui.viewmodel.PlayerViewModel

sealed class Screen(val route: String, val label: String, val icon: androidx.compose.ui.graphics.vector.ImageVector? = null) {
    object Home : Screen("home", "Home", Icons.Filled.Home)
    object Search : Screen("search", "Search", Icons.Filled.Search)
    object Library : Screen("library", "Library", Icons.Filled.LibraryMusic)
    object Player : Screen("player", "Player")
    object Queue : Screen("queue", "Queue")
    object Album : Screen("album/{albumId}", "Album") {
        fun route(id: String) = "album/$id"
    }
    object Artist : Screen("artist/{artistId}", "Artist") {
        fun route(id: String) = "artist/$id"
    }
    object Playlist : Screen("playlist/{playlistId}", "Playlist") {
        fun route(id: String) = "playlist/$id"
    }
    object Lyrics : Screen("lyrics", "Lyrics")
    object Settings : Screen("settings", "Settings")
}

val bottomNavItems = listOf(Screen.Home, Screen.Search, Screen.Library)

@Composable
fun MusicStreamNavGraph() {
    val navController = rememberNavController()
    val playerVm: PlayerViewModel = hiltViewModel()
    val playerState by playerVm.playerState.collectAsState()

    Scaffold(
        bottomBar = {
            Column {
                if (playerState.currentSong != null) {
                    MiniPlayerBar(
                        playerState = playerState,
                        onTap = { navController.navigate(Screen.Player.route) },
                        onPlayPause = playerVm::togglePlayPause,
                        onNext = playerVm::skipToNext
                    )
                }
                NavigationBar {
                    val navBackStackEntry by navController.currentBackStackEntryAsState()
                    val currentDestination = navBackStackEntry?.destination
                    bottomNavItems.forEach { screen ->
                        NavigationBarItem(
                            icon = { Icon(screen.icon!!, contentDescription = screen.label) },
                            label = { Text(screen.label) },
                            selected = currentDestination?.hierarchy?.any { it.route == screen.route } == true,
                            onClick = {
                                navController.navigate(screen.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Home.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Screen.Home.route) {
                HomeScreen(
                    onSongClick = { song -> playerVm.playSong(song) },
                    onAlbumClick = { navController.navigate(Screen.Album.route(it)) },
                    onArtistClick = { navController.navigate(Screen.Artist.route(it)) }
                )
            }
            composable(Screen.Search.route) {
                SearchScreen(
                    onSongClick = { song -> playerVm.playSong(song) },
                    onAlbumClick = { navController.navigate(Screen.Album.route(it)) },
                    onArtistClick = { navController.navigate(Screen.Artist.route(it)) },
                    onPlaylistClick = { navController.navigate(Screen.Playlist.route(it)) }
                )
            }
            composable(Screen.Library.route) {
                LibraryScreen(
                    onSongClick = { song -> playerVm.playSong(song) },
                    onPlaylistClick = { navController.navigate(Screen.Playlist.route(it)) }
                )
            }
            composable(Screen.Player.route) {
                PlayerScreen(
                    onBack = { navController.popBackStack() },
                    onQueueClick = { navController.navigate(Screen.Queue.route) },
                    onLyricsClick = { navController.navigate(Screen.Lyrics.route) }
                )
            }
            composable(Screen.Queue.route) {
                QueueScreen(onBack = { navController.popBackStack() })
            }
            composable(Screen.Lyrics.route) {
                LyricsScreen(onBack = { navController.popBackStack() })
            }
            composable(
                Screen.Album.route,
                arguments = listOf(navArgument("albumId") { type = NavType.StringType })
            ) { backStack ->
                AlbumScreen(
                    albumId = backStack.arguments!!.getString("albumId")!!,
                    onBack = { navController.popBackStack() },
                    onSongClick = { song -> playerVm.playSong(song) },
                    onArtistClick = { navController.navigate(Screen.Artist.route(it)) }
                )
            }
            composable(
                Screen.Artist.route,
                arguments = listOf(navArgument("artistId") { type = NavType.StringType })
            ) { backStack ->
                ArtistScreen(
                    artistId = backStack.arguments!!.getString("artistId")!!,
                    onBack = { navController.popBackStack() },
                    onSongClick = { song -> playerVm.playSong(song) },
                    onAlbumClick = { navController.navigate(Screen.Album.route(it)) }
                )
            }
            composable(
                Screen.Playlist.route,
                arguments = listOf(navArgument("playlistId") { type = NavType.StringType })
            ) { backStack ->
                PlaylistScreen(
                    playlistId = backStack.arguments!!.getString("playlistId")!!,
                    onBack = { navController.popBackStack() },
                    onSongClick = { song -> playerVm.playSong(song) }
                )
            }
            composable(Screen.Settings.route) {
                SettingsScreen(onBack = { navController.popBackStack() })
            }
        }
    }
}
