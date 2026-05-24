<template>
  <div class="hero">
    <div class="cyber-grid"></div>
    <div class="particles" ref="particlesEl"></div>

    <div class="hero-text">
      <div class="hero-badge">보컬로이드 팬들의 비밀기지</div>
      <h1>
        내 최애<br />
        <ruby><span class="gradient-word">ボカロ</span><rt>보 카 로</rt></ruby
        >를<br />
        기록하는 곳<span class="blink">_</span>
      </h1>
      <p>
        미쿠, 루카, 렌… 좋아하는 캐릭터의 곡을 직접 등록하고<br />
        <span class="kw">태그</span>와 <span class="kw">분위기</span>로 탐색하는
        <strong>덕후들의 아카이브</strong>
      </p>
      <div class="hero-actions">
        <RouterLink to="/search" class="btn-primary">♪ 탐색 시작하기</RouterLink>
        <RouterLink to="/songs/new" class="btn-secondary">＋ 곡 등록하기</RouterLink>
      </div>
    </div>

    <div class="hero-stats">
      <div class="stat-card">
        <div class="stat-icon stat-icon--miku">♪</div>
        <div>
          <div class="stat-num">{{ formatCount(stats.songCount) }}</div>
          <div class="stat-label">등록된 곡 / songs</div>
        </div>
      </div>
      <div class="stat-card">
        <div class="stat-icon stat-icon--pink">♡</div>
        <div>
          <div class="stat-num">{{ formatCount(stats.userCount) }}</div>
          <div class="stat-label">활동 유저 / fans</div>
        </div>
      </div>
      <div class="stat-card">
        <div class="stat-icon stat-icon--lav">✦</div>
        <div>
          <div class="stat-num">{{ formatCount(stats.tagCount) }}</div>
          <div class="stat-label">태그 / tags</div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue';
import { RouterLink } from 'vue-router';

defineProps({
  stats: {
    type: Object,
    default: () => ({ songCount: 0, userCount: 0, tagCount: 0 }),
  },
});

const particlesEl = ref(null);

const GLYPHS = [
  { c: '♪', cls: 'note' },
  { c: '♫', cls: 'note' },
  { c: '✦', cls: 'star' },
  { c: '✧', cls: 'star' },
  { c: '★', cls: 'star' },
  { c: '♡', cls: 'heart' },
  { c: '♬', cls: 'note' },
  { c: '✦', cls: 'star' },
];

onMounted(() => {
  const root = particlesEl.value;
  if (!root) return;
  for (let i = 0; i < 22; i++) {
    const g = GLYPHS[Math.floor(Math.random() * GLYPHS.length)];
    const el = document.createElement('div');
    el.className = 'particle ' + g.cls;
    el.textContent = g.c;
    el.style.left = Math.random() * 100 + '%';
    el.style.top = Math.random() * 100 + '%';
    el.style.setProperty('--dx', (Math.random() - 0.5) * 80 + 'px');
    el.style.setProperty('--dy', -40 - Math.random() * 80 + 'px');
    const dur = 6 + Math.random() * 8;
    const delay = -Math.random() * dur;
    el.style.animation = `drift ${dur}s linear ${delay}s infinite, float-y ${3 + Math.random() * 3}s ease-in-out ${delay}s infinite`;
    el.style.fontSize = 12 + Math.random() * 14 + 'px';
    root.appendChild(el);
  }
});

function formatCount(n) {
  if (n >= 1000) return (n / 1000).toFixed(1) + 'k';
  return String(n ?? 0);
}
</script>

<style scoped>
.hero {
  background: linear-gradient(135deg, var(--miku-lt) 0%, var(--lav-lt) 50%, var(--pink-lt) 100%);
  padding: 80px 32px 72px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 32px;
  position: relative;
  overflow: hidden;
  border-bottom: 1.5px solid var(--border);
}

.cyber-grid {
  position: absolute;
  inset: 0;
  background-image: 
    linear-gradient(var(--grid-color) 1.5px, transparent 1.5px),
    linear-gradient(90deg, var(--grid-color) 1.5px, transparent 1.5px);
  background-size: 50px 50px;
  background-position: center top;
  opacity: 0.8;
  z-index: 0;
  mask-image: linear-gradient(to bottom, rgba(0,0,0,1) 30%, rgba(0,0,0,0) 100%);
  -webkit-mask-image: linear-gradient(to bottom, rgba(0,0,0,1) 30%, rgba(0,0,0,0) 100%);
  transform: perspective(250px) rotateX(60deg) translateY(-25%) translateZ(0);
  transform-origin: top center;
  animation: grid-flow 20s linear infinite;
  pointer-events: none;
}

@keyframes grid-flow {
  0% {
    background-position: center 0px;
  }
  100% {
    background-position: center 1000px;
  }
}

