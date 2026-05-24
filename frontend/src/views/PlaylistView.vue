<template>
  <div class="playlist-page">
    <div class="page-header">
      <div>
        <div class="eyebrow">♪ MY PLAYLISTS · プレイリスト</div>
        <h1>나의 <span class="gradient-word">플레이리스트</span></h1>
      </div>
      <button class="btn-create" @click="openCreateModal">+ 새 플레이리스트</button>
    </div>

    <div v-if="error" class="error-banner">플레이리스트를 불러올 수 없습니다.</div>
    <div v-else-if="loading" class="loading-msg">♪ 불러오는 중...</div>
    <div v-else-if="playlists.length === 0" class="empty-card">
      <div class="empty-emoji">✦</div>
      <div class="empty-text">아직 플레이리스트가 없어요</div>
      <div class="empty-sub">첫 플레이리스트를 만들어 좋아하는 곡을 모아보세요.</div>
      <button class="btn-create primary" @click="openCreateModal">+ 만들기</button>
    </div>

    <div v-else class="layout">
      <!-- 좌측: 목록 -->
      <aside class="list-pane">
        <ul class="playlist-list">
          <li
            v-for="pl in playlists"
            :key="pl.id"
            class="playlist-row"
            :class="{ active: selectedId === pl.id }"
            @click="selectPlaylist(pl.id)"
          >
            <div class="playlist-row-info">
              <div class="playlist-row-title">{{ pl.title }}</div>
              <div class="playlist-row-sub">
                <span :class="['visibility', pl.isPublic ? 'public' : 'private']">
                  {{ pl.isPublic ? '공개' : '비공개' }}
                </span>
                · {{ pl.songCount }}곡 · {{ formatDate(pl.createdAt) }}
              </div>
            </div>
            <button class="row-delete" @click.stop="onDeletePlaylist(pl)" title="삭제">✕</button>
          </li>
        </ul>
      </aside>

      <!-- 우측: 상세 -->
      <section class="detail-pane">
        <div v-if="!selectedId" class="detail-empty">← 목록에서 플레이리스트를 선택해주세요</div>
        <div v-else-if="detailLoading" class="loading-msg sm">♪ 불러오는 중...</div>
        <div v-else-if="detailError" class="error-banner">상세를 불러올 수 없어요.</div>
        <div v-else-if="detail" class="detail-card">
          <div class="detail-header">
            <h2 class="detail-title">{{ detail.title }}</h2>
            <span :class="['visibility', detail.isPublic ? 'public' : 'private']">
              {{ detail.isPublic ? '공개' : '비공개' }}
            </span>
            <button
              class="btn-share"
              :disabled="sharing || detail.songs.length === 0"
              :title="detail.songs.length === 0 ? '곡을 먼저 담아주세요' : '공유하기'"
              @click="onShare"
            >
              {{ sharing ? '준비 중...' : '✦ 공유' }}
            </button>
          </div>
          <div v-if="shareError" class="error-banner sm share-msg">{{ shareError }}</div>
          <div class="detail-meta">
            {{ detail.ownerUsername }} · {{ detail.songs.length }}곡 ·
            {{ formatDate(detail.createdAt) }}
          </div>

          <ul v-if="detail.songs.length" class="song-list">
            <li v-for="item in detail.songs" :key="item.songId" class="song-row">
              <img
                :src="item.thumbnailUrl || emptyThumb"
                :alt="item.title"
                class="song-thumb"
                @error="onThumbError"
              />
              <div class="song-info">
                <div class="song-title">{{ item.title }}</div>
                <div class="song-order">#{{ item.orderIndex + 1 }}</div>
              </div>
              <RouterLink :to="`/songs/${item.songId}`" class="btn-go">곡 보기</RouterLink>
              <button class="btn-remove" @click="onRemoveSong(item.songId)" title="제거">✕</button>
            </li>
          </ul>
          <div v-else class="detail-empty-songs">
            ✦ 아직 담긴 곡이 없어요. <RouterLink to="/" class="link">곡 둘러보러 가기</RouterLink>
          </div>
        </div>
      </section>
    </div>

    <!-- 생성 모달 -->
    <div v-if="createOpen" class="modal-backdrop" @click.self="closeCreateModal">
      <div class="modal">
        <div class="modal-header">
          <h2>새 플레이리스트</h2>
          <button class="modal-close" @click="closeCreateModal">✕</button>
        </div>
        <form class="modal-body" @submit.prevent="onCreate">
          <div class="field" :class="{ error: createErrors.title }">
            <input
              id="newTitle"
              v-model="createForm.title"
              type="text"
              placeholder=" "
              maxlength="200"
              @input="createErrors.title = ''"
            />
            <label for="newTitle">제목 *</label>
            <div class="field-hint">{{ createErrors.title }}</div>
          </div>

          <label class="checkbox-row">
            <input type="checkbox" v-model="createForm.isPublic" />
            <span>다른 사람에게 공개</span>
          </label>

          <div v-if="createServerError" class="error-banner sm">{{ createServerError }}</div>

          <div class="modal-actions">
            <button type="button" class="btn-cancel" @click="closeCreateModal">취소</button>
            <button
              type="submit"
              class="btn-primary"
              :disabled="creating || !createForm.title.trim()"
            >
              {{ creating ? '만드는 중...' : '✦ 만들기' }}
            </button>
          </div>
        </form>
      </div>
    </div>

    <!-- 공유 시트 -->
    <ShareSheet
      v-if="shareOpen"
      :share-url="shareData.shareUrl"
      :code="shareData.shareCode"
      @close="shareOpen = false"
    />
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue';
import { RouterLink } from 'vue-router';
import emptyThumb from '@/assets/empty-thumb.svg';
import ShareSheet from '../components/ShareSheet.vue';
import {
  fetchMyPlaylists,
  fetchPlaylist,
  createPlaylist,
  deletePlaylist,
  removeSongFromPlaylist,
  ensureShare,
} from '../api/playlists.js';

