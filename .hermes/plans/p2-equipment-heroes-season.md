# P2 — 装备系统 + 英雄系统 + 赛季重置

> 项目路径：`/home/heaton/pengyouquan-english`
> Git 分支：`sit`
> 所有中文内容必须正确 UTF-8 编码
> Spring Boot + React + MySQL + Docker 部署到 SIT

---

## 执行顺序

1. ✅ **装备系统** — 最独立，表已存在，先做
2. ✅ **英雄系统** — 新增表+后端+前端英雄选择页
3. ✅ **赛季重置系统** — 月结+段位降级+赛季宝箱发放

---

## 任务 1：装备系统

### 1.1 现有代码

- **已有**：`Equipment.java` Model（nameCn/nameEn/slot/rarity/statBonus/effectJson/affinityShowId/unlockCondition）
- **已有**：`UserEquipment.java` Model（userId/equipmentId/quantity）
- **已有**：V18 迁移的 `equipment` 和 `user_equipment` 表
- **缺少**：Repository、Service、Controller、装备管理的API端点、前端页面

### 1.2 V23 迁移——插入初始装备数据

```sql
-- 守夜人套装（GOT）
INSERT IGNORE INTO equipment (name_cn, name_en, slot, rarity, stat_bonus, effect_json, affinity_show_id, unlock_condition) VALUES
('守夜人之剑', 'Night Watch Sword', 'weapon', 'common', '{"attack_bonus":1}', '{}', 1, 'login_3_days'),
('守夜人皮甲', 'Night Watch Leather', 'armor', 'common', '{"health_bonus":3}', '{}', 1, 'reach_rank_1'),
('守夜人徽章', 'Night Watch Badge', 'trinket', 'rare', '{"draw_bonus":1}', '{}', 1, 'win_5_battles'),
('守夜人守夜记录', 'Night Watch Log', 'tome', 'common', '{"time_bonus":5}', '{}', 1, 'complete_50_sentences'),
('守夜人头盔', 'Night Watch Helm', 'crown', 'rare', '{"cooldown_reduction":1}', '{}', 1, 'reach_rank_3');

-- 史塔克套装（GOT）
INSERT IGNORE INTO equipment (name_cn, name_en, slot, rarity, stat_bonus, effect_json, affinity_show_id, unlock_condition) VALUES
('寒冰之剑', 'Ice Sword', 'weapon', 'epic', '{"attack_bonus":2,"ice_damage_bonus":1}', '{"set":"stark"}', 1, 'win_10_battles'),
('史塔克家徽胸甲', 'Stark Cuirass', 'armor', 'rare', '{"health_bonus":5}', '{"set":"stark"}', 1, 'reach_rank_5'),
('北境之戒', 'North Ring', 'trinket', 'epic', '{"draw_bonus":1,"mana_discount":1}', '{"set":"stark"}', 1, 'win_20_battles'),
('史塔克家谱', 'Stark Family Tree', 'tome', 'rare', '{"time_bonus":10}', '{"set":"stark"}', 1, 'complete_100_sentences'),
('北境王冠', 'Crown of Winter', 'crown', 'legendary', '{"cooldown_reduction":2,"first_card_free":true}', '{"set":"stark"}', 1, 'reach_legend_rank');

-- 坦格利安套装（GOT）
INSERT IGNORE INTO equipment (name_cn, name_en, slot, rarity, stat_bonus, effect_json, affinity_show_id, unlock_condition) VALUES
('龙晶匕首', 'Dragonglass Dagger', 'weapon', 'rare', '{"attack_bonus":1,"legendary_damage_bonus":2}', '{"set":"targaryen"}', 1, 'collect_5_legendaries'),
('龙鳞甲', 'Dragonscale Armor', 'armor', 'epic', '{"health_bonus":8}', '{"set":"targaryen"}', 1, 'reach_rank_8'),
('龙之坠', 'Dragon Pendant', 'trinket', 'legendary', '{"draw_bonus":2,"legendary_cost_reduction":1}', '{"set":"targaryen"}', 1, 'collect_10_legendaries'),
('龙族秘典', 'Targaryen Tome', 'tome', 'epic', '{"time_bonus":15}', '{"set":"targaryen"}', 1, 'season_pass_level_10'),
('龙王冠', 'Crown of the Dragon', 'crown', 'legendary', '{"cooldown_reduction":2,"ult_damage_bonus":3}', '{"set":"targaryen"}', 1, 'season_rank_1');

-- 唐顿套装（DA）
INSERT IGNORE INTO equipment (name_cn, name_en, slot, rarity, stat_bonus, effect_json, affinity_show_id, unlock_condition) VALUES
('绅士手杖', 'Gentlemans Cane', 'weapon', 'common', '{"heal_bonus":1}', '{}', 2, 'login_7_days'),
('庄园礼服', 'Estate Gown', 'armor', 'rare', '{"health_bonus":4,"rest_heal_bonus":0.2}', '{"set":"downton"}', 2, 'reach_rank_2'),
('伯爵印章戒', 'Earls Signet Ring', 'trinket', 'rare', '{"event_bonus_option":true}', '{"set":"downton"}', 2, 'complete_200_sentences'),
('下午茶手札', 'Tea Time Notes', 'tome', 'common', '{"time_bonus":5}', '{}', 2, 'login_3_days'),
('家族冕冠', 'Family Coronet', 'crown', 'epic', '{"rest_heal_bonus":0.3,"event_extra_option":true}', '{"set":"downton"}', 2, 'reach_diamond_rank');
```

