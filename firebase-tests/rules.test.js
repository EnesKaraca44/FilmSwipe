/**
 * database.rules.json için güvenlik testleri.
 * Çalıştırma (proje kökünden):
 *   firebase emulators:exec --only database --project demo-filmswipe "npm --prefix firebase-tests test -- rules"
 */
const fs = require("fs");
const path = require("path");
const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { ref, set, update, get, push, remove, serverTimestamp, runTransaction } = require("firebase/database");
require("firebase/app").setLogLevel("silent");

const PROJECT_ID = "demo-filmswipe";
let env;

const db = (uid) => (uid ? env.authenticatedContext(uid) : env.unauthenticatedContext()).database();

function newRoom(hostId, extra = {}) {
  return {
    status: "waiting",
    hostId,
    filters: { genres: [28], startYear: 2000 },
    movieDeck: [550, 551, 552],
    createdAt: serverTimestamp(),
    ...extra,
  };
}

/** Kuralları atlayarak hazır veri yazar. */
async function seed(p, value) {
  await env.withSecurityRulesDisabled(async (ctx) => {
    await set(ref(ctx.database(), p), value);
  });
}

beforeAll(async () => {
  const [host, port] = (process.env.FIREBASE_DATABASE_EMULATOR_HOST || "127.0.0.1:9000").split(":");
  env = await initializeTestEnvironment({
    projectId: PROJECT_ID,
    database: {
      host,
      port: Number(port),
      rules: fs.readFileSync(path.join(__dirname, "..", "database.rules.json"), "utf8"),
    },
  });
});

afterEach(async () => env.clearDatabase());
afterAll(async () => env.cleanup());

describe("oda oluşturma", () => {
  test("giriş yapmamış kullanıcı oda kuramaz", async () => {
    await assertFails(set(ref(db(null), "rooms/ABC123"), newRoom("alice")));
  });

  test("kullanıcı kendi adına oda kurabilir", async () => {
    await assertSucceeds(set(ref(db("alice"), "rooms/ABC123"), newRoom("alice")));
  });

  test("başkası adına (hostId sahte) oda kurulamaz", async () => {
    await assertFails(set(ref(db("mallory"), "rooms/ABC123"), newRoom("alice")));
  });

  test("var olan odanın üzerine yazılamaz (kod çakışması koruması)", async () => {
    await seed("rooms/ABC123", { ...newRoom("alice"), createdAt: Date.now() });
    await assertFails(set(ref(db("bob"), "rooms/ABC123"), newRoom("bob")));
    await assertFails(set(ref(db("alice"), "rooms/ABC123"), newRoom("alice")));
  });

  test("geçersiz kod biçimi ve eksik alanlar reddedilir", async () => {
    await assertFails(set(ref(db("alice"), "rooms/abc"), newRoom("alice")));
    const { movieDeck, ...noDeck } = newRoom("alice");
    await assertFails(set(ref(db("alice"), "rooms/ABC123"), noDeck));
  });

  test("oda listesi okunamaz (kodlar tahmin edilmeden bulunamaz)", async () => {
    await seed("rooms/ABC123", { ...newRoom("alice"), createdAt: Date.now() });
    await assertFails(get(ref(db("bob"), "rooms")));
  });

  test("sadece host odayı silebilir", async () => {
    await seed("rooms/ABC123", { ...newRoom("alice"), createdAt: Date.now() });
    await assertFails(remove(ref(db("bob"), "rooms/ABC123")));
    await assertSucceeds(remove(ref(db("alice"), "rooms/ABC123")));
  });
});

describe("odaya katılma", () => {
  beforeEach(async () => {
    await seed("rooms/ABC123", { ...newRoom("alice"), createdAt: Date.now() });
  });

  test("bekleyen oda okunabilir ve misafir katılabilir", async () => {
    await assertSucceeds(get(ref(db("bob"), "rooms/ABC123")));
    await assertSucceeds(update(ref(db("bob"), "rooms/ABC123"), { guestId: "bob", status: "active" }));
  });

  test("başkasının adına katılınamaz", async () => {
    await assertFails(update(ref(db("bob"), "rooms/ABC123"), { guestId: "carol", status: "active" }));
  });

  test("dolu odaya üçüncü kişi katılamaz ve misafiri değiştiremez", async () => {
    await update(ref(db("bob"), "rooms/ABC123"), { guestId: "bob", status: "active" });
    await assertFails(update(ref(db("carol"), "rooms/ABC123"), { guestId: "carol", status: "active" }));
    await assertFails(set(ref(db("carol"), "rooms/ABC123/guestId"), "carol"));
  });

  test("host kendi odasına misafir olarak katılamaz", async () => {
    await assertFails(update(ref(db("alice"), "rooms/ABC123"), { guestId: "alice", status: "active" }));
  });

  test("aktif oda sadece katılımcılara görünür (sohbet gizliliği)", async () => {
    await update(ref(db("bob"), "rooms/ABC123"), { guestId: "bob", status: "active" });
    await assertSucceeds(get(ref(db("alice"), "rooms/ABC123")));
    await assertSucceeds(get(ref(db("bob"), "rooms/ABC123/chat")));
    await assertFails(get(ref(db("carol"), "rooms/ABC123")));
    await assertFails(get(ref(db("carol"), "rooms/ABC123/chat")));
  });
});

