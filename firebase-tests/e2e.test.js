/**
 * Uçtan uca testler: gerçek kurallar + Cloud Functions emülatörü.
 * Çalıştırma (proje kökünden):
 *   firebase emulators:exec --only database,functions --project demo-filmswipe "npm --prefix firebase-tests test -- e2e"
 */
const fs = require("fs");
const path = require("path");
const { initializeTestEnvironment } = require("@firebase/rules-unit-testing");
const { setLogLevel } = require("firebase/app");
const { ref, set, update, get, push, serverTimestamp } = require("firebase/database");

setLogLevel("error");

const PROJECT_ID = "demo-filmswipe";
// Cloud Functions emülatörü varsayılan örneği (<proje>-default-rtdb) dinler.
const NS = `${PROJECT_ID}-default-rtdb`;
const [HOST, PORT] = (process.env.FIREBASE_DATABASE_EMULATOR_HOST || "127.0.0.1:9000").split(":");
const DB_URL = `http://${HOST}:${PORT}?ns=${NS}`;
const RULES = fs.readFileSync(path.join(__dirname, "..", "database.rules.json"), "utf8");

let env;
const db = (uid) => env.authenticatedContext(uid).database(DB_URL);
const sleep = (ms) => new Promise((r) => setTimeout(r, ms));

async function waitFor(fn, timeoutMs = 20000) {
  const start = Date.now();
  for (;;) {
    const value = await fn();
    if (value !== null && value !== undefined) return value;
    if (Date.now() - start > timeoutMs) return null;
    await sleep(300);
  }
}

async function adminSet(p, value) {
  await env.withSecurityRulesDisabled(async (ctx) => set(ref(ctx.database(DB_URL), p), value));
}
async function adminGet(p) {
  let v;
  await env.withSecurityRulesDisabled(async (ctx) => {
    v = (await get(ref(ctx.database(DB_URL), p))).val();
  });
  return v;
}

beforeAll(async () => {
  env = await initializeTestEnvironment({ projectId: PROJECT_ID, database: { host: HOST, port: Number(PORT) } });
  // Kuralları fonksiyonların dinlediği isim alanına da yükle.
  const res = await fetch(`http://${HOST}:${PORT}/.settings/rules.json?ns=${NS}`, {
    method: "PUT",
    headers: { Authorization: "Bearer owner" },
    body: RULES,
  });
  if (!res.ok) throw new Error(`Kurallar yüklenemedi: ${res.status} ${await res.text()}`);
});

beforeEach(async () => adminSet("/", null));
afterAll(async () => {
  await env.cleanup();
  // cleanupOldData testinin açtığı firebase-admin bağlantılarını kapat (yoksa Jest çıkmaz).
  const admin = require(require.resolve("firebase-admin", { paths: [path.join(__dirname, "..", "functions")] }));
  await Promise.all(admin.apps.map((app) => app && app.delete()));
});

test("iki oyuncu aynı filmi beğenince Cloud Function eşleşmeyi yazar", async () => {
  const host = db("host1");
  const guest = db("guest1");

  await set(ref(host, "rooms/ROOM01"), {
    status: "waiting",
    hostId: "host1",
    filters: { genres: [28] },
    movieDeck: [550, 551],
    createdAt: serverTimestamp(),
  });
  await update(ref(guest, "rooms/ROOM01"), { guestId: "guest1", status: "active" });

  // Film 550: biri beğenmedi -> eşleşme yok
  await set(ref(host, "rooms/ROOM01/choices/550/host1"), "like");
  await set(ref(guest, "rooms/ROOM01/choices/550/guest1"), "pass");
  await sleep(2500);
  expect(await adminGet("rooms/ROOM01/match")).toBeNull();

  // Film 551: ikisi de beğendi -> eşleşme 551
  await set(ref(host, "rooms/ROOM01/choices/551/host1"), "like");
  await set(ref(guest, "rooms/ROOM01/choices/551/guest1"), "like");
  const match = await waitFor(() => adminGet("rooms/ROOM01/match"));
  expect(match).toBe(551);
});

test("eski sürüm odaları (hostId'siz) da eşleşmeye devam eder", async () => {
  await adminSet("rooms/LEGACY", {
    status: "active",
    guestId: "guest_123",
    movieDeck: [700],
    timestamp: Date.now(),
    choices: { 700: { a: "like" } },
  });
  await adminSet("rooms/LEGACY/choices/700/b", "like");
  expect(await waitFor(() => adminGet("rooms/LEGACY/match"))).toBe(700);
});

