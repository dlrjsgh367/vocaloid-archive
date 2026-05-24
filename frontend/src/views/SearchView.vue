<template>
  <div>
    <FilterBar v-model="selectedMood" v-model:search="searchQuery" />

    <div class="search-page">
      <div class="search-toolbar">
        <div class="char-pill-row">
          <button
            class="char-pill"
            :class="{ active: !selectedCharacterId }"
            @click="setCharacter(null)"
          >
            전체 캐릭터
          </button>
          <button
            v-for="char in characters"
            :key="char.id"
            class="char-pill"
            :class="{ active: selectedCharacterId === char.id }"
            :style="getCharColorVars(char.colorHex)"
            @click="setCharacter(char.id)"
          >
            {{ char.name }}
          </button>
        </div>

        <div class="sort-wrap">
          <label class="sort-label">정렬</label>
          <select v-model="selectedSort" class="sort-select">
            <option value="LATEST">최신순</option>
            <option value="POPULAR">인기순</option>
            <option value="PLAYED">재생순</option>
          </select>
        </div>
      </div>

      <div class="result-meta">
        <span v-if="!loading && !error">
          총 <strong>{{ total }}</strong
          >곡
          <span v-if="hasFilters" class="filter-summary">· {{ filterSummary }}</span>
        </span>
      </div>

      <div v-if="error" class="error-banner">검색 결과를 불러올 수 없습니다.</div>
      <div v-else-if="loading" class="loading-msg">♪ 불러오는 중...</div>
      <div v-else-if="songs.length === 0" class="empty-msg">
        ✦ 조건에 맞는 곡이 없어요. 필터를 바꿔보세요.
      </div>
      <SongGrid v-else :songs="songItems" :total="total" @like="onLike" @open="onOpen" />

      <div v-if="totalPages > 1" class="pagination">
        <button :disabled="page === 0" @click="goToPage(page - 1)">‹ 이전</button>
        <span class="page-indicator">{{ page + 1 }} / {{ totalPages }}</span>
        <button :disabled="page >= totalPages - 1" @click="goToPage(page + 1)">다음 ›</button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, watch, onMounted } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import FilterBar from '../components/FilterBar.vue';
import SongGrid from '../components/SongGrid.vue';
import { fetchSongs } from '../api/songs.js';
import { fetchCharacters } from '../api/characters.js';
import { toggleLike } from '../api/likes.js';
import { useAuthStore } from '../stores/auth.js';
import { getCharColorVars } from '../utils/characterColors.js';

const route = useRoute();
const router = useRouter();
const authStore = useAuthStore();

const MOOD_MAP = {
  all: undefined,
  bright: 'BRIGHT',
  dark: 'DARK',
  emotional: 'EMOTIONAL',
  energetic: 'ENERGETIC',
  calm: 'CALM',
};
const MOOD_LABELS = {
  bright: '밝음',
  dark: '다크',
  emotional: '감성',
  energetic: '신남',
  calm: '잔잔함',
};

const PAGE_SIZE = 20;

const selectedMood = ref('all');
const searchQuery = ref('');
const selectedCharacterId = ref(null);
const selectedSort = ref('LATEST');
const page = ref(0);

const songs = ref([]);
const total = ref(0);
const totalPages = ref(0);
const loading = ref(false);
const error = ref(false);

const characters = ref([]);

// ── URL 쿼리 → 상태 (초기화 + 뒤/앞 네비게이션 대응) ──
function applyFromQuery() {
  const q = route.query;
  selectedMood.value = q.mood && MOOD_MAP[q.mood] ? q.mood : 'all';
  searchQuery.value = q.keyword ? String(q.keyword) : '';
  selectedCharacterId.value = q.character ? Number(q.character) : null;
  selectedSort.value = ['LATEST', 'POPULAR', 'PLAYED'].includes(q.sort) ? q.sort : 'LATEST';
  page.value = q.page ? Math.max(0, Number(q.page)) : 0;
}