function onThumbError(e) {
  if (e.target.dataset.fallback) return;
  e.target.dataset.fallback = '1';
  e.target.src = emptyThumb;
}

const playlists = ref([]);
const loading = ref(true);
const error = ref(false);

const selectedId = ref(null);
const detail = ref(null);
const detailLoading = ref(false);
const detailError = ref(false);

const sharing = ref(false);
const shareError = ref('');
const shareOpen = ref(false);
const shareData = reactive({ shareUrl: '', shareCode: '' });

const createOpen = ref(false);
const createForm = reactive({ title: '', isPublic: false });
const createErrors = reactive({ title: '' });
const createServerError = ref('');
const creating = ref(false);

async function loadPlaylists() {
  loading.value = true;
  error.value = false;
  try {
    playlists.value = await fetchMyPlaylists();
  } catch {
    error.value = true;
  } finally {
    loading.value = false;
  }
}

async function selectPlaylist(id) {
  selectedId.value = id;
  detail.value = null;
  detailError.value = false;
  detailLoading.value = true;
  shareError.value = '';
  try {
    detail.value = await fetchPlaylist(id);
  } catch {
    detailError.value = true;
  } finally {
    detailLoading.value = false;
  }
}

async function onDeletePlaylist(pl) {
  if (!confirm(`'${pl.title}' 플레이리스트를 삭제할까요?`)) return;
  try {
    await deletePlaylist(pl.id);
    playlists.value = playlists.value.filter((p) => p.id !== pl.id);
    if (selectedId.value === pl.id) {
      selectedId.value = null;
      detail.value = null;
    }
  } catch {
    alert('삭제에 실패했어요.');
  }
}

async function onRemoveSong(songId) {
  if (!detail.value || !selectedId.value) return;
  if (!confirm('이 곡을 플레이리스트에서 제거할까요?')) return;
  try {
    await removeSongFromPlaylist(selectedId.value, songId);
    detail.value.songs = detail.value.songs.filter((s) => s.songId !== songId);
    const pl = playlists.value.find((p) => p.id === selectedId.value);
    if (pl) pl.songCount = Math.max(0, pl.songCount - 1);
  } catch {
    alert('제거에 실패했어요.');
  }
}

async function onShare() {
  if (!detail.value || !selectedId.value) return;
  if (detail.value.songs.length === 0) {
    shareError.value = '곡을 먼저 담은 뒤 공유할 수 있어요.';
    return;
  }
  sharing.value = true;
  shareError.value = '';
  try {
    const res = await ensureShare(selectedId.value);
    shareData.shareUrl = res.shareUrl;
    shareData.shareCode = res.shareCode;
    shareOpen.value = true;
  } catch (err) {
    const code = err.response?.data?.error?.code;
    if (code === 'PLAYLIST_NOT_PUBLIC') {
      shareError.value = '공개 플레이리스트만 공유할 수 있어요.';
    } else {
      shareError.value = err.response?.data?.error?.message ?? '공유 준비에 실패했어요.';
    }
  } finally {
    sharing.value = false;
  }
}

