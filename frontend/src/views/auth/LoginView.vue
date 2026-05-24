<template>
  <div class="auth-page">
    <div class="auth-card">
      <!-- 로고 -->
      <RouterLink to="/" class="logo">
        <span class="logo-dot"></span>
        VocaloidArchive
        <span class="logo-jp">ボカロ図書館</span>
      </RouterLink>

      <div class="card-header">
        <div class="eyebrow">♪ 환영합니다</div>
        <h1>다시 돌아오신<br /><span class="gradient-word">덕후님</span></h1>
        <p v-if="justRegistered" class="success-msg">✦ 가입 완료! 로그인해주세요.</p>
        <p v-else>로그인하고 보카로 아카이브를 즐겨보세요.</p>
      </div>

      <!-- 에러 배너 -->
      <div v-if="serverError" class="error-banner">{{ serverError }}</div>

      <form @submit.prevent="handleSubmit" novalidate>
        <!-- 이메일 -->
        <div class="field" :class="{ error: errors.email }">
          <input
            id="email"
            v-model="form.email"
            type="email"
            placeholder=" "
            autocomplete="email"
            @input="errors.email = ''"
          />
          <label for="email">이메일</label>
          <div class="field-hint">{{ errors.email }}</div>
        </div>

        <!-- 비밀번호 -->
        <div class="field" :class="{ error: errors.password }">
          <input
            id="password"
            v-model="form.password"
            :type="showPw ? 'text' : 'password'"
            placeholder=" "
            autocomplete="current-password"
            @input="errors.password = ''"
          />
          <label for="password">비밀번호</label>
          <button
            type="button"
            class="pw-toggle"
            @click="showPw = !showPw"
            aria-label="비밀번호 보기"
          >
            <svg v-if="!showPw" width="16" height="16" viewBox="0 0 24 24" fill="none">
              <path
                d="M2 12s3.5-7 10-7 10 7 10 7-3.5 7-10 7S2 12 2 12Z"
                stroke="currentColor"
                stroke-width="1.5"
              />
              <circle cx="12" cy="12" r="3" stroke="currentColor" stroke-width="1.5" />
            </svg>
            <svg v-else width="16" height="16" viewBox="0 0 24 24" fill="none">
              <path
                d="M3 3l18 18M10.5 10.5A3 3 0 0013.5 13.5M6.4 6.4C4 8 2 12 2 12s3.5 7 10 7a9.7 9.7 0 005.6-1.8M9.9 5.1A9.7 9.7 0 0112 5c6.5 0 10 7 10 7s-.8 1.6-2.2 3.2"
                stroke="currentColor"
                stroke-width="1.5"
                stroke-linecap="round"
              />
            </svg>
          </button>
          <div class="field-hint">{{ errors.password }}</div>
        </div>

        <button
          type="submit"
          class="btn-submit"
          :disabled="!form.email || !form.password || loading"
        >
          <span>{{ loading ? '로그인 중...' : '로그인' }}</span>
          <svg v-if="!loading" class="arrow" width="16" height="16" viewBox="0 0 24 24" fill="none">
            <path
              d="M5 12h14M13 6l6 6-6 6"
              stroke="currentColor"
              stroke-width="1.7"
              stroke-linecap="round"
              stroke-linejoin="round"
            />
          </svg>
        </button>
      </form>

      <div class="card-footer">
        아직 계정이 없으신가요?
        <RouterLink to="/signup">가입하기 ✦</RouterLink>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, computed } from 'vue';
import { useRouter, useRoute, RouterLink } from 'vue-router';
import { useAuthStore } from '../../stores/auth.js';

const router = useRouter();
const route = useRoute();
const auth = useAuthStore();

const form = reactive({ email: '', password: '' });
const errors = reactive({ email: '', password: '' });
const showPw = ref(false);
const loading = ref(false);
const serverError = ref('');

const justRegistered = computed(() => route.query.registered === '1');

async function handleSubmit() {
  if (!form.email || !form.password) return;
  loading.value = true;
  serverError.value = '';
  try {
    await auth.login(form.email, form.password);
    const returnTo = route.query.return;
    router.push(returnTo ? String(returnTo) : { name: 'home' });
  } catch (err) {
    serverError.value =
      err.response?.data?.error?.message ?? '이메일 또는 비밀번호가 올바르지 않습니다.';
  } finally {
    loading.value = false;
  }
}
</script>

<style scoped>
.auth-page {
  min-height: calc(100vh - 68px);
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 40px 16px;
}

.auth-card {
  width: 100%;
  max-width: 460px;
  background: var(--surface);
  border-radius: var(--radius-xl);
  border: 1.5px solid var(--border);
  box-shadow: var(--shadow-lg);
  padding: 40px 40px 32px;
  position: relative;
  overflow: hidden;
}

.auth-card::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  height: 4px;
  background: repeating-linear-gradient(
    90deg,
    var(--miku) 0 24px,
    var(--pink) 24px 48px,
    var(--lav) 48px 72px,
    var(--yellow) 72px 96px
  );
}

.logo {
  font-family: var(--font-display);
  font-size: 20px;
  color: var(--miku-dk);
  display: flex;
  align-items: center;
  gap: 8px;
  text-decoration: none;
  margin-bottom: 32px;
}

.logo-dot {
  width: 12px;
  height: 12px;
  background: var(--pink);
  border-radius: 50%;
  box-shadow:
    0 0 0 3px var(--pink-lt),
    0 0 10px var(--pink);
  animation: pulse-glow 2.4s ease-in-out infinite;
  color: var(--pink);
  flex-shrink: 0;
}

