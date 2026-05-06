-- ======================================================
-- 演示：重新导入句子数据（清空 + 插入）
-- 模拟真实场景：删掉旧数据，重新导入
-- ======================================================

-- 1. 清空所有数据（先删外键关联的表）
DELETE FROM wrong_sentences;
DELETE FROM practice_logs;
DELETE FROM sentence_flags;
DELETE FROM sentences;
DELETE FROM shows;

-- 2. 重新插入剧集
INSERT INTO shows (id, name, source_file) VALUES
(1, 'Demo Show - 01', 'demo.srt');
INSERT INTO shows (id, name, source_file) VALUES
(2, 'Demo Show - 02', 'demo2.srt');

-- 3. 重新插入句子
INSERT INTO sentences (id, show_id, text, start_time, end_time, audio_file) VALUES
(1, 1, '#1 Hello World / 你好世界', 0.0, 2.0, 'demo001.mp3');
INSERT INTO sentences (id, show_id, text, start_time, end_time, audio_file) VALUES
(2, 1, '#2 This is Flyway / 这是Flyway迁移', 2.5, 4.5, 'demo002.mp3');
INSERT INTO sentences (id, show_id, text, start_time, end_time, audio_file) VALUES
(3, 2, '#3 Learning by Doing / 在实践中学习', 0.0, 3.0, 'demo003.mp3');

-- 4. 重置自增ID
ALTER TABLE sentences AUTO_INCREMENT = 100;
ALTER TABLE shows AUTO_INCREMENT = 100;
