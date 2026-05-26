# P3 — PVE远征系统 + 公会系统

> 项目路径：`/home/heaton/pengyouquan-english`
> Git 分支：`sit`
> 所有中文内容必须正确 UTF-8 编码
> Spring Boot + React + MySQL + Docker 部署到 SIT

---

## 执行顺序

1. ✅ **PVE远征系统（杀戮尖塔式Roguelike）** — 核心玩法，单人可玩
2. ✅ **公会系统** — 社交粘性

---

## 任务 1：PVE远征系统

### 1.1 核心设计原则

- **杀戮尖塔式Roguelike**：每局独立，永久死亡，局内卡牌变化
- **从收藏选10张牌**作为起始牌组（非30张，精简构筑）
- **3层（Act）× 5-7个节点**，层末Boss
- **答题出伤**：答对→对敌人造成卡牌伤害，答错→自己扣血
- **三选一奖励**：击败敌人后三选一（新卡/Buff/回血/移除卡牌）
- **遗物系统**：改变规则的永久Buff，本局有效
- **死亡即结束**：血条归零→远征结束→已得卡牌和星尘保留

### 1.2 V25 迁移——PVE系统表

```sql
-- 远征实例（每局）
CREATE TABLE expeditions (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  user_id BIGINT NOT NULL,
  show_id BIGINT NOT NULL COMMENT '所属剧集，决定敌人和Boss',
  act INT NOT NULL DEFAULT 1 COMMENT '当前层(1-3)',
  node INT NOT NULL DEFAULT 1 COMMENT '当前节点(1-7)',
  max_act INT NOT NULL DEFAULT 1 COMMENT '已到达的最高层',
  player_hp INT NOT NULL DEFAULT 30 COMMENT '当前血量',
  max_hp INT NOT NULL DEFAULT 30 COMMENT '最大血量',
  starting_deck JSON NOT NULL COMMENT '起始10张牌(存card_id数组)',
  current_deck JSON NOT NULL COMMENT '当前牌组(当前局内全部牌)',
  relics JSON COMMENT '已获得的遗物(存relic_id数组)',
  gold INT NOT NULL DEFAULT 0 COMMENT '远征内金币',
  status VARCHAR(20) NOT NULL DEFAULT 'in_progress' COMMENT 'in_progress/cleared/dead',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  INDEX idx_user_status (user_id, status)
);

-- 遗物配置
CREATE TABLE relics (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  name_cn VARCHAR(50) NOT NULL,
  name_en VARCHAR(100) NOT NULL,
  rarity VARCHAR(20) NOT NULL COMMENT 'common/rare/epic/legendary',
  effect_cn VARCHAR(200) NOT NULL,
  effect_json JSON NOT NULL COMMENT '{\"type\":\"extra_damage\",\"value\":2} 等',
  source VARCHAR(50) NOT NULL COMMENT 'boss/event/shop',
  show_id BIGINT COMMENT '所属剧集(null=通用)'
);

-- 远征敌人配置
CREATE TABLE expedition_enemies (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  show_id BIGINT NOT NULL,
  act INT NOT NULL COMMENT '1-3',
  name_cn VARCHAR(50) NOT NULL,
  name_en VARCHAR(100) NOT NULL,
  hp INT NOT NULL,
  is_boss BOOLEAN NOT NULL DEFAULT FALSE,
  special_rules JSON COMMENT '特殊规则(如：\"开局给玩家挂1层脆弱\")',
  reward_pool JSON COMMENT '击败后可获得的奖励类型:[\"card\",\"relic\",\"heal\"]'
);

-- 事件配置
CREATE TABLE expedition_events (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  show_id BIGINT NOT NULL,
  act INT NOT NULL,
  title_cn VARCHAR(100) NOT NULL,
  description_cn TEXT NOT NULL,
  choices JSON NOT NULL COMMENT '[{"text":"...,"effect":{"type":"...","value":...},"result_text":"..."}]'
);
```

