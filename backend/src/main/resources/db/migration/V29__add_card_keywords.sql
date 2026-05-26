-- V29: 卡牌关键词体系
-- 给 cards 表添加 keywords 字段（存储为 JSON 数组）
ALTER TABLE cards ADD COLUMN keywords JSON DEFAULT NULL COMMENT '关键词数组: ["taunt","divine_shield","deathrattle","battlecry","stealth","rush"]'
    AFTER effect_json;

-- =============================================
-- 为全部现有卡牌分配关键词（70张，按稀有度）
-- 普通(common): 0-1词  稀有(rare): 1词  史诗(epic): 1-2词  传说(legendary): 2词
-- =============================================

-- 传说 (2关键词)
UPDATE cards SET keywords = '["battlecry","taunt"]' WHERE name_en = 'Ned Stark';
UPDATE cards SET keywords = '["battlecry","rush"]' WHERE name_en = 'Daenerys Targaryen';
UPDATE cards SET keywords = '["taunt","deathrattle"]' WHERE name_en = 'Cersei Lannister';
UPDATE cards SET keywords = '["deathrattle","divine_shield"]' WHERE name_en = 'Winter Is Coming';
UPDATE cards SET keywords = '["stealth","deathrattle"]' WHERE name_en = 'Violet Crawley';
UPDATE cards SET keywords = '["stealth","taunt"]' WHERE name_en = 'The Iron Throne';
UPDATE cards SET keywords = '["taunt","divine_shield"]' WHERE name_en = 'Downton Abbey';

-- 史诗 (1-2关键词)
UPDATE cards SET keywords = '["taunt"]' WHERE name_en = 'Dragon Breath';
UPDATE cards SET keywords = '["battlecry","rush"]' WHERE name_en = 'Jon Snow';
UPDATE cards SET keywords = '["stealth"]' WHERE name_en = 'Arya Stark';
UPDATE cards SET keywords = '["rush"]' WHERE name_en = 'Khal Drogo';
UPDATE cards SET keywords = '["taunt"]' WHERE name_en = 'The Great Hall';
UPDATE cards SET keywords = '["battlecry"]' WHERE name_en = 'Olenna Tyrell';
UPDATE cards SET keywords = '["rush","deathrattle"]' WHERE name_en = 'Jaime Lannister';
UPDATE cards SET keywords = '["stealth"]' WHERE name_en = 'Tyrion Lannister';
UPDATE cards SET keywords = '["battlecry"]' WHERE name_en = 'Dracarys';
UPDATE cards SET keywords = '["deathrattle"]' WHERE name_en = 'The Pack Survives';
UPDATE cards SET keywords = '["battlecry"]' WHERE name_en = 'Battle of Blackwater';
UPDATE cards SET keywords = '["taunt"]' WHERE name_en = 'The Rains of Castamere';
UPDATE cards SET keywords = '["battlecry"]' WHERE name_en = 'Lady Mary';
UPDATE cards SET keywords = '["taunt","battlecry"]' WHERE name_en = 'Matthew Crawley';
UPDATE cards SET keywords = '["stealth"]' WHERE name_en = 'Robert Crawley';
UPDATE cards SET keywords = '["divine_shield"]' WHERE name_en = 'Mr. Bates';
UPDATE cards SET keywords = '["battlecry"]' WHERE name_en = 'Below Stairs';
UPDATE cards SET keywords = '["deathrattle"]' WHERE name_en = 'The Grand Ball';
UPDATE cards SET keywords = '["battlecry"]' WHERE name_en = 'A New Era';

-- 稀有 (1关键词)
UPDATE cards SET keywords = '["taunt"]' WHERE name_en = 'Night Watch Oath';
UPDATE cards SET keywords = '["taunt"]' WHERE name_en = 'Kings Landing';
UPDATE cards SET keywords = '["taunt"]' WHERE name_en = 'Crawley House';
UPDATE cards SET keywords = '["taunt"]' WHERE name_en = 'The Ball';
UPDATE cards SET keywords = '["battlecry"]' WHERE name_en = 'Petyr Baelish';
UPDATE cards SET keywords = '["stealth"]' WHERE name_en = 'Varys';
UPDATE cards SET keywords = '["taunt"]' WHERE name_en = 'Sandor Clegane';
UPDATE cards SET keywords = '["battlecry"]' WHERE name_en = 'Samwell Tarly';
UPDATE cards SET keywords = '["taunt"]' WHERE name_en = 'Brienne of Tarth';
UPDATE cards SET keywords = '["battlecry"]' WHERE name_en = 'Melisandre';
UPDATE cards SET keywords = '["stealth"]' WHERE name_en = 'Valar Morghulis';
UPDATE cards SET keywords = '["battlecry"]' WHERE name_en = 'Tom Branson';
UPDATE cards SET keywords = '["taunt"]' WHERE name_en = 'Charles Carson';
UPDATE cards SET keywords = '["battlecry"]' WHERE name_en = 'Mrs. Hughes';
UPDATE cards SET keywords = '["stealth"]' WHERE name_en = 'Thomas Barrow';
UPDATE cards SET keywords = '["battlecry"]' WHERE name_en = 'Anna Bates';
UPDATE cards SET keywords = '["battlecry"]' WHERE name_en = 'Lady Edith';
UPDATE cards SET keywords = '["battlecry"]' WHERE name_en = 'Lady Sybil';
UPDATE cards SET keywords = '["battlecry"]' WHERE name_en = 'O''Brien';
UPDATE cards SET keywords = '["rush"]' WHERE name_en = 'Upstairs Downstairs';
UPDATE cards SET keywords = '["divine_shield"]' WHERE name_en = 'The Dinner';

-- 普通 (0-1关键词)
UPDATE cards SET keywords = '["taunt"]' WHERE name_en = 'Iron Shield';
UPDATE cards SET keywords = '["taunt"]' WHERE name_en = 'Direwolf';
UPDATE cards SET keywords = '["battlecry"]' WHERE name_en = 'Loyal Butler';
UPDATE cards SET keywords = '[]' WHERE name_en = 'Daisy';
UPDATE cards SET keywords = '[]' WHERE name_en = 'Mrs. Patmore';
