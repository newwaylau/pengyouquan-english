-- V23: 装备系统 + 英雄系统

-- ========== 装备表扩展 ==========
-- 扩展slot枚举值以支持五槽位（weapon, armor, trinket, tome, crown）
ALTER TABLE equipment MODIFY COLUMN slot VARCHAR(20) NOT NULL;

-- 插入守夜人套装（GOT, show_id=1）
INSERT IGNORE INTO equipment (name_cn, name_en, slot, rarity, stat_bonus, effect_json, affinity_show_id, unlock_condition) VALUES
('守夜人之剑', 'Night Watch Sword', 'weapon', 'common', '{"attack_bonus":1}', '{}', (SELECT id FROM shows WHERE name LIKE '%S01E01%' AND (name LIKE '%Game of Thrones%' OR name LIKE '%GOT%') LIMIT 1), 'login_3_days'),
('守夜人皮甲', 'Night Watch Leather', 'armor', 'common', '{"health_bonus":3}', '{}', (SELECT id FROM shows WHERE name LIKE '%S01E01%' AND (name LIKE '%Game of Thrones%' OR name LIKE '%GOT%') LIMIT 1), 'reach_rank_1'),
('守夜人徽章', 'Night Watch Badge', 'trinket', 'rare', '{"draw_bonus":1}', '{}', (SELECT id FROM shows WHERE name LIKE '%S01E01%' AND (name LIKE '%Game of Thrones%' OR name LIKE '%GOT%') LIMIT 1), 'win_5_battles'),
('守夜人守夜记录', 'Night Watch Log', 'tome', 'common', '{"time_bonus":5}', '{}', (SELECT id FROM shows WHERE name LIKE '%S01E01%' AND (name LIKE '%Game of Thrones%' OR name LIKE '%GOT%') LIMIT 1), 'complete_50_sentences'),
('守夜人头盔', 'Night Watch Helm', 'crown', 'rare', '{"cooldown_reduction":1}', '{}', (SELECT id FROM shows WHERE name LIKE '%S01E01%' AND (name LIKE '%Game of Thrones%' OR name LIKE '%GOT%') LIMIT 1), 'reach_rank_3');

-- 史塔克套装（GOT, show_id=1）
INSERT IGNORE INTO equipment (name_cn, name_en, slot, rarity, stat_bonus, effect_json, affinity_show_id, unlock_condition) VALUES
('寒冰之剑', 'Ice Sword', 'weapon', 'epic', '{"attack_bonus":2,"ice_damage_bonus":1}', '{"set":"stark"}', (SELECT id FROM shows WHERE name LIKE '%S01E01%' AND (name LIKE '%Game of Thrones%' OR name LIKE '%GOT%') LIMIT 1), 'win_10_battles'),
('史塔克家徽胸甲', 'Stark Cuirass', 'armor', 'rare', '{"health_bonus":5}', '{"set":"stark"}', (SELECT id FROM shows WHERE name LIKE '%S01E01%' AND (name LIKE '%Game of Thrones%' OR name LIKE '%GOT%') LIMIT 1), 'reach_rank_5'),
('北境之戒', 'North Ring', 'trinket', 'epic', '{"draw_bonus":1,"mana_discount":1}', '{"set":"stark"}', (SELECT id FROM shows WHERE name LIKE '%S01E01%' AND (name LIKE '%Game of Thrones%' OR name LIKE '%GOT%') LIMIT 1), 'win_20_battles'),
('史塔克家谱', 'Stark Family Tree', 'tome', 'rare', '{"time_bonus":10}', '{"set":"stark"}', (SELECT id FROM shows WHERE name LIKE '%S01E01%' AND (name LIKE '%Game of Thrones%' OR name LIKE '%GOT%') LIMIT 1), 'complete_100_sentences'),
('北境王冠', 'Crown of Winter', 'crown', 'legendary', '{"cooldown_reduction":2,"first_card_free":true}', '{"set":"stark"}', (SELECT id FROM shows WHERE name LIKE '%S01E01%' AND (name LIKE '%Game of Thrones%' OR name LIKE '%GOT%') LIMIT 1), 'reach_legend_rank');

