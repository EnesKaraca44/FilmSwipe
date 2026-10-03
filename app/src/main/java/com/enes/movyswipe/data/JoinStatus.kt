package com.enes.movyswipe.data

sealed class JoinStatus {
    // Başlangıç durumu, hiçbir işlem yapılmıyor.
    object IDLE : JoinStatus()

    // Odaya katılma işlemi başladığında kullanılacak durum.
    object LOADING : JoinStatus()

    // Odaya katılma işlemi başarıyla bittiğinde kullanılacak durum.
    object SUCCESS : JoinStatus()

    // Odaya katılma işlemi başarısız olduğunda kullanılacak durum.
    // İçinde bir hata mesajı da taşıyabilir.
    data class FAILURE(val message: String) : JoinStatus()
}