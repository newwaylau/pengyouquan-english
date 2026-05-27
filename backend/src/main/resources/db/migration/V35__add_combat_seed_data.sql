-- 战斗系统种子数据：50张卡牌 + 第一幕敌人 + 节点内容

-- ==================== 基础卡（4张） ====================
INSERT IGNORE INTO expedition_cards (card_name, card_name_en, card_type, rarity, cost, description, base_damage, base_block, keywords, upgrade_damage, upgrade_block, upgrade_description, show_id) VALUES
('长剑劈砍', 'Strike', 'attack', 'basic', 1, 'Deal 6 damage.', 6, 0, '[]', 3, 0, 'Deal 9 damage.', 1),
('皮盾防御', 'Defend', 'skill', 'basic', 1, 'Gain 5 Block.', 0, 5, '[]', 0, 3, 'Gain 8 Block.', 1),
('铁剑斩', 'Iron Strike', 'attack', 'basic', 1, 'Deal 9 damage.', 9, 0, '[]', 0, 0, '', 1),
('铁盾防御', 'Iron Defend', 'skill', 'basic', 1, 'Gain 8 Block.', 0, 8, '[]', 0, 0, '', 1);

-- ==================== 攻击牌（20张） ====================
INSERT IGNORE INTO expedition_cards (card_name, card_name_en, card_type, rarity, cost, description, base_damage, base_block, keywords, upgrade_damage, upgrade_block, upgrade_description, show_id) VALUES
-- 1. 长剑劈砍 (done above)
-- 2. 铁剑斩 (done above)
-- 3. 北境劈斩 - 若敌人易伤+4
('北境劈斩', 'Northern Slash', 'attack', 'common', 1, 'Deal 8 damage. If enemy is Vulnerable, deal 4 more.', 8, 0, '[{"type":"vulnerable","value":1}]', 3, 0, 'Deal 11 damage. If enemy is Vulnerable, deal 4 more.', 1),
-- 4. 野人冲撞 - 2费 14伤
('野人冲撞', 'Wildling Charge', 'attack', 'common', 2, 'Deal 14 damage.', 14, 0, '[]', 4, 0, 'Deal 18 damage.', 1),
-- 5. 双刃斩 - 1费 5伤×2
('双刃斩', 'Double Strike', 'attack', 'common', 1, 'Deal 5 damage twice.', 5, 0, '[{"type":"multihit","value":2}]', 2, 0, 'Deal 7 damage twice.', 1),
-- 6. 重劈 - 2费 12伤 消耗
('重劈', 'Heavy Strike', 'attack', 'common', 2, 'Deal 12 damage. Exhaust.', 12, 0, '[{"type":"exhaust"}]', 4, 0, 'Deal 16 damage. Exhaust.', 1),
-- 7. 投掷长矛 - 1费 7伤 抽1
('投掷长矛', 'Spear Throw', 'attack', 'common', 1, 'Deal 7 damage. Draw 1 card.', 7, 0, '[{"type":"draw","value":1}]', 2, 0, 'Deal 9 damage. Draw 1 card.', 1),
-- 8. 怒吼 - 0费 4伤 消耗
('怒吼', 'War Cry', 'attack', 'common', 0, 'Deal 4 damage. Exhaust.', 4, 0, '[{"type":"exhaust"}]', 2, 0, 'Deal 6 damage. Exhaust.', 1),
-- 9. 旋风斩 - X费 X×4伤
('旋风斩', 'Whirlwind', 'attack', 'uncommon', 99, 'Deal 4 damage X times.', 4, 0, '[{"type":"aoe"}]', 1, 0, 'Deal 5 damage X times. X cost.', 1),
-- 10. 突刺 - 1费 6伤 抽1
('突刺', 'Lunge', 'attack', 'common', 1, 'Deal 6 damage. Draw 1 card.', 6, 0, '[{"type":"draw","value":1}]', 2, 0, 'Deal 8 damage. Draw 1 card.', 1),
-- 11. 烈火剑 - 1费 5伤 2烧伤
('烈火剑', 'Fire Sword', 'attack', 'uncommon', 1, 'Deal 5 damage. Apply 2 Poison.', 5, 0, '[{"type":"poison","value":2}]', 2, 0, 'Deal 7 damage. Apply 3 Poison.', 1),
-- 12. 碎甲锤 - 2费 8伤 移除所有格挡
('碎甲锤', 'Shield Breaker', 'attack', 'uncommon', 2, 'Deal 8 damage. Remove all enemy Block.', 8, 0, '[]', 3, 0, 'Deal 11 damage. Remove all enemy Block.', 1),
-- 13. 连击 - 1费 3伤×3
('连击', 'Combo Strike', 'attack', 'common', 1, 'Deal 3 damage 3 times.', 3, 0, '[{"type":"multihit","value":3}]', 1, 0, 'Deal 4 damage 3 times.', 1),
-- 14. 致命一击 - 2费 6伤 半血翻倍
('致命一击', 'Finishing Blow', 'attack', 'uncommon', 2, 'Deal 6 damage. Double if enemy below 50% HP.', 6, 0, '[]', 3, 0, 'Deal 9 damage. Double if enemy below 50% HP.', 1),
-- 15. 战吼 - 2费 10伤 1易伤
('战吼', 'Battle Cry', 'attack', 'common', 2, 'Deal 10 damage. Apply 1 Vulnerable.', 10, 0, '[{"type":"vulnerable","value":1}]', 3, 0, 'Deal 13 damage. Apply 2 Vulnerable.', 1),
-- 16. 回旋踢 - 1费 5伤 1虚弱
('回旋踢', 'Roundhouse Kick', 'attack', 'common', 1, 'Deal 5 damage. Apply 1 Weak.', 5, 0, '[{"type":"weak","value":1}]', 2, 0, 'Deal 7 damage. Apply 1 Weak.', 1),
-- 17. 穿刺 - 2费 7伤 无视格挡
('穿刺', 'Piercing Strike', 'attack', 'uncommon', 2, 'Deal 7 damage. Ignores Block.', 7, 0, '[]', 3, 0, 'Deal 10 damage. Ignores Block.', 1),
-- 18. 猛击 - 3费 20伤
('猛击', 'Slam', 'attack', 'uncommon', 3, 'Deal 20 damage.', 20, 0, '[]', 5, 0, 'Deal 25 damage.', 1),
-- 19. 血色打击 - 1费 4伤 回2血
('血色打击', 'Blood Strike', 'attack', 'common', 1, 'Deal 4 damage. Heal 2 HP.', 4, 0, '[{"type":"heal","value":2}]', 1, 0, 'Deal 5 damage. Heal 3 HP.', 1),
-- 20. 终结技 - 2费 8伤 中毒翻倍
('终结技', 'Execute', 'attack', 'uncommon', 2, 'Deal 8 damage. Triple damage if enemy is Poisoned.', 8, 0, '[{"type":"poison","value":2}]', 3, 0, 'Deal 11 damage. Triple damage if enemy is Poisoned.', 1);