function openCreateModal() {
  createForm.title = '';
  createForm.isPublic = false;
  createErrors.title = '';
  createServerError.value = '';
  createOpen.value = true;
}

function closeCreateModal() {
  createOpen.value = false;
}

async function onCreate() {
  createErrors.title = '';
  createServerError.value = '';
  const title = createForm.title.trim();
  if (!title) {
    createErrors.title = '제목을 입력해주세요.';
    return;
  }
  if (title.length > 200) {
    createErrors.title = '제목은 200자 이하여야 해요.';
    return;
  }
  creating.value = true;
  try {
    const created = await createPlaylist({ title, isPublic: createForm.isPublic });
    playlists.value.unshift(created);
    closeCreateModal();
    selectPlaylist(created.id);
  } catch (err) {
    createServerError.value = err.response?.data?.error?.message ?? '만드는 데 실패했어요.';
  } finally {
    creating.value = false;
  }
}

function formatDate(iso) {
  if (!iso) return '';
  const d = new Date(iso);
  const yy = d.getFullYear();
  const mm = String(d.getMonth() + 1).padStart(2, '0');
  const dd = String(d.getDate()).padStart(2, '0');
  return `${yy}.${mm}.${dd}`;
}

onMounted(loadPlaylists);
</script>

<style scoped>
.playlist-page {
  max-width: 1200px;
  margin: 0 auto;
  padding: 36px 32px 80px;
}

.page-header {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: 20px;
  margin-bottom: 28px;
  flex-wrap: wrap;
}

.eyebrow {
  font-family: var(--font-jp);
  font-size: 11px;
  font-weight: 800;
  color: var(--lav-dk);
  letter-spacing: 0.12em;
  text-transform: uppercase;
  margin-bottom: 6px;
}
h1 {
  font-family: var(--font-display);
  font-size: 32px;
  line-height: 1.1;
  color: var(--text);
  margin: 0;
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
}

.btn-create {
  font-family: var(--font-body);
  font-size: 13px;
  font-weight: 800;
  color: var(--miku-dk);
  background: var(--surface);
  border: 1.5px solid var(--miku);
  padding: 10px 20px;
  border-radius: 99px;
  cursor: pointer;
  transition: all 0.2s;
}
.btn-create:hover {
  background: var(--miku-lt);
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(59, 188, 176, 0.3);
}
.btn-create.primary {
  background: linear-gradient(135deg, var(--miku-dk), var(--lav-dk));
  color: white;
  border-color: transparent;
  margin-top: 16px;
}

.error-banner {
  background: var(--coral-lt);
  color: #b91c1c;
  border: 1.5px solid var(--coral);
  border-radius: var(--radius-sm);
  padding: 12px 16px;
  font-size: 13px;
  font-weight: 700;
}
.error-banner.sm {
  font-size: 12px;
  padding: 10px 14px;
}

.loading-msg {
  font-family: var(--font-display);
  font-size: 22px;
  color: var(--text3);
  text-align: center;
  padding: 60px 0;
  animation: float-y 2s ease-in-out infinite;
}
.loading-msg.sm {
  font-size: 16px;
  padding: 28px 0;
}

.empty-card {
  background: var(--surface);
  border: 1.5px dashed var(--border);
  border-radius: var(--radius-xl);
  padding: 60px 24px;
  text-align: center;
}
.empty-emoji {
  font-size: 48px;
  color: var(--lav);
  animation: sparkle 3s ease-in-out infinite;
}
.empty-text {
  font-family: var(--font-display);
  font-size: 24px;
  color: var(--text);
  margin-top: 12px;
}
.empty-sub {
  font-size: 13px;
  color: var(--text2);
  font-weight: 600;
  margin-top: 6px;
}

.layout {
  display: grid;
  grid-template-columns: minmax(260px, 1fr) minmax(0, 2fr);
  gap: 20px;
}
@media (max-width: 800px) {
  .layout {
    grid-template-columns: 1fr;
  }
}

.list-pane {
  min-width: 0;
  background: var(--surface);
  border-radius: var(--radius-lg);
  border: 1.5px solid var(--border);
  box-shadow: var(--shadow-sm);
  padding: 12px;
  position: relative;
  overflow: hidden;
}
.list-pane::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  height: 3px;
  background: repeating-linear-gradient(
    90deg,
    var(--miku) 0 18px,
    var(--pink) 18px 36px,
    var(--lav) 36px 54px,
    var(--yellow) 54px 72px
  );
}

