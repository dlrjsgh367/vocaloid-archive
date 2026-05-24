<template>
  <div class="public-page">
    <div v-if="loading" class="loading-msg">♪ 불러오는 중...</div>

    <!-- 404 / 비공개 -->
    <div v-else-if="notFound" class="notfound-card">
      <div class="notfound-emoji">✦</div>
      <div class="notfound-text">플레이리스트를 찾을 수 없어요</div>
      <div class="notfound-sub">삭제되었거나 비공개로 바뀌었을 수 있어요.</div>
      <RouterLink to="/" class="btn-home">홈으로 가기</RouterLink>
    </div>

    <div v-else-if="error" class="error-banner">플레이리스트를 불러올 수 없어요.</div>

    <!-- 정상 -->
    <div v-else-if="data" class="content" :style="accentVars">
      <header class="pl-hero">
        <div class="pl-eyebrow">♪ SHARED PLAYLIST · プレイリスト</div>
        <h1 class="pl-title">{{ data.title }}</h1>
        <div class="pl-owner">by @{{ data.ownerUsername }}</div>
        <div class="pl-stats">
          <span class="stat songs">♪ {{ data.songCount }}곡</span>
          <span class="stat likes">♥ {{ data.likeSum }}</span>
          <span v-if="data.primaryCharName" class="stat char">★ {{ data.primaryCharName }}</span>
        </div>
      </header>

      <ul v-if="data.songs && data.songs.length" class="song-list">
        <li v-for="(item, i) in data.songs" :key="item.songId" class="song-row">
          <div class="song-idx">{{ i + 1 }}</div>
          <img
            :src="item.thumbnailUrl || emptyThumb"
            :alt="item.title"
            class="song-thumb"
            @error="onThumbError"
          />
          <div class="song-info">
            <div class="song-title">{{ item.title }}</div>
            <span v-if="item.mood" class="mood-badge">{{ moodLabel(item.mood) }}</span>
          </div>
        </li>
      </ul>
      <div v-else class="empty-songs">✦ 아직 담긴 곡이 없어요.</div>

      <!-- 유입(acquisition) CTA — 모두에게 노출 -->
      <div class="cta-card">
        <div class="cta-deco">✦ ♪ ✧ ♡ ✦</div>
        <div class="cta-text">나도 이런 플리, 만들 수 있어요!</div>
        <div class="cta-sub">VocaloidArchive에서 좋아하는 곡을 모아 나만의 플리를 자랑해보세요.</div>
        <RouterLink to="/signup" class="cta-btn">가입하고 나도 플리 만들기 ✦</RouterLink>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue';
import { useRoute, RouterLink } from 'vue-router';
import emptyThumb from '@/assets/empty-thumb.svg';
import { getCharColorVars } from '@/utils/characterColors.js';
import { getPublicByCode } from '@/api/playlists.js';

const route = useRoute();

const data = ref(null);
const loading = ref(true);
const error = ref(false);
const notFound = ref(false);

const accentVars = computed(() => getCharColorVars(data.value?.themeColorHex));

const MOOD_LABELS = {
  BRIGHT: '밝은',
  DARK: '어두운',
  EMOTIONAL: '감성',
  ENERGETIC: '신나는',
  CALM: '잔잔한',
};
function moodLabel(mood) {
  return MOOD_LABELS[mood] ?? mood;
}

function onThumbError(e) {
  if (e.target.dataset.fallback) return;
  e.target.dataset.fallback = '1';
  e.target.src = emptyThumb;
}

onMounted(async () => {
  const code = route.params.code;
  loading.value = true;
  error.value = false;
  notFound.value = false;
  try {
    data.value = await getPublicByCode(code);
  } catch (err) {
    const errorCode = err.response?.data?.error?.code;
    if (err.response?.status === 404 || errorCode === 'PLAYLIST_NOT_FOUND') {
      notFound.value = true;
    } else {
      error.value = true;
    }
  } finally {
    loading.value = false;
  }
});
</script>

<style scoped>
.public-page {
  max-width: 720px;
  margin: 0 auto;
  padding: 36px 20px 80px;
}

.loading-msg {
  font-family: var(--font-display);
  font-size: 22px;
  color: var(--text3);
  text-align: center;
  padding: 80px 0;
  animation: float-y 2s ease-in-out infinite;
}

.notfound-card {
  background: var(--surface);
  border: 1.5px dashed var(--border);
  border-radius: var(--radius-xl);
  padding: 60px 24px;
  text-align: center;
}
.notfound-emoji {
  font-size: 48px;
  color: var(--lav);
  animation: sparkle 3s ease-in-out infinite;
}
.notfound-text {
  font-family: var(--font-display);
  font-size: 24px;
  color: var(--text);
  margin-top: 12px;
}
.notfound-sub {
  font-size: 13px;
  color: var(--text2);
  font-weight: 600;
  margin-top: 6px;
}
.btn-home {
  display: inline-block;
  margin-top: 18px;
  font-family: var(--font-body);
  font-size: 13px;
  font-weight: 800;
  color: white;
  background: linear-gradient(135deg, var(--miku-dk), var(--lav-dk));
  padding: 10px 22px;
  border-radius: 99px;
  text-decoration: none;
  transition: transform 0.2s;
}
.btn-home:hover {
  transform: translateY(-2px);
}

