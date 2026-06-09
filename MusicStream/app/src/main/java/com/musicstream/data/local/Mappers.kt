package com.musicstream.data.local

import com.musicstream.data.local.entity.*
import com.musicstream.domain.model.*

// ── Entity → Domain ──

fun SongEntity.toDomain() = Song(
    id = id,
    title = title,
    artistId = artistId,
    artistName = artistName,
    albumId = albumId,
    albumTitle = albumTitle,
    thumbnailUrl = thumbnailUrl,
    durationMs = durationMs,
    isExplicit = isExplicit
)

fun AlbumEntity.toDomain() = Album(
    id = id,
    title = title,
    artistId = artistId,
    artistName = artistName,
    thumbnailUrl = thumbnailUrl,
    year = year,
    trackCount = trackCount
)

fun ArtistEntity.toDomain() = Artist(
    id = id,
    name = name,
    thumbnailUrl = thumbnailUrl,
    subscriberCount = subscriberCount
)

fun PlaylistEntity.toDomain() = Playlist(
    id = id,
    title = title,
    description = description,
    thumbnailUrl = thumbnailUrl,
    songCount = songCount,
    author = author
)

fun DownloadEntity.toDomain() = DownloadedSong(
    songId = songId,
    title = title,
    artistName = artistName,
    albumTitle = albumTitle,
    thumbnailUrl = thumbnailUrl,
    localPath = localPath,
    downloadedAt = downloadedAt,
    fileSizeBytes = fileSizeBytes
)

// ── Domain → Entity ──

fun Song.toEntity() = SongEntity(
    id = id,
    title = title,
    artistId = artistId,
    artistName = artistName,
    albumId = albumId,
    albumTitle = albumTitle,
    thumbnailUrl = thumbnailUrl,
    durationMs = durationMs,
    isExplicit = isExplicit
)

fun Album.toEntity() = AlbumEntity(
    id = id,
    title = title,
    artistId = artistId,
    artistName = artistName,
    thumbnailUrl = thumbnailUrl,
    year = year,
    trackCount = trackCount
)

fun Artist.toEntity() = ArtistEntity(
    id = id,
    name = name,
    thumbnailUrl = thumbnailUrl,
    subscriberCount = subscriberCount
)

fun Playlist.toEntity(isLocal: Boolean = false) = PlaylistEntity(
    id = id,
    title = title,
    description = description,
    thumbnailUrl = thumbnailUrl,
    songCount = songCount,
    author = author,
    isLocal = isLocal
)

// Alias for backwards compatibility inside repository code
object Mappers {
    fun Song.toEntity() = this.toEntity()
    fun Album.toEntity() = this.toEntity()
    fun Artist.toEntity() = this.toEntity()
    fun Playlist.toEntity(isLocal: Boolean = false) = this.toEntity(isLocal)
}
