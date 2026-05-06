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
        <div class="eyebrow">✦ 새 계정</div>
        <h1>덕후의 공간에<br><span class="gradient-word">가입하기</span></h1>
        <p>좋아하는 보카로 곡을 등록하고 공유해보세요.</p>
      </div>

      <!-- 에러 배너 -->
      <div v-if="serverError" class="error-banner">{{ serverError }}</div>

      <form @submit.prevent="handleSubmit" novalidate>
        <!-- 닉네임 -->
        <div class="field" :class="{ error: errors.username, ok: touched.username && !errors.username }">
          <input
            id="username"
            v-model="form.username"
            type="text"
            placeholder=" "
            autocomplete="username"
            @blur="touched.username = true"
            @input="validate"
          />
          <label for="username">닉네임</label>
          <div class="field-hint">{{ errors.username || (touched.username && !errors.username ? '✓ 좋아요' : '2–20자') }}</div>
        </div>

        <!-- 이메일 -->
        <div class="field" :class="{ error: errors.email, ok: touched.email && !errors.email }">
          <input
            id="email"
            v-model="form.email"
            type="email"
            placeholder=" "
            autocomplete="email"
            @blur="touched.email = true"
            @input="validate"
          />
          <label for="email">이메일</label>
          <div class="field-hint">{{ errors.email || '' }}</div>
        </div>

        <!-- 비밀번호 -->
        <div class="field" :class="{ error: errors.password }">
          <input
            id="password"
            v-model="form.password"
            :type="showPw ? 'text' : 'password'"
            placeholder=" "
            autocomplete="new-password"
            @input="validate"
          />
          <label for="password">비밀번호</label>
          <button type="button" class="pw-toggle" @click="showPw = !showPw" aria-label="비밀번호 보기">
            <svg v-if="!showPw" width="16" height="16" viewBox="0 0 24 24" fill="none"><path d="M2 12s3.5-7 10-7 10 7 10 7-3.5 7-10 7S2 12 2 12Z" stroke="currentColor" stroke-width="1.5"/><circle cx="12" cy="12" r="3" stroke="currentColor" stroke-width="1.5"/></svg>
            <svg v-else width="16" height="16" viewBox="0 0 24 24" fill="none"><path d="M3 3l18 18M10.5 10.5A3 3 0 0013.5 13.5M6.4 6.4C4 8 2 12 2 12s3.5 7 10 7a9.7 9.7 0 005.6-1.8M9.9 5.1A9.7 9.7 0 0112 5c6.5 0 10 7 10 7s-.8 1.6-2.2 3.2" stroke="currentColor" stroke-width="1.5" stroke-linecap="round"/></svg>
          </button>
          <!-- 강도 미터 -->
          <div class="pw-strength">
            <span v-for="i in 4" :key="i" :class="{ active: pwStrength >= i, [`level-${pwStrength}`]: pwStrength >= i }"></span>
          </div>
          <div class="field-hint">{{ pwStrengthLabel }}</div>
        </div>

        <!-- 비밀번호 확인 -->
        <div class="field" :class="{ error: errors.confirm, ok: form.confirm && !errors.confirm }">
          <input
            id="confirm"
            v-model="form.confirm"
            :type="showPw ? 'text' : 'password'"
            placeholder=" "
            autocomplete="new-password"
            @input="validate"
          />
          <label for="confirm">비밀번호 확인</label>
          <div class="field-hint">{{ errors.confirm || (form.confirm && !errors.confirm ? '✓ 일치합니다' : '') }}</div>
        </div>

        <!-- 약관 동의 -->
        <label class="terms">
          <input type="checkbox" v-model="form.agree" @change="validate" />
          <span class="box">
            <svg viewBox="0 0 24 24" fill="none"><path d="M5 12l4 4 10-10" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round"/></svg>
          </span>
          <span class="terms-text">이용약관 및 개인정보 처리방침에 동의합니다.</span>
        </label>

        <button type="submit" class="btn-submit" :disabled="!isValid || loading">
          <span>{{ loading ? '가입 중...' : '가입하기' }}</span>
          <svg v-if="!loading" class="arrow" width="16" height="16" viewBox="0 0 24 24" fill="none">
            <path d="M5 12h14M13 6l6 6-6 6" stroke="currentColor" stroke-width="1.7" stroke-linecap="round" stroke-linejoin="round"/>
          </svg>
        </button>
      </form>

      <div class="card-footer">
        이미 계정이 있으신가요?
        <RouterLink to="/login">로그인 ♪</RouterLink>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, reactive } from 'vue';