-- ==================== 格挡/技能牌（15张） ====================
INSERT IGNORE INTO expedition_cards (card_name, card_name_en, card_type, rarity, cost, description, base_damage, base_block, keywords, upgrade_damage, upgrade_block, upgrade_description, show_id) VALUES
-- 21. 皮盾防御 (done above)
-- 22. 铁盾防御 (done above)
-- 23. 举盾
('举盾', 'Shield Up', 'skill', 'common', 1, 'Gain 6 Block.', 0, 6, '[]', 0, 3, 'Gain 9 Block.', 1),
-- 24. 铁壁
('铁壁', 'Iron Wall', 'skill', 'common', 2, 'Gain 12 Block.', 0, 12, '[]', 0, 4, 'Gain 16 Block.', 1),
-- 25. 盾墙
('盾墙', 'Shield Wall', 'skill', 'common', 1, 'Gain 4 Block. Gain 1 Dexterity.', 0, 4, '[{"type":"dexterity","value":1}]', 0, 2, 'Gain 6 Block. Gain 1 Dexterity.', 1),
-- 26. 战术撤退
('战术撤退', 'Tactical Retreat', 'skill', 'common', 1, 'Gain 8 Block. Draw 2 cards.', 0, 8, '[{"type":"draw","value":2}]', 0, 3, 'Gain 11 Block. Draw 2 cards.', 1),
-- 27. 坚守阵地
('坚守阵地', 'Hold Ground', 'skill', 'uncommon', 2, 'Gain 10 Block. Gain 1 Strength.', 0, 10, '[{"type":"strength","value":1}]', 0, 4, 'Gain 14 Block. Gain 1 Strength.', 1),
-- 28. 铁甲
('铁甲', 'Iron Armor', 'skill', 'uncommon', 2, 'Gain 12 Block. Exhaust.', 0, 12, '[{"type":"exhaust"}]', 0, 6, 'Gain 18 Block. Exhaust.', 1),
-- 29. 反射
('反射', 'Reflection', 'skill', 'uncommon', 2, 'Gain 6 Block. Enemy takes half the damage dealt this turn.', 0, 6, '[]', 0, 3, 'Gain 9 Block. Enemy takes half the damage dealt this turn.', 1),
-- 30. 哨兵
('哨兵', 'Sentinel', 'skill', 'common', 1, 'Gain 5 Block. Draw 1 card.', 0, 5, '[{"type":"draw","value":1}]', 0, 2, 'Gain 7 Block. Draw 1 card.', 1),
-- 31. 绊马索
('绊马索', 'Tripwire', 'skill', 'common', 1, 'Gain 5 Block. Apply 1 Vulnerable.', 0, 5, '[{"type":"vulnerable","value":1}]', 0, 2, 'Gain 7 Block. Apply 1 Vulnerable.', 1),
-- 32. 战旗
('战旗', 'Battle Standard', 'skill', 'uncommon', 1, 'Gain 3 Block. All attacks +2 damage this turn.', 0, 3, '[]', 0, 2, 'Gain 5 Block. All attacks +3 damage this turn.', 1),
-- 33. 铜墙铁壁
('铜墙铁壁', 'Fortress', 'skill', 'uncommon', 3, 'Gain 18 Block.', 0, 18, '[]', 0, 6, 'Gain 24 Block.', 1),
-- 34. 卸力
('卸力', 'Parry', 'skill', 'common', 0, 'Gain 3 Block.', 0, 3, '[]', 0, 2, 'Gain 5 Block.', 1),
-- 35. 盾牌猛击
('盾牌猛击', 'Shield Bash', 'attack', 'uncommon', 2, 'Gain 6 Block. Deal damage equal to Block gained.', 6, 6, '[]', 2, 2, 'Gain 8 Block. Deal damage equal to Block gained.', 1);

