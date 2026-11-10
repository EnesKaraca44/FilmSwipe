// Top-level build file where you can add configuration options common to all sub-projects/modules.
// Tüm eklenti sürümleri tek yerde tanımlanır; buildscript classpath ile çakışan sürümler kaldırıldı.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
    id("androidx.navigation.safeargs.kotlin") version "2.7.7" apply false
    id("com.google.dagger.hilt.android") version "2.51.1" apply false
    id("com.google.gms.google-services") version "4.4.0" apply false
}