.playlist-list {
  list-style: none;
  padding: 0;
  margin: 4px 0 0;
  display: flex;
  flex-direction: column;
  gap: 6px;
}
.playlist-row {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 12px 14px;
  border-radius: var(--radius-md);
  cursor: pointer;
  transition: all 0.15s;
  border: 1.5px solid transparent;
}
.playlist-row:hover {
  background: var(--bg2);
}
.playlist-row.active {
  background: var(--miku-lt);
  border-color: var(--miku);
}
.playlist-row-info {
  flex: 1;
  min-width: 0;
}
.playlist-row-title {
  font-weight: 800;
  color: var(--text);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
.playlist-row-sub {
  font-size: 11px;
  color: var(--text3);
  font-weight: 700;
  margin-top: 2px;
}
.visibility {
  display: inline-block;
  padding: 1px 8px;
  border-radius: 99px;
  font-size: 10px;
  font-weight: 800;
  letter-spacing: 0.05em;
}
.visibility.public {
  color: var(--miku-dk);
  background: var(--miku-lt);
}
.visibility.private {
  color: var(--text3);
  background: var(--bg2);
}
.row-delete {
  background: none;
  border: none;
  color: var(--text3);
  cursor: pointer;
  font-size: 12px;
  padding: 4px 8px;
  border-radius: 6px;
  opacity: 0.5;
  transition: all 0.15s;
}
.playlist-row:hover .row-delete {
  opacity: 1;
}
.row-delete:hover {
  color: var(--coral);
  background: var(--coral-lt);
}

.detail-pane {
  min-width: 0;
  min-height: 200px;
}
.detail-empty {
  background: var(--surface);
  border: 1.5px dashed var(--border);
  border-radius: var(--radius-lg);
  padding: 80px 24px;
  text-align: center;
  color: var(--text3);
  font-family: var(--font-display);
  font-size: 18px;
}
.detail-card {
  background: var(--surface);
  border-radius: var(--radius-lg);
  border: 1.5px solid var(--border);
  box-shadow: var(--shadow-sm);
  padding: 24px;
  position: relative;
  overflow: hidden;
}
.detail-card::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  height: 3px;
  background: linear-gradient(90deg, var(--miku) 0%, var(--lav) 50%, var(--pink) 100%);
}
.detail-header {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 4px;
}
.detail-title {
  font-family: var(--font-jp);
  font-size: 22px;
  font-weight: 900;
  color: var(--text);
  margin: 0;
}
.detail-meta {
  font-size: 12px;
  color: var(--text3);
  font-weight: 700;
  margin-bottom: 18px;
}

.btn-share {
  margin-left: auto;
  font-family: var(--font-body);
  font-size: 12px;
  font-weight: 800;
  color: white;
  background: linear-gradient(135deg, var(--pink-dk) 0%, var(--lav-dk) 100%);
  border: none;
  padding: 7px 16px;
  border-radius: 99px;
  cursor: pointer;
  transition: all 0.2s;
  flex-shrink: 0;
}
.btn-share:hover:not(:disabled) {
  transform: translateY(-2px) scale(1.03);
  box-shadow: 0 4px 12px rgba(232, 121, 176, 0.4);
}
.btn-share:disabled {
  background: var(--border);
  color: var(--text3);
  cursor: not-allowed;
}
.share-msg {
  margin-bottom: 12px;
}

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
  padding: 8px 10px;
  background: var(--surface2);
  border: 1.5px solid var(--border);
  border-radius: var(--radius-md);
  transition: all 0.15s;
}
.song-row:hover {
  border-color: var(--miku);
  background: var(--miku-lt);
}
.song-thumb {
  width: 56px;
  height: 56px;
  object-fit: cover;
  border-radius: var(--radius-sm);
  flex-shrink: 0;
  border: 1.5px solid var(--border);
}
.song-info {
  flex: 1;
  min-width: 0;
}
.song-title {
  font-weight: 800;
  color: var(--text);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  font-size: 14px;
}
.song-order {
  font-size: 11px;
  color: var(--text3);
  font-weight: 700;
  margin-top: 2px;
}
.btn-go {
  font-size: 11px;
  font-weight: 800;
  color: var(--lav-dk);
  background: var(--lav-lt);
  border: 1.5px solid var(--lav);
  padding: 5px 12px;
  border-radius: 99px;
  text-decoration: none;
  transition: all 0.2s;
}
.btn-go:hover {
  background: var(--lav);
  color: white;
}
.btn-remove {
  background: none;
  border: 1.5px solid var(--coral-lt);
  color: var(--coral);
  cursor: pointer;
  font-size: 12px;
  padding: 4px 8px;
  border-radius: 99px;
  transition: all 0.15s;
}
.btn-remove:hover {
  background: var(--coral);
  color: white;
  border-color: var(--coral);
}