-- ==================== 技能牌（10张） ====================
INSERT IGNORE INTO expedition_cards (card_name, card_name_en, card_type, rarity, cost, description, base_damage, base_block, keywords, upgrade_damage, upgrade_block, upgrade_description, show_id) VALUES
('战术谋划', 'Tactical Planning', 'skill', 'common', 1, 'Draw 2 cards.', 0, 0, '[{"type":"draw","value":2}]', 0, 0, 'Draw 3 cards.', 1),
('集结', 'Rally', 'skill', 'common', 1, 'Gain 1 Energy. Exhaust.', 0, 0, '[{"type":"exhaust"}]', 0, 0, 'Gain 2 Energy. Exhaust.', 1),
('鼓舞', 'Inspire', 'skill', 'uncommon', 2, 'Gain 1 Strength. Exhaust.', 0, 0, '[{"type":"strength","value":1},{"type":"exhaust"}]', 0, 0, 'Gain 2 Strength. Exhaust.', 1),
('磨刀石', 'Whetstone', 'skill', 'uncommon', 1, 'All attacks +2 damage this turn.', 0, 0, '[]', 0, 0, 'All attacks +3 damage this turn.', 1),
('包扎', 'Bandage', 'skill', 'common', 1, 'Heal 4 HP. Exhaust.', 0, 0, '[{"type":"heal","value":4},{"type":"exhaust"}]', 0, 0, 'Heal 7 HP. Exhaust.', 1),
('战前准备', 'Battle Prep', 'skill', 'common', 0, 'Draw 1 card. If hand has 3 or fewer, draw 1 more.', 0, 0, '[{"type":"draw","value":2}]', 0, 0, 'Draw 2 cards. If hand has 4 or fewer, draw 1 more.', 1),
('振奋', 'Energize', 'skill', 'uncommon', 2, 'Gain 2 Energy. Draw 2 cards.', 0, 0, '[]', 0, 0, 'Gain 3 Energy. Draw 2 cards.', 1),
('急行军', 'March', 'skill', 'common', 1, 'Draw 3 cards. Discard 1 card.', 0, 0, '[{"type":"draw","value":3}]', 0, 0, 'Draw 4 cards. Discard 1 card.', 1),
('突袭命令', 'Assault Order', 'skill', 'uncommon', 1, 'All attacks cost 1 less this turn.', 0, 0, '[]', 0, 0, 'All attacks cost 1 less this turn.', 1),
('回天', 'Revitalize', 'skill', 'rare', 3, 'Heal 12 HP. Exhaust. Convert all Block to healing.', 0, 0, '[{"type":"heal","value":12},{"type":"exhaust"}]', 0, 0, 'Heal 16 HP. Exhaust. Convert all Block to healing.', 1);

-- ==================== 能力牌（5张） ====================
INSERT IGNORE INTO expedition_cards (card_name, card_name_en, card_type, rarity, cost, description, base_damage, base_block, keywords, upgrade_damage, upgrade_block, upgrade_description, show_id) VALUES
('战争艺术', 'Art of War', 'power', 'rare', 2, 'At the start of each turn, gain 1 Strength.', 0, 0, '[{"type":"strength","value":1}]', 0, 0, 'At the start of each turn, gain 2 Strength.', 1),
('铁甲护体', 'Iron Body', 'power', 'uncommon', 2, 'At the start of each turn, gain 2 Block.', 0, 0, '[]', 0, 0, 'At the start of each turn, gain 3 Block.', 1),
('狂暴', 'Berserker', 'power', 'uncommon', 1, 'Lose 3 Max HP. Gain 1 Strength at start of each turn.', 0, 0, '[{"type":"strength","value":1}]', 0, 0, 'Lose 2 Max HP. Gain 1 Strength at start of each turn.', 1),
('寒冰护盾', 'Ice Shield', 'power', 'rare', 1, 'Gain 1 Block at start of each turn. Attacker takes 2 damage.', 0, 0, '[]', 0, 0, 'Gain 2 Block at start of each turn. Attacker takes 2 damage.', 1),
('复仇意志', 'Vengeance', 'power', 'rare', 2, 'For each 1 HP lost, deal 1 extra damage this combat.', 0, 0, '[]', 0, 0, 'For each 1 HP lost, deal 1 extra damage this combat.', 1);