-- 坦格利安套装（GOT, show_id=1）
INSERT IGNORE INTO equipment (name_cn, name_en, slot, rarity, stat_bonus, effect_json, affinity_show_id, unlock_condition) VALUES
('龙晶匕首', 'Dragonglass Dagger', 'weapon', 'rare', '{"attack_bonus":1,"legendary_damage_bonus":2}', '{"set":"targaryen"}', (SELECT id FROM shows WHERE name LIKE '%S01E01%' AND (name LIKE '%Game of Thrones%' OR name LIKE '%GOT%') LIMIT 1), 'collect_5_legendaries'),
('龙鳞甲', 'Dragonscale Armor', 'armor', 'epic', '{"health_bonus":8}', '{"set":"targaryen"}', (SELECT id FROM shows WHERE name LIKE '%S01E01%' AND (name LIKE '%Game of Thrones%' OR name LIKE '%GOT%') LIMIT 1), 'reach_rank_8'),
('龙之坠', 'Dragon Pendant', 'trinket', 'legendary', '{"draw_bonus":2,"legendary_cost_reduction":1}', '{"set":"targaryen"}', (SELECT id FROM shows WHERE name LIKE '%S01E01%' AND (name LIKE '%Game of Thrones%' OR name LIKE '%GOT%') LIMIT 1), 'collect_10_legendaries'),
('龙族秘典', 'Targaryen Tome', 'tome', 'epic', '{"time_bonus":15}', '{"set":"targaryen"}', (SELECT id FROM shows WHERE name LIKE '%S01E01%' AND (name LIKE '%Game of Thrones%' OR name LIKE '%GOT%') LIMIT 1), 'season_pass_level_10'),
('龙王冠', 'Crown of the Dragon', 'crown', 'legendary', '{"cooldown_reduction":2,"ult_damage_bonus":3}', '{"set":"targaryen"}', (SELECT id FROM shows WHERE name LIKE '%S01E01%' AND (name LIKE '%Game of Thrones%' OR name LIKE '%GOT%') LIMIT 1), 'season_rank_1');

-- 唐顿套装（DA, show_id=2）
INSERT IGNORE INTO equipment (name_cn, name_en, slot, rarity, stat_bonus, effect_json, affinity_show_id, unlock_condition) VALUES
('绅士手杖', 'Gentlemans Cane', 'weapon', 'common', '{"heal_bonus":1}', '{}', 2, 'login_7_days'),
('庄园礼服', 'Estate Gown', 'armor', 'rare', '{"health_bonus":4,"rest_heal_bonus":0.2}', '{"set":"downton"}', 2, 'reach_rank_2'),
('伯爵印章戒', 'Earls Signet Ring', 'trinket', 'rare', '{"event_bonus_option":true}', '{"set":"downton"}', 2, 'complete_200_sentences'),
('下午茶手札', 'Tea Time Notes', 'tome', 'common', '{"time_bonus":5}', '{}', 2, 'login_3_days'),
('家族冕冠', 'Family Coronet', 'crown', 'epic', '{"rest_heal_bonus":0.3,"event_extra_option":true}', '{"set":"downton"}', 2, 'reach_diamond_rank');

