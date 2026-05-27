-- V33: 权游卡牌扩充 (33张 → 60张)
-- 新增 10 名角色 + 17 张法术/兵种/装备/场地
-- 使用 INSERT IGNORE ... SELECT 模式关联 show_id

-- ========== 新增角色 (Characters) ==========

-- 1. 艾莉亚·史塔克 (epic, 3费3/2, human+shadow, stark)
INSERT IGNORE INTO cards (show_id, name_cn, name_en, card_type, rarity, cost, attack, health, effect_json, keywords, faction, quote_text)
SELECT s.id, '艾莉亚·史塔克', 'Arya Stark', 'minion', 'epic', 3, 3, 2,
 '{"description_cn":"潜行。攻击时额外造成2点伤害。","description_en":"Stealth. Deal 2 extra damage when attacking."}',
 '["stealth"]', 'stark', 'Not today.'
FROM shows s WHERE s.name LIKE '%S01E01%' AND (s.name LIKE '%Game of Thrones%' OR s.name LIKE '%GOT%')
LIMIT 1;

-- 2. 凯特琳·史塔克 (epic, 4费3/5, human+ice, stark)
INSERT IGNORE INTO cards (show_id, name_cn, name_en, card_type, rarity, cost, attack, health, effect_json, keywords, faction, quote_text)
SELECT s.id, '凯特琳·史塔克', 'Catelyn Stark', 'minion', 'epic', 4, 3, 5,
 '{"description_cn":"战吼：使一个友方随从获得+2生命值和嘲讽。","description_en":"Battlecry: Give a friendly minion +2 Health and Taunt."}',
 '["battlecry"]', 'stark', 'Family. Duty. Honor.'
FROM shows s WHERE s.name LIKE '%S01E01%' AND (s.name LIKE '%Game of Thrones%' OR s.name LIKE '%GOT%')
LIMIT 1;

-- 3. 劳勃·拜拉席恩 (legendary, 7费8/5, human+metal, baratheon)
INSERT IGNORE INTO cards (show_id, name_cn, name_en, card_type, rarity, cost, attack, health, effect_json, keywords, faction, quote_text)
SELECT s.id, '劳勃·拜拉席恩', 'Robert Baratheon', 'minion', 'legendary', 7, 8, 5,
 '{"description_cn":"冲锋。战吼：对所有敌方随从造成2点伤害。","description_en":"Charge. Battlecry: Deal 2 damage to all enemy minions."}',
 '["charge","battlecry"]', 'baratheon', 'Give me something for the pain and let me die.'
FROM shows s WHERE s.name LIKE '%S01E01%' AND (s.name LIKE '%Game of Thrones%' OR s.name LIKE '%GOT%')
LIMIT 1;

-- 4. 乔佛里·拜拉席恩 (epic, 4费4/3, human+shadow, lannister)
INSERT IGNORE INTO cards (show_id, name_cn, name_en, card_type, rarity, cost, attack, health, effect_json, keywords, faction, quote_text)
SELECT s.id, '乔佛里·拜拉席恩', 'Joffrey Baratheon', 'minion', 'epic', 4, 4, 3,
 '{"description_cn":"亡语：召唤一个2/1的\"御林铁卫\"。","description_en":"Deathrattle: Summon a 2/1 Kingsguard."}',
 '["deathrattle"]', 'lannister', 'I am the king!'
FROM shows s WHERE s.name LIKE '%S01E01%' AND (s.name LIKE '%Game of Thrones%' OR s.name LIKE '%GOT%')
LIMIT 1;

-- 5. 詹姆·兰尼斯特 (epic, 5费5/4, human+metal, lannister)
INSERT IGNORE INTO cards (show_id, name_cn, name_en, card_type, rarity, cost, attack, health, effect_json, keywords, faction, quote_text)
SELECT s.id, '詹姆·兰尼斯特', 'Jaime Lannister', 'minion', 'epic', 5, 5, 4,
 '{"description_cn":"冲锋。攻击时抽1张牌。","description_en":"Charge. Draw 1 card when attacking."}',
 '["charge"]', 'lannister', 'The things I do for love.'