-- ==================== 第一幕敌人（含行为模式） ====================

-- 更新现有敌人：添加行为模式到 special_rules
UPDATE expedition_enemies SET special_rules = '{"pattern":[{"type":"attack","value":6},{"type":"attack","value":6},{"type":"attack","value":6}]}' WHERE name_en = 'Wildling Scout' AND act = 1;

UPDATE expedition_enemies SET special_rules = '{"pattern":[{"type":"attack","value":8},{"type":"block","value":6},{"type":"attack","value":8}]}' WHERE name_en = 'Direwolf' AND act = 1;

-- 新增敌人（符合计划中的数据）
INSERT IGNORE INTO expedition_enemies (show_id, act, name_cn, name_en, hp, is_boss, special_rules, reward_pool) VALUES
(1, 1, '野人斥候', 'Wildling Scout', 16, FALSE,
 '{"pattern":[{"type":"attack","value":6},{"type":"attack","value":6},{"type":"attack","value":6}]}',
 '["card","gold"]'),

(1, 1, '冰原狼', 'Direwolf', 20, FALSE,
 '{"pattern":[{"type":"attack","value":8},{"type":"block","value":6},{"type":"attack","value":8}]}',
 '["card","gold"]'),

(1, 1, '守夜人逃兵', 'Deserter', 18, FALSE,
 '{"pattern":[{"type":"attack","value":7},{"type":"attack","value":7},{"type":"debuff","buff":"weak","value":1},{"type":"attack","value":7}]}',
 '["card","gold"]'),

(1, 1, '雪地伏击者', 'Snow Ambusher', 22, FALSE,
 '{"pattern":[{"type":"attack","value":8,"times":2},{"type":"attack","value":6},{"type":"attack","value":10}]}',
 '["card","gold"]'),

(1, 1, '班扬·史塔克', 'Benjen Stark', 48, FALSE,
 '{"pattern":[{"type":"buff","buff":"strength","value":2},{"type":"attack","value":10},{"type":"attack","value":12},{"type":"attack","value":14}]}',
 '["card","gold","relic_chance"]'),

(1, 1, '野人掠夺者', 'Wildling Marauder', 44, FALSE,
 '{"pattern":[{"type":"attack","value":8},{"type":"attack","value":8},{"type":"buff","buff":"strength","value":3},{"type":"attack","value":12}]}',
 '["card","gold","relic_chance"]'),

(1, 1, '野人首领曼斯·雷德', 'Mance Rayder', 70, TRUE,
 '{"pattern":[{"type":"attack","value":10},{"type":"attack","value":12},{"type":"buff","buff":"strength","value":2,"target":"self"},{"type":"attack","value":15}],"phase2_pattern":[{"type":"attack","value":12,"times":2},{"type":"attack","value":18}],"phase2_hp_percent":50}',
 '["card","relic","gold"]');

INSERT IGNORE INTO expedition_enemies (show_id, act, name_cn, name_en, hp, is_boss, special_rules, reward_pool) VALUES
(1, 1, '班扬·史塔克', 'Benjen Stark', 48, FALSE,
 '{"pattern":[{"type":"buff","buff":"strength","value":2,"target":"self"},{"type":"attack","value":10},{"type":"attack","value":12},{"type":"attack","value":14}]}',
 '["card","gold","relic_chance"]');

-- ==================== 节点内容（占位符） ====================

-- Act 1 节点：Slay the Spire 风格地图（约15层）
-- 每层3个可选节点，路径分支
-- 层1-6: 前置战斗/事件
-- 层7: 篝火
-- 层8-10: 中段战斗/精英
-- 层11: 商店
-- 层12-14: 后段战斗/事件
-- 层15: 篝火
-- 层16: Boss

-- 层1: 3个战斗节点入口
INSERT IGNORE INTO node_content (act, floor, node_type, position, title, content, connected_to, is_boss_node) VALUES
(1, 1, 'combat', 0, 'The Wall - West Gate',
 'The Wall stretches before you, a monument of ice and ancient magic. Beyond it lies the true north, a land of snow and secrets. Lord Commander Mormont has sent you on a ranging mission to discover what lies beyond the Wall. The wind howls through the battlements as you prepare to venture into the unknown. A group of wildling scouts has been spotted near the western gate. They must be dealt with before they can report your movements.',
 '[2,2,2]', FALSE),
