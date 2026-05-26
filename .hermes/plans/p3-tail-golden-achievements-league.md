# P3 尾料 — 金卡 + 装备重铸套装 + 全服排行 + 成就系统 + 公会联赛

> 项目路径：`/home/heaton/pengyouquan-english`
> Git 分支：`sit`
> 所有中文内容必须正确 UTF-8 编码
> Spring Boot + React + MySQL + Docker 部署到 SIT

---

## 执行顺序（按独立性和工作量排列）

1. ✅ **金卡系统** — 最独立，纯CSS+后端逻辑
2. ✅ **装备重铸+套装效果** — 补全装备系统
3. ✅ **成就系统+全收集统计** — 长期目标
4. ✅ **三模式全服排行"月之王者"** — 赛季扩展
5. ✅ **公会联赛** — 公会扩展

---

## 任务 1：金卡系统

### 1.1 V27 迁移

```sql
-- 金卡配置：哪些卡有金卡版本
ALTER TABLE cards ADD COLUMN has_golden BOOLEAN NOT NULL DEFAULT FALSE COMMENT '是否有金卡版本';

-- user_cards 加 golden 标记
ALTER TABLE user_cards ADD COLUMN is_golden BOOLEAN NOT NULL DEFAULT FALSE COMMENT '是否是金卡';
```

### 1.2 规则

- 只有**史诗和传说**稀有度的卡牌可以有金卡版本
- 金卡 = 动画流光边框 + 卡面轻微辉光 + 额外+1属性
- 获取方式：**仅通过星尘合成**，不能从卡包开出
- 消耗：金卡史诗 = 1600星尘（普通史诗800的两倍），金卡传说 = 6400星尘
- 如果用户已拥有普通版本，合成金卡时自动获得金卡（保有普通版）

### 1.3 后端改动

#### `CardService.java`
- `craftGoldenCard(Long userId, Long cardId)` — 合成金卡
  - 检查卡片支持 golden
  - 检查稀有度（仅史诗/传说）
  - 消耗双倍星尘
  - 设置 user_cards.is_golden = true
- `getGoldenCards(Long userId)` — 用户金卡列表
- 修改现有卡片查询接口：返回 is_golden 字段

#### `CardController.java`
- `POST /api/cards/craft-golden` — {cardId}
- `GET /api/cards/golden` — 金卡列表

### 1.4 前端改动

#### 修改 `CardCollectionPage.tsx` — 金卡显示
- 金卡卡片有：
  - ✅ **动画流光边框** — CSS animation border-image / box-shadow 呼吸光效（金色）
  - ✅ **✨"金"标记** — 卡片右上角金色小星星
  - ✅ **属性值+1** — 显示增强后的数值（用金色数字）
- 卡牌详情弹窗：显示"可升级为金卡"按钮（如果有普通版且星尘够）
- 合成标签页增加"金卡"子标签
- 进度追踪：用户已获得金卡数 / 可合成金卡总数

#### 修改 `PackOpeningModal.tsx`
- 金卡有特殊开包光效：全屏金色粒子+缓慢旋转

---

## 任务 2：装备重铸 + 套装效果

### 2.1 套装效果完整实现

#### V27 迁移补充
```sql
-- 套装配置表
CREATE TABLE equipment_sets (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  set_key VARCHAR(50) NOT NULL UNIQUE COMMENT 'stark/targaryen/downton',
  name_cn VARCHAR(50) NOT NULL,
  two_piece_effect_cn VARCHAR(200) COMMENT '2件套效果',
  five_piece_effect_cn VARCHAR(200) COMMENT '5件套效果（即凑满装备）',
  two_piece_effect_json JSON COMMENT '{\"type\":\"mana_discount\",\"value\":1,\"condition\":\"ice_cards\"}',
  five_piece_effect_json JSON COMMENT '{\"type\":\"first_card_free\"}'
);
```

