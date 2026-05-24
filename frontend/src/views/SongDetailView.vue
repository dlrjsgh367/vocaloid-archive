<template>
  <div class="detail-page">
    <div v-if="loading" class="loading-msg">♪ 불러오는 중...</div>

    <div v-else-if="error" class="error-banner">
      곡을 불러올 수 없습니다. <RouterLink to="/" class="link">홈으로</RouterLink>
    </div>

    <div v-else-if="song" class="detail-grid">
      <!-- 좌: 플레이어 -->
      <section class="player-section">
        <div v-if="youtubeEmbedUrl" class="player">
          <iframe
            :src="youtubeEmbedUrl"
            title="YouTube player"
            frameborder="0"
            allow="
              accelerometer;
              autoplay;
              clipboard-write;
              encrypted-media;
              gyroscope;
              picture-in-picture;
            "
            allowfullscreen
          />
        </div>
        <div v-else class="player-empty">
          <span class="player-empty-text">▶ 재생 가능한 영상이 없어요</span>
        </div>

        <div v-if="song.niconicoUrl" class="external-link-row">
          <a :href="song.niconicoUrl" target="_blank" rel="noopener" class="external-link">
            ★ 니코니코에서 보기
          </a>
        </div>
      </section>

      <!-- 우: 메타데이터 -->
      <aside class="meta-section">
        <div class="meta-card">
          <div class="meta-eyebrow">♪ {{ MOOD_LABELS[song.mood] ?? song.mood }}</div>
          <h1 class="meta-title">{{ song.title }}</h1>

          <div class="char-row">
            <span
              v-for="char in song.characters"
              :key="char.id"
              class="char-badge"
              :style="getCharColorVars(char.colorHex)"
            >
              {{ char.name }}
            </span>
          </div>

          <div class="producer-row" v-if="song.registeredBy">
            <span class="producer-label">등록자</span>
            <span class="producer-name">{{ song.registeredBy.username }}</span>
          </div>

          <div class="stats-row">
            <div class="stat">
              <div class="stat-num">{{ formatCount(song.playCount ?? 0) }}</div>
              <div class="stat-label">재생</div>
            </div>
            <div class="stat">
              <div class="stat-num">{{ formatCount(song.likeCount ?? 0) }}</div>
              <div class="stat-label">좋아요</div>
            </div>
            <div v-if="song.bpm" class="stat">
              <div class="stat-num">♪ {{ song.bpm }}</div>
              <div class="stat-label">BPM</div>
            </div>
          </div>

          <div class="action-row">
            <button class="btn-like" :class="{ liked }" @click="onLike">
              ♥ {{ liked ? '좋아요 취소' : '좋아요' }}
            </button>
            <button class="btn-playlist" @click="openPlaylistModal">✦ 플레이리스트에 담기</button>
          </div>

          <div v-if="song.tags?.length" class="tag-row">
            <span v-for="tag in song.tags" :key="tag" class="tag">#{{ tag }}</span>
          </div>

          <div v-if="isOwner" class="owner-row">
            <button class="btn-danger" @click="onDeleteSong">곡 삭제</button>
          </div>
        </div>
      </aside>

      <!-- 하단: 댓글 -->
      <section class="comments-section">
        <div class="section-header">
          <div class="section-title">댓글</div>
          <div class="section-title-jp">COMMENTS / コメント</div>
        </div>

        <form
          v-if="authStore.isAuthenticated"
          class="comment-form"
          @submit.prevent="onSubmitComment"
        >
          <textarea
            v-model="commentDraft"
            placeholder="이 곡에 한 줄 남겨주세요... (최대 500자)"
            maxlength="500"
            :disabled="commentSubmitting"
          />
          <div class="form-footer">
            <span class="char-count">{{ commentDraft.length }}/500</span>
            <button
              type="submit"
              class="btn-submit-sm"
              :disabled="!commentDraft.trim() || commentSubmitting"
            >
              {{ commentSubmitting ? '등록 중...' : '댓글 남기기 ✦' }}
            </button>
          </div>
        </form>
        <div v-else class="login-prompt">
          댓글을 남기려면 <RouterLink :to="loginReturnTo" class="link">로그인</RouterLink>이
          필요해요.
        </div>

        <div v-if="commentsError" class="error-banner">댓글을 불러오지 못했습니다.</div>
        <div v-else-if="commentsLoading" class="loading-msg sm">♪ 불러오는 중...</div>
        <ul v-else-if="comments.length" class="comment-list">
          <li v-for="c in comments" :key="c.id" class="comment-item">
            <div class="comment-head">
              <span class="comment-user">{{ c.username }}</span>
              <span class="comment-date">{{ formatDate(c.createdAt) }}</span>
              <button
                v-if="authStore.isAuthenticated"
                class="comment-delete"
                @click="onDeleteComment(c.id)"
                title="삭제 (본인 댓글만)"
              >
                ✕
              </button>
            </div>
            <div class="comment-body">{{ c.content }}</div>
          </li>
        </ul>
        <div v-else class="empty-msg">아직 댓글이 없어요. 첫 댓글을 남겨보세요 ♪</div>

        <div v-if="commentTotalPages > 1" class="pagination">
          <button :disabled="commentPage === 0" @click="changeCommentPage(commentPage - 1)">
            ‹ 이전
          </button>
          <span class="page-indicator">{{ commentPage + 1 }} / {{ commentTotalPages }}</span>
          <button
            :disabled="commentPage >= commentTotalPages - 1"
            @click="changeCommentPage(commentPage + 1)"
          >
            다음 ›
          </button>
        </div>
      </section>
    </div>

    <!-- 플레이리스트 모달 -->
    <div v-if="playlistModalOpen" class="modal-backdrop" @click.self="closePlaylistModal">
      <div class="modal">
        <div class="modal-header">
          <h2>플레이리스트에 담기</h2>
          <button class="modal-close" @click="closePlaylistModal">✕</button>
        </div>
        <div class="modal-body">
          <div v-if="playlistsLoading" class="loading-msg sm">♪ 불러오는 중...</div>
          <div v-else-if="playlists.length === 0" class="empty-msg">
            아직 플레이리스트가 없어요.
            <RouterLink to="/playlists" class="link">만들러 가기</RouterLink>
          </div>
          <ul v-else class="playlist-pick-list">
            <li v-for="pl in playlists" :key="pl.id" class="playlist-pick">
              <div class="playlist-pick-info">
                <div class="playlist-pick-title">{{ pl.title }}</div>
                <div class="playlist-pick-sub">
                  {{ pl.isPublic ? '공개' : '비공개' }} · {{ pl.songCount }}곡
                </div>
              </div>
              <button
                class="btn-add"
                :disabled="addingTo === pl.id"
                @click="onAddToPlaylist(pl.id)"
              >
                {{ addingTo === pl.id ? '...' : '+ 담기' }}
              </button>
            </li>
          </ul>
          <div v-if="playlistFeedback" class="playlist-feedback">{{ playlistFeedback }}</div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted, watch } from 'vue';