### 1.3 初始数据

#### 遗物（7个，与设计文档一致）
```sql
INSERT IGNORE INTO relics (name_cn, name_en, rarity, effect_cn, effect_json, source, show_id) VALUES
('龙鳞护符', 'Dragon Scale Talisman', 'rare', '本局所有选择题变为听力题（难度↑但奖励×1.5）', '{"type":"harder_questions","reward_multiplier":1.5}', 'boss', 1),
('渡鸦之眼', 'Raven Eye', 'epic', '每场战斗第一次答错不扣血', '{"type":"first_mistake_no_damage"}', 'event', 1),
('龙焰宝珠', 'Dragonflame Orb', 'rare', '答对时对敌方额外造成2点伤害', '{"type":"extra_damage_on_correct","value":2}', 'combat', 1),
('铁王座碎片', 'Iron Throne Shard', 'legendary', '每通过一个Boss，回满血', '{"type":"full_heal_after_boss"}', 'act3_boss', 1),
('学士典籍', 'Maester Tome', 'epic', '每场战斗首题自动答对（免费出牌）', '{"type":"first_answer_auto_correct"}', 'shop', 1),
('无面者面具', 'Faceless Mask', 'rare', '每答错一次，下题答对时伤害×2', '{"type":"consecutive_damage_boost"}', 'event', 1),
('蜂蜜酒', 'Mead', 'common', '休息节点回血量从30%→60%', '{"type":"rest_heal_bonus","multiplier":2}', 'shop', 1);
```

#### 敌人（GOT主题）
```sql
-- Act 1：绝境长城
INSERT IGNORE INTO expedition_enemies (show_id, act, name_cn, name_en, hp, is_boss, special_rules, reward_pool) VALUES
(1, 1, '野人斥候', 'Wildling Scout', 10, FALSE, '{}', '["card","gold"]'),
(1, 1, '冰原狼', 'Direwolf', 15, FALSE, '{}', '["card","gold"]'),
(1, 1, '野人劫掠者', 'Wildling Raider', 20, FALSE, '{"enrage_at_hp":5,"enrage_damage_bonus":2}', '["card","relic_chance","gold"]'),
(1, 1, '守夜人叛徒', 'Night Watch Traitor', 12, FALSE, '{"first_turn_stealth":true}', '["gold","card"]'),
(1, 1, '巨人Wun Wun', 'Wun Wun the Giant', 40, TRUE, '{"heavy_hit_every_3_turns":5}', '["card","relic","gold"]');

-- Act 2：君临城
INSERT IGNORE INTO expedition_enemies (show_id, act, name_cn, name_en, hp, is_boss, special_rules, reward_pool) VALUES
(1, 2, '金袍子卫兵', 'Gold Cloak Guard', 18, FALSE, '{}', '["card","gold"]'),
(1, 2, '御前侍卫', 'Kingsguard', 25, FALSE, '{"taunt":true}', '["card","gold","relic_chance"]'),
(1, 2, '情报总管', 'Master of Whispers', 20, FALSE, '{"steal_card":true}', '["gold","event"]'),
(1, 2, '兰尼斯特士兵', 'Lannister Soldier', 22, FALSE, '{"armor":3}', '["card","gold"]'),
(1, 2, '瑟曦·兰尼斯特', 'Cersei Lannister', 50, TRUE, '{"summon_minion_every_2_turns":"gold_cloak"}', '["card","relic","gold"]');

-- Act 3：龙石岛
INSERT IGNORE INTO expedition_enemies (show_id, act, name_cn, name_en, hp, is_boss, special_rules, reward_pool) VALUES
(1, 3, '无垢者', 'Unsullied', 25, FALSE, '{"immune_to_spells":true}', '["gold","card"]'),
(1, 3, '多斯拉克骑手', 'Dothraki Rider', 30, FALSE, '{"first_attack_double":true}', '["card","gold"]'),
(1, 3, '龙', 'Dragon', 35, FALSE, '{"aoe_attack":3}', '["card","relic_chance","gold"]'),
(1, 3, '红衣女巫', 'Red Witch', 28, FALSE, '{"heal_self_every_3_turns":8}', '["card","gold"]'),
(1, 3, '夜王', 'Night King', 65, TRUE, '{"resurrect_once":true,"aoe_every_3_turns":4,"raise_dead":true}', '["card","relic","gold","legendary_chance"]');
```

