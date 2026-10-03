# ============================
# 📦 Android Temel Kuralları
# ============================

# ============================
# 🚨 KRİTİK - RESULT SEALED CLASS
# ============================

# Result sealed class ve tüm alt sınıflarını TAMAMEN koru
-keep class com.enes.movyswipe.data.Result { *; }
-keep class com.enes.movyswipe.data.Result$Success { *; }
-keep class com.enes.movyswipe.data.Result$Error { *; }
-keep class com.enes.movyswipe.data.Result$Loading { *; }

-keepclassmembers class com.enes.movyswipe.data.Result { *; }
-keepclassmembers class com.enes.movyswipe.data.Result$** { *; }

# Sealed class için kritik
-keepnames class com.enes.movyswipe.data.Result
-keepnames class com.enes.movyswipe.data.Result$*

-keepattributes Signature,InnerClasses,EnclosingMethod
-keepattributes *Annotation*
-keepattributes SourceFile,LineNumberTable
-keepattributes RuntimeVisibleAnnotations,RuntimeVisibleParameterAnnotations

# Activity, Service, Application gibi Android bileşenleri
-keep public class * extends android.app.Activity
-keep public class * extends android.app.Application
-keep public class * extends android.app.Service
-keep public class * extends android.content.BroadcastReceiver
-keep public class * extends android.content.ContentProvider

# XML'de kullanılan özel View'ları koru
-keep public class * extends android.view.View {
    public <init>(android.content.Context);
    public <init>(android.content.Context, android.util.AttributeSet);
    public <init>(android.content.Context, android.util.AttributeSet, int);
    public void set*(...);
}

# Enum'ların metotlarını koru
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# Parcelable sınıfları koru
-keep class * implements android.os.Parcelable {
    public static final android.os.Parcelable$Creator *;
}

# ============================
# 🔥 Firebase / Google Servisleri
# ============================
-keep class com.google.firebase.** { *; }
-keep class com.google.android.gms.** { *; }
-dontwarn com.google.firebase.**
-dontwarn com.google.android.gms.**

# ============================
# 🌐 Retrofit
# ============================
-keepattributes Signature
-keepattributes Exceptions
-keepattributes *Annotation*

-keep class retrofit2.** { *; }
-keepclasseswithmembers class * {
    @retrofit2.http.* <methods>;
}

-keep interface retrofit2.** { *; }
-dontwarn retrofit2.**

# ApiService interface'ini koru
-keep interface com.enes.movyswipe.network.ApiService { *; }
-keep class com.enes.movyswipe.network.** { *; }

# Repository ve ViewModel'leri koru
-keep class com.enes.movyswipe.data.repository.** { *; }
-keep class com.enes.movyswipe.viewmodels.** { *; }
-keepclassmembers class com.enes.movyswipe.data.repository.** { *; }
-keepclassmembers class com.enes.movyswipe.viewmodels.** { *; }

# OkHttp
-dontwarn okhttp3.**
-dontwarn okio.**
-dontwarn javax.annotation.**
-dontwarn org.conscrypt.**
-keepnames class okhttp3.internal.publicsuffix.PublicSuffixDatabase

# ============================
# 🎯 GSON - KRİTİK KURALLAR
# ============================

# Gson kütüphanesini koru
-keep class com.google.gson.** { *; }
-dontwarn sun.misc.Unsafe

# GSON generic types - BU ÇOK ÖNEMLİ!
-keepattributes Signature
-keepattributes *Annotation*

# Gson reflection için generic type bilgisini korur
-keep class * implements com.google.gson.TypeAdapter
-keep class * implements com.google.gson.TypeAdapterFactory
-keep class * implements com.google.gson.JsonSerializer
-keep class * implements com.google.gson.JsonDeserializer

# Generic type reflection için kritik
-keepclassmembers,allowobfuscation class * {
  @com.google.gson.annotations.SerializedName <fields>;
}

# Kotlin'de nullable types için (String?, Int? vb.)
-keepclassmembers class * {
    @com.google.gson.annotations.SerializedName <fields>;
}

# ============================
# 🎬 MOVIE MODEL'LERİ - ÖZEL KORUMA
# ============================

# Movie ve MovieResponse sınıflarını TAM olarak koru
-keep class com.enes.movyswipe.data.Movie { *; }
-keep class com.enes.movyswipe.data.MovieResponse { *; }

# Tüm data paketi altındaki sınıfları koru
-keep class com.enes.movyswipe.data.** { *; }

# Data class'ların tüm member'larını koru (field, constructor, method)
-keepclassmembers class com.enes.movyswipe.data.** {
    <fields>;
    <init>(...);
    <methods>;
}

# Kotlin data class'ların Companion object'lerini koru
-keep class com.enes.movyswipe.data.**$Companion { *; }

# SerializedName annotation'larını mutlaka koru
-keepclassmembers class com.enes.movyswipe.data.** {
    @com.google.gson.annotations.SerializedName <fields>;
}

# List<Movie> gibi generic type'lar için
-keepclassmembers class com.enes.movyswipe.data.MovieResponse {
    *** movies;
}

# Response ve Request modellerini koru
-keep class * implements com.google.gson.TypeAdapter {
    <init>(...);
}

# Kotlin Metadata (Reflection için gerekli)
-keep class kotlin.Metadata { *; }
-keep class kotlin.reflect.** { *; }
-dontwarn kotlin.reflect.**

# Kotlin sealed class için
-keep class kotlin.** { *; }
-keepclassmembers class kotlin.** { *; }

# Sealed class generic type koruması
-keepattributes Signature
-keepattributes *Annotation*
-keepattributes InnerClasses
-keepattributes EnclosingMethod

# ============================
# 🧊 Glide (Resim Yükleme)
# ============================
-keep public class * implements com.bumptech.glide.module.GlideModule
-keep class * extends com.bumptech.glide.module.AppGlideModule {
    <init>(...);
}
-keep public enum com.bumptech.glide.load.ImageHeaderParser$** {
    **[] $VALUES;
    public *;
}
-keep class com.bumptech.glide.load.data.ParcelFileDescriptorRewinder$InternalRewinder {
    *** rewind();
}
-dontwarn com.bumptech.glide.**

# ============================
# 💾 Room Database
# ============================
-keep class androidx.room.** { *; }
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
-dontwarn androidx.room.**

-keepclassmembers class * extends androidx.room.RoomDatabase {
    public static ** getDatabase(...);
}

# ============================
# ⚙️ Kotlin Coroutines
# ============================
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-keepclassmembers class kotlinx.** {
    volatile <fields>;
}
-keepclassmembers class kotlin.coroutines.jvm.internal.BaseContinuationImpl {
    <fields>;
    <init>(...);
}
-dontwarn kotlinx.coroutines.**

# ============================
# 📱 CardStackView
# ============================
-keep class com.yuyakaido.android.cardstackview.** { *; }

# ============================
# 🎨 Lottie Animations
# ============================
-keep class com.airbnb.lottie.** { *; }

# ============================
# 💳 Google Play Billing
# ============================
-keep class com.android.billingclient.** { *; }

# ============================
# 📊 Google Play Services Ads
# ============================
-keep class com.google.android.gms.ads.** { *; }

# ============================
# 🛡️ Genel Uyarıları Sustur
# ============================
-dontwarn org.bouncycastle.**
-dontwarn org.openjsse.**
-dontwarn javax.annotation.**
-dontwarn javax.inject.**

# R8 optimizasyonu
-allowaccessmodification
-repackageclasses