import { useRoute, useRouter, RouterLink } from 'vue-router';
import { fetchSong, deleteSong } from '../api/songs.js';
import { toggleLike } from '../api/likes.js';
import { fetchComments, createComment, deleteComment } from '../api/comments.js';
import { fetchMyPlaylists, addSongToPlaylist } from '../api/playlists.js';
import { useAuthStore } from '../stores/auth.js';
import { getCharColorVars } from '../utils/characterColors.js';

const route = useRoute();
const router = useRouter();
const authStore = useAuthStore();

const MOOD_LABELS = {
  BRIGHT: '밝음',
  DARK: '다크',
  EMOTIONAL: '감성',
  ENERGETIC: '신남',
  CALM: '잔잔함',
};

const songId = computed(() => Number(route.params.id));

const song = ref(null);
const loading = ref(true);
const error = ref(false);
const liked = ref(false);

const comments = ref([]);
const commentPage = ref(0);
const commentTotalPages = ref(0);
const commentsLoading = ref(false);
const commentsError = ref(false);
const commentDraft = ref('');
const commentSubmitting = ref(false);

const playlistModalOpen = ref(false);
const playlists = ref([]);
const playlistsLoading = ref(false);
const addingTo = ref(null);
const playlistFeedback = ref('');

const isOwner = computed(() => {
  if (!authStore.isAuthenticated || !song.value?.registeredBy) return false;
  return authStore.currentUserId === song.value.registeredBy.id;
});

