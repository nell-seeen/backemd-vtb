package com.musicstream.data.repository

import com.musicstream.data.api.ApiResult
import com.musicstream.data.api.InnertubeApi
import com.musicstream.data.api.SearchFilter
import com.musicstream.data.local.MusicDatabase
import com.musicstream.data.local.entity.SearchHistoryEntity
import com.musicstream.domain.model.SearchResult
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SearchRepository @Inject constructor(
    private val api: InnertubeApi,
    private val db: MusicDatabase
) {
    suspend fun search(query: String, filter: SearchFilter = SearchFilter.ALL): ApiResult<SearchResult> {
        saveToHistory(query)
        return api.search(query, filter)
    }

    suspend fun getSuggestions(input: String): ApiResult<List<String>> =
        api.getSearchSuggestions(input)

    fun getSearchHistory(): Flow<List<String>> =
        db.searchHistoryDao().getRecentSearches()
            .map { list -> list.map { it.query } }

    suspend fun removeFromHistory(query: String) =
        db.searchHistoryDao().removeQuery(query)

    suspend fun clearHistory() = db.searchHistoryDao().clearAll()

    private suspend fun saveToHistory(query: String) {
        if (query.isBlank()) return
        db.searchHistoryDao().insertQuery(SearchHistoryEntity(query = query.trim()))
    }
}
