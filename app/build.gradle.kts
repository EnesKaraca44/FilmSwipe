import java.util.Properties

val localProperties = Properties()
val localPropertiesFile = rootProject.file("local.properties")
if (localPropertiesFile.exists()) {
    localProperties.load(localPropertiesFile.inputStream())
}

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    id("androidx.navigation.safeargs.kotlin")
    id("com.google.gms.google-services")
    id("kotlin-kapt")
    id("com.google.dagger.hilt.android")
}

android {
    namespace = "com.enes.movyswipe"
    compileSdk = 35


    signingConfigs {
        create("release") {
            // 'storeFile' yolunu, kendi .jks dosyanın konumuyla değiştir.
            // ÖNEMLİ: Proje klasörünün dışındaki tam yolu kullanmak en iyisidir.
            // Windows'ta \ yerine / kullanmak genellikle daha sorunsuzdur.
            storeFile = file("C:\\Dell\\AndroidKeys/filmswipe_keystore.jks")

            storePassword = localProperties.getProperty("KEYSTORE_PASSWORD") ?: ""
            keyAlias = localProperties.getProperty("KEY_ALIAS") ?: ""
            keyPassword = localProperties.getProperty("KEY_PASSWORD") ?: ""
        }
    }

    buildFeatures {
        viewBinding = true
        buildConfig = true
    }
    defaultConfig {
        applicationId = "com.enes.movyswipe"
        minSdk = 24
        targetSdk = 35
        // Google Play için yeni sürüm
        versionCode = 9
        versionName = "1.0.8"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        
        val tmdbApiKey = localProperties.getProperty("TMDB_API_KEY") ?: "\"\""
        buildConfigField("String", "TMDB_API_KEY", tmdbApiKey)
    }
    sourceSets {
        getByName("main") {
            java.srcDirs("src/main/java")
        }
        getByName("test") {
            java.srcDirs("src/test/java")
        }
        getByName("androidTest") {
            java.srcDirs("src/androidTest/java")
        }
    }

    buildTypes {
        release {
            // Kod Küçültmeyi (Minification) Aktif Et
            // Bu, kullanılmayan kodları (sınıflar, metotlar) siler ve kalanları
            // yeniden adlandırarak (obfuscation) APK boyutunu küçültür ve kodu
            // tersine mühendisliğe karşı daha zor hale getirir.
            isMinifyEnabled = true

            // Kaynak Sıkıştırmayı (Resource Shrinking) Aktif Et
            // Bu, kullanılmayan resim, layout, string gibi kaynakları siler.
            // Sadece minifyEnabled true ise çalışır.
            isShrinkResources = true

            // ProGuard/R8 kurallarını tanımlayan dosyalar.
            // Bu dosyalar, hangi kodların silinmemesi gerektiğini belirtir.
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfig = signingConfigs.getByName("release")

        }

        debug {
            isMinifyEnabled = false
            isShrinkResources = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }



    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
}

dependencies {

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    implementation("androidx.navigation:navigation-fragment-ktx:2.7.7")
    implementation("androidx.navigation:navigation-ui-ktx:2.7.7")

    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.5.1")
    implementation("androidx.activity:activity-ktx:1.6.1")
    implementation("com.google.dagger:hilt-android:2.51.1")
    kapt("com.google.dagger:hilt-android-compiler:2.51.1")
    implementation("com.squareup.retrofit2:retrofit:2.9.0")


    // Moshi (Reflection tabanlı, kapt gerektirmez)


    // --- GSON SATIRLARINI EKLE ---
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")

    // OkHttp Logging Interceptor
    implementation ("com.squareup.okhttp3:logging-interceptor:4.10.0")

    // Glide (Resim yükleme için)


    implementation ("com.github.yuyakaido:cardstackview:2.3.4")
    implementation ("com.github.bumptech.glide:glide:4.14.2")

    implementation(platform("com.google.firebase:firebase-bom:32.7.2"))
    implementation("com.google.firebase:firebase-database-ktx")
    implementation("com.google.firebase:firebase-analytics-ktx")
    implementation("com.google.firebase:firebase-auth-ktx")

    implementation ("androidx.core:core-ktx:1.13.1")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-play-services:1.7.1")
    implementation("com.airbnb.android:lottie:6.1.0")
    implementation("androidx.core:core-splashscreen:1.0.1")

    val room_version = "2.6.1"
    implementation("androidx.room:room-runtime:$room_version")
    kapt("androidx.room:room-compiler:$room_version")
    implementation("androidx.room:room-ktx:$room_version")

    implementation("com.google.android.gms:play-services-ads:23.0.0")
    // Google Play Billing Kütüphanesi
    implementation("com.android.billingclient:billing-ktx:6.1.0")
}