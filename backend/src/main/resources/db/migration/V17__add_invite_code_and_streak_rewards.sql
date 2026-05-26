-- 1. 用户邀请码
ALTER TABLE users ADD COLUMN invite_code VARCHAR(20) UNIQUE DEFAULT NULL;

-- 2. 统治奖励配置
CREATE TABLE IF NOT EXISTS streak_rewards (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  days_required INT NOT NULL UNIQUE,
  reward_type VARCHAR(20) NOT NULL COMMENT 'badge/title/theme',
  reward_name VARCHAR(100) NOT NULL,
  reward_icon VARCHAR(10) DEFAULT '🎁',
  description VARCHAR(200) DEFAULT ''
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 3. 用户领取记录
CREATE TABLE IF NOT EXISTS user_streak_rewards (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  user_id BIGINT NOT NULL,
  reward_id BIGINT NOT NULL,
  claimed_at DATETIME DEFAULT CURRENT_TIMESTAMP,
  UNIQUE KEY uk_user_reward (user_id, reward_id),
  FOREIGN KEY (user_id) REFERENCES users(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 4. 默认奖励配置
INSERT INTO streak_rewards (days_required, reward_type, reward_name, reward_icon, description) VALUES
(3, 'badge', '新秀之盾', '🛡️', '连续3天完成御前挑战'),
(7, 'title', '七日学士', '📜', '连续7天完成御前挑战'),
(15, 'badge', '铁骑徽记', '⚔️', '连续15天完成御前挑战'),
(30, 'title', '镇北王', '👑', '连续30天完成御前挑战');
