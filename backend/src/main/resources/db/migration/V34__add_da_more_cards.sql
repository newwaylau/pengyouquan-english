-- V34: 唐顿卡牌扩充 (21张 → 40张)
-- 新增 10 名角色 + 9 张法术/兵种/装备/场地
-- 使用 INSERT IGNORE ... SELECT 模式关联 show_id

-- ========== 新增角色 (Characters) ==========

-- 1. 西比尔·克劳利 (rare, 3费3/3, crawley)
INSERT IGNORE INTO cards (show_id, name_cn, name_en, card_type, rarity, cost, attack, health, effect_json, keywords, faction, quote_text)
SELECT s.id, '西比尔·克劳利', 'Sybil Crawley', 'minion', 'rare', 3, 3, 3,
 '{"description_cn":"每当你使用一张法术牌，所有友方随从获得+1生命值。","description_en":"Whenever you cast a spell, give all friendly minions +1 Health."}',
 '[]', 'crawley', 'We must change with the times.'
FROM shows s WHERE s.name LIKE '%Downton Abbey%' OR s.name LIKE '%唐顿%' OR s.name LIKE '%DA%'
LIMIT 1;

-- 2. 伊迪丝·克劳利 (rare, 3费2/3, crawley)
INSERT IGNORE INTO cards (show_id, name_cn, name_en, card_type, rarity, cost, attack, health, effect_json, keywords, faction, quote_text)
SELECT s.id, '伊迪丝·克劳利', 'Edith Crawley', 'minion', 'rare', 3, 2, 3,
 '{"description_cn":"战吼：抽1张牌。如果是贵族随从，再抽1张。","description_en":"Battlecry: Draw 1 card. If it''s a noble minion, draw another."}',
 '["battlecry"]', 'crawley', 'I have a voice.'
FROM shows s WHERE s.name LIKE '%Downton Abbey%' OR s.name LIKE '%唐顿%' OR s.name LIKE '%DA%'
LIMIT 1;

-- 3. 汤姆·布兰森 (rare, 3费3/2, servant)
INSERT IGNORE INTO cards (show_id, name_cn, name_en, card_type, rarity, cost, attack, health, effect_json, keywords, faction, quote_text)
SELECT s.id, '汤姆·布兰森', 'Tom Branson', 'minion', 'rare', 3, 3, 2,
 '{"description_cn":"战吼：使一个友方仆从随从获得+2/+1。","description_en":"Battlecry: Give a friendly servant minion +2/+1."}',
 '["battlecry"]', 'servant', 'I do not belong here.'
FROM shows s WHERE s.name LIKE '%Downton Abbey%' OR s.name LIKE '%唐顿%' OR s.name LIKE '%DA%'
LIMIT 1;

-- 4. 黛西·梅森 (common, 2费1/2, servant)
INSERT IGNORE INTO cards (show_id, name_cn, name_en, card_type, rarity, cost, attack, health, effect_json, keywords, faction, quote_text)
SELECT s.id, '黛西·梅森', 'Daisy Mason', 'minion', 'common', 2, 1, 2,
 '{"description_cn":"亡语：获得1点法力水晶。","description_en":"Deathrattle: Gain 1 Mana Crystal."}',
 '["deathrattle"]', 'servant', 'I am just a kitchen maid.'
FROM shows s WHERE s.name LIKE '%Downton Abbey%' OR s.name LIKE '%唐顿%' OR s.name LIKE '%DA%'
LIMIT 1;

-- 5. 奥布莱恩 (epic, 4费4/3, servant)
INSERT IGNORE INTO cards (show_id, name_cn, name_en, card_type, rarity, cost, attack, health, effect_json, keywords, faction, quote_text)
SELECT s.id, '奥布莱恩', 'Sarah O''Brien', 'minion', 'epic', 4, 4, 3,
 '{"description_cn":"战吼：选择一个敌方随从。下一个回合开始时消灭它。","description_en":"Battlecry: Choose an enemy minion. Destroy it at the start of next turn."}',
 '["battlecry"]', 'servant', 'I always win.'
FROM shows s WHERE s.name LIKE '%Downton Abbey%' OR s.name LIKE '%唐顿%' OR s.name LIKE '%DA%'
LIMIT 1;

-- 6. 托马斯·巴罗 (rare, 4费3/2, servant)
INSERT IGNORE INTO cards (show_id, name_cn, name_en, card_type, rarity, cost, attack, health, effect_json, keywords, faction, quote_text)
SELECT s.id, '托马斯·巴罗', 'Thomas Barrow', 'minion', 'rare', 4, 3, 2,
 '{"description_cn":"潜行。攻击后抽1张牌。","description_en":"Stealth. Draw 1 card after attacking."}',
 '["stealth"]', 'servant', 'I will find a way.'