describe("oyun içi yazmalar", () => {
  beforeEach(async () => {
    await seed("rooms/ABC123", { ...newRoom("alice"), createdAt: Date.now(), guestId: "bob", status: "active" });
  });

  test("oyuncular kendi seçimlerini yazabilir", async () => {
    await assertSucceeds(set(ref(db("alice"), "rooms/ABC123/choices/550/alice"), "like"));
    await assertSucceeds(set(ref(db("bob"), "rooms/ABC123/choices/550/bob"), "pass"));
  });

  test("başkasının seçimi yazılamaz, geçersiz değer ve yabancı oyuncu reddedilir", async () => {
    await assertFails(set(ref(db("alice"), "rooms/ABC123/choices/550/bob"), "like"));
    await assertFails(set(ref(db("alice"), "rooms/ABC123/choices/550/alice"), "love"));
    await assertFails(set(ref(db("carol"), "rooms/ABC123/choices/550/carol"), "like"));
  });

  test("eşleşmeyi (match) istemci yazamaz; sadece Cloud Function", async () => {
    await assertFails(set(ref(db("alice"), "rooms/ABC123/match"), 550));
  });

  test("bitiş işareti sadece kendi adına", async () => {
    await assertSucceeds(set(ref(db("alice"), "rooms/ABC123/finished/alice"), true));
    await assertFails(set(ref(db("alice"), "rooms/ABC123/finished/bob"), true));
  });

  test("odaya tanımsız alan eklenemez", async () => {
    await assertFails(set(ref(db("alice"), "rooms/ABC123/hacked"), true));
  });
});

describe("sohbet", () => {
  beforeEach(async () => {
    await seed("rooms/ABC123", { ...newRoom("alice"), createdAt: Date.now(), guestId: "bob", status: "active" });
  });

  const msg = (senderId, text) => ({ senderId, senderNickname: "Nick", text, timestamp: serverTimestamp() });

  test("katılımcı mesaj gönderebilir", async () => {
    await assertSucceeds(push(ref(db("bob"), "rooms/ABC123/chat"), msg("bob", "Merhaba!")));
  });

  test("başkası adına, boş, çok uzun mesaj veya yabancı reddedilir", async () => {
    await assertFails(push(ref(db("bob"), "rooms/ABC123/chat"), msg("alice", "ben alice'im")));
    await assertFails(push(ref(db("bob"), "rooms/ABC123/chat"), msg("bob", "")));
    await assertFails(push(ref(db("bob"), "rooms/ABC123/chat"), msg("bob", "x".repeat(501))));
    await assertFails(push(ref(db("carol"), "rooms/ABC123/chat"), msg("carol", "selam")));
  });

  test("gönderilmiş mesaj değiştirilemez", async () => {
    const r = await push(ref(db("bob"), "rooms/ABC123/chat"), msg("bob", "ilk"));
    await assertFails(set(ref(db("bob"), `rooms/ABC123/chat/${r.key}`), msg("bob", "değişti")));
  });
});

