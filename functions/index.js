const { onValueUpdated } = require("firebase-functions/v2/database");
const { logger } = require("firebase-functions");
const admin = require("firebase-admin");

admin.initializeApp();

// YENİ VE DAHA GÜVENİLİR TETİKLEYİCİ:
// Artık .../{userId} seviyesini değil, bir üstü olan .../{movieId} seviyesini dinliyoruz.
// Ve onValueWritten yerine onValueUpdated kullanıyoruz.
exports.checkForMatch = onValueUpdated(
  "/rooms/{roomCode}/choices/{movieId}",
  async (event) => {

    const { roomCode, movieId } = event.params;

    // Eşleşme zaten bulunduysa, tekrar kontrol etme.
    const matchSnapshot = await admin
      .database()
      .ref(`/rooms/${roomCode}/match`)
      .once("value");
    if (matchSnapshot.exists()) {
      logger.info("Eşleşme zaten var, işlem durduruldu.");
      return null;
    }

    // event.data.after.val() komutu, değişiklikten SONRAKI tüm veriyi bize verir.
    // Yani { "userId1": "like", "userId2": "like" } gibi bir nesne.
    const choices = event.data.after.val();

    // Yeterli seçim olup olmadığını kontrol edelim. Object.keys ile anahtar sayısını alırız.
    if (Object.keys(choices).length < 2) {
      logger.info("Henüz yeterli seçim yok.");
      return null;
    }

    let isMatch = true;
    for (const userId in choices) {
      if (choices[userId] !== "like") {
        isMatch = false;
        break;
      }
    }

    if (isMatch) {
      logger.info(`EŞLEŞME BULUNDU! Oda: ${roomCode}, Film: ${movieId}`);
      const roomRef = admin.database().ref(`/rooms/${roomCode}`);
      return roomRef.child("match").set(parseInt(movieId, 10));
    }

    logger.info("Eşleşme bulunamadı, tüm seçimler 'like' değil.");
    return null;
  }
);