const loginReturnTo = computed(() => ({
  name: 'login',
  query: { return: route.fullPath },
}));

const youtubeEmbedUrl = computed(() => {
  const url = song.value?.youtubeUrl;
  if (!url) return null;
  const m = url.match(/(?:youtu\.be\/|youtube\.com\/(?:watch\?v=|embed\/|v\/))([\w-]{11})/);
  return m ? `https://www.youtube.com/embed/${m[1]}` : null;
});

async function loadSong() {
  loading.value = true;
  error.value = false;
  try {
    song.value = await fetchSong(songId.value);
  } catch {
    error.value = true;
  } finally {
    loading.value = false;
  }
}

async function loadComments(page = 0) {
  commentsLoading.value = true;
  commentsError.value = false;
  try {
    const res = await fetchComments(songId.value, { page, size: 10 });
    comments.value = res.content;
    commentPage.value = res.page;
    commentTotalPages.value = res.totalPages;
  } catch {
    commentsError.value = true;
  } finally {
    commentsLoading.value = false;
  }
}

function changeCommentPage(p) {
  if (p < 0 || p >= commentTotalPages.value) return;
  loadComments(p);
}

async function onLike() {
  if (!authStore.isAuthenticated) {
    router.push(loginReturnTo.value);
    return;
  }
  try {
    const result = await toggleLike(songId.value);
    liked.value = result.liked;
    if (song.value) song.value.likeCount = result.likeCount;
  } catch {
    // 무시
  }
}

async function onSubmitComment() {
  const content = commentDraft.value.trim();
  if (!content || commentSubmitting.value) return;
  commentSubmitting.value = true;
  try {
    const created = await createComment(songId.value, { content });
    commentDraft.value = '';
    if (commentPage.value === 0) {
      comments.value.unshift(created);
      if (comments.value.length > 10) comments.value.pop();
    } else {
      await loadComments(0);
    }
  } catch {
    // 서버 에러는 조용히 무시 (인증 만료 시 인터셉터가 처리)
  } finally {
    commentSubmitting.value = false;
  }
}

async function onDeleteComment(commentId) {
  if (!confirm('댓글을 삭제할까요?')) return;
  try {
    await deleteComment(commentId);
    comments.value = comments.value.filter((c) => c.id !== commentId);
  } catch (err) {
    if (err.response?.status === 403) {
      alert('본인이 작성한 댓글만 삭제할 수 있어요.');
    }
  }
}

async function onDeleteSong() {
  if (!confirm('이 곡을 정말 삭제할까요? 되돌릴 수 없어요.')) return;
  try {
    await deleteSong(songId.value);
    router.push('/');
  } catch {
    alert('삭제하지 못했습니다.');
  }
}

async function openPlaylistModal() {
  if (!authStore.isAuthenticated) {
    router.push(loginReturnTo.value);
    return;
  }
  playlistModalOpen.value = true;
  playlistFeedback.value = '';
  playlistsLoading.value = true;
  try {
    playlists.value = await fetchMyPlaylists();
  } catch {
    playlistFeedback.value = '플레이리스트를 불러오지 못했어요.';
  } finally {
    playlistsLoading.value = false;
  }
}

function closePlaylistModal() {
  playlistModalOpen.value = false;
}

async function onAddToPlaylist(playlistId) {
  addingTo.value = playlistId;
  playlistFeedback.value = '';
  try {
    await addSongToPlaylist(playlistId, songId.value);
    playlistFeedback.value = '✦ 담겼어요!';
    const target = playlists.value.find((pl) => pl.id === playlistId);
    if (target) target.songCount += 1;
  } catch (err) {
    if (err.response?.status === 409) {
      playlistFeedback.value = '이미 담겨있는 곡이에요.';
    } else {
      playlistFeedback.value = '담는 데 실패했어요.';
    }
  } finally {
    addingTo.value = null;
  }
}

function formatCount(n) {
  if (n >= 1000) return (n / 1000).toFixed(1) + 'k';
  return String(n);
}

