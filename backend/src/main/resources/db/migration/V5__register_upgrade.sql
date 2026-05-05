-- 注册升级 V5：邮箱验证码 + 手机号 + 邀请码

-- 用户表增加 phone 字段（手机号）
ALTER TABLE users ADD COLUMN phone VARCHAR(20) DEFAULT '' COMMENT '手机号' AFTER email;

-- 创建邮件验证码表
CREATE TABLE IF NOT EXISTS email_codes (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    email VARCHAR(255) NOT NULL,
    code VARCHAR(6) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_email (email)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='邮箱验证码表';

-- 用户表增加 invited_by 字段（邀请码）
ALTER TABLE users ADD COLUMN invited_by VARCHAR(100) DEFAULT '' COMMENT '邀请码' AFTER role;