// ── 상태 → URL 쿼리 ──
function pushQuery(extra = {}) {
  const query = {};
  if (selectedMood.value !== 'all') query.mood = selectedMood.value;
  if (searchQuery.value.trim()) query.keyword = searchQuery.value.trim();
  if (selectedCharacterId.value) query.character = String(selectedCharacterId.value);
  if (selectedSort.value !== 'LATEST') query.sort = selectedSort.value;
  if (page.value > 0) query.page = String(page.value);
  Object.assign(query, extra);
  router.replace({ name: 'search', query });
}

async function loadSongs() {
  loading.value = true;
  error.value = false;
  try {
    const params = {
      keyword: searchQuery.value.trim() || undefined,
      mood: MOOD_MAP[selectedMood.value],
      characterId: selectedCharacterId.value ?? undefined,
      sort: selectedSort.value,
      page: page.value,
      size: PAGE_SIZE,
    };
    const res = await fetchSongs(params);
    songs.value = res.content;
    total.value = res.totalElements;
    totalPages.value = res.totalPages;
  } catch {
    error.value = true;
    songs.value = [];
  } finally {
    loading.value = false;
  }
}

function setCharacter(id) {
  selectedCharacterId.value = id;
  page.value = 0;
}

function goToPage(p) {
  if (p < 0 || p >= totalPages.value) return;
  page.value = p;
  window.scrollTo({ top: 0, behavior: 'smooth' });
}

// 필터 변경 시 page 0으로 리셋 + URL 갱신 + 재조회 (키워드는 디바운스)
let debounceTimer;
watch(searchQuery, () => {
  clearTimeout(debounceTimer);
  debounceTimer = setTimeout(() => {
    page.value = 0;
    pushQuery();
    loadSongs();
  }, 300);
});

watch([selectedMood, selectedCharacterId, selectedSort], () => {
  page.value = 0;
  pushQuery();
  loadSongs();
});

watch(page, () => {
  pushQuery();
  loadSongs();
});

// 브라우저 back/forward 시 쿼리 변화 → 상태 동기화
watch(
  () => route.query,
  () => {
    if (route.name !== 'search') return;
    const before = JSON.stringify({
      mood: selectedMood.value,
      kw: searchQuery.value,
      char: selectedCharacterId.value,
      sort: selectedSort.value,
      page: page.value,
    });
    applyFromQuery();
    const after = JSON.stringify({
      mood: selectedMood.value,
      kw: searchQuery.value,
      char: selectedCharacterId.value,
      sort: selectedSort.value,
      page: page.value,
    });
    if (before !== after) loadSongs();
  },
);

onMounted(async () => {
  applyFromQuery();
  try {
    characters.value = await fetchCharacters();
  } catch {
    // 캐릭터 실패는 무시
  }
  loadSongs();
});

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

const hasFilters = computed(
  () =>
    selectedMood.value !== 'all' ||
    searchQuery.value.trim() ||
    selectedCharacterId.value ||
    selectedSort.value !== 'LATEST',
);

const filterSummary = computed(() => {
  const parts = [];
  if (searchQuery.value.trim()) parts.push(`'${searchQuery.value.trim()}'`);
  if (selectedMood.value !== 'all') parts.push(MOOD_LABELS[selectedMood.value]);
  if (selectedCharacterId.value) {
    const c = characters.value.find((x) => x.id === selectedCharacterId.value);
    if (c) parts.push(c.name);
  }
  return parts.join(' · ');
});

async function onLike(songId) {
  if (!authStore.isAuthenticated) {
    router.push({ name: 'login', query: { return: route.fullPath } });
    return;
  }
  try {
    const result = await toggleLike(songId);
    const song = songs.value.find((s) => s.id === songId);
    if (song) song.likeCount = result.likeCount;
  } catch {
    // 무시
  }
}

function onOpen(songId) {
  router.push(`/songs/${songId}`);
}
</script>