function formatDate(iso) {
  if (!iso) return '';
  const d = new Date(iso);
  const yy = d.getFullYear();
  const mm = String(d.getMonth() + 1).padStart(2, '0');
  const dd = String(d.getDate()).padStart(2, '0');
  return `${yy}.${mm}.${dd}`;
}

watch(songId, (id) => {
  if (Number.isFinite(id)) {
    loadSong();
    loadComments(0);
  }
});

onMounted(() => {
  loadSong();
  loadComments(0);
});
</script>

<style scoped>
.detail-page {
  max-width: 1200px;
  margin: 0 auto;
  padding: 36px 32px 80px;
}

.detail-grid {
  display: grid;
  grid-template-columns: minmax(0, 1.5fr) minmax(280px, 1fr);
  grid-template-areas:
    'player meta'
    'comments comments';
  gap: 28px;
}

.player-section {
  grid-area: player;
}
.meta-section {
  grid-area: meta;
}
.comments-section {
  grid-area: comments;
  margin-top: 16px;
}

@media (max-width: 900px) {
  .detail-grid {
    grid-template-columns: 1fr;
    grid-template-areas: 'player' 'meta' 'comments';
  }
}

.player {
  position: relative;
  width: 100%;
  aspect-ratio: 16/9;
  background: #000;
  border-radius: var(--radius-lg);
  overflow: hidden;
  box-shadow: var(--shadow-md);
  border: 1.5px solid var(--border);
}
.player iframe {
  position: absolute;
  inset: 0;
  width: 100%;
  height: 100%;
}
.player-empty {
  width: 100%;
  aspect-ratio: 16/9;
  display: grid;
  place-items: center;
  background: var(--surface2);
  border-radius: var(--radius-lg);
  border: 1.5px dashed var(--border);
}
.player-empty-text {
  font-family: var(--font-display);
  font-size: 22px;
  color: var(--text3);
}

.external-link-row {
  margin-top: 14px;
}
.external-link {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  font-size: 12px;
  font-weight: 800;
  color: var(--lav-dk);
  background: var(--lav-lt);
  border: 1.5px solid var(--lav);
  padding: 6px 14px;
  border-radius: 99px;
  text-decoration: none;
  transition: all 0.2s;
}
.external-link:hover {
  background: var(--lav);
  color: white;
  transform: translateY(-1px);
}

