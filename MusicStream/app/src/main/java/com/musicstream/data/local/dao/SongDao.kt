package com.musicstream.data.local.dao

import androidx.room.*
import com.musicstream.data.local.entity.*
import kotlinx.coroutines.flow.Flow

@Dao
interface SongDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSong(song: SongEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSongs(songs: List<SongEntity>)

    @Query("SELECT * FROM songs WHERE id = :id")
    suspend fun getSongById(id: String): SongEntity?

    @Query("SELECT * FROM songs WHERE title LIKE '%' || :query || '%' OR artistName LIKE '%' || :query || '%'")
    suspend fun searchSongs(query: String): List<SongEntity>

    @Query("DELETE FROM songs WHERE id = :id")
    suspend fun deleteSong(id: String)

    // ── Liked Songs ──

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun likeSong(liked: LikedSongEntity)

    @Query("DELETE FROM liked_songs WHERE songId = :songId")
    suspend fun unlikeSong(songId: String)

    @Query("SELECT EXISTS(SELECT 1 FROM liked_songs WHERE songId = :songId)")
    fun isLiked(songId: String): Flow<Boolean>

    @Query("""
        SELECT s.* FROM songs s
        INNER JOIN liked_songs l ON s.id = l.songId
        ORDER BY l.likedAt DESC
    """)
    fun getLikedSongs(): Flow<List<SongEntity>>

    // ── Stream Cache ──

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun cacheStreamUrl(cache: StreamCacheEntity)

    @Query("SELECT * FROM stream_cache WHERE songId = :songId AND expireTimestamp > :now")
    suspend fun getCachedStream(songId: String, now: Long = System.currentTimeMillis()): StreamCacheEntity?

    @Query("DELETE FROM stream_cache WHERE expireTimestamp < :now")
    suspend fun clearExpiredStreams(now: Long = System.currentTimeMillis())
}

@Dao
interface AlbumDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAlbum(album: AlbumEntity)

    @Query("SELECT * FROM albums WHERE id = :id")
    suspend fun getAlbum(id: String): AlbumEntity?

    @Query("SELECT * FROM albums WHERE artistId = :artistId")
    suspend fun getAlbumsByArtist(artistId: String): List<AlbumEntity>

    @Query("DELETE FROM albums WHERE cachedAt < :olderThan")
    suspend fun evictOldAlbums(olderThan: Long)
}

@Dao
interface ArtistDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertArtist(artist: ArtistEntity)

    @Query("SELECT * FROM artists WHERE id = :id")
    suspend fun getArtist(id: String): ArtistEntity?
}

@Dao
interface PlaylistDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlaylist(playlist: PlaylistEntity)

    @Query("SELECT * FROM playlists WHERE isLocal = 1 ORDER BY createdAt DESC")
    fun getLocalPlaylists(): Flow<List<PlaylistEntity>>

    @Query("SELECT * FROM playlists WHERE id = :id")
    suspend fun getPlaylist(id: String): PlaylistEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun addSongToPlaylist(crossRef: PlaylistSongCrossRef)

    @Query("DELETE FROM playlist_songs WHERE playlistId = :playlistId AND songId = :songId")
    suspend fun removeSongFromPlaylist(playlistId: String, songId: String)

    @Query("""
        SELECT s.* FROM songs s
        INNER JOIN playlist_songs ps ON s.id = ps.songId
        WHERE ps.playlistId = :playlistId
        ORDER BY ps.position ASC
    """)
    fun getPlaylistSongs(playlistId: String): Flow<List<SongEntity>>

    @Query("DELETE FROM playlists WHERE id = :id")
    suspend fun deletePlaylist(id: String)

    @Query("DELETE FROM playlist_songs WHERE playlistId = :id")
    suspend fun deletePlaylistSongs(id: String)

    @Query("SELECT MAX(position) FROM playlist_songs WHERE playlistId = :playlistId")
    suspend fun getMaxPosition(playlistId: String): Int?
}

@Dao
interface DownloadDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDownload(download: DownloadEntity)

    @Query("SELECT * FROM downloads ORDER BY downloadedAt DESC")
    fun getAllDownloads(): Flow<List<DownloadEntity>>

    @Query("SELECT * FROM downloads WHERE songId = :songId")
    suspend fun getDownload(songId: String): DownloadEntity?

    @Query("DELETE FROM downloads WHERE songId = :songId")
    suspend fun deleteDownload(songId: String)

    @Query("SELECT EXISTS(SELECT 1 FROM downloads WHERE songId = :songId)")
    fun isDownloaded(songId: String): Flow<Boolean>
}

@Dao
interface SearchHistoryDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertQuery(query: SearchHistoryEntity)

    @Query("SELECT * FROM search_history ORDER BY searchedAt DESC LIMIT 10")
    fun getRecentSearches(): Flow<List<SearchHistoryEntity>>

    @Query("DELETE FROM search_history WHERE query = :query")
    suspend fun removeQuery(query: String)

    @Query("DELETE FROM search_history")
    suspend fun clearAll()
}
