"use strict";

const crypto = require("crypto");

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

/** Bir kuyruk kaydındaki desteyi doğrular. Geçersizse null döner. */
function normalizeDeck(entry) {
  const deck = entry && entry.deck;
  if (!deck || typeof deck !== "object") return null;
  const raw = Array.isArray(deck.movieIds) ? deck.movieIds : Object.values(deck.movieIds || {});
  const movieIds = raw.filter((id) => Number.isInteger(id) && id > 0).slice(0, MAX_DECK_SIZE);
  if (movieIds.length === 0) return null;
  return {
    genreName: typeof deck.genreName === "string" ? deck.genreName.slice(0, 40) : "",
    genreId: Number.isInteger(deck.genreId) ? deck.genreId : 0,
    movieIds,
  };
}

/**
 * Kuyruğa yeni giren `me` için en uzun süredir bekleyen uygun rakibi seçer.
 *
 * - `excluded`: me ile herhangi bir yönde engelleme ilişkisi olan kullanıcılar.
 * - Odayı kurmak için en az bir tarafın geçerli bir destesi olmalı (eski sürümler deste göndermez).
 * - Destesi olan ve daha uzun bekleyen taraf host olur.
 *
 * @param {Object<string,{ts:number,deck?:Object}>|null} queue
 * @param {string} me
 * @param {Set<string>} excluded
 * @returns {{hostId:string, guestId:string, deck:Object}|null}
 */
function pickPair(queue, me, excluded = new Set()) {
  if (!queue || typeof queue !== "object" || !queue[me]) return null;
  const myDeck = normalizeDeck(queue[me]);
  const tsOf = (uid) => (typeof queue[uid].ts === "number" ? queue[uid].ts : Number.MAX_SAFE_INTEGER);

  const candidates = Object.keys(queue)
    .filter((uid) => uid !== me && queue[uid] && !excluded.has(uid))
    .filter((uid) => myDeck || normalizeDeck(queue[uid]))
    .sort((a, b) => tsOf(a) - tsOf(b) || a.localeCompare(b));

  const opponent = candidates[0];
  if (!opponent) return null;

  const opponentDeck = normalizeDeck(queue[opponent]);
  // Rakip daha uzun bekliyor; destesi varsa o host olur.
  if (opponentDeck) return { hostId: opponent, guestId: me, deck: opponentDeck };
  return { hostId: me, guestId: opponent, deck: myDeck };
}

/** Uygulamadaki RoomCodes ile aynı alfabe (0/O, 1/I yok). */
const ROOM_CODE_ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
function generateRoomCode() {
  let code = "";
  for (let i = 0; i < 6; i++) code += ROOM_CODE_ALPHABET[crypto.randomInt(ROOM_CODE_ALPHABET.length)];
  return code;
}

const MAX_DECK_SIZE = 40;

/** Silinmesi gereken eski kayıtlar için zaman sınırları (ms). */
const ROOM_TTL_MS = 24 * 60 * 60 * 1000; // 24 saat
const QUEUE_TTL_MS = 10 * 60 * 1000; // 10 dakika (bağlantısı kopmuş "hayalet" kullanıcılar)

module.exports = {
  isMatch,
  normalizeDeck,
  pickPair,
  generateRoomCode,
  ROOM_CODE_ALPHABET,
  MAX_DECK_SIZE,
  ROOM_TTL_MS,
  QUEUE_TTL_MS,
};
