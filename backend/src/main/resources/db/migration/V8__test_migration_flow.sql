-- ======================================================
-- 测试：数据库迁移流程
-- ======================================================
ALTER TABLE email_codes
    ADD COLUMN test_expires_at DATETIME DEFAULT NULL COMMENT '测试用：验证码过期时间';

UPDATE flyway_schema_history SET success = 1 WHERE version = 8;
