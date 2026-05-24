<template>
  <div ref="root" class="bs" :class="{ open }">
    <button
      type="button"
      class="bs-trigger"
      role="combobox"
      :aria-expanded="open"
      aria-haspopup="listbox"
      :aria-label="ariaLabel"
      @click="toggle"
      @keydown="onTriggerKey"
    >
      <span class="bs-value" :class="{ placeholder: !selected }">
        {{ selected ? selected.label : placeholder }}
      </span>
      <span class="bs-arrow" aria-hidden="true">▾</span>
    </button>

    <Transition name="bs-pop">
      <ul v-if="open" class="bs-menu" role="listbox" :aria-activedescendant="activeId">
        <li
          v-for="(opt, i) in options"
          :id="`${uid}-opt-${i}`"
          :key="opt.value"
          class="bs-option"
          :class="{ selected: opt.value === modelValue, active: i === activeIndex }"
          role="option"
          :aria-selected="opt.value === modelValue"
          @click="select(opt)"
          @mouseenter="activeIndex = i"
        >
          <span class="bs-check" aria-hidden="true">♪</span>
          <span class="bs-label">{{ opt.label }}</span>
        </li>
      </ul>
    </Transition>
  </div>
</template>

<script setup>
import { ref, computed, watch, onMounted, onBeforeUnmount, nextTick } from 'vue';

const props = defineProps({
  // 문자열·숫자 값 모두 허용 (선택 없음일 때는 null)
  modelValue: { default: null },
  // [{ value, label }]
  options: { type: Array, required: true },
  placeholder: { type: String, default: '선택' },
  ariaLabel: { type: String, default: '선택' },
});

const emit = defineEmits(['update:modelValue']);

const root = ref(null);
const open = ref(false);
const activeIndex = ref(-1);
const uid = `bs-${Math.random().toString(36).slice(2, 8)}`;

const selected = computed(() => props.options.find((o) => o.value === props.modelValue) ?? null);
const activeId = computed(() => (activeIndex.value >= 0 ? `${uid}-opt-${activeIndex.value}` : undefined));

function openMenu() {
  open.value = true;
  // 현재 선택된 항목을 활성으로 시작 (없으면 첫 항목)
  const cur = props.options.findIndex((o) => o.value === props.modelValue);
  activeIndex.value = cur >= 0 ? cur : 0;
}

function closeMenu() {
  open.value = false;
  activeIndex.value = -1;
}

function toggle() {
  open.value ? closeMenu() : openMenu();
}

function select(opt) {
  if (opt.value !== props.modelValue) emit('update:modelValue', opt.value);
  closeMenu();
}

function onTriggerKey(e) {
  if (['ArrowDown', 'ArrowUp', 'Enter', ' '].includes(e.key)) {
    e.preventDefault();
    if (!open.value) {
      openMenu();
      return;
    }
  }
  if (!open.value) return;

  switch (e.key) {
    case 'ArrowDown':
      activeIndex.value = (activeIndex.value + 1) % props.options.length;
      break;
    case 'ArrowUp':
      activeIndex.value = (activeIndex.value - 1 + props.options.length) % props.options.length;
      break;
    case 'Enter':
    case ' ':
      if (activeIndex.value >= 0) select(props.options[activeIndex.value]);
      break;
    case 'Escape':
      e.preventDefault();
      closeMenu();
      break;
    case 'Tab':
      closeMenu();
      break;
  }
}

function onClickOutside(e) {
  if (open.value && root.value && !root.value.contains(e.target)) closeMenu();
}

// 활성 항목이 보이도록 스크롤
watch(activeIndex, async (i) => {
  if (i < 0 || !open.value) return;
  await nextTick();
  root.value?.querySelector(`#${uid}-opt-${i}`)?.scrollIntoView({ block: 'nearest' });
});

onMounted(() => document.addEventListener('click', onClickOutside));
onBeforeUnmount(() => document.removeEventListener('click', onClickOutside));
</script>

<style scoped>
.bs {
  position: relative;
  display: inline-block;
}

.bs-trigger {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  font-family: var(--font-body);
  font-size: 13px;
  font-weight: 800;
  color: var(--text);
  background: var(--surface);
  border: 1.5px solid var(--border);
  border-radius: 99px;
  padding: 7px 14px;
  cursor: pointer;
  outline: none;
  transition: all 0.2s;
  white-space: nowrap;
}
.bs-trigger:hover {
  border-color: var(--miku);
}
.bs.open .bs-trigger,
.bs-trigger:focus-visible {
  border-color: var(--miku);
  box-shadow: 0 0 0 4px var(--miku-lt);
}

.bs-value.placeholder {
  color: var(--text3);
}

.bs-arrow {
  font-size: 11px;
  color: var(--lav-dk);
  transition: transform 0.22s ease;
}
.bs.open .bs-arrow {
  transform: rotate(180deg);
}

.bs-menu {
  position: absolute;
  top: calc(100% + 8px);
  right: 0;
  min-width: 100%;
  list-style: none;
  margin: 0;
  padding: 6px;
  background: var(--surface);
  border: 1.5px solid var(--border);
  border-radius: var(--radius-md);
  box-shadow: var(--shadow-lg);
  z-index: 50;
  max-height: 280px;
  overflow: hidden auto;
}
/* 상단 데코 무지개 바 */
.bs-menu::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  height: 3px;
  background: repeating-linear-gradient(
    90deg,
    var(--miku) 0 16px,
    var(--pink) 16px 32px,
    var(--lav) 32px 48px,
    var(--yellow) 48px 64px
  );
  border-radius: var(--radius-md) var(--radius-md) 0 0;
}

.bs-option {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 9px 12px;
  margin-top: 2px;
  border-radius: var(--radius-sm);
  font-size: 13px;
  font-weight: 800;
  color: var(--text2);
  cursor: pointer;
  transition:
    background 0.12s,
    color 0.12s;
}
.bs-option:first-of-type {
  margin-top: 4px;
}
.bs-option.active {
  background: var(--miku-lt);
  color: var(--miku-dk);
}
.bs-option.selected {
  color: var(--miku-dk);
}
.bs-option.selected.active {
  background: var(--miku-lt);
}

.bs-check {
  font-size: 11px;
  color: var(--pink-dk);
  opacity: 0;
  transition: opacity 0.12s;
}
.bs-option.selected .bs-check {
  opacity: 1;
}
.bs-label {
  flex: 1;
}

/* ── 메뉴 등장/퇴장 ── */
.bs-pop-enter-active {
  transition:
    opacity 0.18s ease,
    transform 0.18s cubic-bezier(0.34, 1.56, 0.64, 1);
  transform-origin: top right;
}
.bs-pop-leave-active {
  transition:
    opacity 0.12s ease,
    transform 0.12s ease;
  transform-origin: top right;
}
.bs-pop-enter-from,
.bs-pop-leave-to {
  opacity: 0;
  transform: scale(0.92) translateY(-6px);
}
</style>
