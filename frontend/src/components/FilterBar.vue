<template>
  <div class="filter-bar">
    <span class="filter-label">분위기</span>
    <button
      v-for="mood in MOODS"
      :key="mood.value"
      class="chip"
      :class="[mood.cls, { active: modelValue === mood.value }]"
      @click="$emit('update:modelValue', mood.value)"
    >
      {{ mood.label }}
    </button>

    <div class="search-wrap">
      <span class="search-icon">♪</span>
      <input
        class="search-input"
        placeholder="曲を検索 / 곡 검색..."
        :value="search"
        @input="$emit('update:search', $event.target.value)"
      />
    </div>
  </div>
</template>

<script setup>
defineProps({
  modelValue: { type: String, default: 'all' },
  search: { type: String, default: '' },
});

defineEmits(['update:modelValue', 'update:search']);

const MOODS = [
  { value: 'all', label: '전체', cls: '' },
  { value: 'bright', label: '밝음', cls: 'yellow' },
  { value: 'emotional', label: '감성', cls: 'pink' },
  { value: 'dark', label: '다크', cls: 'lav' },
  { value: 'calm', label: '잔잔함', cls: '' },
  { value: 'energetic', label: '신남', cls: '' },
];
</script>

<style scoped>
.filter-bar {
  background: var(--surface);
  border-bottom: 1.5px solid var(--border);
  padding: 20px 32px;
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
  position: relative;
  transition: background 0.3s ease;
}

.filter-bar::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  height: 2px;
  background: linear-gradient(90deg, transparent, var(--pink), var(--miku), transparent);
  opacity: 0.5;
}

.filter-label {
  font-family: var(--font-display);
  font-size: 16px;
  color: var(--lav-dk);
  margin-right: 6px;
  display: flex;
  align-items: center;
  gap: 4px;
}

.filter-label::before {
  content: '★';
  font-size: 12px;
  color: var(--yellow-dk);
}

.chip {
  font-family: var(--font-body);
  font-size: 13px;
  font-weight: 800;
  padding: 7px 16px;
  border-radius: 99px;
  border: 1.5px solid var(--border);
  background: var(--surface2);
  color: var(--text2);
  cursor: pointer;
  transition: all 0.2s ease;
  position: relative;
}

.chip:hover {
  border-color: var(--miku);
  color: var(--miku-dk);
  background: var(--miku-lt);
  transform: translateY(-1px);
}

.chip.active {
  background: var(--miku-dk);
  color: white;
  border-color: var(--miku-dk);
  box-shadow: 0 4px 14px rgba(59, 188, 176, 0.45);
}

.chip.active::before {
  content: '';
  position: absolute;
  inset: -2px;
  border-radius: 99px;
  padding: 2px;
  background: linear-gradient(90deg, var(--miku), var(--pink), var(--lav), var(--miku));
  background-size: 300% 100%;
  -webkit-mask:
    linear-gradient(#000 0 0) content-box,
    linear-gradient(#000 0 0);
  mask:
    linear-gradient(#000 0 0) content-box,
    linear-gradient(#000 0 0);
  -webkit-mask-composite: xor;
  mask-composite: exclude;
  animation: shimmer 2.5s linear infinite;
  pointer-events: none;
}

.chip.pink.active {
  background: var(--pink-dk);
  border-color: var(--pink-dk);
  box-shadow: 0 4px 14px rgba(232, 121, 176, 0.5);
}
.chip.lav.active {
  background: var(--lav-dk);
  border-color: var(--lav-dk);
  box-shadow: 0 4px 14px rgba(139, 92, 246, 0.5);
}
.chip.yellow.active {
  background: var(--yellow-dk);
  border-color: var(--yellow-dk);
  box-shadow: 0 4px 14px rgba(245, 158, 11, 0.45);
}

.search-wrap {
  margin-left: auto;
  position: relative;
}

.search-input {
  font-family: var(--font-body);
  font-size: 13px;
  font-weight: 700;
  color: var(--text);
  background: var(--bg2);
  border: 1.5px solid var(--border);
  border-radius: 99px;
  padding: 8px 16px 8px 36px;
  width: 240px;
  outline: none;
  transition: all 0.25s cubic-bezier(0.4, 0, 0.2, 1);
}

.search-input::placeholder {
  color: var(--text3);
}

.search-input:focus {
  border-color: var(--miku-dk);
  width: 280px;
  background: var(--surface);
  box-shadow: 0 0 16px var(--miku-lt);
}

.search-icon {
  position: absolute;
  left: 14px;
  top: 50%;
  transform: translateY(-50%);
  color: var(--miku-dk);
  font-size: 14px;
  pointer-events: none;
}

@media (max-width: 768px) {
  .filter-bar {
    padding: 14px 16px;
    gap: 8px;
  }

  .search-wrap {
    margin-left: 0;
    width: 100%;
  }

  .search-input {
    width: 100%;
  }

  .search-input:focus {
    width: 100%;
  }
}
</style>
