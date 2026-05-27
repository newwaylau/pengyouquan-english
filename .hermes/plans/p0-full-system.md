# P0 实现计划：基础战斗系统 + 第一幕

## 项目信息
- 项目路径：`/home/heaton/pengyouquan-english`
- Git 分支：`sit`
- 架构：Spring Boot (backend) + React (frontend) + MySQL
- 部署：Docker compose

## 设计文档
完整设计在 `Claude_Design_Complete_System.md`，以下为实现要点。

---

## 第一步：数据库表（Flyway迁移）

创建 V34__add_expedition_combat_system.sql，包含以下表：

### cards 表
- id, card_name, card_name_en, card_type(attack/skill/power/curse/status), rarity(basic/common/uncommon/rare), cost(0-3, 99=X费), description, base_damage, base_block, keywords(JSON), upgrade_description, upgrade_damage, upgrade_block, is_x_cost, show_id

### user_collections 表
- id, user_id, card_id, quantity, is_upgraded, acquired_from(basic/pack/shop/reward), created_at
- UNIQUE KEY uk_user_card (user_id, card_id)

### enemies 表
- id, name, enemy_type(normal/elite/boss), act, hp, behavior_pattern(JSON), phase2_hp_percent, phase2_pattern(JSON), loot_gold_min, loot_gold_max, relic_drop_id, description, special_mechanics(JSON)

### relics 表
- id, name, relic_type(boss/elite/common/event), description, effect_type(JSON), is_unique, pool(relic/boss/shop/event)

### potions 表
- id, name, description, effect_type, effect_value, rarity(common/uncommon/rare)

### node_content 表
- id, act, floor, node_type(combat/elite/event/rest/shop/treasure/boss), position, title, content(500字), connected_to(JSON), is_boss_node, event_choices(JSON), enemy_id, boss_id

### expeditions 表（远征实例）
- id, user_id, act, floor, hp, max_hp, gold, deck_json, relics_json, potions_json, status(active/won/dead), visited_nodes(JSON), created_at, updated_at

### expedition_battle_state 表（战斗中临时状态）
- id, expedition_id, enemy_id, enemy_hp, enemy_buffs(JSON), player_buffs(JSON), turn_number, hand_cards(JSON), draw_pile(JSON), discard_pile(JSON), energy, status(fighting/won/lost)

---

## 第二步：后端 Java 代码

### Model 实体类
- Card.java, UserCollection.java, Enemy.java, Relic.java, Potion.java, NodeContent.java, Expedition.java, ExpeditionBattleState.java
- 放在 `com.pengyouquan.english.model`

### Repository 接口
- CardRepository, UserCollectionRepository, EnemyRepository, RelicRepository, PotionRepository, NodeContentRepository, ExpeditionRepository, ExpeditionBattleStateRepository
- 放在 `com.pengyouquan.english.repository`

### Service 层

#### BattleService.java
- 核心战斗状态机：startBattle → playerTurn → playCard → endTurn → enemyAct → checkWin
- 伤害计算：基础伤害 + 力量加成，易伤*1.5，虚弱*0.75
- 状态系统：Vulnerable/Weak/Frail/Poison/Strength/Dexterity
- 格挡系统：格挡值回合结束清零
- 抽牌/弃牌系统：手牌上限10，抽牌堆空时洗回弃牌堆
- 战斗奖励：金币+卡牌3选1

#### ExpeditionService.java
- startExpedition: 从收藏池选初始8-10张卡，创建远征实例，加载第一幕地图
- getNextFloorNodes: 返回当前层可选节点
- enterNode: 进入节点（战斗/事件/篝火/商店/宝箱/Boss）
- getBattleReward: 战斗胜利后从收藏池随机3张
- handleDeath: 远征死亡，记录结算
- loadExpedition: 读取已有远征

#### CardCollectionService.java
- getCollection: 获取用户收藏
- addCardToCollection: 添加卡到收藏
- getCardPoolForReward: 从收藏池中随机3张（用于战斗奖励）
- addCardToDeck: 将收藏卡加入远征牌组

