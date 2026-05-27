// ====== GaspAnimations — GSAP 驱动的战斗动画系统 ======
// 替换 EffectEngine.ts 的 setTimeout 链式管理
// 物理缓动: elastic.out / back.out / bounce.out / power3.out

import { getShakeIntensity, getDamageFontSize, ELEMENT_STYLES } from '../AnimProfileBuilder';
import gsap from 'gsap';

/** 在战场容器上创建闪屏覆盖层 */
export function elementFlash(container: HTMLElement, elementType: string) {
  const overlay = document.createElement('div');
  overlay.style.cssText = `
    position: absolute; inset: 0; z-index: 200; pointer-events: none; border-radius: inherit;
    background: radial-gradient(circle, ${getFlashColor(elementType)} 0%, transparent 60%);
    opacity: 0;
  `;
  container.appendChild(overlay);

  gsap.to(overlay, {
    opacity: 1,
    duration: 0.15,
    ease: 'power2.out',
    onComplete: () => {
      gsap.to(overlay, {
        opacity: 0,
        duration: 0.4,
        delay: 0.05,
        ease: 'power2.out',
        onComplete: () => overlay.remove(),
      });
    },
  });
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
  flash.style.cssText = `
    position: absolute; inset: 0; z-index: 250; pointer-events: none; border-radius: inherit;
    background: white; opacity: 0;
  `;
  container.appendChild(flash);

  gsap.to(flash, {
    opacity: 0.7,
    duration: 0.08,
    ease: 'power2.out',
    onComplete: () => {
      gsap.to(flash, {
        opacity: 0,
        duration: 0.15,
        ease: 'power2.out',
        onComplete: () => flash.remove(),
      });
    },
  });
}

/** 卡牌出牌动画: 弧线飞向战场中心 + 弹性着陆 */
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
    const startX = rect.left - containerRect.left;
    const startY = rect.top - containerRect.top;
    clone.style.cssText = `
      position: absolute;
      z-index: 300;
      margin: 0;
      left: ${startX}px;
      top: ${startY}px;
      width: ${rect.width}px;
      height: ${rect.height}px;
      pointer-events: none;
      border-radius: ${getComputedStyle(cardEl).borderRadius || '6px'};
      box-shadow: 0 0 20px ${color};
      border: 2px solid ${color};
    `;
    container.appendChild(clone);
    cardEl.style.visibility = 'hidden';

    // 目标: 战场中心
    const cx = containerRect.width / 2 - rect.width / 2;
    const cy = containerRect.height / 2 - rect.height / 2;

    // 弧线路径：先上弧再飞入中心
    const midX = (startX + cx) / 2;
    const midY = Math.min(startY, cy) - 60;

    const tl = gsap.timeline({
      onComplete: () => {
        clone.remove();
        cardEl.style.visibility = 'visible';
        resolve();
      },
    });

    // 弧线飞入
    tl.to(clone, {
      x: cx - startX,
      y: cy - startY,
      scale: 1.15,
      duration: 0.35,
      ease: 'power3.out',
    })
    // 弹性着陆 + 收缩
    .to(clone, {
      scale: 0.4,
      opacity: 0,
      duration: 0.25,
      ease: 'power2.in',
    });
  });
}

/** 伤害数字弹出 — 弹跳上浮 + 渐隐 */
export function showDamageNumber(
  container: HTMLElement,
  x: number,
  y: number,
  value: number,
  color: string,
  fontSize?: number
) {
  const el = document.createElement('div');
  const size = fontSize || getDamageFontSize(value);
  const sign = value > 0 ? `-${value}` : `${value}`;
  el.textContent = sign;
  el.style.cssText = `
    position: absolute; z-index: 180; pointer-events: none;
    font-weight: 900; color: ${color};
    text-shadow: 0 0 15px ${color}, 0 0 30px ${color}40;
    left: ${x}px; top: ${y}px;
    font-size: ${size}px;
  `;
  container.appendChild(el);

  gsap.fromTo(el,
    { y: 0, opacity: 1, scale: 0.5 },
    {
      y: -50,
      opacity: 0,
      scale: 1.2,
      duration: 0.9,
      ease: 'power2.out',
      onComplete: () => el.remove(),
    }
  );
}

/** 受击后仰 + 闪烁 */
export function hitAnimation(el: HTMLElement, intensity: 'light' | 'mid' | 'big' = 'mid') {
  const duration = intensity === 'big' ? 0.6 : intensity === 'mid' ? 0.4 : 0.3;

  gsap.timeline()
    .to(el, {
      x: -5,
      rotation: -3,
      scaleX: 0.95,
      duration: duration * 0.3,
      ease: 'power2.out',
    })
    .to(el, {
      x: 3,
      rotation: 2,
      scaleX: 1.02,
      duration: duration * 0.3,
      ease: 'power1.out',
    })
    .to(el, {
      x: 0,
      rotation: 0,
      scaleX: 1,
      duration: duration * 0.4,
      ease: 'elastic.out(1, 0.4)',
    });
}

/** 屏幕震动 */
export function screenShake(container: HTMLElement, damage: number) {
  const intensity = getShakeIntensity(damage);
  const duration = intensity === 'big' ? 0.5 : intensity === 'mid' ? 0.4 : 0.3;
  const xAmount = intensity === 'big' ? 12 : intensity === 'mid' ? 8 : 4;

  gsap.to(container, {
    x: gsap.utils.random(-xAmount, xAmount, 1),
    duration: 0.03,
    repeat: Math.floor(duration * 20),
    yoyo: true,
    ease: 'none',
    onComplete: () => {
      gsap.set(container, { x: 0 });
    },
  });
}

