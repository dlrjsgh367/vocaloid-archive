import { createRouter, createWebHistory } from 'vue-router';
import { useAuthStore } from '@/stores/auth';

const routes = [
  { path: '/', name: 'home', component: () => import('@/views/HomeView.vue') },
  { path: '/songs/new', name: 'song-create', component: () => import('@/views/SongCreateView.vue'), meta: { requiresAuth: true } },
  { path: '/songs/:id', name: 'song-detail', component: () => import('@/views/SongDetailView.vue') },
  { path: '/search', name: 'search', component: () => import('@/views/SearchView.vue') },
  { path: '/playlists', name: 'playlists', component: () => import('@/views/PlaylistView.vue'), meta: { requiresAuth: true } },
  { path: '/login', name: 'login', component: () => import('@/views/auth/LoginView.vue') },
  { path: '/signup', name: 'signup', component: () => import('@/views/auth/SignUpView.vue') },
];

const router = createRouter({
  history: createWebHistory(),
  routes,
});

router.beforeEach((to) => {
  if (to.meta.requiresAuth) {
    const auth = useAuthStore();
    if (!auth.isAuthenticated) {
      return { name: 'login', query: { return: to.fullPath } };
    }
  }
});

export default router;
