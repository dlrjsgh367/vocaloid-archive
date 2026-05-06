<template>
  <div class="char-strip">
    <div
      v-for="char in characters"
      :key="char.id"
      class="char-pill"
      :style="{ '--c': char.color, '--c-dk': char.colorDk }"
      @click="$emit('select', char.id)"
    >
      <div class="char-avatar" :style="{ background: char.color, color: char.color }">
        {{ char.initial }}
      </div>
      <div>
        <div class="char-name">{{ char.name }}</div>
        <div class="char-count">{{ char.jpName }} · {{ char.songCount }}곡</div>
      </div>
    </div>
  </div>
</template>

<script setup>
defineProps({
  characters: { type: Array, required: true },
});

defineEmits(['select']);
</script>

<style scoped>
.char-strip {
  display: flex;
  gap: 12px;
  margin-bottom: 44px;
  overflow-x: auto;
  padding: 8px 0 12px;
}

.char-strip::-webkit-scrollbar {
  height: 6px;
}
.char-strip::-webkit-scrollbar-thumb {
  background: linear-gradient(90deg, var(--miku), var(--pink), var(--lav));
  border-radius: 99px;
}

.char-pill {
  --c: var(--miku);
  --c-dk: var(--miku-dk);
  display: flex;
  align-items: center;
  gap: 10px;
  background: var(--surface);
  border: 1.5px solid var(--border);
  border-radius: 99px;
  padding: 8px 18px 8px 8px;
  cursor: pointer;
  transition: all 0.2s;
  flex-shrink: 0;
  box-shadow: var(--shadow-sm);
  position: relative;
}

.char-pill:hover {
  transform: translateY(-3px);
  box-shadow: 0 8px 24px color-mix(in srgb, var(--c) 50%, transparent);
  border-color: var(--c);
}

.char-pill::after {
  content: '♡';
  position: absolute;
  top: -4px;
  right: 12px;
  font-size: 11px;
  color: var(--c-dk);
  opacity: 0;
  transition: all 0.25s;
}

.char-pill:hover::after {
  opacity: 1;
  top: -10px;
  animation: float-y 1.4s ease-in-out infinite;
}

.char-avatar {
  width: 36px;
  height: 36px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 16px;
  font-weight: 800;
  color: white !important;
  flex-shrink: 0;
  font-family: var(--font-jp);
  box-shadow:
    inset 0 0 0 2px rgba(255, 255, 255, 0.5),
    0 0 12px color-mix(in srgb, var(--c) 60%, transparent);
}

.char-name {
  font-size: 13px;
  font-weight: 800;
  color: var(--text);
  white-space: nowrap;
}

.char-count {
  font-size: 10px;
  color: var(--text3);
  font-weight: 700;
  letter-spacing: 0.04em;
  white-space: nowrap;
}
</style>
