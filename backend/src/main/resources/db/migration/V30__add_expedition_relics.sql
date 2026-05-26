-- 远征遗物系统 - 新增遗物配置表和玩家遗物表

-- 遗物配置（面向远征的遗物，effect_type/effect_value 替代原来的 effect_json）
CREATE TABLE IF NOT EXISTS expedition_relics (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  name_cn VARCHAR(50) NOT NULL COMMENT '中文名',
  name_en VARCHAR(100) NOT NULL COMMENT '英文名',
  rarity VARCHAR(20) NOT NULL COMMENT 'common/rare/epic/legendary',
  effect_type VARCHAR(50) NOT NULL COMMENT '效果类型',
  effect_value INT NOT NULL DEFAULT 0 COMMENT '效果数值',
  description_cn VARCHAR(300) NOT NULL COMMENT '中文说明',
  description_en VARCHAR(300) NOT NULL COMMENT '英文说明',
  icon VARCHAR(20) NOT NULL DEFAULT '🪙' COMMENT '显示图标',
  show_id BIGINT COMMENT '所属剧集(null=通用)'
);

-- 玩家单局获得的遗物（替换 expeditions.relics JSON字段）
CREATE TABLE IF NOT EXISTS player_relics (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  expedition_id BIGINT NOT NULL,
  relic_id BIGINT NOT NULL,
  INDEX idx_expedition (expedition_id),
  FOREIGN KEY (expedition_id) REFERENCES expeditions(id) ON DELETE CASCADE,
  FOREIGN KEY (relic_id) REFERENCES expedition_relics(id) ON DELETE CASCADE
);

-- ==================== 插入15-20种遗物数据 ====================

-- COMBAT_DAMAGE_BOOST（战斗伤害+）
INSERT IGNORE INTO expedition_relics (name_cn, name_en, rarity, effect_type, effect_value, description_cn, description_en, icon, show_id) VALUES
('磨刀石', 'Whetstone', 'common', 'COMBAT_DAMAGE_BOOST', 2, '所有战斗伤害+2', 'All combat damage +2', '⚔️', NULL),
('龙焰匕首', 'Dragonflame Dagger', 'rare', 'COMBAT_DAMAGE_BOOST', 4, '所有战斗伤害+4', 'All combat damage +4', '🗡️', NULL),
('瓦雷利亚钢剑', 'Valyrian Steel Sword', 'epic', 'COMBAT_DAMAGE_BOOST', 6, '所有战斗伤害+6', 'All combat damage +6', '⚔️', NULL),
('光明使者', 'Lightbringer', 'legendary', 'COMBAT_DAMAGE_BOOST', 10, '所有战斗伤害+10', 'All combat damage +10', '🔥', NULL);

-- ANSWER_TIME_BONUS（答题时间+秒）
INSERT IGNORE INTO expedition_relics (name_cn, name_en, rarity, effect_type, effect_value, description_cn, description_en, icon, show_id) VALUES
('沙漏', 'Hourglass', 'common', 'ANSWER_TIME_BONUS', 5, '答题时间+5秒', 'Answer time +5 seconds', '⏳', NULL),
('时光水晶', 'Time Crystal', 'rare', 'ANSWER_TIME_BONUS', 10, '答题时间+10秒', 'Answer time +10 seconds', '💎', NULL);

-- HEAL_ON_COMBAT_WIN（战斗胜利回血）
INSERT IGNORE INTO expedition_relics (name_cn, name_en, rarity, effect_type, effect_value, description_cn, description_en, icon, show_id) VALUES
('治疗之泉', 'Healing Spring', 'common', 'HEAL_ON_COMBAT_WIN', 3, '战斗胜利后回复3点生命', 'Heal 3 HP after combat victory', '💧', NULL),
('生命之符', 'Rune of Vitality', 'rare', 'HEAL_ON_COMBAT_WIN', 5, '战斗胜利后回复5点生命', 'Heal 5 HP after combat victory', '❤️', NULL);