import { useRouter } from 'vue-router';
import { RouterLink } from 'vue-router';
import { signupApi } from '../../api/auth.js';

const router = useRouter();

const form = reactive({ username: '', email: '', password: '', confirm: '', agree: false });
const errors = reactive({ username: '', email: '', password: '', confirm: '' });
const touched = reactive({ username: false, email: false });
const showPw = ref(false);
const loading = ref(false);
const serverError = ref('');

function scorePw(pw) {
  if (!pw) return 0;
  let s = 0;
  if (pw.length >= 8) s++;
  if (/[A-Z]/.test(pw) && /[a-z]/.test(pw)) s++;
  if (/\d/.test(pw)) s++;
  if (/[^A-Za-z0-9]/.test(pw) || pw.length >= 12) s++;
  return s;
}

const pwStrength = computed(() => scorePw(form.password));
const pwStrengthLabel = computed(() => {
  if (!form.password) return '최소 8자, 숫자 포함';
  return ['', '너무 약해요', '약해요', '좋아요 ✦', '매우 강해요 ✦✦'][pwStrength.value] || '';
});

function validate() {
  errors.username = form.username.trim().length < 2 ? '2자 이상 입력해주세요' :
                    form.username.trim().length > 20 ? '20자 이하로 입력해주세요' : '';
  errors.email = !/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(form.email) ? '올바른 이메일을 입력해주세요' : '';
  errors.password = form.password.length < 8 || !/\d/.test(form.password) || !/[A-Za-z]/.test(form.password)
    ? '최소 8자, 숫자와 문자를 포함해주세요' : '';
  errors.confirm = form.confirm !== form.password ? '비밀번호가 일치하지 않습니다' : '';
}

const isValid = computed(() =>
  !errors.username && !errors.email && !errors.password && !errors.confirm &&
  form.username && form.email && form.password && form.confirm && form.agree
);