(1, 1, 'combat', 1, 'The Wall - East Gate',
 'The eastern approach to Castle Black is quieter, but no less dangerous. The snow lies thick on the ground, and the cold seeps through even the thickest furs. You tighten your cloak and begin the descent down the winding staircase carved into the ice itself. Below, the haunted forest waits, its trees standing like silent sentinels. Reports speak of a direwolf prowling these parts, driven south by the harsh winter.',
 '[2,2]', FALSE),
(1, 1, 'combat', 2, 'The Wall - Castle Black',
 'You take the main gate through Castle Black, passing the training yard where recruits practice with wooden swords. The Night''s Watch is stretched thin, and every ranger is needed beyond the Wall. The gate creaks open, and the cold hits you like a wall. Beyond lies the true north, and your first test. A wildling raiding party has been seen nearby, and they must be stopped.',
 '[2,3]', FALSE);

-- 层2: 战斗/事件岔路
INSERT IGNORE INTO node_content (act, floor, node_type, position, title, content, connected_to, is_boss_node) VALUES
(1, 2, 'combat', 0, 'The Haunted Forest - Edge',
 'You enter the haunted forest, where the trees grow twisted and strange. The canopy blocks out the sun, casting everything in an eerie twilight. Snow muffles your footsteps as you move deeper. The air smells of pine and decay. Something moves in the shadows ahead. You grip your weapon and press forward.',
 '[3,3]', FALSE),
(1, 2, 'event', 1, 'Abandoned Camp',
 'You discover an abandoned wildling camp. The fires are still warm, suggesting the occupants left in a hurry. Among the debris, you find scattered supplies and what appears to be a crude map drawn on leather. The map shows a path through the forest that avoids the main wildling patrols. A raven''s feather lies beside the map, still black as night.',
 '[3,3,3]', FALSE),
(1, 2, 'combat', 2, 'Frozen Stream',
 'A frozen stream cuts across your path. The ice looks solid, but you can hear the water flowing beneath. On the far bank, movement catches your eye. Wildling scouts are crossing upstream. If you hurry, you can cut them off before they reach higher ground.',
 '[3,4]', FALSE);

-- 层3: 战斗节点
INSERT IGNORE INTO node_content (act, floor, node_type, position, title, content, connected_to, is_boss_node) VALUES
(1, 3, 'combat', 0, 'Wolf Den',
 'You come upon a den of direwolves. The massive wolves are larger than any you have seen in the south, with thick grey fur and eyes that glow in the dim light. They have been feeding on something, and they do not appreciate the intrusion. The alpha rises, baring teeth as long as your fingers.',
 '[4,4]', FALSE),
(1, 3, 'combat', 1, 'Ranger Tower',
 'An old ranger tower stands atop a rocky outcropping. It has been abandoned for years, but smoke rises from the chimney. Someone - or something - has taken up residence. The door hangs open, and you can hear rough voices from inside. Deserters from the Night''s Watch, by the sound of it.',
 '[4,4,5]', FALSE),
(1, 3, 'rest', 2, 'Sacred Grove',
 'You find a grove of weirwood trees, their white bark and red leaves creating a stark contrast against the snow. The faces carved into the trunks seem to watch you. The ground is soft with fallen leaves, and the area feels protected somehow. A good place to rest and tend to your wounds. The old gods watch over this place.',
 '[4,5]', FALSE);

-- 层4: 战斗/事件
INSERT IGNORE INTO node_content (act, floor, node_type, position, title, content, connected_to, is_boss_node) VALUES
(1, 4, 'combat', 0, 'Hidden Path',
 'You find a narrow game trail winding through the thickest part of the forest. It is barely visible, but it leads in the direction you need to go. As you push through the undergrowth, you hear voices ahead. Wildling scouts are using this same path. They have not seen you yet.',
 '[5,5]', FALSE),
(1, 4, 'event', 1, 'Crone''s Hut',
 'Deep in the woods, you find a small hut that seems out of place. An old woman sits by the fire, stirring a pot. She speaks in the Old Tongue, but she seems to understand the Common Tongue well enough. She offers you warmth and food, but her eyes hold secrets. There is something not quite right about this place.',
 '[5,5,6]', FALSE),
(1, 4, 'shop', 2, 'Wandering Merchant',
 'A wildling merchant has set up a makeshift stall in a clearing. He has furs, dried meat, and various trinkets spread out on a sled. He eyes you warily but does not reach for his weapon. "Trade?" he asks in broken Common. His goods look surprisingly well-made for a wildling craftsman.',
 '[5,6]', FALSE);