#### 事件
```sql
INSERT IGNORE INTO expedition_events (show_id, act, title_cn, description_cn, choices) VALUES
(1, 1, '发现古籍残页', '你在长城废墟中发现一页古籍。', '[{"text":"研读（练5句得稀有卡）","effect":{"type":"study","sentences":5,"reward":"rare_card"}},{"text":"跳过（得50星尘）","effect":{"type":"stardust","value":50}}]'),
(1, 1, '山姆的请求', '山姆威尔需要你帮忙整理藏书。', '[{"text":"帮忙（随机移除一张牌，获得一张稀有牌）","effect":{"type":"swap_card","rarity":"rare"}},{"text":"婉拒（得20金币）","effect":{"type":"gold","value":20}}]'),
(1, 2, '小指头的交易', '培提尔·贝里席想和你做一笔交易。', '[{"text":"接受（接下来2场战斗奖励翻倍，但每次答错扣双倍血）","effect":{"type":"double_reward_2_fights"}},{"text":"拒绝（得30星尘）","effect":{"type":"stardust","value":30}}]'),
(1, 2, '红堡比武', '御前举办比武大会，你可以报名参加。', '[{"text":"参加（答对3题，全对得稀有遗物）","effect":{"type":"challenge_quiz","questions":3,"reward":"rare_relic"}},{"text":"观战（回10血）","effect":{"type":"heal","value":10}}]'),
(1, 3, '龙母的考验', '丹妮莉丝要测试你是否配得上龙的信任。', '[{"text":"接受考验（答段落题，全对得传说级奖励）","effect":{"type":"boss_quiz"}},{"text":"退缩（无事发生）","effect":{"type":"nothing"}}]'),
(1, 3, '瓦雷利亚钢', '你发现一块瓦雷利亚钢碎片。', '[{"text":"锻造武器（攻击型卡牌伤害永久+1）","effect":{"type":"buff_attack","value":1}},{"text":"打造铠甲（最大血量+5）","effect":{"type":"buff_max_hp","value":5}}]');
```

#### 商店配置
商店在节点类型中作为可选节点出现，卖卡牌/遗物/回血，用金币购买。

### 1.4 后端

#### Models
- `Expedition.java` — 远征实例
- `Relic.java` — 遗物配置
- `ExpeditionEnemy.java` — 敌人配置
- `ExpeditionEvent.java` — 事件配置
- 复用已有的 `Card.java` 和 `UserCard.java`

#### Repository
- `ExpeditionRepository.java` — findByUserIdAndStatus(Long userId, String status)
- `RelicRepository.java` — findAll, findBySource
- `ExpeditionEnemyRepository.java` — findByShowIdAndAct(int showId, int act), findByShowIdAndActAndIsBossTrue
- `ExpeditionEventRepository.java` — findByShowIdAndAct

#### Service: `ExpeditionService.java`（核心！）

**远征流程：**
1. `startExpedition(Long userId, Long showId, List<Long> deckCardIds)` — 创建新远征
   - 从收藏验证这10张卡牌存在
   - 生成地图（当前层节点序列：战斗/事件/休息/商店/战斗/Boss）
   - 创建 Expedition 记录
2. `getExpedition(Long userId)` — 获取当前远征
   - 返回当前状态、地图、敌人信息
3. `getCurrentEnemy(Long userId)` — 获取当前敌人数据
   - 根据 act + node 索引敌人
   - Boss节点返回Boss特殊规则
