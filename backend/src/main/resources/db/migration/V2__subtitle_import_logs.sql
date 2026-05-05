-- ======================================================
-- 字幕导入记录表
-- 跟踪每次字幕文件导入的元信息
-- ======================================================
CREATE TABLE IF NOT EXISTS subtitle_import_logs (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    show_id BIGINT NOT NULL COMMENT '关联剧集ID',
    file_name VARCHAR(500) NOT NULL COMMENT '原始文件名',
    format VARCHAR(10) NOT NULL COMMENT '字幕格式：srt/ass/vtt',
    sentence_count INT DEFAULT 0 COMMENT '本次导入的句子数',
    duplicate_count INT DEFAULT 0 COMMENT '重复跳过数',
    imported_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '导入时间',
    FOREIGN KEY (show_id) REFERENCES shows(id) ON DELETE CASCADE,
    INDEX idx_show_id (show_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='字幕导入记录表';