FROM shows s WHERE s.name LIKE '%S01E01%' AND (s.name LIKE '%Game of Thrones%' OR s.name LIKE '%GOT%')
LIMIT 1;

-- 6. 猎狗 (rare, 4费5/3, human+fire, neutral)
INSERT IGNORE INTO cards (show_id, name_cn, name_en, card_type, rarity, cost, attack, health, effect_json, keywords, faction, quote_text)
SELECT s.id, '猎狗', 'The Hound', 'minion', 'rare', 4, 5, 3,
 '{"description_cn":"嘲讽。受到伤害时获得+1攻击力。","description_en":"Taunt. Gain +1 Attack when damaged."}',
 '["taunt"]', 'neutral', 'I understand that if any more words come pouring out your cunt mouth, I''m gonna have to eat every fucking chicken in this room.'
FROM shows s WHERE s.name LIKE '%S01E01%' AND (s.name LIKE '%Game of Thrones%' OR s.name LIKE '%GOT%')
LIMIT 1;

-- 7. 魔山 (rare, 5费6/2, human+shadow, neutral)
INSERT IGNORE INTO cards (show_id, name_cn, name_en, card_type, rarity, cost, attack, health, effect_json, keywords, faction, quote_text)
SELECT s.id, '魔山', 'The Mountain', 'minion', 'rare', 5, 6, 2,
 '{"description_cn":"冲锋。无法被法术指定为目标。","description_en":"Charge. Cannot be targeted by spells."}',
 '["charge"]', 'neutral', 'I am the strongest.'
FROM shows s WHERE s.name LIKE '%S01E01%' AND (s.name LIKE '%Game of Thrones%' OR s.name LIKE '%GOT%')
LIMIT 1;

-- 8. 布兰·史塔克 (rare, 2费1/3, human+ice, stark)
INSERT IGNORE INTO cards (show_id, name_cn, name_en, card_type, rarity, cost, attack, health, effect_json, keywords, faction, quote_text)
SELECT s.id, '布兰·史塔克', 'Bran Stark', 'minion', 'rare', 2, 1, 3,
 '{"description_cn":"在你的回合结束时，抽1张牌。","description_en":"At the end of your turn, draw 1 card."}',
 '[]', 'stark', 'Old stories are like old friends. You have to visit them from time to time.'
FROM shows s WHERE s.name LIKE '%S01E01%' AND (s.name LIKE '%Game of Thrones%' OR s.name LIKE '%GOT%')
LIMIT 1;

-- 9. 罗柏·史塔克 (epic, 5费5/4, human+ice, stark)
INSERT IGNORE INTO cards (show_id, name_cn, name_en, card_type, rarity, cost, attack, health, effect_json, keywords, faction, quote_text)
SELECT s.id, '罗柏·史塔克', 'Robb Stark', 'minion', 'epic', 5, 5, 4,
 '{"description_cn":"战吼：使所有友方史塔克随从获得+1/+1。","description_en":"Battlecry: Give all friendly Stark minions +1/+1."}',
 '["battlecry"]', 'stark', 'The king in the North!'
FROM shows s WHERE s.name LIKE '%S01E01%' AND (s.name LIKE '%Game of Thrones%' OR s.name LIKE '%GOT%')
LIMIT 1;

-- 10. 玛格丽·提利尔 (rare, 3费2/4, human+light, tyrell)
INSERT IGNORE INTO cards (show_id, name_cn, name_en, card_type, rarity, cost, attack, health, effect_json, keywords, faction, quote_text)
SELECT s.id, '玛格丽·提利尔', 'Margaery Tyrell', 'minion', 'rare', 3, 2, 4,
 '{"description_cn":"战吼：恢复4点生命值。","description_en":"Battlecry: Restore 4 Health."}',
 '["battlecry"]', 'tyrell', 'I am the queen.'
FROM shows s WHERE s.name LIKE '%S01E01%' AND (s.name LIKE '%Game of Thrones%' OR s.name LIKE '%GOT%')
LIMIT 1;

-- ========== 新增兵种 (Units) ==========

