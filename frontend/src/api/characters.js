import api from './index.js';

export async function fetchCharacters() {
  const res = await api.get('/characters');
  return res.data.data; // CharacterResponse[]
}