初始套装数据：
```sql
INSERT IGNORE INTO equipment_sets (set_key, name_cn, two_piece_effect_cn, five_piece_effect_cn, two_piece_effect_json, five_piece_effect_json) VALUES
('stark', '史塔克', '冰系卡费用-1', '每回合首张卡免费', '{"type":"mana_discount","value":1,"condition":"ice_cards"}', '{"type":"first_card_free"}'),
('targaryen', '坦格利安', '传说卡费用-1', '答对大招+3伤害', '{"type":"legendary_cost_reduction","value":1}', '{"type":"ult_damage_bonus","value":3}'),
('downton', '唐顿庄园', '休息节点回血+20%', '事件节点额外选项', '{"type":"rest_heal_bonus","pct":20}', '{"type":"event_extra_option"}');
```

#### 后端 `EquipmentService.java`
- `getActiveSetBonuses(Long userId)` — 计算当前穿戴装备激活的套装效果
  - 遍历 hero_gear 的5个槽位
  - 按 set_key 分组
  - ≥2件 = 2件套效果激活
  - 5件全 = 5件套效果激活
- 套装效果数据通过 equipment 表的 effect_json 中的 "set" 字段关联到 equipment_sets

#### 前端 `EquipmentPage.tsx`
- 顶部显示当前激活的套装效果
- 2件套：半亮图标+文字
- 5件套：全亮图标+文字+特效
- 每个装备卡片显示套装标记（如果属于某套装）

### 2.2 装备重铸

#### V27 迁移补充
```sql
ALTER TABLE user_equipment ADD COLUMN level INT NOT NULL DEFAULT 1 COMMENT '装备等级';
ALTER TABLE user_equipment ADD COLUMN bonus_stats JSON COMMENT '附加属性（重铸可变的属性）';
ALTER TABLE user_equipment ADD COLUMN reroll_count INT NOT NULL DEFAULT 0 COMMENT '已重铸次数';
```

#### 后端 `EquipmentService.java`
- `rerollEquipment(Long userId, Long userEquipmentId)` — 重铸装备附加属性
  - 消耗 200 星尘
  - 随机替换 bonus_stats 中的一条属性
  - reroll_count + 1
  - 稀有装备最多可重铸 10 次，史诗 20 次，传说 30 次
- `upgradeEquipment(Long userId, Long userEquipmentId)` — 升级装备
  - 消耗 100 星尘/级
  - 每5级解锁新词条（普通5级上限，传说20级上限）
  - level + 1，属性提升

#### 前端 `EquipmentPage.tsx`
- 装备详情弹窗增加"重铸"按钮
- 显示当前附加属性和可重铸次数
- 升级按钮 + 进度条
- 升级/重铸花费显示

---

## 任务 3：成就系统

### 3.1 V28 迁移

```sql
CREATE TABLE achievements (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  category VARCHAR(30) NOT NULL COMMENT 'collection/battle/expedition/guild/streak',
  key_name VARCHAR(50) NOT NULL UNIQUE COMMENT '成就唯一标识',
  name_cn VARCHAR(100) NOT NULL,
  description_cn VARCHAR(200) NOT NULL,
  icon VARCHAR(50) DEFAULT '' COMMENT '🎴⚔️🗡️🏰🔥等',
  rarity VARCHAR(20) NOT NULL DEFAULT 'common' COMMENT 'common/rare/epic/legendary',
  condition_type VARCHAR(50) NOT NULL COMMENT 'collect_cards/win_battles/expedition_clear/login_days等',
  condition_value INT NOT NULL COMMENT '达成条件值（如收集50张卡）',
  reward_stardust INT NOT NULL DEFAULT 0,
  reward_card_id BIGINT COMMENT '奖励卡牌(null=星尘奖励)',
  sort_order INT NOT NULL DEFAULT 0
);

CREATE TABLE user_achievements (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  user_id BIGINT NOT NULL,
  achievement_id BIGINT NOT NULL,
  progress INT NOT NULL DEFAULT 0,
  target INT NOT NULL,
  unlocked BOOLEAN NOT NULL DEFAULT FALSE,
  unlocked_at DATETIME,
  UNIQUE KEY uk_user_achievement (user_id, achievement_id)
);
```

