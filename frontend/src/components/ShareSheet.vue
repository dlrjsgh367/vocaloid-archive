<template>
  <div class="sheet-backdrop" @click.self="$emit('close')">
    <div class="sheet">
      <div class="sheet-grip"></div>
      <div class="sheet-header">
        <div class="sheet-eyebrow">♪ SHARE · シェア</div>
        <h2 class="sheet-title">플리 <span class="gradient-word">자랑하기</span></h2>
        <button class="sheet-close" @click="$emit('close')" aria-label="닫기">✕</button>
      </div>

      <div class="sheet-url">
        <span class="url-text">{{ shareUrl }}</span>
      </div>

      <div class="sheet-actions">
        <button class="action copy" :class="{ done: copied }" @click="onCopyLink">
          {{ copied ? '✓ 복사됨' : '🔗 링크 복사' }}
        </button>
        <button class="action save" :disabled="saving" @click="onSaveImage">
          {{ saving ? '저장 중...' : '🖼 이미지 저장' }}
        </button>
      </div>

      <div v-if="errorMsg" class="sheet-error">{{ errorMsg }}</div>
      <p class="sheet-hint">친구에게 링크를 보내거나 카드 이미지를 SNS에 올려보세요 ✦</p>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue';
import { cardPngUrl } from '@/api/playlists.js';

const props = defineProps({
  shareUrl: { type: String, required: true },
  code: { type: String, required: true },
});

defineEmits(['close']);

const copied = ref(false);
const saving = ref(false);
const errorMsg = ref('');

let copyTimer;
async function onCopyLink() {
  errorMsg.value = '';
  if (await copyToClipboard(props.shareUrl)) {
    copied.value = true;
    clearTimeout(copyTimer);
    copyTimer = setTimeout(() => (copied.value = false), 2000);
  } else {
    errorMsg.value = '복사에 실패했어요. 링크를 길게 눌러 직접 복사해주세요.';
  }
}

// navigator.clipboard는 보안 컨텍스트(HTTPS/localhost)에서만 동작 → HTTP에선 execCommand 폴백.
async function copyToClipboard(text) {
  try {
    if (window.isSecureContext && navigator.clipboard) {
      await navigator.clipboard.writeText(text);
      return true;
    }
  } catch {
    // 폴백으로 진행
  }
  try {
    const ta = document.createElement('textarea');
    ta.value = text;
    ta.setAttribute('readonly', '');
    ta.style.position = 'fixed';
    ta.style.top = '0';
    ta.style.left = '-9999px';
    document.body.appendChild(ta);
    ta.focus();
    ta.select();
    ta.setSelectionRange(0, text.length); // iOS Safari
    const ok = document.execCommand('copy');
    document.body.removeChild(ta);
    return ok;
  } catch {
    return false;
  }
}

async function onSaveImage() {
  errorMsg.value = '';
  saving.value = true;
  try {
    const res = await fetch(cardPngUrl(props.code));
    if (!res.ok) throw new Error('image fetch failed');
    const blob = await res.blob();
    const objectUrl = URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = objectUrl;
    a.download = `playlist-${props.code}.png`;
    document.body.appendChild(a);
    a.click();
    a.remove();
    URL.revokeObjectURL(objectUrl);
  } catch {
    errorMsg.value = '이미지를 저장하지 못했어요. 잠시 후 다시 시도해주세요.';
  } finally {
    saving.value = false;
  }
}
</script>

<style scoped>
.sheet-backdrop {
  position: fixed;
  inset: 0;
  background: rgba(45, 27, 78, 0.45);
  backdrop-filter: blur(8px);
  display: grid;
  place-items: center;
  z-index: 120;
  padding: 16px;
}

.sheet {
  width: 100%;
  max-width: 420px;
  background: var(--surface);
  border-radius: var(--radius-xl);
  border: 1.5px solid var(--border);
  box-shadow: var(--shadow-lg);
  padding: 22px 22px 24px;
  position: relative;
  overflow: hidden;
}
.sheet::before {
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

.sheet-grip {
  display: none;
  width: 44px;
  height: 5px;
  border-radius: 99px;
  background: var(--border);
  margin: 0 auto 12px;
}

.sheet-header {
  position: relative;
  margin-bottom: 16px;
}
.sheet-eyebrow {
  font-family: var(--font-jp);
  font-size: 10px;
  font-weight: 800;
  color: var(--lav-dk);
  letter-spacing: 0.12em;
  text-transform: uppercase;
}
.sheet-title {
  font-family: var(--font-display);
  font-size: 26px;
  color: var(--text);
  margin: 4px 0 0;
}
.gradient-word {
  background: linear-gradient(90deg, var(--miku-dk), var(--lav-dk), var(--pink-dk), var(--miku-dk));
  background-size: 300% 100%;
  -webkit-background-clip: text;
  background-clip: text;
  -webkit-text-fill-color: transparent;
  animation: shimmer 4s linear infinite;
}
.sheet-close {
  position: absolute;
  top: 0;
  right: 0;
  background: none;
  border: none;
  color: var(--text3);
  font-size: 18px;
  cursor: pointer;
  padding: 4px 8px;
  border-radius: 8px;
}
.sheet-close:hover {
  color: var(--text);
  background: var(--bg2);
}

.sheet-url {
  background: var(--bg2);
  border: 1.5px dashed var(--border);
  border-radius: var(--radius-md);
  padding: 10px 14px;
  margin-bottom: 14px;
}
.url-text {
  font-size: 12px;
  font-weight: 700;
  color: var(--text2);
  word-break: break-all;
  line-height: 1.4;
}

.sheet-actions {
  display: flex;
  gap: 10px;
}
.action {
  flex: 1;
  height: 48px;
  border-radius: var(--radius-md);
  font-family: var(--font-body);
  font-size: 14px;
  font-weight: 800;
  cursor: pointer;
  transition: all 0.2s;
}
.action.copy {
  color: var(--miku-dk);
  background: var(--miku-lt);
  border: 1.5px solid var(--miku);
}
.action.copy:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(59, 188, 176, 0.3);
}
.action.copy.done {
  background: var(--miku);
  color: white;
}
.action.save {
  color: white;
  border: none;
  background: linear-gradient(135deg, var(--pink-dk) 0%, var(--lav-dk) 100%);
}
.action.save:hover:not(:disabled) {
  transform: translateY(-2px);
  box-shadow: 0 4px 14px rgba(232, 121, 176, 0.4);
}
.action.save:disabled {
  background: var(--border);
  color: var(--text3);
  cursor: not-allowed;
}

.sheet-error {
  margin-top: 12px;
  background: var(--coral-lt);
  color: #b91c1c;
  border: 1.5px solid var(--coral);
  border-radius: var(--radius-sm);
  padding: 10px 14px;
  font-size: 12px;
  font-weight: 700;
}

.sheet-hint {
  margin: 14px 0 0;
  font-size: 12px;
  font-weight: 700;
  color: var(--text3);
  text-align: center;
}

@media (max-width: 768px) {
  .sheet-backdrop {
    align-items: flex-end;
    padding: 0;
  }
  .sheet {
    border-radius: var(--radius-xl) var(--radius-xl) 0 0;
    max-width: 100%;
  }
  .sheet-grip {
    display: block;
  }
}
</style>