-- 11. 守夜人斥候 (common, 2费2/1, nightwatch)
INSERT IGNORE INTO cards (show_id, name_cn, name_en, card_type, rarity, cost, attack, health, effect_json, keywords, faction, quote_text)
SELECT s.id, '守夜人斥候', 'Night Watch Scout', 'minion', 'common', 2, 2, 1,
 '{"description_cn":"潜行。","description_en":"Stealth."}',
 '["stealth"]', 'nightwatch', 'I am the sword in the darkness.'
FROM shows s WHERE s.name LIKE '%S01E01%' AND (s.name LIKE '%Game of Thrones%' OR s.name LIKE '%GOT%')
LIMIT 1;

-- 12. 多斯拉克骑手 (common, 3费3/2, targaryen)
INSERT IGNORE INTO cards (show_id, name_cn, name_en, card_type, rarity, cost, attack, health, effect_json, keywords, faction, quote_text)
SELECT s.id, '多斯拉克骑手', 'Dothraki Rider', 'minion', 'common', 3, 3, 2,
 '{"description_cn":"冲锋。","description_en":"Charge."}',
 '["charge"]', 'targaryen', 'A khal who cannot ride is no khal.'
FROM shows s WHERE s.name LIKE '%S01E01%' AND (s.name LIKE '%Game of Thrones%' OR s.name LIKE '%GOT%')
LIMIT 1;

-- 13. 无垢者 (rare, 4费3/4, targaryen)
INSERT IGNORE INTO cards (show_id, name_cn, name_en, card_type, rarity, cost, attack, health, effect_json, keywords, faction, quote_text)
SELECT s.id, '无垢者', 'Unsullied', 'minion', 'rare', 4, 3, 4,
 '{"description_cn":"圣盾。无法被恐惧。","description_en":"Divine Shield. Cannot be frightened."}',
 '["divine_shield"]', 'targaryen', 'I am not a soldier.'
FROM shows s WHERE s.name LIKE '%S01E01%' AND (s.name LIKE '%Game of Thrones%' OR s.name LIKE '%GOT%')
LIMIT 1;

-- 14. 野人劫掠者 (common, 3费4/2, neutral)
INSERT IGNORE INTO cards (show_id, name_cn, name_en, card_type, rarity, cost, attack, health, effect_json, keywords, faction, quote_text)
SELECT s.id, '野人劫掠者', 'Wildling Raider', 'minion', 'common', 3, 4, 2,
 '{"description_cn":"","description_en":""}',
 '[]', 'neutral', 'The free folk do not kneel.'
FROM shows s WHERE s.name LIKE '%S01E01%' AND (s.name LIKE '%Game of Thrones%' OR s.name LIKE '%GOT%')
LIMIT 1;

-- 15. 御林铁卫 (rare, 5费3/6, lannister)
INSERT IGNORE INTO cards (show_id, name_cn, name_en, card_type, rarity, cost, attack, health, effect_json, keywords, faction, quote_text)
SELECT s.id, '御林铁卫', 'Kingsguard', 'minion', 'rare', 5, 3, 6,
 '{"description_cn":"嘲讽。","description_en":"Taunt."}',
 '["taunt"]', 'lannister', 'I am the shield that guards the realms of men.'
FROM shows s WHERE s.name LIKE '%S01E01%' AND (s.name LIKE '%Game of Thrones%' OR s.name LIKE '%GOT%')
LIMIT 1;

-- 16. 鸦巢信使 (common, 1费1/1, neutral)
INSERT IGNORE INTO cards (show_id, name_cn, name_en, card_type, rarity, cost, attack, health, effect_json, keywords, faction, quote_text)
SELECT s.id, '鸦巢信使', 'Raven Messenger', 'minion', 'common', 1, 1, 1,
 '{"description_cn":"亡语：抽1张牌。","description_en":"Deathrattle: Draw 1 card."}',
 '["deathrattle"]', 'neutral', 'A raven flies.'
FROM shows s WHERE s.name LIKE '%S01E01%' AND (s.name LIKE '%Game of Thrones%' OR s.name LIKE '%GOT%')
LIMIT 1;