### 3.2 初始成就数据（30+个成就）

```sql
INSERT IGNORE INTO achievements (category, key_name, name_cn, description_cn, icon, rarity, condition_type, condition_value, reward_stardust, sort_order) VALUES
-- 收集类
('collection', 'collect_10', '卡牌新手', '收集10张卡牌', '🎴', 'common', 'collect_cards', 10, 50, 1),
('collection', 'collect_30', '卡牌爱好者', '收集30张卡牌', '🎴', 'common', 'collect_cards', 30, 100, 2),
('collection', 'collect_50', '卡牌收藏家', '收集50张卡牌', '🎴', 'rare', 'collect_cards', 50, 200, 3),
('collection', 'collect_70', '全图鉴', '收集全部70张卡牌', '🎴', 'epic', 'collect_cards', 70, 500, 4),
('collection', 'golden_5', '金光闪闪', '拥有5张金卡', '✨', 'rare', 'golden_cards', 5, 300, 5),
('collection', 'golden_10', '金色传说', '拥有10张金卡', '✨', 'epic', 'golden_cards', 10, 600, 6),
('collection', 'legendary_5', '传说集结', '收集5张传说卡牌', '👑', 'rare', 'legendary_cards', 5, 300, 7),
('collection', 'got_full', '凛冬之主', '收集全部GOT卡牌', '🏔️', 'epic', 'show_complete', 1, 500, 8),
('collection', 'da_full', '唐顿伯爵', '收集全部DA卡牌', '🏡', 'epic', 'show_complete', 2, 500, 9),

-- 对战类
('battle', 'win_1', '首胜', '赢得第1场对战', '⚔️', 'common', 'win_battles', 1, 50, 10),
('battle', 'win_10', '十胜勇士', '赢得10场对战', '⚔️', 'common', 'win_battles', 10, 150, 11),
('battle', 'win_50', '百战精英', '赢得50场对战', '⚔️', 'rare', 'win_battles', 50, 400, 12),
('battle', 'win_100', '无双战神', '赢得100场对战', '⚔️', 'epic', 'win_battles', 100, 800, 13),
('battle', 'streak_3', '三连胜', '连续赢得3场对战', '🔥', 'common', 'win_streak', 3, 100, 14),
('battle', 'streak_5', '五连胜', '连续赢得5场对战', '🔥', 'rare', 'win_streak', 5, 250, 15),
('battle', 'streak_10', '十连胜', '连续赢得10场对战', '🔥', 'epic', 'win_streak', 10, 600, 16),
('battle', 'rank_gold', '黄金段位', '达到黄金段位', '🏆', 'rare', 'rank_reach', 3, 300, 17),
('battle', 'rank_diamond', '钻石段位', '达到钻石段位', '🏆', 'epic', 'rank_reach', 5, 500, 18),
('battle', 'rank_legend', '传说段位', '达到传说段位', '🏆', 'legendary', 'rank_reach', 6, 1000, 19),

-- 远征类
('expedition', 'expedition_1', '初次远征', '完成第1次远征', '🗡️', 'common', 'expedition_clear', 1, 100, 20),
('expedition', 'expedition_10', '远征老兵', '完成10次远征', '🗡️', 'rare', 'expedition_clear', 10, 500, 21),
('expedition', 'expedition_50', '远征之王', '完成50次远征', '🗡️', 'epic', 'expedition_clear', 50, 1500, 22),
('expedition', 'boss_kill_20', '弑君者', '击败20个Boss', '🗡️', 'epic', 'boss_kills', 20, 800, 23),
('expedition', 'relic_7', '遗物收藏家', '集齐全部7种遗物', '🪙', 'epic', 'relic_collect', 7, 500, 24),

-- 公会类
('guild', 'join_guild', '加入公会', '加入一个公会', '🏰', 'common', 'join_guild', 1, 100, 25),
('guild', 'guild_war_win', '领地胜利', '在领地战中获胜', '🏰', 'rare', 'guild_war_win', 1, 300, 26),

-- 日常类
('streak', 'login_7', '一周守护', '连续登录7天', '📅', 'rare', 'login_streak', 7, 200, 27),
('streak', 'login_30', '月度常客', '连续登录30天', '📅', 'epic', 'login_streak', 30, 800, 28),
('streak', 'login_365', '年度铁粉', '连续登录365天', '📅', 'legendary', 'login_streak', 365, 5000, 29),
('streak', 'daily_7', '七日挑战', '连续7天完成御前挑战', '⚡', 'rare', 'daily_challenge_streak', 7, 300, 30);
```

