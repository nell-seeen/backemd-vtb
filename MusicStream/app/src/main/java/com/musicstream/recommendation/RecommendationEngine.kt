package com.musicstream.recommendation

import com.musicstream.data.api.ApiResult
import com.musicstream.data.local.MusicDatabase
import com.musicstream.data.local.toDomain
import com.musicstream.data.repository.MusicRepository
import com.musicstream.domain.model.Song
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * RecommendationEngine — generates personalised song recommendations.
 *
 * Strategies (in priority order):
 *  1. Radio seeds  — fetch "next" songs from the API using recently played songs.
 *  2. Liked songs  — mix in tracks similar to the user's liked songs.
 *  3. Offline mix  — if no network, shuffle from downloaded/liked songs.
 *
 * Architecture:
 *   PlayerController / ViewModel  →  RecommendationEngine  →  MusicRepository + DB
 */
@Singleton
class RecommendationEngine @Inject constructor(
    private val musicRepository: MusicRepository,
    private val db: MusicDatabase
) {
    /**
     * Return a list of recommended songs based on a seed song.
     * This is used to auto-queue the "radio" / "Up Next" feature.
     */
    suspend fun recommend(seedSongId: String, limit: Int = 20): List<Song> =
        withContext(Dispatchers.IO) {
            when (val result = musicRepository.getNextSongs(seedSongId)) {
                is ApiResult.Success -> result.data.take(limit)
                else -> fallbackRecommendations(limit)
            }
        }

    /**
     * Return a "Daily Mix" — blends liked songs and similar tracks.
     */
    suspend fun getDailyMix(limit: Int = 50): List<Song> = withContext(Dispatchers.IO) {
        val liked = db.songDao().getLikedSongsSnapshot()
        if (liked.isEmpty()) return@withContext emptyList()

        val seeds = liked.shuffled().take(3)
        val results = mutableListOf<Song>()

        for (seed in seeds) {
            when (val r = musicRepository.getNextSongs(seed.id)) {
                is ApiResult.Success -> results.addAll(r.data)
                else -> {}
            }
        }

        // Dedup and limit
        results.distinctBy { it.id }
            .filterNot { song -> liked.any { it.id == song.id } } // exclude already liked
            .shuffled()
            .take(limit)
    }

    /**
     * Return a radio playlist seeded from an artist.
     */
    suspend fun getArtistRadio(artistId: String, limit: Int = 30): List<Song> =
        withContext(Dispatchers.IO) {
            when (val result = musicRepository.getArtist(artistId)) {
                is ApiResult.Success -> {
                    val firstSong = result.data.songs.firstOrNull()
                        ?: return@withContext emptyList()
                    recommend(firstSong.id, limit)
                }
                else -> emptyList()
            }
        }

    private suspend fun fallbackRecommendations(limit: Int): List<Song> {
        val liked = db.songDao().getLikedSongsSnapshot()
        return liked.shuffled().take(limit)
    }
}

// Extension: synchronous snapshot for offline use
private suspend fun com.musicstream.data.local.dao.SongDao.getLikedSongsSnapshot(): List<Song> {
    return try {
        // Collect first emission from the Flow
        kotlinx.coroutines.flow.first(getLikedSongs()).map { it.toDomain() }
    } catch (e: Exception) {
        emptyList()
    }
}