.hero::before {
  content: '';
  position: absolute;
  width: 380px;
  height: 380px;
  background: radial-gradient(circle, var(--miku) 0%, transparent 70%);
  top: -120px;
  right: 60px;
  border-radius: 50%;
  opacity: 0.35;
  animation: float-y 6s ease-in-out infinite;
}

.hero::after {
  content: '';
  position: absolute;
  width: 280px;
  height: 280px;
  background: radial-gradient(circle, var(--pink) 0%, transparent 70%);
  bottom: -80px;
  left: -40px;
  border-radius: 50%;
  opacity: 0.3;
  animation: float-y 7s ease-in-out infinite reverse;
}

.particles {
  position: absolute;
  inset: 0;
  pointer-events: none;
  overflow: hidden;
  z-index: 0;
}

:deep(.particle) {
  position: absolute;
  will-change: transform;
  user-select: none;
  pointer-events: none;
}

:deep(.particle.note) {
  color: var(--lav-dk);
  opacity: 0.55;
  text-shadow: 0 0 10px rgba(196, 181, 253, 0.6);
}

:deep(.particle.star) {
  color: var(--pink-dk);
  opacity: 0.7;
  animation: sparkle 2.4s ease-in-out infinite;
}

:deep(.particle.heart) {
  color: var(--miku-dk);
  opacity: 0.55;
}

.hero-text {
  max-width: 560px;
  position: relative;
  z-index: 2;
}

.hero-badge {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  background: var(--surface);
  color: var(--miku-dk);
  font-size: 12px;
  font-weight: 800;
  padding: 6px 14px 6px 10px;
  border-radius: 99px;
  margin-bottom: 18px;
  border: 2px solid transparent;
  background-image:
    linear-gradient(var(--surface), var(--surface)),
    linear-gradient(90deg, var(--miku), var(--pink), var(--lav), var(--miku));
  background-origin: border-box;
  background-clip: padding-box, border-box;
  background-size:
    100% 100%,
    300% 100%;
  animation: shimmer 4s linear infinite;
  box-shadow: 0 4px 14px rgba(125, 223, 212, 0.3);
}

.hero-badge::before {
  content: '✦';
  color: var(--pink-dk);
  animation: sparkle 1.8s ease-in-out infinite;
  display: inline-block;
}

h1 {
  font-family: var(--font-display);
  font-size: 52px;
  line-height: 1.15;
  color: var(--text);
  margin-bottom: 14px;
  text-shadow:
    2px 2px 0 var(--surface),
    4px 4px 0 var(--pink-lt),
    6px 6px 0 var(--lav-lt);
  letter-spacing: -0.5px;
}

.gradient-word {
  background: linear-gradient(90deg, var(--miku-dk), var(--lav-dk), var(--pink-dk), var(--miku-dk));
  background-size: 300% 100%;
  -webkit-background-clip: text;
  background-clip: text;
  -webkit-text-fill-color: transparent;
  color: transparent;
  animation: shimmer 4s linear infinite;
}

ruby {
  font-size: inherit;
}
ruby rt {
  font-family: var(--font-jp);
  font-size: 0.32em;
  color: var(--lav-dk);
  font-weight: 700;
  letter-spacing: 0.1em;
  line-height: 1;
}

p {
  font-size: 15px;
  color: var(--text2);
  line-height: 1.75;
  margin-bottom: 28px;
  font-weight: 600;
}

.kw {
  background: var(--yellow-lt);
  box-shadow: 0 -8px 0 var(--yellow-lt) inset;
  color: var(--text);
  font-weight: 800;
  padding: 0 2px;
}

.blink {
  animation: blink-cursor 1s steps(2) infinite;
  color: var(--pink-dk);
}

.hero-actions {
  display: flex;
  gap: 12px;
  flex-wrap: wrap;
}

.btn-primary {
  font-family: var(--font-body);
  font-weight: 800;
  font-size: 15px;
  background: linear-gradient(135deg, var(--miku-dk), var(--lav-dk));
  color: white;
  border: none;
  padding: 14px 30px;
  border-radius: var(--radius-md);
  cursor: pointer;
  transition: all 0.2s;
  box-shadow:
    0 6px 18px rgba(59, 188, 176, 0.45),
    inset 0 1px 0 rgba(255, 255, 255, 0.4);
  position: relative;
  overflow: hidden;
  text-decoration: none;
  display: inline-flex;
  align-items: center;
}

.btn-primary::after {
  content: '';
  position: absolute;
  inset: 0;
  background: linear-gradient(
    120deg,
    transparent 35%,
    rgba(255, 255, 255, 0.5) 50%,
    transparent 65%
  );
  background-size: 200% 100%;
  animation: shimmer 2.6s linear infinite;
}

