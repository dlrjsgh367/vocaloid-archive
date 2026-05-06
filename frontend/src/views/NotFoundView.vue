<template>
  <div class="not-found-page">
    <div class="ghost">404</div>
    <div class="jp-sub">ページがありません</div>
    <h1>찾으시는 곡은 <span class="gradient-word">없는 것 같아요</span></h1>
    <p>주소가 정확한지 확인해주세요. 아니면 홈에서 다시 시작해볼까요?</p>
    <div class="actions">
      <RouterLink to="/" class="btn-primary">✦ 홈으로</RouterLink>
      <RouterLink to="/search" class="btn-secondary">곡 둘러보기</RouterLink>
    </div>
  </div>
</template>

<script setup>
import { RouterLink } from 'vue-router';
</script>

<style scoped>
.not-found-page {
  min-height: calc(100vh - 68px);
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  text-align: center;
  padding: 40px 20px;
}

.ghost {
  font-family: var(--font-display);
  font-size: 140px;
  line-height: 1;
  color: var(--lav-lt);
  text-shadow:
    4px 4px 0 var(--surface),
    8px 8px 0 var(--pink-lt);
  animation: float-y 3s ease-in-out infinite;
}

.jp-sub {
  font-family: var(--font-jp);
  font-size: 12px;
  letter-spacing: 0.2em;
  color: var(--lav-dk);
  font-weight: 800;
  background: var(--lav-lt);
  padding: 4px 14px;
  border-radius: 99px;
  margin-top: 12px;
}

h1 {
  font-family: var(--font-display);
  font-size: 36px;
  color: var(--text);
  margin: 18px 0 10px;
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

p {
  font-size: 14px;
  color: var(--text2);
  font-weight: 600;
  margin: 0 0 28px;
}

.actions {
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
  justify-content: center;
}
.btn-primary {
  font-size: 14px;
  font-weight: 800;
  color: white;
  background: linear-gradient(135deg, var(--miku-dk), var(--lav-dk));
  padding: 12px 28px;
  border-radius: 99px;
  text-decoration: none;
  box-shadow: 0 6px 18px rgba(59,188,176,0.35);
  transition: transform 0.2s;
}
.btn-primary:hover { transform: translateY(-2px); }

.btn-secondary {
  font-size: 14px;
  font-weight: 800;
  color: var(--lav-dk);
  background: var(--surface);
  border: 1.5px solid var(--lav);
  padding: 12px 28px;
  border-radius: 99px;
  text-decoration: none;
  transition: all 0.2s;
}
.btn-secondary:hover {
  background: var(--lav-lt);
  transform: translateY(-2px);
}
</style>
