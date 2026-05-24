<template>
  <div class="create-page">
    <div class="create-card">
      <div class="card-header">
        <div class="eyebrow">♪ NEW SONG · 신곡 등록</div>
        <h1>새로운 <span class="gradient-word">보카로</span> 곡 등록</h1>
        <p>유튜브 또는 니코니코 영상 URL과 캐릭터, 분위기를 알려주세요.</p>
      </div>

      <div v-if="serverError" class="error-banner">{{ serverError }}</div>

      <form class="form" @submit.prevent="onSubmit" novalidate>
        <!-- 제목 -->
        <div class="field" :class="{ error: errors.title }">
          <input
            id="title"
            v-model="form.title"
            type="text"
            placeholder=" "
            maxlength="200"
            @input="errors.title = ''"
          />
          <label for="title">곡 제목 *</label>
          <div class="field-hint">{{ errors.title }}</div>
        </div>

        <!-- YouTube URL -->
        <div class="field" :class="{ error: errors.youtubeUrl }">
          <input
            id="ytUrl"
            v-model="form.youtubeUrl"
            type="url"
            placeholder=" "
            maxlength="500"
            @input="errors.youtubeUrl = ''"
          />
          <label for="ytUrl">YouTube URL</label>
          <div class="field-hint">{{ errors.youtubeUrl || 'youtube.com 또는 youtu.be' }}</div>
        </div>

        <!-- Niconico URL -->
        <div class="field" :class="{ error: errors.niconicoUrl }">
          <input
            id="ncUrl"
            v-model="form.niconicoUrl"
            type="url"
            placeholder=" "
            maxlength="500"
            @input="errors.niconicoUrl = ''"
          />
          <label for="ncUrl">Niconico URL</label>
          <div class="field-hint">
            {{ errors.niconicoUrl || 'nicovideo.jp 또는 nico.ms (선택)' }}
          </div>
        </div>

        <!-- BPM -->
        <div class="field-row">
          <div class="field" :class="{ error: errors.bpm }">
            <input
              id="bpm"
              v-model.number="form.bpm"
              type="number"
              min="40"
              max="300"
              placeholder=" "
              @input="errors.bpm = ''"
            />
            <label for="bpm">BPM</label>
            <div class="field-hint">{{ errors.bpm || '40 ~ 300' }}</div>
          </div>
        </div>

        <!-- Mood -->
        <div class="block">
          <div class="block-title">분위기 *</div>
          <div class="chip-row">
            <button
              v-for="m in MOODS"
              :key="m.value"
              type="button"
              class="chip"
              :class="{ active: form.mood === m.value }"
              @click="
                form.mood = m.value;
                errors.mood = '';
              "
            >
              {{ m.label }}
            </button>
          </div>
          <div v-if="errors.mood" class="block-hint error">{{ errors.mood }}</div>
        </div>

        <!-- Characters -->
        <div class="block">
          <div class="block-title">
            캐릭터 * <span class="block-sub">(1개 이상, 최대 10개)</span>
          </div>
          <div v-if="charactersLoading" class="loading-msg sm">♪ 불러오는 중...</div>
          <div v-else class="chip-row">
            <button
              v-for="char in characters"
              :key="char.id"
              type="button"
              class="char-chip"
              :class="{ active: form.characterIds.includes(char.id) }"
              :style="getCharColorVars(char.colorHex)"
              @click="toggleCharacter(char.id)"
            >
              {{ char.name }}
            </button>
          </div>
          <div v-if="errors.characterIds" class="block-hint error">{{ errors.characterIds }}</div>
        </div>

        <!-- Tags -->
        <div class="block">
          <div class="block-title">
            태그 <span class="block-sub">(쉼표 또는 Enter로 추가, 최대 10개)</span>
          </div>
          <div class="tag-input-wrap">
            <span v-for="tag in form.tagNames" :key="tag" class="tag-chip">
              #{{ tag }}
              <button type="button" class="tag-remove" @click="removeTag(tag)">×</button>
            </span>
            <input
              v-model="tagDraft"
              type="text"
              :placeholder="form.tagNames.length === 0 ? '예: 록, 발라드, jpop' : ''"
              class="tag-input"
              @keydown.enter.prevent="commitTag"
              @keydown.,.prevent="commitTag"
              @blur="commitTag"
            />
          </div>
          <div v-if="errors.tagNames" class="block-hint error">{{ errors.tagNames }}</div>
        </div>

        <div class="actions">
          <button type="button" class="btn-cancel" @click="$router.back()">취소</button>
          <button type="submit" class="btn-submit" :disabled="submitting">
            {{ submitting ? '등록 중...' : '✦ 곡 등록하기' }}
          </button>
        </div>
      </form>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue';
