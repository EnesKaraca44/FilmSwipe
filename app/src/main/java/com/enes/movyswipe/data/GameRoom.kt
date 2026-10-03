package com.enes.movyswipe.data


import com.google.firebase.database.IgnoreExtraProperties

// Bu anotasyon, Firebase'in veritabanında olup da data class'ta olmayan
// alanları görmezden gelmesini sağlar. Bu, gelecekteki hataları önler.
@IgnoreExtraProperties
data class GameRoom(
    // Her bir alan için varsayılan bir değer atamak ÇOK ÖNEMLİDİR.
    // Bu, Firebase'in veriyi okurken sorun yaşamasını engeller.

    val status: String = "waiting", // Odanın durumu: "waiting", "active", "finished"

    // Filtreleri Map olarak saklamak esneklik sağlar.
    // TMDB'den gelen genre ID'leri (Int) ve yıl (Int) burada tutulacak.
    val filters: Map<String, Any>? = null,

    // Düello için seçilen film ID'lerinin listesi.
    val movieDeck: List<Int>? = null,

    // Oyuncuların ID'leri.
    val hostId: String? = null,
    val guestId: String? = null,

    // Yapılan seçimleri tutacak olan yapı.
    // Örnek: "choices": { "550": { "user123": "like", "user456": "pass" } }
    // Map<String, Any> kullanmak en esnek yöntemdir.
    val choices: Map<String, Any>? = null,

    // Eşleşme olduğunda eşleşen filmin ID'si buraya yazılacak.
    val match: Int? = null,

    // Firebase'in veriyi geri okuyabilmesi (deserialization) için
    // BOŞ BİR CONSTRUCTOR (yapıcı metot) mutlaka gereklidir.
    // Bu satır olmadan Firebase'den veri okurken çökme yaşarsın.
    val timestamp: Long = System.currentTimeMillis() // Odanın ne zaman oluşturulduğunu tutar
) {
    // Firebase'in hatasız çalışması için gerekli olan boş constructor.
    constructor() : this("waiting", null, null, null, null, null, null, 0L)
}