package com.musicstream.player

import com.musicstream.domain.model.QueueItem
import com.musicstream.domain.model.RepeatMode
import com.musicstream.domain.model.Song
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * QueueManager — manages the playback queue with shuffle, repeat, and history.
 *
 * Architecture:
 *   - Holds the current ordered queue as a StateFlow.
 *   - Maintains an internal shuffle order separate from the display order.
 *   - Exposes prev/next navigation respecting repeat mode.
 */
@Singleton
class QueueManager @Inject constructor() {

    private val _queue = MutableStateFlow<List<QueueItem>>(emptyList())
    val queue: StateFlow<List<QueueItem>> = _queue.asStateFlow()

    private val _currentIndex = MutableStateFlow(-1)
    val currentIndex: StateFlow<Int> = _currentIndex.asStateFlow()

    private val _repeatMode = MutableStateFlow(RepeatMode.OFF)
    val repeatMode: StateFlow<RepeatMode> = _repeatMode.asStateFlow()

    private val _isShuffleEnabled = MutableStateFlow(false)
    val isShuffleEnabled: StateFlow<Boolean> = _isShuffleEnabled.asStateFlow()

    // Maintains shuffled index mapping: shuffledOrder[i] = real queue index
    private var shuffledOrder: List<Int> = emptyList()
    private var shufflePosition: Int = -1

    // ──────────────────────── Current Song ────────────────────────

    val currentSong: QueueItem?
        get() {
            val idx = _currentIndex.value
            return if (idx in _queue.value.indices) _queue.value[idx] else null
        }

    // ──────────────────────── Queue Operations ────────────────────────

    fun setQueue(songs: List<Song>, startIndex: Int = 0) {
        val items = songs.map { QueueItem(song = it, queueId = UUID.randomUUID().toString()) }
        _queue.value = items
        _currentIndex.value = startIndex.coerceIn(0, items.lastIndex.coerceAtLeast(0))
        if (_isShuffleEnabled.value) rebuildShuffleOrder()
    }

    fun addToQueue(song: Song) {
        val item = QueueItem(song = song, queueId = UUID.randomUUID().toString())
        _queue.value = _queue.value + item
        if (_isShuffleEnabled.value) rebuildShuffleOrder()
    }

    fun addNext(song: Song) {
        val item = QueueItem(song = song, queueId = UUID.randomUUID().toString())
        val current = _currentIndex.value
        val mutable = _queue.value.toMutableList()
        mutable.add((current + 1).coerceAtLeast(0), item)
        _queue.value = mutable
        if (_isShuffleEnabled.value) rebuildShuffleOrder()
    }

    fun removeAt(index: Int) {
        val mutable = _queue.value.toMutableList()
        if (index !in mutable.indices) return
        mutable.removeAt(index)
        _queue.value = mutable
        if (index < _currentIndex.value) {
            _currentIndex.value = (_currentIndex.value - 1).coerceAtLeast(0)
        }
        if (_isShuffleEnabled.value) rebuildShuffleOrder()
    }

    fun moveItem(from: Int, to: Int) {
        val mutable = _queue.value.toMutableList()
        if (from !in mutable.indices || to !in mutable.indices) return
        val item = mutable.removeAt(from)
        mutable.add(to, item)
        _queue.value = mutable
        // Update current index after move
        _currentIndex.value = when (_currentIndex.value) {
            from -> to
            in (minOf(from, to)..maxOf(from, to)) -> {
                if (from < to) _currentIndex.value - 1 else _currentIndex.value + 1
            }
            else -> _currentIndex.value
        }
    }

    fun clearQueue() {
        _queue.value = emptyList()
        _currentIndex.value = -1
        shuffledOrder = emptyList()
        shufflePosition = -1
    }

    // ──────────────────────── Navigation ────────────────────────

    fun hasNext(): Boolean {
        return when (_repeatMode.value) {
            RepeatMode.ALL -> _queue.value.isNotEmpty()
            RepeatMode.ONE -> true
            RepeatMode.OFF -> {
                if (_isShuffleEnabled.value) shufflePosition < shuffledOrder.lastIndex
                else _currentIndex.value < _queue.value.lastIndex
            }
        }
    }

    fun hasPrevious(): Boolean = _currentIndex.value > 0 || _repeatMode.value == RepeatMode.ALL

    fun goToNext(): QueueItem? {
        if (_queue.value.isEmpty()) return null
        return when (_repeatMode.value) {
            RepeatMode.ONE -> currentSong
            RepeatMode.ALL -> {
                if (_isShuffleEnabled.value) {
                    shufflePosition = (shufflePosition + 1) % shuffledOrder.size
                    _currentIndex.value = shuffledOrder[shufflePosition]
                } else {
                    _currentIndex.value = (_currentIndex.value + 1) % _queue.value.size
                }
                currentSong
            }
            RepeatMode.OFF -> {
                if (_isShuffleEnabled.value) {
                    if (shufflePosition >= shuffledOrder.lastIndex) return null
                    shufflePosition++
                    _currentIndex.value = shuffledOrder[shufflePosition]
                } else {
                    if (_currentIndex.value >= _queue.value.lastIndex) return null
                    _currentIndex.value++
                }
                currentSong
            }
        }
    }

    fun goToPrevious(): QueueItem? {
        if (_queue.value.isEmpty()) return null
        return when {
            _repeatMode.value == RepeatMode.ONE -> currentSong
            _isShuffleEnabled.value -> {
                shufflePosition = (shufflePosition - 1).coerceAtLeast(0)
                _currentIndex.value = shuffledOrder[shufflePosition]
                currentSong
            }
            else -> {
                _currentIndex.value = (_currentIndex.value - 1).coerceAtLeast(0)
                currentSong
            }
        }
    }

    fun jumpTo(index: Int): QueueItem? {
        if (index !in _queue.value.indices) return null
        _currentIndex.value = index
        if (_isShuffleEnabled.value) {
            shufflePosition = shuffledOrder.indexOf(index).coerceAtLeast(0)
        }
        return currentSong
    }

    // ──────────────────────── Repeat / Shuffle ────────────────────────

    fun toggleRepeat() {
        _repeatMode.value = when (_repeatMode.value) {
            RepeatMode.OFF -> RepeatMode.ALL
            RepeatMode.ALL -> RepeatMode.ONE
            RepeatMode.ONE -> RepeatMode.OFF
        }
    }

    fun toggleShuffle() {
        _isShuffleEnabled.value = !_isShuffleEnabled.value
        if (_isShuffleEnabled.value) rebuildShuffleOrder() else shuffledOrder = emptyList()
    }

    private fun rebuildShuffleOrder() {
        val currentIdx = _currentIndex.value
        val indices = (0 until _queue.value.size).toMutableList()
        indices.remove(currentIdx)
        indices.shuffle()
        if (currentIdx >= 0) indices.add(0, currentIdx)
        shuffledOrder = indices
        shufflePosition = 0
    }
}
