// ====== EffectEngine — 战斗动画系统 ======
// Reference implementation: ~/.hermes/cache/sts-battle-anim.html
// 杀戮尖塔风格: 卡牌飞入、伤害数字、屏幕震动、受击效果、粒子爆发

import { getShakeIntensity, getDamageFontSize, ELEMENT_STYLES } from './AnimProfileBuilder';

/** 在战场容器上创建闪屏覆盖层 */
export function elementFlash(container: HTMLElement, elementType: string) {
  const overlay = document.createElement('div');
  overlay.className = 'ba-flash-overlay';
  overlay.style.cssText = `
    position: absolute; inset: 0; z-index: 200; pointer-events: none; border-radius: inherit;
    background: radial-gradient(circle, ${getFlashColor(elementType)} 0%, transparent 60%);
    opacity: 0;
  `;
  container.appendChild(overlay);
  requestAnimationFrame(() => {
    overlay.style.transition = 'opacity 0.15s ease-out';
    overlay.style.opacity = '1';
  });
  setTimeout(() => {
    overlay.style.transition = 'opacity 0.4s ease-out';
    overlay.style.opacity = '0';
    setTimeout(() => overlay.remove(), 500);
  }, 200);
}

function getFlashColor(elementType: string): string {
  switch (elementType) {
    case 'fire':   return 'rgba(239,68,68,0.35)';
    case 'ice':    return 'rgba(147,197,253,0.3)';
    case 'shadow': return 'rgba(168,85,247,0.25)';
    case 'light':  return 'rgba(250,204,21,0.3)';
    case 'nature': return 'rgba(34,197,94,0.25)';
    case 'metal':  return 'rgba(148,163,184,0.2)';
    case 'blood':  return 'rgba(239,68,68,0.4)';
    default:        return 'rgba(239,68,68,0.35)';
  }
}

/** 白色闪屏（大伤害专用） */
export function whiteFlash(container: HTMLElement) {
  const flash = document.createElement('div');
  flash.className = 'ba-white-flash';
  flash.style.cssText = `
    position: absolute; inset: 0; z-index: 250; pointer-events: none; border-radius: inherit;
    background: white;
  `;
  container.appendChild(flash);
  requestAnimationFrame(() => {
    flash.style.transition = 'opacity 0.08s ease-out';
    flash.style.opacity = '0.7';
  });
  setTimeout(() => {
    flash.style.transition = 'opacity 0.15s ease-out';
    flash.style.opacity = '0';
    setTimeout(() => flash.remove(), 200);
  }, 50);
}

/** 卡牌出牌动画: 从手牌位置飞向战场中心 */
export function playCardAnimation(
  cardEl: HTMLElement,
  container: HTMLElement,
  color: string
): Promise<void> {
  return new Promise(resolve => {
    const rect = cardEl.getBoundingClientRect();
    const containerRect = container.getBoundingClientRect();

    // 克隆卡牌元素
    const clone = cardEl.cloneNode(true) as HTMLElement;
    clone.style.cssText = `
      position: absolute;
      z-index: 300;
      margin: 0;
      transition: none;
      left: ${rect.left - containerRect.left}px;
      top: ${rect.top - containerRect.top}px;
      width: ${rect.width}px;
      height: ${rect.height}px;
      pointer-events: none;
      border-radius: ${getComputedStyle(cardEl).borderRadius || '6px'};
    `;
    container.appendChild(clone);
    cardEl.style.visibility = 'hidden';

    // 目标: 战场中心
    const cx = containerRect.width / 2 - rect.width / 2;
    const cy = containerRect.height / 2 - rect.height / 2;

    requestAnimationFrame(() => {
      clone.style.transition = 'all 0.3s cubic-bezier(0.22, 1, 0.36, 1)';
      clone.style.left = cx + 'px';
      clone.style.top = cy + 'px';
      clone.style.transform = 'scale(1.15)';
      clone.style.boxShadow = `0 0 30px ${color}`;
      clone.style.borderColor = color;
    });

    // 卡牌收缩消失
    setTimeout(() => {
      clone.style.transition = 'all 0.25s ease-out';
      clone.style.transform = 'scale(0.3)';
      clone.style.opacity = '0';
      setTimeout(() => {
        clone.remove();
        cardEl.style.visibility = 'visible';
        resolve();
      }, 300);
    }, 350);
  });
}

