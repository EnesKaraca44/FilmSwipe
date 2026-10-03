package com.enes.movyswipe.data

import com.google.gson.annotations.SerializedName


data class MovieResponse(
    @SerializedName( "page")
    val page: Int,

    @SerializedName( "results")
    val movies: List<Movie> // En önemli kısım: filmlerin bir listesi
)