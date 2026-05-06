import { createApp } from 'vue';
import { createPinia } from 'pinia';
import App from './App.vue';
import router from './router';
import { useAuthStore } from './stores/auth';
import './assets/styles/tokens.css';
import './assets/styles/reset.css';

const app = createApp(App);
const pinia = createPinia();
app.use(pinia);
app.use(router);

// 콜드 스타트 시 refreshToken만 있고 accessToken은 없는 상태(localStorage 복원)에서
// requiresAuth 라우트가 즉시 /login으로 튕기지 않도록 mount 전에 access token 발급.
const auth = useAuthStore(pinia);
auth.bindCrossTabSync();
if (auth.refreshToken) {
  auth
    .refresh()
    .catch(() => auth.clear())
    .finally(() => app.mount('#app'));
} else {
  app.mount('#app');
}
