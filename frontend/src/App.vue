<template>
  <div class="app">
    <header class="app-header">
      <RouterLink to="/" class="logo">
        <span class="logo-dot"></span>
        VocaloidArchive
        <span class="logo-jp">ボカロ図書館</span>
      </RouterLink>

      <nav>
        <RouterLink to="/" class="nav-link" exact-active-class="active">홈</RouterLink>
        <RouterLink to="/search" class="nav-link" active-class="active">탐색</RouterLink>
        <RouterLink to="/playlists" class="nav-link" active-class="active">플레이리스트</RouterLink>
        <RouterLink to="/songs/new" class="nav-link" active-class="active">+ 곡 등록</RouterLink>
      </nav>

      <RouterLink v-if="!authStore.isAuthenticated" to="/login" class="btn-login">
        ログイン ✦
      </RouterLink>
      <button v-else class="btn-login" @click="authStore.logout">
        {{ authStore.user?.username }} · 로그아웃
      </button>
    </header>

    <main>
      <RouterView />
    </main>
  </div>
</template>

<script setup>
import { RouterLink, RouterView } from 'vue-router';
import { useAuthStore } from './stores/auth.js';

const authStore = useAuthStore();
</script>

<style scoped>
.app-header {
  background: linear-gradient(90deg,
    rgba(255,255,255,0.92) 0%,
    rgba(253,232,243,0.85) 50%,
    rgba(255,255,255,0.92) 100%);
  backdrop-filter: blur(12px);
  -webkit-backdrop-filter: blur(12px);
  border-bottom: 1.5px solid var(--border);
  padding: 0 32px;
  height: 68px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  position: sticky;
  top: 0;
  z-index: 100;
  box-shadow: var(--shadow-sm);
}

.app-header::after {
  content: '';
  position: absolute;
  left: 0; right: 0; bottom: -4px;
  height: 4px;
  background: repeating-linear-gradient(90deg,
    var(--miku) 0 24px,
    var(--pink) 24px 48px,
    var(--lav) 48px 72px,
    var(--yellow) 72px 96px);
  opacity: 0.7;
}

.logo {
  font-family: var(--font-display);
  font-size: 24px;
  color: var(--miku-dk);
  display: flex;
  align-items: center;
  gap: 10px;
  text-decoration: none;
  text-shadow: 1px 1px 0 var(--miku-lt), 2px 2px 0 rgba(196,181,253,0.4);
  cursor: pointer;
  white-space: nowrap;
  flex-shrink: 0;
}

.logo-dot {
  width: 14px;
  height: 14px;
  background: var(--pink);
  border-radius: 50%;
  display: inline-block;
  box-shadow:
    0 0 0 2px var(--surface),
    0 0 0 4px var(--pink-lt),
    0 0 14px var(--pink);
  animation: pulse-glow 2.4s ease-in-out infinite;
  color: var(--pink);
  flex-shrink: 0;
}

.logo-jp {
  font-family: var(--font-jp);
  font-size: 11px;
  color: var(--lav-dk);
  font-weight: 700;
  letter-spacing: 0.08em;
  margin-left: -4px;
  margin-top: 2px;
  white-space: nowrap;
}

nav {
  display: flex;
  align-items: center;
  gap: 6px;
}

.nav-link {
  font-size: 14px;
  font-weight: 700;
  color: var(--text2);
  padding: 7px 16px;
  border-radius: var(--radius-sm);
  cursor: pointer;
  transition: all 0.18s;
  text-decoration: none;
  position: relative;
}

.nav-link:hover {
  background: var(--lav-lt);
  color: var(--lav-dk);
  transform: translateY(-1px);
}

.nav-link.active {
  background: var(--miku-lt);
  color: var(--miku-dk);
  box-shadow: inset 0 0 0 1.5px var(--miku);
}

.nav-link.active::after {
  content: '♪';
  position: absolute;
  top: -6px;
  right: -2px;
  font-size: 12px;
  color: var(--pink-dk);
  animation: float-y 1.6s ease-in-out infinite;
}

.btn-login {
  font-family: var(--font-body);
  font-size: 14px;
  font-weight: 800;
  color: white;
  background: linear-gradient(135deg, var(--miku-dk) 0%, var(--lav-dk) 100%);
  border: none;
  border-radius: var(--radius-sm);
  padding: 9px 22px;
  cursor: pointer;
  transition: all 0.18s;
  box-shadow: 0 4px 14px rgba(59,188,176,0.35), inset 0 1px 0 rgba(255,255,255,0.4);
  position: relative;
  overflow: hidden;
  text-decoration: none;
  display: inline-flex;
  align-items: center;
  white-space: nowrap;
}

.btn-login::before {
  content: '';
  position: absolute;
  inset: 0;
  background: linear-gradient(120deg, transparent 30%, rgba(255,255,255,0.5) 50%, transparent 70%);
  background-size: 200% 100%;
  animation: shimmer 3s linear infinite;
}

.btn-login:hover {
  transform: translateY(-2px) scale(1.02);
}
</style>
