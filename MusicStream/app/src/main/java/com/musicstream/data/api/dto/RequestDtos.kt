package com.musicstream.data.api.dto

import com.google.gson.annotations.SerializedName

// ──────────────────────────────────────────
//  Request DTOs
// ──────────────────────────────────────────

data class SearchRequest(
    val context: Context = Context.default(),
    val query: String,
    val params: String? = null   // optional filter param
)

data class BrowseRequest(
    val context: Context = Context.default(),
    @SerializedName("browseId") val browseId: String,
    val params: String? = null
)

data class PlayerRequest(
    val context: Context = Context.default(),
    @SerializedName("videoId") val videoId: String,
    @SerializedName("playlistId") val playlistId: String? = null
)

data class NextRequest(
    val context: Context = Context.default(),
    @SerializedName("videoId") val videoId: String,
    @SerializedName("playlistId") val playlistId: String? = null,
    val index: Int = 0,
    val params: String? = null
)

data class SuggestionsRequest(
    val context: Context = Context.default(),
    val input: String
)

data class Context(
    val client: ClientInfo
) {
    companion object {
        fun default() = Context(
            client = ClientInfo(
                clientName = "WEB_REMIX",
                clientVersion = "1.20240101.01.00",
                hl = "en",
                gl = "US"
            )
        )
    }
}

data class ClientInfo(
    val clientName: String,
    val clientVersion: String,
    val hl: String,
    val gl: String
)
