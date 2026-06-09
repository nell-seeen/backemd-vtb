package com.musicstream.domain.usecase

import com.musicstream.domain.model.DownloadedSong
import com.musicstream.domain.model.Song
import com.musicstream.download.DownloadManager
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * DownloadSongUseCase — downloads or deletes a song for offline playback.
 */
class DownloadSongUseCase @Inject constructor(
    private val downloadManager: DownloadManager
) {
    suspend fun download(song: Song) = downloadManager.enqueueSong(song)
    suspend fun cancel(songId: String) = downloadManager.cancelDownload(songId)
    suspend fun delete(songId: String) = downloadManager.deleteDownload(songId)
    fun isDownloaded(songId: String): Flow<Boolean> = downloadManager.isDownloaded(songId)
    fun getDownloads(): Flow<List<DownloadedSong>> = downloadManager.getDownloads()
}