### 1.3 后端

#### Repository
- `EquipmentRepository.java` — `findBySlot(String slot)`, `findByRarity(String rarity)`, `findByEffectJsonContaining(String set)` (找套装)
- `UserEquipmentRepository.java` — `findByUserId(Long userId)`, `findByUserIdAndEquipmentId(Long userId, Long equipmentId)`, `countByUserIdAndEquipmentId(Long userId, Long equipmentId)`

#### Service: `EquipmentService.java`
- `getAvailableEquipment(Long userId)` — 用户可获取/已拥有的全部装备
- `equipItem(Long userId, Long equipmentId)` — 装备到英雄，返回装备后的英雄配置
- `unequipItem(Long userId, int slotIndex)` — 卸下某槽位装备
- `getEquippedGear(Long userId)` — 当前装备配置
- `getSetBonuses(Long userId)` — 计算当前激活的套装效果
- `upgradeEquipment(Long userId, Long userEquipmentId)` — 消耗星尘升级
- `rerollStats(Long userId, Long userEquipmentId)` — 重铸附加属性

装备穿脱需要新增 `hero_equipment` 表（见任务2）或使用 `user_cards` 关联。

**简化方案（P2 MVP）**：暂时不做穿戴到英雄+套装的复杂逻辑，先做：
- 装备展示页面：所有装备列表（按槽位分类）
- 穿/脱装备（存到 user_equipment，track 当前穿戴的装备）
- 装备在实时对战中生效（BattleArenaPage读取装备加成）

需要新增表记录英雄当前装备配置：
```sql
-- V23 补充：英雄装备配置
CREATE TABLE IF NOT EXISTS hero_gear (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  user_id BIGINT NOT NULL UNIQUE,
  weapon_id BIGINT,
  armor_id BIGINT,
  trinket_id BIGINT,
  tome_id BIGINT,
  crown_id BIGINT,
  FOREIGN KEY (user_id) REFERENCES users(id),
  FOREIGN KEY (weapon_id) REFERENCES user_equipment(id),
  FOREIGN KEY (armor_id) REFERENCES user_equipment(id),
  FOREIGN KEY (trinket_id) REFERENCES user_equipment(id),
  FOREIGN KEY (tome_id) REFERENCES user_equipment(id),
  FOREIGN KEY (crown_id) REFERENCES user_equipment(id)
);
```

#### Controller: `EquipmentController.java`
- `GET /api/equipment` — 全部装备（含持有状态）
- `GET /api/equipment/mine` — 用户装备配置
- `POST /api/equipment/equip/{userEquipmentId}/{slot}` — 穿装备
- `POST /api/equipment/unequip/{slot}` — 卸装备
- `POST /api/equipment/upgrade/{userEquipmentId}` — 升级
- `POST /api/equipment/reroll/{userEquipmentId}` — 重铸

EquipmentRepository.java 需要添加。此外，需要新增 HeroGear Model（hero_gear 表）。

### 1.4 前端

#### 新建 `frontend/src/EquipmentPage.tsx`
装备管理页面，可以放在底部导航 "⚔️对战" 下的子页面或新标签页：

- 5个槽位卡片展示（武器/护甲/饰品/典籍/头冠）
- 每个槽位点击弹出可选装备列表
- 装备卡片：名称+稀有度颜色+属性加成+套装标记
- 已装备的高亮，未装备的灰色
- 底部显示当前激活的套装效果

#### 装备详情弹窗
- 属性加成列表
- 套装名称+凑齐进度（2/5件套）
- 升级按钮（消耗星尘）
- 重铸按钮（消耗星尘）

#### 修改 `BattleArenaPage.tsx`
- 游戏引擎读取 user gear → 应用到英雄属性
- 显示当前装备加成标记（小小图标在英雄头像旁）

---

## 任务 2：英雄系统

