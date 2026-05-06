-- ======================================================
-- 演示：Flyway 全流程测试
-- 清空并重新导入 GOT 数据
-- ======================================================

-- 1. 清空数据
DELETE FROM wrong_sentences;
DELETE FROM practice_logs;
DELETE FROM sentence_flags;
DELETE FROM sentences;
DELETE FROM shows;

-- 2. 插入剧集
INSERT INTO shows (id, name) VALUES
(1, 'Game of Thrones S01E01'),
(2, 'Game of Thrones S01E02');

-- 3. 插入句子
INSERT INTO sentences (id, show_id, text, start_time, end_time, audio_file) VALUES
(1, 1, '#1 Winter is coming / 凛冬将至', 0.0, 2.0, 'got_e0001.mp3'),
(2, 1, '#2 The lone wolf dies / 独狼死', 2.5, 4.5, 'got_e0002.mp3'),
(3, 1, '#3 Valar morghulis / 凡人皆有一死', 5.0, 7.0, 'got_e0003.mp3'),
(4, 2, '#1 You know nothing / 你什么都不懂', 0.0, 2.0, 'got_e0004.mp3'),
(5, 2, '#2 Hold the door / 守住门', 2.5, 4.5, 'got_e0005.mp3');

ALTER TABLE sentences AUTO_INCREMENT = 100;
ALTER TABLE shows AUTO_INCREMENT = 100;
