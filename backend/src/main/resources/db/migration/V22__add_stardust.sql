-- V22: 星尘分解/合成系统
ALTER TABLE users ADD COLUMN stardust INT NOT NULL DEFAULT 0 COMMENT '星尘数量';

CREATE TABLE IF NOT EXISTS stardust_logs (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  user_id BIGINT NOT NULL,
  card_id BIGINT NOT NULL,
  card_name VARCHAR(100),
  action VARCHAR(20) NOT NULL COMMENT 'disenchant/craft',
  stardust_amount INT NOT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  INDEX idx_user (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
