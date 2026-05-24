import { createRouter, createWebHistory } from 'vue-router';
import { useAuthStore } from '@/stores/auth';

const routes = [
  {
    path: '/',
    name: 'home',
    component: () => import('@/views/HomeView.vue'),
    meta: { title: '홈' },
  },
  {
    path: '/songs/new',
    name: 'song-create',
    component: () => import('@/views/SongCreateView.vue'),
    meta: { requiresAuth: true, title: '곡 등록' },
  },
  {
    path: '/songs/:id',
    name: 'song-detail',
    component: () => import('@/views/SongDetailView.vue'),
    meta: { title: '곡 상세' },
  },
  {
    path: '/search',
    name: 'search',
    component: () => import('@/views/SearchView.vue'),
    meta: { title: '검색' },
  },
  {
    path: '/playlists',
    name: 'playlists',
    component: () => import('@/views/PlaylistView.vue'),
    meta: { requiresAuth: true, title: '플레이리스트' },
  },
  {
    path: '/login',
    name: 'login',
    component: () => import('@/views/auth/LoginView.vue'),
    meta: { title: '로그인' },
  },
  {
    path: '/signup',
    name: 'signup',
    component: () => import('@/views/auth/SignUpView.vue'),
    meta: { title: '회원가입' },
  },
  {
    path: '/p/:code',
    name: 'public-playlist',
    component: () => import('@/views/PublicPlaylistView.vue'),
    meta: { title: '공유 플레이리스트' },
  },
  {
    path: '/:pathMatch(.*)*',
    name: 'not-found',
    component: () => import('@/views/NotFoundView.vue'),
    meta: { title: '페이지 없음' },
  },
];

const router = createRouter({
  history: createWebHistory(),
  routes,
});

// After login, LoginView should read route.query.return and redirect there.
router.beforeEach((to) => {
  if (to.meta.requiresAuth) {
    const auth = useAuthStore();
    if (!auth.isAuthenticated) {
      return { name: 'login', query: { return: to.fullPath } };
    }
  }
});

router.afterEach((to) => {
  document.title = to.meta?.title ? `${to.meta.title} · VocaloidArchive` : 'VocaloidArchive';
});

export default router;