### 3.3 后端

#### Models
- `Achievement.java`
- `UserAchievement.java`

#### Repository
- `AchievementRepository.java`
- `UserAchievementRepository.java` — findByUserId, findByUserIdAndUnlockedTrue

#### Service: `AchievementService.java`
- `initUserAchievements(Long userId)` — 新用户注册时初始化所有成就记录
- `checkAchievements(Long userId, String triggerType, int value)` — 通用成就检查入口
  - 触发点：卡片收集完成/对战胜利/远征通关/连续登录/到达段位
  - 遍历该类别的成就，更新 progress
  - 如果达到 condition_value → 解锁
- `getUserAchievements(Long userId)` — 返回所有成就+进度
- `claimAchievementReward(Long userId, Long achievementId)` — 领取成就奖励
- `getCollectionStats(Long userId)` — 全收集统计（卡牌/金卡/遗物等）

#### Controller: `AchievementController.java`
- `GET /api/achievements` — 所有成就+用户进度
- `POST /api/achievements/claim/{achievementId}` — 领取奖励

#### 触发点集成
- `CardService.grantPack()` → 触发 `checkAchievements(userId, 'collect_cards', count)`
- `BattleService.recordBattle()` → 触发 `'win_battles'`, `'win_streak'`
- `RankService` 段位变化 → 触发 `'rank_reach'`
- `ExpeditionService` 通关 → 触发 `'expedition_clear'`, `'boss_kills'`
- `GuildService.joinGuild()` → 触发 `'join_guild'`
- 每日登录 → 触发 `'login_streak'`
- `DailyChallengeService` → 触发 `'daily_challenge_streak'`

### 3.4 前端

#### 新建 `frontend/src/AchievementPage.tsx`
- 成就页面，可从卡牌页面进入（卡牌→成就标签）
- 分类展示：🎴收集 / ⚔️对战 / 🗡️远征 / 🏰公会 / 📅日常
- 每个成就卡片：
  - 图标+名称+描述
  - 进度条（progress/target）
  - 已解锁：金色边框+领取按钮
  - 未解锁：灰色+显示条件
  - 稀有度颜色边框
- 顶部：总成就进度（x/30 个成就解锁）
- 领取时：星尘飞入动画

#### 修改 `CardCollectionPage.tsx`
- 增加"🏆成就"标签页

---

## 任务 4：三模式全服排行"月之王者"

### 4.1 V28 迁移补充

```sql
CREATE TABLE season_rankings (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  user_id BIGINT NOT NULL,
  season_number INT NOT NULL,
  pvp_score INT NOT NULL DEFAULT 0 COMMENT 'PVP奖杯数',
  expedition_score INT NOT NULL DEFAULT 0 COMMENT '远征评分',
  guild_score INT NOT NULL DEFAULT 0 COMMENT '公会贡献评分',
  total_score INT NOT NULL DEFAULT 0 COMMENT '综合评分',
  title VARCHAR(50) COMMENT '称号(月之王者/月之大师等)',
  rank_position INT COMMENT '排名',
  FOREIGN KEY (user_id) REFERENCES users(id),
  INDEX idx_season_score (season_number, total_score)
);
```

