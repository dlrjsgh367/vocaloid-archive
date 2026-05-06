import api from './index.js';

export async function signupApi(body) {
  const res = await api.post('/auth/signup', body);
  return res.data.data;
}

export async function loginApi(body) {
  const res = await api.post('/auth/login', body);
  return res.data.data;
}

export async function refreshApi(body) {
  const res = await api.post('/auth/refresh', body);
  return res.data.data;
}

export async function logoutApi(body) {
  await api.post('/auth/logout', body);
}
