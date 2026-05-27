-- V37: 新增 80 张 GOT 主题卡牌（Hearthstone 风格）
-- 分布：随从 55 张 + 法术 15 张 + 装备 8 张 + 奥秘 2 张（location）
-- 费用：0费5, 1费15, 2费19, 3费15, 4费10, 5费5, 6费5, 7费3, 8费3
-- 稀有度：common 30 / rare 25 / epic 15 / legendary 10

-- 1. 确保 race 和 element 列存在（已在 V18/后续追加）
-- MySQL 8.0 不支持 ADD COLUMN IF NOT EXISTS，列已存在则跳过
-- 检查 race 列是否存在
SET @dbname = 'pengyouquan_english';
SET @tablename = 'cards';
SET @colname_race = 'race';
SET @colname_element = 'element';
SET @preparedStatement = (SELECT IF(
  (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = @dbname AND TABLE_NAME = @tablename AND COLUMN_NAME = @colname_race) = 0,
  'ALTER TABLE cards ADD COLUMN race VARCHAR(20) DEFAULT NULL COMMENT ''种族: dragon/human/beast/undead/giant/shadow/wolf'' AFTER faction',
  'SELECT 1'
));
PREPARE stmt FROM @preparedStatement;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @preparedStatement = (SELECT IF(
  (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = @dbname AND TABLE_NAME = @tablename AND COLUMN_NAME = @colname_element) = 0,
  'ALTER TABLE cards ADD COLUMN element VARCHAR(20) DEFAULT NULL COMMENT ''元素: fire/ice/shadow/light/nature/metal/lightning/poison/blood'' AFTER race',
  'SELECT 1'
));
PREPARE stmt FROM @preparedStatement;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- ============================================================================
-- 2. 插入 80 张 GOT 卡牌（按费用升序排列）
-- ============================================================================

-- ========== 0费 5张 ==========

INSERT IGNORE INTO cards (show_id, name_cn, name_en, card_type, rarity, cost, attack, health, effect_json, keywords, faction, race, element, has_golden, quote_text) VALUES
(1, '乌鸦信使', 'Raven Messenger', 'minion', 'common', 0, 0, 1, '{"description_cn":"","description_en":""}', '[]', 'neutral', 'beast', 'nature', FALSE, 'A raven flew over the castle.');

INSERT IGNORE INTO cards (show_id, name_cn, name_en, card_type, rarity, cost, attack, health, effect_json, keywords, faction, race, element, has_golden, quote_text) VALUES
(1, '野火之瓶', 'Wildfire Flask', 'spell', 'common', 0, NULL, NULL, '{"description_cn":"造成1点伤害","description_en":"Deal 1 damage"}', '[]', 'neutral', NULL, 'fire', FALSE, 'Wildfire!');

INSERT IGNORE INTO cards (show_id, name_cn, name_en, card_type, rarity, cost, attack, health, effect_json, keywords, faction, race, element, has_golden, quote_text) VALUES
(1, '流浪歌手', 'Wandering Minstrel', 'minion', 'common', 0, 1, 1, '{"description_cn":"","description_en":""}', '[]', 'neutral', 'human', 'light', FALSE, 'The Rains of Castamere...');

INSERT IGNORE INTO cards (show_id, name_cn, name_en, card_type, rarity, cost, attack, health, effect_json, keywords, faction, race, element, has_golden, quote_text) VALUES
(1, '笼中渡鸦', 'Caged Raven', 'spell', 'common', 0, NULL, NULL, '{"description_cn":"抽1张牌","description_en":"Draw 1 card"}', '[]', 'neutral', NULL, 'nature', FALSE, '');

INSERT IGNORE INTO cards (show_id, name_cn, name_en, card_type, rarity, cost, attack, health, effect_json, keywords, faction, race, element, has_golden, quote_text) VALUES
(1, '小蜘蛛', 'Little Spider', 'minion', 'common', 0, 0, 2, '{"description_cn":"","description_en":""}', '[]', 'neutral', 'beast', 'shadow', FALSE, '');

-- ========== 1费 15张 ==========

INSERT IGNORE INTO cards (show_id, name_cn, name_en, card_type, rarity, cost, attack, health, effect_json, keywords, faction, race, element, has_golden, quote_text) VALUES
(1, '守夜人新兵', 'Night Watch Recruit', 'spell', 'common', 1, NULL, NULL, '{"description_cn":"召唤一个1/2守夜人新兵","description_en":"Summon a 1/2 Night Watch Recruit"}', '[]', 'nightwatch', NULL, 'ice', FALSE, 'I am the sword in the darkness.');

INSERT IGNORE INTO cards (show_id, name_cn, name_en, card_type, rarity, cost, attack, health, effect_json, keywords, faction, race, element, has_golden, quote_text) VALUES
(1, '史塔克卫士', 'Stark Guard', 'minion', 'common', 1, 1, 3, '{"description_cn":"嘲讽","description_en":"Taunt"}', '["taunt"]', 'stark', 'human', 'ice', FALSE, 'Winter is coming.');

INSERT IGNORE INTO cards (show_id, name_cn, name_en, card_type, rarity, cost, attack, health, effect_json, keywords, faction, race, element, has_golden, quote_text) VALUES
(1, '兰尼斯特哨兵', 'Lannister Sentinel', 'minion', 'common', 1, 1, 2, '{"description_cn":"","description_en":""}', '[]', 'lannister', 'human', 'metal', FALSE, 'A Lannister always pays his debts.');