4. `answerQuestion(Long userId, Long sentenceId, String answer, boolean correct)` — 答题
   - 答对：对当前敌人造成 card.attack + relic_bonus 伤害
   - 答错：玩家扣 enemy.attack 伤害（无装备普通为2）
   - 遗物效果在每个环节检查应用
   - 敌人死亡→进入奖励选择阶段
   - 玩家死亡→远征结束（status='dead'）
   - Boss击杀→进入下一层或检测通关
5. `chooseReward(Long userId, int choiceIndex)` — 奖励三选一
   - 返回三个可选项（加入新牌/升级现有牌/移除一张牌/回血/遗物）
   - 选择后更新 current_deck 或 relics
6. `enterEvent(Long userId, int choiceIndex)` — 事件选择
   - 执行事件效果
7. `enterRest(Long userId)` — 休息
   - 回血30%（如有蜂蜜酒遗物则60%）
   - 或升级一张卡牌
8. `enterShop(Long userId)` — 商店
   - 显示可购买物品
9. `buyItem(Long userId, String type, Long itemId)` — 购买
10. `getRewardChoices(Long userId)` — 获取三选一奖励选项

#### Controller: `ExpeditionController.java`
- `POST /api/expedition/start` — {showId, deckCardIds: [id1,id2,...]}
- `GET /api/expedition` — 当前远征
- `GET /api/expedition/enemy` — 当前敌人
- `POST /api/expedition/answer` — {sentenceId, answer}
- `POST /api/expedition/reward` — {choiceIndex}
- `POST /api/expedition/event` — {choiceIndex}
- `POST /api/expedition/rest` — {action: "heal"|"upgrade", cardId?}
- `GET /api/expedition/shop` — 商店物品
- `POST /api/expedition/buy` — {type: "card"|"relic"|"heal", itemId}
- `POST /api/expedition/abandon` — 放弃远征

### 1.5 前端

#### 新建 `frontend/src/ExpeditionPage.tsx`（核心！~800行）
完整PVE页面，经历整个流程：

**阶段1：出发准备**
- 用户选择剧集（GOT/DA）
- 从收藏选择10张卡牌（类似卡组编辑但精简版）
- 确认出发按钮

**阶段2：地图浏览（杀戮尖塔风格地图）**
- 显示当前层地图（5-7个节点连线）
- 已过的节点灰色，当前节点高亮，未来节点半透明
- 节点图标：🟢战斗 🟡事件 🔵休息 🟣Boss 🛒商店
- 点击当前节点→进入

**阶段3：战斗（复用部分现有对战UI）**
- 左侧：敌人头像+血条+特殊规则显示
- 右侧上方：玩家血条
- 中央：答题输入框（复用PracticePage的输入逻辑）
- 底部：已抽到手牌的卡牌（显示费用+攻击+效果）
- 答对→卡牌飞向敌人（伤害数字）
- 答错→敌人反击（玩家扣血）
- 敌人死亡→三选一奖励弹窗
- Boss特殊规则显示在Boss头像旁

**阶段4：事件**
- 弹窗显示事件标题+描述
- 2-3个选项按钮
- 点击执行效果

**阶段5：休息**
- 两个按钮：回血30% / 升级一张卡牌
- 选择升级→弹出当前牌组选一张→卡牌属性+1

**阶段6：商店**
- 显示可购买物品列表（卡牌/遗物/回血）
- 金币余额
- 购买按钮

**阶段7：通关结算**
- 击败Boss→通关弹窗
- 统计：击杀数/答对率/获得的遗物
- 奖励：固定星尘+概率稀有卡牌
- 死亡结算：显示答对率/已闯层数/已获卡牌

**阶段8：历史**
- 顶部标签切换到"远征历史"
- 显示以往远征记录（通关/死亡/层数/得分）

**样式**
- 使用现有CSS变量
- 暗色主题，地图用深色背景+发光节点
- 战斗界面类似BattleArenaPage风格

