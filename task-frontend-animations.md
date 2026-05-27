# Frontend P3 — 动画优化

## 项目路径
- 前端: /home/heaton/pengyouquan-english/frontend

## 先安装 GSAP
cd /home/heaton/pengyouquan-english/frontend && npm install gsap

## 任务14: GSAP 引入 — 替换 EffectEngine.ts setTimeout 链

### 新建文件: src/effects/GaspAnimations.ts

创建一个 GSAP 动画工具模块，替代 EffectEngine 中的 `setTimeout` 链式管理。

核心思路：
- 所有动画使用 `gsap.timeline()` 编排连续动画
- 物理缓动：`ease: "elastic.out(1, 0.3)"`, `"back.out(2)"`, `"bounce.out"`
- 动画完成后自动清理 DOM 节点

需要实现的函数（与 EffectEngine 同名但用 GSAP）：

1. `playCardAnimation(cardEl, container, color)` → GSAP Timeline
   - 卡牌从手牌飞向战场中心
   - 弧线轨迹 + 缩放 + 发光
   - 落地弹性: `ease: "power3.out"`
   
2. `showDamageNumber(container, x, y, value, color, fontSize)` 
   - 伤害数字上浮 + 弹跳 + 渐隐
   - 使用 `ease: "bounce.out"` 弹跳效果
   
3. `hitAnimation(el, intensity)`
   - 受击后仰 + 闪烁
   - GSAP Timeline 编排后仰→恢复→闪烁

4. `screenShake(container, damage)`
   - 屏幕震动
   - gsap.to with x 来回抖动
   
5. `spawnParticles(container, cx, cy, count, elementType)`
   - 保留现有DOM粒子方式，但用 GSAP 驱动每个粒子的运动
   - 粒子从中心向外扩散 + 渐隐

6. `victoryEffect(container)`
   - GSAP Timeline 编排：金色粒子×3（间隔）+ 白闪×3 + 震动

7. `minionChargeAnimation(minionEl, damage)`
   - 向前冲锋 + 光效 + 回位弹性

### 保留 EffectEngine.ts 原有接口
在 BattleArenaPage.tsx 中，导入从 EffectEngine 改为导入 GaspAnimations，但保持函数名一致。

## 任务15: 手牌扇形 + 悬停

### 修改 BattleArenaPage.tsx 的手牌区域

1. **扇形排布**：
   - 手牌以圆弧扇形排布，每张牌有 `rotate` 和 `translateY` 变换
   - 多牌时弧度增大（7-8张弧度大，3-4张弧度小）
   - 使用 GSAP 动态设置 `transform`

2. **悬停效果 (PC)**：
   - 鼠标悬停时牌上浮 80px
   - 缩放至 1.2x
   - 微光描边 + 投影片段
   - GSAP 驱动，`ease: "back.out(2)"` 弹性

3. **点击放大 (手机)**：
   - 单击打开卡牌大图详情
   - 长按拖拽出牌（与 `handlePlayCard` 联动）

### 新建 CSS: src/card-fan.css
- 手牌扇形布局样式
- 手机适配(≤480px) 手牌更紧凑

## 任务16: 关键词专属特效

### 修改 battle-arena.css 和 BattleArenaPage.tsx

关键词特效通过 CSS class + GSAP 实现：

| 关键词 | 样式效果 |
|--------|---------|
| taunt (嘲讽) | 红色脉冲边框呼吸光效 → 添加 `@keyframes taunt-pulse`，使用 box-shadow 呼吸 |
| divine_shield (圣盾) | 金色流光环绕 → 添加 `@keyframes shield-shimmer`，用渐变背景+旋转 animation |
| stealth (潜行) | 半透明虚化(0.6 opacity) + 微光闪烁 |
| rush/charge (突袭/冲锋) | 橙色边框光晕 |
| deathrattle (亡语) | 紫色微光晕 |
| lifesteal (吸血) | 红色脉冲 |
| windfury (风怒) | 蓝色双弧线 |

### 修改 `renderMinionCard` 函数
- 根据关键词添加对应的 CSS class
- 将关键词显示为 icon + 颜色标签

## 任务17: 抽牌/回合过渡动画

### 抽牌动画 (用 GSAP 增强)
在 `cardDrawAnimation` 中加入：
- 牌堆位置（右上角45x63）→ 弧线飞向手牌
- 飞行过程中卡牌旋转 + 淡入
- 弹性吸附

### 回合过渡
- 回合开始：法力水晶填充光流动画
  - 从空到满，每个水晶有 0.1s 延迟逐个填充
- 对手回合（等待时）：
  - 己方手牌半透明（不可操作状态）
  - 棋盘中央显示"对手回合中..."
- 自己回合开始时：
  - 棋盘 flash 动画
  - 手牌恢复不透明

### 计时器
- 保留现有倒计时进度条
- 最后10秒换成urgent红色 + 脉动

## CSS 新增
在 battle-arena.css 或新建 battle-animations.css:
- taunt-pulse keyframes
- shield-shimmer keyframes
- stealth shimmer keyframes
- card draw keyframes
- turn transition keyframes
- 手牌扇形布局
