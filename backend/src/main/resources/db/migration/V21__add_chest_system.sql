-- V21: 宝箱系统
CREATE TABLE IF NOT EXISTS user_chests (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  user_id BIGINT NOT NULL,
  chest_type VARCHAR(20) NOT NULL COMMENT 'bronze/silver/gold',
  status VARCHAR(20) NOT NULL DEFAULT 'locked' COMMENT 'locked/unlocking/ready/claimed',
  unlock_progress INT NOT NULL DEFAULT 0 COMMENT '当前已练句数',
  unlock_required INT NOT NULL COMMENT '需要练的句数: bronze=10, silver=25, gold=50',
  source VARCHAR(50) NOT NULL COMMENT '来源: pvp_battle/arena_win/season_reward',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  INDEX idx_user_status (user_id, status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
