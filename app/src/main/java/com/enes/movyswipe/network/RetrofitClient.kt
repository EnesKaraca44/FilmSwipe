package com.enes.movyswipe.network




import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit

import retrofit2.converter.gson.GsonConverterFactory

object RetrofitClient {

    private const val BASE_URL = "https://api.themoviedb.org/3/"

    // Moshi (JSON dönüştürücü) nesnesini oluşturuyoruz
    //private val moshi = Moshi.Builder()
     //   .add(KotlinJsonAdapterFactory())
      //  .build()

    // Ağ isteklerini loglamak için bir interceptor oluşturuyoruz (Hata ayıklama için çok önemli!)
    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY // İstek ve cevapların tüm detaylarını göster
    }

    // Kendi HTTP istemcimizi oluşturuyoruz ve logging interceptor'ı ekliyoruz
    private val client = OkHttpClient.Builder()
        .addInterceptor(loggingInterceptor)
        .build()

    // Retrofit nesnesini tembel (lazy) bir şekilde oluşturuyoruz
    val apiService: ApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create()) // JSON'ı Moshi ile parse et
            .build()
            .create(ApiService::class.java)
    }
}