import { useRouter } from 'vue-router';
import { fetchCharacters } from '../api/characters.js';
import { createSong } from '../api/songs.js';
import { getCharColorVars } from '../utils/characterColors.js';

const router = useRouter();

const MOODS = [
  { value: 'BRIGHT', label: '밝음' },
  { value: 'EMOTIONAL', label: '감성' },
  { value: 'DARK', label: '다크' },
  { value: 'ENERGETIC', label: '신남' },
  { value: 'CALM', label: '잔잔함' },
];

const form = reactive({
  title: '',
  youtubeUrl: '',
  niconicoUrl: '',
  bpm: null,
  mood: '',
  characterIds: [],
  tagNames: [],
});

const errors = reactive({
  title: '',
  youtubeUrl: '',
  niconicoUrl: '',
  bpm: '',
  mood: '',
  characterIds: '',
  tagNames: '',
});

const tagDraft = ref('');
const characters = ref([]);
const charactersLoading = ref(false);
const submitting = ref(false);
const serverError = ref('');

onMounted(async () => {
  charactersLoading.value = true;
  try {
    characters.value = await fetchCharacters();
  } catch {
    serverError.value = '캐릭터 목록을 불러오지 못했어요. 새로고침해 주세요.';
  } finally {
    charactersLoading.value = false;
  }
});

function toggleCharacter(id) {
  errors.characterIds = '';
  const idx = form.characterIds.indexOf(id);
  if (idx >= 0) form.characterIds.splice(idx, 1);
  else if (form.characterIds.length < 10) form.characterIds.push(id);
}

function commitTag() {
  errors.tagNames = '';
  const raw = tagDraft.value.trim().toLowerCase();
  if (!raw) return;
  if (raw.length > 30) {
    errors.tagNames = '태그는 30자 이하여야 해요.';
    return;
  }
  if (form.tagNames.length >= 10) {
    errors.tagNames = '태그는 최대 10개까지 가능해요.';
    tagDraft.value = '';
    return;
  }
  if (form.tagNames.includes(raw)) {
    tagDraft.value = '';
    return;
  }
  form.tagNames.push(raw);
  tagDraft.value = '';
}

function removeTag(tag) {
  form.tagNames = form.tagNames.filter((t) => t !== tag);
}

