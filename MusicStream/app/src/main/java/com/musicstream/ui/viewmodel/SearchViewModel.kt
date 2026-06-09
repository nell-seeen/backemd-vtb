package com.musicstream.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.musicstream.data.api.ApiResult
import com.musicstream.data.api.SearchFilter
import com.musicstream.data.repository.SearchRepository
import com.musicstream.domain.model.SearchResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SearchUiState(
    val query: String = "",
    val filter: SearchFilter = SearchFilter.ALL,
    val result: SearchResult = SearchResult(),
    val suggestions: List<String> = emptyList(),
    val history: List<String> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)

@OptIn(FlowPreview::class)
@HiltViewModel
class SearchViewModel @Inject constructor(
    private val searchRepository: SearchRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(SearchUiState())
    val uiState: StateFlow<SearchUiState> = _uiState.asStateFlow()

    init {
        // Load history immediately
        viewModelScope.launch {
            searchRepository.getSearchHistory().collect { history ->
                _uiState.update { it.copy(history = history) }
            }
        }

        // Debounced suggestion fetch
        _uiState
            .map { it.query }
            .debounce(300)
            .distinctUntilChanged()
            .onEach { query ->
                if (query.length >= 2) fetchSuggestions(query)
                else _uiState.update { it.copy(suggestions = emptyList()) }
            }
            .launchIn(viewModelScope)
    }

    fun setQuery(query: String) = _uiState.update { it.copy(query = query, error = null) }

    fun setFilter(filter: SearchFilter) {
        _uiState.update { it.copy(filter = filter) }
        if (_uiState.value.query.isNotBlank()) search()
    }

    fun search() {
        val query = _uiState.value.query.trim()
        if (query.isBlank()) return
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null, suggestions = emptyList()) }
            when (val result = searchRepository.search(query, _uiState.value.filter)) {
                is ApiResult.Success -> _uiState.update { it.copy(isLoading = false, result = result.data) }
                is ApiResult.Error -> _uiState.update { it.copy(isLoading = false, error = result.message) }
                else -> {}
            }
        }
    }

    fun removeFromHistory(query: String) = viewModelScope.launch {
        searchRepository.removeFromHistory(query)
    }

    fun clearHistory() = viewModelScope.launch { searchRepository.clearHistory() }

    private fun fetchSuggestions(query: String) = viewModelScope.launch {
        when (val result = searchRepository.getSuggestions(query)) {
            is ApiResult.Success -> _uiState.update { it.copy(suggestions = result.data) }
            else -> {}
        }
    }
}
