-- V20: 扩充卡牌内容（GOT + DA）
-- 使用 show_id=2 (GOT S01E01) 和 show_id=12 (DA S01E01)

INSERT IGNORE INTO cards (id, show_id, name_cn, name_en, card_type, rarity, cost, attack, health, effect_json, faction, quote_text) VALUES

-- ===== GOT 新卡 =====
(39, 2, '奈德·史塔克', 'Ned Stark', 'minion', 'legendary', 7, 7, 7,
 '{"keywords":["battlecry"],"description_cn":"本局所有友方随从+1/+1","description_en":"Give all friendly minions +1/+1 this game"}',
 'stark', 'The man who passes the sentence should swing the sword.'),

(40, 2, '琼恩·雪诺', 'Jon Snow', 'minion', 'epic', 5, 5, 5,
 '{"keywords":[],"description_cn":"你的守夜人随从+1/+1","description_en":"Your Night Watch minions gain +1/+1"}',
 'nightwatch', 'I am the shield that guards the realms of men.'),

(41, 2, '丹妮莉丝·坦格利安', 'Daenerys Targaryen', 'minion', 'legendary', 8, 6, 8,
 '{"keywords":["battlecry"],"description_cn":"对全体敌方随从造成3点伤害","description_en":"Deal 3 damage to all enemy minions"}',
 'targaryen', 'I am not a politician. I am a queen.'),

(42, 2, '提利昂·兰尼斯特', 'Tyrion Lannister', 'minion', 'epic', 4, 2, 5,
 '{"keywords":[],"description_cn":"每回合多抽1张牌","description_en":"Draw 1 extra card each turn"}',
 'lannister', 'I drink and I know things.'),

(43, 2, '艾莉亚·史塔克', 'Arya Stark', 'minion', 'epic', 3, 3, 2,
 '{"keywords":["stealth"],"description_cn":"潜行。攻击时不可被防御。","description_en":"Stealth. Cannot be defended when attacking."}',
 'stark', 'Not today.'),

(44, 2, '瑟曦·兰尼斯特', 'Cersei Lannister', 'minion', 'legendary', 7, 5, 8,
 '{"keywords":[],"description_cn":"每当你答对一题，获得+1攻击力","description_en":"Whenever you answer correctly, gain +1 Attack"}',
 'lannister', 'When you play the game of thrones, you win or you die.'),

(45, 2, '詹姆·兰尼斯特', 'Jaime Lannister', 'minion', 'epic', 5, 5, 4,
 '{"keywords":[],"description_cn":"攻击时抽1张牌","description_en":"When attacking, draw 1 card"}',
 'lannister', 'The things I do for love.'),

(46, 2, '卓戈卡奥', 'Khal Drogo', 'minion', 'epic', 6, 7, 4,
 '{"keywords":["charge"],"description_cn":"冲锋","description_en":"Charge"}',
 'targaryen', 'A khal who cannot ride is no khal.'),

(47, 2, '培提尔·贝里席', 'Petyr Baelish', 'minion', 'rare', 3, 2, 3,
 '{"keywords":["battlecry"],"description_cn":"战吼：抽1张牌","description_en":"Battlecry: Draw 1 card"}',
 'neutral', 'Chaos is a ladder.'),

(48, 2, '瓦里斯', 'Varys', 'minion', 'rare', 3, 1, 4,
 '{"keywords":[],"description_cn":"你的抽牌效果翻倍","description_en":"Your draw effects are doubled"}',
 'neutral', 'Power resides where men believe it resides.'),

(49, 2, '桑铎·克里冈', 'Sandor Clegane', 'minion', 'rare', 4, 4, 3,
 '{"keywords":["taunt"],"description_cn":"嘲讽","description_en":"Taunt"}',
 'neutral', 'Look at me. I am the monster they made me.'),

(50, 2, '山姆威尔·塔利', 'Samwell Tarly', 'minion', 'rare', 2, 1, 3,
 '{"keywords":["battlecry"],"description_cn":"战吼：恢复3点生命","description_en":"Battlecry: Restore 3 Health"}',
 'nightwatch', 'I am not a fighter. I am a maester.'),

(51, 2, '塔斯的布蕾妮', 'Brienne of Tarth', 'minion', 'rare', 5, 5, 5,
 '{"keywords":["taunt"],"description_cn":"嘲讽。对战传说随从+2/+2","description_en":"Taunt. +2/+2 vs Legendary minions"}',
 'neutral', 'I protect the ones who cannot protect themselves.'),

