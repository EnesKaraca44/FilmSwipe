/**
 * Android uygulamasını Firebase emülatöründe iki oyunculu test etmek için "bot" ikinci oyuncu.
 *
 *   node firebase-tests/android-bot.js guest   # bot destesiz katılır, odayı uygulamanın destesiyle sunucu kurar
 *   node firebase-tests/android-bot.js host    # bot desteyle önce katılır, oda botun destesiyle kurulur
 *
 * Eşleştirmeyi Cloud Function (pairFromQueue) yapar; bu yüzden emülatör "database,functions" ile çalışmalı.
 *
 * Bot, odadaki tüm filmleri beğenir; uygulamada herhangi bir filme sağa kaydırmak eşleşme üretir.
 * Eşleşmeden sonra sohbete bir mesaj yazar ve uygulamadan gelen mesajları yazdırır.
 */
const { initializeTestEnvironment } = require("@firebase/rules-unit-testing");
const { setLogLevel } = require("firebase/app");
const { ref, set, get, onValue, push, serverTimestamp } = require("firebase/database");

setLogLevel("error");
const MODE = process.argv[2] || "guest";
const PROJECT_ID = "filmswipee"; // google-services.json ile aynı; sadece EMÜLATÖR kullanılır
const DB_URL = "http://127.0.0.1:9000?ns=filmswipee-default-rtdb";
const BOT = "bot-player";
const log = (...a) => console.log(new Date().toISOString().slice(11, 19), "[bot]", ...a);

(async () => {
  const env = await initializeTestEnvironment({ projectId: PROJECT_ID, database: { host: "127.0.0.1", port: 9000 } });
  const db = env.authenticatedContext(BOT).database(DB_URL);

  let roomStarted = false;
  const playRoom = async (code) => {
    if (roomStarted) return;
    roomStarted = true;
    const room = (await get(ref(db, `rooms/${code}`))).val();
    log(`odaya girildi ${code}: host=${room.hostId} guest=${room.guestId} deste=${(room.movieDeck || []).length} film`);
    for (const movieId of room.movieDeck || []) {
      await set(ref(db, `rooms/${code}/choices/${movieId}/${BOT}`), "like");
    }
    log("tüm filmler beğenildi, uygulamanın kaydırması bekleniyor");
    onValue(ref(db, `rooms/${code}/match`), async (s) => {
      if (s.val() == null) return;
      log(`EŞLEŞME film=${s.val()}`);
      await push(ref(db, `rooms/${code}/chat`), {
        senderId: BOT, senderNickname: "Test Botu", text: "Merhaba! Ben test botuyum.", timestamp: serverTimestamp(),
      });
      log("sohbete mesaj gönderildi");
    });
    onValue(ref(db, `rooms/${code}/chat`), (s) => {
      s.forEach((m) => { if (m.val().senderId !== BOT) log(`uygulamadan mesaj: "${m.val().text}" (${m.val().senderNickname})`); });
    });
  };

  await set(ref(db, `matchmaking/${BOT}`), null);
  onValue(ref(db, `matchmaking/${BOT}/assignedRoom`), (s) => {
    if (s.val()) { log(`posta kutusuna oda atandı: ${s.val()}`); playRoom(s.val()); }
  });
  const deck = { genreName: "Bot Testi", genreId: 28, movieIds: [550, 680, 13, 155, 27205] };
  await set(ref(db, `queue/${BOT}`), MODE === "host" ? { ts: serverTimestamp(), deck } : { ts: serverTimestamp() });
  log(`kuyruğa girildi (mod=${MODE})`);
  onValue(ref(db, "queue"), (s) => log(`kuyruk: ${Object.keys(s.val() || {}).join(", ") || "(boş)"}`));
})().catch((e) => { console.error(e); process.exit(1); });
