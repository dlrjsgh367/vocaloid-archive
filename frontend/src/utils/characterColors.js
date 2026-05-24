// 캐릭터 색은 백엔드 CharacterResponse.colorHex가 단일 진실 소스다.
// 이름→클래스 매핑은 시드에 없는 캐릭터(또는 Hermes 직접 INSERT)를 기본색으로
// 떨어뜨려 "전부 초록색" 버그를 만들었으므로, colorHex에서 직접 CSS 변수를 만든다.
// 컴포넌트 CSS는 var(--c) / var(--c-dk) / var(--c-lt)를 참조한다.
const HEX_RE = /^#(?:[0-9a-fA-F]{3}|[0-9a-fA-F]{6})$/;

// colorHex → { '--c', '--c-dk', '--c-lt' } 인라인 스타일 객체.
// 유효한 hex가 없으면 빈 객체를 반환해 CSS의 fallback(중립색)이 적용되게 둔다.
export function getCharColorVars(colorHex) {
  const hex = typeof colorHex === 'string' ? colorHex.trim() : '';
  if (!HEX_RE.test(hex)) return {};
  return {
    '--c': hex,
    // 텍스트·활성 배경용: 흰 글씨가 읽히도록 어두운 톤으로 보정.
    '--c-dk': `color-mix(in srgb, ${hex} 76%, #2d1b4e)`,
    // 옅은 배경용 틴트.
    '--c-lt': `color-mix(in srgb, ${hex} 20%, #fff)`,
  };
}