function validate() {
  let ok = true;
  errors.title = errors.youtubeUrl = errors.niconicoUrl = errors.bpm = '';
  errors.mood = errors.characterIds = errors.tagNames = '';

  if (!form.title.trim()) {
    errors.title = '제목을 입력해주세요.';
    ok = false;
  } else if (form.title.length > 200) {
    errors.title = '제목은 200자 이하여야 해요.';
    ok = false;
  }

  if (
    form.youtubeUrl &&
    !/^(https?:\/\/)?(www\.)?(youtube\.com|youtu\.be).*/i.test(form.youtubeUrl)
  ) {
    errors.youtubeUrl = 'youtube.com 또는 youtu.be URL이어야 해요.';
    ok = false;
  }
  if (
    form.niconicoUrl &&
    !/^(https?:\/\/)?(www\.)?(nicovideo\.jp|nico\.ms).*/i.test(form.niconicoUrl)
  ) {
    errors.niconicoUrl = 'nicovideo.jp 또는 nico.ms URL이어야 해요.';
    ok = false;
  }
  if (form.bpm != null && form.bpm !== '' && (form.bpm < 40 || form.bpm > 300)) {
    errors.bpm = 'BPM은 40 ~ 300이어야 해요.';
    ok = false;
  }
  if (!form.mood) {
    errors.mood = '분위기를 선택해주세요.';
    ok = false;
  }
  if (form.characterIds.length === 0) {
    errors.characterIds = '캐릭터를 1개 이상 선택해주세요.';
    ok = false;
  }
  if (form.characterIds.length > 10) {
    errors.characterIds = '캐릭터는 최대 10개까지 가능해요.';
    ok = false;
  }

  return ok;
}

async function onSubmit() {
  serverError.value = '';
  commitTag();
  if (!validate()) return;

  submitting.value = true;
  const body = {
    title: form.title.trim(),
    youtubeUrl: form.youtubeUrl.trim() || null,
    niconicoUrl: form.niconicoUrl.trim() || null,
    bpm: form.bpm || null,
    mood: form.mood,
    characterIds: form.characterIds,
    tagNames: form.tagNames,
  };

  try {
    const created = await createSong(body);
    router.push({ name: 'song-detail', params: { id: created.id } });
  } catch (err) {
    const e = err.response?.data?.error;
    const details = e?.details;
    if (details && typeof details === 'object') {
      for (const [k, v] of Object.entries(details)) {
        if (k in errors) errors[k] = v;
      }
      serverError.value = '입력값을 확인해주세요.';
    } else {
      serverError.value = e?.message ?? '곡 등록에 실패했어요.';
    }
  } finally {
    submitting.value = false;
  }
}
</script>

