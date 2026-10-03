package com.enes.movyswipe.data.repository

import com.enes.movyswipe.data.Movie
import com.enes.movyswipe.data.Result
import com.enes.movyswipe.network.ApiService
import com.enes.movyswipe.network.RetrofitClient
import com.enes.movyswipe.network.RetrofitClient.apiService


import javax.inject.Inject

class MovieRepository @Inject constructor(
    private val apiService: ApiService
) {
    private val apiKey = com.enes.movyswipe.BuildConfig.TMDB_API_KEY

    /**
     * Verilen filtrelere göre TMDB'den film listesi çeker.
     * Eğer filtre verilmezse, en popüler filmleri getirir.
     * @param customFilters "with_genres", "primary_release_year.gte" gibi anahtarlar içeren bir Map.
     * @return Bulunan filmlerin bir listesini döndürür.
     */
    suspend fun getMovies(customFilters: Map<String, String> ):  Result<List<Movie>>  {
        // Varsayılan parametreleri bir Map olarak hazırlıyoruz.
        val defaultParams = mutableMapOf(
            "api_key" to apiKey,
            "language" to "tr-TR",
            "sort_by" to "popularity.desc"
        )

        // Gelen özel filtreleri de bu Map'e ekliyoruz.
        // Eğer aynı anahtar varsa, özel filtre varsayılanın üzerine yazar.
        defaultParams.putAll(customFilters)

        return try {
            val response = apiService.getDiscoverMovies(
                apiKey = apiKey,
                options = customFilters
            )
            if (response.isSuccessful && response.body() != null) {
                // Başarılı: Veriyi Success paketiyle gönder.
                Result.Success(response.body()!!.movies)
            } else {
                // Başarısız (API hatası): Hatayı Error paketiyle gönder.
                Result.Error(Exception("API'den hata kodu alındı: ${response.code()}"))
            }
        } catch (e: Exception) {
            // Başarısız (Ağ hatası): Hatayı Error paketiyle gönder.
            Result.Error(e)
        }
    }

    suspend fun getMovieDetails(movieId: Int): Result<Movie> {
        return try {
            val response = apiService.getMovieDetails(
                movieId = movieId,
                apiKey = apiKey
            )
            if (response.isSuccessful && response.body() != null) {
                // Başarılı: Veriyi Success paketiyle gönder.
                Result.Success(response.body()!!)
            } else {
                // Başarısız (API hatası): Hatayı Error paketiyle gönder.
                Result.Error(Exception("Film detayı alınamadı: ${response.code()}"))
            }
        } catch (e: Exception) {
            // Başarısız (Ağ hatası): Hatayı Error paketiyle gönder.
            Result.Error(e)
        }
    }
}