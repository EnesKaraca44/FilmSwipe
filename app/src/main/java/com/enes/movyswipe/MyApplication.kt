package com.enes.movyswipe

import android.app.Application
import android.util.Log
import com.enes.movyswipe.data.AppDatabase
import com.enes.movyswipe.data.repository.WatchlistRepository
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch


import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class MyApplication : Application() {

    // Bu 'lazy' tanımları doğru ve kalmalı.
    val database by lazy { AppDatabase.getDatabase(this) }
    val watchlistRepository by lazy { WatchlistRepository(database.watchlistDao()) }

    // --- EN KRİTİK DEĞİŞİKLİK BURADA ---
    override fun onCreate() {
        super.onCreate()
        // 'instance' değişkenini, 'onCreate' metodu içinde, yani Application nesnesi
        // tamamen oluşturulduktan SONRA atıyoruz.
        instance = this
    }

    companion object {
        // 'lateinit' yerine, başlangıçta 'null' olan bir değişken tanımlıyoruz.
        // Bu, 'UninitializedPropertyAccessException' hatasını önler.
        private var instance: MyApplication? = null

        // Bu fonksiyon, ViewModel'in 'instance'a güvenli bir şekilde erişmesini sağlar.
        fun getInstance(): MyApplication {
            // Eğer instance null ise, bu bir geliştirme hatasıdır ve uygulamanın
            // çökmesi gerekir ki sorunu fark edelim. Ama normal akışta null olmayacak.
            return instance!!
        }
    }
    // ------------------------------------
}