#### 底部导航集成
- 在 App.tsx 底部导航增加第7个入口"🗡️远征"
- 或者放在卡牌页面内作为子页面

---

## 任务 2：公会系统

### 2.1 V26 迁移——公会表

```sql
CREATE TABLE guilds (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  name VARCHAR(50) NOT NULL UNIQUE COMMENT '公会名（龙石岛/君临/临冬城等）',
  leader_id BIGINT NOT NULL COMMENT '会长',
  description VARCHAR(200) DEFAULT '' COMMENT '公会宣言',
  member_count INT NOT NULL DEFAULT 1,
  max_members INT NOT NULL DEFAULT 30,
  total_cards_collected INT NOT NULL DEFAULT 0 COMMENT '公会总卡牌收集数',
  weekly_score INT NOT NULL DEFAULT 0 COMMENT '本周领地战积分',
  rank_points INT NOT NULL DEFAULT 0 COMMENT '公会排名积分',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  FOREIGN KEY (leader_id) REFERENCES users(id)
);

CREATE TABLE guild_members (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  guild_id BIGINT NOT NULL,
  user_id BIGINT NOT NULL UNIQUE,
  role VARCHAR(20) NOT NULL DEFAULT 'member' COMMENT 'leader/officer/member',
  weekly_correct INT NOT NULL DEFAULT 0 COMMENT '本周答对数',
  weekly_score INT NOT NULL DEFAULT 0 COMMENT '本周贡献分',
  joined_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  FOREIGN KEY (guild_id) REFERENCES guilds(id),
  FOREIGN KEY (user_id) REFERENCES users(id)
);

CREATE TABLE guild_treasures (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  guild_id BIGINT NOT NULL,
  milestone INT NOT NULL COMMENT '里程碑（总收集数达到500/1000/2000等）',
  chest_type VARCHAR(20) NOT NULL COMMENT 'bronze/silver/gold',
  claimed_count INT NOT NULL DEFAULT 0 COMMENT '已领取人数',
  max_claims INT NOT NULL DEFAULT 30 COMMENT '可领取人数',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  FOREIGN KEY (guild_id) REFERENCES guilds(id)
);

CREATE TABLE guild_treasure_claims (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  treasure_id BIGINT NOT NULL,
  user_id BIGINT NOT NULL,
  claimed_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  FOREIGN KEY (treasure_id) REFERENCES guild_treasures(id),
  UNIQUE KEY uk_claim (treasure_id, user_id)
);
```

### 2.2 公会规则

- **创建**：消耗500星尘，选择预设名或自定义，最多30人
- **领地战**（周常）：每周六00:00结算，全公会本周答对率总和比拼
  - 不是实时对战——是每个成员练习答对题数的总和
  - 排名前10的公会所有成员得奖励
  - 排名1-3：黄金宝箱+200星尘
  - 排名4-10：白银宝箱+100星尘
  - 参与奖：青铜宝箱+50星尘
- **公会宝库**：公会总收集卡牌数达到里程碑时解锁
  - 500张→青铜宝库（每人每周1青铜卡包）
  - 1000张→白银宝库（每人每周1稀有卡包）
  - 2000张→黄金宝库（每人每周1史诗卡包）
  - 每个成员每周可领1次
- **贡献排行**：本周练习答对最多的3人显示在公会页面
- **会长转让**：会长可指定新会长

### 2.3 后端

#### Models
- `Guild.java` — 公会
- `GuildMember.java` — 公会成员
- `GuildTreasure.java` — 公会宝库里程碑
- `GuildTreasureClaim.java` — 宝库领取记录

#### Repository
- `GuildRepository.java` — findByName, findByLeaderId
- `GuildMemberRepository.java` — findByGuildId, findByUserId, findTopByGuildIdOrderByWeeklyScoreDesc (贡献排行)
- `GuildTreasureRepository.java` — findByGuildId
- `GuildTreasureClaimRepository.java` — findByTreasureIdAndUserId

