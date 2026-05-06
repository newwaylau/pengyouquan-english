-- ======================================================
-- 错题表扩展：增加"已掌握"标记、冗余存储剧集名
-- ======================================================

ALTER TABLE wrong_sentences
    ADD COLUMN is_mastered TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否已掌握（标记而非删除）',
    ADD COLUMN show_name VARCHAR(255) DEFAULT '' COMMENT '剧集名（冗余存储，方便筛选和排序）',
    ADD INDEX idx_wrong_user_error (user_id, error_count DESC),
    ADD INDEX idx_wrong_user_mastered (user_id, is_mastered);

-- 回填已有数据的剧集名
UPDATE wrong_sentences ws
    LEFT JOIN sentences s ON ws.sentence_id = s.id
    LEFT JOIN shows sh ON s.show_id = sh.id
SET ws.show_name = COALESCE(sh.name, '');
