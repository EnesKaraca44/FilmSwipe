package com.enes.movyswipe.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import kotlinx.coroutines.flow.Flow
import androidx.room.Query

@Dao
interface WatchlistDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMovie(movie: WatchlistMovie)

    @Delete
    suspend fun deleteMovie(movie: WatchlistMovie)

    @Query("SELECT * FROM watchlist_movies ORDER BY addedTimestamp DESC")
    fun getAllMovies(): Flow<List<WatchlistMovie>>

    @Query("DELETE FROM watchlist_movies")
    suspend fun clearWatchlist()
}