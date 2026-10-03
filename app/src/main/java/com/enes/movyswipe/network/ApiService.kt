package com.enes.movyswipe.network

import com.enes.movyswipe.data.Movie
import com.enes.movyswipe.data.MovieResponse

import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query
import retrofit2.http.QueryMap

interface ApiService {

    // Popüler filmleri keşfetmek için bir endpoint
    @GET("discover/movie")
    suspend fun getDiscoverMovies(

        @Query("api_key") apiKey: String,
        @Query("language") language: String = "tr-TR",
        // Filtreleri (tür, yıl vb.) bir Map olarak alır.
        @QueryMap options: Map<String, String>

    ): Response<MovieResponse> // Dönen cevabı MovieResponse olarak parse et

    @GET("movie/{movie_id}")
    suspend fun getMovieDetails(
        // @Path, bu parametreyi URL'deki {movie_id} kısmına yerleştirir.
        @Path("movie_id") movieId: Int,
        @Query("api_key") apiKey: String,
        @Query("language") language: String = "tr-TR"
    ): Response<Movie> // Geriye sadece tek bir "Movie" nesnesi döndürür.

}