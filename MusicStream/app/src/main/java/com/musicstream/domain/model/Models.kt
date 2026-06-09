package com.musicstream.domain.model

/**
 * Core domain models — pure Kotlin data classes, no Android/framework dependencies.
 */

data class Song(
    val id: String,
    val title: String,
    val artistId: String,
    val artistName: String,
    val albumId: String,
    val albumTitle: String,
    val thumbnailUrl: String,
    val durationMs: Long,
    val isExplicit: Boolean = false,
    val isAvailable: Boolean = true
)

data class Album(
    val id: String,
    val title: String,
    val artistId: String,
    val artistName: String,
    val thumbnailUrl: String,
    val year: Int?,
    val trackCount: Int,
    val songs: List<Song> = emptyList()
)

data class Artist(
    val id: String,
    val name: String,
    val thumbnailUrl: String,
    val subscriberCount: String = "",
    val albums: List<Album> = emptyList(),
    val songs: List<Song> = emptyList()
)

data class Playlist(
    val id: String,
    val title: String,
    val description: String = "",
    val thumbnailUrl: String,
    val songCount: Int,
    val author: String = "",
    val songs: List<Song> = emptyList()
)

data class SearchResult(
    val songs: List<Song> = emptyList(),
    val albums: List<Album> = emptyList(),
    val artists: List<Artist> = emptyList(),
    val playlists: List<Playlist> = emptyList()
)

data class StreamInfo(
    val songId: String,
    val streamUrl: String,
    val format: AudioFormat,
    val bitrateKbps: Int,
    val expireTimestamp: Long
)

enum class AudioFormat { M4A, WEBM, MP3, OGG }

data class LyricLine(
    val startMs: Long,
    val endMs: Long,
    val text: String
)

data class Lyrics(
    val songId: String,
    val lines: List<LyricLine>,
    val isTimeSynced: Boolean
)

data class PlayerState(
    val currentSong: Song? = null,
    val isPlaying: Boolean = false,
    val positionMs: Long = 0L,
    val durationMs: Long = 0L,
    val repeatMode: RepeatMode = RepeatMode.OFF,
    val isShuffleEnabled: Boolean = false,
    val isLoading: Boolean = false
)

enum class RepeatMode { OFF, ONE, ALL }

data class QueueItem(
    val song: Song,
    val queueId: String = java.util.UUID.randomUUID().toString(),
    val isDownloaded: Boolean = false
)

data class DownloadedSong(
    val songId: String,
    val title: String,
    val artistName: String,
    val albumTitle: String,
    val thumbnailUrl: String,
    val localPath: String,
    val downloadedAt: Long,
    val fileSizeBytes: Long
)

data class DownloadProgress(
    val songId: String,
    val progress: Float,       // 0.0 - 1.0
    val status: DownloadStatus
)

enum class DownloadStatus { QUEUED, DOWNLOADING, COMPLETED, FAILED, CANCELLED }

data class RadioStation(
    val id: String,
    val title: String,
    val thumbnailUrl: String,
    val seeds: List<String>   // song or artist IDs
)