-- ========== 英雄装备配置表 ==========
CREATE TABLE IF NOT EXISTS hero_gear (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  user_id BIGINT NOT NULL UNIQUE,
  weapon_id BIGINT,
  armor_id BIGINT,
  trinket_id BIGINT,
  tome_id BIGINT,
  crown_id BIGINT,
  FOREIGN KEY (user_id) REFERENCES users(id),
  INDEX idx_weapon (weapon_id),
  INDEX idx_armor (armor_id),
  INDEX idx_trinket (trinket_id),
  INDEX idx_tome (tome_id),
  INDEX idx_crown (crown_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ========== 英雄配置表 ==========
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
  INDEX idx_show (show_id),
  FOREIGN KEY (show_id) REFERENCES shows(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

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
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ========== 初始英雄数据 ==========
-- GOT: show_id = (select min(id) from shows where name like '%Game of Thrones%')
-- DA:  show_id = (select min(id) from shows where name like '%Downton%')
INSERT IGNORE INTO heroes (show_id, name_cn, name_en, health, skill_name_cn, skill_name_en, skill_description_cn, skill_description_en, skill_cooldown, base_effect_json, unlock_condition)
SELECT (SELECT id FROM shows WHERE name LIKE '%Game of Thrones%' ORDER BY id LIMIT 1), '琼恩·雪诺', 'Jon Snow', 30, '守夜人的意志', 'Watchers Will', '本回合所有随从获得+2生命值', 'All minions gain +2 Health this turn', 3, '{}', 'initial'
WHERE NOT EXISTS (SELECT 1 FROM heroes WHERE name_en = 'Jon Snow');

INSERT IGNORE INTO heroes (show_id, name_cn, name_en, health, skill_name_cn, skill_name_en, skill_description_cn, skill_description_en, skill_cooldown, base_effect_json, unlock_condition)
SELECT (SELECT id FROM shows WHERE name LIKE '%Game of Thrones%' ORDER BY id LIMIT 1), '丹妮莉丝·坦格利安', 'Daenerys Targaryen', 25, '龙之母', 'Mother of Dragons', '召唤一条3/3的龙，然后抽1张牌', 'Summon a 3/3 Dragon, then draw 1 card', 4, '{"max_mana_bonus":1}', 'complete_got_100'
WHERE NOT EXISTS (SELECT 1 FROM heroes WHERE name_en = 'Daenerys Targaryen');

INSERT IGNORE INTO heroes (show_id, name_cn, name_en, health, skill_name_cn, skill_name_en, skill_description_cn, skill_description_en, skill_cooldown, base_effect_json, unlock_condition)
SELECT (SELECT id FROM shows WHERE name LIKE '%Game of Thrones%' ORDER BY id LIMIT 1), '提利昂·兰尼斯特', 'Tyrion Lannister', 28, '智者的计谋', 'Lions Wit', '下一张法术牌不消耗费用', 'Your next spell costs 0', 3, '{"draw_bonus":1}', 'reach_rank_5'
WHERE NOT EXISTS (SELECT 1 FROM heroes WHERE name_en = 'Tyrion Lannister');

INSERT IGNORE INTO heroes (show_id, name_cn, name_en, health, skill_name_cn, skill_name_en, skill_description_cn, skill_description_en, skill_cooldown, base_effect_json, unlock_condition)
SELECT (SELECT id FROM shows WHERE name LIKE '%Downton%' ORDER BY id LIMIT 1), '大小姐玛丽', 'Lady Mary', 30, '贵族风范', 'Noble Grace', '下张卡牌费用-3', 'Next card costs 3 less', 3, '{}', 'initial'
WHERE NOT EXISTS (SELECT 1 FROM heroes WHERE name_en = 'Lady Mary');

INSERT IGNORE INTO heroes (show_id, name_cn, name_en, health, skill_name_cn, skill_name_en, skill_description_cn, skill_description_en, skill_cooldown, base_effect_json, unlock_condition)
SELECT (SELECT id FROM shows WHERE name LIKE '%Downton%' ORDER BY id LIMIT 1), '罗伯特·克劳利', 'Robert Crawley', 35, '庄园之主', 'Lord of the Manor', '恢复6点生命值', 'Restore 6 Health', 4, '{"health_bonus":5}', 'complete_da_100'
WHERE NOT EXISTS (SELECT 1 FROM heroes WHERE name_en = 'Robert Crawley');
