package com.musicstream.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "songs")
data class SongEntity(
    @PrimaryKey val id: String,
    val title: String,
    val artistId: String,
    val artistName: String,
    val albumId: String,
    val albumTitle: String,
    val thumbnailUrl: String,
    val durationMs: Long,
    val isExplicit: Boolean = false,
    val cachedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "albums")
data class AlbumEntity(
    @PrimaryKey val id: String,
    val title: String,
    val artistId: String,
    val artistName: String,
    val thumbnailUrl: String,
    val year: Int?,
    val trackCount: Int,
    val cachedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "artists")
data class ArtistEntity(
    @PrimaryKey val id: String,
    val name: String,
    val thumbnailUrl: String,
    val subscriberCount: String = "",
    val cachedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "playlists")
data class PlaylistEntity(
    @PrimaryKey val id: String,
    val title: String,
    val description: String = "",
    val thumbnailUrl: String,
    val songCount: Int,
    val author: String = "",
    val isLocal: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "playlist_songs", primaryKeys = ["playlistId", "songId"])
data class PlaylistSongCrossRef(
    val playlistId: String,
    val songId: String,
    val position: Int
)

@Entity(tableName = "downloads")
data class DownloadEntity(
    @PrimaryKey val songId: String,
    val title: String,
    val artistName: String,
    val albumTitle: String,
    val thumbnailUrl: String,
    val localPath: String,
    val downloadedAt: Long = System.currentTimeMillis(),
    val fileSizeBytes: Long = 0L
)

@Entity(tableName = "search_history")
data class SearchHistoryEntity(
    @PrimaryKey val query: String,
    val searchedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "liked_songs")
data class LikedSongEntity(
    @PrimaryKey val songId: String,
    val likedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "stream_cache")
data class StreamCacheEntity(
    @PrimaryKey val songId: String,
    val streamUrl: String,
    val format: String,
    val bitrateKbps: Int,
    val expireTimestamp: Long
)