.btn-primary:hover {
  transform: translateY(-3px) scale(1.02);
}

.btn-secondary {
  font-family: var(--font-body);
  font-weight: 800;
  font-size: 15px;
  background: var(--surface);
  color: var(--text2);
  border: 2px dashed var(--lav);
  padding: 12px 28px;
  border-radius: var(--radius-md);
  cursor: pointer;
  transition: all 0.18s;
  text-decoration: none;
  display: inline-flex;
  align-items: center;
}

.btn-secondary:hover {
  background: var(--lav-lt);
  border-color: var(--lav-dk);
  color: var(--lav-dk);
  border-style: solid;
}

.hero-stats {
  display: flex;
  flex-direction: column;
  gap: 14px;
  position: relative;
  z-index: 2;
  flex-shrink: 0;
}

.stat-card {
  background: var(--surface);
  border-radius: var(--radius-lg);
  padding: 18px 24px;
  box-shadow: var(--shadow-md);
  display: flex;
  align-items: center;
  gap: 14px;
  min-width: 230px;
  border: 1.5px solid var(--border);
  position: relative;
  transition: all 0.3s cubic-bezier(0.25, 0.8, 0.25, 1);
  overflow: hidden;
}

.stat-card::after {
  content: '';
  position: absolute;
  inset: 0;
  border-radius: var(--radius-lg);
  border: 2px solid transparent;
  background: linear-gradient(135deg, var(--miku), var(--pink)) border-box;
  -webkit-mask: linear-gradient(#fff 0 0) padding-box, linear-gradient(#fff 0 0);
  mask: linear-gradient(#fff 0 0) padding-box, linear-gradient(#fff 0 0);
  -webkit-mask-composite: destination-out;
  mask-composite: exclude;
  opacity: 0;
  transition: opacity 0.3s ease;
  pointer-events: none;
}

.stat-card:hover {
  transform: translateY(-5px) scale(1.03) rotate(-1.5deg);
  box-shadow: 
    0 12px 28px rgba(139, 92, 246, 0.15),
    0 0 14px var(--miku-lt);
  border-color: var(--miku);
}

.stat-card:nth-child(2):hover {
  transform: translateY(-5px) scale(1.03) rotate(1.5deg);
  border-color: var(--pink);
  box-shadow: 
    0 12px 28px rgba(139, 92, 246, 0.15),
    0 0 14px var(--pink-lt);
}

.stat-card:nth-child(3):hover {
  transform: translateY(-5px) scale(1.03) rotate(-0.5deg);
  border-color: var(--lav);
  box-shadow: 
    0 12px 28px rgba(139, 92, 246, 0.15),
    0 0 14px var(--lav-lt);
}

.stat-card::before {
  content: '✦';
  position: absolute;
  top: -8px;
  right: 14px;
  font-size: 14px;
  color: var(--pink-dk);
  background: var(--surface);
  padding: 0 4px;
  animation: sparkle 3s ease-in-out infinite;
}

.stat-icon {
  width: 44px;
  height: 44px;
  border-radius: var(--radius-sm);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 20px;
  flex-shrink: 0;
  box-shadow: inset 0 0 0 1.5px rgba(255, 255, 255, 0.6);
}
.stat-icon--miku { background: var(--miku-lt); color: var(--miku-dk); }
.stat-icon--pink { background: var(--pink-lt); color: var(--pink-dk); }
.stat-icon--lav  { background: var(--lav-lt);  color: var(--lav-dk);  }

.stat-num {
  font-family: var(--font-display);
  font-size: 26px;
  font-weight: 800;
  color: var(--text);
  line-height: 1;
  letter-spacing: 0.5px;
}

.stat-label {
  font-size: 11px;
  color: var(--text3);
  font-weight: 800;
  margin-top: 4px;
  letter-spacing: 0.05em;
  text-transform: uppercase;
}

@media (max-width: 768px) {
  .hero {
    flex-direction: column;
    padding: 40px 16px 36px;
    gap: 24px;
    text-align: center;
  }

  h1 {
    font-size: 34px;
  }

  p {
    font-size: 14px;
  }

  .hero-actions {
    justify-content: center;
  }

  .hero-stats {
    flex-direction: row;
    flex-wrap: wrap;
    justify-content: center;
    gap: 10px;
    width: 100%;
  }

  .stat-card {
    min-width: unset;
    flex: 1 1 140px;
    padding: 14px 16px;
    gap: 10px;
  }

  .stat-num {
    font-size: 20px;
  }

  .hero-badge {
    font-size: 11px;
  }

  .btn-primary,
  .btn-secondary {
    font-size: 14px;
    padding: 12px 22px;
  }
}
</style>