### Controller 层

#### BattleController.java
- POST /api/expedition/battle/start - 开始战斗
- POST /api/expedition/battle/play-card - 出牌 {cardIndex, targetIndex}
- POST /api/expedition/battle/end-turn - 结束回合
- POST /api/expedition/battle/use-potion - 用药水
- GET /api/expedition/battle/state - 获取当前战斗状态

#### ExpeditionController.java
- POST /api/expedition/start - 开始远征 {deckCardIds}
- GET /api/expedition/map - 获取当前层地图节点 {act, floor}
- POST /api/expedition/enter-node - 进入节点 {nodeId}
- POST /api/expedition/rest - 篝火休息
- POST /api/expedition/smith - 篝火锻造 {cardId}
- POST /api/expedition/shop-list - 商店商品
- POST /api/expedition/shop-buy - 购买 {itemType, itemId}
- POST /api/expedition/shop-remove - 删卡 {cardId}
- POST /api/expedition/event-choose - 事件选择 {choiceIndex}
- GET /api/expedition/state - 远征当前状态

---

## 第三步：初始数据（Seed Data）

### 50张基础卡牌（完成战斗系统测试所必需）

**攻击牌（20张）：**
1. 长剑劈砍 (Strike 基本) - 1费, 6伤
2. 铁剑斩 (升级Strike) - 1费, 9伤
3. 北境劈斩 - 1费, 8伤, 若敌人易伤+4
4. 野人冲撞 - 2费, 14伤
5. 双刃斩 - 1费, 5伤×2
6. 重劈 - 2费, 12伤, 消耗
7. 投掷长矛 - 1费, 7伤, 抽1张
8. 怒吼 - 0费, 4伤, 消耗
9. 旋风斩 - X费, X×4伤(AOE)
10. 突刺 - 1费, 6伤, 抽1张
11. 烈火剑 - 1费, 5伤, 上2层烧伤
12. 碎甲锤 - 2费, 8伤, 移除所有格挡
13. 连击 - 1费, 3伤×3
14. 致命一击 - 2费, 6伤, 若敌人半血以下翻倍
15. 战吼 - 2费, 10伤, 上1层易伤
16. 回旋踢 - 1费, 5伤, 上1层虚弱
17. 穿刺 - 2费, 7伤, 无视格挡
18. 猛击 - 3费, 20伤
19. 血色打击 - 1费, 4伤, 回2血
20. 终结技 - 2费, 8伤, 若敌人中毒伤害翻倍

**格挡牌（15张）：**
21. 皮盾防御 (Defend 基本) - 1费, 5挡
22. 铁盾防御 (升级Defend) - 1费, 8挡
23. 举盾 - 1费, 6挡
24. 铁壁 - 2费, 12挡
25. 盾墙 - 1费, 4挡, 上1层敏捷
26. 战术撤退 - 1费, 8挡, 抽2张
27. 坚守阵地 - 2费, 10挡, 上1层力量
28. 铁甲 - 2费, 12挡, 消耗
29. 反射 - 2费, 6挡, 敌人本回合受到该次攻击伤害的一半
30. 哨兵 - 1费, 5挡, 抽1张
31. 绊马索 - 1费, 5挡, 上1层易伤
32. 战旗 - 1费, 3挡, 所有牌加1格挡(本回合)
33. 铜墙铁壁 - 3费, 18挡
34. 卸力 - 0费, 3挡
35. 盾牌猛击 - 2费, 6挡, 造成等于格挡值的伤害

