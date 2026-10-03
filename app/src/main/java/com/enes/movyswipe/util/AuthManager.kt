package com.enes.movyswipe.util

import android.content.Context
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.tasks.await

object AuthManager {

    private const val PREFS_NAME = "FilmSwipePrefs"
    private const val KEY_NICKNAME = "user_nickname" // YENİ ANAHTAR

    private val auth: FirebaseAuth = Firebase.auth
    private var currentUserId: String? = null

    fun getUserNickname(context: Context): String {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        var nickname = prefs.getString(KEY_NICKNAME, null)

        // Eğer hafızada kayıtlı bir kullanıcı adı yoksa...
        if (nickname == null) {
            val adjectives = listOf("Hızlı", "Gizemli", "Neşeli", "Cesur", "Uykucu", "Meraklı")
            val nouns = listOf("Aslan", "Kaplan", "Kartal", "Panda", "Tilki", "Gezgin")

            // Bu listelerden rastgele bir sıfat ve bir isim seçip birleştirelim.
            nickname = "${adjectives.random()} ${nouns.random()}"

            // Bu yeni kullanıcı adını, gelecekte kullanmak üzere diske kaydediyoruz.
            prefs.edit().putString(KEY_NICKNAME, nickname).apply()
        }
        return nickname
    }

    // Yeni fonksiyon: Kullanıcı adını kaydeder
    fun saveUserNickname(context: Context, nickname: String) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_NICKNAME, nickname).apply()
    }

    // Yeni fonksiyon: Bir kullanıcı adı daha önce kaydedilmiş mi diye kontrol eder.
    fun isNicknameSet(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.contains(KEY_NICKNAME)
    }

    /**
     * Kullanıcının oturumunu kapatır ve tüm yerel verilerini (kullanıcı adı) temizler.
     */
    fun signOut(context: Context) {
        // 1. Firebase Authentication'dan çıkış yap.
        Firebase.auth.signOut()

        // 2. SharedPreferences'ten tüm kayıtlı verileri sil.
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().clear().apply()

        // 3. Hafızada tuttuğumuz değişkenleri sıfırla.
        currentUserId = null
        // Eğer nickname'i de hafızada tutan bir değişken varsa, onu da null yap.
    }

    /**
     * Mevcut kullanıcının benzersiz Firebase UID'sini döndürür.
     * Eğer kullanıcı giriş yapmamışsa, arkaplanda anonim olarak giriş yapar.
     * Bu bir suspend fonksiyonudur çünkü ağ işlemi gerektirebilir.
     */
    suspend fun getCurrentUserId(): String {
        // Eğer ID'yi daha önce aldıysak, tekrar istemeye gerek yok.
        if (currentUserId != null) {
            return currentUserId!!
        }

        // Mevcut bir kullanıcı var mı diye kontrol et.
        val firebaseUser = auth.currentUser
        if (firebaseUser != null) {
            // Varsa, onun UID'sini al ve döndür.
            currentUserId = firebaseUser.uid
            return firebaseUser.uid
        } else {
            // Yoksa (uygulama ilk kez açılıyor veya kullanıcı çıkış yapmış),
            // arkaplanda anonim olarak yeni bir kullanıcı oluştur ve giriş yap.
            return try {
                val authResult = auth.signInAnonymously().await()
                val newUserId = authResult.user?.uid
                if (newUserId != null) {
                    currentUserId = newUserId
                    newUserId
                } else {
                    // Çok nadir bir hata durumu
                    "error_user_null"
                }
            } catch (e: Exception) {
                // İnternet yoksa veya başka bir hata olursa
                println("Anonim giriş başarısız: ${e.message}")
                "error_auth_failed"
            }
        }
    }
}