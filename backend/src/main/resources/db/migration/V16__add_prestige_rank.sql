-- ======================================================
-- 权游主题游戏化系统：封号等级 + 御前挑战
-- ======================================================

-- 用户表新增字段
ALTER TABLE users ADD COLUMN prestige INT NOT NULL DEFAULT 0 COMMENT '威望值';
ALTER TABLE users ADD COLUMN rank_tier INT NOT NULL DEFAULT 1 COMMENT '封号等级 1-10';
ALTER TABLE users ADD COLUMN consecutive_days INT NOT NULL DEFAULT 0 COMMENT '统治天数(连续打卡)';
ALTER TABLE users ADD COLUMN last_daily_date DATE DEFAULT NULL COMMENT '上次完成御前挑战日期';

-- 御前挑战记录表
CREATE TABLE IF NOT EXISTS daily_challenges (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  user_id BIGINT NOT NULL,
  challenge_date DATE NOT NULL,
  total_questions INT NOT NULL DEFAULT 10,
  correct_count INT NOT NULL DEFAULT 0,
  prestige_earned INT NOT NULL DEFAULT 0,
  combo_count INT NOT NULL DEFAULT 0,
  completed TINYINT(1) NOT NULL DEFAULT 0,
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
  INDEX idx_user_date (user_id, challenge_date),
  FOREIGN KEY (user_id) REFERENCES users(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 每日挑战题目表
CREATE TABLE IF NOT EXISTS daily_challenge_questions (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  challenge_id BIGINT NOT NULL,
  sentence_id BIGINT NOT NULL,
  user_answer TEXT,
  is_correct TINYINT(1) DEFAULT NULL,
  answered_at DATETIME DEFAULT NULL,
  INDEX idx_challenge (challenge_id),
  FOREIGN KEY (challenge_id) REFERENCES daily_challenges(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
