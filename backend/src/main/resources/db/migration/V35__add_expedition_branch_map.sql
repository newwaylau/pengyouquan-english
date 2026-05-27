-- V35: 远征分支地图系统 - 杀戮尖塔风格
-- 新增 map_data(全地图结构)、potions(药水)、金币列
-- 新建 expedition_potions 表

-- ========== 1. expeditions 表新增列 ==========

-- map_data: 存储完整的地图结构（每个节点的坐标、类型、连接关系、是否已到达等）
ALTER TABLE expeditions ADD COLUMN IF NOT EXISTS map_data JSON
    COMMENT '完整地图结构：{acts:[{nodes:[{id,type,row,col,connections:[],cleared,branchChoice},...]}]}';

-- potions: 玩家当前拥有的药水（数组）
ALTER TABLE expeditions ADD COLUMN IF NOT EXISTS potions JSON
    COMMENT '拥有的药水：[{potionId,quantity}]';

-- 更新已有数据默认值
UPDATE expeditions SET potions = '[]' WHERE potions IS NULL;
UPDATE expeditions SET map_data = '{"acts":[]}' WHERE map_data IS NULL;

-- ========== 2. 新建药水配置表 ==========

CREATE TABLE IF NOT EXISTS expedition_potions (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  name_cn VARCHAR(50) NOT NULL COMMENT '中文名',
  name_en VARCHAR(100) NOT NULL COMMENT '英文名',
  rarity VARCHAR(20) NOT NULL DEFAULT 'common' COMMENT 'common/rare/epic',
  effect_type VARCHAR(50) NOT NULL COMMENT '效果类型',
  effect_value INT NOT NULL DEFAULT 0 COMMENT '效果数值',
  description_cn VARCHAR(300) NOT NULL COMMENT '中文说明',
  description_en VARCHAR(300) NOT NULL COMMENT '英文说明',
  icon VARCHAR(20) NOT NULL DEFAULT '🧪' COMMENT '显示图标',
  show_id BIGINT COMMENT '所属剧集(null=通用)',
  price INT NOT NULL DEFAULT 40 COMMENT '商店价格',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
) COMMENT '远征药水配置';

-- ========== 3. 插入初始药水数据 ==========

-- 生命药水 (回血)
INSERT IGNORE INTO expedition_potions (name_cn, name_en, rarity, effect_type, effect_value, description_cn, description_en, icon, show_id, price) VALUES
('小型生命药水', 'Small Health Potion', 'common', 'HEAL', 15, '恢复15点生命值', 'Restore 15 HP', '❤️', NULL, 30),
('大型生命药水', 'Large Health Potion', 'rare', 'HEAL', 30, '恢复30点生命值', 'Restore 30 HP', '💖', NULL, 60);

-- 力量药水 (临时加攻)
INSERT IGNORE INTO expedition_potions (name_cn, name_en, rarity, effect_type, effect_value, description_cn, description_en, icon, show_id, price) VALUES
('力量药水', 'Strength Potion', 'common', 'ATTACK_BUFF', 3, '本场战斗中攻击力+3', 'Gain +3 Attack this combat', '💪', NULL, 35),
('巨力药水', 'Giant Strength Potion', 'rare', 'ATTACK_BUFF', 6, '本场战斗中攻击力+6', 'Gain +6 Attack this combat', '💪', NULL, 60);

-- 格挡药水 (获得格挡)
INSERT IGNORE INTO expedition_potions (name_cn, name_en, rarity, effect_type, effect_value, description_cn, description_en, icon, show_id, price) VALUES
('铁皮药水', 'Iron Skin Potion', 'common', 'BLOCK', 10, '获得10点格挡', 'Gain 10 Block', '🛡️', NULL, 35),
('钢甲药水', 'Steel Armor Potion', 'rare', 'BLOCK', 20, '获得20点格挡', 'Gain 20 Block', '🛡️', NULL, 65);

-- 能量药水 (费用恢复)
INSERT IGNORE INTO expedition_potions (name_cn, name_en, rarity, effect_type, effect_value, description_cn, description_en, icon, show_id, price) VALUES
('能量药水', 'Energy Potion', 'common', 'ENERGY', 2, '获得2点额外费用', 'Gain 2 Energy', '⚡', NULL, 40);

-- 敏捷药水 (抽牌)
INSERT IGNORE INTO expedition_potions (name_cn, name_en, rarity, effect_type, effect_value, description_cn, description_en, icon, show_id, price) VALUES
('敏捷药水', 'Swiftness Potion', 'common', 'DRAW', 2, '抽2张牌', 'Draw 2 cards', '🏃', NULL, 35);

-- 精英药水 (对Boss特攻)
INSERT IGNORE INTO expedition_potions (name_cn, name_en, rarity, effect_type, effect_value, description_cn, description_en, icon, show_id, price) VALUES
('屠龙药剂', 'Dragon Slayer Elixir', 'epic', 'BOSS_DAMAGE', 50, '对Boss造成伤害+50%（本场战斗）', 'Deal 50% more damage to Bosses this combat', '🐉', NULL, 100);

-- 回复药水 (战斗后回血)
INSERT IGNORE INTO expedition_potions (name_cn, name_en, rarity, effect_type, effect_value, description_cn, description_en, icon, show_id, price) VALUES
('再生药水', 'Regeneration Potion', 'rare', 'COMBAT_HEAL', 10, '本场战斗胜利后恢复10点生命', 'Heal 10 HP after this combat', '🌿', NULL, 50);