(52, 2, '奥莲娜·雷德温', 'Olenna Tyrell', 'minion', 'epic', 4, 3, 4,
 '{"keywords":["battlecry"],"description_cn":"战吼：消灭一个攻击力≤2的敌方随从","description_en":"Battlecry: Destroy an enemy minion with 2 or less Attack"}',
 'neutral', 'Tell Cersei. I want her to know it was me.'),

(53, 2, '梅丽珊卓', 'Melisandre', 'minion', 'rare', 4, 3, 3,
 '{"keywords":["battlecry"],"description_cn":"战吼：复活一个随机友方随从","description_en":"Battlecry: Resurrect a random friendly minion"}',
 'neutral', 'The night is dark and full of terrors.'),

(54, 2, '龙焰', 'Dracarys', 'spell', 'epic', 5, NULL, NULL,
 '{"keywords":[],"description_cn":"对全部敌方随从造成4点伤害","description_en":"Deal 4 damage to all enemy minions"}',
 'targaryen', 'Dracarys!'),

(55, 2, '凡人皆有一死', 'Valar Morghulis', 'spell', 'rare', 2, NULL, NULL,
 '{"keywords":[],"description_cn":"抽2张牌","description_en":"Draw 2 cards"}',
 'neutral', 'Valar morghulis.'),

(56, 2, '狼家血脉', 'The Pack Survives', 'spell', 'epic', 3, NULL, NULL,
 '{"keywords":[],"description_cn":"所有友方随从+1/+1","description_en":"Give all friendly minions +1/+1"}',
 'stark', 'The pack survives.'),

(57, 2, '铁王座', 'The Iron Throne', 'location', 'legendary', 6, NULL, NULL,
 '{"keywords":[],"description_cn":"每回合获得1点额外费用","description_en":"Gain 1 extra mana each turn"}',
 'neutral', ''),

(58, 2, '黑水河之战', 'Battle of Blackwater', 'spell', 'epic', 6, NULL, NULL,
 '{"keywords":[],"description_cn":"对所有敌人造成2点伤害，恢复2点生命","description_en":"Deal 2 damage to all enemies and restore 2 Health"}',
 'lannister', 'Let them see the flames.'),

(59, 2, '卡斯特梅的雨季', 'The Rains of Castamere', 'spell', 'epic', 4, NULL, NULL,
 '{"keywords":[],"description_cn":"消灭一个敌方随从","description_en":"Destroy an enemy minion"}',
 'lannister', 'The Rains of Castamere.'),

-- ===== DA 新卡 =====
(60, 12, '维奥莱特伯爵夫人', 'Violet Crawley', 'minion', 'legendary', 7, 5, 8,
 '{"keywords":[],"description_cn":"每回合恢复2点生命","description_en":"Restore 2 Health each turn"}',
 'crawley', 'What is a weekend?'),

(61, 12, '罗伯特伯爵', 'Robert Crawley', 'minion', 'epic', 5, 4, 6,
 '{"keywords":[],"description_cn":"你的贵族随从+1/+1","description_en":"Your noble minions gain +1/+1"}',
 'crawley', 'I am a gentleman. I do not have to know how to do anything.'),

(62, 12, '玛丽小姐', 'Lady Mary', 'minion', 'epic', 4, 4, 4,
 '{"keywords":["battlecry"],"description_cn":"战吼：消灭一个攻击力≤3的敌方随从","description_en":"Battlecry: Destroy an enemy minion with 3 or less Attack"}',
 'crawley', 'I am a modern woman.'),

(63, 12, '马修·克劳利', 'Matthew Crawley', 'minion', 'epic', 5, 5, 4,
 '{"keywords":[],"description_cn":"每回合多抽1张牌","description_en":"Draw 1 extra card each turn"}',
 'crawley', 'I do not want to be a middle-class hero.'),

(64, 12, '汤姆·布兰森', 'Tom Branson', 'minion', 'rare', 3, 3, 3,
 '{"keywords":["battlecry"],"description_cn":"战吼：使一个友方随从+2攻击","description_en":"Battlecry: Give a friendly minion +2 Attack"}',
 'servant', 'I do not belong here.'),

(65, 12, '查尔斯·卡森', 'Charles Carson', 'minion', 'rare', 4, 2, 6,
 '{"keywords":[],"description_cn":"你的仆从随从+1/+1","description_en":"Your servant minions gain +1/+1"}',
 'servant', 'A house is not a home.'),

