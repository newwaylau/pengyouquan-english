# P1 — 宝箱系统 + 星尘分解合成 + 开包动画

> 项目路径：`/home/heaton/pengyouquan-english`
> Git 分支：`sit`
> 所有中文内容必须正确 UTF-8 编码
> Spring Boot + React + MySQL + Docker 部署到 SIT

---

## 任务 1：宝箱系统（V21 迁移）

### 1.1 新增表 `user_chests`（Flyway V21）

```sql
CREATE TABLE user_chests (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  user_id BIGINT NOT NULL,
  chest_type VARCHAR(20) NOT NULL COMMENT 'bronze/silver/gold',
  status VARCHAR(20) NOT NULL DEFAULT 'locked' COMMENT 'locked/unlocking/ready/claimed',
  unlock_progress INT NOT NULL DEFAULT 0 COMMENT '当前已练句数',
  unlock_required INT NOT NULL COMMENT '需要练的句数: bronze=10, silver=25, gold=50',
  source VARCHAR(50) NOT NULL COMMENT '来源: pvp_battle/arena_win/season_reward',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  INDEX idx_user_status (user_id, status)
);
```

### 1.2 宝箱规则

| 类型 | 获取条件 | 解锁需要 | 奖励 |
|------|----------|----------|------|
| 🥉 青铜 | 赢1场对战 | 练10句 | 1稀有+2普通 |
| 🥈 白银 | 赢2场对战 | 练25句 | 1史诗+3随机 |
| 🥇 黄金 | 赢3场(连胜奖励) | 练50句 | 1传说(30%)+5随机 |

- 同类型宝箱最多持有 **3个**（防止囤积）
- 宝箱槽位最多 **4个**（3个自己 + 1个赛季）
- 解锁进度在用户每次完成听写时更新（复用现有 `PracticePage` 的完成回调）

### 1.3 后端改动

#### Model: `UserChest.java`
```java
@Entity @Table(name = "user_chests")
public class UserChest {
  @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;
  @Column(nullable = false) private Long userId;
  @Column(nullable = false) private String chestType; // bronze/silver/gold
  @Column(nullable = false) private String status; // locked/unlocking/ready/claimed
  private int unlockProgress;
  private int unlockRequired;
  private String source;
  private LocalDateTime createdAt;
  private LocalDateTime updatedAt;
}
```

#### Repository: `UserChestRepository.java`
- `findByUserIdAndStatus(Long userId, String status)` — 查待开宝箱
- `countByUserIdAndStatusAndChestType(Long userId, String status, String chestType)` — 检查同类型数量
- `findTopByUserIdAndStatusOrderByCreatedAt(Long userId, String status)` — 取最早待开

#### Service: `ChestService.java`
- `grantBattleChest(Long userId, String source)` — 赢对战后发放宝箱
  - 青铜：赢≥1场即可
  - 白银：赢≥2场（检查最近战斗记录）
  - 黄金：3连胜以上（检查最近战斗记录中的连续胜场）
  - 检查同类型槽位 ≤ 3，总槽位 ≤ 4
- `progressChest(Long userId, int sentencesDone)` — 练听写后推进解锁进度
  - 查所有 status='unlocking' 的宝箱
  - 每个宝箱 +sentencesDone 进度
  - 达到 unlock_required → 状态改为 'ready'
- `claimChest(Long chestId, Long userId)` — 领取宝箱奖励
  - 检查 status='ready'
  - 根据 chest_type 调用 CardService.grantPack 发卡
  - 支持返回新卡列表供前端展示
  - 状态改为 'claimed'
- `getChests(Long userId)` — 返回该用户所有宝箱及状态
- `checkAndOpenDailyChest(Long userId)` — 每日首次练听写时自动发放每日宝箱（可选）

