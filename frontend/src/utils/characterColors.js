// 캐릭터 이름 → colorKey 매핑.
// colorKey는 tokens.css의 .char-{key} 클래스와 매칭된다.
export const CHAR_KEY_MAP = {
  '하츠네 미쿠': 'miku',
  '메구리네 루카': 'luka',
  '카가미네 렌': 'ren',
  '카가미네 린': 'rin',
  카이토: 'kaito',
  메이코: 'meiko',
};

export const DEFAULT_CHAR_KEY = 'miku';

export function getCharColorKey(name) {
  return CHAR_KEY_MAP[name] ?? DEFAULT_CHAR_KEY;
}

export function getCharColorClass(keyOrChar) {
  const key =
    typeof keyOrChar === 'string'
      ? keyOrChar
      : (keyOrChar?.colorKey ?? getCharColorKey(keyOrChar?.name));
  return `char-${key ?? DEFAULT_CHAR_KEY}`;
}
