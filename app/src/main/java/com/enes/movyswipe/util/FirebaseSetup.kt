package com.enes.movyswipe.util

import com.enes.movyswipe.BuildConfig
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase

object FirebaseSetup {
    /** Android emülatöründen bilgisayardaki Firebase emülatörlerine erişim adresi. */
    private const val EMULATOR_HOST = "10.0.2.2"

    fun configure() {
        if (BuildConfig.USE_FIREBASE_EMULATOR) {
            // Sadece '-PuseFirebaseEmulator=true' ile derlenen debug sürümünde çalışır.
            Firebase.database.useEmulator(EMULATOR_HOST, 9000)
            Firebase.auth.useEmulator(EMULATOR_HOST, 9099)
        }
    }
}
