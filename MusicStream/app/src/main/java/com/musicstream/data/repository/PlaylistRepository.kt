package com.musicstream.data.repository

import com.musicstream.data.api.ApiResult
import com.musicstream.data.api.InnertubeApi
import com.musicstream.data.local.MusicDatabase
import com.musicstream.data.local.entity.PlaylistEntity
import com.musicstream.data.local.entity.PlaylistSongCrossRef
import com.musicstream.data.local.toDomain
import com.musicstream.data.local.Mappers.toEntity
import com.musicstream.domain.model.Playlist
import com.musicstream.domain.model.Song
import kotlinx.coroutines.flow.*
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PlaylistRepository @Inject constructor(
    private val api: InnertubeApi,
    private val db: MusicDatabase
) {
    // ── Local playlists ──

    fun getLocalPlaylists(): Flow<List<Playlist>> =
        db.playlistDao().getLocalPlaylists().map { list -> list.map { it.toDomain() } }

    fun getPlaylistSongs(playlistId: String): Flow<List<Song>> =
        db.playlistDao().getPlaylistSongs(playlistId).map { list -> list.map { it.toDomain() } }

    suspend fun createPlaylist(title: String, description: String = ""): String {
        val id = UUID.randomUUID().toString()
        db.playlistDao().insertPlaylist(
            PlaylistEntity(
                id = id,
                title = title,
                description = description,
                thumbnailUrl = "",
                songCount = 0,
                author = "Me",
                isLocal = true
            )
        )
        return id
    }

    suspend fun deletePlaylist(playlistId: String) {
        db.playlistDao().deletePlaylist(playlistId)
        db.playlistDao().deletePlaylistSongs(playlistId)
    }

    suspend fun addSongToPlaylist(playlistId: String, song: Song) {
        db.songDao().insertSong(song.toEntity())
        val nextPos = (db.playlistDao().getMaxPosition(playlistId) ?: -1) + 1
        db.playlistDao().addSongToPlaylist(
            PlaylistSongCrossRef(playlistId = playlistId, songId = song.id, position = nextPos)
        )
    }

    suspend fun removeSongFromPlaylist(playlistId: String, songId: String) {
        db.playlistDao().removeSongFromPlaylist(playlistId, songId)
    }

    // ── Remote playlists ──

    suspend fun getRemotePlaylist(playlistId: String): ApiResult<Playlist> =
        api.getPlaylist(playlistId)
}
