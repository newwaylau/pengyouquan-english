-- ======================================================
-- 错题表扩展：增加间隔复习字段
-- review_count: 连续答对次数（用于SM-2间隔算法）
-- next_review_at: 下次复习时间
-- ======================================================

ALTER TABLE wrong_sentences
    ADD COLUMN review_count INT NOT NULL DEFAULT 0 COMMENT '连续答对次数（间隔复习用）',
    ADD COLUMN next_review_at DATETIME DEFAULT NULL COMMENT '下次复习时间',
    ADD INDEX idx_wrong_next_review (user_id, next_review_at);
