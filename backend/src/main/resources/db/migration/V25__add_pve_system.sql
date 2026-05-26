-- PVE远征系统 — 杀戮尖塔式Roguelike

-- 远征实例（每局）
CREATE TABLE IF NOT EXISTS expeditions (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  user_id BIGINT NOT NULL,
  show_id BIGINT NOT NULL COMMENT '所属剧集，决定敌人和Boss',
  act INT NOT NULL DEFAULT 1 COMMENT '当前层(1-3)',
  node INT NOT NULL DEFAULT 1 COMMENT '当前节点(1-7)',
  max_act INT NOT NULL DEFAULT 1 COMMENT '已到达的最高层',
  player_hp INT NOT NULL DEFAULT 30 COMMENT '当前血量',
  max_hp INT NOT NULL DEFAULT 30 COMMENT '最大血量',
  starting_deck JSON NOT NULL COMMENT '起始10张牌(存card_id数组)',
  current_deck JSON NOT NULL COMMENT '当前牌组(当前局内全部牌)',
  relics JSON COMMENT '已获得的遗物(存relic_id数组)',
  gold INT NOT NULL DEFAULT 0 COMMENT '远征内金币',
  status VARCHAR(20) NOT NULL DEFAULT 'in_progress' COMMENT 'in_progress/cleared/dead',
  questions_answered INT NOT NULL DEFAULT 0 COMMENT '答对题数',
  questions_total INT NOT NULL DEFAULT 0 COMMENT '总答题数',
  enemies_killed INT NOT NULL DEFAULT 0 COMMENT '击杀数',
  current_enemy_id BIGINT COMMENT '当前敌人ID',
  current_enemy_hp INT COMMENT '当前敌人HP',
  map_nodes JSON COMMENT '当前层节点序列(act/node_type数组)',
  battle_state JSON COMMENT '战斗状态(drawn_cards等)',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  INDEX idx_user_status (user_id, status)
);

-- 遗物配置
CREATE TABLE IF NOT EXISTS relics (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  name_cn VARCHAR(50) NOT NULL,
  name_en VARCHAR(100) NOT NULL,
  rarity VARCHAR(20) NOT NULL COMMENT 'common/rare/epic/legendary',
  effect_cn VARCHAR(200) NOT NULL,
  effect_json JSON NOT NULL COMMENT '{"type":"extra_damage","value":2} etc',
  source VARCHAR(50) NOT NULL COMMENT 'boss/event/shop',
  show_id BIGINT COMMENT '所属剧集(null=通用)'
);

-- 远征敌人配置
CREATE TABLE IF NOT EXISTS expedition_enemies (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  show_id BIGINT NOT NULL,
  act INT NOT NULL COMMENT '1-3',
  name_cn VARCHAR(50) NOT NULL,
  name_en VARCHAR(100) NOT NULL,
  hp INT NOT NULL,
  is_boss BOOLEAN NOT NULL DEFAULT FALSE,
  special_rules JSON COMMENT '特殊规则',
  reward_pool JSON COMMENT '击败后可获得的奖励类型'
);

-- 远征事件配置
CREATE TABLE IF NOT EXISTS expedition_events (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  show_id BIGINT NOT NULL,
  act INT NOT NULL,
  title_cn VARCHAR(100) NOT NULL,
  description_cn TEXT NOT NULL,
  choices JSON NOT NULL COMMENT '[{"text":"...","effect":{"type":"...","value":...},"result_text":"..."}]'
);

-- 遗物初始数据
INSERT IGNORE INTO relics (name_cn, name_en, rarity, effect_cn, effect_json, source, show_id) VALUES
('龙鳞护符', 'Dragon Scale Talisman', 'rare', '本局所有选择题变为听力题（难度↑但奖励×1.5）', '{"type":"harder_questions","reward_multiplier":1.5}', 'boss', 1),
('渡鸦之眼', 'Raven Eye', 'epic', '每场战斗第一次答错不扣血', '{"type":"first_mistake_no_damage"}', 'event', 1),
('龙焰宝珠', 'Dragonflame Orb', 'rare', '答对时对敌方额外造成2点伤害', '{"type":"extra_damage_on_correct","value":2}', 'combat', 1),
('铁王座碎片', 'Iron Throne Shard', 'legendary', '每通过一个Boss，回满血', '{"type":"full_heal_after_boss"}', 'act3_boss', 1),
('学士典籍', 'Maester Tome', 'epic', '每场战斗首题自动答对（免费出牌）', '{"type":"first_answer_auto_correct"}', 'shop', 1),
('无面者面具', 'Faceless Mask', 'rare', '每答错一次，下题答对时伤害×2', '{"type":"consecutive_damage_boost"}', 'event', 1),
('蜂蜜酒', 'Mead', 'common', '休息节点回血量从30%→60%', '{"type":"rest_heal_bonus","multiplier":2}', 'shop', 1);

