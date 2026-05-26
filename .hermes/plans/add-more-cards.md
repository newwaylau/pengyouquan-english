# 扩充卡牌内容 — 实现计划

> Claude Code 执行。git 分支：`sit`
> 目标：从当前 28 张卡扩充到约 80 张，覆盖 GOT + DA 经典角色和名场面。

## 任务概览

1. 检查现有卡牌数据，避免重复
2. 生成 GOT 新卡（约 25 张）——角色/法术/事件/地点
3. 生成 DA 新卡（约 25 张）
4. 每张卡绑定来自 `sentences` 表的 `challenge_sentence_id`
5. 通过 Flyway V20 迁移插入
6. 验证

---

## Task 1: 检查现有卡牌

先查已有卡牌，确认 ID 范围和各 show 分布。

**命令**:
```bash
docker exec pengyouquan-mysql mysql -uroot -ppengyouquan123 --default-character-set=utf8mb4 \
  -e "SELECT id, name_cn, name_en, rarity, card_type FROM cards ORDER BY id;" pengyouquan_english
```

现有卡牌：ID 1-?（GOT）+ ?-28（DA），初始数据在 V18 中。

---

## Task 2: 生成 GOT 新卡（25 张左右）

基于权游经典角色+台词。格式同 V18 的 INSERT 语句。

每张卡包含：
- `name_cn` / `name_en` — 中文名/英文名
- `card_type` — minion / spell / location / equipment
- `rarity` — common / rare / epic / legendary
- `cost` — 1-10（费用=题目难度）
- `attack` / `health` — 随从的攻击和生命
- `effect_json` — `{"keywords":[],"description_cn":"效果","description_en":"effect"}`
- `faction` — stark / lannister / targaryen / nightwatch / neutral 等
- `quote_text` — 该角色的经典英文台词
- `challenge_sentence_id` — 从 sentences 表选取该角色的名言对应 ID

### 建议新增 GOT 卡牌

| 角色/卡牌 | 稀有度 | 类型 | 费用 | 攻击/生命 | 效果 | 名言 |
|---|---|---|---|---|---|---|
| 奈德·史塔克 | 传说 | 随从 | 7 | 7/7 | 战吼：本局其余随从+1/+1 | "The man who passes the sentence should swing the sword." |
| 琼恩·雪诺 | 史诗 | 随从 | 5 | 5/5 | 你的守夜人随从+1/+1 | "I am the shield that guards the realms of men." |
| 丹妮莉丝·坦格利安 | 传说 | 随从 | 8 | 6/8 | 战吼：对全体敌人造成3点伤害 | "I am not a politician. I am a queen." |
| 提利昂·兰尼斯特 | 史诗 | 随从 | 4 | 2/5 | 每回合多抽1张牌 | "I drink and I know things." |
| 艾莉亚·史塔克 | 史诗 | 随从 | 3 | 3/2 | 潜行，攻击时不可被防御 | "Not today." |
| 瑟曦·兰尼斯特 | 传说 | 随从 | 7 | 5/8 | 每当你答对，获得+1攻击力 | "When you play the game of thrones, you win or you die." |
| 詹姆·兰尼斯特 | 史诗 | 随从 | 5 | 5/4 | 攻击时抽1张牌 | "The things I do for love." |
| 马王·卓戈 | 史诗 | 随从 | 6 | 7/4 | 冲锋 | "A khal who cannot ride is no khal." |
| 小指头 | 稀有 | 随从 | 3 | 2/3 | 战吼：抽1张牌 | "Chaos is a ladder." |
| 瓦里斯 | 稀有 | 随从 | 3 | 1/4 | 你的间谍类效果翻倍 | "Power resides where men believe it resides." |
| 猎狗 | 稀有 | 随从 | 4 | 4/3 | 嘲讽 | "Look at me. I'm the monster they made me." |
| 山姆威尔·塔利 | 稀有 | 随从 | 2 | 1/3 | 战吼：恢复3点生命 | "I'm not a fighter. I'm a maester." |
| 布蕾妮 | 稀有 | 随从 | 5 | 5/5 | 嘲讽，对战传说随从+2/+2 | "I protect the ones who can't protect themselves." |
| 奥莲娜夫人 | 史诗 | 随从 | 4 | 3/4 | 战吼：消灭一个敌方小随从 | "Tell Cersei. I want her to know it was me." |
| 红袍女巫 | 稀有 | 随从 | 4 | 3/3 | 战吼：复活一个友方随从 | "The night is dark and full of terrors." |
| 血色婚礼 | 传说 | 事件 | 8 | - | 消灭所有敌方随从，你的英雄受3伤 | "The Lannisters send their regards." |
| 龙焰 | 史诗 | 法术 | 5 | - | 对全体敌人造成4点伤害 | "Dracarys." |
| 面纱之人 | 稀有 | 法术 | 2 | - | 抽2张牌 | "Valar morghulis." |
| 守夜人誓言 | 稀有 | 法术 | 2 | - | 恢复3点生命（已有，但可补充） | "" |
| 狼家血脉 | 史诗 | 法术 | 3 | - | 所有友方随从+1/+1 | "The pack survives." |
| 铁王座 | 传说 | 地点 | 6 | - | 每回合获得1点额外费用 | "" |
| 黑水河之战 | 史诗 | 事件 | 6 | - | 对所有敌人造成2伤害，恢复2生命 | "Let them see the flames." |
| 卡斯特梅雨 | 史诗 | 法术 | 4 | - | 消灭一个敌方随从 | "The Rains of Castamere." |

