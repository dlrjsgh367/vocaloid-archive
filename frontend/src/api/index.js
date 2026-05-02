import axios from 'axios';

const api = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || '/api',
  timeout: 10000,
});

// Phase 2: attach Authorization header from auth store
api.interceptors.request.use((config) => config);

// Phase 2: handle 401 with refresh-token rotation
api.interceptors.response.use(
  (response) => response,
  (error) => Promise.reject(error)
);

export default api;
