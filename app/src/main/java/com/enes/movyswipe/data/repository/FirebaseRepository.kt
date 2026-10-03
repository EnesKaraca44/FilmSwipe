package com.enes.movyswipe.data.repository


import android.util.Log
import com.enes.movyswipe.data.ChatMessage
import com.enes.movyswipe.data.GameRoom
import com.google.firebase.Firebase
import com.google.firebase.database.DataSnapshot
import com.enes.movyswipe.data.Result
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.GenericTypeIndicator
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.database
import com.google.firebase.database.ktx.database // Gerekli import
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await             // Gerekli import

class FirebaseRepository { // Class burada başlıyor

    // Bu değişken, class'ın içinde.
    private val database = Firebase.database.reference

    /**
     * Yeni bir düello odası oluşturur ve bunu Firebase'e yazar.
     */
    // Bu fonksiyon, class'ın içinde.
    suspend fun createRoom(filters: Map<String, Any>, movieDeck: List<Int>): String {
        val roomCode = generateRandomCode() // Artık bu fonksiyonu görebilir.

        val roomData = GameRoom(
            status = "waiting",
            filters = filters,
            movieDeck = movieDeck

        )

        database.child("rooms").child(roomCode).setValue(roomData).await()

        return roomCode
    }

    /**
     * 6 haneli, büyük harf ve rakamlardan oluşan rastgele bir kod üretir.
     */
    // Bu fonksiyon da class'ın içinde.
    private fun generateRandomCode(): String {
        val karakterHavuzu: List<Char> = ('A'..'Z') + ('0'..'9')
        return (1..6)
            .map { karakterHavuzu.random() }
            .joinToString("")
    }

    suspend fun joinRoom(roomCode: String, guestId: String): Boolean {
        return try {
            val roomRef = database.child("rooms").child(roomCode)
            val snapshot = roomRef.get().await()
            if (!snapshot.exists()) {
                println("Hata: ${roomCode} kodlu oda bulunamadı.")
                false // Oda yoksa, fonksiyon başarısız olur ve 'false' döndürür.
            } else {
                val updates = mapOf(
                    "guestId" to guestId,   // 'guestId' alanını gelen parametreyle güncelle
                    "status" to "active"    // 'status' alanını "active" olarak değiştir
                )
                roomRef.updateChildren(updates).await()

                println("Başarılı: ${guestId}, ${roomCode} kodlu odaya katıldı.")
                true // Tüm işlemler başarılıysa, fonksiyon 'true' döndürür.
            }
        } catch (e: Exception) {
            // Bir veritabanı hatası veya internet sorunu olursa, burası çalışır.
            println("Odaya katılırken hata oluştu: ${e.message}")
            false // Hata durumunda da fonksiyon 'false' döndürür.


        }
    }

    fun listenToRoomStatus(roomCode: String): Flow<String> = callbackFlow {
        val roomStatusRef = database.child("rooms").child(roomCode).child("status")
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                // Gelen veriyi (snapshot) alıp String'e çeviriyoruz.
                // Eğer veri yoksa veya beklenmedik bir tipteyse, varsayılan olarak "unknown" diyoruz.
                val status = snapshot.getValue(String::class.java) ?: "unknown"

                // 4. Adım: 'trySend(status)', yakaladığımız yeni durumu Flow'u dinleyen
                // herkese (ViewModel'e) anında gönderir.
                trySend(status)
            }