async function handleSubmit() {
  validate();
  if (!isValid.value) return;
  loading.value = true;
  serverError.value = '';
  try {
    await signupApi({ username: form.username.trim(), email: form.email, password: form.password });
    router.push({ name: 'login', query: { registered: '1' } });
  } catch (err) {
    serverError.value = err.response?.data?.error?.message ?? '가입에 실패했습니다. 다시 시도해주세요.';
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
  top: 0; left: 0; right: 0;
  height: 4px;
  background: repeating-linear-gradient(90deg,
    var(--miku) 0 24px, var(--pink) 24px 48px,
    var(--lav) 48px 72px, var(--yellow) 72px 96px);
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
  width: 12px; height: 12px;
  background: var(--pink);
  border-radius: 50%;
  box-shadow: 0 0 0 3px var(--pink-lt), 0 0 10px var(--pink);
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
  text-shadow: 2px 2px 0 var(--surface), 3px 3px 0 var(--pink-lt);
}

.gradient-word {
  background: linear-gradient(90deg, var(--miku-dk), var(--lav-dk), var(--pink-dk), var(--miku-dk));
  background-size: 300% 100%;
  -webkit-background-clip: text;
  background-clip: text;
  -webkit-text-fill-color: transparent;
  animation: shimmer 4s linear infinite;
}

.card-header p {
  font-size: 14px;
  color: var(--text2);
  margin: 0 0 28px;
  line-height: 1.6;
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

form { display: flex; flex-direction: column; gap: 16px; }

.field { position: relative; }

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
  transition: border-color 0.15s, box-shadow 0.15s, background 0.15s;
}

.field input:focus {
  border-color: var(--miku);
  background: var(--surface);
  box-shadow: 0 0 0 4px var(--miku-lt);
}

.field label {
  position: absolute;
  top: 15px; left: 14px;
  font-size: 14px;
  font-weight: 600;
  color: var(--text3);
  pointer-events: none;
  transition: all 0.18s cubic-bezier(.2,.8,.2,1);
  background: transparent;
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

.field.error input { border-color: var(--coral); box-shadow: 0 0 0 4px var(--coral-lt); }
.field.error label { color: #b91c1c; }
.field.ok input { border-color: var(--miku); }

.field-hint {
  font-size: 11px;
  font-weight: 700;
  color: var(--text3);
  margin-top: 5px;
  padding-left: 2px;
  min-height: 15px;
}
.field.error .field-hint { color: #b91c1c; }
.field.ok .field-hint { color: var(--miku-dk); }

.pw-toggle {
  position: absolute;
  top: 14px; right: 12px;
  background: none;
  border: none;
  color: var(--text3);
  cursor: pointer;
  padding: 4px;
  border-radius: var(--radius-sm);
  display: grid;
  place-items: center;
}
.pw-toggle:hover { color: var(--text); background: var(--bg2); }

.pw-strength {
  display: flex;
  gap: 4px;
  margin-top: 8px;
}
.pw-strength span {
  flex: 1;
  height: 3px;
  background: var(--border);
  border-radius: 2px;
  transition: background 0.2s;
}
.pw-strength span.active.level-1 { background: var(--coral); }
.pw-strength span.active.level-2 { background: var(--yellow-dk); }
.pw-strength span.active.level-3 { background: var(--miku); }
.pw-strength span.active.level-4 { background: var(--miku-dk); }

.terms {
  display: flex;
  align-items: flex-start;
  gap: 10px;
  cursor: pointer;
  user-select: none;
  padding: 2px 0;
}
.terms input { position: absolute; opacity: 0; pointer-events: none; }
.terms .box {
  width: 20px; height: 20px;
  border: 1.5px solid var(--border);
  border-radius: 6px;
  flex-shrink: 0;
  display: grid;
  place-items: center;
  transition: all 0.15s;
  background: var(--surface);
  margin-top: 1px;
}
.terms .box svg { width: 12px; height: 12px; color: white; opacity: 0; transform: scale(0.6); transition: all 0.15s; }
.terms input:checked ~ .box { background: var(--miku-dk); border-color: var(--miku-dk); }
.terms input:checked ~ .box svg { opacity: 1; transform: scale(1); }
.terms-text { font-size: 13px; color: var(--text2); line-height: 1.5; font-weight: 600; }

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
  box-shadow: 0 6px 18px rgba(59,188,176,0.35), inset 0 1px 0 rgba(255,255,255,0.4);
  position: relative;
  overflow: hidden;
  margin-top: 4px;
}
.btn-submit::before {
  content: '';
  position: absolute;
  inset: 0;
  background: linear-gradient(120deg, transparent 30%, rgba(255,255,255,0.4) 50%, transparent 70%);
  background-size: 200% 100%;
  animation: shimmer 3s linear infinite;
}
.btn-submit:hover:not(:disabled) { transform: translateY(-2px); }
.btn-submit .arrow { transition: transform 0.2s; }
.btn-submit:hover:not(:disabled) .arrow { transform: translateX(3px); }
.btn-submit:disabled { background: var(--border); color: var(--text3); cursor: not-allowed; box-shadow: none; }
.btn-submit:disabled::before { display: none; }

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
.card-footer a:hover { color: var(--lav-dk); border-color: var(--lav-dk); }
</style>
