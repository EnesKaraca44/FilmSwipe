const { isChatRateExceeded, CHAT_RATE_MAX, CHAT_RATE_WINDOW_MS } = require("../chat");

describe("isChatRateExceeded", () => {
  const now = 1_000_000;

  test("pencerede sınırdan az mesaj varsa izin verilir", () => {
    const earlier = Array.from({ length: CHAT_RATE_MAX - 1 }, (_, i) => now - i * 100);
    expect(isChatRateExceeded(earlier, now)).toBe(false);
  });

  test("pencerede sınır kadar mesaj varsa yenisi reddedilir", () => {
    const earlier = Array.from({ length: CHAT_RATE_MAX }, (_, i) => now - i * 100);
    expect(isChatRateExceeded(earlier, now)).toBe(true);
  });

  test("pencereden eski mesajlar sayılmaz", () => {
    const old = Array.from({ length: 10 }, (_, i) => now - CHAT_RATE_WINDOW_MS - i);
    expect(isChatRateExceeded(old, now)).toBe(false);
  });

  test("ilk mesaj ve geçersiz veri", () => {
    expect(isChatRateExceeded([], now)).toBe(false);
    expect(isChatRateExceeded([now, now, now, now, now], undefined)).toBe(false);
    expect(isChatRateExceeded(["x", null, now], now)).toBe(false);
  });
});
