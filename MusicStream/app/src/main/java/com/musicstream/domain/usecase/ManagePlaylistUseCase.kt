package com.musicstream.domain.usecase

import com.musicstream.data.repository.PlaylistRepository
import com.musicstream.domain.model.Playlist
import com.musicstream.domain.model.Song
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * ManagePlaylistUseCase — CRUD operations for local playlists.
 */
class ManagePlaylistUseCase @Inject constructor(
    private val playlistRepository: PlaylistRepository
) {
    fun getLocalPlaylists(): Flow<List<Playlist>> = playlistRepository.getLocalPlaylists()

    suspend fun create(title: String, description: String = ""): String =
        playlistRepository.createPlaylist(title, description)

    suspend fun delete(playlistId: String) = playlistRepository.deletePlaylist(playlistId)

    suspend fun addSong(playlistId: String, song: Song) =
        playlistRepository.addSongToPlaylist(playlistId, song)

    suspend fun removeSong(playlistId: String, songId: String) =
        playlistRepository.removeSongFromPlaylist(playlistId, songId)

    fun getPlaylistSongs(playlistId: String): Flow<List<Song>> =
        playlistRepository.getPlaylistSongs(playlistId)
}