INSERT IGNORE INTO cards (show_id, name_cn, name_en, card_type, rarity, cost, attack, health, effect_json, keywords, faction, race, element, has_golden, quote_text) VALUES
(1, '多斯拉克斥候', 'Dothraki Scout', 'minion', 'common', 1, 2, 1, '{"description_cn":"","description_en":""}', '[]', 'targaryen', 'human', 'fire', FALSE, 'The Dothraki ride.');

INSERT IGNORE INTO cards (show_id, name_cn, name_en, card_type, rarity, cost, attack, health, effect_json, keywords, faction, race, element, has_golden, quote_text) VALUES
(1, '铁群岛掠夺者', 'Ironborn Raider', 'minion', 'common', 1, 2, 1, '{"description_cn":"","description_en":""}', '[]', 'greyjoy', 'human', 'metal', FALSE, 'What is dead may never die.');

INSERT IGNORE INTO cards (show_id, name_cn, name_en, card_type, rarity, cost, attack, health, effect_json, keywords, faction, race, element, has_golden, quote_text) VALUES
(1, '提利尔园丁', 'Tyrell Gardener', 'minion', 'common', 1, 1, 2, '{"description_cn":"","description_en":""}', '[]', 'tyrell', 'human', 'nature', FALSE, 'Growing strong.');

INSERT IGNORE INTO cards (show_id, name_cn, name_en, card_type, rarity, cost, attack, health, effect_json, keywords, faction, race, element, has_golden, quote_text) VALUES
(1, '学士学徒', 'Maester Apprentice', 'minion', 'rare', 1, 1, 1, '{"description_cn":"战吼：恢复2点生命","description_en":"Battlecry: Restore 2 Health"}', '["battlecry"]', 'neutral', 'human', 'light', FALSE, 'A maester serves the realm.');

INSERT IGNORE INTO cards (show_id, name_cn, name_en, card_type, rarity, cost, attack, health, effect_json, keywords, faction, race, element, has_golden, quote_text) VALUES
(1, '缝衣针', 'Needle', 'equipment', 'rare', 1, 1, 2, '{"description_cn":"攻击力1，耐久度2","description_en":"1 Attack, 2 Durability"}', '["battlecry"]', 'stark', NULL, 'metal', FALSE, 'Stick them with the pointy end.');

INSERT IGNORE INTO cards (show_id, name_cn, name_en, card_type, rarity, cost, attack, health, effect_json, keywords, faction, race, element, has_golden, quote_text) VALUES
(1, '火焰之吻', 'Kiss of Fire', 'spell', 'rare', 1, NULL, NULL, '{"description_cn":"对一个敌方随从造成3点伤害","description_en":"Deal 3 damage to an enemy minion"}', '["battlecry"]', 'targaryen', NULL, 'fire', FALSE, 'Dracarys!');

INSERT IGNORE INTO cards (show_id, name_cn, name_en, card_type, rarity, cost, attack, health, effect_json, keywords, faction, race, element, has_golden, quote_text) VALUES
(1, '冰封之触', 'Touch of Ice', 'spell', 'common', 1, NULL, NULL, '{"description_cn":"冻结一个敌方随从","description_en":"Freeze an enemy minion"}', '[]', 'stark', NULL, 'ice', FALSE, '');

INSERT IGNORE INTO cards (show_id, name_cn, name_en, card_type, rarity, cost, attack, health, effect_json, keywords, faction, race, element, has_golden, quote_text) VALUES
(1, '阴影之步', 'Shadow Step', 'spell', 'common', 1, NULL, NULL, '{"description_cn":"使一个友方随从获得潜行直到下回合","description_en":"Give a friendly minion Stealth until next turn"}', '[]', 'neutral', NULL, 'shadow', FALSE, 'A girl has no name.');

INSERT IGNORE INTO cards (show_id, name_cn, name_en, card_type, rarity, cost, attack, health, effect_json, keywords, faction, race, element, has_golden, quote_text) VALUES
(1, '红袍僧', 'Red Priest', 'minion', 'rare', 1, 1, 2, '{"description_cn":"战吼：使一个友方随从获得+1攻击力","description_en":"Battlecry: Give a friendly minion +1 Attack"}', '["battlecry"]', 'neutral', 'human', 'fire', FALSE, 'The night is dark and full of terrors.');

INSERT IGNORE INTO cards (show_id, name_cn, name_en, card_type, rarity, cost, attack, health, effect_json, keywords, faction, race, element, has_golden, quote_text) VALUES
(1, '马泰尔刺客', 'Martell Assassin', 'minion', 'rare', 1, 2, 1, '{"description_cn":"剧毒","description_en":"Poisonous"}', '["poison"]', 'martell', 'human', 'poison', FALSE, 'Unbowed, Unbent, Unbroken.');

INSERT IGNORE INTO cards (show_id, name_cn, name_en, card_type, rarity, cost, attack, health, effect_json, keywords, faction, race, element, has_golden, quote_text) VALUES
(1, '风暴地弓箭手', 'Stormland Archer', 'minion', 'rare', 1, 2, 1, '{"description_cn":"战吼：造成1点伤害","description_en":"Battlecry: Deal 1 damage"}', '["battlecry"]', 'baratheon', 'human', 'lightning', FALSE, 'Ours is the fury.');

-- ========== 2费 19张 ==========

