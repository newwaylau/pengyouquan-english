/** 动画参数合成器 — 根据Card特征构建完整动画配置 */
import type { CardResponse } from '../api/CardResponse';
import type { ParticleStyle } from './EffectEngine';
import type { RarityConfig } from './RarityEffects';

/* ─────────── 类型导出 ─────────── */
export interface AnimProfile {
  /** CSS animation name(s) */
  enterAnim: string;
  overlayAnim: string | null;
  kwAnim: string[];
  rarityGlow: string | null;
  /** 粒子数量 */
  particleCount: number;
  /** 粒子配色 */
  particleStyle: ParticleStyle;
  /** 屏幕震动 0-1 */
  shakeLevel: number;
  /** 是否白闪 */
  flash: boolean;
  /** 动画时长 s */
  duration: number;
  /** 稀有度配置 */
  rarityConfig: RarityConfig;
}

/** 入场类型 */
export type EnterType = 'enter-cheap' | 'enter-mid' | 'enter-expensive' | 'enter-legendary' | 'enter-boss';

/** 元素 -> 粒子配色映射 */
const ELEMENT_TO_STYLE: Record<string, ParticleStyle> = {
  fire: 'fire',
  ice: 'ice',
  shadow: 'shadow',
  light: 'light',
  nature: 'nature',
  metal: 'metal',
};

/* ─────────── 关键词识别 ─────────── */
const KEYWORD_MAP: Record<string, string> = {
  battlecry: 'kw-battlecry',
  deathrattle: 'kw-deathrattle',
  charge: 'kw-charge',
  taunt: 'kw-taunt',
  stealth: 'kw-stealth',
};

/* ─────────── 种族粒子特效 ─────────── */
const RACE_PARTICLE: Record<string, ParticleStyle> = {
  dragon: 'fire',
  undead: 'ice',
  beast: 'nature',
  human: 'metal',
  holy: 'light',
};

/* ─────────── 稀有度配置 ─────────── */
const RARITY_MULTIPLIER: Record<string, number> = {
  common: 1,
  rare: 1.5,
  epic: 2,
  legendary: 3,
};

/* ─────────── 费用区间配置 ─────────── */
interface CostConfig {
  enterType: EnterType;
  shakeLevel: number;
  duration: number;
}

const COST_CONFIGS: Array<{ min: number; max: number; config: CostConfig }> = [
  { min: 1, max: 3, config: { enterType: 'enter-cheap', shakeLevel: 0.1, duration: 0.4 } },
  { min: 4, max: 6, config: { enterType: 'enter-mid', shakeLevel: 0.3, duration: 0.7 } },
  { min: 7, max: 10, config: { enterType: 'enter-expensive', shakeLevel: 0.6, duration: 1.0 } },
];

/**
 * 根据卡牌特征合成完整动画配置
 */
export function buildProfile(card: CardResponse): AnimProfile {
  /* ───── 费用 / 入场 ───── */
  const costCfg = COST_CONFIGS.find(c => card.cost >= c.min && card.cost <= c.max)
    ?? { config: { enterType: 'enter-cheap' as EnterType, shakeLevel: 0.1, duration: 0.4 } };
  const { enterType, shakeLevel, duration } = costCfg.config;

  /* ───── 传说/Boss覆盖 ───── */
  let finalEnterType = enterType;
  if (card.rarity === 'legendary' && card.cost >= 7) {
    finalEnterType = 'enter-legendary';
  }
  if (card.cardType === 'location' && card.cost >= 8) {
    finalEnterType = 'enter-boss';
  }

  /* ───── 元素覆盖层 ───── */
  const overlayAnim = card.element ? `ol-${card.element}` : null;

  /* ───── 粒子样式 ───── */
  const elementStyle = card.element ? ELEMENT_TO_STYLE[card.element] : null;
  const raceStyle = card.race ? RACE_PARTICLE[card.race] : null;
  const particleStyle: ParticleStyle = elementStyle ?? raceStyle ?? 'light';

  /* ───── 关键词特效 ───── */
  const kwAnim: string[] = [];
  if (card.keywords) {
    const parsed = parseKeywords(card.keywords);
    for (const kw of parsed) {
      const mapped = KEYWORD_MAP[kw];
      if (mapped) kwAnim.push(mapped);
    }
  }

  /* ───── 稀有度 ───── */
  const rarityMultiplier = RARITY_MULTIPLIER[card.rarity] ?? 1;
  const particleCount = Math.round(15 * rarityMultiplier);
  const flash = card.rarity === 'legendary';

  /* ───── 稀有光效 ───── */
  let rarityGlow: string | null = null;
  if (card.rarity === 'legendary') rarityGlow = 'leg-glow';
  else if (card.rarity === 'epic') rarityGlow = 'epic-glow';
  else if (card.rarity === 'rare') rarityGlow = 'rare-glow';

  return {
    enterAnim: finalEnterType,
    overlayAnim,
    kwAnim,
    rarityGlow,
    particleCount,
    particleStyle,
    shakeLevel,
    flash,
    duration,
    rarityConfig: {
      className: card.rarity,
      particleMultiplier: rarityMultiplier,
      flash: card.rarity === 'legendary',
      glow: rarityGlow,
    },
  };
}

/**
 * 解析 keywords 字段 (逗号分隔, 下划线格式)
 * e.g. "battlecry,taunt" → ["battlecry","taunt"]
 */
function parseKeywords(kw: string): string[] {
  return kw
    .split(',')
    .map(s => s.trim().toLowerCase().replace(/-/g, '_'))
    .filter(Boolean);
}
