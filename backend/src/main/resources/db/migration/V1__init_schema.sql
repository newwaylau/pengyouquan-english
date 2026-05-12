-- ======================================================
-- 英语剧场 - 初始数据库结构
-- 对应老版 dictation-app 的所有功能 + 新增用户系统
-- ======================================================

-- 用户表
CREATE TABLE IF NOT EXISTS users (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '用户ID',
    email VARCHAR(255) NOT NULL UNIQUE COMMENT '邮箱（登录凭证）',
    password VARCHAR(255) NOT NULL COMMENT '密码（BCrypt加密）',
    nickname VARCHAR(100) DEFAULT '' COMMENT '昵称',
    avatar VARCHAR(500) DEFAULT '' COMMENT '头像URL',
    wechat_open_id VARCHAR(100) DEFAULT NULL COMMENT '微信小程序openId',
    enabled TINYINT(1) DEFAULT 1 COMMENT '是否启用（1启用 0禁用）',
    role VARCHAR(20) DEFAULT 'user' COMMENT '角色：user/admin',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户表';

-- 用户设置表（持久化练习偏好）
CREATE TABLE IF NOT EXISTS user_settings (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    setting_key VARCHAR(100) NOT NULL COMMENT '设置键',
    setting_value VARCHAR(500) DEFAULT '' COMMENT '设置值',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_user_setting (user_id, setting_key),
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户设置表';

-- 剧集表（对应老版 shows 表）
CREATE TABLE IF NOT EXISTS shows (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '剧集ID',
    name VARCHAR(255) NOT NULL COMMENT '剧集名称（如 "Game of Thrones S01E01"）',
    source_file VARCHAR(500) DEFAULT '' COMMENT '来源字幕文件',
    imported_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '导入时间'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='剧集表';

-- 句子表（对应老版 sentences 表）
CREATE TABLE IF NOT EXISTS sentences (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '句子ID',
    show_id BIGINT NOT NULL COMMENT '所属剧集ID',
    text TEXT NOT NULL COMMENT '字幕文本（英文 / 中文）',
    start_time DOUBLE DEFAULT NULL COMMENT '字幕开始时间（秒）',
    end_time DOUBLE DEFAULT NULL COMMENT '字幕结束时间（秒）',
    audio_file VARCHAR(500) DEFAULT NULL COMMENT '预生成音频文件名',
    episode_info VARCHAR(100) DEFAULT '' COMMENT '集信息',
    accuracy DOUBLE DEFAULT NULL COMMENT '准确率标记',
    imported_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '导入时间',
    FOREIGN KEY (show_id) REFERENCES shows(id) ON DELETE CASCADE,
    INDEX idx_show_id (show_id),
    INDEX idx_text (text(255))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='字幕句子表';

-- 练习记录表（对应老版 practice_log 表）
CREATE TABLE IF NOT EXISTS practice_logs (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '记录ID',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    sentence_id BIGINT NOT NULL COMMENT '句子ID',
    correct TINYINT(1) DEFAULT 0 COMMENT '是否完全正确（1正确 0错误）',
    correct_count INT DEFAULT 0 COMMENT '正确单词数',
    total_words INT DEFAULT 0 COMMENT '总单词数',
    mode VARCHAR(20) DEFAULT 'sentry' COMMENT '练习模式：dictation/translation',
    practiced_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '练习时间',
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (sentence_id) REFERENCES sentences(id) ON DELETE CASCADE,
    INDEX idx_user_id (user_id),
    INDEX idx_sentence_id (sentence_id),
    INDEX idx_practiced_at (practiced_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='练习记录表';

-- 错题表（自动记录错误次数 > N 的句子）
CREATE TABLE IF NOT EXISTS wrong_sentences (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    sentence_id BIGINT NOT NULL COMMENT '句子ID',
    error_count INT DEFAULT 1 COMMENT '错误次数',
    last_practiced_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '最近练习时间',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_user_sentence (user_id, sentence_id),
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (sentence_id) REFERENCES sentences(id) ON DELETE CASCADE,
    INDEX idx_user_wrong (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='错题表';
