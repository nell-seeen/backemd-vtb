package com.musicstream.domain.usecase

import com.musicstream.domain.model.Song
import com.musicstream.player.PlayerController
import com.musicstream.recommendation.RecommendationEngine
import javax.inject.Inject

/**
 * PlaySongUseCase — orchestrates starting playback with optional radio auto-queue.
 */
class PlaySongUseCase @Inject constructor(
    private val playerController: PlayerController,
    private val recommendationEngine: RecommendationEngine
) {
    suspend operator fun invoke(
        song: Song,
        autoQueue: Boolean = true
    ) {
        if (autoQueue) {
            val recommendations = recommendationEngine.recommend(song.id, limit = 25)
            playerController.playQueue(listOf(song) + recommendations, 0)
        } else {
            playerController.playSong(song)
        }
    }
}
