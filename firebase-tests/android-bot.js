/**
 * Android uygulamasını Firebase emülatöründe iki oyunculu test etmek için "bot" ikinci oyuncu.
 *
 *   node firebase-tests/android-bot.js guest   # bot kuyrukta bekler, uygulama host olur
 *   node firebase-tests/android-bot.js host    # bot uygulamayı yakalayıp odayı kurar
 *
 * Bot, odadaki tüm filmleri beğenir; uygulamada herhangi bir filme sağa kaydırmak eşleşme üretir.
 * Eşleşmeden sonra sohbete bir mesaj yazar ve uygulamadan gelen mesajları yazdırır.
 */
const { initializeTestEnvironment } = require("@firebase/rules-unit-testing");
const { setLogLevel } = require("firebase/app");
const { ref, set, get, onValue, push, serverTimestamp, runTransaction } = require("firebase/database");

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
  await set(ref(db, `queue/${BOT}`), { ts: serverTimestamp() });
  log(`kuyruğa girildi (mod=${MODE})`);

  if (MODE === "host") {
    onValue(ref(db, "queue"), async (s) => {
      const q = s.val() || {};
      const others = Object.keys(q).filter((k) => k !== BOT);
      if (roomStarted || others.length === 0 || !(BOT in q)) return;
      let opponent = null;
      const r = await runTransaction(ref(db, "queue"), (cur) => {
        if (!cur || !(BOT in cur)) return cur === null ? cur : undefined;
        opponent = Object.keys(cur).find((k) => k !== BOT) || null;
        if (!opponent) return undefined;
        delete cur[BOT];
        delete cur[opponent];
        return cur;
      });
      if (!r.committed || !opponent) return;
      const code = "BOT" + Math.random().toString(36).slice(2, 5).toUpperCase().replace(/[^A-Z0-9]/g, "Z");
      await set(ref(db, `rooms/${code}`), {
        status: "active", hostId: BOT, guestId: opponent, filters: { genreName: "Bot Testi" },
        movieDeck: [550, 680, 13, 155, 27205], createdAt: serverTimestamp(),
      });
      await set(ref(db, `matchmaking/${opponent}/assignedRoom`), code);
      log(`uygulama kullanıcısı (${opponent}) yakalandı, oda ${code} kuruldu`);
      playRoom(code);
    });
  } else {
    onValue(ref(db, "queue"), (s) => log(`kuyruk: ${Object.keys(s.val() || {}).join(", ") || "(boş)"}`));
  }
})().catch((e) => { console.error(e); process.exit(1); });
