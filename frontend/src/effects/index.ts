/** 特效模块 — 导出所有 */

export { ParticleManager, Spark, COLOR_PALETTES } from './EffectEngine';
export type { ParticleStyle, ColorPalette } from './EffectEngine';

export { ALL_ANIMATIONS, injectCardAnimations } from './CardAnimations';

export { buildProfile } from './AnimProfileBuilder';
export type { AnimProfile, EnterType } from './AnimProfileBuilder';

export { getRarityConfig, RARITY_CSS } from './RarityEffects';
export type { RarityConfig } from './RarityEffects';
