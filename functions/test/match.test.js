const { isMatch, pickPair, normalizeDeck, generateRoomCode, ROOM_CODE_ALPHABET } = require("../match");

describe("isMatch", () => {
  test("iki oyuncu da beğendiyse eşleşme", () => {
    expect(isMatch({ host: "like", guest: "like" }, "host", "guest")).toBe(true);
  });

  test("biri geçtiyse eşleşme yok", () => {
    expect(isMatch({ host: "like", guest: "pass" }, "host", "guest")).toBe(false);
  });

  test("sadece bir oyuncu seçtiyse eşleşme yok", () => {
    expect(isMatch({ host: "like" }, "host", "guest")).toBe(false);
  });

  test("odaya ait olmayan üçüncü kişinin beğenisi sayılmaz", () => {
    expect(isMatch({ host: "like", intruder: "like" }, "host", "guest")).toBe(false);
  });

  test("eski sürüm odaları: hostId yoksa en az iki 'like'", () => {
    expect(isMatch({ a: "like", b: "like" }, null, "guest_1")).toBe(true);
    expect(isMatch({ a: "like" }, null, null)).toBe(false);
    expect(isMatch({ a: "like", b: "pass" }, undefined, undefined)).toBe(false);
  });

  test("boş/geçersiz veri", () => {
    expect(isMatch(null, "h", "g")).toBe(false);
    expect(isMatch("like", "h", "g")).toBe(false);
  });
});

const deck = (ids = [550, 551]) => ({ genreName: "Aksiyon", genreId: 28, movieIds: ids });

describe("pickPair", () => {
  test("en uzun bekleyen rakibi seçer, destesi varsa o host olur", () => {
    const queue = { a: { ts: 3, deck: deck() }, b: { ts: 1, deck: deck([7]) }, me: { ts: 5, deck: deck() } };
    expect(pickPair(queue, "me")).toEqual({ hostId: "b", guestId: "me", deck: deck([7]) });
  });

  test("tek başına kuyruktaysam veya kuyrukta değilsem eşleşme yok", () => {
    expect(pickPair({ me: { ts: 1, deck: deck() } }, "me")).toBeNull();
    expect(pickPair({ a: { ts: 1, deck: deck() } }, "me")).toBeNull();
    expect(pickPair(null, "me")).toBeNull();
  });

  test("engelleme ilişkisi olan kullanıcı atlanır", () => {
    const queue = { bad: { ts: 1, deck: deck() }, good: { ts: 2, deck: deck() }, me: { ts: 3, deck: deck() } };
    expect(pickPair(queue, "me", new Set(["bad"])).hostId).toBe("good");
    expect(pickPair({ bad: { ts: 1, deck: deck() }, me: { ts: 2, deck: deck() } }, "me", new Set(["bad"]))).toBeNull();
  });

  test("eski sürüm (destesiz) rakip: yeni gelen kendi destesiyle host olur", () => {
    const queue = { old: { ts: 1 }, me: { ts: 2, deck: deck() } };
    expect(pickPair(queue, "me")).toEqual({ hostId: "me", guestId: "old", deck: deck() });
  });

  test("iki taraf da destesizse eşleşme kurulamaz", () => {
    expect(pickPair({ old: { ts: 1 }, me: { ts: 2 } }, "me")).toBeNull();
  });

  test("zamanı olmayan kayıt en sona, eşitlikte uid sırası", () => {
    const queue = {
      z: { deck: deck() },
      y: { ts: 4, deck: deck() },
      x: { ts: 4, deck: deck() },
      me: { ts: 9, deck: deck() },
    };
    expect(pickPair(queue, "me").hostId).toBe("x");
  });
});

describe("normalizeDeck", () => {
  test("geçersiz film kimliklerini atar, nesne biçimindeki listeyi kabul eder", () => {
    expect(normalizeDeck({ deck: { genreName: "K", genreId: 35, movieIds: { 0: 5, 1: "x", 2: -1, 3: 9 } } }))
      .toEqual({ genreName: "K", genreId: 35, movieIds: [5, 9] });
  });

  test("boş veya eksik deste null", () => {
    expect(normalizeDeck({ ts: 1 })).toBeNull();
    expect(normalizeDeck({ deck: { movieIds: [] } })).toBeNull();
  });
});

test("oda kodu uygulamanın alfabesiyle 6 karakter", () => {
  for (let i = 0; i < 50; i++) {
    const code = generateRoomCode();
    expect(code).toMatch(/^[A-Z0-9]{6}$/);
    expect([...code].every((c) => ROOM_CODE_ALPHABET.includes(c))).toBe(true);
  }
});