-- 层5: 战斗节点
INSERT IGNORE INTO node_content (act, floor, node_type, position, title, content, connected_to, is_boss_node) VALUES
(1, 5, 'combat', 0, 'Frozen Lake',
 'The forest opens onto a frozen lake. The ice is smooth and dark, reflecting the grey sky like a mirror. In the middle of the lake, a group of wildlings are performing some sort of ritual. They have not noticed you yet, but your passage across the open ice would be exposed. You need to cross, or find another way around.',
 '[6,6]', FALSE),
(1, 5, 'rest', 1, 'Cave Shelter',
 'You discover a cave hidden behind a frozen waterfall. The cave is dry and relatively warm, with evidence of previous occupants. A fire pit has been dug in the center, and there is a stack of dry wood nearby. The sound of rushing water fills the cave, creating a surprisingly peaceful atmosphere. A good place to recuperate.',
 '[6,6,6]', FALSE),
(1, 5, 'combat', 2, 'Ambush Alley',
 'The terrain narrows into a rocky pass. It is an ideal place for an ambush, and you are proven right when arrows fly from the rocks above. Snow-covered wildlings emerge from hiding, their weapons ready. They have done this before. The leader barks orders in the Old Tongue.',
 '[6,7]', FALSE);

-- 层6: 精英层
INSERT IGNORE INTO node_content (act, floor, node_type, position, title, content, connected_to, is_boss_node) VALUES
(1, 6, 'combat', 0, 'Old Battlefield',
 'You stumble onto an old battlefield, where the bones of fallen warriors protrude from the snow like pale branches. The ground is littered with shattered weapons and frozen banners. In the center stands a lone figure, seemingly waiting for you. He wears the black of the Night''s Watch, but his cloak is tattered and his eyes are hard. Benjen Stark has heard of your ranging and comes to test your worth.',
 '[7,7]', FALSE),
(1, 6, 'event', 1, 'Hot Spring',
 'Steam rises from a pool of water fed by volcanic activity deep underground. The hot spring is surrounded by moss-covered rocks, and the air here is warm and humid. The contrast with the frozen forest is striking. You could rest here and recover, but the steam would mask the approach of enemies. Strange markings on the rocks suggest this place is considered sacred.',
 '[7,7,7]', FALSE),
(1, 6, 'combat', 2, 'Raider Camp',
 'You find a large wildling raider camp. These are not simple scouts; these are hardened warriors, their faces painted with woad and their weapons sharp. They have been raiding the Gift, the fertile lands south of the Wall. Their leader is a massive brute who swings an axe as if it weighs nothing. The camp is well-organized, suggesting these are not just random raiders.',
 '[7,8]', FALSE);

-- 层7: 篝火
INSERT IGNORE INTO node_content (act, floor, node_type, position, title, content, connected_to, is_boss_node) VALUES
(1, 7, 'rest', 0, 'Watchtower',
 'You reach an ancient watchtower that predates the Wall itself. Built by the First Men, its stones are covered in old runes that tell of battles long forgotten. The tower offers shelter and a strategic view of the surrounding forest. A fire can be lit safely within its stone walls without being seen from outside.',
 '[8,8]', FALSE),
(1, 7, 'rest', 1, 'Wildling Hut',
 'You find a deserted wildling hut, built from sod and timbers. It is crude but functional. There is a fire pit and beds of furs. Whoever lived here left in a hurry, abandoning tools and personal effects. The hut offers shelter from the elements and a chance to rest.',
 '[8,8,8]', FALSE),
(1, 7, 'rest', 2, 'Frozen Waterfall Cave',
 'Behind a frozen waterfall, you find another cave system. This one is larger, with multiple chambers. Glowing fungi line the walls, providing dim illumination. The air is still and quiet. A perfect place to rest and prepare for the challenges ahead.',
 '[8,9]', FALSE);

-- 层8-9: 中段战斗
INSERT IGNORE INTO node_content (act, floor, node_type, position, title, content, connected_to, is_boss_node) VALUES
(1, 8, 'combat', 0, 'Frost Canyon',
 'The ground drops away into a deep canyon carved by ancient glaciers. A narrow bridge of ice spans the gap, creaking under its own weight. On the far side, wildlings are waiting. They have been expecting you and have prepared an ambush. There is no other way across for miles.',
 '[9,9]', FALSE),
(1, 8, 'combat', 1, 'Snowstorm',
 'A sudden snowstorm reduces visibility to almost nothing. You struggle forward, your cloak heavy with accumulating snow. In the white chaos, shapes move. The wildlings know this weather and use it to their advantage. You hear their war cries mixing with the howling wind.',
 '[9,9,10]', FALSE),
(1, 8, 'event', 2, 'Buried Temple',
 'The storm reveals something unexpected: the entrance to an ancient temple buried beneath the snow for centuries. The stone door is carved with the symbols of the old gods and the children of the forest. What secrets lie within? The temple radiates an ancient power that both intrigues and disturbs you.',
 '[9,10]', FALSE);