FROM shows s WHERE s.name LIKE '%Downton Abbey%' OR s.name LIKE '%唐顿%' OR s.name LIKE '%DA%'
LIMIT 1;

-- 7. 贝茨先生 (rare, 3费2/4, servant)
INSERT IGNORE INTO cards (show_id, name_cn, name_en, card_type, rarity, cost, attack, health, effect_json, keywords, faction, quote_text)
SELECT s.id, '贝茨先生', 'John Bates', 'minion', 'rare', 3, 2, 4,
 '{"description_cn":"嘲讽。在你的回合结束时，恢复2点生命值。","description_en":"Taunt. At the end of your turn, restore 2 Health."}',
 '["taunt"]', 'servant', 'I am a loyal man.'
FROM shows s WHERE s.name LIKE '%Downton Abbey%' OR s.name LIKE '%唐顿%' OR s.name LIKE '%DA%'
LIMIT 1;

-- 8. 安娜·贝茨 (common, 2费2/2, servant)
INSERT IGNORE INTO cards (show_id, name_cn, name_en, card_type, rarity, cost, attack, health, effect_json, keywords, faction, quote_text)
SELECT s.id, '安娜·贝茨', 'Anna Bates', 'minion', 'common', 2, 2, 2,
 '{"description_cn":"战吼：恢复2点生命值。","description_en":"Battlecry: Restore 2 Health."}',
 '["battlecry"]', 'servant', 'I love my work.'
FROM shows s WHERE s.name LIKE '%Downton Abbey%' OR s.name LIKE '%唐顿%' OR s.name LIKE '%DA%'
LIMIT 1;

-- 9. 罗斯·麦克莱尔 (common, 3费2/3, crawley)
INSERT IGNORE INTO cards (show_id, name_cn, name_en, card_type, rarity, cost, attack, health, effect_json, keywords, faction, quote_text)
SELECT s.id, '罗斯·麦克莱尔', 'Rose MacClare', 'minion', 'common', 3, 2, 3,
 '{"description_cn":"你的贵族随从获得+1攻击力。","description_en":"Your noble minions gain +1 Attack."}',
 '[]', 'crawley', 'I want to have fun!'
FROM shows s WHERE s.name LIKE '%Downton Abbey%' OR s.name LIKE '%唐顿%' OR s.name LIKE '%DA%'
LIMIT 1;

-- 10. 莫斯利 (common, 2费1/2, servant)
INSERT IGNORE INTO cards (show_id, name_cn, name_en, card_type, rarity, cost, attack, health, effect_json, keywords, faction, quote_text)
SELECT s.id, '莫斯利', 'Joseph Molesley', 'minion', 'common', 2, 1, 2,
 '{"description_cn":"战吼：抽1张牌。","description_en":"Battlecry: Draw 1 card."}',
 '["battlecry"]', 'servant', 'I am a man of learning.'
FROM shows s WHERE s.name LIKE '%Downton Abbey%' OR s.name LIKE '%唐顿%' OR s.name LIKE '%DA%'
LIMIT 1;

-- ========== 新增法术 (Spells) ==========

-- 11. 楼上秘闻 (rare, 2费, crawley)
INSERT IGNORE INTO cards (show_id, name_cn, name_en, card_type, rarity, cost, attack, health, effect_json, keywords, faction, quote_text)
SELECT s.id, '楼上秘闻', 'Upstairs Secrets', 'spell', 'rare', 2, NULL, NULL,
 '{"description_cn":"抽2张牌。","description_en":"Draw 2 cards."}',
 '[]', 'crawley', 'The Crawleys are a great family.'
FROM shows s WHERE s.name LIKE '%Downton Abbey%' OR s.name LIKE '%唐顿%' OR s.name LIKE '%DA%'
LIMIT 1;

-- 12. 楼下流言 (rare, 3费, servant)
INSERT IGNORE INTO cards (show_id, name_cn, name_en, card_type, rarity, cost, attack, health, effect_json, keywords, faction, quote_text)
SELECT s.id, '楼下流言', 'Below Stairs Gossip', 'spell', 'rare', 3, NULL, NULL,
 '{"description_cn":"对敌方英雄造成3点伤害，恢复你3点生命值。","description_en":"Deal 3 damage to enemy hero and restore 3 Health."}',
 '[]', 'servant', 'The servants know everything.'
FROM shows s WHERE s.name LIKE '%Downton Abbey%' OR s.name LIKE '%唐顿%' OR s.name LIKE '%DA%'
LIMIT 1;