<style scoped>
.search-page {
  max-width: 1200px;
  margin: 0 auto;
  padding: 28px 32px 80px;
}

.search-toolbar {
  display: flex;
  align-items: center;
  gap: 16px;
  margin-bottom: 18px;
  flex-wrap: wrap;
}

.char-pill-row {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  flex: 1;
  min-width: 0;
}
/* char-pill에 colorHex 기반 --c/--c-dk/--c-lt가 인라인 스타일로 주입되면
 * 캐릭터 색으로 렌더된다. 스타일이 없는 '전체' 버튼은 fallback 값(중립색)으로 렌더된다.
 */
.char-pill {
  font-size: 12px;
  font-weight: 800;
  color: var(--c-dk, var(--text2));
  background: var(--c-lt, var(--surface));
  border: 1.5px solid var(--c, var(--border));
  padding: 6px 14px;
  border-radius: 99px;
  cursor: pointer;
  transition: all 0.2s;
}
.char-pill:hover {
  transform: translateY(-1px);
}
.char-pill.active {
  background: var(--c-dk, var(--miku-dk));
  color: white;
  border-color: var(--c-dk, var(--miku-dk));
}

.sort-wrap {
  display: flex;
  align-items: center;
  gap: 8px;
}
.sort-label {
  font-size: 11px;
  font-weight: 800;
  color: var(--text3);
  letter-spacing: 0.1em;
  text-transform: uppercase;
}
.sort-select {
  font-family: var(--font-body);
  font-size: 13px;
  font-weight: 800;
  color: var(--text);
  background: var(--surface);
  border: 1.5px solid var(--border);
  border-radius: 99px;
  padding: 6px 14px;
  cursor: pointer;
  outline: none;
  transition: all 0.2s;
}
.sort-select:focus {
  border-color: var(--miku);
  box-shadow: 0 0 0 4px var(--miku-lt);
}

.result-meta {
  font-size: 13px;
  font-weight: 700;
  color: var(--text2);
  margin-bottom: 14px;
}
.result-meta strong {
  color: var(--miku-dk);
  font-weight: 900;
}
.filter-summary {
  color: var(--lav-dk);
  margin-left: 6px;
}

.error-banner {
  background: var(--coral-lt);
  color: #b91c1c;
  border: 1.5px solid var(--coral);
  border-radius: var(--radius-sm);
  padding: 14px 18px;
  font-size: 13px;
  font-weight: 700;
  text-align: center;
}

.loading-msg {
  font-family: var(--font-display);
  font-size: 22px;
  color: var(--text3);
  text-align: center;
  padding: 60px 0;
  animation: float-y 2s ease-in-out infinite;
}

.empty-msg {
  font-family: var(--font-display);
  font-size: 22px;
  color: var(--text3);
  text-align: center;
  padding: 60px 0;
}

.pagination {
  display: flex;
  justify-content: center;
  align-items: center;
  gap: 10px;
  margin-top: 28px;
}
.pagination button {
  font-size: 12px;
  font-weight: 800;
  color: var(--text);
  background: var(--surface);
  border: 1.5px solid var(--border);
  padding: 8px 18px;
  border-radius: 99px;
  cursor: pointer;
  transition: all 0.2s;
}
.pagination button:hover:not(:disabled) {
  border-color: var(--miku);
  color: var(--miku-dk);
  transform: translateY(-1px);
}
.pagination button:disabled {
  opacity: 0.4;
  cursor: not-allowed;
}
.page-indicator {
  font-size: 13px;
  font-weight: 800;
  color: var(--text2);
}

@media (max-width: 768px) {
  .search-page {
    padding: 20px 16px 60px;
  }

  .search-toolbar {
    flex-direction: column;
    align-items: stretch;
    gap: 10px;
  }

  .sort-wrap {
    justify-content: flex-end;
  }

  .sort-select {
    font-size: 12px;
  }
}
</style>
