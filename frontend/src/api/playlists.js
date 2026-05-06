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
