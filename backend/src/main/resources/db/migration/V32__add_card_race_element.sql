-- V32: 卡牌特征字段 race/element
-- race: dragon/undead/human/beast/ice/holy
-- element: fire/ice/shadow/light/nature/metal

-- 确保通用剧集存在（cards 已有 show_id=1 的卡牌）
INSERT IGNORE INTO shows (id, name) VALUES (1, '通用');

-- 添加 race 和 element 列
ALTER TABLE cards
  ADD COLUMN race VARCHAR(20) DEFAULT NULL COMMENT 'dragon/undead/human/beast/ice/holy',
  ADD COLUMN element VARCHAR(20) DEFAULT NULL COMMENT 'fire/ice/shadow/light/nature/metal';

-- ===== show_id=1 (通用) =====
UPDATE cards SET race='holy',   element='light'  WHERE id=1;   -- 守夜人誓言
UPDATE cards SET race='beast',  element='nature' WHERE id=2;   -- 冰原狼
UPDATE cards SET race='ice',    element='ice'    WHERE id=3;   -- 凛冬将至
UPDATE cards SET race='dragon', element='fire'   WHERE id=4;   -- 龙之吐息
UPDATE cards SET race='human',  element='metal'  WHERE id=5;   -- 铁盾兵
UPDATE cards SET race='human',  element='metal'  WHERE id=6;   -- 君临城
UPDATE cards SET race='human',  element='ice'    WHERE id=21;  -- 艾德·史塔克
UPDATE cards SET race='human',  element='ice'    WHERE id=22;  -- 琼恩·雪诺
UPDATE cards SET race='human',  element='fire'   WHERE id=23;  -- 丹妮莉丝·坦格利安
UPDATE cards SET race='human',  element='nature' WHERE id=24;  -- 提利昂·兰尼斯特
UPDATE cards SET race='human',  element='shadow' WHERE id=25;  -- 小指头
UPDATE cards SET race='dragon', element='fire'   WHERE id=26;  -- 烈火燎原
UPDATE cards SET race='undead', element='shadow' WHERE id=27;  -- 暗影匕首
UPDATE cards SET race='dragon', element='fire'   WHERE id=28;  -- 龙晶匕首
UPDATE cards SET race='undead', element='shadow' WHERE id=29;  -- 血色婚礼
UPDATE cards SET race='human',  element='ice'    WHERE id=30;  -- 史塔克家族