describe("hızlı düello kuyruğu ve posta kutusu", () => {
  const deck = { genreName: "Aksiyon", genreId: 28, movieIds: [550, 551, 552] };

  test("kullanıcı kendini (desteyle) kuyruğa ekleyebilir, başkasını ekleyemez", async () => {
    await assertSucceeds(set(ref(db("alice"), "queue/alice"), { ts: serverTimestamp(), deck }));
    await assertSucceeds(set(ref(db("dave"), "queue/dave"), { ts: serverTimestamp() })); // eski sürüm
    await assertFails(set(ref(db("alice"), "queue/bob"), { ts: serverTimestamp() }));
    await assertFails(set(ref(db(null), "queue/anon"), { ts: serverTimestamp() }));
  });

  test("geçersiz veya çok büyük deste reddedilir", async () => {
    const big = Array.from({ length: 41 }, (_, i) => i + 1);
    await assertFails(set(ref(db("alice"), "queue/alice"), { ts: serverTimestamp(), deck: { ...deck, movieIds: big } }));
    await assertFails(set(ref(db("alice"), "queue/alice"), { ts: serverTimestamp(), deck: { ...deck, movieIds: ["x"] } }));
    await assertFails(set(ref(db("alice"), "queue/alice"), { ts: serverTimestamp(), deck: { genreName: "A" } }));
    await assertFails(set(ref(db("alice"), "queue/alice"), { ts: serverTimestamp(), extra: 1 }));
  });

  test("kimse kuyruğun tamamını veya başkasının kaydını silemez", async () => {
    await seed("queue", { alice: { ts: 1 }, bob: { ts: 2 }, carol: { ts: 3 } });
    await assertFails(remove(ref(db("mallory"), "queue")));
    await assertFails(remove(ref(db("alice"), "queue/bob")));
    await assertFails(set(ref(db("alice"), "queue/bob/ts"), 5));
    const result = await runTransaction(ref(db("alice"), "queue"), (q) => {
      if (!q) return q;
      delete q.alice;
      delete q.bob;
      return q;
    }).catch((e) => e);
    expect(result instanceof Error).toBe(true);
    await assertSucceeds(remove(ref(db("alice"), "queue/alice")));
  });

  test("posta kutusuna istemci yazamaz (host bile); oda kodunu sadece sunucu atar", async () => {
    await seed("rooms/ABC123", { ...newRoom("alice"), createdAt: Date.now(), guestId: "bob", status: "active" });
    await assertFails(set(ref(db("alice"), "matchmaking/bob/assignedRoom"), "ABC123"));
    await assertFails(set(ref(db("bob"), "matchmaking/bob/assignedRoom"), "ABC123"));
    await assertFails(set(ref(db("mallory"), "matchmaking/bob/assignedRoom"), "ABC123"));
  });

  test("posta kutusunu sadece sahibi okuyabilir ve temizleyebilir", async () => {
    await seed("matchmaking/bob", { assignedRoom: "ABC123" });
    await assertSucceeds(get(ref(db("bob"), "matchmaking/bob/assignedRoom")));
    await assertFails(get(ref(db("mallory"), "matchmaking/bob")));
    await assertFails(remove(ref(db("mallory"), "matchmaking/bob")));
    await assertSucceeds(remove(ref(db("bob"), "matchmaking/bob")));
  });
});

describe("engelleme", () => {
  test("kullanıcı kendi engel listesini yazıp okuyabilir, başkasınınkini değil", async () => {
    await assertSucceeds(set(ref(db("alice"), "blocks/alice/mallory"), true));
    await assertSucceeds(get(ref(db("alice"), "blocks/alice")));
    await assertFails(get(ref(db("mallory"), "blocks/alice")));
    await assertFails(set(ref(db("mallory"), "blocks/alice/bob"), true));
    await assertFails(remove(ref(db("mallory"), "blocks/alice")));
  });

  test("geçersiz engel kaydı reddedilir", async () => {
    await assertFails(set(ref(db("alice"), "blocks/alice/alice"), true));
    await assertFails(set(ref(db("alice"), "blocks/alice/bob"), "evet"));
  });

  test("engellenen kişi aynı odada mesaj yazamaz, engelleyen yazabilir", async () => {
    await seed("rooms/ABC123", { ...newRoom("alice"), createdAt: Date.now(), guestId: "bob", status: "active" });
    const msg = (senderId) => ({ senderId, senderNickname: "Nick", text: "selam", timestamp: serverTimestamp() });
    await assertSucceeds(push(ref(db("bob"), "rooms/ABC123/chat"), msg("bob")));
    await seed("blocks/alice", { bob: true });
    await assertFails(push(ref(db("bob"), "rooms/ABC123/chat"), msg("bob")));
    await assertSucceeds(push(ref(db("alice"), "rooms/ABC123/chat"), msg("alice")));
  });
});

describe("şikayetler", () => {
  const report = (reporterId) => ({
    roomCode: "ABC123",
    reporterId,
    reportedUserId: "mallory",
    reason: "Hakaret / taciz",
    messageText: "kötü mesaj",
    createdAt: serverTimestamp(),
  });

  test("kullanıcı kendi adına şikayet gönderebilir ama okuyamaz", async () => {
    await assertSucceeds(push(ref(db("alice"), "reports"), report("alice")));
    await assertFails(get(ref(db("alice"), "reports")));
  });

  test("başkası adına şikayet gönderilemez", async () => {
    await assertFails(push(ref(db("alice"), "reports"), report("bob")));
  });
});
