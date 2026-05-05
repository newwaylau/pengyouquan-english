-- E2E 测试种子数据
-- 注意 H2 用 MODE=MySQL，但 IDENTITY 语法有差异
INSERT INTO shows (id, name, source_file) VALUES (1, '测试剧集', 'test.srt');
INSERT INTO shows (id, name, source_file) VALUES (2, '测试剧集2', 'test2.srt');

INSERT INTO sentences (id, show_id, text, start_time, end_time) VALUES (1, 1, '#1 Hello world / 你好世界', 0.0, 2.0);
INSERT INTO sentences (id, show_id, text, start_time, end_time) VALUES (2, 1, '#2 This is a test / 这是一个测试', 2.5, 4.5);
INSERT INTO sentences (id, show_id, text, start_time, end_time) VALUES (3, 2, '#1 Welcome to English / 欢迎来学英语', 0.0, 3.0);
