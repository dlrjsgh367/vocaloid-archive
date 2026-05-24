import api from './index.js';

export async function fetchMyPlaylists() {
  const res = await api.get('/playlists');
  return res.data.data;
}

export async function fetchPlaylist(id) {
  const res = await api.get(`/playlists/${id}`);
  return res.data.data;
}

export async function createPlaylist(body) {
  const res = await api.post('/playlists', body);
  return res.data.data;
}

export async function deletePlaylist(id) {
  await api.delete(`/playlists/${id}`);
}

export async function addSongToPlaylist(playlistId, songId) {
  const res = await api.post(`/playlists/${playlistId}/songs`, { songId });
  return res.data.data;
}

export async function removeSongFromPlaylist(playlistId, songId) {
  await api.delete(`/playlists/${playlistId}/songs/${songId}`);
}

// 공유 코드 발급(없으면 생성). 소유자만 가능. 비공개면 400 PLAYLIST_NOT_PUBLIC.
export async function ensureShare(id) {
  const res = await api.post(`/playlists/${id}/share`);
  return res.data.data; // { shareCode, shareUrl }
}

// 공유 코드로 공개 플레이리스트 조회 (인증 불필요).
export async function getPublicByCode(code) {
  const res = await api.get(`/share/playlists/${code}`);
  return res.data.data;
}

// 공유 카드 이미지(PNG) URL. 동일 출처 루트 상대 경로.
export function cardPngUrl(code) {
  return `/api/share/playlists/${code}/card.png`;
}
