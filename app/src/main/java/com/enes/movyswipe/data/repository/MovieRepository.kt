package com.enes.movyswipe.data.repository

import com.enes.movyswipe.BuildConfig
import com.enes.movyswipe.data.Movie
import com.enes.movyswipe.data.Result
import com.enes.movyswipe.network.ApiService
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MovieRepository @Inject constructor(
    private val apiService: ApiService
) {
    private val apiKey = BuildConfig.TMDB_API_KEY

    /** Verilen filtrelere göre TMDB'den film listesi çeker. Başlığı olmayan kayıtlar elenir. */
    suspend fun getMovies(filters: Map<String, String>): Result<List<Movie>> {
        return try {
            val response = apiService.getDiscoverMovies(apiKey = apiKey, options = filters)
            val body = response.body()
            if (response.isSuccessful && body != null) {
                Result.Success(body.movies.orEmpty().filter { !it.title.isNullOrBlank() })
            } else {
                Result.Error(Exception("Filmler alınamadı (HTTP ${response.code()})"))
            }
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    suspend fun getMovieDetails(movieId: Int): Result<Movie> {
        return try {
            val response = apiService.getMovieDetails(movieId = movieId, apiKey = apiKey)
            val body = response.body()
            if (response.isSuccessful && body != null) {
                Result.Success(body)
            } else {
                Result.Error(Exception("Film detayı alınamadı (HTTP ${response.code()})"))
            }
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    /** Birden çok filmi aynı anda (en fazla 6 paralel istek) çeker; sırayı korur, hatalıları atlar. */
    suspend fun getMovieDetailsList(ids: List<Int>): List<Movie> = coroutineScope {
        val limit = Semaphore(6)
        ids.map { id -> async { limit.withPermit { getMovieDetails(id) } } }
            .awaitAll()
            .mapNotNull { (it as? Result.Success)?.data }
    }
}