### 2.1 V23 补充——英雄配置表（已有show_id可作为剧集关联）

```sql
-- 英雄配置
CREATE TABLE IF NOT EXISTS heroes (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  show_id BIGINT NOT NULL COMMENT '所属剧集',
  name_cn VARCHAR(50) NOT NULL,
  name_en VARCHAR(100) NOT NULL,
  health INT NOT NULL DEFAULT 30 COMMENT '英雄血量',
  skill_name_cn VARCHAR(50) COMMENT '技能名',
  skill_name_en VARCHAR(100) COMMENT '技能英文名',
  skill_description_cn VARCHAR(200) COMMENT '技能描述',
  skill_description_en VARCHAR(200) COMMENT '技能英文描述',
  skill_cooldown INT NOT NULL DEFAULT 3 COMMENT '技能冷却回合数',
  base_effect_json JSON COMMENT '基础效果（如额外抽牌、减费等）',
  unlock_condition VARCHAR(200) COMMENT '解锁条件（练习句数/段位）',
  UNIQUE KEY uk_show (show_id)
);

-- 用户已选英雄
CREATE TABLE IF NOT EXISTS user_heroes (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  user_id BIGINT NOT NULL,
  hero_id BIGINT NOT NULL,
  unlocked_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  is_active BOOLEAN NOT NULL DEFAULT FALSE COMMENT '当前使用的英雄',
  skill_level INT NOT NULL DEFAULT 1 COMMENT '技能等级',
  FOREIGN KEY (user_id) REFERENCES users(id),
  FOREIGN KEY (hero_id) REFERENCES heroes(id),
  UNIQUE KEY uk_user_hero (user_id, hero_id)
);
```

### 2.2 初始英雄数据（同V23迁移插入）

```sql
INSERT IGNORE INTO heroes (show_id, name_cn, name_en, health, skill_name_cn, skill_name_en, skill_description_cn, skill_description_en, skill_cooldown, base_effect_json, unlock_condition) VALUES
(1, '琼恩·雪诺', 'Jon Snow', 30, '守夜人的意志', 'Watcher''s Will', '本回合所有随从获得+2生命值', 'All minions gain +2 Health this turn', 3, '{}', 'initial'),
(1, '丹妮莉丝·坦格利安', 'Daenerys Targaryen', 25, '龙之母', 'Mother of Dragons', '召唤一条3/3的龙，然后抽1张牌', 'Summon a 3/3 Dragon, then draw 1 card', 4, '{"max_mana_bonus":1}', 'complete_got_100'),
(1, '提利昂·兰尼斯特', 'Tyrion Lannister', 28, '智者的计谋', 'Lion''s Wit', '下一张法术牌不消耗费用', 'Your next spell costs 0', 3, '{"draw_bonus":1}', 'reach_rank_5'),
(2, '大小姐玛丽', 'Lady Mary', 30, '贵族风范', 'Noble Grace', '下张卡牌费用-3', 'Next card costs 3 less', 3, '{}', 'initial'),
(2, '罗伯特·克劳利', 'Robert Crawley', 35, '庄园之主', 'Lord of the Manor', '恢复6点生命值', 'Restore 6 Health', 4, '{"health_bonus":5}', 'complete_da_100');
```

### 2.3 后端

#### Models
- `Hero.java` — showId, nameCn, nameEn, health, skillNameCn, skillNameEn, skillDescriptionCn, skillDescriptionEn, skillCooldown, baseEffectJson, unlockCondition
- `UserHero.java` — userId, heroId, unlockedAt, isActive, skillLevel

#### Repository
- `HeroRepository.java` — `findByShowId`, findAll
- `UserHeroRepository.java` — `findByUserId`, `findByUserIdAndIsActiveTrue`

#### Service: `HeroService.java`
- `getAvailableHeroes(Long userId)` — 全部英雄+用户解锁状态
- `selectHero(Long userId, Long heroId)` — 选择/切换英雄
- `getActiveHero(Long userId)` — 当前激活的英雄及其装备
- `upgradeSkill(Long userId)` — 消耗星尘升级英雄技能

#### Controller: `HeroController.java`
- `GET /api/heroes` — 所有英雄+解锁状态
- `POST /api/heroes/select/{heroId}` — 切换英雄
- `GET /api/heroes/active` — 当前英雄+装备
- `POST /api/heroes/upgrade-skill` — 技能升级

#### 修改 `GameEngine.java` / `BattleArenaPage`
- 读取用户的 active hero 数据
- 英雄技能按钮（每3回合可用）
- 英雄血量=英雄基础血量+装备加成

### 2.4 前端

