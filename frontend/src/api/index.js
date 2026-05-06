import axios from 'axios';
import router from '@/router/index.js';
import { useAuthStore } from '@/stores/auth.js';

const api = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || '/api',
  timeout: 10000,
});

// 요청 인터셉터 — accessToken 주입
api.interceptors.request.use((config) => {
  const auth = useAuthStore();
  if (auth.accessToken) {
    config.headers.Authorization = `Bearer ${auth.accessToken}`;
  }
  return config;
});

// 응답 인터셉터 — 401 시 refresh 후 재시도 (1회)
let isRefreshing = false;
let waitQueue = [];

function flushQueue(error, token = null) {
  waitQueue.forEach(({ resolve, reject }) =>
    error ? reject(error) : resolve(token)
  );
  waitQueue = [];
}

api.interceptors.response.use(
  (res) => res,
  async (err) => {
    const original = err.config;

    if (err.response?.status !== 401 || original._retry) {
      return Promise.reject(err);
    }

    if (isRefreshing) {
      return new Promise((resolve, reject) => {
        waitQueue.push({ resolve, reject });
      }).then((token) => {
        original.headers.Authorization = `Bearer ${token}`;
        return api(original);
      });
    }

    original._retry = true;
    isRefreshing = true;

    const auth = useAuthStore();
    try {
      await auth.refresh();
      flushQueue(null, auth.accessToken);
      original.headers.Authorization = `Bearer ${auth.accessToken}`;
      return api(original);
    } catch (refreshErr) {
      flushQueue(refreshErr);
      auth.clear();
      router.push({ name: 'login' });
      return Promise.reject(refreshErr);
    } finally {
      isRefreshing = false;
    }
  }
);

export default api;
