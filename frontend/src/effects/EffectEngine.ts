/** 动画引擎核心 — Canvas粒子系统 */

/* ─────────── 颜色方案类型 ─────────── */
export type ParticleStyle = 'fire' | 'ice' | 'shadow' | 'light' | 'nature' | 'metal';

export interface ColorPalette {
  primary: string;
  secondary: string;
  trail: string;
  glow: string;
}

export const COLOR_PALETTES: Record<ParticleStyle, ColorPalette> = {
  fire:   { primary: '#ff4500', secondary: '#ff8c00', trail: 'rgba(255,69,0,0.3)', glow: '#ff4500' },
  ice:    { primary: '#00bfff', secondary: '#e0ffff', trail: 'rgba(0,191,255,0.3)', glow: '#00bfff' },
  shadow: { primary: '#8b00ff', secondary: '#dda0dd', trail: 'rgba(139,0,255,0.3)', glow: '#8b00ff' },
  light:  { primary: '#ffd700', secondary: '#fffacd', trail: 'rgba(255,215,0,0.3)', glow: '#ffd700' },
  nature: { primary: '#32cd32', secondary: '#98fb98', trail: 'rgba(50,205,50,0.3)', glow: '#32cd32' },
  metal:  { primary: '#a9a9a9', secondary: '#dcdcdc', trail: 'rgba(169,169,169,0.3)', glow: '#a9a9a9' },
};

/* ─────────── 粒子类 ─────────── */
export class Spark {
  x: number;
  y: number;
  vx: number;
  vy: number;
  life: number;
  maxLife: number;
  decay: number;
  color: string;
  trailColor: string;
  size: number;
  /** 拖尾历史 */
  trail: Array<{ x: number; y: number }>;
  maxTrail: number;

  constructor(
    x: number,
    y: number,
    vx: number,
    vy: number,
    life: number,
    decay: number,
    color: string,
    trailColor: string,
    size: number,
  ) {
    this.x = x;
    this.y = y;
    this.vx = vx;
    this.vy = vy;
    this.life = life;
    this.maxLife = life;
    this.decay = decay;
    this.color = color;
    this.trailColor = trailColor;
    this.size = size;
    this.trail = [];
    this.maxTrail = 5;
  }

  /** 更新位置、生命、拖尾 */
  update(): void {
    this.trail.push({ x: this.x, y: this.y });
    if (this.trail.length > this.maxTrail) {
      this.trail.shift();
    }
    this.x += this.vx;
    this.y += this.vy;
    this.vy += 0.05; // 重力
    this.vx *= 0.98; // 摩擦
    this.vy *= 0.98;
    this.life -= this.decay;
  }

  /** 是否存活 */
  get alive(): boolean {
    return this.life > 0;
  }

  /** 生命比例 */
  get lifeRatio(): number {
    return Math.max(0, this.life / this.maxLife);
  }
}

/* ─────────── 粒子管理器 ─────────── */
export class ParticleManager {
  particles: Spark[] = [];
  private animFrameId: number | null = null;
  private canvas: HTMLCanvasElement | null = null;
  private ctx: CanvasRenderingContext2D | null = null;

  /** 挂载到 canvas */
  mount(canvas: HTMLCanvasElement): void {
    this.canvas = canvas;
    this.ctx = canvas.getContext('2d')!;
    this.resize();
    window.addEventListener('resize', this.resize);
  }

  /** 卸载 */
  unmount(): void {
    this.stop();
    window.removeEventListener('resize', this.resize);
    this.canvas = null;
    this.ctx = null;
  }

  private resize = (): void => {
    if (!this.canvas) return;
    this.canvas.width = this.canvas.offsetWidth * devicePixelRatio;
    this.canvas.height = this.canvas.offsetHeight * devicePixelRatio;
    this.canvas.style.width = `${this.canvas.offsetWidth}px`;
    this.canvas.style.height = `${this.canvas.offsetHeight}px`;
  };

  /** 爆发粒子 */
  burstEffect(
    cx: number,
    cy: number,
    count: number,
    style: ParticleStyle = 'fire',
  ): void {
    const pal = COLOR_PALETTES[style];
    for (let i = 0; i < count; i++) {
      const angle = (Math.PI * 2 * i) / count + (Math.random() - 0.5) * 0.5;
      const speed = 1 + Math.random() * 4;
      const size = 2 + Math.random() * 4;
      const life = 30 + Math.random() * 40;
      const decay = 0.5 + Math.random() * 1.5;
      const color = Math.random() > 0.5 ? pal.primary : pal.secondary;
      this.particles.push(
        new Spark(
          cx, cy,
          Math.cos(angle) * speed,
          Math.sin(angle) * speed,
          life, decay, color, pal.trail, size,
        ),
      );
    }
  }