/** 伤害数字弹出 */
export function showDamageNumber(
  container: HTMLElement,
  x: number,
  y: number,
  value: number,
  color: string,
  fontSize?: number
) {
  const el = document.createElement('div');
  el.className = 'ba-dmg-popup';
  const size = fontSize || getDamageFontSize(value);
  const sign = value > 0 ? `-${value}` : `${value}`;
  el.textContent = sign;
  el.style.cssText = `
    position: absolute; z-index: 180; pointer-events: none;
    font-weight: 900; color: ${color};
    text-shadow: 0 0 15px ${color}, 0 0 30px ${color}40;
    left: ${x}px; top: ${y}px;
    font-size: ${size}px;
    animation: ba-dmg-float 0.9s ease-out forwards;
  `;
  container.appendChild(el);
  setTimeout(() => el.remove(), 1000);
}

/** 受击后仰+闪烁 */
export function hitAnimation(el: HTMLElement, intensity: 'light' | 'mid' | 'big' = 'mid') {
  el.style.transition = 'none';
  const duration = intensity === 'big' ? 0.6 : intensity === 'mid' ? 0.4 : 0.3;
  el.style.animation = `ba-enemy-hit ${duration}s ease-out`;
  setTimeout(() => {
    el.style.animation = '';
  }, duration * 1000 + 100);
}

/** 屏幕震动 */
export function screenShake(container: HTMLElement, damage: number) {
  const intensity = getShakeIntensity(damage);
  const duration = intensity === 'big' ? 0.5 : intensity === 'mid' ? 0.4 : 0.3;
  const animName = intensity === 'big' ? 'ba-shake-big' : intensity === 'mid' ? 'ba-shake-mid' : 'ba-shake-light';
  container.style.animation = `${animName} ${duration}s ease-out`;
  setTimeout(() => {
    container.style.animation = '';
  }, duration * 1000 + 200);
}

/** 粒子爆发(基于DOM, 创建一组小球) */
export function spawnParticles(
  container: HTMLElement,
  cx: number,
  cy: number,
  count: number,
  elementType: string
) {
  const style = ELEMENT_STYLES[elementType] || ELEMENT_STYLES.fire;
  const particles: HTMLElement[] = [];

  for (let i = 0; i < count; i++) {
    const p = document.createElement('div');
    const color = style.particlePalette[Math.floor(Math.random() * style.particlePalette.length)];
    const size = 3 + Math.random() * 6;
    const angle = Math.random() * Math.PI * 2;
    const speed = 50 + Math.random() * 120;
    const dx = Math.cos(angle) * speed;
    const dy = Math.sin(angle) * speed - 30;

    p.style.cssText = `
      position: absolute; z-index: 150; pointer-events: none;
      left: ${cx}px; top: ${cy}px;
      width: ${size}px; height: ${size}px;
      border-radius: 50%;
      background: ${color};
      box-shadow: 0 0 6px ${color};
      transition: all ${0.4 + Math.random() * 0.4}s cubic-bezier(0.25, 0.46, 0.45, 0.94);
      opacity: 1;
    `;
    container.appendChild(p);
    particles.push(p);

    requestAnimationFrame(() => {
      p.style.transform = `translate(${dx}px, ${dy}px) scale(${0.2 + Math.random() * 0.5})`;
      p.style.opacity = '0';
    });

    setTimeout(() => p.remove(), 900);
  }

  return particles;
}

/** 敌人死亡: 放大→变亮→消失 */
export function enemyDeathAnimation(el: HTMLElement): Promise<void> {
  return new Promise(resolve => {
    el.style.transition = 'all 0.5s ease-out';
    el.style.transform = 'scale(1.3)';
    el.style.filter = 'brightness(2)';
    el.style.opacity = '0';
    setTimeout(() => {
      el.style.transform = '';
      el.style.filter = '';
      el.style.opacity = '';
      resolve();
    }, 600);
  });
}

/** 胜利特效: 文本弹跳+金色粒子 */
export function victoryEffect(container: HTMLElement): Promise<void> {
  return new Promise(resolve => {
    const cx = container.offsetWidth / 2;
    const cy = container.offsetHeight / 3;

    // 金色粒子爆发 x3
    for (let i = 0; i < 3; i++) {
      setTimeout(() => {
        spawnParticles(
          container,
          cx + (Math.random() - 0.5) * 100,
          cy + (Math.random() - 0.5) * 60,
          30 + Math.random() * 20,
          'light'
        );
      }, i * 150);
    }

    // 白色闪屏三重
    for (let i = 0; i < 3; i++) {
      setTimeout(() => whiteFlash(container), i * 100);
    }

    // 震动
    container.style.animation = 'ba-shake-big 0.6s ease-out';
    setTimeout(() => { container.style.animation = ''; }, 700);

    resolve();
  });
}