const DECK = { genreName: "Aksiyon", genreId: 28, movieIds: [550, 551, 552] };
const joinQueue = (uid, deck = DECK) => set(ref(db(uid), `queue/${uid}`), deck ? { ts: serverTimestamp(), deck } : { ts: serverTimestamp() });
const assigned = (uid) => adminGet(`matchmaking/${uid}/assignedRoom`);

test("sunucu eşleştirmesi: aynı anda kuyruğa giren 4 kişi ikişer ikişer, birer kez eşleşir", async () => {
  const users = ["u1", "u2", "u3", "u4"];
  await Promise.all(users.map((u) => joinQueue(u)));

  const codes = await Promise.all(users.map((u) => waitFor(() => assigned(u))));
  expect(codes.every(Boolean)).toBe(true);
  expect(new Set(codes).size).toBe(2);

  for (const code of new Set(codes)) {
    const room = await adminGet(`rooms/${code}`);
    expect(room.status).toBe("active");
    expect(room.movieDeck).toEqual(DECK.movieIds);
    const members = users.filter((u, i) => codes[i] === code);
    expect([room.hostId, room.guestId].sort()).toEqual(members.sort());
  }
  expect(await adminGet("queue")).toBeNull();
});

test("engelleme iki yönlü: engellenen kişi engelleyenle eşleşmez", async () => {
  await set(ref(db("alice"), "blocks/alice/bob"), true);
  await joinQueue("alice");
  await sleep(300);
  await joinQueue("bob"); // bob, alice'i engellemedi ama alice onu engelledi
  await sleep(3000);
  expect(await assigned("alice")).toBeNull();
  expect(await assigned("bob")).toBeNull();

  await joinQueue("carol");
  const code = await waitFor(() => assigned("carol"));
  expect(code).not.toBeNull();
  expect(await assigned("alice")).toBe(code); // en uzun bekleyen uygun kişi
  expect(await assigned("bob")).toBeNull();
  expect(Object.keys(await adminGet("queue"))).toEqual(["bob"]);
});

test("eski sürüm (destesiz) kullanıcı, destesi olan yeni sürümle eşleşir; o host olur", async () => {
  await joinQueue("old", null);
  await sleep(300);
  await joinQueue("new");
  const code = await waitFor(() => assigned("old"));
  const room = await adminGet(`rooms/${code}`);
  expect(room.hostId).toBe("new");
  expect(room.guestId).toBe("old");
});

test("sohbet sınırı: 10 saniyede 5'ten fazla mesajın fazlası silinir, karşı taraf etkilenmez", async () => {
  await adminSet("rooms/CHAT01", {
    status: "active", hostId: "spam", guestId: "calm", movieDeck: [1], createdAt: Date.now(),
  });
  const msg = (uid, i) => ({ senderId: uid, senderNickname: uid, text: `m${i}`, timestamp: serverTimestamp() });
  for (let i = 0; i < 8; i++) await push(ref(db("spam"), "rooms/CHAT01/chat"), msg("spam", i));
  await push(ref(db("calm"), "rooms/CHAT01/chat"), msg("calm", 0));

  await sleep(4000);
  const chat = Object.values((await adminGet("rooms/CHAT01/chat")) || {});
  const spam = chat.filter((m) => m.senderId === "spam").map((m) => m.text).sort();
  expect(spam).toEqual(["m0", "m1", "m2", "m3", "m4"]);
  expect(chat.filter((m) => m.senderId === "calm")).toHaveLength(1);
});

test("temizlik fonksiyonu 24 saatten eski odaları ve bayat kuyruk kayıtlarını siler", async () => {
  const now = Date.now();
  await adminSet("rooms/OLD001", { status: "active", hostId: "h", movieDeck: [1], createdAt: now - 25 * 3600e3 });
  await adminSet("rooms/NEW001", { status: "active", hostId: "h", movieDeck: [1], createdAt: now - 3600e3 });
  await adminSet("rooms/LEG001", { status: "active", movieDeck: [1], timestamp: now - 30 * 3600e3 });
  await adminSet("queue/ghost", { ts: now - 20 * 60e3 });
  await adminSet("queue/fresh", { ts: now });

  process.env.FIREBASE_CONFIG = JSON.stringify({ projectId: PROJECT_ID, databaseURL: DB_URL });
  process.env.GCLOUD_PROJECT = PROJECT_ID;
  const { cleanupOldData } = require("../functions/index.js");
  await cleanupOldData.run({});

  expect(await adminGet("rooms/OLD001")).toBeNull();
  expect(await adminGet("rooms/LEG001")).toBeNull();
  expect(await adminGet("rooms/NEW001")).not.toBeNull();
  expect(await adminGet("queue/ghost")).toBeNull();
  expect(await adminGet("queue/fresh")).not.toBeNull();
});
