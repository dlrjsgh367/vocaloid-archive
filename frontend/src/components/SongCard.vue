<template>
  <div class="song-card" :style="cardVars">
    <div class="card-thumb" :style="{ background: thumbGradient }">
      <div class="card-thumb-corner">{{ thumbLabel }}</div>
      <div class="card-thumb-corner-r">{{ song.duration ?? '—' }}</div>
      <div class="card-thumb-inner" :class="{ small: thumbText.length > 2 }">{{ thumbText }}</div>
      <div class="card-play">▶</div>
    </div>

    <div class="card-body">
      <div class="card-chars">
        <span
          v-for="char in song.characters"
          :key="char.id"
          class="char-badge"
          :style="charBadgeStyle(char)"
          >{{ char.name }}</span
        >
      </div>

      <div class="card-title">
        {{ song.title }}
        <span v-if="song.producer" class="card-title-sub">{{ song.producer }}</span>
      </div>

      <div class="card-tags">
        <span v-for="tag in song.tags" :key="tag" class="tag">#{{ tag }}</span>
      </div>

      <div class="card-footer">
        <div class="card-meta">
          <span v-if="song.bpm">♪ {{ song.bpm }}</span>
          <span v-if="song.playCount">👁 {{ formatCount(song.playCount) }}</span>
        </div>
        <button class="like-btn" @click.stop="$emit('like', song.id)">
          ♥ {{ formatCount(song.likeCount ?? 0) }}
        </button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue';

const props = defineProps({
  song: { type: Object, required: true },
});

defineEmits(['like']);

const CHAR_COLORS = {
  miku: { c: '#7DDFD4', dk: '#3BBCB0', lt: '#C8F5F0' },
  luka: { c: '#F9A8D4', dk: '#E879B0', lt: '#FDE8F3' },
  ren: { c: '#FDE68A', dk: '#F59E0B', lt: '#FFFBEB' },
  rin: { c: '#FFCA34', dk: '#F59E0B', lt: '#FFFBEB' },
  kaito: { c: '#A0C4D8', dk: '#5A8FB0', lt: '#DAEEF7' },
  meiko: { c: '#D4A5C9', dk: '#A66E96', lt: '#F5E6F1' },
};

const primaryChar = computed(() => props.song.characters?.[0]);

const cardColors = computed(() => {
  const key = primaryChar.value?.colorKey;
  return CHAR_COLORS[key] ?? CHAR_COLORS.miku;
});

const cardVars = computed(() => ({
  '--c': cardColors.value.c,
  '--c-dk': cardColors.value.dk,
  '--c-lt': cardColors.value.lt,
}));

const thumbGradient = computed(() => {
  const { c, dk, lt } = cardColors.value;
  return `linear-gradient(135deg, ${lt} 0%, ${c} 60%, ${dk} 100%)`;
});

const thumbText = computed(() => primaryChar.value?.jpName ?? '♪');

const THUMB_LABELS = ['★ HOT', '♡ FEELS', '⚡ COLLAB', '♪ BALLAD', '✦ PICK'];
const thumbLabel = computed(() => {
  const idx = (props.song.id ?? 0) % THUMB_LABELS.length;
  return THUMB_LABELS[idx];
});

function charBadgeStyle(char) {
  const key = char.colorKey;
  const col = CHAR_COLORS[key] ?? CHAR_COLORS.miku;
  return {
    color: col.dk,
    borderColor: col.c,
    background: col.lt,
  };
}

function formatCount(n) {
  if (n >= 1000) return (n / 1000).toFixed(1) + 'k';
  return String(n);
}
</script>

<style scoped>
.song-card {
  --c: var(--miku);
  --c-dk: var(--miku-dk);
  --c-lt: var(--miku-lt);
  background: var(--surface);
  border-radius: var(--radius-lg);
  overflow: hidden;
  border: 1.5px solid var(--border);
  box-shadow: var(--shadow-sm);
  cursor: pointer;
  transition:
    transform 0.25s,
    box-shadow 0.25s;
  position: relative;
}

.song-card::before {
  content: '';
  position: absolute;
  inset: -2px;
  border-radius: var(--radius-lg);
  background: var(--holo);
  background-size: 300% 100%;
  opacity: 0;
  z-index: -1;
  transition: opacity 0.3s;
  animation: shimmer 4s linear infinite;
  filter: blur(8px);
}

.song-card:hover {
  transform: translateY(-6px) rotate(-0.4deg);
  box-shadow:
    0 16px 40px color-mix(in srgb, var(--c) 55%, transparent),
    0 0 0 1.5px var(--c);
}

.song-card:hover::before {
  opacity: 0.7;
}

.card-thumb {
  width: 100%;
  aspect-ratio: 16/10;
  display: flex;
  align-items: center;
  justify-content: center;
  position: relative;
  overflow: hidden;
  isolation: isolate;
}

