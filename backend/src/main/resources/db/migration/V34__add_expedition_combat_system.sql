-- 杀戮尖塔式战斗系统扩展
-- 在现有远征系统上增加全新卡牌战斗子系统

-- ==================== 远征专用卡牌（Slay the Spire风格） ====================
CREATE TABLE IF NOT EXISTS expedition_cards (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  card_name VARCHAR(100) NOT NULL COMMENT '卡牌中文名',
  card_name_en VARCHAR(200) NOT NULL COMMENT '卡牌英文名',
  card_type VARCHAR(20) NOT NULL COMMENT 'attack/skill/power/curse/status',
  rarity VARCHAR(20) NOT NULL DEFAULT 'common' COMMENT 'basic/common/uncommon/rare',
  cost INT NOT NULL DEFAULT 1 COMMENT '费用(0-3, 99=X费)',
  description VARCHAR(500) DEFAULT '' COMMENT '效果描述',
  base_damage INT NOT NULL DEFAULT 0 COMMENT '基础伤害',
  base_block INT NOT NULL DEFAULT 0 COMMENT '基础格挡',
  keywords JSON COMMENT '额外效果关键词',
  upgrade_description VARCHAR(500) DEFAULT '' COMMENT '升级后描述',
  upgrade_damage INT NOT NULL DEFAULT 0 COMMENT '升级后额外伤害',
  upgrade_block INT NOT NULL DEFAULT 0 COMMENT '升级后额外格挡',
  is_x_cost BOOLEAN NOT NULL DEFAULT FALSE COMMENT '是否X费牌',
  show_id BIGINT COMMENT '所属剧集',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  INDEX idx_rarity (rarity),
  INDEX idx_card_type (card_type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ==================== 用户卡牌收藏（远征版） ====================
CREATE TABLE IF NOT EXISTS user_collections (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  user_id BIGINT NOT NULL,
  card_id BIGINT NOT NULL COMMENT '关联expedition_cards.id',
  quantity INT NOT NULL DEFAULT 1,
  is_upgraded BOOLEAN NOT NULL DEFAULT FALSE COMMENT '是否已升级',
  acquired_from VARCHAR(20) NOT NULL DEFAULT 'basic' COMMENT 'basic/pack/shop/reward',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  UNIQUE KEY uk_user_card (user_id, card_id),
  INDEX idx_user (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ==================== 节点内容配置 ====================
CREATE TABLE IF NOT EXISTS node_content (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  act INT NOT NULL COMMENT '第几幕',
  floor INT NOT NULL COMMENT '层数(1-15+)',
  node_type VARCHAR(20) NOT NULL COMMENT 'combat/elite/event/rest/shop/treasure/boss',
  position INT NOT NULL DEFAULT 0 COMMENT '同行中第几个节点',
  title VARCHAR(200) DEFAULT '' COMMENT '节点标题',
  content TEXT COMMENT '场景描述(英文约500字)',
  connected_to JSON COMMENT '后续节点位置数组',
  is_boss_node BOOLEAN NOT NULL DEFAULT FALSE,
  event_choices JSON COMMENT '事件分支选项',
  enemy_id BIGINT COMMENT '关联敌人ID(普通战斗)',
  boss_id BIGINT COMMENT '关联Boss ID',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  INDEX idx_act_floor (act, floor),
  INDEX idx_node_type (node_type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ==================== 战斗临时状态 ====================
CREATE TABLE IF NOT EXISTS expedition_battle_state (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  expedition_id BIGINT NOT NULL COMMENT '关联远征实例',
  enemy_id BIGINT NOT NULL,
  enemy_hp INT NOT NULL,
  enemy_max_hp INT NOT NULL,
  enemy_block INT NOT NULL DEFAULT 0 COMMENT '敌人当前格挡',
  enemy_buffs JSON COMMENT '敌人状态效果',
  player_buffs JSON COMMENT '玩家状态效果',
  turn_number INT NOT NULL DEFAULT 1,
  hand_cards JSON NOT NULL COMMENT '当前手牌(完整牌对象)',
  draw_pile JSON NOT NULL COMMENT '抽牌堆',
  discard_pile JSON NOT NULL COMMENT '弃牌堆',
  energy INT NOT NULL DEFAULT 3 COMMENT '当前能量',
  max_energy INT NOT NULL DEFAULT 3,
  player_block INT NOT NULL DEFAULT 0 COMMENT '玩家当前格挡',
  status VARCHAR(20) NOT NULL DEFAULT 'fighting' COMMENT 'fighting/won/lost',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE KEY uk_expedition (expedition_id),
  FOREIGN KEY (expedition_id) REFERENCES expeditions(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ==================== 扩展远征表 ====================
ALTER TABLE expeditions
  ADD COLUMN IF NOT EXISTS floor INT NOT NULL DEFAULT 1 COMMENT '当前层行数(1-15)',
  ADD COLUMN IF NOT EXISTS visited_nodes JSON COMMENT '已访问节点ID列表',
  ADD COLUMN IF NOT EXISTS max_floor INT NOT NULL DEFAULT 1 COMMENT '当前幕总层数';
