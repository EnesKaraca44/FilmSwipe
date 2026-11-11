"use strict";

/**
 * Sohbet sıklık sınırı (saf mantık; test/chat.test.js ile test edilir).
 * Uygulamadaki ChatRateLimit.kt ile aynı değerler kullanılmalı.
 */
const CHAT_RATE_MAX = 5; // pencere içinde izin verilen mesaj sayısı
const CHAT_RATE_WINDOW_MS = 10 * 1000;

/**
 * @param {number[]} earlierTimestamps aynı göndericinin bu mesajdan önceki (silinmemiş) mesaj zamanları
 * @param {number} timestamp bu mesajın zamanı
 * @returns {boolean} mesaj sınırı aşıyorsa true (silinmeli)
 */
function isChatRateExceeded(earlierTimestamps, timestamp) {
  if (typeof timestamp !== "number") return false;
  const inWindow = earlierTimestamps.filter(
    (t) => typeof t === "number" && t <= timestamp && t > timestamp - CHAT_RATE_WINDOW_MS
  );
  return inWindow.length >= CHAT_RATE_MAX;
}

module.exports = { isChatRateExceeded, CHAT_RATE_MAX, CHAT_RATE_WINDOW_MS };
