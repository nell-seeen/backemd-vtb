package com.musicstream.download

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.*
import com.musicstream.data.local.MusicDatabase
import com.musicstream.data.local.entity.DownloadEntity
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream

/**
 * DownloadWorker — downloads a single song to the device's music directory.
 *
 * Runs on a background thread via WorkManager.
 * Reports progress (0.0 – 1.0) via setProgressAsync.
 */
@HiltWorker
class DownloadWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val db: MusicDatabase,
    private val okHttpClient: OkHttpClient
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val songId = inputData.getString(KEY_SONG_ID) ?: return@withContext Result.failure()
        val title = inputData.getString(KEY_TITLE) ?: return@withContext Result.failure()
        val artist = inputData.getString(KEY_ARTIST) ?: ""
        val album = inputData.getString(KEY_ALBUM) ?: ""
        val thumbnail = inputData.getString(KEY_THUMBNAIL) ?: ""
        val streamUrl = inputData.getString(KEY_STREAM_URL) ?: return@withContext Result.failure()

        val outputDir = File(applicationContext.getExternalFilesDir(null), "Music").also { it.mkdirs() }
        val outputFile = File(outputDir, "$songId.m4a")

        try {
            val request = Request.Builder().url(streamUrl).build()
            val response = okHttpClient.newCall(request).execute()

            if (!response.isSuccessful) return@withContext Result.failure()

            val body = response.body ?: return@withContext Result.failure()
            val contentLength = body.contentLength()

            body.byteStream().use { input ->
                FileOutputStream(outputFile).use { output ->
                    val buffer = ByteArray(8 * 1024)
                    var bytesRead = 0L
                    var bytes: Int
                    while (input.read(buffer).also { bytes = it } != -1) {
                        output.write(buffer, 0, bytes)
                        bytesRead += bytes
                        val progress = if (contentLength > 0) bytesRead.toFloat() / contentLength else 0f
                        setProgress(workDataOf(KEY_PROGRESS to progress))
                    }
                }
            }

            db.downloadDao().insertDownload(
                DownloadEntity(
                    songId = songId,
                    title = title,
                    artistName = artist,
                    albumTitle = album,
                    thumbnailUrl = thumbnail,
                    localPath = outputFile.absolutePath,
                    fileSizeBytes = outputFile.length()
                )
            )

            Result.success()
        } catch (e: Exception) {
            outputFile.delete()
            Result.failure()
        }
    }

    companion object {
        const val KEY_SONG_ID = "song_id"
        const val KEY_TITLE = "title"
        const val KEY_ARTIST = "artist"
        const val KEY_ALBUM = "album"
        const val KEY_THUMBNAIL = "thumbnail"
        const val KEY_STREAM_URL = "stream_url"
        const val KEY_PROGRESS = "progress"
        const val TAG_DOWNLOAD = "music_download"
    }
}
