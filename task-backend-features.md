# Backend P1 — 玩法完整度 (GameEngine扩展)

## ⚠️ 请先确保 P0 任务已完成再开始本任务
P0 包括: 法力水晶修正、先手后手+幸运币、Mulligan、冷却

## 项目路径
- 项目: /home/heaton/pengyouquan-english
- 后端: /home/heaton/pengyouquan-english/backend

## 任务5: 奥秘系统 (Secrets)

### 模型
- 在 PlayerState 中添加 `List<SecretState> secrets` 
- SecretState: {cardId, triggerCondition, effectType, effectValue, isRevealed}

### 触发条件
- 敌方打出法术 → "counter" (反制：抵消法术)
- 敌方攻击英雄 → "ice_barrier" (冰甲：获得8护甲)
- 敌方回合结束 → "vaporize" (蒸发：消灭攻击者)
- 己方随从死亡 → "effigy" (镜像：召唤替代随从)
- 己方英雄受到伤害 → "noble_sacrifice" (崇高牺牲：召唤2/1防御者)

### 实现
1. 卡牌类型新增 "secret" 
2. 打出奥秘牌：扣费，挂到己方 secrets 列表
3. 每个行动后检查对手的 secrets 是否能触发
4. 触发后：reveal=true，执行效果，从列表移除
5. 前端显示：对手区显示奥秘问号牌

### 关键词新增
在 Card 模型和 CardState 中添加 `isSecret` 字段

## 任务6: 武器系统 (Weapons)

### 模型
- 新增 WeaponState: {cardId, nameCn, attack, durability, maxDurability}
- 在 PlayerState 中添加 `WeaponState weapon`

### 武器牌属性
- cost: 费用
- attack: 攻击力（英雄装备后获得该攻击力）
- durability: 耐久度（每次攻击消耗1）
- 关键词可选：吸血/风怒

### 实现
1. 卡牌类型新增 "weapon"
2. 打出武器：扣费，装备（替换旧武器到墓地）
3. 英雄获得武器的攻击力
4. 英雄攻击时计算武器攻击力 + 消耗耐久
5. 耐久归0武器损坏
6. 武器攻击英雄不受伤（炉石规则）
7. 英雄攻击随从时，随从反击只打英雄，不打武器

### 英雄攻击
- 当玩家有武器并点击英雄头像时发起攻击
- 选择目标（随从或敌方英雄）
- 攻击后耐久-1

## 任务7: 更多关键词

在 GameEngine.applyKeyword 中添加以下关键词支持：

| 关键词 | 效果 |
|--------|------|
| lifesteal (吸血) | 攻击造成伤害时，为己方英雄恢复等量生命 |
| poisonous (剧毒) | 对随从造成伤害时，直接消灭（无视血量） |
| windfury (风怒) | 每回合可攻击两次（canAttack 用完一次不清零） |
| spell_damage (法强) | 法术伤害+1（可叠加，记录全局法强值） |
| discover (发现) | 从3张随机卡牌中选择1张加入手牌 |

### CardState 新增字段
- isHasLifesteal, isHasPoisonous, isHasWindfury, isHasSpellDamage
- windfuryAttacksRemaining (风怒剩余攻击次数)
- globalSpellDamage (当前玩家的总法强值)

### 实现细节
- **吸血**: 在 declareAttack 中，攻击造成伤害后，如果 attacker 有 lifesteal，为所属玩家加血
- **剧毒**: 在随从战斗伤害结算时，攻击方有 poisonous 则目标直接死亡
- **风怒**: 每回合 canAttack=true，但用完一次不清零（只减次数），需重置
- **法强**: calculateSpellDamage 时加上 player 所有随从的法强总和
- **发现**: 实现 discover 机制，从全卡池抽3张随机卡，玩家选1张（通过 WebSocket 交互）

## 任务8: 锁定牌组

### 目标
玩家需要预设牌组（20张），不能再每次随机选牌

### 实现
1. 新增 Deck 相关表（或者用现有 UserCard 结合牌组序号）
2. API: POST /api/deck/save (保存牌组, 20张ID列表)
3. API: GET /api/deck/list (获取我的牌组列表)
4. API: GET /api/deck/current (获取当前使用牌组)
5. GameEngine.initPlayer 改为从用户的预设牌组取牌
6. BattleController 创建对战时检查玩家是否有牌组，无则不允许匹配
7. DeckBuilderPage.tsx 前端已经存在，需要确保它能正常工作

### 数据库
- 新增 flyway migration: V38__create_decks.sql
```sql
CREATE TABLE IF NOT EXISTS decks (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  user_id BIGINT NOT NULL,
  name VARCHAR(50) NOT NULL DEFAULT '我的牌组',
  card_ids TEXT NOT NULL,
  is_active BOOLEAN DEFAULT FALSE,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  FOREIGN KEY (user_id) REFERENCES users(id)
);
```
- 每个用户至少要有1套牌组才能对战
- 初始牌组 = 系统不随机分配，而是前20张基础卡

## 任务9: 英语练习→卡牌收集

### 概念
- 完成一定量的英语练习获得金币
- 每练习50句获得10金币
- 每日首胜获得额外20金币
- 金币用来购买卡包（100金币/包）
- 卡包开出5张随机卡（按稀有度概率）

### 后端修改
1. 新增 GoldService: 查询/增加金币
2. User 模型新增 gold 字段 (int, default=0)
3. API: GET /api/gold/balance
4. API: POST /api/gold/earn?sentences=X (练习完成50句时调用)
5. API: POST /api/shop/buy-pack (100金币 → 1卡包)
6. API: POST /api/shop/open-pack (开包API，调用现有 PackOpening)
7. 每日首胜检测：检查 BattleHistory 中今日是否有胜利

### 数据库
- V39__add_gold_to_users.sql: `ALTER TABLE users ADD COLUMN gold INT DEFAULT 0;`
- 每日练习记录关联

### 前端
- 在底部导航栏或主页显示金币数量
- 卡包商店页面（可用100金币购买）
- 练习页显示进度"50句可获10金币"
- 对战胜利后显示金币获得动画

## 部部署
Docker:
1. cd /home/heaton/pengyouquan-english/backend && mvn package -DskipTests -q
2. docker cp target/english-0.0.1-SNAPSHOT.jar pengyouquan-english-backend-1:/app/app.jar
3. docker restart pengyouquan-english-backend-1
