-- ======================================================
-- V13: 清空所有句子和剧集数据（用于重新测试）
-- ======================================================

-- 1. 清空所有数据（按外键顺序）
DELETE FROM wrong_sentences;
DELETE FROM practice_logs;
DELETE FROM sentence_flags;
DELETE FROM subtitle_import_logs;
DELETE FROM sentences;
DELETE FROM shows;

-- 2. 重置自增ID
ALTER TABLE sentences AUTO_INCREMENT = 1;
ALTER TABLE shows AUTO_INCREMENT = 1;