#### Service: `GuildService.java`
- `createGuild(Long userId, String name)` — 创建公会（扣500星尘）
- `searchGuilds(String query)` — 搜索公会
- `joinGuild(Long userId, Long guildId)` — 加入公会
- `leaveGuild(Long userId)` — 退出公会
- `transferLeadership(Long userId, Long newLeaderId)` — 转让会长
- `kickMember(Long userId, Long kickedUserId)` — 踢人(仅会长)
- `getGuildInfo(Long guildId)` — 公会信息+成员+宝库
- `getGuildContribution(Long userId)` — 本周贡献
- `updateWeeklyScore(Long userId, int correctCount)` — 练习完成时更新贡献
- `settleTerritoryWar()` — 每周六结算领地战（内部定时接口）
- `claimTreasure(Long userId, Long treasureId)` — 领取宝库

#### Controller: `GuildController.java`
- `POST /api/guilds/create` — {name, description}
- `GET /api/guilds/search?q=` — 搜索
- `POST /api/guilds/{id}/join` — 加入
- `POST /api/guilds/leave` — 退出
- `POST /api/guilds/{id}/kick/{userId}` — 踢人
- `POST /api/guilds/{id}/transfer/{userId}` — 转让会长
- `GET /api/guilds/{id}` — 公会信息
- `GET /api/guilds/mine` — 我的公会
- `GET /api/guilds/leaderboard` — 公会排行榜
- `POST /api/guilds/treasure/{treasureId}/claim` — 领取宝库

### 2.4 前端

#### 新建 `frontend/src/GuildPage.tsx`
公会主页，包含多个标签：

**标签1：公会概览**（有公会时）
- 公会名+宣言+徽标（预设名有对应图标）
- 成员列表（头衔：👑会长/🗡️官员/🛡️成员）
- 本周贡献排行（Top3高亮）
- 公会宝库（里程碑进度条+可领取宝箱）
- 领地战倒计时+排名

**标签2：搜索公会**（无公会时）
- 搜索框
- 搜索结果列表（公会名+人数+排名）

**标签3：创建公会**
- 名称输入（预设名推荐或自定义）
- 宣言输入
- 创建按钮（显示消耗500星尘）
- 星尘余额不足时提示

**标签4：公会排行**
- 全服公会排名列表
- 显示排名/公会名/人数/周积分

#### 修改 `BattlePage.tsx` 或新建入口
- 底部导航可以将"👑封臣"改为公会的入口
- 或者在现有"封臣"页面添加公会入口

---

## 技术约束

1. **Docker 部署**：所有代码需构建 Docker 镜像并重新部署
2. **Flyway 迁移**：V25__add_pve_system.sql + V26__add_guild_system.sql
3. **后端**：保持现有 package 结构（model/repository/service/controller）
4. **前端**：React + TypeScript，使用现有 CSS 变量
5. **样式**：暗色主题风格
6. **底部导航**：新增"🗡️远征"+ "🏰公会"入口（现有6个，加2个=8个，可能放不下，建议合并或用二级菜单）

**底部导航建议**：现在有8个入口太多，建议合并方案：
- 原有：练习 / 复习(在练习里) / 🎴卡牌 / ⚔️对战 / 👑封臣 / 🦸英雄
- 新增：🗡️远征 / 🏰公会
- 保留6个：练习 / 🎴卡牌 / ⚔️对战 / 🦸英雄 / 🗡️远征 / 🏰公会
- 复习放入练习页内

## 验证

1. 进入远征→选剧集→选10张牌→出发
2. 地图显示正常→点击战斗节点→答题对战
3. 击败敌人→三选一奖励
4. 事件节点→选择选项→效果生效
5. 休息/商店可用
6. Boss击败→通关结算
7. 死亡→远征结束，卡牌保留
8. 创建公会→搜索→加入→查看成员列表
9. 公会宝库里程碑+领取
10. 领地战排行
