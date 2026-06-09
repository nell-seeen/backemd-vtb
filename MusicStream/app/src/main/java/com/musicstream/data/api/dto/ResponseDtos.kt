package com.musicstream.data.api.dto

import com.google.gson.annotations.SerializedName

// ──────────────────────────────────────────
//  Response DTOs — maps raw API JSON
// ──────────────────────────────────────────

data class SearchResponse(
    val contents: SearchContents?
)

data class SearchContents(
    @SerializedName("tabbedSearchResultsRenderer")
    val tabbedSearchResultsRenderer: TabbedSearchResultsRenderer?
)

data class TabbedSearchResultsRenderer(
    val tabs: List<Tab>?
)

data class Tab(
    val tabRenderer: TabRenderer?
)

data class TabRenderer(
    val content: TabContent?
)

data class TabContent(
    val sectionListRenderer: SectionListRenderer?
)

data class SectionListRenderer(
    val contents: List<SectionContent>?
)

data class SectionContent(
    val musicShelfRenderer: MusicShelfRenderer?
)

data class MusicShelfRenderer(
    val title: Runs?,
    val contents: List<MusicShelfContent>?
)

data class MusicShelfContent(
    val musicResponsiveListItemRenderer: MusicResponsiveListItemRenderer?
)

data class MusicResponsiveListItemRenderer(
    @SerializedName("flexColumns") val flexColumns: List<FlexColumn>?,
    val thumbnail: RendererThumbnail?,
    val overlay: Overlay?,
    val navigationEndpoint: NavigationEndpoint?
)

data class FlexColumn(
    val musicResponsiveListItemFlexColumnRenderer: FlexColumnRenderer?
)

data class FlexColumnRenderer(
    val text: Runs?
)

data class Runs(
    val runs: List<Run>?
) {
    fun text(): String = runs?.joinToString("") { it.text } ?: ""
}

data class Run(
    val text: String,
    val navigationEndpoint: NavigationEndpoint? = null
)

data class NavigationEndpoint(
    @SerializedName("watchEndpoint") val watchEndpoint: WatchEndpoint?,
    @SerializedName("browseEndpoint") val browseEndpoint: BrowseEndpoint?
)

data class WatchEndpoint(
    @SerializedName("videoId") val videoId: String?,
    @SerializedName("playlistId") val playlistId: String?
)

data class BrowseEndpoint(
    @SerializedName("browseId") val browseId: String?,
    val params: String?
)

data class RendererThumbnail(
    @SerializedName("musicThumbnailRenderer")
    val musicThumbnailRenderer: MusicThumbnailRenderer?
)

data class MusicThumbnailRenderer(
    val thumbnail: Thumbnail?
)

data class Thumbnail(
    val thumbnails: List<ThumbnailItem>?
) {
    fun best(): String = thumbnails?.maxByOrNull { it.width ?: 0 }?.url ?: ""
    fun first(): String = thumbnails?.firstOrNull()?.url ?: ""
}

data class ThumbnailItem(
    val url: String,
    val width: Int?,
    val height: Int?
)

data class Overlay(
    val musicItemThumbnailOverlayRenderer: MusicItemThumbnailOverlayRenderer?
)

data class MusicItemThumbnailOverlayRenderer(
    val content: OverlayContent?
)

data class OverlayContent(
    val musicPlayButtonRenderer: MusicPlayButtonRenderer?
)

data class MusicPlayButtonRenderer(
    val playNavigationEndpoint: NavigationEndpoint?
)

// ── Browse Response ──

data class BrowseResponse(
    val header: BrowseHeader?,
    val contents: BrowseContents?
)

data class BrowseHeader(
    val musicImmersiveHeaderRenderer: MusicImmersiveHeaderRenderer?,
    val musicDetailHeaderRenderer: MusicDetailHeaderRenderer?
)

data class MusicImmersiveHeaderRenderer(
    val title: Runs?,
    val thumbnail: RendererThumbnail?,
    val subscriptionButton: Any?
)

data class MusicDetailHeaderRenderer(
    val title: Runs?,
    val subtitle: Runs?,
    val thumbnail: RendererThumbnail?
)

data class BrowseContents(
    val singleColumnBrowseResultsRenderer: SingleColumnBrowseResultsRenderer?,
    val twoColumnBrowseResultsRenderer: TwoColumnBrowseResultsRenderer?
)

data class SingleColumnBrowseResultsRenderer(
    val tabs: List<Tab>?
)

data class TwoColumnBrowseResultsRenderer(
    val firstColumn: Any?,
    val secondColumn: Any?
)

// ── Player Response ──

data class PlayerResponse(
    val videoDetails: VideoDetails?,
    val streamingData: StreamingData?
)

data class VideoDetails(
    @SerializedName("videoId") val videoId: String,
    val title: String,
    val author: String,
    @SerializedName("lengthSeconds") val lengthSeconds: String,
    val thumbnail: Thumbnail?
)

data class StreamingData(
    val expiresInSeconds: String,
    val formats: List<StreamFormat>?,
    @SerializedName("adaptiveFormats") val adaptiveFormats: List<StreamFormat>?
)

data class StreamFormat(
    val itag: Int,
    val url: String?,
    val mimeType: String,
    val bitrate: Int,
    @SerializedName("contentLength") val contentLength: String?,
    @SerializedName("approxDurationMs") val approxDurationMs: String?
)

// ── Next Response ──

data class NextResponse(
    val contents: NextContents?
)

data class NextContents(
    @SerializedName("singleColumnMusicWatchNextResultsRenderer")
    val singleColumnMusicWatchNextResultsRenderer: SingleColumnMusicWatchNextResultsRenderer?
)

data class SingleColumnMusicWatchNextResultsRenderer(
    val tabbedRenderer: TabbedRenderer?
)

data class TabbedRenderer(
    @SerializedName("watchNextTabbedResultsRenderer")
    val watchNextTabbedResultsRenderer: WatchNextTabbedResultsRenderer?
)

data class WatchNextTabbedResultsRenderer(
    val tabs: List<Tab>?
)

// ── Suggestions Response ──

data class SuggestionsResponse(
    val contents: List<SuggestionContent>?
)

data class SuggestionContent(
    @SerializedName("searchSuggestionsSectionRenderer")
    val sectionRenderer: SuggestionSectionRenderer?
)

data class SuggestionSectionRenderer(
    val contents: List<SuggestionItem>?
)

data class SuggestionItem(
    @SerializedName("searchSuggestionRenderer")
    val renderer: SuggestionRenderer?
)

data class SuggestionRenderer(
    val suggestion: Runs?
)

// ── Lyrics Response ──

data class LyricsResponse(
    val lyrics: LyricsData?,
    val hasTimestamps: Boolean = false
)

data class LyricsData(
    val lyricsLines: List<LyricLineDto>?,
    val plainLyrics: String?
)

data class LyricLineDto(
    @SerializedName("startTimeMs") val startTimeMs: String,
    @SerializedName("endTimeMs") val endTimeMs: String?,
    val words: String
)