/** 失败特效 */
export function defeatEffect(container: HTMLElement): Promise<void> {
  return new Promise(resolve => {
    const overlay = document.createElement('div');
    overlay.style.cssText = `
      position: absolute; inset: 0; z-index: 190; pointer-events: none; border-radius: inherit;
      background: radial-gradient(circle, rgba(239,68,68,0.2), transparent 60%);
      opacity: 0;
      transition: opacity 0.5s ease-out;
    `;
    container.appendChild(overlay);
    requestAnimationFrame(() => { overlay.style.opacity = '1'; });

    // 暗色粒子
    const cx = container.offsetWidth / 2;
    const cy = container.offsetHeight / 2;
    spawnParticles(container, cx, cy, 25, 'shadow');

    setTimeout(() => {
      overlay.style.opacity = '0';
      setTimeout(() => overlay.remove(), 500);
    }, 800);

    resolve();
  });
}

/** 抽牌动画: 卡牌从牌堆飞入手牌区 */
export function cardDrawAnimation(
  container: HTMLElement,
  handArea: HTMLElement,
  cardContent: string
): Promise<void> {
  return new Promise(resolve => {
    const handRect = handArea.getBoundingClientRect();
    const containerRect = container.getBoundingClientRect();

    // 从牌堆位置开始(右上角)
    const startX = containerRect.width - 60;
    const startY = 10;

    const card = document.createElement('div');
    card.style.cssText = `
      position: absolute; z-index: 300; pointer-events: none;
      left: ${startX}px; top: ${startY}px;
      width: 50px; height: 70px;
      background: linear-gradient(145deg, #1e293b, #2d2d3d);
      border: 2px solid #475569;
      border-radius: 5px;
      display: flex; align-items: center; justify-content: center;
      font-size: 20px;
      transition: all 0.35s cubic-bezier(0.22, 1, 0.36, 1);
      box-shadow: 0 0 20px rgba(45,212,191,0.3);
    `;
    card.textContent = cardContent || '🃏';
    container.appendChild(card);

    // 目标: 手牌区左侧
    const targetX = handRect.left - containerRect.left + 10;
    const targetY = handRect.top - containerRect.top + handRect.height / 2 - 35;

    requestAnimationFrame(() => {
      card.style.left = targetX + 'px';
      card.style.top = targetY + 'px';
      card.style.transform = 'scale(1) rotateZ(0deg)';
      card.style.boxShadow = '0 0 30px rgba(45,212,191,0.5)';
      card.style.borderColor = '#2dd4bf';
    });

    setTimeout(() => {
      card.style.transition = 'all 0.2s ease-out';
      card.style.opacity = '0';
      setTimeout(() => {
        card.remove();
        resolve();
      }, 250);
    }, 400);
  });
}

/** 英雄技能: 全屏覆盖层+粒子爆发 */
export function heroPowerEffect(container: HTMLElement, elementType: string = 'fire') {
  elementFlash(container, elementType);
  const cx = container.offsetWidth / 2;
  const cy = container.offsetHeight / 2;
  spawnParticles(container, cx, cy, 40, elementType);

  // 额外中心爆发
  setTimeout(() => {
    spawnParticles(container, cx, cy, 20, elementType);
  }, 200);
}

/** 随从冲锋动画 */
export function minionChargeAnimation(
  minionEl: HTMLElement,
  damage: number
): Promise<void> {
  return new Promise(resolve => {
    // 向前冲锋
    minionEl.style.transition = 'all 0.2s cubic-bezier(0.22, 1, 0.36, 1)';
    minionEl.style.transform = 'translateY(-20px) scale(1.1)';
    minionEl.style.boxShadow = '0 0 20px rgba(255,165,0,0.6)';
    minionEl.style.borderColor = '#f59e0b';

    // 回位+震动
    setTimeout(() => {
      minionEl.style.transition = 'all 0.15s ease-out';
      minionEl.style.transform = '';
      minionEl.style.boxShadow = '';
      minionEl.style.borderColor = '';

      // 受击结果震动
      const intensity = getShakeIntensity(damage);
      minionEl.style.animation = `ba-minion-recoil ${intensity === 'big' ? 0.4 : 0.25}s ease-out`;
      setTimeout(() => {
        minionEl.style.animation = '';
        resolve();
      }, 500);
    }, 300);
  });
}