INSERT IGNORE INTO node_content (act, floor, node_type, position, title, content, connected_to, is_boss_node) VALUES
(1, 9, 'combat', 0, 'Ice Caves',
 'The trail leads into a labyrinth of ice caves. The walls glow with an ethereal blue light, and the sound of dripping water echoes through the tunnels. The ice formations are beautiful but treacherous. Someone - or something - has been using these caves as a highway. Fresh footprints mark the frozen floor.',
 '[10,10]', FALSE),
(1, 9, 'rest', 1, 'Geothermal Vent',
 'A crack in the earth releases warm air from deep below, creating a pocket of warmth in the frozen landscape. Moss and even some hardy flowers grow here. The ground is comfortable, and the warmth seeps into your cold bones. This natural hot spot is a welcome respite from the bitter cold.',
 '[10,10,11]', FALSE),
(1, 9, 'combat', 2, 'Ridge Ambush',
 'You follow a ridge that offers a clear view of the surrounding forest. The vantage point is excellent, but it also makes you silhouetted against the sky. Wildling archers have taken position on a higher ridge. Their arrows rain down as you scramble for cover.',
 '[10,11]', FALSE);

-- 层10: 商店
INSERT IGNORE INTO node_content (act, floor, node_type, position, title, content, connected_to, is_boss_node) VALUES
(1, 10, 'shop', 0, 'Underground Market',
 'Deep beneath an uprooted tree, you discover a hidden market where wildlings trade with a man who wears the black of the Night''s Watch. Deserters, both sides, find common ground in commerce. The merchant offers you a nod of recognition. His goods include weapons, potions, and various supplies that could prove vital for the journey ahead. He speaks the Common Tongue with a northern accent.',
 '[11,11]', FALSE),
(1, 10, 'shop', 1, 'Free Folk Trader',
 'A group of free folk have set up a trading post by a frozen river. They are more civilized than the raiders, and they seem willing to do business. Their leader, a woman with grey-streaked hair, examines you with keen interest. "You''re the one they''re all talking about," she says. "The crow who walks alone." Her prices are fair, and her goods are quality.',
 '[11,11,11]', FALSE),
(1, 10, 'combat', 2, 'Supplies Cache',
 'You discover a hidden cache of supplies, likely stashed by rangers who passed this way before. But as you approach, you realize it is guarded. A massive snow bear has made its den here, and it does not appreciate intruders. The bear rises on its hind legs, easily ten feet tall. Its roar echoes through the forest.',
 '[11,12]', FALSE);

-- 层11-13: 后段战斗
INSERT IGNORE INTO node_content (act, floor, node_type, position, title, content, connected_to, is_boss_node) VALUES
(1, 11, 'combat', 0, 'Winding Pass',
 'The terrain becomes increasingly rocky and difficult. The pass winds through tight spaces where you must squeeze between boulders. It is slow going, and the cold seeps into your joints. Wildling hunters know these passes well and use them to trap unwary rangers. The sound of rolling stones ahead warns you of approaching danger.',
 '[12,12]', FALSE),
(1, 11, 'event', 1, 'Deserter''s Confession',
 'You find a man in tattered black, half-frozen against a tree. He is a deserter from the Watch, and he knows he will be executed if taken back. He speaks of what drove him to flee: not cowardice, but something he saw beyond the Wall. "The dead walk," he whispers, his eyes wild with terror. "I saw them. The dead walk."',
 '[12,12,13]', FALSE),
(1, 11, 'combat', 2, 'Wolf Pack',
 'A pack of direwolves has been tracking you for miles. They have finally decided to make their move. They surround you, their breath forming clouds in the cold air. The alpha is a massive beast with a scarred muzzle and one eye. The pack works together with deadly coordination, herding you toward a cliff edge.',
 '[12,13]', FALSE);

INSERT IGNORE INTO node_content (act, floor, node_type, position, title, content, connected_to, is_boss_node) VALUES
(1, 12, 'combat', 0, 'Frozen Fortress',
 'An ancient fortress of the First Men emerges from the snow. Its walls are made of black stone, and it stands defiant against the elements. The gate hangs broken, and snow has drifted into the courtyard. But lights flicker in the tower - someone occupies this place. Wildlings have made this fortress their stronghold.',
 '[13,13]', FALSE),
(1, 12, 'rest', 1, 'Shielded Valley',
 'You discover a valley sheltered from the wind by high cliffs. The sun actually reaches the floor here, and the snow is less deep. A small stream flows, not frozen, fed by an underground spring. The valley feels almost peaceful, a hidden paradise in the frozen north.',
 '[13,13,13]', FALSE),
