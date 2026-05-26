-- P2 Features: 段位保护 + 公会部落战 + 公会换卡

-- Feature 1: 段位保护 - 用户统计表新增保底段位字段
ALTER TABLE user_stats ADD COLUMN tier_floor INT NOT NULL DEFAULT 0 COMMENT '段位保护底线（当前段位最小奖杯数）';

-- Feature 3: 公会部落战
CREATE TABLE IF NOT EXISTS guild_wars (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  guild_id BIGINT NOT NULL COMMENT '本方公会ID',
  opponent_guild_id BIGINT NOT NULL COMMENT '对方公会ID',
  week_number INT NOT NULL COMMENT '周数（ISO周数）',
  phase VARCHAR(20) NOT NULL DEFAULT 'preparation' COMMENT 'preparation/battle/settlement/ended',
  start_date DATE NOT NULL COMMENT '周一开始日期',
  end_date DATE NOT NULL COMMENT '周日结束日期',
  guild_trophies INT NOT NULL DEFAULT 0 COMMENT '本方总奖杯数（匹配时记录）',
  opponent_trophies INT NOT NULL DEFAULT 0 COMMENT '对方总奖杯数',
  guild_wins INT NOT NULL DEFAULT 0 COMMENT '本方胜场数',
  opponent_wins INT NOT NULL DEFAULT 0 COMMENT '对方胜场数',
  winner_id BIGINT DEFAULT NULL COMMENT '获胜公会ID（null=未结算）',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  FOREIGN KEY (guild_id) REFERENCES guilds(id),
  FOREIGN KEY (opponent_guild_id) REFERENCES guilds(id),
  INDEX idx_guild_week (guild_id, week_number),
  INDEX idx_phase (phase)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 公会部落战成员贡献记录
CREATE TABLE IF NOT EXISTS guild_war_contributions (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  war_id BIGINT NOT NULL,
  user_id BIGINT NOT NULL,
  cards_contributed INT NOT NULL DEFAULT 0 COMMENT '备战阶段贡献卡牌数',
  battles_fought INT NOT NULL DEFAULT 0 COMMENT '战斗阶段参战场数',
  battles_won INT NOT NULL DEFAULT 0 COMMENT '战斗阶段胜场数',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  FOREIGN KEY (war_id) REFERENCES guild_wars(id),
  FOREIGN KEY (user_id) REFERENCES users(id),
  UNIQUE KEY uk_war_user (war_id, user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Feature 4: 公会换卡请求
CREATE TABLE IF NOT EXISTS card_trade_requests (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  requester_id BIGINT NOT NULL COMMENT '发起方用户ID',
  receiver_id BIGINT NOT NULL COMMENT '接收方用户ID',
  requested_card_id BIGINT NOT NULL COMMENT '发起方想要的卡牌ID',
  offered_card_id BIGINT DEFAULT NULL COMMENT '发起方提供的卡牌ID（可以是null=无条件请求）',
  status VARCHAR(20) NOT NULL DEFAULT 'pending' COMMENT 'pending/accepted/rejected/cancelled',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  FOREIGN KEY (requester_id) REFERENCES users(id),
  FOREIGN KEY (receiver_id) REFERENCES users(id),
  FOREIGN KEY (requested_card_id) REFERENCES cards(id),
  FOREIGN KEY (offered_card_id) REFERENCES cards(id),
  INDEX idx_receiver_status (receiver_id, status),
  INDEX idx_requester_status (requester_id, status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 每日换卡计数
CREATE TABLE IF NOT EXISTS card_trade_daily_limits (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  user_id BIGINT NOT NULL,
  trade_date DATE NOT NULL,
  trade_count INT NOT NULL DEFAULT 0,
  UNIQUE KEY uk_user_date (user_id, trade_date),
  FOREIGN KEY (user_id) REFERENCES users(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
