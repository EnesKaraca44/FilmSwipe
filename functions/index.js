const { onValueWritten, onValueCreated } = require("firebase-functions/v2/database");
const { onSchedule } = require("firebase-functions/v2/scheduler");
const { logger } = require("firebase-functions");
const admin = require("firebase-admin");
const { isMatch, pickPair, generateRoomCode, ROOM_TTL_MS, QUEUE_TTL_MS } = require("./match");

const { isChatRateExceeded, CHAT_RATE_WINDOW_MS } = require("./chat");

admin.initializeApp();

/**
 * Sohbet sıklık sınırı: aynı kişinin pencere içindeki fazla mesajlarını siler.
 * Kurallarla yapmak eski sürümlerin mesaj göndermesini bozacağı için sunucuda yapılır.
 */
exports.limitChatRate = onValueCreated("/rooms/{roomCode}/chat/{messageId}", async (event) => {
  const msg = event.data.val();
  if (!msg || !msg.senderId || typeof msg.timestamp !== "number") return null;

  const recent = await event.data.ref.parent
    .orderByChild("timestamp")
    .startAt(msg.timestamp - CHAT_RATE_WINDOW_MS)
    .once("value");
  const earlier = [];
  recent.forEach((child) => {
    const v = child.val();
    if (child.key === event.params.messageId || !v || v.senderId !== msg.senderId) return;
    // Aynı milisaniyedeki mesajlarda sırayı push anahtarı belirler.
    if (v.timestamp < msg.timestamp || (v.timestamp === msg.timestamp && child.key < event.params.messageId)) {
      earlier.push(v.timestamp);
    }
  });

  if (isChatRateExceeded(earlier, msg.timestamp)) {
    await event.data.ref.remove();
    logger.info(`Sohbet sınırı: oda=${event.params.roomCode} mesaj silindi`);
  }
  return null;
});

/**
 * Hızlı düello eşleştirmesi (sunucu tarafı).
 *
 * Eskiden her istemci tüm /queue üzerinde transaction çalıştırıyordu; bu yüzden kurallar
 * giriş yapmış herkesin kuyruğun tamamını silmesine izin vermek zorundaydı ve engelleme
 * yalnızca eşleştirmeyi yapan tarafın listesine bakıyordu. Artık istemci yalnızca kendi
 * kaydını yazar; çifti burada, iki yönlü engellemeyi kontrol ederek seçeriz.
 */
exports.pairFromQueue = onValueWritten("/queue/{uid}", async (event) => {
  if (!event.data.after.exists()) return null;
  const me = event.params.uid;
  const db = event.data.after.ref.root;
  const queueRef = db.child("queue");

  // Engeller transaction dışında okunur: nadiren değişir ve transaction içinde okuma yapılamaz.
  const [queueSnap, myBlocksSnap] = await Promise.all([
    queueRef.once("value"),
    db.child(`blocks/${me}`).once("value"),
  ]);
  const others = Object.keys(queueSnap.val() || {}).filter((uid) => uid !== me);
  if (others.length === 0) return null;

  const excluded = new Set(Object.keys(myBlocksSnap.val() || {}));
  const theirBlocks = await Promise.all(others.map((uid) => db.child(`blocks/${uid}/${me}`).once("value")));
  others.forEach((uid, i) => {
    if (theirBlocks[i].exists()) excluded.add(uid);
  });

  let pair = null;
  const result = await queueRef.transaction((current) => {
    pair = pickPair(current, me, excluded);
    // Yerel önbellek boşsa null dönmek sunucudaki gerçek veriyle yeniden denemeyi sağlar.
    if (!pair) return current === null ? null : undefined;
    delete current[pair.hostId];
    delete current[pair.guestId];
    return current;
  });
  if (!result.committed || !pair) return null;

  try {
    const code = await createQuickDuelRoom(db, pair);
    await db.update({
      [`matchmaking/${pair.hostId}/assignedRoom`]: code,
      [`matchmaking/${pair.guestId}/assignedRoom`]: code,
    });
    logger.info(`Hızlı düello: oda=${code}`);
  } catch (e) {
    // Kuyruğa burada geri yazmıyoruz: kalıcı bir hatada fonksiyon kendini sonsuza dek tetiklerdi.
    // Oda kodu gelmeyen istemciler REJOIN_TIMEOUT_MS sonra kuyruğa kendileri geri döner.
    logger.error("Hızlı düello odası kurulamadı", e);
  }
  return null;
});

