import api from './index.js';

export async function fetchStats() {
  const res = await api.get('/stats');
  return res.data.data; // { songCount, userCount, tagCount }
}
