import java.util.Properties

val localProperties = Properties()
val localPropertiesFile = rootProject.file("local.properties")
if (localPropertiesFile.exists()) {
    localPropertiesFile.inputStream().use { localProperties.load(it) }
}

/** local.properties içindeki değeri, yoksa -P ile verilen Gradle özelliğini döndürür. */
fun secret(name: String): String? =
    localProperties.getProperty(name) ?: (project.findProperty(name) as String?)

// Google'ın herkese açık TEST banner birimi. Sadece debug derlemelerde kullanılır.
val testBannerAdUnitId = "ca-app-pub-3940256099942544/6300978111"
val releaseBannerAdUnitId = secret("ADMOB_BANNER_ID")

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
    compileSdk = 36

    signingConfigs {
        create("release") {
            // Keystore yolu local.properties'teki KEYSTORE_FILE ile değiştirilebilir.
            storeFile = file(secret("KEYSTORE_FILE") ?: "C:/Dell/AndroidKeys/filmswipe_keystore.jks")
            storePassword = secret("KEYSTORE_PASSWORD") ?: ""
            keyAlias = secret("KEY_ALIAS") ?: ""
            keyPassword = secret("KEY_PASSWORD") ?: ""
        }
    }

    buildFeatures {
        viewBinding = true
        buildConfig = true
    }
    defaultConfig {
        applicationId = "com.enes.movyswipe"
        minSdk = 24
        targetSdk = 36
        // Google Play için yeni sürüm
        versionCode = 11
        versionName = "1.0.10"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        val tmdbApiKey = secret("TMDB_API_KEY") ?: "\"\""
        buildConfigField("String", "TMDB_API_KEY", tmdbApiKey)

        // Firebase emülatörüne bağlanmak için: ./gradlew installDebug -PuseFirebaseEmulator=true
        buildConfigField("boolean", "USE_FIREBASE_EMULATOR", "false")
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfig = signingConfigs.getByName("release")
            buildConfigField("String", "ADMOB_BANNER_ID", "\"${releaseBannerAdUnitId ?: ""}\"")
        }

        debug {
            isMinifyEnabled = false
            isShrinkResources = false
            buildConfigField("String", "ADMOB_BANNER_ID", "\"$testBannerAdUnitId\"")
            val useEmulator = (project.findProperty("useFirebaseEmulator") as String?) == "true"
            buildConfigField("boolean", "USE_FIREBASE_EMULATOR", useEmulator.toString())
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
    lint {
        // Glide'ın kullanmadığımız bildirim hedefinden gelen yanlış alarm; uygulama bildirim göndermiyor.
        disable += "NotificationPermission"
        abortOnError = true
    }
    testOptions {
        unitTests.isReturnDefaultValues = true
    }
}

kapt {
    arguments {
        // Room şemalarını sürüm kontrolüne alıyoruz; ileride migration yazmak için gerekli.
        arg("room.schemaLocation", "$projectDir/schemas")
    }
}

// Gerçek AdMob banner ID'si olmadan release paketi üretilmesini engelle.
tasks.configureEach {
    val isReleasePackaging = name.matches(Regex("(assemble|bundle|package)Release.*"))
    if (isReleasePackaging) {
        doFirst {
            val id = releaseBannerAdUnitId
            if (id.isNullOrBlank() || !id.matches(Regex("ca-app-pub-\\d+/\\d+"))) {
                throw GradleException(
                    "local.properties içinde geçerli bir ADMOB_BANNER_ID yok " +
                        "(biçim: ca-app-pub-XXXX/YYYY). AdMob'da banner reklam birimi oluşturup ekle."
                )
            }
        }
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)
    implementation("androidx.fragment:fragment-ktx:1.6.2")
    implementation("androidx.navigation:navigation-fragment-ktx:2.7.7")
    implementation("androidx.navigation:navigation-ui-ktx:2.7.7")
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.7.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.7.0")

    implementation("com.google.dagger:hilt-android:2.51.1")
    kapt("com.google.dagger:hilt-android-compiler:2.51.1")

    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.10.0")

    implementation("com.github.yuyakaido:cardstackview:2.3.4")
    implementation("com.github.bumptech.glide:glide:4.14.2")

    implementation(platform("com.google.firebase:firebase-bom:33.7.0"))
    implementation("com.google.firebase:firebase-database-ktx")
    implementation("com.google.firebase:firebase-analytics-ktx")
    implementation("com.google.firebase:firebase-auth-ktx")

    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-play-services:1.7.1")
    implementation("com.airbnb.android:lottie:6.1.0")
    implementation("androidx.core:core-splashscreen:1.0.1")

    val roomVersion = "2.6.1"
    implementation("androidx.room:room-runtime:$roomVersion")
    kapt("androidx.room:room-compiler:$roomVersion")
    implementation("androidx.room:room-ktx:$roomVersion")

    implementation("com.google.android.gms:play-services-ads:23.0.0")
    // AB/BK kullanıcıları için zorunlu reklam onayı (Google UMP)
    implementation("com.google.android.ump:user-messaging-platform:2.2.0")
    implementation("com.android.billingclient:billing-ktx:8.0.0")

    testImplementation(libs.junit)
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.7.3")
    testImplementation("org.json:json:20231013")
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}
