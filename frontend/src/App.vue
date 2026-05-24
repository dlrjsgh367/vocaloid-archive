<template>
  <div class="app">
    <header class="app-header">
      <RouterLink to="/" class="logo">
        <span class="logo-dot"></span>
        VocaloidArchive
        <span class="logo-jp">ボカロ図書館</span>
      </RouterLink>

      <!-- 데스크탑 nav -->
      <nav class="desktop-nav">
        <RouterLink to="/" class="nav-link" exact-active-class="active">홈</RouterLink>
        <RouterLink to="/search" class="nav-link" active-class="active">탐색</RouterLink>
        <RouterLink to="/playlists" class="nav-link" active-class="active">플레이리스트</RouterLink>
        <RouterLink to="/songs/new" class="nav-link" active-class="active">+ 곡 등록</RouterLink>
      </nav>

      <div class="header-actions desktop-actions">
        <button
          class="theme-toggle"
          @click="toggleTheme"
          :title="theme === 'light' ? '다크 모드로 전환' : '라이트 모드로 전환'"
        >
          <span class="theme-icon">{{ theme === 'light' ? '♪' : '⚡' }}</span>
          <span class="theme-text">{{ theme === 'light' ? 'LIGHT' : 'DARK' }}</span>
        </button>

        <RouterLink v-if="!authStore.isAuthenticated" to="/login" class="btn-login">
          로그인 ✦
        </RouterLink>
        <button v-else class="btn-login" @click="authStore.logout">
          {{ authStore.user?.username }} · 로그아웃
        </button>
      </div>

      <!-- 모바일 우측: 테마 + 햄버거 -->
      <div class="mobile-header-right">
        <button
          class="theme-toggle"
          @click="toggleTheme"
          :title="theme === 'light' ? '다크 모드로 전환' : '라이트 모드로 전환'"
        >
          <span class="theme-icon">{{ theme === 'light' ? '♪' : '⚡' }}</span>
        </button>
        <button class="hamburger" @click="menuOpen = !menuOpen" :class="{ open: menuOpen }" aria-label="메뉴">
          <span></span>
          <span></span>
          <span></span>
        </button>
      </div>
    </header>

    <!-- 모바일 드로어 -->
    <div class="mobile-drawer" :class="{ open: menuOpen }">
      <div class="drawer-backdrop" @click="menuOpen = false"></div>
      <nav class="drawer-nav" @click="menuOpen = false">
        <RouterLink to="/" class="drawer-link" exact-active-class="active">♪ 홈</RouterLink>
        <RouterLink to="/search" class="drawer-link" active-class="active">🔍 탐색</RouterLink>
        <RouterLink to="/playlists" class="drawer-link" active-class="active">✦ 플레이리스트</RouterLink>
        <RouterLink to="/songs/new" class="drawer-link" active-class="active">＋ 곡 등록</RouterLink>
        <div class="drawer-divider"></div>
        <RouterLink v-if="!authStore.isAuthenticated" to="/login" class="drawer-link login">
          로그인 ✦
        </RouterLink>
        <button v-else class="drawer-link login" @click="authStore.logout">
          {{ authStore.user?.username }} · 로그아웃
        </button>
      </nav>
    </div>

    <main>
      <RouterView />
    </main>

    <AppDialog />
  </div>
</template>

<script setup>
import { ref, onMounted, watch } from 'vue';
import { RouterLink, RouterView, useRoute } from 'vue-router';
import { useAuthStore } from './stores/auth.js';
import AppDialog from './components/AppDialog.vue';

const authStore = useAuthStore();
const route = useRoute();

const theme = ref(localStorage.getItem('theme') || 'light');
const menuOpen = ref(false);

function toggleTheme() {
  theme.value = theme.value === 'light' ? 'dark' : 'light';
  localStorage.setItem('theme', theme.value);
  document.documentElement.setAttribute('data-theme', theme.value);
}