-- 17. 铁金库使者 (rare, 3费2/3, neutral)
INSERT IGNORE INTO cards (show_id, name_cn, name_en, card_type, rarity, cost, attack, health, effect_json, keywords, faction, quote_text)
SELECT s.id, '铁金库使者', 'Iron Bank Emissary', 'minion', 'rare', 3, 2, 3,
 '{"description_cn":"战吼：获得2点额外法力水晶（仅本回合）。","description_en":"Battlecry: Gain 2 temporary Mana Crystals this turn only."}',
 '["battlecry"]', 'neutral', 'The Iron Bank will have its due.'
FROM shows s WHERE s.name LIKE '%S01E01%' AND (s.name LIKE '%Game of Thrones%' OR s.name LIKE '%GOT%')
LIMIT 1;

-- 18. 学城学士 (common, 2费1/3, neutral)
INSERT IGNORE INTO cards (show_id, name_cn, name_en, card_type, rarity, cost, attack, health, effect_json, keywords, faction, quote_text)
SELECT s.id, '学城学士', 'Maester', 'minion', 'common', 2, 1, 3,
 '{"description_cn":"在你的回合结束时，恢复2点生命值。","description_en":"At the end of your turn, restore 2 Health."}',
 '[]', 'neutral', 'A maester serves the realm.'
FROM shows s WHERE s.name LIKE '%S01E01%' AND (s.name LIKE '%Game of Thrones%' OR s.name LIKE '%GOT%')
LIMIT 1;

-- ========== 新增法术 (Spells) ==========

-- 19. 长城守卫 (rare, 2费, nightwatch)
INSERT IGNORE INTO cards (show_id, name_cn, name_en, card_type, rarity, cost, attack, health, effect_json, keywords, faction, quote_text)
SELECT s.id, '长城守卫', 'The Wall Defends', 'spell', 'rare', 2, NULL, NULL,
 '{"description_cn":"使一个友方随从获得+0/+4。","description_en":"Give a friendly minion +0/+4."}',
 '[]', 'nightwatch', 'The Wall is yours.'
FROM shows s WHERE s.name LIKE '%S01E01%' AND (s.name LIKE '%Game of Thrones%' OR s.name LIKE '%GOT%')
LIMIT 1;

-- 20. 异鬼入侵 (epic, 7费, neutral)
INSERT IGNORE INTO cards (show_id, name_cn, name_en, card_type, rarity, cost, attack, health, effect_json, keywords, faction, quote_text)
SELECT s.id, '异鬼入侵', 'White Walker Invasion', 'spell', 'epic', 7, NULL, NULL,
 '{"description_cn":"消灭所有敌方随从，然后召唤一个5/5的\"异鬼\"。","description_en":"Destroy all enemy minions, then summon a 5/5 White Walker."}',
 '[]', 'neutral', 'The night is dark and full of terrors.'
FROM shows s WHERE s.name LIKE '%S01E01%' AND (s.name LIKE '%Game of Thrones%' OR s.name LIKE '%GOT%')
LIMIT 1;

-- 21. 多斯拉克冲锋 (rare, 4费, targaryen)
INSERT IGNORE INTO cards (show_id, name_cn, name_en, card_type, rarity, cost, attack, health, effect_json, keywords, faction, quote_text)
SELECT s.id, '多斯拉克冲锋', 'Dothraki Charge', 'spell', 'rare', 4, NULL, NULL,
 '{"description_cn":"召唤三个2/1的多斯拉克骑手。","description_en":"Summon three 2/1 Dothraki Riders."}',
 '[]', 'targaryen', 'The Dothraki are the most savage warriors.'
FROM shows s WHERE s.name LIKE '%S01E01%' AND (s.name LIKE '%Game of Thrones%' OR s.name LIKE '%GOT%')
LIMIT 1;

-- 22. 无面者的刺杀 (epic, 5费, neutral)
INSERT IGNORE INTO cards (show_id, name_cn, name_en, card_type, rarity, cost, attack, health, effect_json, keywords, faction, quote_text)
SELECT s.id, '无面者的刺杀', 'Faceless Assassin', 'spell', 'epic', 5, NULL, NULL,
 '{"description_cn":"消灭一个敌方随从。如果是传说随从，改为消灭并抽2张牌。","description_en":"Destroy an enemy minion. If Legendary, destroy and draw 2 cards."}',
 '[]', 'neutral', 'Valar dohaeris.'