-- 13. 午茶时分 (common, 1费, crawley)
INSERT IGNORE INTO cards (show_id, name_cn, name_en, card_type, rarity, cost, attack, health, effect_json, keywords, faction, quote_text)
SELECT s.id, '午茶时分', 'Tea Time', 'spell', 'common', 1, NULL, NULL,
 '{"description_cn":"恢复4点生命值。","description_en":"Restore 4 Health."}',
 '[]', 'crawley', 'Shall we have some tea?'
FROM shows s WHERE s.name LIKE '%Downton Abbey%' OR s.name LIKE '%唐顿%' OR s.name LIKE '%DA%'
LIMIT 1;

-- 14. 狩猎会 (epic, 6费, crawley)
INSERT IGNORE INTO cards (show_id, name_cn, name_en, card_type, rarity, cost, attack, health, effect_json, keywords, faction, quote_text)
SELECT s.id, '狩猎会', 'The Hunt', 'spell', 'epic', 6, NULL, NULL,
 '{"description_cn":"消灭所有攻击力≤3的敌方随从。","description_en":"Destroy all enemy minions with 3 or less Attack."}',
 '[]', 'crawley', 'The hunt is on!'
FROM shows s WHERE s.name LIKE '%Downton Abbey%' OR s.name LIKE '%唐顿%' OR s.name LIKE '%DA%'
LIMIT 1;

-- 15. 书信往来 (common, 1费, crawley)
INSERT IGNORE INTO cards (show_id, name_cn, name_en, card_type, rarity, cost, attack, health, effect_json, keywords, faction, quote_text)
SELECT s.id, '书信往来', 'Correspondence', 'spell', 'common', 1, NULL, NULL,
 '{"description_cn":"抽1张牌。","description_en":"Draw 1 card."}',
 '[]', 'crawley', 'A letter has arrived.'
FROM shows s WHERE s.name LIKE '%Downton Abbey%' OR s.name LIKE '%唐顿%' OR s.name LIKE '%DA%'
LIMIT 1;

-- ========== 新增兵种 (Units) ==========

-- 16. 园丁 (common, 1费1/2, servant)
INSERT IGNORE INTO cards (show_id, name_cn, name_en, card_type, rarity, cost, attack, health, effect_json, keywords, faction, quote_text)
SELECT s.id, '园丁', 'The Gardener', 'minion', 'common', 1, 1, 2,
 '{"description_cn":"","description_en":""}',
 '[]', 'servant', 'The gardens must be perfect.'
FROM shows s WHERE s.name LIKE '%Downton Abbey%' OR s.name LIKE '%唐顿%' OR s.name LIKE '%DA%'
LIMIT 1;

-- 17. 厨娘帮工 (common, 1费1/1, servant)
INSERT IGNORE INTO cards (show_id, name_cn, name_en, card_type, rarity, cost, attack, health, effect_json, keywords, faction, quote_text)
SELECT s.id, '厨娘帮工', 'Kitchen Maid', 'minion', 'common', 1, 1, 1,
 '{"description_cn":"亡语：召唤一个1/1的\"洗碗工\"。","description_en":"Deathrattle: Summon a 1/1 Scullery Maid."}',
 '["deathrattle"]', 'servant', 'Yes, Mrs Patmore.'
FROM shows s WHERE s.name LIKE '%Downton Abbey%' OR s.name LIKE '%唐顿%' OR s.name LIKE '%DA%'
LIMIT 1;

-- 18. 管家铃 (equipment, common, 1费, servant)
INSERT IGNORE INTO cards (show_id, name_cn, name_en, card_type, rarity, cost, attack, health, effect_json, keywords, faction, quote_text)
SELECT s.id, '管家铃', 'Butler''s Bell', 'equipment', 'common', 1, NULL, NULL,
 '{"description_cn":"战吼：召唤一个1/1的\"男仆\"。","description_en":"Battlecry: Summon a 1/1 Footman."}',
 '[]', 'servant', 'You rang?'
FROM shows s WHERE s.name LIKE '%Downton Abbey%' OR s.name LIKE '%唐顿%' OR s.name LIKE '%DA%'
LIMIT 1;

-- ========== 新增场地 (Locations) ==========

-- 19. 马车房 (rare, 3费, servant)
INSERT IGNORE INTO cards (show_id, name_cn, name_en, card_type, rarity, cost, attack, health, effect_json, keywords, faction, quote_text)
SELECT s.id, '马车房', 'The Stables', 'location', 'rare', 3, NULL, NULL,
 '{"description_cn":"每当你使用一张仆从牌，获得1点临时法力。","description_en":"Whenever you play a servant card, gain 1 temporary Mana."}',
 '[]', 'servant', 'Prepare the motor.'
FROM shows s WHERE s.name LIKE '%Downton Abbey%' OR s.name LIKE '%唐顿%' OR s.name LIKE '%DA%'
LIMIT 1;
