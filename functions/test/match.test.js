const { isMatch } = require("../match");

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
