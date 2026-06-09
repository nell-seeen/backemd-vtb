package com.musicstream.cache

import android.content.Context
import com.musicstream.data.local.MusicDatabase
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * CacheManager — manages HTTP cache and DB cache eviction.
 *
 * Architecture:
 *   - OkHttp disk cache (network responses)
 *   - Room DB entries with TTL-based eviction
 *   - Coil image cache (handled automatically by Coil)
 */
@Singleton
class CacheManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val db: MusicDatabase
) {
    companion object {
        const val HTTP_CACHE_DIR = "http_cache"
        const val HTTP_CACHE_SIZE = 50L * 1024 * 1024   // 50 MB
        const val IMAGE_CACHE_SIZE = 100L * 1024 * 1024 // 100 MB (handled by Coil)
        val ALBUM_CACHE_TTL = 7 * 24 * 60 * 60 * 1000L // 7 days
    }

    val httpCacheDir: File get() = File(context.cacheDir, HTTP_CACHE_DIR)

    /** Evict expired DB entries and clear stream URL cache. */
    suspend fun evictExpired() = withContext(Dispatchers.IO) {
        db.songDao().clearExpiredStreams()
        db.albumDao().evictOldAlbums(System.currentTimeMillis() - ALBUM_CACHE_TTL)
    }

    /** Get total cache size in bytes. */
    suspend fun getCacheSizeBytes(): Long = withContext(Dispatchers.IO) {
        var total = 0L
        total += httpCacheDir.walkTopDown().filter { it.isFile }.sumOf { it.length() }
        total += File(context.cacheDir, "image_cache").walkTopDown().filter { it.isFile }.sumOf { it.length() }
        total
    }

    /** Clear all caches. */
    suspend fun clearAll() = withContext(Dispatchers.IO) {
        httpCacheDir.deleteRecursively()
        httpCacheDir.mkdirs()
        db.songDao().clearExpiredStreams()
    }
}
