-- ======================================================
-- 系统设置表
-- 存储全局配置（注册开关、公告等）
-- ======================================================
CREATE TABLE IF NOT EXISTS system_settings (
    setting_key VARCHAR(100) PRIMARY KEY COMMENT '设置键',
    setting_value TEXT COMMENT '设置值',
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统设置表';

-- 默认值
INSERT INTO system_settings (setting_key, setting_value) VALUES ('registrationEnabled', 'true');
INSERT INTO system_settings (setting_key, setting_value) VALUES ('announcement', '');