  /** 单一方向粒子流 (用于稀有度光柱等) */
  streamEffect(
    cx: number,
    cy: number,
    angle: number,
    count: number,
    style: ParticleStyle = 'light',
  ): void {
    const pal = COLOR_PALETTES[style];
    for (let i = 0; i < count; i++) {
      const spread = (Math.random() - 0.5) * 0.3;
      const speed = 0.5 + Math.random() * 2;
      const size = 1 + Math.random() * 2;
      const life = 20 + Math.random() * 30;
      const decay = 0.3 + Math.random() * 0.8;
      const color = Math.random() > 0.5 ? pal.primary : pal.secondary;
      this.particles.push(
        new Spark(
          cx + (Math.random() - 0.5) * 10,
          cy + (Math.random() - 0.5) * 10,
          Math.cos(angle) * speed,
          Math.sin(angle) * speed,
          life, decay, color, pal.trail, size,
        ),
      );
    }
  }

  /** 弹跳粒子 (用于野兽种族) */
  bounceEffect(
    cx: number,
    cy: number,
    count: number,
    style: ParticleStyle = 'nature',
  ): void {
    const pal = COLOR_PALETTES[style];
    for (let i = 0; i < count; i++) {
      const angle = (Math.PI * 2 * i) / count;
      const speed = 2 + Math.random() * 3;
      const size = 3 + Math.random() * 3;
      const life = 40 + Math.random() * 30;
      const decay = 0.4 + Math.random() * 0.6;
      const color = Math.random() > 0.5 ? pal.primary : pal.secondary;
      const s = new Spark(
        cx, cy,
        Math.cos(angle) * speed,
        -Math.abs(Math.sin(angle) * speed), // 向上弹
        life, decay, color, pal.trail, size,
      );
      s.maxTrail = 3;
      this.particles.push(s);
    }
  }

  /** 启动渲染循环 */
  start(): void {
    if (this.animFrameId !== null) return;
    const loop = (): void => {
      this.update();
      this.render();
      this.animFrameId = requestAnimationFrame(loop);
    };
    this.animFrameId = requestAnimationFrame(loop);
  }

  /** 停止渲染循环 */
  stop(): void {
    if (this.animFrameId !== null) {
      cancelAnimationFrame(this.animFrameId);
      this.animFrameId = null;
    }
    this.particles = [];
  }

  private update(): void {
    for (let i = this.particles.length - 1; i >= 0; i--) {
      this.particles[i].update();
      if (!this.particles[i].alive) {
        this.particles.splice(i, 1);
      }
    }
  }

  private render(): void {
    const { ctx, canvas } = this;
    if (!ctx || !canvas) return;

    ctx.clearRect(0, 0, canvas.width, canvas.height);
    const scale = devicePixelRatio || 1;

    for (const p of this.particles) {
      // 拖尾
      for (let t = 0; t < p.trail.length; t++) {
        const alpha = (t / p.trail.length) * p.lifeRatio * 0.5;
        ctx.beginPath();
        ctx.arc(p.trail[t].x * scale, p.trail[t].y * scale, p.size * 0.5 * (t / p.trail.length), 0, Math.PI * 2);
        ctx.fillStyle = p.trailColor.replace(')', `,${alpha})`).replace('rgba', 'rgba');
        ctx.fill();
      }

      // 主体
      ctx.beginPath();
      ctx.arc(p.x * scale, p.y * scale, p.size * p.lifeRatio, 0, Math.PI * 2);
      ctx.fillStyle = p.color;
      ctx.globalAlpha = p.lifeRatio;
      ctx.fill();

      // 发光
      ctx.beginPath();
      ctx.arc(p.x * scale, p.y * scale, p.size * p.lifeRatio * 2, 0, Math.PI * 2);
      ctx.fillStyle = p.trailColor.replace(')', `,${p.lifeRatio * 0.3})`).replace('rgba', 'rgba');
      ctx.fill();

      ctx.globalAlpha = 1;
    }
  }
}