.error-banner {
  background: var(--coral-lt);
  color: #b91c1c;
  border: 1.5px solid var(--coral);
  border-radius: var(--radius-sm);
  padding: 12px 16px;
  font-size: 13px;
  font-weight: 700;
  text-align: center;
}

/* ── 헤더 ── */
.pl-hero {
  background: var(--surface);
  border: 1.5px solid var(--border);
  border-radius: var(--radius-xl);
  box-shadow: var(--shadow-sm);
  padding: 26px 24px;
  position: relative;
  overflow: hidden;
  margin-bottom: 18px;
}
.pl-hero::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  height: 5px;
  background: linear-gradient(
    90deg,
    var(--c, var(--miku)) 0%,
    var(--lav) 50%,
    var(--pink) 100%
  );
}
.pl-eyebrow {
  font-family: var(--font-jp);
  font-size: 11px;
  font-weight: 800;
  color: var(--c-dk, var(--lav-dk));
  letter-spacing: 0.12em;
  text-transform: uppercase;
}
.pl-title {
  font-family: var(--font-display);
  font-size: 30px;
  color: var(--text);
  margin: 8px 0 6px;
  line-height: 1.15;
}
.pl-owner {
  font-size: 13px;
  font-weight: 800;
  color: var(--lav-dk);
  margin-bottom: 12px;
}
.pl-stats {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}
.stat {
  font-size: 12px;
  font-weight: 800;
  padding: 4px 12px;
  border-radius: 99px;
}
.stat.songs {
  color: var(--c-dk, var(--miku-dk));
  background: var(--c-lt, var(--miku-lt));
  border: 1.5px solid var(--c, var(--miku));
}
.stat.likes {
  color: var(--pink-dk);
  background: var(--pink-lt);
  border: 1.5px solid var(--pink);
}
.stat.char {
  color: var(--lav-dk);
  background: var(--lav-lt);
  border: 1.5px solid var(--lav);
}

/* ── 곡 목록 ── */
.song-list {
  list-style: none;
  padding: 0;
  margin: 0;
  display: flex;
  flex-direction: column;
  gap: 8px;
}
.song-row {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 8px 12px;
  background: var(--surface);
  border: 1.5px solid var(--border);
  border-radius: var(--radius-md);
  transition: all 0.15s;
}
.song-row:hover {
  border-color: var(--c, var(--miku));
  background: var(--c-lt, var(--miku-lt));
}
.song-idx {
  font-family: var(--font-display);
  font-size: 16px;
  font-weight: 900;
  color: var(--c-dk, var(--lav-dk));
  width: 22px;
  text-align: center;
  flex-shrink: 0;
}
.song-thumb {
  width: 52px;
  height: 52px;
  object-fit: cover;
  border-radius: var(--radius-sm);
  flex-shrink: 0;
  border: 1.5px solid var(--border);
}
.song-info {
  flex: 1;
  min-width: 0;
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}
.song-title {
  font-weight: 800;
  color: var(--text);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  font-size: 14px;
  min-width: 0;
  flex: 1;
}
.mood-badge {
  font-size: 10px;
  font-weight: 800;
  color: var(--lav-dk);
  background: var(--lav-lt);
  padding: 2px 9px;
  border-radius: 99px;
  flex-shrink: 0;
}
.empty-songs {
  text-align: center;
  padding: 36px 16px;
  color: var(--text3);
  font-size: 14px;
  font-weight: 700;
}

/* ── CTA ── */
.cta-card {
  margin-top: 24px;
  background: linear-gradient(135deg, var(--miku-lt) 0%, var(--lav-lt) 50%, var(--pink-lt) 100%);
  border: 1.5px solid var(--lav);
  border-radius: var(--radius-xl);
  padding: 28px 24px;
  text-align: center;
  position: relative;
  overflow: hidden;
}
.cta-deco {
  font-size: 16px;
  letter-spacing: 0.4em;
  color: var(--pink-dk);
  opacity: 0.7;
  margin-bottom: 8px;
  animation: sparkle 3s ease-in-out infinite;
}
.cta-text {
  font-family: var(--font-display);
  font-size: 22px;
  color: var(--text);
  margin-bottom: 6px;
}
.cta-sub {
  font-size: 13px;
  font-weight: 700;
  color: var(--text2);
  margin-bottom: 18px;
}
.cta-btn {
  display: inline-block;
  font-family: var(--font-body);
  font-size: 15px;
  font-weight: 800;
  color: white;
  background: linear-gradient(135deg, var(--pink-dk) 0%, var(--lav-dk) 100%);
  padding: 13px 28px;
  border-radius: 99px;
  text-decoration: none;
  box-shadow: 0 6px 18px rgba(232, 121, 176, 0.4);
  transition: transform 0.2s;
}
.cta-btn:hover {
  transform: translateY(-3px) scale(1.02);
}

@media (max-width: 768px) {
  .public-page {
    padding: 24px 14px 60px;
  }
  .pl-title {
    font-size: 24px;
  }
}
</style>
