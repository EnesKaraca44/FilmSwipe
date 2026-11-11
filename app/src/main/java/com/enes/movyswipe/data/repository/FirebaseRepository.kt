package com.enes.movyswipe.data.repository

import android.util.Log
import com.enes.movyswipe.data.ChatMessage
import com.enes.movyswipe.data.JoinResult
import com.enes.movyswipe.data.QueueDeck
import com.enes.movyswipe.data.Result
import com.enes.movyswipe.data.RoomCodes
import com.enes.movyswipe.data.RoomInfo
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.ServerValue
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Realtime Database erişimi. Veri yapısı ve yetkiler için kök dizindeki
 * database.rules.json dosyasına bakın.
 */
@Singleton
class FirebaseRepository @Inject constructor() {

    private val database: DatabaseReference get() = Firebase.database.reference
    private fun room(code: String) = database.child("rooms").child(code)

    // ---------------------------------------------------------------- Odalar

    /**
     * Yeni oda oluşturur. Kod çakışırsa (kurallar mevcut odanın üzerine yazmayı reddeder)
     * yeni bir kodla tekrar dener.
     * @param guestId Hızlı düelloda rakip baştan bellidir; o zaman oda doğrudan "active" açılır.
     */
    suspend fun createRoom(
        hostId: String,
        filters: Map<String, Any>,
        movieDeck: List<Int>,
        guestId: String? = null
    ): String {
        var lastError: Exception? = null
        repeat(5) {
            val code = RoomCodes.generate()
            try {
                if (room(code).get().await().exists()) return@repeat
                val data = mutableMapOf<String, Any>(
                    "status" to if (guestId == null) STATUS_WAITING else STATUS_ACTIVE,
                    "hostId" to hostId,
                    "filters" to filters,
                    "movieDeck" to movieDeck,
                    "createdAt" to ServerValue.TIMESTAMP
                )
                if (guestId != null) data["guestId"] = guestId
                room(code).setValue(data).await()
                return code
            } catch (e: Exception) {
                lastError = e
                Log.w(TAG, "Oda oluşturma denemesi başarısız ($code)", e)
            }
        }
        throw lastError ?: IllegalStateException("Boş oda kodu bulunamadı")
    }

