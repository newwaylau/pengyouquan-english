-- V24: 赛季重置系统

CREATE TABLE IF NOT EXISTS season_rewards (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  user_id BIGINT NOT NULL,
  season_number INT NOT NULL COMMENT '第几赛季',
  final_rank VARCHAR(30) COMMENT '最终段位',
  final_trophies INT COMMENT '最终奖杯数',
  reward_claimed BOOLEAN NOT NULL DEFAULT FALSE,
  chest_type VARCHAR(20) COMMENT '赛季宝箱类型',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  claimed_at DATETIME,
  INDEX idx_user_season (user_id, season_number)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 赛季配置表
CREATE TABLE IF NOT EXISTS season_config (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  season_number INT NOT NULL UNIQUE,
  start_date DATETIME NOT NULL,
  end_date DATETIME NOT NULL,
  is_active BOOLEAN NOT NULL DEFAULT FALSE,
  title_cn VARCHAR(100),
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 初始化当前赛季（S1: 2026-05-01 ~ 2026-05-31）
INSERT IGNORE INTO season_config (season_number, start_date, end_date, is_active, title_cn)
VALUES (1, '2026-05-01 00:00:00', '2026-05-31 23:59:59', TRUE, 'S1 赛季 · 凛冬将至');

INSERT IGNORE INTO season_config (season_number, start_date, end_date, is_active, title_cn)
VALUES (2, '2026-06-01 00:00:00', '2026-06-30 23:59:59', FALSE, 'S2 赛季 · 长夏来临');
