package com.musicstream.data.api

import com.musicstream.data.api.dto.*
import retrofit2.http.*

/**
 * Retrofit interface for the Innertube-compatible music API.
 * Uses YouTube Music's internal API as the backend source.
 */
interface MusicApiService {

    @POST("search")
    suspend fun search(
        @Body body: SearchRequest
    ): SearchResponse

    @POST("browse")
    suspend fun browse(
        @Body body: BrowseRequest
    ): BrowseResponse

    @POST("player")
    suspend fun getPlayer(
        @Body body: PlayerRequest
    ): PlayerResponse

    @POST("next")
    suspend fun getNext(
        @Body body: NextRequest
    ): NextResponse

    @POST("music/get_search_suggestions")
    suspend fun getSearchSuggestions(
        @Body body: SuggestionsRequest
    ): SuggestionsResponse

    @GET("lyrics")
    suspend fun getLyrics(
        @Query("browseId") browseId: String
    ): LyricsResponse
}