-- Act 1敌人：绝境长城
INSERT IGNORE INTO expedition_enemies (show_id, act, name_cn, name_en, hp, is_boss, special_rules, reward_pool) VALUES
(1, 1, '野人斥候', 'Wildling Scout', 10, FALSE, '{}', '["card","gold"]'),
(1, 1, '冰原狼', 'Direwolf', 15, FALSE, '{}', '["card","gold"]'),
(1, 1, '野人劫掠者', 'Wildling Raider', 20, FALSE, '{"enrage_at_hp":5,"enrage_damage_bonus":2}', '["card","relic_chance","gold"]'),
(1, 1, '守夜人叛徒', 'Night Watch Traitor', 12, FALSE, '{"first_turn_stealth":true}', '["gold","card"]'),
(1, 1, '巨人Wun Wun', 'Wun Wun the Giant', 40, TRUE, '{"heavy_hit_every_3_turns":5}', '["card","relic","gold"]');

-- Act 2敌人：君临城
INSERT IGNORE INTO expedition_enemies (show_id, act, name_cn, name_en, hp, is_boss, special_rules, reward_pool) VALUES
(1, 2, '金袍子卫兵', 'Gold Cloak Guard', 18, FALSE, '{}', '["card","gold"]'),
(1, 2, '御前侍卫', 'Kingsguard', 25, FALSE, '{"taunt":true}', '["card","gold","relic_chance"]'),
(1, 2, '情报总管', 'Master of Whispers', 20, FALSE, '{"steal_card":true}', '["gold","event"]'),
(1, 2, '兰尼斯特士兵', 'Lannister Soldier', 22, FALSE, '{"armor":3}', '["card","gold"]'),
(1, 2, '瑟曦·兰尼斯特', 'Cersei Lannister', 50, TRUE, '{"summon_minion_every_2_turns":"gold_cloak"}', '["card","relic","gold"]');

-- Act 3敌人：龙石岛
INSERT IGNORE INTO expedition_enemies (show_id, act, name_cn, name_en, hp, is_boss, special_rules, reward_pool) VALUES
(1, 3, '无垢者', 'Unsullied', 25, FALSE, '{"immune_to_spells":true}', '["gold","card"]'),
(1, 3, '多斯拉克骑手', 'Dothraki Rider', 30, FALSE, '{"first_attack_double":true}', '["card","gold"]'),
(1, 3, '龙', 'Dragon', 35, FALSE, '{"aoe_attack":3}', '["card","relic_chance","gold"]'),
(1, 3, '红衣女巫', 'Red Witch', 28, FALSE, '{"heal_self_every_3_turns":8}', '["card","gold"]'),
(1, 3, '夜王', 'Night King', 65, TRUE, '{"resurrect_once":true,"aoe_every_3_turns":4,"raise_dead":true}', '["card","relic","gold","legendary_chance"]');

-- 事件数据
INSERT IGNORE INTO expedition_events (show_id, act, title_cn, description_cn, choices) VALUES
(1, 1, '发现古籍残页', '你在长城废墟中发现一页古籍。', '[{"text":"研读（练5句得稀有卡）","effect":{"type":"study","sentences":5,"reward":"rare_card"}},{"text":"跳过（得50星尘）","effect":{"type":"stardust","value":50}}]'),
(1, 1, '山姆的请求', '山姆威尔需要你帮忙整理藏书。', '[{"text":"帮忙（随机移除一张牌，获得一张稀有牌）","effect":{"type":"swap_card","rarity":"rare"}},{"text":"婉拒（得20金币）","effect":{"type":"gold","value":20}}]'),
(1, 2, '小指头的交易', '培提尔·贝里席想和你做一笔交易。', '[{"text":"接受（接下来2场战斗奖励翻倍，但每次答错扣双倍血）","effect":{"type":"double_reward_2_fights"}},{"text":"拒绝（得30星尘）","effect":{"type":"stardust","value":30}}]'),
(1, 2, '红堡比武', '御前举办比武大会，你可以报名参加。', '[{"text":"参加（答对3题，全对得稀有遗物）","effect":{"type":"challenge_quiz","questions":3,"reward":"rare_relic"}},{"text":"观战（回10血）","effect":{"type":"heal","value":10}}]'),
(1, 3, '龙母的考验', '丹妮莉丝要测试你是否配得上龙的信任。', '[{"text":"接受考验（答段落题，全对得传说级奖励）","effect":{"type":"boss_quiz"}},{"text":"退缩（无事发生）","effect":{"type":"nothing"}}]'),
(1, 3, '瓦雷利亚钢', '你发现一块瓦雷利亚钢碎片。', '[{"text":"锻造武器（攻击型卡牌伤害永久+1）","effect":{"type":"buff_attack","value":1}},{"text":"打造铠甲（最大血量+5）","effect":{"type":"buff_max_hp","value":5}}]');