-- ===== show_id=2 (权游 GOT) =====
UPDATE cards SET race='human',  element='nature' WHERE id=7;   -- 卡劳利公馆
UPDATE cards SET race='human',  element='light'  WHERE id=8;   -- 大庄园
UPDATE cards SET race='human',  element='metal'  WHERE id=9;   -- 忠诚管家
UPDATE cards SET race='human',  element='light'  WHERE id=10;  -- 庄园舞会
UPDATE cards SET race='human',  element='metal'  WHERE id=31;  -- 罗伯特·卡劳利
UPDATE cards SET race='human',  element='nature' WHERE id=32;  -- 珂拉·卡劳利
UPDATE cards SET race='human',  element='light'  WHERE id=33;  -- 玛丽·卡劳利
UPDATE cards SET race='human',  element='metal'  WHERE id=34;  -- 管家卡森
UPDATE cards SET race='human',  element='nature' WHERE id=35;  -- 精致茶具
UPDATE cards SET race='human',  element='light'  WHERE id=36;  -- 唐顿的舞会
UPDATE cards SET race='human',  element='nature' WHERE id=37;  -- 女仆安娜
UPDATE cards SET race='human',  element='nature' WHERE id=38;  -- 庄园晚宴
UPDATE cards SET race='human',  element='ice'    WHERE id=39;  -- 奈德·史塔克
UPDATE cards SET race='human',  element='ice'    WHERE id=40;  -- 琼恩·雪诺
UPDATE cards SET race='human',  element='fire'   WHERE id=41;  -- 丹妮莉丝·坦格利安
UPDATE cards SET race='human',  element='nature' WHERE id=42;  -- 提利昂·兰尼斯特
UPDATE cards SET race='human',  element='shadow' WHERE id=43;  -- 艾莉亚·史塔克
UPDATE cards SET race='human',  element='shadow' WHERE id=44;  -- 瑟曦·兰尼斯特
UPDATE cards SET race='human',  element='metal'  WHERE id=45;  -- 詹姆·兰尼斯特
UPDATE cards SET race='human',  element='fire'   WHERE id=46;  -- 卓戈卡奥
UPDATE cards SET race='human',  element='shadow' WHERE id=47;  -- 培提尔·贝里席
UPDATE cards SET race='human',  element='shadow' WHERE id=48;  -- 瓦里斯
UPDATE cards SET race='human',  element='fire'   WHERE id=49;  -- 桑铎·克里冈
UPDATE cards SET race='human',  element='nature' WHERE id=50;  -- 山姆威尔·塔利
UPDATE cards SET race='human',  element='metal'  WHERE id=51;  -- 塔斯的布蕾妮
UPDATE cards SET race='human',  element='nature' WHERE id=52;  -- 奥莲娜·雷德温
UPDATE cards SET race='human',  element='fire'   WHERE id=53;  -- 梅丽珊卓
UPDATE cards SET race='dragon', element='fire'   WHERE id=54;  -- 龙焰
UPDATE cards SET race='undead', element='shadow' WHERE id=55;  -- 凡人皆有一死
UPDATE cards SET race='beast',  element='nature' WHERE id=56;  -- 狼家血脉
UPDATE cards SET race='human',  element='metal'  WHERE id=57;  -- 铁王座
UPDATE cards SET race='human',  element='fire'   WHERE id=58;  -- 黑水河之战
UPDATE cards SET race='undead', element='shadow' WHERE id=59;  -- 卡斯特梅的雨季

-- ===== show_id=12 (唐顿 Downton Abbey) =====
UPDATE cards SET race='human', element='light'  WHERE id=60;  -- 维奥莱特伯爵夫人
UPDATE cards SET race='human', element='metal'  WHERE id=61;  -- 罗伯特伯爵
UPDATE cards SET race='human', element='light'  WHERE id=62;  -- 玛丽小姐
UPDATE cards SET race='human', element='light'  WHERE id=63;  -- 马修·克劳利
UPDATE cards SET race='human', element='fire'   WHERE id=64;  -- 汤姆·布兰森
UPDATE cards SET race='human', element='metal'  WHERE id=65;  -- 查尔斯·卡森
UPDATE cards SET race='human', element='nature' WHERE id=66;  -- 休斯太太
UPDATE cards SET race='human', element='shadow' WHERE id=67;  -- 托马斯·巴罗
UPDATE cards SET race='human', element='metal'  WHERE id=68;  -- 贝茨先生
UPDATE cards SET race='human', element='nature' WHERE id=69;  -- 安娜·贝茨
UPDATE cards SET race='human', element='nature' WHERE id=70;  -- 黛西
UPDATE cards SET race='human', element='nature' WHERE id=71;  -- 帕特莫尔太太
UPDATE cards SET race='human', element='nature' WHERE id=72;  -- 伊迪丝小姐
UPDATE cards SET race='human', element='light'  WHERE id=73;  -- 西比尔小姐
UPDATE cards SET race='human', element='shadow' WHERE id=74;  -- 奥布莱恩
UPDATE cards SET race='human', element='light'  WHERE id=75;  -- 唐顿大宅
UPDATE cards SET race='human', element='nature' WHERE id=76;  -- 楼下的世界
UPDATE cards SET race='human', element='light'  WHERE id=77;  -- 楼上与楼下
UPDATE cards SET race='human', element='nature' WHERE id=78;  -- 庄园晚餐
UPDATE cards SET race='human', element='light'  WHERE id=79;  -- 盛大舞会
UPDATE cards SET race='human', element='light'  WHERE id=80;  -- 新时代