#### Controller: `ChestController.java`
- `GET /api/chests` — 用户的所有宝箱
- `POST /api/chests/claim/{chestId}` — 领取宝箱
- `POST /api/chests/progress` — 推进解锁（内部调用，也可由 PracticePage 完成后自动调）
- `GET /api/chests/rewards/{chestId}` — 查看宝箱内奖励（领取后展示）

#### 修改 `BattleService.java`
- 异步对战或实时对战胜利后，调用 `ChestService.grantBattleChest`

#### 修改 `PracticePage` 后端
- 听写完成后，调用 `ChestService.progressChest`

### 1.4 前端改动

#### 新建 `frontend/src/ChestPanel.tsx`
宝箱面板组件，可嵌入 CardCollectionPage 或 BattlePage 作为顶部横条：

- 显示 4 个宝箱槽位
- 每个宝箱卡片显示：类型图标 + 解锁进度条
- 进度条：`progress/required` 百分比
- 点击宝箱：弹出详情
  - 锁定中：显示"还需练 N 句"
  - 解锁中：显示进度 + "继续练习解锁"
  - 已就绪：显示"领取"按钮 + 预览奖励
  - 已领取：显示"已领取"标记

#### 开包动画复用
- 领取宝箱后，后端返回新卡列表
- 前端弹出开包动画（任务3）展示新卡

#### 在 `BattlePage.tsx` 顶部嵌入 ChestPanel
```tsx
// BattlePage.tsx 顶部
<div className="chest-bar">
  <ChestPanel user={user} onChestOpened={loadAll} />
</div>
```

#### 在 `CardCollectionPage.tsx` 嵌入ChestPanel
卡册页面第一页（卡册标签）顶部也显示宝箱进度

---

## 任务 2：星尘分解/合成系统

### 2.1 新增列和表（V22 迁移）

```sql
-- 用户表加星尘字段
ALTER TABLE users ADD COLUMN stardust INT NOT NULL DEFAULT 0 COMMENT '星尘数量';

-- 卡牌分解日志（可选，用于防误操作回退）
CREATE TABLE stardust_logs (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  user_id BIGINT NOT NULL,
  card_id BIGINT NOT NULL,
  card_name VARCHAR(100),
  action VARCHAR(20) NOT NULL COMMENT 'disenchant/craft',
  stardust_amount INT NOT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  INDEX idx_user (user_id)
);
```

### 2.2 经济规则

| 稀有度 | 分解得星尘 | 合成需星尘 |
|--------|-----------|-----------|
| 普通 | 5 | 40 |
| 稀有 | 20 | 160 |
| 史诗 | 100 | 800 |
| 传说 | 400 | 3200 |

- 分解一次只能拆1张（不能批量）
- 合成可以指定任意已存在的卡牌（从cards表）
- 合成后自动插入 user_cards

### 2.3 后端改动

#### `CardService.java` 新增方法
- `disenchantCard(Long userId, Long cardId)` — 分解单张卡牌
  - 检查用户确有该卡（user_cards 有记录）
  - 减少 user_cards 数量（如果没有剩余次数则删行）
  - 增加用户 stardust
  - 记录 stardust_logs
- `craftCard(Long userId, Long cardId)` — 合成指定卡牌
  - 检查 cards 表存在该卡
  - 检查用户 stardust 是否足够
  - 扣除 stardust
  - 新增 user_cards 记录
  - 记录 stardust_logs
- `getStardust(Long userId)` — 返回用户星尘数

#### `CardController.java` 新增端点
- `POST /api/cards/disenchant` — {cardId} → 分解
- `POST /api/cards/craft` — {cardId} → 合成
- `GET /api/cards/stardust` — 获取星尘数量

### 2.4 前端改动

#### 在 `CardCollectionPage.tsx` 卡册标签页

- 每张卡牌增加一个 **分解按钮**（⛏️图标），点击弹出确认
- 顶部显示用户星尘余额 ✨
- 卡册新增 **"可合成"标签** 或页面底部 **"合成"入口**
  - 显示所有未拥有的卡牌
  - 每张显示合成所需星尘
  - 点击合成按钮调用 API

