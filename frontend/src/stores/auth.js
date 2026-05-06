import { defineStore } from 'pinia';
import { loginApi, logoutApi, refreshApi } from '@/api/auth.js';

export const useAuthStore = defineStore('auth', {
  state: () => ({
    user: null,
    accessToken: null,
    refreshToken: localStorage.getItem('refreshToken') || null,
  }),

  getters: {
    isAuthenticated: (state) => !!state.accessToken,
    // JWT subject contains userId (Long). Returns null if no token or parse fails.
    currentUserId: (state) => {
      if (!state.accessToken) return null;
      try {
        const payload = state.accessToken.split('.')[1];
        const json = atob(payload.replace(/-/g, '+').replace(/_/g, '/'));
        const claims = JSON.parse(json);
        const id = Number(claims.sub);
        return Number.isFinite(id) ? id : null;
      } catch {
        return null;
      }
    },
  },

  actions: {
    async login(email, password) {
      const data = await loginApi({ email, password });
      this._setTokens(data.accessToken, data.refreshToken);
    },

    async logout() {
      if (this.refreshToken) {
        try {
          await logoutApi({ refreshToken: this.refreshToken });
        } catch {
          // 서버 에러가 나도 로컬은 클리어
        }
      }
      this.clear();
    },

    async refresh() {
      const data = await refreshApi({ refreshToken: this.refreshToken });
      this._setTokens(data.accessToken, data.refreshToken);
    },

    clear() {
      this.user = null;
      this.accessToken = null;
      this.refreshToken = null;
      localStorage.removeItem('refreshToken');
    },

    _setTokens(accessToken, refreshToken) {
      this.accessToken = accessToken;
      this.refreshToken = refreshToken;
      localStorage.setItem('refreshToken', refreshToken);
    },
  },
});