#### 新建 `frontend/src/HeroSelectPage.tsx`
英雄选择页面，可在CardCollectionPage底部导航增加"英雄"入口：

- 显示所有英雄头像+名称+血量
- 已解锁：可点击选择/切换
- 未解锁：显示解锁条件（"练完GOT 100句可解锁"）
- 当前选中的英雄高亮边框
- 当前英雄下方显示装备配置（5槽位预览）
- "装备"按钮→跳转EquipmentPage

#### 修改 `BattleArenaPage.tsx`
- 左上角显示当前英雄头像+名称+血量
- 技能按钮（显示冷却中/可用）
- 点击技能→触发英雄技能效果

#### 底部导航增加"英雄"入口
在底部导航第6个入口加 🦸（或将封臣改为英雄按钮），需要在 CardCollectionPage 或新增英雄选择页之间切换。

---

## 任务 3：赛季重置系统

### 3.1 V24 迁移

```sql
CREATE TABLE IF NOT EXISTS season_rewards (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  user_id BIGINT NOT NULL,
  season_number INT NOT NULL COMMENT '第几赛季',
  final_rank VARCHAR(30) COMMENT '最终段位',
  final_trophies INT COMMENT '最终奖杯数',
  reward_claimed BOOLEAN NOT NULL DEFAULT FALSE,
  chest_type VARCHAR(20) COMMENT '赛季宝箱类型',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  claimed_at DATETIME,
  INDEX idx_user_season (user_id, season_number)
);

-- 赛季配置表
CREATE TABLE IF NOT EXISTS season_config (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  season_number INT NOT NULL UNIQUE,
  start_date DATETIME NOT NULL,
  end_date DATETIME NOT NULL,
  is_active BOOLEAN NOT NULL DEFAULT FALSE,
  title_cn VARCHAR(100),
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
);
```

### 3.2 赛季规则

- 每月1日00:00结算
- 每个段位降级规则：
  - 传说→钻石（降1大段）
  - 钻石→白金
  - 白金→黄金
  - 黄金→白银
  - 白银→青铜
  - 青铜→保底0分
- 赛季宝箱奖励（按最终段位）：
  - 传说：1传说卡+5随机+300星尘
  - 钻石：1史诗+4随机+200星尘
  - 白金：1史诗+3随机+150星尘
  - 黄金：1稀有+3随机+100星尘
  - 白银：1稀有+2随机+50星尘
  - 青铜：3普通+20星尘

### 3.3 后端

#### Models
- `SeasonReward.java`
- `SeasonConfig.java`

#### Repository
- `SeasonRewardRepository.java`
- `SeasonConfigRepository.java`

#### Service: `SeasonService.java`
- `settleSeason(Long userId)` — 手动结算（或定时任务 V1）
- `claimSeasonReward(Long userId)` — 领取赛季宝箱
- `getCurrentSeason()` — 当前赛季信息
- `getSeasonHistory(Long userId)` — 历史赛季记录
- `resetRanks()` — 全服段位降级

#### Controller: `SeasonController.java`
- `GET /api/season/current` — 当前赛季
- `GET /api/season/history` — 赛季历史
- `POST /api/season/claim` — 领取赛季奖励
- `GET /api/season/rewards` — 赛季奖励预览

### 3.4 前端

#### 在 `BattlePage.tsx` 中增加赛季信息
- 顶部显示赛季倒计时（"S2赛季 还剩 12天"）
- 当前段位+赛季目标预览
- 赛季奖励列表（青铜→传说逐段位展示）
- 赛季结算后弹出赛季奖励领取弹窗

#### 赛季奖励弹窗
- 显示最终段位+奖杯数
- 显示赛季宝箱（调用 PackOpeningModal）
- "领取"按钮

---

## 技术约束

1. **Docker 部署**：所有代码需构建 Docker 镜像并重新部署
2. **Flyway 迁移**：V23__add_equipment_and_heroes.sql + V24__add_season_system.sql
3. **后端**：保持现有 package 结构（model/repository/service/controller）
4. **前端**：React + TypeScript，使用现有 CSS 变量（`var(--teal)`, `var(--gold)` 等）
5. **样式**：暗色主题风格，保持与现有卡牌页面一致
6. **底部导航**：如果需要新入口，在 App.tsx 底部导航加，最多6个（现在已有5个：Practice/Review/🎴卡牌/⚔️对战/👑封臣）

## 验证

1. 进入装备页面→查看到4套装备（守夜人/史塔克/坦格利安/唐顿）
2. 穿装备→实时对战→英雄血量改变
3. 切换英雄→进入实时对战→英雄头像+技能按钮可见
4. 赛季信息显示正确倒计时
5. 手动触发赛季结算→段位降级→发宝箱→可领取