#### 分解流程
1. 用户点击卡牌上的"分解"图标
2. 弹出确认框："分解后将获得 N 星尘，确定分解？"
3. 确认 → 调 API → 更新界面
4. 动画：卡牌化为星尘飘散（CSS简单动画即可）

#### 合成流程
1. 用户进入合成页面（卡册新标签页 "✨ 合成"）
2. 按稀有度筛选所有可合成卡牌（未拥有的优先显示）
3. 每张卡牌显示：名称 + 稀有度 + 星尘价格
4. 点击→确认对话框→调API→卡牌飞入收藏

---

## 任务 3：开包动画

### 3.1 触发场景
1. 听写完成发卡包后
2. 领取宝箱后
3. 赛季奖励发放后

### 3.2 前端组件：`PackOpeningModal.tsx`

```tsx
// 弹窗全屏覆盖，背景半透明黑色
// 步骤：
// 1. 宝箱/卡包在屏幕中央旋转
// 2. 点击/等待 → 卡包打开，光芒四射
// 3. 卡牌一张张飞出（有延迟递进，配合音效模拟feel）
// 4. 稀有/史诗/传说分别有不同光效
//    - 普通：白色微光
//    - 稀有：蓝色光芒
//    - 史诗：紫色光芒 + 粒子
//    - 传说：橙色光芒 + 金色粒子 + 屏幕震动
// 5. 最后展示获得的全部卡牌，可点击查看详情
// 6. 点击"确认"关闭模态框
```

#### Props
```tsx
interface PackOpeningProps {
  cards: CardResponse[];  // 新获得的卡牌
  onClose: () => void;
  packType?: 'bronze' | 'silver' | 'gold' | 'practice';
}
```

#### CSS 动画
- 使用 CSS keyframes，不依赖任何动画库
- 卡牌飞入：从中心向四周散开，带弹跳效果
- 传说话：金色粒子用 CSS 伪元素 + radial-gradient
- 光效：叠加多层 box-shadow + filter: blur

### 3.3 集成到现有页面

#### `PracticePage.tsx`
- 听写完成收到 grantPack 返回结果后
- 如果有新卡（卡片列表非空）→ 弹出 `PackOpeningModal`
- 现有弹窗逻辑改为调用此模态框

#### `ChestPanel.tsx` (任务1)
- 领取宝箱成功后 → 弹出 `PackOpeningModal` 展示奖励

---

## 技术约束

1. **Docker 部署**：所有代码需构建 Docker 镜像并重新部署
2. **Flyway 迁移**：创建 V21__add_chest_system.sql 和 V22__add_stardust.sql
3. **后端**：保持现有 package 结构（model/repository/service/controller）
4. **前端**：React + TypeScript，使用现有 CSS 变量（`var(--teal)`, `var(--gold)` 等）
5. **样式**：暗色主题风格，保持与现有卡牌页面一致
6. **无需新增底部导航项** — 宝箱嵌入现有页面，星尘作为卡牌页面功能

## 执行顺序

1. ✅ 任务1（宝箱系统）— V21 迁移 → Model → Repository → Service → Controller → ChestPanel 前端
2. ✅ 任务2（星尘系统）— V22 迁移 → CardService 新增 → 前端
3. ✅ 任务3（开包动画）— PackOpeningModal → 集成到任务1+2
4. ✅ 构建 Docker 部署到 SIT
5. ✅ git commit + push

## 验证

1. 登录 SIT → 进入对战 → 赢一场 → 检查宝箱栏出现青铜宝箱
2. 练听写 10 句 → 检查宝箱解锁进度 + 变为 ready
3. 领取宝箱 → 弹出开包动画 → 卡牌增加
4. 进入卡册 → 分解一张卡 → 检查星尘增加
5. 合成一张新卡 → 检查星尘减少 + 卡牌出现
6. 多赢几场测试白银/黄金宝箱条件