INSERT IGNORE INTO cards (show_id, name_cn, name_en, card_type, rarity, cost, attack, health, effect_json, keywords, faction, race, element, has_golden, quote_text) VALUES
(1, '自由民战士', 'Free Folk Warrior', 'spell', 'rare', 2, NULL, NULL, '{"description_cn":"使一个友方随从获得+2/+2","description_en":"Give a friendly minion +2/+2"}', '["battlecry"]', 'neutral', NULL, 'ice', FALSE, 'We are the free folk.');

INSERT IGNORE INTO cards (show_id, name_cn, name_en, card_type, rarity, cost, attack, health, effect_json, keywords, faction, race, element, has_golden, quote_text) VALUES
(1, '史塔克剑士', 'Stark Swordsman', 'minion', 'rare', 2, 3, 2, '{"description_cn":"","description_en":""}', '[]', 'stark', 'human', 'ice', FALSE, 'The man who passes the sentence should swing the sword.');

INSERT IGNORE INTO cards (show_id, name_cn, name_en, card_type, rarity, cost, attack, health, effect_json, keywords, faction, race, element, has_golden, quote_text) VALUES
(1, '兰尼斯特弩手', 'Lannister Crossbowman', 'minion', 'rare', 2, 2, 2, '{"description_cn":"战吼：造成1点伤害","description_en":"Battlecry: Deal 1 damage"}', '["battlecry"]', 'lannister', 'human', 'metal', FALSE, 'Hear me roar!');

INSERT IGNORE INTO cards (show_id, name_cn, name_en, card_type, rarity, cost, attack, health, effect_json, keywords, faction, race, element, has_golden, quote_text) VALUES
(1, '野火罐', 'Wildfire Jar', 'equipment', 'rare', 2, 2, 2, '{"description_cn":"攻击力2，耐久度2","description_en":"2 Attack, 2 Durability"}', '["battlecry"]', 'lannister', NULL, 'fire', FALSE, 'Let them burn.');

INSERT IGNORE INTO cards (show_id, name_cn, name_en, card_type, rarity, cost, attack, health, effect_json, keywords, faction, race, element, has_golden, quote_text) VALUES
(1, '多斯拉克骑手', 'Dothraki Rider', 'minion', 'common', 2, 3, 1, '{"description_cn":"冲锋","description_en":"Rush"}', '["rush"]', 'targaryen', 'human', 'fire', FALSE, 'The Dothraki scream.');

INSERT IGNORE INTO cards (show_id, name_cn, name_en, card_type, rarity, cost, attack, health, effect_json, keywords, faction, race, element, has_golden, quote_text) VALUES
(1, '无垢者新兵', 'Unsullied Recruit', 'minion', 'common', 2, 2, 2, '{"description_cn":"","description_en":""}', '[]', 'targaryen', 'human', 'fire', FALSE, 'They are not men. They are soldiers.');

INSERT IGNORE INTO cards (show_id, name_cn, name_en, card_type, rarity, cost, attack, health, effect_json, keywords, faction, race, element, has_golden, quote_text) VALUES
(1, '铁群岛水手', 'Ironborn Sailor', 'minion', 'common', 2, 2, 2, '{"description_cn":"","description_en":""}', '[]', 'greyjoy', 'human', 'metal', FALSE, 'We do not sow.');

INSERT IGNORE INTO cards (show_id, name_cn, name_en, card_type, rarity, cost, attack, health, effect_json, keywords, faction, race, element, has_golden, quote_text) VALUES
(1, '提利尔哨兵', 'Tyrell Sentinel', 'minion', 'common', 2, 1, 4, '{"description_cn":"嘲讽","description_en":"Taunt"}', '["taunt"]', 'tyrell', 'human', 'nature', FALSE, '');

INSERT IGNORE INTO cards (show_id, name_cn, name_en, card_type, rarity, cost, attack, health, effect_json, keywords, faction, race, element, has_golden, quote_text) VALUES
(1, '渡鸦信使', 'Raven Courier', 'minion', 'common', 2, 1, 3, '{"description_cn":"战吼：抽1张牌","description_en":"Battlecry: Draw 1 card"}', '["battlecry"]', 'neutral', 'beast', 'nature', FALSE, '');

INSERT IGNORE INTO cards (show_id, name_cn, name_en, card_type, rarity, cost, attack, health, effect_json, keywords, faction, race, element, has_golden, quote_text) VALUES
(1, '雇佣剑士', 'Sellsword', 'minion', 'common', 2, 3, 1, '{"description_cn":"","description_en":""}', '[]', 'neutral', 'human', 'metal', FALSE, 'Gold buys loyalty.');

INSERT IGNORE INTO cards (show_id, name_cn, name_en, card_type, rarity, cost, attack, health, effect_json, keywords, faction, race, element, has_golden, quote_text) VALUES
(1, '君临间谍', 'King''s Landing Spy', 'minion', 'common', 2, 2, 1, '{"description_cn":"潜行","description_en":"Stealth"}', '["stealth"]', 'neutral', 'human', 'shadow', FALSE, 'Chaos is a ladder.');

INSERT IGNORE INTO cards (show_id, name_cn, name_en, card_type, rarity, cost, attack, health, effect_json, keywords, faction, race, element, has_golden, quote_text) VALUES
(1, '毒蛇之吻', 'Viper''s Kiss', 'spell', 'common', 2, NULL, NULL, '{"description_cn":"对一个敌方随从造成2点伤害，如果它死亡，抽1张牌","description_en":"Deal 2 damage to an enemy minion. If it dies, draw 1 card."}', '[]', 'martell', NULL, 'poison', FALSE, '');

