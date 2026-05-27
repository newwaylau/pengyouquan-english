/** 稀有度特效 — 返回className+配置 */

export interface RarityConfig {
  className: 'common' | 'rare' | 'epic' | 'legendary';
  /** 粒子数量乘数 */
  particleMultiplier: number;
  /** 是否白闪 */
  flash: boolean;
  /** 光效动画名称 (null=无) */
  glow: string | null;
}

/**
 * 获取稀有度对应的 CSS class 名 (注入index.css中)
 *
 * 传说 → legend-glow:  金色流光旋转(conic-gradient)+白闪0.08s+粒子×3
 * 史诗 → epic-glow:    紫色光柱+粒子×2
 * 稀有 → rare-glow:    蓝色微光+粒子×1.5
 * 普通 → common:       无额外特效
 */
export function getRarityConfig(rarity: string): RarityConfig {
  switch (rarity) {
    case 'legendary':
      return { className: 'legendary', particleMultiplier: 3, flash: true, glow: 'leg-glow' };
    case 'epic':
      return { className: 'epic', particleMultiplier: 2, flash: false, glow: 'epic-glow' };
    case 'rare':
      return { className: 'rare', particleMultiplier: 1.5, flash: false, glow: 'rare-glow' };
    default:
      return { className: 'common', particleMultiplier: 1, flash: false, glow: null };
  }
}

/** 所有稀有度CSS — 注入index.css使用 */
export const RARITY_CSS = `
/* ─── 传说稀有度 ─── */
.legendary-glow {
  position: relative;
  overflow: hidden;
}
.legendary-glow::before {
  content: '';
  position: absolute;
  inset: -2px;
  border-radius: inherit;
  background: conic-gradient(
    from 0deg,
    #ff8c00, #ffd700, #ffa500, #ff8c00, #ff4500,
    #ff8c00, #ffd700, #ffa500, #ff8c00
  );
  z-index: -1;
  animation: legendary-spin 3s linear infinite;
  mask: linear-gradient(#fff 0 0) content-box, linear-gradient(#fff 0 0);
  mask-composite: exclude;
  -webkit-mask: linear-gradient(#fff 0 0) content-box, linear-gradient(#fff 0 0);
  -webkit-mask-composite: xor;
  padding: 2px;
}
.legendary-flash {
  animation: legendary-flash 0.08s ease-out 1;
}
@keyframes legendary-spin {
  to { transform: rotate(360deg); }
}
@keyframes legendary-flash {
  0%   { filter: brightness(1); }
  50%  { filter: brightness(3) saturate(0); }
  100% { filter: brightness(1); }
}

/* ─── 史诗稀有度 ─── */
.epic-glow {
  position: relative;
}
.epic-glow::before {
  content: '';
  position: absolute;
  inset: 0;
  background: linear-gradient(180deg, transparent 0%, rgba(163,53,238,0.15) 30%, rgba(163,53,238,0.25) 60%, transparent 100%);
  pointer-events: none;
  border-radius: inherit;
}

/* ─── 稀有稀有度 ─── */
.rare-glow {
  box-shadow: 0 0 4px rgba(0,112,221,0.4), inset 0 0 4px rgba(0,112,221,0.15);
}

/* ─── 普通稀有度 ─── */
.common-card {
  /* 无额外特效 */
}
`;
