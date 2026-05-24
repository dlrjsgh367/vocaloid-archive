<template>
  <div>
    <HeroSection :stats="stats" />

    <FilterBar v-model="selectedMood" v-model:search="searchQuery" />

    <div class="main">
      <div class="section-header">
        <div class="section-title">캐릭터</div>
        <div class="section-title-jp">CHARACTERS / キャラクター</div>
      </div>
      <CharacterStrip :characters="characterItems" @select="onCharSelect" />

      <div v-if="songError" class="error-msg">곡 목록을 불러오지 못했습니다.</div>
      <div v-else-if="songLoading" class="loading-msg">♪ 불러오는 중...</div>
      <SongGrid v-else :songs="songItems" :total="songTotal" @like="onLike" @open="onOpen" />
    </div>
  </div>
</template>

<script setup>
import { ref, computed, watch, onMounted } from 'vue';
import { useRouter } from 'vue-router';
import HeroSection from '../components/HeroSection.vue';
import FilterBar from '../components/FilterBar.vue';
import CharacterStrip from '../components/CharacterStrip.vue';
import SongGrid from '../components/SongGrid.vue';
import { fetchCharacters } from '../api/characters.js';
import { fetchSongs } from '../api/songs.js';
import { fetchStats } from '../api/stats.js';
import { toggleLike } from '../api/likes.js';
import { useAuthStore } from '../stores/auth.js';

const router = useRouter();
const authStore = useAuthStore();

const selectedMood = ref('all');
const searchQuery = ref('');

// ── 통계 ──
const stats = ref({ songCount: 0, userCount: 0, tagCount: 0 });

// ── 캐릭터 ──
const characters = ref([]);

onMounted(async () => {
  try {
    stats.value = await fetchStats();
  } catch {
    // 통계 로드 실패는 무시 (0 유지)
  }
  try {
    characters.value = await fetchCharacters();
  } catch {
    // 캐릭터 로드 실패는 무시 (빈 스트립)
  }
  loadSongs();
});

// CharacterSummaryResponse { id, name, colorHex, imageUrl, songCount } → CharacterStrip props shape
const characterItems = computed(() =>
  characters.value.map((c) => ({
    id: c.id,
    name: c.name,
    jpName: '',
    initial: c.name.charAt(0),
    colorHex: c.colorHex,
    songCount: c.songCount ?? 0,
  })),
);

// ── 곡 목록 ──
const songs = ref([]);
const songTotal = ref(0);
const songLoading = ref(false);
const songError = ref(false);

const MOOD_MAP = {
  all: undefined,
  bright: 'BRIGHT',
  dark: 'DARK',
  emotional: 'EMOTIONAL',
  energetic: 'ENERGETIC',
  calm: 'CALM',
};

async function loadSongs() {
  songLoading.value = true;
  songError.value = false;
  try {
    const page = await fetchSongs({
      keyword: searchQuery.value || undefined,
      mood: MOOD_MAP[selectedMood.value],
      size: 20,
    });
    songs.value = page.content;
    songTotal.value = page.totalElements;
  } catch {
    songError.value = true;
  } finally {
    songLoading.value = false;
  }
}

// 필터·검색 변경 시 재조회 (300ms debounce)
let debounceTimer;
watch([selectedMood, searchQuery], () => {
  clearTimeout(debounceTimer);
  debounceTimer = setTimeout(loadSongs, 300);
});

// SongResponse → SongCard props shape
const songItems = computed(() =>
  songs.value.map((s) => ({
    id: s.id,
    title: s.title,
    producer: s.registeredBy?.username,
    characters: (s.characters ?? []).map((c) => ({
      id: c.id,
      name: c.name,
      colorHex: c.colorHex,
    })),
    tags: s.tags ?? [],
    bpm: null,
    playCount: s.playCount,
    likeCount: s.likeCount,
    duration: null,
    thumbnailUrl: s.thumbnailUrl,
  })),
);

function onCharSelect(charId) {
  router.push({ path: '/search', query: { character: charId } });
}

async function onLike(songId) {
  if (!authStore.isAuthenticated) {
    router.push({ name: 'login' });
    return;
  }
  try {
    const result = await toggleLike(songId);
    const song = songs.value.find((s) => s.id === songId);
    if (song) song.likeCount = result.likeCount;
  } catch {
    // 에러는 조용히 무시
  }
}

function onOpen(songId) {
  router.push(`/songs/${songId}`);
}
</script>

<style scoped>
.main {
  max-width: 1200px;
  margin: 0 auto;
  padding: 36px 32px;
}

.section-header {
  display: flex;
  align-items: baseline;
  gap: 14px;
  margin-bottom: 22px;
  flex-wrap: wrap;
}

.section-title {
  font-family: var(--font-display);
  font-size: 30px;
  color: var(--text);
  display: inline-flex;
  align-items: center;
  gap: 10px;
  text-shadow:
    2px 2px 0 var(--surface),
    3px 3px 0 var(--pink-lt);
  white-space: nowrap;
}

.section-title::before {
  content: '✦';
  color: var(--pink-dk);
  font-size: 22px;
  animation: sparkle 2.4s ease-in-out infinite;
}

.section-title::after {
  content: '✦';
  color: var(--miku-dk);
  font-size: 18px;
  animation: sparkle 2.4s ease-in-out infinite 0.6s;
}

.section-title-jp {
  font-family: var(--font-jp);
  font-size: 11px;
  color: var(--lav-dk);
  font-weight: 700;
  letter-spacing: 0.15em;
  background: var(--lav-lt);
  padding: 3px 10px;
  border-radius: 99px;
  align-self: center;
  white-space: nowrap;
}

.loading-msg {
  font-family: var(--font-display);
  font-size: 22px;
  color: var(--text3);
  text-align: center;
  padding: 60px 0;
  animation: float-y 2s ease-in-out infinite;
}

.error-msg {
  font-size: 14px;
  font-weight: 700;
  color: var(--pink-dk);
  background: var(--pink-lt);
  border: 1.5px solid var(--pink);
  border-radius: var(--radius-md);
  padding: 16px 24px;
  text-align: center;
}

@media (max-width: 768px) {
  .main {
    padding: 24px 16px;
  }

  .section-title {
    font-size: 22px;
  }
}
</style>