INSERT IGNORE INTO cards (show_id, name_cn, name_en, card_type, rarity, cost, attack, health, effect_json, keywords, faction, race, element, has_golden, quote_text) VALUES
(1, '大提琴手', 'The Lute Player', 'minion', 'common', 2, 1, 3, '{"description_cn":"亡语：抽1张牌","description_en":"Deathrattle: Draw 1 card"}', '["deathrattle"]', 'neutral', 'human', 'light', FALSE, 'Music soothes the savage beast.');

INSERT IGNORE INTO cards (show_id, name_cn, name_en, card_type, rarity, cost, attack, health, effect_json, keywords, faction, race, element, has_golden, quote_text) VALUES
(1, '国王的嘉奖', 'King''s Reward', 'spell', 'common', 2, NULL, NULL, '{"description_cn":"恢复4点生命","description_en":"Restore 4 Health"}', '[]', 'neutral', NULL, 'light', FALSE, '');

INSERT IGNORE INTO cards (show_id, name_cn, name_en, card_type, rarity, cost, attack, health, effect_json, keywords, faction, race, element, has_golden, quote_text) VALUES
(1, '占卜师', 'Seer', 'minion', 'common', 2, 2, 2, '{"description_cn":"战吼：观看敌方手牌中1张牌","description_en":"Battlecry: Look at 1 card in the enemy''s hand"}', '["battlecry"]', 'neutral', 'human', 'shadow', FALSE, 'I see things.');

INSERT IGNORE INTO cards (show_id, name_cn, name_en, card_type, rarity, cost, attack, health, effect_json, keywords, faction, race, element, has_golden, quote_text) VALUES
(1, '食人魔守卫', 'Mountain Guard', 'minion', 'common', 2, 2, 3, '{"description_cn":"嘲讽","description_en":"Taunt"}', '["taunt"]', 'neutral', 'giant', 'fire', FALSE, '');

INSERT IGNORE INTO cards (show_id, name_cn, name_en, card_type, rarity, cost, attack, health, effect_json, keywords, faction, race, element, has_golden, quote_text) VALUES
(1, '贾昆·赫加尔', 'Jaqen H''ghar', 'minion', 'rare', 2, 2, 2, '{"description_cn":"潜行。亡语：抽2张牌","description_en":"Stealth. Deathrattle: Draw 2 cards"}', '["stealth", "deathrattle"]', 'neutral', 'human', 'shadow', FALSE, 'Valar morghulis.');

INSERT IGNORE INTO cards (show_id, name_cn, name_en, card_type, rarity, cost, attack, health, effect_json, keywords, faction, race, element, has_golden, quote_text) VALUES
(1, '维斯特洛商人', 'Westerosi Merchant', 'minion', 'common', 2, 2, 2, '{"description_cn":"战吼：获得1颗空白水晶（仅限本回合）","description_en":"Battlecry: Gain 1 empty Mana Crystal (this turn only)"}', '["battlecry"]', 'neutral', 'human', 'light', FALSE, 'A fair trade.');

-- ========== 3费 15张 ==========

INSERT IGNORE INTO cards (show_id, name_cn, name_en, card_type, rarity, cost, attack, health, effect_json, keywords, faction, race, element, has_golden, quote_text) VALUES
(1, '守夜人游骑兵', 'Night Watch Ranger', 'minion', 'rare', 3, 3, 3, '{"description_cn":"","description_en":""}', '[]', 'nightwatch', 'human', 'ice', FALSE, 'I am the watcher on the walls.');

INSERT IGNORE INTO cards (show_id, name_cn, name_en, card_type, rarity, cost, attack, health, effect_json, keywords, faction, race, element, has_golden, quote_text) VALUES
(1, '长爪', 'Longclaw', 'equipment', 'epic', 3, 2, 3, '{"description_cn":"攻击力2，耐久度3。你的守夜人随从获得+1/+1","description_en":"2 Attack, 3 Durability. Your Night Watch minions gain +1/+1."}', '["battlecry"]', 'nightwatch', NULL, 'metal', TRUE, 'It was forged in the fires of the Wall.');

INSERT IGNORE INTO cards (show_id, name_cn, name_en, card_type, rarity, cost, attack, health, effect_json, keywords, faction, race, element, has_golden, quote_text) VALUES
(1, '无垢者卫兵', 'Unsullied Guard', 'minion', 'rare', 3, 3, 4, '{"description_cn":"嘲讽","description_en":"Taunt"}', '["taunt"]', 'targaryen', 'human', 'fire', FALSE, 'Unsullied do not flee.');

INSERT IGNORE INTO cards (show_id, name_cn, name_en, card_type, rarity, cost, attack, health, effect_json, keywords, faction, race, element, has_golden, quote_text) VALUES
(1, '灰虫子', 'Grey Worm', 'minion', 'rare', 3, 3, 3, '{"description_cn":"战吼：对一名敌方随从造成2点伤害","description_en":"Battlecry: Deal 2 damage to an enemy minion"}', '["battlecry"]', 'targaryen', 'human', 'fire', FALSE, 'I am Grey Worm. I lead.');

