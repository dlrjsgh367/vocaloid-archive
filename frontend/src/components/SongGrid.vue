<template>
  <div>
    <div class="section-header">
      <div class="section-title">인기 곡</div>
      <div class="section-title-jp">POPULAR / 人気曲</div>
      <div class="section-count">★ {{ total }}</div>
    </div>

    <div class="song-grid">
      <SongCard
        v-for="song in songs"
        :key="song.id"
        :song="song"
        @like="$emit('like', $event)"
        @click="$emit('open', song.id)"
      />
    </div>
  </div>
</template>

<script setup>
import SongCard from './SongCard.vue';

defineProps({
  songs: { type: Array, required: true },
  total: { type: Number, default: 0 },
});

defineEmits(['like', 'open']);
</script>

<style scoped>
.section-header {
  display: flex;
  align-items: baseline;
  gap: 14px;
  margin-bottom: 22px;
  position: relative;
  flex-wrap: wrap;
}

.section-title {
  font-family: var(--font-display);
  font-size: 30px;
  color: var(--text);
  position: relative;
  display: inline-flex;
  align-items: center;
  gap: 10px;
  text-shadow: 2px 2px 0 var(--surface), 3px 3px 0 var(--pink-lt);
  white-space: nowrap;
  flex-shrink: 0;
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
  flex-shrink: 0;
}

.section-count {
  font-family: var(--font-display);
  font-size: 16px;
  color: var(--pink-dk);
  margin-left: auto;
  white-space: nowrap;
  flex-shrink: 0;
}

.song-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(260px, 1fr));
  gap: 22px;
  margin-bottom: 56px;
}
</style>