async function createQuickDuelRoom(db, { hostId, guestId, deck }) {
  for (let attempt = 0; attempt < 5; attempt++) {
    const code = generateRoomCode();
    const roomRef = db.child(`rooms/${code}`);
    const result = await roomRef.transaction((current) => {
      if (current !== null) return undefined; // kod dolu
      return {
        status: "active",
        hostId,
        guestId,
        filters: { genreName: deck.genreName, genreId: deck.genreId },
        movieDeck: deck.movieIds,
        createdAt: { ".sv": "timestamp" }, // ServerValue.TIMESTAMP
      };
    });
    if (result.committed) return code;
  }
  throw new Error("Boş oda kodu bulunamadı");
}

/**
 * Bir filme yapılan her seçimde çalışır. Odanın iki oyuncusu da "like" dediyse
 * eşleşmeyi yazar. Eskiden yalnızca "güncelleme"de tetikleniyor, odadaki herhangi
 * iki kişinin seçimini sayıyor ve eşleşmeyi transaction olmadan yazıyordu.
 */
exports.checkForMatch = onValueWritten("/rooms/{roomCode}/choices/{movieId}", async (event) => {
  const { roomCode, movieId } = event.params;
  const choices = event.data.after.val();
  if (!choices) return null;

  // Odayı, olayın geldiği veritabanı örneğinden türet (choices/{movieId} -> rooms/{roomCode}).
  const roomRef = event.data.after.ref.parent.parent;
  const [hostSnap, guestSnap] = await Promise.all([
    roomRef.child("hostId").once("value"),
    roomRef.child("guestId").once("value"),
  ]);

  if (!isMatch(choices, hostSnap.val(), guestSnap.val())) return null;

  const matchedId = parseInt(movieId, 10);
  if (Number.isNaN(matchedId)) return null;

  // Aynı anda iki film eşleşirse yalnızca ilki yazılır.
  const result = await roomRef.child("match").transaction((current) => (current === null ? matchedId : undefined));
  if (result.committed) logger.info(`Eşleşme: oda=${roomCode} film=${matchedId}`);
  return null;
});

/**
 * Eski odaları, sohbetleri ve kuyrukta kalmış kayıtları temizler.
 * (Eskiden hiçbir veri silinmiyordu; gizlilik ve maliyet açısından sorunluydu.)
 */
exports.cleanupOldData = onSchedule("every 6 hours", async () => {
  const db = admin.database();
  const now = Date.now();
  const updates = {};

  const oldRooms = await db.ref("/rooms").orderByChild("createdAt").endAt(now - ROOM_TTL_MS).once("value");
  oldRooms.forEach((room) => {
    updates[`/rooms/${room.key}`] = null;
  });

  // createdAt alanı olmayan eski sürüm odaları: timestamp alanına bak.
  const legacyRooms = await db.ref("/rooms").orderByChild("timestamp").endAt(now - ROOM_TTL_MS).once("value");
  legacyRooms.forEach((room) => {
    if (room.child("timestamp").exists()) updates[`/rooms/${room.key}`] = null;
  });

  const staleQueue = await db.ref("/queue").orderByChild("ts").endAt(now - QUEUE_TTL_MS).once("value");
  staleQueue.forEach((entry) => {
    updates[`/queue/${entry.key}`] = null;
  });

  const count = Object.keys(updates).length;
  if (count > 0) await db.ref().update(updates);
  logger.info(`Temizlik tamamlandı: ${count} kayıt silindi.`);
});