INSERT IGNORE INTO cards (show_id, name_cn, name_en, card_type, rarity, cost, attack, health, effect_json, keywords, faction, race, element, has_golden, quote_text) VALUES
(1, '马泰尔游击兵', 'Martell Skirmisher', 'minion', 'rare', 3, 3, 2, '{"description_cn":"剧毒","description_en":"Poisonous"}', '["poison"]', 'martell', 'human', 'poison', FALSE, '');

INSERT IGNORE INTO cards (show_id, name_cn, name_en, card_type, rarity, cost, attack, health, effect_json, keywords, faction, race, element, has_golden, quote_text) VALUES
(1, '多斯拉克骑兵', 'Dothraki Horseman', 'spell', 'common', 3, NULL, NULL, '{"description_cn":"召唤一个4/2多斯拉克骑兵，具有冲锋","description_en":"Summon a 4/2 Dothraki Horseman with Rush"}', '["rush"]', 'targaryen', NULL, 'fire', FALSE, '');

INSERT IGNORE INTO cards (show_id, name_cn, name_en, card_type, rarity, cost, attack, health, effect_json, keywords, faction, race, element, has_golden, quote_text) VALUES
(1, '提利尔骑士', 'Tyrell Knight', 'spell', 'common', 3, NULL, NULL, '{"description_cn":"召唤一个3/3提利尔骑士","description_en":"Summon a 3/3 Tyrell Knight"}', '[]', 'tyrell', NULL, 'nature', FALSE, '');

INSERT IGNORE INTO cards (show_id, name_cn, name_en, card_type, rarity, cost, attack, health, effect_json, keywords, faction, race, element, has_golden, quote_text) VALUES
(1, '学士', 'Maester', 'minion', 'rare', 3, 2, 4, '{"description_cn":"战吼：恢复3点生命","description_en":"Battlecry: Restore 3 Health"}', '["battlecry"]', 'neutral', 'human', 'light', FALSE, 'A maester serves.');

INSERT IGNORE INTO cards (show_id, name_cn, name_en, card_type, rarity, cost, attack, health, effect_json, keywords, faction, race, element, has_golden, quote_text) VALUES
(1, '艾拉莉亚·沙德', 'Ellaria Sand', 'minion', 'rare', 3, 3, 2, '{"description_cn":"潜行。剧毒","description_en":"Stealth. Poisonous."}', '["stealth", "poison"]', 'martell', 'human', 'poison', FALSE, 'I am a sand snake.');

INSERT IGNORE INTO cards (show_id, name_cn, name_en, card_type, rarity, cost, attack, health, effect_json, keywords, faction, race, element, has_golden, quote_text) VALUES
(1, '达里奥·纳哈里斯', 'Daario Naharis', 'minion', 'rare', 3, 3, 3, '{"description_cn":"冲锋","description_en":"Rush"}', '["rush"]', 'targaryen', 'human', 'fire', FALSE, 'I am Daario Naharis. I am a sellsword.');

INSERT IGNORE INTO cards (show_id, name_cn, name_en, card_type, rarity, cost, attack, health, effect_json, keywords, faction, race, element, has_golden, quote_text) VALUES
(1, '蓝礼·拜拉席恩', 'Renly Baratheon', 'minion', 'rare', 3, 3, 4, '{"description_cn":"战吼：你的其他随从获得+1/+1","description_en":"Battlecry: Give your other minions +1/+1"}', '["battlecry"]', 'baratheon', 'human', 'lightning', FALSE, 'I am the rightful king.');

INSERT IGNORE INTO cards (show_id, name_cn, name_en, card_type, rarity, cost, attack, health, effect_json, keywords, faction, race, element, has_golden, quote_text) VALUES
(1, '狼群战术', 'Wolf Pack Tactics', 'spell', 'rare', 3, NULL, NULL, '{"description_cn":"召唤3个1/1冰原狼","description_en":"Summon three 1/1 Direwolves"}', '["battlecry"]', 'stark', NULL, 'ice', FALSE, 'The pack survives.');

INSERT IGNORE INTO cards (show_id, name_cn, name_en, card_type, rarity, cost, attack, health, effect_json, keywords, faction, race, element, has_golden, quote_text) VALUES
(1, '铁群岛长船', 'Ironborn Longship', 'spell', 'rare', 3, NULL, NULL, '{"description_cn":"召唤一个3/4铁群岛长船，具有嘲讽","description_en":"Summon a 3/4 Ironborn Longship with Taunt"}', '["taunt"]', 'greyjoy', NULL, 'metal', FALSE, 'What is dead may never die.');

INSERT IGNORE INTO cards (show_id, name_cn, name_en, card_type, rarity, cost, attack, health, effect_json, keywords, faction, race, element, has_golden, quote_text) VALUES
(1, '洛拉斯·提利尔', 'Loras Tyrell', 'minion', 'rare', 3, 4, 2, '{"description_cn":"冲锋","description_en":"Rush"}', '["rush"]', 'tyrell', 'human', 'nature', FALSE, 'I am the Knight of Flowers.');

INSERT IGNORE INTO cards (show_id, name_cn, name_en, card_type, rarity, cost, attack, health, effect_json, keywords, faction, race, element, has_golden, quote_text) VALUES
(1, '莱安娜·莫尔蒙', 'Lyanna Mormont', 'minion', 'rare', 3, 2, 4, '{"description_cn":"嘲讽。战吼：所有友方随从获得+1攻击力","description_en":"Taunt. Battlecry: Give all friendly minions +1 Attack."}', '["taunt", "battlecry"]', 'stark', 'human', 'ice', FALSE, 'I don''t care if you are a girl. I am a bear!');