.card-thumb::after {
  content: '';
  position: absolute;
  inset: 0;
  background-image:
    radial-gradient(circle at 18% 22%, rgba(255, 255, 255, 0.9) 0, transparent 2px),
    radial-gradient(circle at 78% 30%, rgba(255, 255, 255, 0.7) 0, transparent 2.5px),
    radial-gradient(circle at 42% 70%, rgba(255, 255, 255, 0.8) 0, transparent 2px),
    radial-gradient(circle at 88% 80%, rgba(255, 255, 255, 0.6) 0, transparent 1.5px),
    radial-gradient(circle at 12% 85%, rgba(255, 255, 255, 0.7) 0, transparent 2px);
  pointer-events: none;
  opacity: 0.85;
}

.card-thumb::before {
  content: '';
  position: absolute;
  inset: 0;
  background: linear-gradient(
    115deg,
    transparent 30%,
    rgba(255, 255, 255, 0.45) 50%,
    transparent 70%
  );
  background-size: 250% 100%;
  pointer-events: none;
  mix-blend-mode: overlay;
  opacity: 0;
  transition: opacity 0.3s;
}

.song-card:hover .card-thumb::before {
  opacity: 1;
  animation: shimmer 1.6s linear infinite;
}

.card-thumb-inner {
  position: absolute;
  inset: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  font-family: var(--font-jp);
  font-weight: 900;
  font-size: 96px;
  color: white;
  letter-spacing: -4px;
  opacity: 0.32;
  user-select: none;
}

.card-thumb-inner.small {
  font-size: 72px;
}

.card-thumb-corner {
  position: absolute;
  top: 10px;
  left: 12px;
  font-family: var(--font-jp);
  font-size: 10px;
  font-weight: 700;
  color: rgba(255, 255, 255, 0.85);
  background: rgba(0, 0, 0, 0.18);
  padding: 3px 8px;
  border-radius: 99px;
  backdrop-filter: blur(4px);
  letter-spacing: 0.1em;
  z-index: 2;
}

.card-thumb-corner-r {
  position: absolute;
  top: 10px;
  right: 12px;
  font-size: 10px;
  font-weight: 800;
  color: white;
  background: rgba(255, 255, 255, 0.22);
  border: 1px solid rgba(255, 255, 255, 0.5);
  padding: 3px 8px;
  border-radius: 99px;
  backdrop-filter: blur(6px);
  letter-spacing: 0.05em;
  z-index: 2;
}

.card-play {
  width: 48px;
  height: 48px;
  background: rgba(255, 255, 255, 0.95);
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 16px;
  color: var(--c-dk);
  box-shadow:
    0 4px 16px rgba(0, 0, 0, 0.18),
    inset 0 0 0 2px rgba(255, 255, 255, 1);
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  z-index: 3;
  transition:
    transform 0.2s,
    box-shadow 0.2s;
  padding-left: 3px;
}

.song-card:hover .card-play {
  transform: translate(-50%, -50%) scale(1.15) rotate(-8deg);
  box-shadow: 0 6px 24px color-mix(in srgb, var(--c) 70%, transparent);
}

.card-body {
  padding: 14px 16px;
  position: relative;
}

.card-body::before {
  content: '';
  position: absolute;
  top: -1px;
  left: 12%;
  right: 12%;
  height: 2px;
  background: linear-gradient(90deg, transparent, var(--c), transparent);
  opacity: 0.6;
}

.card-chars {
  display: flex;
  gap: 5px;
  margin-bottom: 10px;
  flex-wrap: wrap;
}

.char-badge {
  font-size: 11px;
  font-weight: 800;
  padding: 3px 9px;
  border-radius: 99px;
  border: 1.5px solid;
}

.card-title {
  font-family: var(--font-jp);
  font-size: 17px;
  font-weight: 900;
  color: var(--text);
  margin-bottom: 8px;
  line-height: 1.35;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.card-title-sub {
  display: block;
  font-family: var(--font-body);
  font-size: 11px;
  color: var(--text3);
  font-weight: 700;
  margin-top: 2px;
  letter-spacing: 0.04em;
}

.card-tags {
  display: flex;
  gap: 4px;
  flex-wrap: wrap;
  margin-bottom: 12px;
}

.tag {
  font-size: 11px;
  font-weight: 800;
  color: var(--lav-dk);
  background: var(--lav-lt);
  padding: 2px 9px;
  border-radius: 99px;
  transition: all 0.15s;
  white-space: nowrap;
}

.tag:hover {
  background: var(--lav);
  color: white;
}

.card-footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding-top: 10px;
  border-top: 1.5px dashed var(--border);
}

.card-meta {
  font-size: 11px;
  color: var(--text3);
  font-weight: 700;
  display: flex;
  align-items: center;
  gap: 10px;
}

.like-btn {
  font-size: 12px;
  font-weight: 800;
  color: var(--pink-dk);
  background: var(--pink-lt);
  border: 1.5px solid var(--pink);
  padding: 5px 12px;
  border-radius: 99px;
  cursor: pointer;
  display: flex;
  align-items: center;
  gap: 4px;
  transition: all 0.2s;
}

.like-btn:hover {
  background: var(--pink);
  color: white;
  transform: scale(1.08) rotate(-3deg);
  box-shadow: 0 4px 12px rgba(232, 121, 176, 0.5);
}

.like-btn:active {
  transform: scale(0.95);
}
</style>
