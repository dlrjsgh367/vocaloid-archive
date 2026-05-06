import api from './index.js';

export async function toggleLike(songId) {
  const res = await api.post(`/songs/${songId}/like`);
  return res.data.data; // { liked: boolean, likeCount: number }
}