-- ========== 4费 10张 ==========

INSERT IGNORE INTO cards (show_id, name_cn, name_en, card_type, rarity, cost, attack, health, effect_json, keywords, faction, race, element, has_golden, quote_text) VALUES
(1, '红毒蛇·奥伯伦', 'Oberyn Martell', 'minion', 'epic', 4, 4, 3, '{"description_cn":"剧毒。战吼：对敌方英雄造成3点伤害","description_en":"Poisonous. Battlecry: Deal 3 damage to the enemy hero."}', '["battlecry", "poison"]', 'martell', 'human', 'poison', TRUE, 'I will be your champion.');

INSERT IGNORE INTO cards (show_id, name_cn, name_en, card_type, rarity, cost, attack, health, effect_json, keywords, faction, race, element, has_golden, quote_text) VALUES
(1, '巨人克星·托蒙德', 'Tormund Giantsbane', 'minion', 'epic', 4, 4, 5, '{"description_cn":"嘲讽","description_en":"Taunt"}', '["taunt", "battlecry"]', 'neutral', 'human', 'ice', TRUE, 'I have blue eyes and a blue penis.');

INSERT IGNORE INTO cards (show_id, name_cn, name_en, card_type, rarity, cost, attack, health, effect_json, keywords, faction, race, element, has_golden, quote_text) VALUES
(1, '铁金库使者', 'Iron Bank Envoy', 'minion', 'epic', 4, 4, 4, '{"description_cn":"战吼：获得2颗空白水晶","description_en":"Battlecry: Gain 2 empty Mana Crystals"}', '["battlecry"]', 'neutral', 'human', 'metal', TRUE, 'The Iron Bank will have its due.');

INSERT IGNORE INTO cards (show_id, name_cn, name_en, card_type, rarity, cost, attack, health, effect_json, keywords, faction, race, element, has_golden, quote_text) VALUES
(1, '易形者·布兰', 'Bran the Greenseer', 'minion', 'epic', 4, 2, 5, '{"description_cn":"潜行。在你的回合开始时，观看敌方手牌中的所有牌","description_en":"Stealth. At the start of your turn, look at all cards in the enemy''s hand."}', '["stealth"]', 'stark', 'human', 'nature', TRUE, 'I see everything now.');

INSERT IGNORE INTO cards (show_id, name_cn, name_en, card_type, rarity, cost, attack, health, effect_json, keywords, faction, race, element, has_golden, quote_text) VALUES
(1, '碎心', 'Shattered Heart', 'equipment', 'rare', 4, 4, 2, '{"description_cn":"攻击力4，耐久度2","description_en":"4 Attack, 2 Durability"}', '["battlecry"]', 'neutral', NULL, 'metal', FALSE, 'The Mountain crushes all.');

INSERT IGNORE INTO cards (show_id, name_cn, name_en, card_type, rarity, cost, attack, health, effect_json, keywords, faction, race, element, has_golden, quote_text) VALUES
(1, '大麻雀', 'High Sparrow', 'minion', 'common', 4, 3, 6, '{"description_cn":"嘲讽","description_en":"Taunt"}', '["taunt"]', 'neutral', 'human', 'light', FALSE, 'The gods have mercy, but I do not.');

INSERT IGNORE INTO cards (show_id, name_cn, name_en, card_type, rarity, cost, attack, health, effect_json, keywords, faction, race, element, has_golden, quote_text) VALUES
(1, '瓦雷利亚钢剑', 'Valyrian Steel Sword', 'equipment', 'epic', 4, 3, 3, '{"description_cn":"攻击力3，耐久度3。攻击时无视嘲讽","description_en":"3 Attack, 3 Durability. Ignore Taunt when attacking."}', '["battlecry"]', 'neutral', NULL, 'metal', TRUE, 'Valyrian steel. Forged in dragonfire.');

INSERT IGNORE INTO cards (show_id, name_cn, name_en, card_type, rarity, cost, attack, health, effect_json, keywords, faction, race, element, has_golden, quote_text) VALUES
(1, '无畏的艾迪', 'Dolorous Edd', 'minion', 'epic', 4, 3, 5, '{"description_cn":"嘲讽。亡语：抽2张牌","description_en":"Taunt. Deathrattle: Draw 2 cards."}', '["taunt", "deathrattle"]', 'nightwatch', 'human', 'ice', TRUE, 'I always wanted to die in my sleep.');

INSERT IGNORE INTO cards (show_id, name_cn, name_en, card_type, rarity, cost, attack, health, effect_json, keywords, faction, race, element, has_golden, quote_text) VALUES
(1, '听我怒吼', 'Hear Me Roar', 'location', 'epic', 4, NULL, NULL, '{"description_cn":"在本回合中，你的角色获得+3攻击力","description_en":"Your characters gain +3 Attack this turn."}', '["battlecry"]', 'lannister', NULL, 'metal', TRUE, 'Hear me roar!');

INSERT IGNORE INTO cards (show_id, name_cn, name_en, card_type, rarity, cost, attack, health, effect_json, keywords, faction, race, element, has_golden, quote_text) VALUES
(1, '龙晶匕首', 'Dragonglass Dagger', 'equipment', 'common', 4, 3, 2, '{"description_cn":"攻击力3，耐久度2","description_en":"3 Attack, 2 Durability"}', '[]', 'neutral', NULL, 'metal', FALSE, 'Dragonglass kills white walkers.');