.logo-jp {
  font-family: var(--font-jp);
  font-size: 10px;
  color: var(--lav-dk);
  font-weight: 700;
  letter-spacing: 0.08em;
}

.eyebrow {
  font-family: var(--font-jp);
  font-size: 11px;
  font-weight: 700;
  color: var(--lav-dk);
  letter-spacing: 0.12em;
  text-transform: uppercase;
  margin-bottom: 10px;
}

h1 {
  font-family: var(--font-display);
  font-size: 36px;
  line-height: 1.2;
  color: var(--text);
  margin: 0 0 10px;
  text-shadow:
    2px 2px 0 var(--surface),
    3px 3px 0 var(--pink-lt);
}

.gradient-word {
  background: linear-gradient(90deg, var(--miku-dk), var(--lav-dk), var(--pink-dk), var(--miku-dk));
  background-size: 300% 100%;
  -webkit-background-clip: text;
  background-clip: text;
  -webkit-text-fill-color: transparent;
  animation: shimmer 4s linear infinite;
  /* h1's offset text-shadow ghosts behind transparent-fill gradient text — drop it here. */
  text-shadow: none;
}

.card-header p {
  font-size: 14px;
  color: var(--text2);
  margin: 0 0 28px;
  line-height: 1.6;
}

.success-msg {
  color: var(--miku-dk) !important;
  font-weight: 700 !important;
  background: var(--miku-lt);
  border: 1.5px solid var(--miku);
  border-radius: var(--radius-sm);
  padding: 8px 12px;
  font-size: 13px !important;
}

.error-banner {
  background: var(--coral-lt);
  color: #b91c1c;
  border: 1.5px solid var(--coral);
  border-radius: var(--radius-sm);
  padding: 10px 14px;
  font-size: 13px;
  font-weight: 700;
  margin-bottom: 16px;
}

form {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.field {
  position: relative;
}

.field input {
  width: 100%;
  height: 52px;
  padding: 22px 14px 6px;
  border: 1.5px solid var(--border);
  border-radius: var(--radius-sm);
  background: var(--bg2);
  font-family: var(--font-body);
  font-size: 15px;
  font-weight: 600;
  color: var(--text);
  outline: none;
  transition:
    border-color 0.15s,
    box-shadow 0.15s,
    background 0.15s;
}

.field input:focus {
  border-color: var(--miku);
  background: var(--surface);
  box-shadow: 0 0 0 4px var(--miku-lt);
}

.field label {
  position: absolute;
  top: 15px;
  left: 14px;
  font-size: 14px;
  font-weight: 600;
  color: var(--text3);
  pointer-events: none;
  transition: all 0.18s cubic-bezier(0.2, 0.8, 0.2, 1);
}

.field input:focus + label,
.field input:not(:placeholder-shown) + label {
  top: 6px;
  font-size: 10px;
  font-weight: 800;
  color: var(--miku-dk);
  letter-spacing: 0.06em;
  text-transform: uppercase;
}

.field.error input {
  border-color: var(--coral);
  box-shadow: 0 0 0 4px var(--coral-lt);
}
.field.error label {
  color: #b91c1c;
}

.field-hint {
  font-size: 11px;
  font-weight: 700;
  color: #b91c1c;
  margin-top: 5px;
  padding-left: 2px;
  min-height: 15px;
}

.pw-toggle {
  position: absolute;
  top: 14px;
  right: 12px;
  background: none;
  border: none;
  color: var(--text3);
  cursor: pointer;
  padding: 4px;
  border-radius: var(--radius-sm);
  display: grid;
  place-items: center;
}
.pw-toggle:hover {
  color: var(--text);
  background: var(--bg2);
}

.btn-submit {
  height: 52px;
  width: 100%;
  border: none;
  background: linear-gradient(135deg, var(--miku-dk) 0%, var(--lav-dk) 100%);
  color: white;
  border-radius: var(--radius-md);
  font-family: var(--font-body);
  font-size: 15px;
  font-weight: 800;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  transition: all 0.2s;
  box-shadow:
    0 6px 18px rgba(59, 188, 176, 0.35),
    inset 0 1px 0 rgba(255, 255, 255, 0.4);
  position: relative;
  overflow: hidden;
  margin-top: 4px;
}
.btn-submit::before {
  content: '';
  position: absolute;
  inset: 0;
  background: linear-gradient(
    120deg,
    transparent 30%,
    rgba(255, 255, 255, 0.4) 50%,
    transparent 70%
  );
  background-size: 200% 100%;
  animation: shimmer 3s linear infinite;
}
.btn-submit:hover:not(:disabled) {
  transform: translateY(-2px);
}
.btn-submit .arrow {
  transition: transform 0.2s;
}
.btn-submit:hover:not(:disabled) .arrow {
  transform: translateX(3px);
}
.btn-submit:disabled {
  background: var(--border);
  color: var(--text3);
  cursor: not-allowed;
  box-shadow: none;
}
.btn-submit:disabled::before {
  display: none;
}

.card-footer {
  margin-top: 24px;
  text-align: center;
  font-size: 13px;
  font-weight: 600;
  color: var(--text2);
}
.card-footer a {
  color: var(--miku-dk);
  font-weight: 800;
  text-decoration: none;
  border-bottom: 1.5px solid var(--miku);
  padding-bottom: 1px;
}
.card-footer a:hover {
  color: var(--lav-dk);
  border-color: var(--lav-dk);
}

@media (max-width: 480px) {
  .auth-page {
    padding: 24px 12px;
    align-items: flex-start;
  }

  .auth-card {
    padding: 28px 20px 24px;
    border-radius: var(--radius-lg);
  }

  h1 {
    font-size: 28px;
  }
}
</style>
