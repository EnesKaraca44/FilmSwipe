package com.enes.movyswipe.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.enes.movyswipe.util.AuthManager

class AyarlarViewModel(application: Application) : AndroidViewModel(application) {

    // Mevcut kullanıcı adını getirir.
    fun getCurrentNickname(): String {
        return AuthManager.getUserNickname(getApplication())
    }

    // Yeni kullanıcı adını kaydeder.
    fun saveNewNickname(nickname: String) {
        AuthManager.saveUserNickname(getApplication(), nickname)
    }

    /**
    * AuthManager aracılığıyla kullanıcının oturumunu kapatır ve yerel verilerini temizler.
    */
    fun signOut() {
        AuthManager.signOut(getApplication())
    }
}