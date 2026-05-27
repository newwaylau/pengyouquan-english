// ====== AnimProfileBuilder — 元素色配置 & 动画参数 ======

/** 元素风格配置 */
export interface ElementStyle {
  label: string;
  color: string;        // 主色
  glowColor: string;    // 发光色
  flashClass: string;   // CSS flash overlay class
  particlePalette: string[]; // 粒子颜色
}

/** 卡牌动画配置 */
export interface CardAnimConfig {
  element: string;
  damageColor: string;
  particleCount: number;
  effectStyle: string;
}

const PALETTES: Record<string, string[]> = {
  fire:   ['#ef4444','#f97316','#fbbf24','#dc2626','#fff7ed'],
  ice:    ['#93c5fd','#60a5fa','#3b82f6','#bfdbfe','#e0f2fe'],
  shadow: ['#c084fc','#a855f7','#7c3aed','#ddd6fe','#ede9fe'],
  nature: ['#86efac','#22c55e','#16a34a','#bbf7d0','#dcfce7'],
  light:  ['#fde047','#eab308','#fef08a','#fff7ed'],
  metal:  ['#cbd5e1','#94a3b8','#64748b','#f1f5f9'],
  blood:  ['#dc2626','#b91c1c','#991b1b','#ef4444','#7f1d1d'],
};

export const ELEMENT_STYLES: Record<string, ElementStyle> = {
  fire:   { label: '火焰', color: '#ef4444', glowColor: 'rgba(239,68,68,0.6)', flashClass: 'fire', particlePalette: PALETTES.fire },
  ice:    { label: '寒冰', color: '#60a5fa', glowColor: 'rgba(96,165,250,0.6)', flashClass: 'ice', particlePalette: PALETTES.ice },
  shadow: { label: '暗影', color: '#a855f7', glowColor: 'rgba(168,85,247,0.6)', flashClass: 'shadow', particlePalette: PALETTES.shadow },
  nature: { label: '自然', color: '#22c55e', glowColor: 'rgba(34,197,94,0.6)', flashClass: 'nature', particlePalette: PALETTES.nature },
  light:  { label: '神圣', color: '#eab308', glowColor: 'rgba(234,179,8,0.6)', flashClass: 'light', particlePalette: PALETTES.light },
  metal:  { label: '钢铁', color: '#94a3b8', glowColor: 'rgba(148,163,184,0.6)', flashClass: 'metal', particlePalette: PALETTES.metal },
  blood:  { label: '鲜血', color: '#ef4444', glowColor: 'rgba(239,68,68,0.6)', flashClass: 'blood', particlePalette: PALETTES.blood },
};

/** 根据卡牌类型/稀有度获取动画配置 */
export function getCardAnimConfig(attack: number, rarity?: string): CardAnimConfig {
  let element = 'fire';
  if (attack >= 8) element = 'fire';
  else if (attack >= 5) element = 'ice';
  else if (attack >= 3) element = 'metal';
  else element = 'shadow';

  const style = ELEMENT_STYLES[element] || ELEMENT_STYLES.fire;

  let particleCount = 15;
  switch (rarity) {
    case 'legendary': particleCount = 50; break;
    case 'epic':      particleCount = 35; break;
    case 'rare':      particleCount = 25; break;
    default:          particleCount = 15;
  }

  return {
    element,
    damageColor: style.color,
    particleCount,
    effectStyle: element,
  };
}

/** 屏幕震动强度 */
export function getShakeIntensity(damage: number): 'light' | 'mid' | 'big' {
  if (damage >= 8) return 'big';
  if (damage >= 5) return 'mid';
  return 'light';
}

/** 伤害字体大小 */
export function getDamageFontSize(damage: number): number {
  return Math.max(18, Math.min(16 + damage * 2, 48));
}
