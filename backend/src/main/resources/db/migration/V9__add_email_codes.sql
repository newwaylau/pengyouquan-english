-- ======================================================
-- 新增邮箱验证码表
-- ======================================================

-- 邮箱验证码表
CREATE TABLE IF NOT EXISTS email_codes (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    email VARCHAR(255) NOT NULL COMMENT '邮箱',
    code VARCHAR(10) NOT NULL COMMENT '验证码',
    test_expires_at DATETIME DEFAULT NULL COMMENT '测试用：验证码过期时间',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    INDEX idx_email (email),
    INDEX idx_email_code (email, code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='邮箱验证码表';
