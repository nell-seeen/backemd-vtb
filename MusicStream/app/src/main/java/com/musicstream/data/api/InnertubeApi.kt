package com.musicstream.data.api

import com.musicstream.data.api.dto.*
import com.musicstream.domain.model.*
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import javax.inject.Inject
import javax.inject.Singleton

/**
 * High-level API layer that wraps MusicApiService and maps DTOs → domain models.
 *
 * Architecture:
 *   MusicApiService (Retrofit)  →  InnertubeApi (mapping)  →  Repositories
 */
@Singleton
class InnertubeApi @Inject constructor(
    private val service: MusicApiService
) {
    // ──────────────── Search ────────────────

    suspend fun search(query: String, filter: SearchFilter = SearchFilter.ALL): ApiResult<SearchResult> =
        safeApiCall {
            val response = service.search(
                SearchRequest(
                    query = query,
                    params = filter.param
                )
            )
            response.mapToSearchResult()
        }

    suspend fun getSearchSuggestions(input: String): ApiResult<List<String>> =
        safeApiCall {
            val response = service.getSearchSuggestions(SuggestionsRequest(input = input))
            response.contents
                ?.flatMap { it.sectionRenderer?.contents.orEmpty() }
                ?.mapNotNull { it.renderer?.suggestion?.text() }
                ?: emptyList()
        }

    // ──────────────── Browse ────────────────

    suspend fun getAlbum(albumId: String): ApiResult<Album> =
        safeApiCall {
            val response = service.browse(BrowseRequest(browseId = albumId))
            response.mapToAlbum(albumId)
        }

    suspend fun getArtist(artistId: String): ApiResult<Artist> =
        safeApiCall {
            val response = service.browse(BrowseRequest(browseId = artistId))
            response.mapToArtist(artistId)
        }

    suspend fun getPlaylist(playlistId: String): ApiResult<Playlist> =
        safeApiCall {
            val response = service.browse(BrowseRequest(browseId = "VL$playlistId"))
            response.mapToPlaylist(playlistId)
        }

    suspend fun getHome(): ApiResult<List<Any>> =
        safeApiCall {
            service.browse(BrowseRequest(browseId = "FEmusic_home"))
            emptyList<Any>() // simplified
        }

    // ──────────────── Player ────────────────

    suspend fun getStreamInfo(songId: String): ApiResult<StreamInfo> =
        safeApiCall {
            val response = service.getPlayer(PlayerRequest(videoId = songId))
            response.mapToStreamInfo(songId)
        }

    // ──────────────── Next / Radio ────────────────

    suspend fun getNextSongs(
        songId: String,
        playlistId: String? = null
    ): ApiResult<List<Song>> =
        safeApiCall {
            val response = service.getNext(NextRequest(videoId = songId, playlistId = playlistId))
            response.mapToSongs()
        }

    // ──────────────── Lyrics ────────────────

    suspend fun getLyrics(browseId: String): ApiResult<Lyrics> =
        safeApiCall {
            val response = service.getLyrics(browseId)
            response.mapToLyrics(browseId)
        }
}

// ──────────────── Mapping Extensions ────────────────

private fun SearchResponse.mapToSearchResult(): SearchResult {
    val sections = contents
        ?.tabbedSearchResultsRenderer?.tabs?.firstOrNull()
        ?.tabRenderer?.content?.sectionListRenderer?.contents.orEmpty()

    val songs = mutableListOf<Song>()
    val albums = mutableListOf<Album>()
    val artists = mutableListOf<Artist>()
    val playlists = mutableListOf<Playlist>()

    sections.forEach { section ->
        val shelf = section.musicShelfRenderer ?: return@forEach
        val type = shelf.title?.text() ?: ""
        val items = shelf.contents.orEmpty()

        items.forEach { item ->
            val renderer = item.musicResponsiveListItemRenderer ?: return@forEach
            val columns = renderer.flexColumns.orEmpty()
            val title = columns.getOrNull(0)?.musicResponsiveListItemFlexColumnRenderer?.text?.text() ?: ""
            val subtitle = columns.getOrNull(1)?.musicResponsiveListItemFlexColumnRenderer?.text?.text() ?: ""
            val thumbnail = renderer.thumbnail?.musicThumbnailRenderer?.thumbnail?.best() ?: ""
            val videoId = renderer.overlay?.musicItemThumbnailOverlayRenderer?.content
                ?.musicPlayButtonRenderer?.playNavigationEndpoint?.watchEndpoint?.videoId
                ?: renderer.navigationEndpoint?.watchEndpoint?.videoId

            when {
                type.contains("Song", ignoreCase = true) && videoId != null -> {
                    songs.add(
                        Song(
                            id = videoId,
                            title = title,
                            artistId = "",
                            artistName = subtitle,
                            albumId = "",
                            albumTitle = "",
                            thumbnailUrl = thumbnail,
                            durationMs = 0L
                        )
                    )
                }
                type.contains("Album", ignoreCase = true) -> {
                    val browseId = renderer.navigationEndpoint?.browseEndpoint?.browseId ?: return@forEach
                    albums.add(
                        Album(
                            id = browseId,
                            title = title,
                            artistId = "",
                            artistName = subtitle,
                            thumbnailUrl = thumbnail,
                            year = null,
                            trackCount = 0
                        )
                    )
                }
                type.contains("Artist", ignoreCase = true) -> {
                    val browseId = renderer.navigationEndpoint?.browseEndpoint?.browseId ?: return@forEach
                    artists.add(Artist(id = browseId, name = title, thumbnailUrl = thumbnail))
                }
                type.contains("Playlist", ignoreCase = true) -> {
                    val browseId = renderer.navigationEndpoint?.browseEndpoint?.browseId ?: return@forEach
                    playlists.add(
                        Playlist(
                            id = browseId,
                            title = title,
                            thumbnailUrl = thumbnail,
                            songCount = 0,
                            author = subtitle
                        )
                    )
                }
            }
        }
    }

    return SearchResult(songs, albums, artists, playlists)
}

