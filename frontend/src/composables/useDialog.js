import { reactive, readonly } from 'vue';

// 네이티브 window.confirm / window.alert을 대체하는 전역 다이얼로그 상태.
// AppDialog.vue가 App.vue에 단 한 번 마운트되어 이 상태를 렌더한다.
// 뷰에서는 confirmDialog() / alertDialog()를 import해 Promise로 사용한다.
const state = reactive({
  open: false,
  mode: 'confirm', // 'confirm' | 'alert'
  variant: 'default', // 'default' | 'danger'
  title: '',
  message: '',
  confirmText: '확인',
  cancelText: '취소',
  icon: '',
});

let resolver = null;

function openDialog(opts) {
  // 이미 떠 있던 다이얼로그가 있으면 취소 처리하고 새로 띄운다.
  if (resolver) {
    resolver(false);
    resolver = null;
  }
  Object.assign(state, {
    open: true,
    mode: opts.mode ?? 'confirm',
    variant: opts.variant ?? 'default',
    title: opts.title ?? '',
    message: opts.message ?? '',
    confirmText: opts.confirmText ?? '확인',
    cancelText: opts.cancelText ?? '취소',
    icon: opts.icon ?? (opts.variant === 'danger' ? '🗑️' : '✦'),
  });
  return new Promise((resolve) => {
    resolver = resolve;
  });
}

// AppDialog 호스트가 호출. result=true(확인) / false(취소·닫기)
export function resolveDialog(result) {
  state.open = false;
  if (resolver) {
    resolver(result);
    resolver = null;
  }
}

// 문자열 또는 옵션 객체를 옵션 객체로 정규화.
function normalize(arg) {
  return typeof arg === 'string' ? { message: arg } : arg ?? {};
}

/**
 * 스타일이 적용된 확인 다이얼로그. window.confirm 대체.
 * @returns {Promise<boolean>} 확인 시 true, 취소 시 false
 */
export function confirmDialog(arg) {
  return openDialog({ ...normalize(arg), mode: 'confirm' });
}

/**
 * 스타일이 적용된 알림 다이얼로그. window.alert 대체.
 * @returns {Promise<boolean>} 닫으면 resolve (항상 true)
 */
export function alertDialog(arg) {
  return openDialog({ ...normalize(arg), mode: 'alert' });
}

// 읽기 전용 상태 (AppDialog가 구독)
export const dialogState = readonly(state);