// 라우트 변경 시 드로어 닫기
watch(() => route.path, () => {
  menuOpen.value = false;
});

onMounted(() => {
  document.documentElement.setAttribute('data-theme', theme.value);
});
</script>

<style scoped>
.app-header {
  background: linear-gradient(
    90deg,
    color-mix(in srgb, var(--surface) 92%, transparent) 0%,
    color-mix(in srgb, var(--surface2) 85%, transparent) 50%,
    color-mix(in srgb, var(--surface) 92%, transparent) 100%
  );
  backdrop-filter: blur(20px);
  -webkit-backdrop-filter: blur(20px);
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
  transition: all 0.3s ease;
}

.app-header::after {
  content: '';
  position: absolute;
  left: 0;
  right: 0;
  bottom: -3px;
  height: 3px;
  background: linear-gradient(
    90deg,
    var(--miku),
    var(--pink),
    var(--lav),
    var(--yellow),
    var(--miku)
  );
  background-size: 300% 100%;
  animation: shimmer 4s linear infinite;
  opacity: 0.8;
}

.logo {
  font-family: var(--font-display);
  font-size: 24px;
  color: var(--miku-dk);
  display: flex;
  align-items: center;
  gap: 10px;
  text-decoration: none;
  text-shadow:
    1px 1px 0 var(--miku-lt),
    2px 2px 0 rgba(196, 181, 253, 0.4);
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

/* ── 데스크탑 nav ── */
.desktop-nav {
  display: flex;
  align-items: center;
  gap: 6px;
}

.nav-link {
  font-size: 14px;
  font-weight: 700;
  color: var(--text2);
  padding: 8px 18px;
  border-radius: var(--radius-sm);
  cursor: pointer;
  transition: all 0.25s cubic-bezier(0.4, 0, 0.2, 1);
  text-decoration: none;
  position: relative;
  overflow: hidden;
}

.nav-link:hover {
  background: var(--lav-lt);
  color: var(--lav-dk);
  transform: translateY(-1px);
}

.nav-link.active {
  background: var(--miku-lt);
  color: var(--miku-dk);
}

.nav-link::after {
  content: '';
  position: absolute;
  bottom: 4px;
  left: 50%;
  width: 0;
  height: 3px;
  background: var(--miku-dk);
  border-radius: 99px;
  transform: translateX(-50%);
  transition: width 0.25s ease;
}

.nav-link.active::after {
  width: 16px;
}

.nav-link.active::before {
  content: '♪';
  position: absolute;
  top: -2px;
  right: 2px;
  font-size: 10px;
  color: var(--pink-dk);
  animation: float-y 1.6s ease-in-out infinite;
}

.header-actions {
  display: flex;
  align-items: center;
  gap: 12px;
}

.theme-toggle {
  background: var(--surface2);
  border: 1.5px solid var(--border);
  color: var(--text2);
  font-family: var(--font-body);
  font-size: 12px;
  font-weight: 800;
  padding: 8px 14px;
  border-radius: var(--radius-sm);
  cursor: pointer;
  display: inline-flex;
  align-items: center;
  gap: 6px;
  transition: all 0.25s ease;
  box-shadow: var(--shadow-sm);
  outline: none;
}

.theme-toggle:hover {
  background: var(--lav-lt);
  border-color: var(--lav);
  color: var(--lav-dk);
  transform: translateY(-1px);
  box-shadow: 0 4px 12px var(--miku-lt);
}

.theme-toggle:active {
  transform: translateY(0);
}

.theme-icon {
  font-size: 13px;
  color: var(--pink-dk);
  display: inline-block;
  animation: float-y 2.0s ease-in-out infinite;
}

.theme-text {
  /* 모바일에서는 아이콘만 보임 */
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
  box-shadow:
    0 4px 14px rgba(59, 188, 176, 0.35),
    inset 0 1px 0 rgba(255, 255, 255, 0.4);
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
  background: linear-gradient(
    120deg,
    transparent 30%,
    rgba(255, 255, 255, 0.5) 50%,
    transparent 70%
  );
  background-size: 200% 100%;
  animation: shimmer 3s linear infinite;
}

.btn-login:hover {
  transform: translateY(-2px) scale(1.02);
}

/* ── 햄버거 버튼 ── */
.hamburger {
  display: none;
  flex-direction: column;
  justify-content: center;
  align-items: center;
  gap: 5px;
  width: 40px;
  height: 40px;
  background: var(--surface2);
  border: 1.5px solid var(--border);
  border-radius: var(--radius-sm);
  cursor: pointer;
  padding: 8px;
  transition: all 0.2s;
  outline: none;
}

.hamburger span {
  display: block;
  width: 18px;
  height: 2px;
  background: var(--text2);
  border-radius: 2px;
  transition: all 0.3s ease;
  transform-origin: center;
}

.hamburger.open span:nth-child(1) {
  transform: translateY(7px) rotate(45deg);
}
.hamburger.open span:nth-child(2) {
  opacity: 0;
  transform: scaleX(0);
}
.hamburger.open span:nth-child(3) {
  transform: translateY(-7px) rotate(-45deg);
}

/* 모바일 우측 액션 그룹 */
.mobile-header-right {
  display: none;
  align-items: center;
  gap: 8px;
}

/* ── 모바일 드로어 ── */
.mobile-drawer {
  display: none;
  position: fixed;
  inset: 0;
  z-index: 200;
  pointer-events: none;
}

.mobile-drawer.open {
  pointer-events: auto;
}

.drawer-backdrop {
  position: absolute;
  inset: 0;
  background: rgba(45, 27, 78, 0);
  transition: background 0.3s ease;
}

.mobile-drawer.open .drawer-backdrop {
  background: rgba(45, 27, 78, 0.45);
  backdrop-filter: blur(4px);
}

.drawer-nav {
  position: absolute;
  top: 0;
  right: -280px;
  width: 260px;
  height: 100%;
  background: var(--surface);
  border-left: 1.5px solid var(--border);
  box-shadow: var(--shadow-lg);
  display: flex;
  flex-direction: column;
  padding: 80px 16px 32px;
  gap: 4px;
  transition: right 0.3s cubic-bezier(0.25, 0.8, 0.25, 1);
  overflow-y: auto;
}

.mobile-drawer.open .drawer-nav {
  right: 0;
}

.drawer-link {
  font-family: var(--font-body);
  font-size: 16px;
  font-weight: 800;
  color: var(--text2);
  padding: 14px 18px;
  border-radius: var(--radius-md);
  text-decoration: none;
  display: flex;
  align-items: center;
  gap: 10px;
  transition: all 0.2s;
  border: none;
  background: none;
  cursor: pointer;
  text-align: left;
  width: 100%;
}

.drawer-link:hover,
.drawer-link.active {
  background: var(--miku-lt);
  color: var(--miku-dk);
}

.drawer-link.login {
  background: linear-gradient(135deg, var(--miku-dk) 0%, var(--lav-dk) 100%);
  color: white;
  justify-content: center;
  border-radius: var(--radius-md);
  margin-top: 8px;
}

.drawer-divider {
  height: 1.5px;
  background: var(--border);
  margin: 8px 0;
  border-radius: 2px;
}

/* ── 반응형 ── */
@media (max-width: 768px) {
  .app-header {
    padding: 0 16px;
  }

  .logo-jp {
    display: none;
  }

  .logo {
    font-size: 18px;
    gap: 8px;
  }

  .desktop-nav,
  .desktop-actions {
    display: none;
  }

  .mobile-header-right {
    display: flex;
  }

  .hamburger {
    display: flex;
  }

  .mobile-drawer {
    display: block;
  }

  /* 모바일에서 theme-toggle은 아이콘만 */
  .theme-text {
    display: none;
  }

  .theme-toggle {
    padding: 8px 10px;
  }
}
</style>