.meta-card {
  background: var(--surface);
  border-radius: var(--radius-lg);
  border: 1.5px solid var(--border);
  box-shadow: var(--shadow-sm);
  padding: 24px;
  position: relative;
  overflow: hidden;
}
.meta-card::before {
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

.meta-eyebrow {
  font-family: var(--font-jp);
  font-size: 11px;
  font-weight: 800;
  color: var(--lav-dk);
  letter-spacing: 0.12em;
  margin-bottom: 8px;
  text-transform: uppercase;
}
.meta-title {
  font-family: var(--font-jp);
  font-size: 22px;
  font-weight: 900;
  color: var(--text);
  line-height: 1.3;
  margin: 0 0 16px;
  text-shadow:
    1px 1px 0 var(--surface),
    2px 2px 0 var(--pink-lt);
}

.char-row {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  margin-bottom: 14px;
}
.char-badge {
  font-size: 11px;
  font-weight: 800;
  padding: 4px 10px;
  border-radius: 99px;
  border: 1.5px solid var(--c);
  color: var(--c-dk);
  background: var(--c-lt);
}

.producer-row {
  display: flex;
  align-items: center;
  gap: 10px;
  font-size: 12px;
  margin-bottom: 14px;
  padding: 8px 12px;
  background: var(--bg2);
  border-radius: var(--radius-sm);
}
.producer-label {
  font-weight: 700;
  color: var(--text3);
  letter-spacing: 0.1em;
  font-size: 10px;
  text-transform: uppercase;
}
.producer-name {
  font-weight: 800;
  color: var(--text);
}

.stats-row {
  display: flex;
  gap: 12px;
  margin: 16px 0 18px;
}
.stat {
  flex: 1;
  text-align: center;
  padding: 10px 8px;
  background: var(--surface2);
  border-radius: var(--radius-sm);
  border: 1.5px solid var(--border);
}
.stat-num {
  font-family: var(--font-display);
  font-size: 22px;
  color: var(--miku-dk);
}
.stat-label {
  font-size: 10px;
  font-weight: 800;
  color: var(--text3);
  letter-spacing: 0.1em;
  text-transform: uppercase;
  margin-top: 2px;
}

.action-row {
  display: flex;
  gap: 8px;
  margin-bottom: 14px;
}
.btn-like,
.btn-playlist {
  flex: 1;
  font-size: 13px;
  font-weight: 800;
  padding: 10px 14px;
  border-radius: 99px;
  cursor: pointer;
  transition: all 0.2s;
  border: 1.5px solid;
}
.btn-like {
  color: var(--pink-dk);
  background: var(--pink-lt);
  border-color: var(--pink);
}
.btn-like:hover {
  background: var(--pink);
  color: white;
  transform: scale(1.03) rotate(-2deg);
}
.btn-like.liked {
  background: var(--pink);
  color: white;
}
.btn-playlist {
  color: var(--lav-dk);
  background: var(--lav-lt);
  border-color: var(--lav);
}
.btn-playlist:hover {
  background: var(--lav);
  color: white;
  transform: translateY(-2px);
}

.tag-row {
  display: flex;
  gap: 5px;
  flex-wrap: wrap;
  margin-bottom: 12px;
}
.tag {
  font-size: 11px;
  font-weight: 800;
  color: var(--lav-dk);
  background: var(--lav-lt);
  padding: 3px 10px;
  border-radius: 99px;
}

.owner-row {
  margin-top: 16px;
  padding-top: 14px;
  border-top: 1.5px dashed var(--border);
}
.btn-danger {
  font-size: 12px;
  font-weight: 800;
  color: #b91c1c;
  background: var(--coral-lt);
  border: 1.5px solid var(--coral);
  padding: 8px 14px;
  border-radius: 99px;
  cursor: pointer;
  transition: all 0.2s;
}
.btn-danger:hover {
  background: var(--coral);
  color: white;
}

/* ── 댓글 ── */
.section-header {
  display: flex;
  align-items: baseline;
  gap: 14px;
  margin-bottom: 18px;
  flex-wrap: wrap;
}
.section-title {
  font-family: var(--font-display);
  font-size: 26px;
  color: var(--text);
  display: inline-flex;
  align-items: center;
  gap: 10px;
  text-shadow:
    2px 2px 0 var(--surface),
    3px 3px 0 var(--pink-lt);
}
.section-title::before {
  content: '✦';
  color: var(--pink-dk);
  font-size: 18px;
  animation: sparkle 2.4s ease-in-out infinite;
}
.section-title-jp {
  font-family: var(--font-jp);
  font-size: 10px;
  color: var(--lav-dk);
  font-weight: 700;
  letter-spacing: 0.15em;
  background: var(--lav-lt);
  padding: 3px 10px;
  border-radius: 99px;
}

.comment-form {
  background: var(--surface);
  border-radius: var(--radius-md);
  border: 1.5px solid var(--border);
  padding: 14px;
  margin-bottom: 18px;
}
.comment-form textarea {
  width: 100%;
  min-height: 70px;
  padding: 10px 12px;
  border: 1.5px solid var(--border);
  border-radius: var(--radius-sm);
  background: var(--bg2);
  font-family: var(--font-body);
  font-size: 14px;
  font-weight: 600;
  color: var(--text);
  resize: vertical;
  outline: none;
  transition:
    border-color 0.15s,
    background 0.15s,
    box-shadow 0.15s;
}
.comment-form textarea:focus {
  border-color: var(--miku);
  background: var(--surface);
  box-shadow: 0 0 0 4px var(--miku-lt);
}
.form-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-top: 8px;
}
.char-count {
  font-size: 11px;
  font-weight: 700;
  color: var(--text3);
}
.btn-submit-sm {
  font-size: 12px;
  font-weight: 800;
  color: white;
  background: linear-gradient(135deg, var(--miku-dk), var(--lav-dk));
  border: none;
  padding: 8px 18px;
  border-radius: 99px;
  cursor: pointer;
  transition: transform 0.2s;
}
.btn-submit-sm:hover:not(:disabled) {
  transform: translateY(-2px);
}
.btn-submit-sm:disabled {
  background: var(--border);
  color: var(--text3);
  cursor: not-allowed;
}