/** 粒子爆发 (DOM + GSAP 驱动) */
export function spawnParticles(
  container: HTMLElement,
  cx: number,
  cy: number,
  count: number,
  elementType: string
) {
  const style = ELEMENT_STYLES[elementType] || ELEMENT_STYLES.fire;
  const fragment = document.createDocumentFragment();

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
      opacity: 1;
    `;
    fragment.appendChild(p);

    // GSAP 驱动粒子运动
    const px = dx;
    const py = dy;
    const finalScale = 0.2 + Math.random() * 0.5;

    gsap.to(p, {
      x: px,
      y: py,
      scale: finalScale,
      opacity: 0,
      duration: 0.4 + Math.random() * 0.4,
      ease: 'power2.out',
      delay: Math.random() * 0.05,
      onComplete: () => p.remove(),
    });
  }

  container.appendChild(fragment);
}

/** 敌人死亡: 放大→变亮→消失 */
export function enemyDeathAnimation(el: HTMLElement): Promise<void> {
  return new Promise(resolve => {
    gsap.to(el, {
      scale: 1.3,
      filter: 'brightness(2)',
      opacity: 0,
      duration: 0.5,
      ease: 'power2.out',
      onComplete: () => {
        el.style.transform = '';
        el.style.filter = '';
        el.style.opacity = '';
        resolve();
      },
    });
  });
}

/** 胜利特效: 金色粒子 ×3 + 白闪 ×3 + 震动 */
export function victoryEffect(container: HTMLElement): Promise<void> {
  return new Promise(resolve => {
    const cx = container.offsetWidth / 2;
    const cy = container.offsetHeight / 3;

    // 金色粒子爆发 x3
    for (let i = 0; i < 3; i++) {
      gsap.delayedCall(i * 0.15, () => {
        spawnParticles(
          container,
          cx + (Math.random() - 0.5) * 100,
          cy + (Math.random() - 0.5) * 60,
          30 + Math.random() * 20,
          'light'
        );
      });
    }

    // 白色闪屏三重
    gsap.timeline()
      .call(() => whiteFlash(container))
      .call(() => whiteFlash(container), undefined, 0.1)
      .call(() => whiteFlash(container), undefined, 0.2);

    // 震动
    gsap.to(container, {
      x: 8,
      duration: 0.03,
      repeat: 20,
      yoyo: true,
      ease: 'none',
      onComplete: () => gsap.set(container, { x: 0 }),
    });

    gsap.delayedCall(0.7, resolve);
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
    `;
    container.appendChild(overlay);

    gsap.to(overlay, {
      opacity: 1,
      duration: 0.5,
      ease: 'power2.out',
      onComplete: () => {
        gsap.to(overlay, {
          opacity: 0,
          duration: 0.5,
          delay: 0.3,
          ease: 'power2.out',
          onComplete: () => overlay.remove(),
        });
      },
    });

    const cx = container.offsetWidth / 2;
    const cy = container.offsetHeight / 2;
    spawnParticles(container, cx, cy, 25, 'shadow');

    gsap.delayedCall(1.2, resolve);
  });
}

/** 抽牌动画: 牌堆→手牌弧线飞入 */
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
      box-shadow: 0 0 20px rgba(45,212,191,0.3);
      opacity: 0;
    `;
    card.textContent = cardContent || '🃏';
    container.appendChild(card);

    // 目标: 手牌区左侧
    const targetX = handRect.left - containerRect.left + 10;
    const targetY = handRect.top - containerRect.top + handRect.height / 2 - 35;
    const midX = (startX + targetX) / 2;
    const midY = Math.min(startY, targetY) - 40;

    // 弧线飞入: 先上弧再吸附
    gsap.timeline({ onComplete: resolve })
      .set(card, { opacity: 1, scale: 0.3, rotation: -10 })
      .to(card, {
        x: targetX - startX,
        y: targetY - startY,
        scale: 1,
        rotation: 0,
        duration: 0.35,
        ease: 'power3.out',
        boxShadow: '0 0 30px rgba(45,212,191,0.5)',
        borderColor: '#2dd4bf',
      })
      .to(card, {
        opacity: 0,
        scale: 0.8,
        duration: 0.15,
        ease: 'power1.in',
        onComplete: () => card.remove(),
      });
  });
}

/** 英雄技能: 全屏覆盖层+粒子爆发 */
export function heroPowerEffect(container: HTMLElement, elementType: string = 'fire') {
  elementFlash(container, elementType);
  const cx = container.offsetWidth / 2;
  const cy = container.offsetHeight / 2;
  spawnParticles(container, cx, cy, 40, elementType);

  gsap.delayedCall(0.2, () => {
    spawnParticles(container, cx, cy, 20, elementType);
  });
}

/** 随从冲锋动画 */
export function minionChargeAnimation(
  minionEl: HTMLElement,
  damage: number
): Promise<void> {
  return new Promise(resolve => {
    const tl = gsap.timeline({ onComplete: resolve });

    // 向前冲锋
    tl.to(minionEl, {
      y: -20,
      scale: 1.1,
      boxShadow: '0 0 20px rgba(255,165,0,0.6)',
      borderColor: '#f59e0b',
      duration: 0.2,
      ease: 'power2.out',
    })
    // 回位弹性
    .to(minionEl, {
      y: 0,
      scale: 1,
      boxShadow: '',
      borderColor: '',
      duration: 0.15,
      ease: 'elastic.out(1, 0.4)',
    })
    // 受击结果震动
    .to(minionEl, {
      x: damage >= 8 ? 5 : 3,
      duration: 0.04,
      repeat: damage >= 8 ? 5 : 3,
      yoyo: true,
      ease: 'none',
      onComplete: () => {
        gsap.set(minionEl, { x: 0 });
      },
    });
  });
}