(66, 12, '休斯太太', 'Mrs. Hughes', 'minion', 'rare', 3, 3, 3,
 '{"keywords":["battlecry"],"description_cn":"战吼：恢复4点生命","description_en":"Battlecry: Restore 4 Health"}',
 'servant', 'We all have our dreams.'),

(67, 12, '托马斯·巴罗', 'Thomas Barrow', 'minion', 'rare', 4, 4, 2,
 '{"keywords":["stealth"],"description_cn":"潜行","description_en":"Stealth"}',
 'servant', 'I will find a way.'),

(68, 12, '贝茨先生', 'Mr. Bates', 'minion', 'epic', 4, 3, 5,
 '{"keywords":[],"description_cn":"你的领主盟友获得+1攻击力","description_en":"Your lord allies gain +1 Attack"}',
 'servant', 'I am a loyal man.'),

(69, 12, '安娜·贝茨', 'Anna Bates', 'minion', 'rare', 2, 2, 2,
 '{"keywords":["battlecry"],"description_cn":"战吼：恢复2点生命","description_en":"Battlecry: Restore 2 Health"}',
 'servant', 'I love my work.'),

(70, 12, '黛西', 'Daisy', 'minion', 'common', 1, 1, 2,
 '{"keywords":[],"description_cn":"","description_en":""}',
 'servant', 'I am just a kitchen maid.'),

(71, 12, '帕特莫尔太太', 'Mrs. Patmore', 'minion', 'common', 3, 2, 4,
 '{"keywords":[],"description_cn":"每回合恢复1点生命","description_en":"Restore 1 Health each turn"}',
 'servant', 'A good cook is worth her weight in gold.'),

(72, 12, '伊迪丝小姐', 'Lady Edith', 'minion', 'rare', 3, 2, 3,
 '{"keywords":["battlecry"],"description_cn":"战吼：抽1张牌","description_en":"Battlecry: Draw 1 card"}',
 'crawley', 'I have a voice.'),

(73, 12, '西比尔小姐', 'Lady Sybil', 'minion', 'rare', 3, 3, 3,
 '{"keywords":["battlecry"],"description_cn":"战吼：所有友方随从+1生命","description_en":"Battlecry: Give all friendly minions +1 Health"}',
 'crawley', 'We must change with the times.'),

(74, 12, '奥布莱恩', 'O''Brien', 'minion', 'rare', 3, 3, 2,
 '{"keywords":["battlecry"],"description_cn":"战吼：对一个敌方随从造成2点伤害","description_en":"Battlecry: Deal 2 damage to an enemy minion"}',
 'servant', 'I always win.'),

(75, 12, '唐顿大宅', 'Downton Abbey', 'location', 'legendary', 7, NULL, NULL,
 '{"keywords":[],"description_cn":"每回合你的仆从随从+1/+1","description_en":"Give your servant minions +1/+1 each turn"}',
 'crawley', ''),

(76, 12, '楼下的世界', 'Below Stairs', 'location', 'epic', 5, NULL, NULL,
 '{"keywords":[],"description_cn":"每回合召唤一个1/1仆从","description_en":"Summon a 1/1 servant each turn"}',
 'servant', ''),

(77, 12, '楼上与楼下', 'Upstairs Downstairs', 'spell', 'rare', 3, NULL, NULL,
 '{"keywords":[],"description_cn":"抽2张牌","description_en":"Draw 2 cards"}',
 'crawley', 'The servants and the family. Two worlds.'),

(78, 12, '庄园晚餐', 'The Dinner', 'spell', 'rare', 4, NULL, NULL,
 '{"keywords":[],"description_cn":"恢复6点生命","description_en":"Restore 6 Health"}',
 'crawley', 'Dinner is served.'),

(79, 12, '盛大舞会', 'The Grand Ball', 'spell', 'epic', 6, NULL, NULL,
 '{"keywords":[],"description_cn":"所有友方随从+2/+2","description_en":"Give all friendly minions +2/+2"}',
 'crawley', 'The annual Crawley ball.'),

(80, 12, '新时代', 'A New Era', 'spell', 'epic', 5, NULL, NULL,
 '{"keywords":[],"description_cn":"抽3张牌并获得3点额外法力","description_en":"Draw 3 cards and gain 3 extra mana"}',
 'crawley', 'A new era is coming.');
