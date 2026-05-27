/** 卡牌动画模板 — 15个纯CSS keyframes字符串, 动态注入<style> */

/** 将所有 keyframes 注入页面 <style> */
export function injectCardAnimations(): HTMLStyleElement {
  const existing = document.getElementById('card-animations-style');
  if (existing) return existing as HTMLStyleElement;

  const style = document.createElement('style');
  style.id = 'card-animations-style';
  style.textContent = ALL_ANIMATIONS;
  document.head.appendChild(style);
  return style;
}

/** 全部动画 CSS 文本 */
export const ALL_ANIMATIONS = [
  /* ─────────── 5种入场类型 ─────────── */
  `
@keyframes card-enter-cheap {
  0%   { transform: scale(0.3) rotate(-5deg); opacity: 0; }
  60%  { transform: scale(1.05) rotate(1deg); opacity: 1; }
  100% { transform: scale(1) rotate(0deg); opacity: 1; }
}
@keyframes card-enter-mid {
  0%   { transform: scale(0.2) translateY(60px); opacity: 0; }
  50%  { transform: scale(1.08) translateY(-8px); opacity: 1; }
  80%  { transform: scale(0.98) translateY(2px); }
  100% { transform: scale(1) translateY(0); opacity: 1; }
}
@keyframes card-enter-expensive {
  0%   { transform: scale(0.1) translateY(100px) rotate(-10deg); opacity: 0; filter: brightness(0.3); }
  40%  { transform: scale(1.12) translateY(-15px) rotate(3deg); opacity: 1; filter: brightness(1.3); }
  70%  { transform: scale(0.95) translateY(5px) rotate(-1deg); }
  100% { transform: scale(1) translateY(0) rotate(0deg); opacity: 1; filter: brightness(1); }
}
@keyframes card-enter-legendary {
  0%   { transform: scale(0) rotate(-15deg); opacity: 0; filter: brightness(0) saturate(0); }
  30%  { transform: scale(1.2) rotate(5deg); opacity: 1; filter: brightness(2) saturate(1.8); }
  60%  { transform: scale(0.95) rotate(-2deg); filter: brightness(1.2) saturate(1.2); }
  100% { transform: scale(1) rotate(0deg); opacity: 1; filter: brightness(1) saturate(1); }
}
@keyframes card-enter-boss {
  0%   { transform: scale(0) translateY(-200px); opacity: 0; filter: blur(10px); }
  40%  { transform: scale(1.15) translateY(0); opacity: 1; filter: blur(0); }
  70%  { transform: scale(0.9); }
  100% { transform: scale(1); opacity: 1; }
}
`,
  /* ─────────── 6种元素覆盖层 ─────────── */
  `
@keyframes ol-fire {
  0%   { box-shadow: inset 0 0 10px #ff4500; background: radial-gradient(circle, rgba(255,69,0,0.15) 0%, transparent 70%); }
  50%  { box-shadow: inset 0 0 25px #ff8c00, 0 0 15px #ff4500; background: radial-gradient(circle, rgba(255,140,0,0.25) 20%, transparent 80%); }
  100% { box-shadow: inset 0 0 10px #ff4500; background: radial-gradient(circle, rgba(255,69,0,0.15) 0%, transparent 70%); }
}
@keyframes ol-ice {
  0%   { box-shadow: inset 0 0 10px #00bfff; background: radial-gradient(circle, rgba(0,191,255,0.15) 0%, transparent 70%); }
  50%  { box-shadow: inset 0 0 25px #87ceeb, 0 0 15px #00bfff; background: radial-gradient(circle, rgba(135,206,235,0.25) 20%, transparent 80%); }
  100% { box-shadow: inset 0 0 10px #00bfff; background: radial-gradient(circle, rgba(0,191,255,0.15) 0%, transparent 70%); }
}
@keyframes ol-shadow {
  0%   { box-shadow: inset 0 0 10px #8b00ff; background: radial-gradient(circle, rgba(139,0,255,0.15) 0%, transparent 70%); }
  50%  { box-shadow: inset 0 0 25px #dda0dd, 0 0 15px #8b00ff; background: radial-gradient(circle, rgba(221,160,221,0.25) 20%, transparent 80%); }
  100% { box-shadow: inset 0 0 10px #8b00ff; background: radial-gradient(circle, rgba(139,0,255,0.15) 0%, transparent 70%); }
}
@keyframes ol-light {
  0%   { box-shadow: inset 0 0 10px #ffd700; background: radial-gradient(circle, rgba(255,215,0,0.15) 0%, transparent 70%); }
  50%  { box-shadow: inset 0 0 30px #fffacd, 0 0 20px #ffd700; background: radial-gradient(circle, rgba(255,250,205,0.3) 20%, transparent 80%); }
  100% { box-shadow: inset 0 0 10px #ffd700; background: radial-gradient(circle, rgba(255,215,0,0.15) 0%, transparent 70%); }
}
@keyframes ol-nature {
  0%   { box-shadow: inset 0 0 10px #32cd32; background: radial-gradient(circle, rgba(50,205,50,0.15) 0%, transparent 70%); }
  50%  { box-shadow: inset 0 0 25px #98fb98, 0 0 15px #32cd32; background: radial-gradient(circle, rgba(152,251,152,0.25) 20%, transparent 80%); }
  100% { box-shadow: inset 0 0 10px #32cd32; background: radial-gradient(circle, rgba(50,205,50,0.15) 0%, transparent 70%); }
}
@keyframes ol-metal {
  0%   { box-shadow: inset 0 0 10px #a9a9a9; background: radial-gradient(circle, rgba(169,169,169,0.15) 0%, transparent 70%); }
  50%  { box-shadow: inset 0 0 25px #dcdcdc, 0 0 15px #a9a9a9; background: radial-gradient(circle, rgba(220,220,220,0.25) 20%, transparent 80%); }
  100% { box-shadow: inset 0 0 10px #a9a9a9; background: radial-gradient(circle, rgba(169,169,169,0.15) 0%, transparent 70%); }
}
`,
  /* ─────────── 5种关键词特效 ─────────── */
  `
@keyframes kw-battlecry {
  0%   { filter: brightness(1) drop-shadow(0 0 0px #3498db); }
  25%  { filter: brightness(1.5) drop-shadow(0 0 12px #3498db); transform: scale(1.03); }
  50%  { filter: brightness(1) drop-shadow(0 0 6px #3498db); }
  75%  { filter: brightness(1.3) drop-shadow(0 0 10px #3498db); transform: scale(1.02); }
  100% { filter: brightness(1) drop-shadow(0 0 0px #3498db); }
}
@keyframes kw-deathrattle {
  0%   { filter: brightness(1); clip-path: inset(0 0 0 0); }
  30%  { filter: brightness(1.3) sepia(0.3); clip-path: inset(0 0 0 0); }
  70%  { filter: brightness(0.7) sepia(0.6); clip-path: inset(0 0 20% 20%); opacity: 0.7; }
  100% { filter: brightness(0.3) sepia(0.8); clip-path: inset(0 0 40% 40%); opacity: 0.4; }
}
@keyframes kw-charge {
  0%   { transform: translateX(-20px) scaleX(1); filter: brightness(1); }
  30%  { transform: translateX(5px) scaleX(1.1); filter: brightness(1.3); }
  60%  { transform: translateX(-3px) scaleX(0.95); filter: brightness(1.1); }
  100% { transform: translateX(0) scaleX(1); filter: brightness(1); }
}
@keyframes kw-taunt {
  0%   { box-shadow: 0 0 0 0 rgba(231,76,60,0.6); border-color: #e74c3c; }
  25%  { box-shadow: 0 0 15px 5px rgba(231,76,60,0.4); }
  50%  { box-shadow: 0 0 0 0 rgba(231,76,60,0.6); }
  75%  { box-shadow: 0 0 10px 3px rgba(231,76,60,0.4); }
  100% { box-shadow: 0 0 0 0 rgba(231,76,60,0.6); border-color: #e74c3c; }
}
@keyframes kw-stealth {
  0%   { opacity: 1; filter: brightness(1); }
  20%  { opacity: 0.3; filter: brightness(1.5) contrast(1.2); }
  40%  { opacity: 0.7; filter: brightness(1); }
  60%  { opacity: 0.2; filter: brightness(1.5) contrast(1.2); }
  80%  { opacity: 0.6; filter: brightness(1); }
  100% { opacity: 1; filter: brightness(1); }
}
`,
  /* ─────────── 3种稀有度光效 ─────────── */
  `
@keyframes leg-glow {
  0%   { box-shadow: 0 0 5px #ff8c00, 0 0 10px #ff8c00, 0 0 20px #ff8c00, 0 0 40px #ff4500; }
  25%  { box-shadow: 0 0 8px #ffa500, 0 0 16px #ffa500, 0 0 30px #ff8c00, 0 0 50px #ff4500; }
  50%  { box-shadow: 0 0 10px #ffd700, 0 0 20px #ffd700, 0 0 40px #ff8c00, 0 0 60px #ff4500; }
  75%  { box-shadow: 0 0 8px #ffa500, 0 0 16px #ffa500, 0 0 30px #ff8c00, 0 0 50px #ff4500; }
  100% { box-shadow: 0 0 5px #ff8c00, 0 0 10px #ff8c00, 0 0 20px #ff8c00, 0 0 40px #ff4500; }
}
@keyframes epic-glow {
  0%   { box-shadow: 0 0 3px #a335ee, 0 0 8px #a335ee, 0 0 15px #8b00ff; }
  50%  { box-shadow: 0 0 6px #c77dff, 0 0 15px #a335ee, 0 0 25px #8b00ff; }
  100% { box-shadow: 0 0 3px #a335ee, 0 0 8px #a335ee, 0 0 15px #8b00ff; }
}
@keyframes rare-glow {
  0%   { box-shadow: 0 0 2px #0070dd, 0 0 5px #0070dd; }
  50%  { box-shadow: 0 0 4px #4a9eff, 0 0 10px #0070dd; }
  100% { box-shadow: 0 0 2px #0070dd, 0 0 5px #0070dd; }
}
`,
].join('\n');