            override fun onCancelled(error: DatabaseError) {
                // Hata durumunda Flow'u bir istisna ile kapatıyoruz ki dinleyenler
                // bir sorun olduğunu anlasın.
                close(error.toException())
            }

        }
        roomStatusRef.addValueEventListener(listener)
        awaitClose {
            // Artık dinlemeye gerek kalmadığı için, ajanı (listener) görevden alıyoruz.
            // Bu, bellekte gereksiz dinleyicilerin birikmesini (bellek sızıntısı) önler.
            // Bu satır ÇOK ÖNEMLİDİR.
            roomStatusRef.removeEventListener(listener)
            println("Status dinleyicisi ${roomCode} için kaldırıldı.")
        }
    }

    suspend fun getMovieDeck(roomCode: String): Result<List<Int>> {
        return try {
            val snapshot = database.child("rooms").child(roomCode).child("movieDeck").get().await()
            val movieDeck = snapshot.getValue(object : GenericTypeIndicator<List<Int>>() {})

            if (movieDeck.isNullOrEmpty()) {
                Result.Error(Exception("Oda için film listesi bulunamadı."))
            } else {
                Result.Success(movieDeck)
            }
        } catch (e: Exception) {
            Result.Error(e)
        }
    }


    suspend fun submitChoice(roomCode: String, movieId: Int, userId: String, choice: String) {
        try {
            // GÖREVİ SADECE VERİYİ YAZMAK. KONTROL YAPMAK YOK.
            database.child("rooms")
                .child(roomCode)
                .child("choices")
                .child(movieId.toString())
                .child(userId)
                .setValue(choice)
                .await()
        } catch (e: Exception) {
            println("Seçim gönderilirken hata oluştu: ${e.message}")
        }
    }
    fun listenForMatch(roomCode: String): Flow<Int?> = callbackFlow {
        val matchRef = database.child("rooms").child(roomCode).child("match")

        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                // Gelen veriyi Int'e çevirmeye çalışıyoruz.
                trySend(snapshot.getValue(Int::class.java))
            }

            override fun onCancelled(error: DatabaseError) {
                close(error.toException())
            }
        }

        matchRef.addValueEventListener(listener)

        awaitClose { matchRef.removeEventListener(listener) }
    }

    fun listenToMessages(roomCode: String): Flow<List<ChatMessage>> = callbackFlow {
        val chatRef = database.child("rooms").child(roomCode).child("chat")

        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val messages = mutableListOf<ChatMessage>()
                // snapshot.children ile "chat" dalının altındaki tüm mesajları geziyoruz.
                for (messageSnapshot in snapshot.children) {
                    // Her bir mesajı ChatMessage nesnesine çeviriyoruz.
                    val chatMessage = messageSnapshot.getValue(ChatMessage::class.java)
                    if (chatMessage != null) {
                        messages.add(chatMessage)
                    }
                }
                // Topladığımız tüm mesajları liste olarak Flow'a gönderiyoruz.
                trySend(messages)
            }

            override fun onCancelled(error: DatabaseError) {
                close(error.toException())
            }
        }

        chatRef.addValueEventListener(listener)

        awaitClose { chatRef.removeEventListener(listener) }
    }

    suspend fun sendMessage(roomCode: String, message: ChatMessage) {
        try {
            // "chat" dalına gidiyoruz ve .push() ile yeni, benzersiz bir anahtar oluşturuyoruz.
            // Bu, her mesajın kendine ait bir ID'sinin olmasını sağlar (örn: "-N_xYz123Abc...").
            // Sonra bu yeni anahtarın altına mesaj nesnemizi yazıyoruz.
            database.child("rooms").child(roomCode).child("chat").push().setValue(message).await()
        } catch (e: Exception) {
            println("Mesaj gönderilirken hata oluştu: ${e.message}")
        }
    }

    /**
    * Kullanıcıyı bekleme havuzuna ('queue') ekler.
    * @param userId Havuza eklenecek kullanıcının ID'si.
    */
    suspend fun joinQueue(userId: String) {
        database.child("queue").child(userId).setValue(true).await()
    }
    /**
     * Kullanıcıyı bekleme havuzundan ('queue') çıkarır.
     * @param userId Havuzdan çıkarılacak kullanıcının ID'si.
     */
    suspend fun leaveQueue(userId: String) {
        database.child("queue").child(userId).removeValue().await()
    }
    /**
     * Bekleme havuzundaki değişiklikleri anlık olarak dinler.
     * @return Havuzdaki tüm kullanıcı ID'lerinin bir listesini her değişiklikte yayan bir Flow.
     */
    fun listenToQueue(): Flow<List<String>> = callbackFlow {
        val queueRef = database.child("queue")

        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                // snapshot.children ile havuzdaki tüm kullanıcıları gezip ID'lerini (key) bir listeye topluyoruz.
                val userIdsInQueue = snapshot.children.mapNotNull { it.key }
                trySend(userIdsInQueue)
            }
            override fun onCancelled(error: DatabaseError) {
                close(error.toException())
            }
        }
        queueRef.addValueEventListener(listener)
        awaitClose { queueRef.removeEventListener(listener) }
    }
    /**
     * Hızlı düelloda eşleşen kullanıcıya, hangi odaya katılması gerektiğini bildirir.
     * @param opponentId Haberin gönderileceği rakip kullanıcının ID'si.
     * @param roomCode Katılması gereken odanın kodu.
     */
    suspend fun notifyOpponent(opponentId: String, roomCode: String) {
        // "matchmaking" adında yeni bir dal oluşturup, her kullanıcının ID'si altına
        // bir "posta kutusu" açıyoruz.
        database.child("matchmaking").child(opponentId).child("assignedRoom").setValue(roomCode).await()
    }
    /**
     * Kullanıcının, kendisine atanmış bir oda olup olmadığını dinlemesini sağlar.
     */
    fun listenForAssignedRoom(userId: String): Flow<String?> = callbackFlow {
        val assignedRoomRef = database.child("matchmaking").child(userId).child("assignedRoom")

        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                trySend(snapshot.getValue(String::class.java))
            }
            override fun onCancelled(error: DatabaseError) { close(error.toException()) }
        }
        assignedRoomRef.addValueEventListener(listener)
        awaitClose { assignedRoomRef.removeEventListener(listener) }
    }

    /**
     * Belirtilen odanın filtre bilgilerini Firebase'den çeker.
     * @param roomCode Hangi odanın filtrelerinin çekileceği.
     * @return Filtreleri içeren bir Map<String, Any> veya oda bulunamazsa/filtre yoksa null döner.
     */
    suspend fun getRoomFilters(roomCode: String): Result<Map<String, Any>> { // Result tipini kullanalım
        return try {
            val snapshot = database.child("rooms").child(roomCode).child("filters").get().await()
            val filters = snapshot.getValue(object : GenericTypeIndicator<Map<String, Any>>() {})
            if (filters != null) {
                Result.Success(filters)
            } else {
                Result.Error(Exception("Filtreler bulunamadı."))
            }
        } catch (e: Exception) {
            Result.Error(e)
        }
    }
    suspend fun clearMatchmakingData(userId: String) {
        try {
            database.child("matchmaking").child(userId).removeValue().await()
        } catch (e: Exception) {
            println("Matchmaking verisi temizlenirken hata: ${e.message}")
        }
    }

    suspend fun deleteRoom(roomCode: String) {
        database.child("rooms").child(roomCode).removeValue().await()
    }


}






