package com.musicstream.data.repository

import com.musicstream.data.api.ApiResult
import com.musicstream.data.api.InnertubeApi
import com.musicstream.data.api.SearchFilter
import com.musicstream.data.local.MusicDatabase
import com.musicstream.data.local.Mappers.toEntity
import com.musicstream.data.local.toDomain
import com.musicstream.domain.model.*
import kotlinx.coroutines.flow.*
import javax.inject.Inject
import javax.inject.Singleton

/**
 * MusicRepository — single source of truth combining remote API + local DB.
 *
 * Pattern:
 *   1. Return cached data immediately (if available).
 *   2. Fetch fresh data from API in background.
 *   3. Update cache and re-emit.
 */
@Singleton
class MusicRepository @Inject constructor(
    private val api: InnertubeApi,
    private val db: MusicDatabase
) {
    // ────────────── Songs ──────────────

    suspend fun getSong(id: String): ApiResult<Song> {
        val cached = db.songDao().getSongById(id)
        if (cached != null) return ApiResult.Success(cached.toDomain())
        return ApiResult.Error("Song not found locally")
    }

    suspend fun getStreamInfo(songId: String): ApiResult<StreamInfo> {
        val cached = db.songDao().getCachedStream(songId)
        if (cached != null) {
            return ApiResult.Success(
                StreamInfo(
                    songId = songId,
                    streamUrl = cached.streamUrl,
                    format = AudioFormat.valueOf(cached.format),
                    bitrateKbps = cached.bitrateKbps,
                    expireTimestamp = cached.expireTimestamp
                )
            )
        }
        return api.getStreamInfo(songId).also { result ->
            if (result is ApiResult.Success) {
                val info = result.data
                db.songDao().cacheStreamUrl(
                    com.musicstream.data.local.entity.StreamCacheEntity(
                        songId = songId,
                        streamUrl = info.streamUrl,
                        format = info.format.name,
                        bitrateKbps = info.bitrateKbps,
                        expireTimestamp = info.expireTimestamp
                    )
                )
            }
        }
    }

    fun getLikedSongs(): Flow<List<Song>> =
        db.songDao().getLikedSongs().map { list -> list.map { it.toDomain() } }

    suspend fun likeSong(song: Song) {
        db.songDao().insertSong(song.toEntity())
        db.songDao().likeSong(com.musicstream.data.local.entity.LikedSongEntity(song.id))
    }

    suspend fun unlikeSong(songId: String) = db.songDao().unlikeSong(songId)

    fun isLiked(songId: String): Flow<Boolean> = db.songDao().isLiked(songId)

    // ────────────── Albums ──────────────

    suspend fun getAlbum(albumId: String): ApiResult<Album> {
        val cached = db.albumDao().getAlbum(albumId)
        if (cached != null) return ApiResult.Success(cached.toDomain())
        return api.getAlbum(albumId).also { result ->
            if (result is ApiResult.Success) {
                db.albumDao().insertAlbum(result.data.toEntity())
            }
        }
    }

    // ────────────── Artists ──────────────

    suspend fun getArtist(artistId: String): ApiResult<Artist> {
        val cached = db.artistDao().getArtist(artistId)
        if (cached != null) return ApiResult.Success(cached.toDomain())
        return api.getArtist(artistId).also { result ->
            if (result is ApiResult.Success) {
                db.artistDao().insertArtist(result.data.toEntity())
            }
        }
    }

    // ────────────── Next / Radio ──────────────

    suspend fun getNextSongs(songId: String, playlistId: String? = null): ApiResult<List<Song>> =
        api.getNextSongs(songId, playlistId).also { result ->
            if (result is ApiResult.Success) {
                db.songDao().insertSongs(result.data.map { it.toEntity() })
            }
        }
}