.detail-empty-songs {
  text-align: center;
  padding: 36px 16px;
  color: var(--text3);
  font-size: 14px;
  font-weight: 700;
}

.link {
  color: var(--miku-dk);
  font-weight: 800;
  text-decoration: none;
  border-bottom: 1.5px solid var(--miku);
}

/* ── 모달 ── */
.modal-backdrop {
  position: fixed;
  inset: 0;
  background: rgba(45, 27, 78, 0.45);
  backdrop-filter: blur(8px);
  display: grid;
  place-items: center;
  z-index: 100;
  padding: 16px;
}
.modal {
  width: 100%;
  max-width: 460px;
  background: var(--surface);
  border-radius: var(--radius-xl);
  border: 1.5px solid var(--border);
  box-shadow: var(--shadow-lg);
  overflow: hidden;
  position: relative;
}
.modal::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  height: 4px;
  background: repeating-linear-gradient(
    90deg,
    var(--miku) 0 20px,
    var(--pink) 20px 40px,
    var(--lav) 40px 60px,
    var(--yellow) 60px 80px
  );
}
.modal-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 22px 24px 12px;
}
.modal-header h2 {
  font-family: var(--font-display);
  font-size: 24px;
  margin: 0;
  color: var(--text);
}
.modal-close {
  background: none;
  border: none;
  color: var(--text3);
  font-size: 18px;
  cursor: pointer;
  padding: 4px 8px;
  border-radius: 8px;
}
.modal-close:hover {
  color: var(--text);
  background: var(--bg2);
}
.modal-body {
  padding: 4px 24px 24px;
  display: flex;
  flex-direction: column;
  gap: 14px;
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
.field-hint {
  font-size: 11px;
  font-weight: 700;
  color: #b91c1c;
  margin-top: 5px;
  padding-left: 2px;
  min-height: 14px;
}

.checkbox-row {
  display: flex;
  align-items: center;
  gap: 10px;
  font-size: 13px;
  font-weight: 700;
  color: var(--text2);
  cursor: pointer;
}
.checkbox-row input {
  width: 16px;
  height: 16px;
  cursor: pointer;
}

.modal-actions {
  display: flex;
  gap: 10px;
  margin-top: 4px;
}
.btn-cancel {
  flex: 0 0 auto;
  height: 44px;
  padding: 0 20px;
  border: 1.5px solid var(--border);
  background: var(--surface);
  border-radius: var(--radius-md);
  font-family: var(--font-body);
  font-size: 13px;
  font-weight: 800;
  color: var(--text2);
  cursor: pointer;
  transition: all 0.2s;
}
.btn-cancel:hover {
  border-color: var(--text3);
  color: var(--text);
}
.btn-primary {
  flex: 1;
  height: 44px;
  border: none;
  background: linear-gradient(135deg, var(--miku-dk) 0%, var(--lav-dk) 100%);
  color: white;
  border-radius: var(--radius-md);
  font-family: var(--font-body);
  font-size: 14px;
  font-weight: 800;
  cursor: pointer;
  transition: all 0.2s;
}
.btn-primary:hover:not(:disabled) {
  transform: translateY(-2px);
}
.btn-primary:disabled {
  background: var(--border);
  color: var(--text3);
  cursor: not-allowed;
}

@media (max-width: 768px) {
  .playlist-page {
    padding: 24px 16px 60px;
  }

  .page-header {
    flex-direction: column;
    align-items: flex-start;
    gap: 12px;
  }

  h1 {
    font-size: 26px;
  }

  .detail-empty {
    padding: 40px 16px;
    font-size: 15px;
  }

  .modal-backdrop {
    align-items: flex-end;
    padding: 0;
  }

  .modal {
    border-radius: var(--radius-xl) var(--radius-xl) 0 0;
    max-width: 100%;
  }
}
</style>