**技能牌（10张）：**
36. 战术谋划 - 1费, 抽2张牌
37. 集结 - 1费, 获得1能量, 消耗
38. 鼓舞 - 2费, 获得1力量, 消耗
39. 磨刀石 - 1费, 所有攻击牌+2伤害(本回合)
40. 包扎 - 1费, 回4血, 消耗
41. 战前准备 - 0费, 抽1张, 额外抽1张(若手牌≤3)
42. 振奋 - 2费, 获得1能量, 抽2张
43. 急行军 - 1费, 抽3张, 弃1张
44. 突袭命令 - 1费, 所有攻击牌费用-1(本回合)
45. 回天 - 3费, 回12血, 消耗, 消耗所有格挡转为等量回血

**能力牌（5张）：**
46. 战争艺术 - 2费, 每回合+1力量(整场战斗)
47. 铁甲护体 - 2费, 每回合+2格挡(整场战斗)
48. 狂暴 - 1费, 失去3最大HP, +1力量(整场战斗)
49. 寒冰护盾 - 1费, 每回合+1格挡, 攻击者受2伤
50. 复仇意志 - 2费, 每损失1HP,+1攻击(整场战斗)

### 初始用户收藏
- 所有用户基础拥有：长剑劈砍×3, 皮盾防御×3
- 收藏表在用户注册时自动插入6条基础卡记录

### 第一幕敌人数据

**普通敌人：**
- 野人斥候 (HP16): [A6]→[A6]→[A6] 
- 冰原狼 (HP20): [A8]→[D6]→[A8]
- 守夜人逃兵 (HP18): [A7]→[A7]→[B:给Weak1]→[A7]
- 雪地伏击者 (HP22): [A8,A8]→[A6]→[A10]

**精英：**
- 班扬·史塔克 (HP48): [B:Str+2]→[A10]→[A12]→[A14]，掉落遗物「守夜人披风」(每回合1格挡)
- 野人掠夺者 (HP44): [A8]→[A8]→[B:+3Str]→[A12]，掉落遗物「野人骨刃」(攻击+1)

**Boss：野人首领曼斯·雷德 (HP70)**
- P1: [A10]→[A12]→[B:所有野人+2Str]→[A15]
- P2(半血): [A12]×2→[C:野火AOE12]→[A18]

### 第一幕节点内容（52节点×500字）
- 按 `Claude_Design_Complete_System.md` 第12章结构写
- 每个节点500字英文权游内容
- 权游字幕数据在 project 的数据库中已有
- 战斗节点：场景描写→敌人台词→战斗氛围→动机
- 事件节点：剧情场景→核心情节点→双分支选项
- 篝火/商店：氛围描写→NPC对话
- Boss节点：登场→台词→战斗意义

---

## 第四步：前端 React 页面

### BattlePage.tsx
- 战斗界面：手牌显示、敌人显示、意图图标、玩家HP/格挡/能量
- 点击手牌→出牌（如有目标选择，点击敌人）
- 结束回合按钮
- 药水使用
- 战斗奖励弹窗（3卡选1）

### ExpeditionPage.tsx（重构）
- 地图渲染：岔路节点布局，已访问路线高亮
- 节点点击进入
- 篝火界面（休息/锻造）
- 商店界面（商品列表+购买+删卡）
- 事件界面（文本+分支选择）
- Boss战入口

### CardSelectModal.tsx（初始选牌）
- 从收藏池展示所有卡牌
- 选8-10张，确认进入远征

### GameState显示
- 远征进行中：HP、金币、幕数、层数
- 战斗进行中：手牌、能量、敌人状态

### CSS
- 深色主题，符合项目现有CSS变量

---

## 执行顺序

1. Flyway迁移V34（全部表）
2. Java实体类（Model）
3. Java Repository
4. Java Service（BattleService→ExpeditionService→CardCollectionService）
5. Java Controller
6. Seed数据（50张卡+敌人+节点内容）
7. 前端页面（BattlePage→ExpeditionPage→CardSelect）
8. Docker部署验证

---

## 关键约束
- 不修改已有的练习/复习页面
- 使用现有项目代码风格（Spring Boot + React + MySQL + Flyway）
- 保持现有CSS变量
- Docker部署，git branch sit
- 英文内容使用项目已有字幕数据
