import api from './index.js';

/**
 * @param {Object} params - keyword, mood, characterId, tagId, sort, page, size
 */
export async function fetchSongs(params = {}) {
  const res = await api.get('/songs', { params });
  return res.data.data; // PageResponse<SongResponse>
}

export async function fetchSong(id) {
  const res = await api.get(`/songs/${id}`);
  return res.data.data; // SongDetailResponse
}

export async function createSong(body) {
  const res = await api.post('/songs', body);
  return res.data.data;
}

export async function deleteSong(id) {
  await api.delete(`/songs/${id}`);
}
