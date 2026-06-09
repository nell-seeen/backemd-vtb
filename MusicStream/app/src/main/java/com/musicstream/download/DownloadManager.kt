package com.musicstream.download

import android.content.Context
import androidx.work.*
import com.musicstream.data.api.ApiResult
import com.musicstream.data.local.MusicDatabase
import com.musicstream.data.local.entity.DownloadEntity
import com.musicstream.data.repository.MusicRepository
import com.musicstream.domain.model.DownloadProgress
import com.musicstream.domain.model.DownloadStatus
import com.musicstream.domain.model.Song
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.*
import javax.inject.Inject
import javax.inject.Singleton

/**
 * DownloadManager — schedules, tracks, and cancels downloads using WorkManager.
 *
 * Architecture:
 *   UI  →  DownloadManager  →  WorkManager  →  DownloadWorker  →  DB
 *
 * Each download is a unique OneTimeWorkRequest tagged with the song ID.
 * Progress is observed via WorkManager's LiveData converted to Flow.
 */
@Singleton
class DownloadManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val db: MusicDatabase,
    private val musicRepository: MusicRepository
) {
    private val workManager = WorkManager.getInstance(context)

    fun getDownloads(): Flow<List<com.musicstream.domain.model.DownloadedSong>> =
        db.downloadDao().getAllDownloads().map { list ->
            list.map { entity ->
                com.musicstream.domain.model.DownloadedSong(
                    songId = entity.songId,
                    title = entity.title,
                    artistName = entity.artistName,
                    albumTitle = entity.albumTitle,
                    thumbnailUrl = entity.thumbnailUrl,
                    localPath = entity.localPath,
                    downloadedAt = entity.downloadedAt,
                    fileSizeBytes = entity.fileSizeBytes
                )
            }
        }

    fun isDownloaded(songId: String): Flow<Boolean> = db.downloadDao().isDownloaded(songId)

    fun getDownloadProgress(songId: String): Flow<DownloadProgress> =
        workManager.getWorkInfosByTagFlow(songId)
            .map { workInfos ->
                val info = workInfos.firstOrNull()
                when (info?.state) {
                    WorkInfo.State.RUNNING -> {
                        val progress = info.progress.getFloat(DownloadWorker.KEY_PROGRESS, 0f)
                        DownloadProgress(songId, progress, DownloadStatus.DOWNLOADING)
                    }
                    WorkInfo.State.SUCCEEDED -> DownloadProgress(songId, 1f, DownloadStatus.COMPLETED)
                    WorkInfo.State.FAILED -> DownloadProgress(songId, 0f, DownloadStatus.FAILED)
                    WorkInfo.State.CANCELLED -> DownloadProgress(songId, 0f, DownloadStatus.CANCELLED)
                    WorkInfo.State.ENQUEUED -> DownloadProgress(songId, 0f, DownloadStatus.QUEUED)
                    else -> DownloadProgress(songId, 0f, DownloadStatus.QUEUED)
                }
            }

    suspend fun enqueueSong(song: Song) {
        val streamResult = musicRepository.getStreamInfo(song.id)
        if (streamResult !is ApiResult.Success) return

        val streamUrl = streamResult.data.streamUrl

        val inputData = workDataOf(
            DownloadWorker.KEY_SONG_ID to song.id,
            DownloadWorker.KEY_TITLE to song.title,
            DownloadWorker.KEY_ARTIST to song.artistName,
            DownloadWorker.KEY_ALBUM to song.albumTitle,
            DownloadWorker.KEY_THUMBNAIL to song.thumbnailUrl,
            DownloadWorker.KEY_STREAM_URL to streamUrl
        )

        val request = OneTimeWorkRequestBuilder<DownloadWorker>()
            .setInputData(inputData)
            .addTag(song.id)
            .addTag(DownloadWorker.TAG_DOWNLOAD)
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build()
            )
            .build()

        workManager.enqueueUniqueWork(
            "download_${song.id}",
            ExistingWorkPolicy.KEEP,
            request
        )
    }

    suspend fun cancelDownload(songId: String) {
        workManager.cancelUniqueWork("download_$songId")
    }

    suspend fun deleteDownload(songId: String) {
        val entity = db.downloadDao().getDownload(songId)
        if (entity != null) {
            val file = java.io.File(entity.localPath)
            if (file.exists()) file.delete()
            db.downloadDao().deleteDownload(songId)
        }
    }
}
