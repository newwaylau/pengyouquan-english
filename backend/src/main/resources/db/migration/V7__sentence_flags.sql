-- ======================================================
-- 句子举报/质量报告功能
-- 1. 创建 sentence_flags 表
-- 2. sentences 表增加停用字段
-- ======================================================

CREATE TABLE sentence_flags (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  sentence_id BIGINT NOT NULL COMMENT '句子ID',
  user_id BIGINT NOT NULL COMMENT '举报人',
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  UNIQUE KEY uk_sentence_user (sentence_id, user_id),
  INDEX idx_sentence_flags_sentence (sentence_id),
  INDEX idx_sentence_flags_user (user_id)
) COMMENT='句子举报记录表';

ALTER TABLE sentences
  ADD COLUMN is_disabled TINYINT(1) NOT NULL DEFAULT 0 COMMENT '句子是否已停用',
  ADD COLUMN disabled_reason VARCHAR(255) DEFAULT '' COMMENT '停用原因',
  ADD INDEX idx_sentences_disabled (is_disabled);