-- ========== 5费 5张 ==========

INSERT IGNORE INTO cards (show_id, name_cn, name_en, card_type, rarity, cost, attack, health, effect_json, keywords, faction, race, element, has_golden, quote_text) VALUES
(1, '无面者·艾莉亚', 'Arya Stark, Faceless Girl', 'minion', 'legendary', 5, 4, 4, '{"description_cn":"潜行。战吼：变为复制一个敌方随从","description_en":"Stealth. Battlecry: Transform into a copy of an enemy minion."}', '["stealth", "battlecry"]', 'stark', 'human', 'shadow', TRUE, 'A girl is Arya Stark. And I''m going home.');

INSERT IGNORE INTO cards (show_id, name_cn, name_en, card_type, rarity, cost, attack, health, effect_json, keywords, faction, race, element, has_golden, quote_text) VALUES
(1, '国王之手·提利昂', 'Tyrion, Hand of the King', 'minion', 'legendary', 5, 3, 6, '{"description_cn":"战吼：你本回合使用的下一张卡牌费用减3","description_en":"Battlecry: The next card you play this turn costs 3 less."}', '["battlecry", "taunt"]', 'lannister', 'human', 'light', TRUE, 'I drink and I know things.');

INSERT IGNORE INTO cards (show_id, name_cn, name_en, card_type, rarity, cost, attack, health, effect_json, keywords, faction, race, element, has_golden, quote_text) VALUES
(1, '红袍女巫·梅丽珊卓', 'Melisandre, the Red Woman', 'minion', 'epic', 5, 3, 5, '{"description_cn":"战吼：复活一个本局死去的友方传说随从","description_en":"Battlecry: Resurrect a friendly Legendary minion that died this game."}', '["battlecry", "deathrattle"]', 'neutral', 'human', 'fire', TRUE, 'The night is dark and full of terrors.');

INSERT IGNORE INTO cards (show_id, name_cn, name_en, card_type, rarity, cost, attack, health, effect_json, keywords, faction, race, element, has_golden, quote_text) VALUES
(1, '八爪蜘蛛·瓦里斯', 'Lord Varys, the Spider', 'minion', 'epic', 5, 2, 6, '{"description_cn":"潜行。战吼：将1张敌方手牌的复制加入你的手牌","description_en":"Stealth. Battlecry: Add a copy of an enemy card to your hand."}', '["stealth", "battlecry"]', 'neutral', 'human', 'shadow', TRUE, 'Power resides where men believe it resides.');

INSERT IGNORE INTO cards (show_id, name_cn, name_en, card_type, rarity, cost, attack, health, effect_json, keywords, faction, race, element, has_golden, quote_text) VALUES
(1, '寒冰', 'Ice', 'equipment', 'epic', 5, 5, 3, '{"description_cn":"攻击力5，耐久度3。攻击时获得+2护甲","description_en":"5 Attack, 3 Durability. Gain +2 Armor when attacking."}', '["battlecry"]', 'stark', NULL, 'ice', TRUE, 'The man who passes the sentence should swing the sword.');

INSERT IGNORE INTO cards (show_id, name_cn, name_en, card_type, rarity, cost, attack, health, effect_json, keywords, faction, race, element, has_golden, quote_text) VALUES
(1, '龙之号角', 'Dragon Horn', 'equipment', 'rare', 5, 3, 3, '{"description_cn":"攻击力3，耐久度3。每当你的龙随从攻击，这件武器不消耗耐久","description_en":"3 Attack, 3 Durability. Whenever one of your Dragon minions attacks, this weapon doesn''t lose Durability."}', '["battlecry"]', 'targaryen', NULL, 'fire', FALSE, '');

-- ========== 6费 5张 ==========

INSERT IGNORE INTO cards (show_id, name_cn, name_en, card_type, rarity, cost, attack, health, effect_json, keywords, faction, race, element, has_golden, quote_text) VALUES
(1, '北境之王·琼恩雪诺', 'Jon Snow, King in the North', 'minion', 'legendary', 6, 5, 6, '{"description_cn":"战吼：复活所有在本局死去的友方史塔克随从","description_en":"Battlecry: Resurrect all friendly Stark minions that died this game."}', '["battlecry", "taunt"]', 'stark', 'human', 'ice', TRUE, 'I don''t want it. I never have.');

INSERT IGNORE INTO cards (show_id, name_cn, name_en, card_type, rarity, cost, attack, health, effect_json, keywords, faction, race, element, has_golden, quote_text) VALUES
(1, '弑君者·詹姆', 'Jaime, the Kingslayer', 'minion', 'legendary', 6, 6, 5, '{"description_cn":"冲锋。圣盾","description_en":"Rush. Divine Shield."}', '["rush", "divine_shield"]', 'lannister', 'human', 'metal', TRUE, 'By what right does the wolf judge the lion?');

INSERT IGNORE INTO cards (show_id, name_cn, name_en, card_type, rarity, cost, attack, health, effect_json, keywords, faction, race, element, has_golden, quote_text) VALUES
(1, '巴隆·葛雷乔伊', 'Balon Greyjoy', 'minion', 'epic', 6, 5, 5, '{"description_cn":"战吼：对所有未受伤的敌方随从造成3点伤害","description_en":"Battlecry: Deal 3 damage to all undamaged enemy minions."}', '["battlecry", "rush"]', 'greyjoy', 'human', 'metal', TRUE, 'We are ironborn!');