(1, 12, 'combat', 2, 'Snowmelt River',
 'The spring thaw has turned a river into a raging torrent. The only crossing is a fallen tree, slick with ice and spray. On the far side, wildlings are waiting. They have you at a disadvantage, and they know it. Their leader shouts something in the Old Tongue, and you hear mocking laughter.',
 '[13,14]', FALSE);

INSERT IGNORE INTO node_content (act, floor, node_type, position, title, content, connected_to, is_boss_node) VALUES
(1, 13, 'combat', 0, 'Elite Camp',
 'This is no ordinary wildling camp. The warriors here are veterans, their weapons of steel rather than bone. They wear the trophies of fallen rangers. A massive banner made of Night''s Watch cloaks hangs from a pole. The Wildling Marauder who leads them is a legend beyond the Wall, known for his cruelty and his tactical mind.',
 '[14,14]', FALSE),
(1, 13, 'combat', 1, 'Ambush Canyon',
 'A narrow canyon funnels you into a kill zone. As you realize the danger, warriors rise from beneath the snow where they were hiding. The ambush is well-planned and nearly perfect. But you are not an easy target. The wildlings realize they have caught more than they bargained for.',
 '[14,14,15]', FALSE),
(1, 13, 'event', 2, 'Children of the Forest',
 'Deep in the woods, you encounter a being you thought existed only in legend. One of the children of the forest, small and dark, with eyes like pools of ancient knowledge. It speaks in a language older than the First Men, but somehow you understand. It offers you a choice: knowledge or power. The decision will shape your journey.',
 '[14,15]', FALSE);

-- 层14: 篝火
INSERT IGNORE INTO node_content (act, floor, node_type, position, title, content, connected_to, is_boss_node) VALUES
(1, 14, 'rest', 0, 'Last Hearth',
 'You find the ruins of an ancient hall, once the seat of a northern house. The roof is long gone, but the stone walls provide shelter. A great hearth, cold for centuries, dominates the main hall. You gather wood and start a fire. The flames cast dancing shadows on the carved stone walls, telling stories of a forgotten age.',
 '[15,15]', FALSE),
(1, 14, 'rest', 1, 'Hot Springs Cave',
 'Another hot spring, this one inside a cave system adorned with primitive paintings. The paintings tell a story of battle between men and something else, something cold and white. The warm water soothes your tired muscles, but the images on the walls trouble your sleep. What awaits you in the far north?',
 '[15,15,15]', FALSE),
(1, 14, 'rest', 2, 'Ranger Cache',
 'You find a well-stocked ranger cache, hidden in a hollow tree marked with the Watch''s sign. Inside are dried provisions, medicine, and a fresh set of warm clothing. Someone was preparing for a long journey north. A note, partially decayed, warns of "the King Beyond the Wall."',
 '[15,16]', FALSE);

-- 层15: 精英
INSERT IGNORE INTO node_content (act, floor, node_type, position, title, content, connected_to, is_boss_node) VALUES
(1, 15, 'combat', 0, 'Benjen Stark - Last Ranger',
 'A lone figure blocks your path, leaning on a spear. He wears the black of the Night''s Watch, but his cloak is pristine, his boots polished. First Ranger Benjen Stark has been tracking you since you left the Wall. "You have come far," he says, his breath misting in the cold. "But the true test lies ahead. Show me you are ready." He raises his spear in challenge.',
 '[16]', FALSE),
(1, 15, 'combat', 1, 'The Marauder''s Challenge',
 'The Wildling Marauder has heard of your progress and has come to meet you personally. He is a mountain of a man, clad in furs and boiled leather, wielding a great axe that could cleave a man in half. His warriors form a circle, chanting in the Old Tongue. This is a challenge, a trial by combat. Win, and you earn the right to face their king.',
 '[16,16]', FALSE),
(1, 15, 'combat', 2, 'Wight Scout',
 'A scout from the Night''s Watch rides toward you, his horse lathered with sweat. He has ridden hard and long. "The King Beyond the Wall," he gasps. "He''s gathering his host. Mance Rayder means to march on the Wall with all the free folk behind him. And there''s worse... there''s something in the snow with him." The scout''s face is pale with more than cold.',
 '[16]', FALSE);

-- 层16: Boss
INSERT IGNORE INTO node_content (act, floor, node_type, position, title, content, connected_to, is_boss_node) VALUES
(1, 16, 'boss', 0, 'Mance Rayder - The King Beyond the Wall',
 'At last you stand before the King Beyond the Wall. Mance Rayder sits on a makeshift throne of antlers and fur, his cloak a patchwork of fabrics from beyond the Wall and the seven kingdoms alike. He was once a man of the Night''s Watch, but he abandoned his vows to unite the free folk. He is a warrior, a leader, and a visionary. Around him, his fiercest warriors stand guard. He rises and draws his sword. "So," he says, his voice calm, "the Watch sends boys to stop me. Let us see what you are made of."',
 '[]', TRUE);