    /** Bekleyen bir odaya misafir olarak katılır. Dolu odalara üçüncü kişi giremez. */
    suspend fun joinRoom(rawCode: String, guestId: String): JoinResult {
        val code = RoomCodes.normalize(rawCode)
        if (!RoomCodes.isAcceptable(code)) return JoinResult.INVALID_CODE
        return try {
            val snapshot = room(code).get().await()
            if (!snapshot.exists()) return JoinResult.NOT_FOUND
            val hostId = snapshot.child("hostId").getValue(String::class.java)
            val currentGuest = snapshot.child("guestId").getValue(String::class.java)
            val status = snapshot.child("status").getValue(String::class.java)
            when {
                hostId == guestId -> JoinResult.OWN_ROOM
                currentGuest == guestId -> JoinResult.SUCCESS // aynı kişi yeniden katılıyor
                currentGuest != null || status != STATUS_WAITING -> JoinResult.FULL
                else -> {
                    // Çok yollu güncelleme atomiktir; kurallar guestId doluysa reddeder.
                    room(code).updateChildren(
                        mapOf("guestId" to guestId, "status" to STATUS_ACTIVE)
                    ).await()
                    JoinResult.SUCCESS
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Odaya katılma başarısız ($code)", e)
            // Yarış durumunda kurallar yazmayı reddeder: büyük olasılıkla oda doldu.
            if (e.message?.contains("Permission denied", ignoreCase = true) == true) JoinResult.FULL
            else JoinResult.ERROR
        }
    }

    suspend fun getRoom(code: String): Result<RoomInfo> = try {
        val s = room(code).get().await()
        if (!s.exists()) {
            Result.Error(Exception("Oda bulunamadı."))
        } else {
            val deck = s.child("movieDeck").children.mapNotNull { it.getValue(Long::class.java)?.toInt() }
            Result.Success(
                RoomInfo(
                    code = code,
                    status = s.child("status").getValue(String::class.java) ?: STATUS_WAITING,
                    hostId = s.child("hostId").getValue(String::class.java),
                    guestId = s.child("guestId").getValue(String::class.java),
                    genreName = s.child("filters").child("genreName").getValue(String::class.java),
                    movieDeck = deck
                )
            )
        }
    } catch (e: Exception) {
        Result.Error(e)
    }

    fun listenToRoomStatus(code: String): Flow<String> =
        valueFlow(room(code).child("status")) { it.getValue(String::class.java) ?: "unknown" }

    suspend fun submitChoice(code: String, movieId: Int, userId: String, liked: Boolean): Boolean = try {
        room(code).child("choices").child(movieId.toString()).child(userId)
            .setValue(if (liked) CHOICE_LIKE else CHOICE_PASS).await()
        true
    } catch (e: Exception) {
        Log.w(TAG, "Seçim gönderilemedi", e)
        false
    }

    fun listenForMatch(code: String): Flow<Int?> =
        valueFlow(room(code).child("match")) { it.getValue(Long::class.java)?.toInt() }

    /** Kullanıcının desteyi bitirdiğini işaretler (iki taraf da bitirince "eşleşme yok"). */
    suspend fun markFinished(code: String, userId: String) {
        try {
            room(code).child("finished").child(userId).setValue(true).await()
        } catch (e: Exception) {
            Log.w(TAG, "Bitiş işaretlenemedi", e)
        }
    }

    fun listenToFinished(code: String): Flow<Set<String>> =
        valueFlow(room(code).child("finished")) { s -> s.children.mapNotNull { it.key }.toSet() }

    // ---------------------------------------------------------------- Sohbet

    fun listenToMessages(code: String): Flow<List<ChatMessage>> =
        valueFlow(room(code).child("chat")) { snapshot ->
            snapshot.children.mapNotNull { child ->
                child.getValue(ChatMessage::class.java)?.also { it.id = child.key.orEmpty() }
            }
        }

    suspend fun sendMessage(code: String, message: ChatMessage): Boolean = try {
        room(code).child("chat").push().setValue(message).await()
        true
    } catch (e: Exception) {
        Log.w(TAG, "Mesaj gönderilemedi", e)
        false
    }

    suspend fun reportUser(
        roomCode: String,
        reporterId: String,
        reportedUserId: String,
        reason: String,
        messageText: String?
    ): Boolean = try {
        val report = mutableMapOf<String, Any>(
            "roomCode" to roomCode,
            "reporterId" to reporterId,
            "reportedUserId" to reportedUserId,
            "reason" to reason,
            "createdAt" to ServerValue.TIMESTAMP
        )
        if (messageText != null) report["messageText"] = messageText.take(ChatMessage.MAX_LENGTH)
        database.child("reports").push().setValue(report).await()
        true
    } catch (e: Exception) {
        Log.w(TAG, "Şikayet gönderilemedi", e)
        false
    }

    // ---------------------------------------------------------------- Engelleme

    /**
     * Engeli sunucuya yazar. Eşleştirme (Cloud Function) iki yönde de bu listeye bakar ve
     * kurallar engellenen kişinin aynı odada mesaj yazmasını reddeder.
     */
    suspend fun blockUser(me: String, blockedUserId: String): Boolean = try {
        database.child("blocks").child(me).child(blockedUserId).setValue(true).await()
        true
    } catch (e: Exception) {
        Log.w(TAG, "Engel sunucuya yazılamadı", e)
        false
    }

    /** Cihazda tutulan (ör. eski sürümde eklenmiş) engelleri sunucuya taşır. */
    suspend fun syncBlocks(me: String, blocked: Set<String>) {
        if (blocked.isEmpty()) return
        try {
            database.child("blocks").child(me).updateChildren(blocked.associateWith { true }).await()
        } catch (e: Exception) {
            Log.w(TAG, "Engeller eşitlenemedi", e)
        }
    }

    // ---------------------------------------------------------------- Hızlı düello

    /**
     * Kuyruğa katılır. Eşleştirmeyi sunucu (functions/index.js: pairFromQueue) yapar; odayı,
     * daha uzun bekleyen tarafın buraya yazdığı desteyle kurar ve iki tarafın posta kutusuna
     * oda kodunu yazar. Uygulama kapanır veya bağlantı koparsa sunucu kaydı otomatik siler,
     * böylece kuyrukta "hayalet" kullanıcı kalmaz.
     */
    suspend fun joinQueue(userId: String, deck: QueueDeck) {
        val ref = database.child("queue").child(userId)
        ref.onDisconnect().removeValue().await()
        ref.setValue(
            mapOf(
                "ts" to ServerValue.TIMESTAMP,
                "deck" to mapOf(
                    "genreName" to deck.genreName,
                    "genreId" to deck.genreId,
                    "movieIds" to deck.movieIds.take(QueueDeck.MAX_SIZE)
                )
            )
        ).await()
    }

    suspend fun leaveQueue(userId: String) {
        val ref = database.child("queue").child(userId)
        try {
            ref.removeValue().await()
            ref.onDisconnect().cancel().await()
        } catch (e: Exception) {
            Log.w(TAG, "Kuyruktan çıkılamadı", e)
        }
    }

    fun listenToQueue(): Flow<Map<String, Long?>> = valueFlow(database.child("queue")) { s ->
        s.children.mapNotNull { child ->
            child.key?.let { it to child.child("ts").getValue(Long::class.java) }
        }.toMap()
    }

    fun listenForAssignedRoom(userId: String): Flow<String?> =
        valueFlow(database.child("matchmaking").child(userId).child("assignedRoom")) {
            it.getValue(String::class.java)
        }

    suspend fun clearMatchmakingData(userId: String) {
        try {
            database.child("matchmaking").child(userId).removeValue().await()
        } catch (e: Exception) {
            Log.w(TAG, "Matchmaking verisi temizlenemedi", e)
        }
    }

    // ---------------------------------------------------------------- Yardımcılar

    private fun <T> valueFlow(ref: DatabaseReference, map: (DataSnapshot) -> T): Flow<T> = callbackFlow {
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                trySend(map(snapshot))
            }

            override fun onCancelled(error: DatabaseError) {
                close(error.toException())
            }
        }
        ref.addValueEventListener(listener)
        awaitClose { ref.removeEventListener(listener) }
    }

    companion object {
        private const val TAG = "FirebaseRepository"
        const val STATUS_WAITING = "waiting"
        const val STATUS_ACTIVE = "active"
        const val CHOICE_LIKE = "like"
        const val CHOICE_PASS = "pass"
    }
}