private fun BrowseResponse.mapToAlbum(albumId: String): Album {
    val header = header?.musicDetailHeaderRenderer
    return Album(
        id = albumId,
        title = header?.title?.text() ?: "",
        artistId = "",
        artistName = header?.subtitle?.text() ?: "",
        thumbnailUrl = header?.thumbnail?.musicThumbnailRenderer?.thumbnail?.best() ?: "",
        year = null,
        trackCount = 0
    )
}

private fun BrowseResponse.mapToArtist(artistId: String): Artist {
    val header = header?.musicImmersiveHeaderRenderer
    return Artist(
        id = artistId,
        name = header?.title?.text() ?: "",
        thumbnailUrl = header?.thumbnail?.musicThumbnailRenderer?.thumbnail?.best() ?: ""
    )
}

private fun BrowseResponse.mapToPlaylist(playlistId: String): Playlist {
    val header = header?.musicDetailHeaderRenderer
    return Playlist(
        id = playlistId,
        title = header?.title?.text() ?: "",
        thumbnailUrl = header?.thumbnail?.musicThumbnailRenderer?.thumbnail?.best() ?: "",
        songCount = 0,
        author = header?.subtitle?.text() ?: ""
    )
}

private fun PlayerResponse.mapToStreamInfo(songId: String): StreamInfo {
    val format = streamingData?.adaptiveFormats
        ?.filter { it.mimeType.startsWith("audio/") }
        ?.maxByOrNull { it.bitrate }
    val url = format?.url ?: throw IllegalStateException("No stream URL available for $songId")
    val expireSeconds = streamingData?.expiresInSeconds?.toLongOrNull() ?: 21600L
    return StreamInfo(
        songId = songId,
        streamUrl = url,
        format = when {
            format.mimeType.contains("mp4") -> AudioFormat.M4A
            format.mimeType.contains("webm") -> AudioFormat.WEBM
            else -> AudioFormat.M4A
        },
        bitrateKbps = format.bitrate / 1000,
        expireTimestamp = System.currentTimeMillis() + expireSeconds * 1000
    )
}

private fun NextResponse.mapToSongs(): List<Song> {
    // Simplified extraction from next response
    return emptyList()
}

private fun LyricsResponse.mapToLyrics(songId: String): Lyrics {
    val lines = lyrics?.lyricsLines?.map { line ->
        LyricLine(
            startMs = line.startTimeMs.toLongOrNull() ?: 0L,
            endMs = line.endTimeMs?.toLongOrNull() ?: 0L,
            text = line.words
        )
    } ?: emptyList()

    return Lyrics(
        songId = songId,
        lines = lines,
        isTimeSynced = lines.isNotEmpty() && hasTimestamps
    )
}

enum class SearchFilter(val param: String?) {
    ALL(null),
    SONGS("EgWKAQIIAWoKEAkQBRAKEAMQBA%3D%3D"),
    ALBUMS("EgWKAQIYAWoKEAkQChAFEAMQBA%3D%3D"),
    ARTISTS("EgWKAQIgAWoKEAkQChAFEAMQBA%3D%3D"),
    PLAYLISTS("EgeKAQQoAEABagoQAxAEEAoQCRAF")
}