INSERT IGNORE INTO cards (show_id, name_cn, name_en, card_type, rarity, cost, attack, health, effect_json, keywords, faction, race, element, has_golden, quote_text) VALUES
(1, '美人·布蕾妮', 'Brienne, the Beauty', 'minion', 'epic', 6, 6, 6, '{"description_cn":"嘲讽。圣盾","description_en":"Taunt. Divine Shield."}', '["taunt", "divine_shield"]', 'neutral', 'human', 'light', TRUE, 'I serve the living. I serve the realm.');

INSERT IGNORE INTO cards (show_id, name_cn, name_en, card_type, rarity, cost, attack, health, effect_json, keywords, faction, race, element, has_golden, quote_text) VALUES
(1, '守夜人指挥官', 'Night Watch Commander', 'minion', 'epic', 6, 4, 6, '{"description_cn":"战吼：所有守夜人随从获得+2/+2","description_en":"Battlecry: Give all Night Watch minions +2/+2."}', '["battlecry", "taunt"]', 'nightwatch', 'human', 'ice', TRUE, 'Night gathers, and now my watch begins.');

-- ========== 7费 3张 ==========

INSERT IGNORE INTO cards (show_id, name_cn, name_en, card_type, rarity, cost, attack, health, effect_json, keywords, faction, race, element, has_golden, quote_text) VALUES
(1, '女王·瑟曦', 'Queen Cersei', 'minion', 'legendary', 7, 5, 9, '{"description_cn":"战吼：对所有敌方角色造成3点伤害","description_en":"Battlecry: Deal 3 damage to all enemy characters."}', '["battlecry", "deathrattle"]', 'lannister', 'human', 'fire', TRUE, 'I choose violence.');

INSERT IGNORE INTO cards (show_id, name_cn, name_en, card_type, rarity, cost, attack, health, effect_json, keywords, faction, race, element, has_golden, quote_text) VALUES
(1, '凛冬将至', 'Winter is Coming', 'location', 'epic', 7, NULL, NULL, '{"description_cn":"对敌方全体造成4点伤害，冻结所有受伤的角色","description_en":"Deal 4 damage to all enemies. Freeze all damaged characters."}', '["deathrattle", "divine_shield"]', 'stark', NULL, 'ice', TRUE, 'Winter is coming.');

INSERT IGNORE INTO cards (show_id, name_cn, name_en, card_type, rarity, cost, attack, health, effect_json, keywords, faction, race, element, has_golden, quote_text) VALUES
(1, '卓耿', 'Drogon', 'minion', 'legendary', 7, 8, 8, '{"description_cn":"冲锋。攻击时对相邻随从造成3点伤害","description_en":"Charge. Deal 3 damage to adjacent minions when attacking."}', '["rush", "taunt"]', 'targaryen', 'dragon', 'fire', TRUE, 'Dracarys!');

-- ========== 8费 3张 ==========

INSERT IGNORE INTO cards (show_id, name_cn, name_en, card_type, rarity, cost, attack, health, effect_json, keywords, faction, race, element, has_golden, quote_text) VALUES
(1, '夜王', 'Night King', 'minion', 'legendary', 8, 8, 8, '{"description_cn":"战吼：消灭所有敌方随从","description_en":"Battlecry: Destroy all enemy minions."}', '["battlecry", "deathrattle"]', 'neutral', 'undead', 'ice', TRUE, 'Winter is coming for all of you.');

INSERT IGNORE INTO cards (show_id, name_cn, name_en, card_type, rarity, cost, attack, health, effect_json, keywords, faction, race, element, has_golden, quote_text) VALUES
(1, '龙之母·丹妮莉丝', 'Daenerys, Mother of Dragons', 'minion', 'legendary', 8, 7, 8, '{"description_cn":"在你的回合结束时，召唤一个3/3幼龙","description_en":"At the end of your turn, summon a 3/3 Dragon."}', '["battlecry", "taunt"]', 'targaryen', 'human', 'fire', TRUE, 'I was born to rule the Seven Kingdoms.');

INSERT IGNORE INTO cards (show_id, name_cn, name_en, card_type, rarity, cost, attack, health, effect_json, keywords, faction, race, element, has_golden, quote_text) VALUES
(1, '史塔克家族', 'House Stark', 'spell', 'legendary', 8, NULL, NULL, '{"description_cn":"每回合结束时，使你所有史塔克随从获得+1/+1","description_en":"At the end of each turn, give all your Stark minions +1/+1."}', '["taunt"]', 'stark', NULL, 'ice', TRUE, 'The lone wolf dies, but the pack survives.');

INSERT IGNORE INTO cards (show_id, name_cn, name_en, card_type, rarity, cost, attack, health, effect_json, keywords, faction, race, element, has_golden, quote_text) VALUES
(1, '兰尼斯特家族', 'House Lannister', 'spell', 'legendary', 8, NULL, NULL, '{"description_cn":"每当你使用一张牌，获得1点生命。每回合额外获得1点费用","description_en":"Whenever you play a card, gain 1 Health. Gain 1 extra mana each turn."}', '["taunt"]', 'lannister', NULL, 'metal', TRUE, 'A Lannister always pays his debts.');

-- 总计80张卡
