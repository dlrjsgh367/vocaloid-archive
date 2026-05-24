<template>
  <Transition name="dlg">
    <div
      v-if="dialogState.open"
      class="dlg-backdrop"
      @click.self="onBackdrop"
    >
      <div
        class="dlg"
        :class="dialogState.variant"
        role="alertdialog"
        aria-modal="true"
        :aria-label="dialogState.title || dialogState.message"
      >
        <div class="dlg-icon" aria-hidden="true">{{ dialogState.icon }}</div>

        <h2 v-if="dialogState.title" class="dlg-title">{{ dialogState.title }}</h2>
        <p class="dlg-message">{{ dialogState.message }}</p>

        <div class="dlg-actions">
          <button
            v-if="dialogState.mode === 'confirm'"
            type="button"
            class="dlg-btn cancel"
            @click="resolveDialog(false)"
          >
            {{ dialogState.cancelText }}
          </button>
          <button
            ref="confirmBtn"
            type="button"
            class="dlg-btn confirm"
            @click="resolveDialog(true)"
          >
            {{ dialogState.confirmText }}
          </button>
        </div>
      </div>
    </div>
  </Transition>
</template>

<script setup>
import { ref, watch, nextTick, onMounted, onBeforeUnmount } from 'vue';
import { dialogState, resolveDialog } from '@/composables/useDialog.js';

const confirmBtn = ref(null);

// alert는 닫기(=확인)로, confirm은 취소로 백드롭 처리.
function onBackdrop() {
  resolveDialog(dialogState.mode === 'alert');
}

function onKeydown(e) {
  if (!dialogState.open) return;
  if (e.key === 'Escape') {
    e.preventDefault();
    resolveDialog(dialogState.mode === 'alert');
  } else if (e.key === 'Enter') {
    e.preventDefault();
    resolveDialog(true);
  }
}

// 다이얼로그가 열리면 확인 버튼에 포커스 (Enter/스크린리더 대응)
watch(
  () => dialogState.open,
  async (open) => {
    if (open) {
      await nextTick();
      confirmBtn.value?.focus();
    }
  },
);

onMounted(() => window.addEventListener('keydown', onKeydown));
onBeforeUnmount(() => window.removeEventListener('keydown', onKeydown));
</script>

<style scoped>
.dlg-backdrop {
  position: fixed;
  inset: 0;
  background: rgba(45, 27, 78, 0.45);
  backdrop-filter: blur(8px);
  -webkit-backdrop-filter: blur(8px);
  display: grid;
  place-items: center;
  z-index: 300;
  padding: 16px;
}

.dlg {
  width: 100%;
  max-width: 380px;
  background: var(--surface);
  border-radius: var(--radius-xl);
  border: 1.5px solid var(--border);
  box-shadow: var(--shadow-lg);
  padding: 30px 26px 24px;
  position: relative;
  overflow: hidden;
  text-align: center;
}
/* 상단 무지개 바 — ShareSheet/모달과 동일한 데코 모티프 */
.dlg::before {
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
.dlg.danger::before {
  background: repeating-linear-gradient(
    90deg,
    var(--coral) 0 20px,
    var(--pink) 20px 40px,
    var(--coral) 40px 60px,
    var(--pink-dk) 60px 80px
  );
}

.dlg-icon {
  font-size: 34px;
  line-height: 1;
  margin-bottom: 12px;
  display: inline-block;
  animation: float-y 2.4s ease-in-out infinite;
}
.dlg.danger .dlg-icon {
  animation: wiggle 1.8s ease-in-out infinite;
}

.dlg-title {
  font-family: var(--font-display);
  font-size: 23px;
  color: var(--text);
  margin: 0 0 8px;
}
.dlg.danger .dlg-title {
  color: var(--coral);
}

.dlg-message {
  font-family: var(--font-body);
  font-size: 14px;
  font-weight: 700;
  line-height: 1.55;
  color: var(--text2);
  margin: 0 0 22px;
  white-space: pre-line;
  word-break: keep-all;
}

.dlg-actions {
  display: flex;
  gap: 10px;
}
.dlg-btn {
  flex: 1;
  height: 46px;
  border-radius: var(--radius-md);
  font-family: var(--font-body);
  font-size: 14px;
  font-weight: 800;
  cursor: pointer;
  transition: all 0.2s;
  outline: none;
}
.dlg-btn.cancel {
  flex: 0 0 auto;
  padding: 0 22px;
  border: 1.5px solid var(--border);
  background: var(--surface);
  color: var(--text2);
}
.dlg-btn.cancel:hover {
  border-color: var(--text3);
  color: var(--text);
  background: var(--bg2);
}
.dlg-btn.cancel:focus-visible {
  border-color: var(--text3);
  box-shadow: 0 0 0 4px var(--bg2);
}

.dlg-btn.confirm {
  border: none;
  color: white;
  background: linear-gradient(135deg, var(--miku-dk) 0%, var(--lav-dk) 100%);
  box-shadow: 0 4px 14px rgba(59, 188, 176, 0.35);
}
.dlg-btn.confirm:hover {
  transform: translateY(-2px);
  box-shadow: 0 6px 18px rgba(59, 188, 176, 0.45);
}
.dlg-btn.confirm:focus-visible {
  box-shadow: 0 0 0 4px var(--miku-lt);
}
.dlg.danger .dlg-btn.confirm {
  background: linear-gradient(135deg, var(--coral) 0%, var(--pink-dk) 100%);
  box-shadow: 0 4px 14px rgba(232, 121, 176, 0.4);
}
.dlg.danger .dlg-btn.confirm:hover {
  box-shadow: 0 6px 18px rgba(232, 121, 176, 0.5);
}
.dlg.danger .dlg-btn.confirm:focus-visible {
  box-shadow: 0 0 0 4px var(--coral-lt);
}

/* ── 등장/퇴장 트랜지션 ── */
.dlg-enter-active,
.dlg-leave-active {
  transition: opacity 0.2s ease;
}
.dlg-enter-from,
.dlg-leave-to {
  opacity: 0;
}
.dlg-enter-active .dlg {
  animation: dlg-pop 0.28s cubic-bezier(0.34, 1.56, 0.64, 1);
}
@keyframes dlg-pop {
  0% {
    transform: scale(0.85) translateY(12px);
    opacity: 0;
  }
  100% {
    transform: scale(1) translateY(0);
    opacity: 1;
  }
}

@media (max-width: 768px) {
  .dlg-backdrop {
    align-items: flex-end;
    padding: 0;
  }
  .dlg {
    border-radius: var(--radius-xl) var(--radius-xl) 0 0;
    max-width: 100%;
    padding-bottom: 32px;
  }
  .dlg-enter-active .dlg {
    animation: dlg-slide-up 0.3s cubic-bezier(0.25, 0.8, 0.25, 1);
  }
  @keyframes dlg-slide-up {
    0% {
      transform: translateY(100%);
    }
    100% {
      transform: translateY(0);
    }
  }
}
</style>
