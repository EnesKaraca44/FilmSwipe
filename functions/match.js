"use strict";

/**
 * Saf (Firebase'den bağımsız) eşleşme mantığı; test/match.test.js ile test edilir.
 *
 * @param {Object<string,string>|null} choices  { uid: "like" | "pass" }
 * @param {string|null|undefined} hostId
 * @param {string|null|undefined} guestId
 * @returns {boolean} iki oyuncu da filmi beğendiyse true
 */
function isMatch(choices, hostId, guestId) {
  if (!choices || typeof choices !== "object") return false;

  if (hostId && guestId) {
    // Yeni sürüm: sadece odanın gerçek iki oyuncusunun seçimi sayılır.
    return choices[hostId] === "like" && choices[guestId] === "like";
  }

  // Eski sürüm odaları (hostId yazılmıyordu): en az iki seçim ve hepsi "like".
  const values = Object.values(choices);
  return values.length >= 2 && values.every((v) => v === "like");
}

/** Silinmesi gereken eski kayıtlar için zaman sınırları (ms). */
const ROOM_TTL_MS = 24 * 60 * 60 * 1000; // 24 saat
const QUEUE_TTL_MS = 10 * 60 * 1000; // 10 dakika (bağlantısı kopmuş "hayalet" kullanıcılar)

module.exports = { isMatch, ROOM_TTL_MS, QUEUE_TTL_MS };