---

## Task 3: 生成 DA 新卡（25 张左右）

| 角色/卡牌 | 稀有度 | 类型 | 费用 | 攻击/生命 | 效果 | 名言/说明 |
|---|---|---|---|---|---|---|
| 维奥莱特伯爵夫人 | 传说 | 随从 | 7 | 5/8 | 每回合恢复2点生命 | "What is a weekend?" |
| 罗伯特伯爵 | 史诗 | 随从 | 5 | 4/6 | 你的贵族随从+1/+1 | "I'm a gentleman. I don't have to know how to do anything." |
| 玛丽小姐 | 史诗 | 随从 | 4 | 4/4 | 战吼：消灭一个敌方小随从 | "I'm a modern woman." |
| 马修·克劳利 | 史诗 | 随从 | 5 | 5/4 | 每回合多抽1张牌 | "I don't want to be a middle-class hero." |
| 汤姆·布兰森 | 稀有 | 随从 | 3 | 3/3 | 战吼：使一个友方随从+2攻击 | "I don't belong here." |
| 卡森管家 | 稀有 | 随从 | 4 | 2/6 | 你的仆从随从+1/+1 | "A house is not a home." |
| 休斯太太 | 稀有 | 随从 | 3 | 3/3 | 战吼：恢复4点生命 | "We all have our dreams." |
| 托马斯·巴罗 | 稀有 | 随从 | 4 | 4/2 | 潜行 | "I'll find a way." |
| 奥布莱恩 | 稀有 | 随从 | 3 | 3/2 | 战吼：对一个敌方随从造成2伤害 | "I always win." |
| 安娜 | 稀有 | 随从 | 2 | 2/2 | 战吼：恢复2点生命 | "I love my work." |
| 贝茨先生 | 史诗 | 随从 | 4 | 3/5 | 你的领主盟友获得+1攻击 | "I'm a loyal man." |
| 黛西 | 普通 | 随从 | 1 | 1/2 | 战吼：获得1点法力 | "I'm just a kitchen maid." |
| 帕特莫尔太太 | 普通 | 随从 | 3 | 2/4 | 每回合恢复1点生命 | "A good cook is worth her weight in gold." |
| 伊迪丝小姐 | 稀有 | 随从 | 3 | 2/3 | 战吼：抽1张牌 | "I have a voice." |
| 西比尔小姐 | 稀有 | 随从 | 3 | 3/3 | 战吼：使所有友方随从获得+1生命 | "We must change with the times." |
| 罗斯 | 普通 | 随从 | 1 | 1/1 | 战吼：获得1星尘 | "" |
| 阿尔弗雷德 | 普通 | 随从 | 2 | 2/2 | - | "" |
| 唐顿大宅 | 传说 | 地点 | 7 | - | 每回合你的仆从获得+1/+1 | "" |
| 楼下的世界 | 史诗 | 地点 | 5 | - | 每回合召唤1个1/1仆从 | "" |
| 楼上楼下 | 稀有 | 法术 | 3 | - | 抽2张牌 | "The servants and the family. Two worlds." |
| 庄园晚餐 | 稀有 | 法术 | 4 | - | 恢复6点生命 | "Dinner is served." |
| 盛大舞会 | 史诗 | 法术 | 6 | - | 所有友方随从+2/+2 | "The annual Crawley ball." |
| 遗产继承 | 传说 | 事件 | 10 | - | 本局所有卡牌费用减半 | "The entail." |
| 新的时代 | 史诗 | 法术 | 5 | - | 抽3张牌并获得3点法力 | "A new era is coming." |
| 战争的消息 | 稀有 | 事件 | 4 | - | 对所有敌人造成3伤害 | "There's a war on." |

---

## Task 4: 绑定 challenge_sentence_id

每张卡需要从 `sentences` 表找一个对应的句子ID。用关键词搜索。

**命令示例**:
```sql
SELECT id, text_en FROM sentences WHERE show_id=1 AND text_en LIKE '%winter is coming%' LIMIT 5;
SELECT id, text_en FROM sentences WHERE show_id=1 AND text_en LIKE '%swing the sword%' LIMIT 5;
```

对每张卡运行类似的查询来找到正确的 sentence_id。

对于没有直接名言的卡（如部分地点/事件卡），设为 NULL。

---

## Task 5: 创建 V20 迁移

**文件**: `backend/src/main/resources/db/migration/V20__add_more_cards.sql`

格式同 V18 的 INSERT 语句，使用 `INSERT IGNORE` 避免重复：
```sql
INSERT IGNORE INTO cards (show_id, name_cn, name_en, card_type, rarity, cost, attack, health, effect_json, faction, quote_text, challenge_sentence_id) VALUES
(...),
(...);
```

---

## Task 6: 验证

```bash
docker exec pengyouquan-mysql mysql -uroot -ppengyouquan123 --default-character-set=utf8mb4 \
  -e "SELECT rarity, COUNT(*) FROM cards GROUP BY rarity ORDER BY FIELD(rarity,'common','rare','epic','legendary');" pengyouquan_english
docker exec pengyouquan-mysql mysql -uroot -ppengyouquan123 --default-character-set=utf8mb4 \
  -e "SELECT show_id, COUNT(*) FROM cards GROUP BY show_id;" pengyouquan_english
curl -s https://english-sit.pengyouquan.top/api/cards | python3 -c "import sys,json; d=json.load(sys.stdin); print(f'Total: {len(d[\"data\"])} cards')" 2>&1
```
