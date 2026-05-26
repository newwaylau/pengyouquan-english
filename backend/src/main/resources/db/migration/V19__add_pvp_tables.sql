-- PvP 异步竞技场系统
-- friends: 好友关系
-- battle_history: 对战记录
-- rank_tiers: 段位配置
-- user_stats: 用户对战统计数据

CREATE TABLE IF NOT EXISTS friends (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  user_id BIGINT NOT NULL,
  friend_id BIGINT NOT NULL,
  status ENUM('pending','accepted','blocked') NOT NULL DEFAULT 'pending',
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE KEY uk_friendship (user_id, friend_id),
  FOREIGN KEY (user_id) REFERENCES users(id),
  FOREIGN KEY (friend_id) REFERENCES users(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS battle_history (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  challenger_id BIGINT NOT NULL,
  defender_id BIGINT NOT NULL,
  winner_id BIGINT DEFAULT NULL,
  challenger_deck_id BIGINT DEFAULT NULL,
  defender_deck_id BIGINT DEFAULT NULL,
  challenger_score INT DEFAULT 0 COMMENT '答对数',
  defender_score INT DEFAULT 0,
  challenger_accuracy DECIMAL(5,2) DEFAULT 0.00,
  defender_accuracy DECIMAL(5,2) DEFAULT 0.00,
  challenger_avg_difficulty DECIMAL(4,2) DEFAULT 0.00,
  defender_avg_difficulty DECIMAL(4,2) DEFAULT 0.00,
  trophy_change INT DEFAULT 0,
  status ENUM('pending','completed','cancelled') NOT NULL DEFAULT 'pending',
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
  completed_at DATETIME DEFAULT NULL,
  INDEX idx_challenger (challenger_id),
  INDEX idx_defender (defender_id),
  FOREIGN KEY (challenger_id) REFERENCES users(id),
  FOREIGN KEY (defender_id) REFERENCES users(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS rank_tiers (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  name_cn VARCHAR(50) NOT NULL,
  name_en VARCHAR(50) NOT NULL,
  min_trophies INT NOT NULL,
  max_trophies INT NOT NULL,
  icon VARCHAR(10) DEFAULT '',
  season_reward_type VARCHAR(20) DEFAULT NULL,
  season_reward_count INT DEFAULT 0
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

INSERT INTO rank_tiers (name_cn, name_en, min_trophies, max_trophies, icon, season_reward_type, season_reward_count) VALUES
('青铜', 'Bronze', 0, 200, '🥉', 'common', 3),
('白银', 'Silver', 201, 500, '🥈', 'rare', 3),
('黄金', 'Gold', 501, 900, '🥇', 'epic', 1),
('白金', 'Platinum', 901, 1500, '💎', 'epic', 3),
('钻石', 'Diamond', 1501, 2500, '🔷', 'legendary', 1),
('传说', 'Legend', 2501, 99999, '🏆', 'legendary', 3);

CREATE TABLE IF NOT EXISTS user_stats (
  user_id BIGINT PRIMARY KEY,
  trophies INT DEFAULT 0,
  wins INT DEFAULT 0,
  losses INT DEFAULT 0,
  win_streak INT DEFAULT 0,
  best_trophies INT DEFAULT 0,
  FOREIGN KEY (user_id) REFERENCES users(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
