package com.enes.movyswipe.util

import android.content.Context
import android.util.Log
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.tasks.await

object AuthManager {

    private const val PREFS_NAME = "FilmSwipePrefs"
    private const val KEY_NICKNAME = "user_nickname"
    const val NICKNAME_MIN = 3
    const val NICKNAME_MAX = 20

    private val signInMutex = Mutex()

    fun getUserNickname(context: Context): String {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.getString(KEY_NICKNAME, null)?.let { return it }

        val adjectives = listOf("Hızlı", "Gizemli", "Neşeli", "Cesur", "Uykucu", "Meraklı")
        val nouns = listOf("Aslan", "Kaplan", "Kartal", "Panda", "Tilki", "Gezgin")
        val nickname = "${adjectives.random()} ${nouns.random()}"
        prefs.edit().putString(KEY_NICKNAME, nickname).apply()
        return nickname
    }

    /** Boşlukları temizler; geçersizse null döner. */
    fun normalizeNickname(raw: String): String? {
        val cleaned = raw.trim().replace(Regex("\\s+"), " ")
        return cleaned.takeIf { it.length in NICKNAME_MIN..NICKNAME_MAX }
    }

    fun saveUserNickname(context: Context, nickname: String) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit().putString(KEY_NICKNAME, nickname).apply()
    }

    fun isNicknameSet(context: Context): Boolean =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).contains(KEY_NICKNAME)

    /**
     * Oturumu kapatır ve kullanıcı adını siler. Satın alımlar ayrı bir dosyada tutulduğu
     * (ve Google Play'den geri yüklendiği) için burada silinmez.
     */
    fun signOut(context: Context) {
        Firebase.auth.signOut()
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit().clear().apply()
    }

    /**
     * Mevcut kullanıcının Firebase UID'si. Giriş yoksa anonim giriş yapar.
     * Giriş başarısızsa (ör. internet yok) null döner; çağıran taraf hata göstermeli.
     */
    suspend fun getCurrentUserId(): String? {
        Firebase.auth.currentUser?.uid?.let { return it }
        return signInMutex.withLock {
            Firebase.auth.currentUser?.uid ?: try {
                Firebase.auth.signInAnonymously().await().user?.uid
            } catch (e: Exception) {
                Log.w("AuthManager", "Anonim giriş başarısız", e)
                null
            }
        }
    }
}
