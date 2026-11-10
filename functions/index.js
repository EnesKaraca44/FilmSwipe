const { onValueWritten } = require("firebase-functions/v2/database");
const { onSchedule } = require("firebase-functions/v2/scheduler");
const { logger } = require("firebase-functions");
const admin = require("firebase-admin");
const { isMatch, ROOM_TTL_MS, QUEUE_TTL_MS } = require("./match");

admin.initializeApp();

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