FROM shows s WHERE s.name LIKE '%S01E01%' AND (s.name LIKE '%Game of Thrones%' OR s.name LIKE '%GOT%')
LIMIT 1;

-- 23. 烧死女巫 (rare, 3费, neutral)
INSERT IGNORE INTO cards (show_id, name_cn, name_en, card_type, rarity, cost, attack, health, effect_json, keywords, faction, quote_text)
SELECT s.id, '烧死女巫', 'Burn the Witch', 'spell', 'rare', 3, NULL, NULL,
 '{"description_cn":"对敌方英雄造成4点伤害。","description_en":"Deal 4 damage to the enemy hero."}',
 '[]', 'neutral', 'Burn them all!'
FROM shows s WHERE s.name LIKE '%S01E01%' AND (s.name LIKE '%Game of Thrones%' OR s.name LIKE '%GOT%')
LIMIT 1;

-- 24. 烈火燎原 (epic, 6费, targaryen)
INSERT IGNORE INTO cards (show_id, name_cn, name_en, card_type, rarity, cost, attack, health, effect_json, keywords, faction, quote_text)
SELECT s.id, '烈火燎原', 'Fire and Blood', 'spell', 'epic', 6, NULL, NULL,
 '{"description_cn":"对所有敌方角色造成5点伤害。","description_en":"Deal 5 damage to all enemy characters."}',
 '[]', 'targaryen', 'Fire and blood.'
FROM shows s WHERE s.name LIKE '%S01E01%' AND (s.name LIKE '%Game of Thrones%' OR s.name LIKE '%GOT%')
LIMIT 1;

-- ========== 新增装备 (Equipment) ==========

-- 25. 瓦雷利亚钢剑 (epic, 3费, neutral)
INSERT IGNORE INTO cards (show_id, name_cn, name_en, card_type, rarity, cost, attack, health, effect_json, keywords, faction, quote_text)
SELECT s.id, '瓦雷利亚钢剑', 'Valyrian Steel Sword', 'equipment', 'epic', 3, NULL, NULL,
 '{"description_cn":"装备：装备随从获得+3攻击力和2耐久。","description_en":"Equip minion gains +3 Attack and 2 Durability."}',
 '[]', 'neutral', 'Valyrian steel.'
FROM shows s WHERE s.name LIKE '%S01E01%' AND (s.name LIKE '%Game of Thrones%' OR s.name LIKE '%GOT%')
LIMIT 1;

-- ========== 新增场地 (Locations) ==========

-- 27. 临冬城 (epic, 4费, stark)
INSERT IGNORE INTO cards (show_id, name_cn, name_en, card_type, rarity, cost, attack, health, effect_json, keywords, faction, quote_text)
SELECT s.id, '临冬城', 'Winterfell', 'location', 'epic', 4, NULL, NULL,
 '{"description_cn":"每回合你的史塔克随从获得+1/+1。","description_en":"Your Stark minions gain +1/+1 each turn."}',
 '[]', 'stark', 'There must always be a Stark in Winterfell.'
FROM shows s WHERE s.name LIKE '%S01E01%' AND (s.name LIKE '%Game of Thrones%' OR s.name LIKE '%GOT%')
LIMIT 1;

-- 28. 绝境长城 (legendary, 6费, nightwatch)
INSERT IGNORE INTO cards (show_id, name_cn, name_en, card_type, rarity, cost, attack, health, effect_json, keywords, faction, quote_text)
SELECT s.id, '绝境长城', 'The Wall', 'location', 'legendary', 6, NULL, NULL,
 '{"description_cn":"每回合召唤一个2/2的\"守夜人\"。","description_en":"Summon a 2/2 Night Watchman each turn."}',
 '[]', 'nightwatch', 'The Wall is more than just ice and stone.'
FROM shows s WHERE s.name LIKE '%S01E01%' AND (s.name LIKE '%Game of Thrones%' OR s.name LIKE '%GOT%')
LIMIT 1;