-- EXTRA_DRAW（每场战斗多抽牌）
INSERT IGNORE INTO expedition_relics (name_cn, name_en, rarity, effect_type, effect_value, description_cn, description_en, icon, show_id) VALUES
('藏书阁钥匙', 'Library Key', 'common', 'EXTRA_DRAW', 1, '每场战斗多抽1张牌', 'Draw 1 extra card per combat', '🔑', NULL),
('智慧之冠', 'Crown of Wisdom', 'rare', 'EXTRA_DRAW', 2, '每场战斗多抽2张牌', 'Draw 2 extra cards per combat', '👑', NULL);

-- DAMAGE_REDUCTION（受伤减免）
INSERT IGNORE INTO expedition_relics (name_cn, name_en, rarity, effect_type, effect_value, description_cn, description_en, icon, show_id) VALUES
('铁甲护符', 'Iron Amulet', 'common', 'DAMAGE_REDUCTION', 1, '受到的伤害-1', 'Reduce damage taken by 1', '🛡️', NULL),
('龙鳞盾', 'Dragon Scale Shield', 'rare', 'DAMAGE_REDUCTION', 2, '受到的伤害-2', 'Reduce damage taken by 2', '🛡️', NULL);

-- GOLD_BONUS（商店金币+）
INSERT IGNORE INTO expedition_relics (name_cn, name_en, rarity, effect_type, effect_value, description_cn, description_en, icon, show_id) VALUES
('钱袋', 'Coin Pouch', 'common', 'GOLD_BONUS', 10, '战斗获得的金币+10', 'Bonus +10 gold from combat', '💰', NULL),
('黄金鹿徽', 'Golden Stag Sigil', 'rare', 'GOLD_BONUS', 20, '战斗获得的金币+20', 'Bonus +20 gold from combat', '🦌', NULL);

-- DOUBLE_EDGED（伤害翻倍但答错自伤翻倍 传说）
INSERT IGNORE INTO expedition_relics (name_cn, name_en, rarity, effect_type, effect_value, description_cn, description_en, icon, show_id) VALUES
('疯王之冠', 'Mad King''s Crown', 'legendary', 'DOUBLE_EDGED', 2, '造成伤害翻倍，但答错受到的伤害也翻倍', 'Deal double damage, but take double damage on mistakes', '👑', NULL);

-- VAMPIRIC（造成伤害的20%回血）
INSERT IGNORE INTO expedition_relics (name_cn, name_en, rarity, effect_type, effect_value, description_cn, description_en, icon, show_id) VALUES
('吸血鬼之吻', 'Vampiric Kiss', 'epic', 'VAMPIRIC', 20, '造成伤害的20%回复为生命', 'Heal 20% of damage dealt', '🧛', NULL);

-- WRONG_PENALTY_REDUCE（答错惩罚减半）
INSERT IGNORE INTO expedition_relics (name_cn, name_en, rarity, effect_type, effect_value, description_cn, description_en, icon, show_id) VALUES
('学士之戒', 'Maester''s Ring', 'rare', 'WRONG_PENALTY_REDUCE', 50, '答错受到的伤害减半', 'Reduce mistake damage by 50%', '📖', NULL);

-- BOSS_DAMAGE_BONUS（打Boss伤害+50%）
INSERT IGNORE INTO expedition_relics (name_cn, name_en, rarity, effect_type, effect_value, description_cn, description_en, icon, show_id) VALUES
('屠龙者徽记', 'Dragon Slayer Sigil', 'epic', 'BOSS_DAMAGE_BONUS', 50, '对Boss造成伤害+50%', 'Deal 50% more damage to Bosses', '🐉', NULL);

-- 额外遗物填充至20个
INSERT IGNORE INTO expedition_relics (name_cn, name_en, rarity, effect_type, effect_value, description_cn, description_en, icon, show_id) VALUES
('幸运四叶草', 'Four-leaf Clover', 'common', 'COMBAT_DAMAGE_BOOST', 1, '所有战斗伤害+1（微效但稳定）', 'All combat damage +1 (modest but reliable)', '🍀', NULL),
('狮鹫之羽', 'Griffin Feather', 'rare', 'ANSWER_TIME_BONUS', 3, '答题时间+3秒', 'Answer time +3 seconds', '🪶', NULL),
('龙晶匕首', 'Dragonglass Dagger', 'epic', 'BOSS_DAMAGE_BONUS', 25, '对Boss造成伤害+25%', 'Deal 25% more damage to Bosses', '🔪', NULL);
