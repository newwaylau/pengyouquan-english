-- 卡牌主表
CREATE TABLE IF NOT EXISTS cards (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  show_id BIGINT NOT NULL,
  name_cn VARCHAR(100) NOT NULL,
  name_en VARCHAR(200) NOT NULL,
  card_type ENUM('minion','spell','equipment','location') NOT NULL,
  rarity ENUM('common','rare','epic','legendary') NOT NULL DEFAULT 'common',
  cost INT NOT NULL DEFAULT 0,
  attack INT DEFAULT NULL,
  health INT DEFAULT NULL,
  effect_json TEXT COMMENT 'JSON: {keywords:[], description_cn, description_en}',
  challenge_type VARCHAR(20) DEFAULT NULL COMMENT 'dictation/fill_blank/null',
  challenge_sentence_id BIGINT DEFAULT NULL,
  faction VARCHAR(50) DEFAULT NULL,
  quote_text VARCHAR(500) DEFAULT NULL,
  image_url VARCHAR(500) DEFAULT NULL,
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
  INDEX idx_show (show_id),
  INDEX idx_rarity (rarity),
  FOREIGN KEY (show_id) REFERENCES shows(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 玩家牌库
CREATE TABLE IF NOT EXISTS user_cards (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  user_id BIGINT NOT NULL,
  card_id BIGINT NOT NULL,
  quantity INT NOT NULL DEFAULT 1,
  obtained_at DATETIME DEFAULT CURRENT_TIMESTAMP,
  UNIQUE KEY uk_user_card (user_id, card_id),
  FOREIGN KEY (user_id) REFERENCES users(id),
  FOREIGN KEY (card_id) REFERENCES cards(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 玩家卡组
CREATE TABLE IF NOT EXISTS decks (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  user_id BIGINT NOT NULL,
  name VARCHAR(50) NOT NULL DEFAULT '未命名卡组',
  card_ids JSON COMMENT '[card_id, card_id, ...]',
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  FOREIGN KEY (user_id) REFERENCES users(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 装备表
CREATE TABLE IF NOT EXISTS equipment (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  name_cn VARCHAR(100) NOT NULL,
  name_en VARCHAR(200) NOT NULL,
  slot ENUM('weapon','armor','artifact') NOT NULL,
  rarity ENUM('common','rare','epic','legendary') NOT NULL DEFAULT 'common',
  stat_bonus JSON COMMENT '{attack: +2, health: +1}',
  effect_json JSON COMMENT '特殊效果描述JSON',
  affinity_show_id BIGINT DEFAULT NULL COMMENT 'null=通用装备',
  unlock_condition VARCHAR(200) DEFAULT NULL,
  FOREIGN KEY (affinity_show_id) REFERENCES shows(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 玩家装备
CREATE TABLE IF NOT EXISTS user_equipment (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  user_id BIGINT NOT NULL,
  equipment_id BIGINT NOT NULL,
  quantity INT NOT NULL DEFAULT 1,
  FOREIGN KEY (user_id) REFERENCES users(id),
  FOREIGN KEY (equipment_id) REFERENCES equipment(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 插入初始卡牌数据（GOT + DA 各 5 张做 P0 演示）
INSERT INTO cards (show_id, name_cn, name_en, card_type, rarity, cost, attack, health, effect_json, faction, quote_text) VALUES
(1, '守夜人誓言', 'Night Watch Oath', 'spell', 'rare', 2, NULL, NULL, '{"keywords":[],"description_cn":"恢复 3 点生命","description_en":"Restore 3 Health"}', 'nightwatch', 'I am the sword in the darkness.'),
(1, '冰原狼', 'Direwolf', 'minion', 'common', 2, 2, 2, '{"keywords":[],"description_cn":"","description_en":""}', 'stark', ''),
(1, '凛冬将至', 'Winter Is Coming', 'spell', 'legendary', 5, NULL, NULL, '{"keywords":["challenge"],"description_cn":"对敌方全体造成 4 点伤害","description_en":"Deal 4 damage to all enemies"}', 'stark', 'Winter is coming.'),
(1, '龙之吐息', 'Dragon Breath', 'spell', 'epic', 3, NULL, NULL, '{"keywords":["challenge"],"description_cn":"造成 5 点伤害","description_en":"Deal 5 damage"}', 'targaryen', 'Dracarys'),
(1, '铁盾兵', 'Iron Shield', 'minion', 'common', 1, 1, 4, '{"keywords":["taunt"],"description_cn":"","description_en":""}', 'neutral', ''),
(1, '君临城', 'Kings Landing', 'location', 'rare', 3, NULL, NULL, '{"keywords":[],"description_cn":"所有随从 +1/+1","description_en":"All minions +1/+1"}', 'neutral', ''),
(2, '卡劳利公馆', 'Crawley House', 'location', 'rare', 3, NULL, NULL, '{"keywords":[],"description_cn":"每回合恢复 2 点生命","description_en":"Restore 2 Health each turn"}', 'crawley', ''),
(2, '大庄园', 'The Great Hall', 'location', 'epic', 4, NULL, NULL, '{"keywords":[],"description_cn":"每回合获得 1 额外法力","description_en":"Gain 1 extra mana each turn"}', 'crawley', ''),
(2, '忠诚管家', 'Loyal Butler', 'minion', 'common', 1, 1, 2, '{"keywords":["battlecry"],"description_cn":"恢复 2 点生命","description_en":"Restore 2 Health"}', 'crawley', ''),
(2, '庄园舞会', 'The Ball', 'spell', 'rare', 3, NULL, NULL, '{"keywords":[],"description_cn":"抽 2 张牌","description_en":"Draw 2 cards"}', 'crawley', '');