### 4.2 规则

- 每月1日结算
- 三模式综合评分 = PVP奖杯数 × 0.5 + 远征最高分 × 0.3 + 公会贡献 × 0.2
- 排名奖励：
  - 🥇 第1名：称号"月之王者" + 传说卡包 + 1000星尘
  - 🥈 第2-3名：称号"月之大师" + 史诗卡包 + 500星尘
  - 🥉 第4-10名：称号"月之勇士" + 稀有卡包 + 300星尘
  - 参与奖：50星尘
- 称号显示在用户头像旁

### 4.3 后端

#### Models
- `SeasonRanking.java`

#### Repository
- `SeasonRankingRepository.java` — findBySeasonNumberOrderByTotalScoreDesc, findByUserIdAndSeasonNumber

#### Service: `SeasonService.java`
- `calculateSeasonRankings(int seasonNumber)` — 计算全服综合排名
- `awardSeasonTitles()` — 发放称号和奖励
- `getCurrentRanking(Long userId)` — 用户当前排名+综合分
- `getTop100()` — TOP100排行

#### Controller 扩展
- `GET /api/season/ranking` — 当前赛季排行
- `GET /api/season/ranking/top100` — TOP100

### 4.4 前端

#### 接口调用
- 在 `BattlePage.tsx` 赛季信息区域增加"综合排行"标签
- 显示TOP10（带称号）
- 用户自己的排名+综合分
- 三模式分数拆解

---

## 任务 5：公会联赛

### 5.1 V28 迁移补充

```sql
CREATE TABLE guild_league_seasons (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  season_number INT NOT NULL,
  start_date DATETIME NOT NULL,
  end_date DATETIME NOT NULL,
  status VARCHAR(20) DEFAULT 'upcoming' COMMENT 'upcoming/active/ended',
  INDEX idx_season (season_number)
);

-- 公会联赛积分
ALTER TABLE guilds ADD COLUMN league_score INT NOT NULL DEFAULT 0;
ALTER TABLE guilds ADD COLUMN league_rank INT DEFAULT 0;
```

### 5.2 规则

- 每月为一个联赛赛季
- 联赛排名基于**公会全成员本周答对总数**（不是实时对战）
- 每周五20:00结算周赛
- 月底总结算
- 奖励：第一名公会全员获得"冠军公会"称号+史诗卡包

### 5.3 后端

#### Models
- `GuildLeagueSeason.java`

#### Repository
- `GuildLeagueSeasonRepository.java`

#### `GuildService.java` 扩展
- `calculateLeagueScores()` — 计算所有公会联赛积分
- `getLeagueStandings()` — 返回联赛排行
- `getGuildLeagueInfo(Long guildId)` — 公会联赛当前排名

#### Controller 扩展
- `GET /api/guilds/league` — 联赛排行
- `GET /api/guilds/league/history` — 历史赛季

### 5.4 前端

#### `GuildPage.tsx`
- 增加"联赛"标签页
- 公会联赛当前排名+积分
- 全服公会联赛排行榜

---

## 技术约束

1. **Docker 部署**：所有代码需构建 Docker 镜像并重新部署
2. **Flyway 迁移**：V27__add_golden_and_set_bonuses.sql + V28__add_achievements_and_rankings.sql
3. **后端**：保持现有 package 结构
4. **前端**：React + TypeScript，使用现有 CSS 变量
5. **样式**：暗色主题风格，金卡用金色渐变+呼吸光效CSS动画

## 验证

1. 进入卡牌→合成标签→合成金卡→返回卡册看到金色边框动画
2. 装备页面→穿2件史塔克装备→显示"冰系卡费用-1"激活
3. 进入成就页面→看到所有成就+进度
4. 领取成就→星尘增加
5. 赛季页面→看到综合排行+三模式分数拆解
6. 公会页面→联赛标签显示排行