<style scoped>
.create-page {
  min-height: calc(100vh - 68px);
  display: flex;
  justify-content: center;
  padding: 36px 16px 80px;
}
.create-card {
  width: 100%;
  max-width: 640px;
  background: var(--surface);
  border-radius: var(--radius-xl);
  border: 1.5px solid var(--border);
  box-shadow: var(--shadow-lg);
  padding: 40px 40px 32px;
  position: relative;
  overflow: hidden;
}
.create-card::before {
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

.card-header {
  margin-bottom: 28px;
}
.eyebrow {
  font-family: var(--font-jp);
  font-size: 11px;
  font-weight: 800;
  color: var(--lav-dk);
  letter-spacing: 0.12em;
  text-transform: uppercase;
  margin-bottom: 10px;
}
h1 {
  font-family: var(--font-display);
  font-size: 32px;
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
}
.card-header p {
  font-size: 14px;
  color: var(--text2);
  margin: 0;
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

.form {
  display: flex;
  flex-direction: column;
  gap: 18px;
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
  color: var(--text3);
  margin-top: 5px;
  padding-left: 2px;
  min-height: 15px;
}
.field.error .field-hint {
  color: #b91c1c;
}

.field-row {
  display: flex;
  gap: 14px;
}
.field-row .field {
  flex: 1;
}

.block {
  display: flex;
  flex-direction: column;
  gap: 8px;
}
.block-title {
  font-size: 13px;
  font-weight: 800;
  color: var(--text);
  display: flex;
  align-items: baseline;
  gap: 8px;
}
.block-sub {
  font-size: 11px;
  font-weight: 600;
  color: var(--text3);
  letter-spacing: 0;
}
.block-hint {
  font-size: 11px;
  font-weight: 700;
}
.block-hint.error {
  color: #b91c1c;
}

.chip-row {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}
.chip {
  font-size: 12px;
  font-weight: 800;
  color: var(--text2);
  background: var(--surface);
  border: 1.5px solid var(--border);
  padding: 8px 16px;
  border-radius: 99px;
  cursor: pointer;
  transition: all 0.2s;
}
.chip:hover {
  border-color: var(--miku);
  color: var(--miku-dk);
}
.chip.active {
  background: linear-gradient(135deg, var(--miku-dk), var(--lav-dk));
  border-color: transparent;
  color: white;
  transform: scale(1.04);
}

.char-chip {
  font-size: 12px;
  font-weight: 800;
  padding: 8px 14px;
  border-radius: 99px;
  border: 1.5px solid var(--c);
  background: var(--c-lt);
  color: var(--c-dk);
  cursor: pointer;
  transition: all 0.2s;
}
.char-chip:hover {
  transform: translateY(-1px);
}
.char-chip.active {
  background: var(--c-dk);
  border-color: var(--c-dk);
  color: white;
}

.tag-input-wrap {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  align-items: center;
  padding: 10px 12px;
  background: var(--bg2);
  border: 1.5px solid var(--border);
  border-radius: var(--radius-sm);
  min-height: 50px;
}
.tag-input-wrap:focus-within {
  border-color: var(--miku);
  background: var(--surface);
  box-shadow: 0 0 0 4px var(--miku-lt);
}
.tag-chip {
  font-size: 12px;
  font-weight: 800;
  color: var(--lav-dk);
  background: var(--lav-lt);
  padding: 4px 10px;
  border-radius: 99px;
  display: inline-flex;
  align-items: center;
  gap: 4px;
}
.tag-remove {
  background: none;
  border: none;
  color: var(--lav-dk);
  cursor: pointer;
  font-size: 14px;
  line-height: 1;
  padding: 0;
}
.tag-remove:hover {
  color: var(--coral);
}
.tag-input {
  flex: 1;
  min-width: 120px;
  border: none;
  outline: none;
  background: transparent;
  font-family: var(--font-body);
  font-size: 14px;
  font-weight: 600;
  color: var(--text);
}

.actions {
  display: flex;
  gap: 10px;
  margin-top: 8px;
}
.btn-cancel {
  flex: 0 0 auto;
  min-height: 52px;
  padding: 15px 22px;
  border: 1.5px solid var(--border);
  background: var(--surface);
  border-radius: var(--radius-md);
  font-family: var(--font-body);
  font-size: 14px;
  font-weight: 800;
  color: var(--text2);
  cursor: pointer;
  transition: all 0.2s;
}
.btn-cancel:hover {
  border-color: var(--text3);
  color: var(--text);
}
.btn-submit {
  flex: 1;
  min-height: 52px;
  padding: 15px 20px;
  border: none;
  background: linear-gradient(135deg, var(--miku-dk) 0%, var(--lav-dk) 100%);
  color: white;
  border-radius: var(--radius-md);
  font-family: var(--font-body);
  font-size: 15px;
  font-weight: 800;
  cursor: pointer;
  transition: all 0.2s;
  box-shadow:
    0 6px 18px rgba(59, 188, 176, 0.35),
    inset 0 1px 0 rgba(255, 255, 255, 0.4);
  position: relative;
  overflow: hidden;
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
.btn-submit:disabled {
  background: var(--border);
  color: var(--text3);
  cursor: not-allowed;
  box-shadow: none;
}
.btn-submit:disabled::before {
  display: none;
}

.loading-msg {
  font-family: var(--font-display);
  font-size: 16px;
  color: var(--text3);
  padding: 8px 0;
}
.loading-msg.sm {
  padding: 4px 0;
}

@media (max-width: 768px) {
  .create-page {
    padding: 20px 12px 60px;
  }

  .create-card {
    padding: 28px 20px 24px;
    border-radius: var(--radius-lg);
  }

  h1 {
    font-size: 24px;
  }

  .field-row {
    flex-direction: column;
  }

  .actions {
    flex-direction: column;
  }

  .btn-cancel {
    order: 2;
  }

  .btn-submit {
    order: 1;
  }
}
</style>