.login-prompt {
  background: var(--miku-lt);
  border: 1.5px dashed var(--miku);
  border-radius: var(--radius-md);
  padding: 14px 18px;
  font-size: 13px;
  font-weight: 700;
  color: var(--miku-dk);
  text-align: center;
  margin-bottom: 18px;
}

.comment-list {
  list-style: none;
  padding: 0;
  margin: 0;
  display: flex;
  flex-direction: column;
  gap: 10px;
}
.comment-item {
  background: var(--surface);
  border: 1.5px solid var(--border);
  border-radius: var(--radius-md);
  padding: 12px 16px;
}
.comment-head {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 6px;
}
.comment-user {
  font-weight: 800;
  color: var(--text);
  font-size: 13px;
}
.comment-date {
  font-size: 11px;
  color: var(--text3);
  font-weight: 700;
}
.comment-delete {
  margin-left: auto;
  background: none;
  border: none;
  color: var(--text3);
  cursor: pointer;
  font-size: 12px;
  padding: 2px 6px;
  border-radius: 6px;
  transition: all 0.15s;
}
.comment-delete:hover {
  color: var(--coral);
  background: var(--coral-lt);
}
.comment-body {
  font-size: 14px;
  font-weight: 600;
  color: var(--text);
  line-height: 1.5;
  white-space: pre-wrap;
  word-break: break-word;
}

.empty-msg {
  font-family: var(--font-display);
  font-size: 18px;
  color: var(--text3);
  text-align: center;
  padding: 36px 0;
}

.pagination {
  display: flex;
  justify-content: center;
  align-items: center;
  gap: 10px;
  margin-top: 18px;
}
.pagination button {
  font-size: 12px;
  font-weight: 800;
  color: var(--text);
  background: var(--surface);
  border: 1.5px solid var(--border);
  padding: 6px 14px;
  border-radius: 99px;
  cursor: pointer;
  transition: all 0.2s;
}
.pagination button:hover:not(:disabled) {
  border-color: var(--miku);
  color: var(--miku-dk);
}
.pagination button:disabled {
  opacity: 0.4;
  cursor: not-allowed;
}
.page-indicator {
  font-size: 12px;
  font-weight: 800;
  color: var(--text2);
}

/* ── 공통 메시지 ── */
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

.error-banner {
  background: var(--coral-lt);
  color: #b91c1c;
  border: 1.5px solid var(--coral);
  border-radius: var(--radius-sm);
  padding: 12px 16px;
  font-size: 13px;
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
}

.playlist-pick-list {
  list-style: none;
  padding: 0;
  margin: 0;
  display: flex;
  flex-direction: column;
  gap: 8px;
  max-height: 320px;
  overflow-y: auto;
}
.playlist-pick {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px 14px;
  background: var(--surface2);
  border: 1.5px solid var(--border);
  border-radius: var(--radius-md);
}
.playlist-pick-info {
  flex: 1;
  min-width: 0;
}
.playlist-pick-title {
  font-weight: 800;
  font-size: 14px;
  color: var(--text);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
.playlist-pick-sub {
  font-size: 11px;
  color: var(--text3);
  font-weight: 700;
  margin-top: 2px;
}
.btn-add {
  font-size: 12px;
  font-weight: 800;
  color: var(--miku-dk);
  background: var(--miku-lt);
  border: 1.5px solid var(--miku);
  padding: 6px 12px;
  border-radius: 99px;
  cursor: pointer;
  transition: all 0.2s;
}
.btn-add:hover:not(:disabled) {
  background: var(--miku);
  color: white;
}
.btn-add:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}
.playlist-feedback {
  margin-top: 14px;
  text-align: center;
  font-size: 12px;
  font-weight: 800;
  color: var(--lav-dk);
}

@media (max-width: 768px) {
  .detail-page {
    padding: 20px 16px 60px;
  }

  .meta-title {
    font-size: 18px;
  }

  .action-row {
    flex-direction: column;
  }

  .btn-like,
  .btn-playlist {
    flex: unset;
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
