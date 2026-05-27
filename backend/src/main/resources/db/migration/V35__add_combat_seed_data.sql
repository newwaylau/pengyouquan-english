-- 战斗系统种子数据：50张卡牌 + 第一幕敌人 + 节点内容

-- ==================== 基础卡（4张） ====================
INSERT IGNORE INTO expedition_cards (card_name, card_name_en, card_type, rarity, cost, description, base_damage, base_block, keywords, upgrade_damage, upgrade_block, upgrade_description, show_id) VALUES
('长剑劈砍', 'Strike', 'attack', 'basic', 1, '造成 6 点伤害。', 6, 0, '[]', 3, 0, '造成 9 点伤害。', 1),
('皮盾防御', 'Defend', 'skill', 'basic', 1, '获得 5 点格挡。', 0, 5, '[]', 0, 3, '获得 8 点格挡。', 1),
('铁剑斩', 'Iron Strike', 'attack', 'basic', 1, '造成 9 点伤害。', 9, 0, '[]', 0, 0, '', 1),
('铁盾防御', 'Iron Defend', 'skill', 'basic', 1, '获得 8 点格挡。', 0, 8, '[]', 0, 0, '', 1);

-- ==================== 攻击牌（20张） ====================
INSERT IGNORE INTO expedition_cards (card_name, card_name_en, card_type, rarity, cost, description, base_damage, base_block, keywords, upgrade_damage, upgrade_block, upgrade_description, show_id) VALUES
-- 1. 长剑劈砍 (done above)
-- 2. 铁剑斩 (done above)
-- 3. 北境劈斩 - 若敌人易伤+4
('北境劈斩', 'Northern Slash', 'attack', 'common', 1, '造成 8 点伤害。若敌人易伤则额外造成 4 点。', 8, 0, '[{"type":"vulnerable","value":1}]', 3, 0, '造成 11 点伤害。若敌人易伤则额外造成 4 点。', 1),
-- 4. 野人冲撞 - 2费 14伤
('野人冲撞', 'Wildling Charge', 'attack', 'common', 2, '造成 14 点伤害。', 14, 0, '[]', 4, 0, '造成 18 点伤害。', 1),
-- 5. 双刃斩 - 1费 5伤×2
('双刃斩', 'Double Strike', 'attack', 'common', 1, '造成 5 点伤害两次。', 5, 0, '[{"type":"multihit","value":2}]', 2, 0, '造成 7 点伤害两次。', 1),
-- 6. 重劈 - 2费 12伤 消耗
('重劈', 'Heavy Strike', 'attack', 'common', 2, '造成 12 点伤害。消耗。', 12, 0, '[{"type":"exhaust"}]', 4, 0, '造成 16 点伤害。消耗。', 1),
-- 7. 投掷长矛 - 1费 7伤 抽1
('投掷长矛', 'Spear Throw', 'attack', 'common', 1, '造成 7 点伤害。抽 1 张牌。', 7, 0, '[{"type":"draw","value":1}]', 2, 0, '造成 9 点伤害。抽 1 张牌。', 1),
-- 8. 怒吼 - 0费 4伤 消耗
('怒吼', 'War Cry', 'attack', 'common', 0, '造成 4 点伤害。消耗。', 4, 0, '[{"type":"exhaust"}]', 2, 0, '造成 6 点伤害。消耗。', 1),
-- 9. 旋风斩 - X费 X×4伤
('旋风斩', 'Whirlwind', 'attack', 'uncommon', 99, '造成 4 点伤害 X 次。', 4, 0, '[{"type":"aoe"}]', 1, 0, '造成 5 点伤害 X 次。X 费。', 1),
-- 10. 突刺 - 1费 6伤 抽1
('突刺', 'Lunge', 'attack', 'common', 1, '造成 6 点伤害。抽 1 张牌。', 6, 0, '[{"type":"draw","value":1}]', 2, 0, '造成 8 点伤害。抽 1 张牌。', 1),
-- 11. 烈火剑 - 1费 5伤 2烧伤
('烈火剑', 'Fire Sword', 'attack', 'uncommon', 1, '造成 5 点伤害。施加 2 层中毒。', 5, 0, '[{"type":"poison","value":2}]', 2, 0, '造成 7 点伤害。施加 3 层中毒。', 1),
-- 12. 碎甲锤 - 2费 8伤 移除所有格挡
('碎甲锤', 'Shield Breaker', 'attack', 'uncommon', 2, '造成 8 点伤害。移除敌人所有格挡。', 8, 0, '[]', 3, 0, '造成 11 点伤害。移除敌人所有格挡。', 1),
-- 13. 连击 - 1费 3伤×3
('连击', 'Combo Strike', 'attack', 'common', 1, '造成 3 点伤害三次。', 3, 0, '[{"type":"multihit","value":3}]', 1, 0, '造成 4 点伤害三次。', 1),
-- 14. 致命一击 - 2费 6伤 半血翻倍
('致命一击', 'Finishing Blow', 'attack', 'uncommon', 2, '造成 6 点伤害。若敌人生命低于 50% 则伤害翻倍。', 6, 0, '[]', 3, 0, '造成 9 点伤害。若敌人生命低于 50% 则伤害翻倍。', 1),
-- 15. 战吼 - 2费 10伤 1易伤
('战吼', 'Battle Cry', 'attack', 'common', 2, '造成 10 点伤害。施加 1 层易伤。', 10, 0, '[{"type":"vulnerable","value":1}]', 3, 0, '造成 13 点伤害。施加 2 层易伤。', 1),
-- 16. 回旋踢 - 1费 5伤 1虚弱
('回旋踢', 'Roundhouse Kick', 'attack', 'common', 1, '造成 5 点伤害。施加 1 层虚弱。', 5, 0, '[{"type":"weak","value":1}]', 2, 0, '造成 7 点伤害。施加 1 层虚弱。', 1),
-- 17. 穿刺 - 2费 7伤 无视格挡
('穿刺', 'Piercing Strike', 'attack', 'uncommon', 2, '造成 7 点伤害。无视格挡。', 7, 0, '[]', 3, 0, '造成 10 点伤害。无视格挡。', 1),
-- 18. 猛击 - 3费 20伤
('猛击', 'Slam', 'attack', 'uncommon', 3, '造成 20 点伤害。', 20, 0, '[]', 5, 0, '造成 25 点伤害。', 1),
-- 19. 血色打击 - 1费 4伤 回2血
('血色打击', 'Blood Strike', 'attack', 'common', 1, '造成 4 点伤害。恢复 2 点生命。', 4, 0, '[{"type":"heal","value":2}]', 1, 0, '造成 5 点伤害。恢复 3 点生命。', 1),
-- 20. 终结技 - 2费 8伤 中毒翻倍
('终结技', 'Execute', 'attack', 'uncommon', 2, '造成 8 点伤害。若敌人中毒则伤害翻倍。', 8, 0, '[{"type":"poison","value":2}]', 3, 0, '造成 11 点伤害。若敌人中毒则伤害翻倍。', 1);

-- ==================== 格挡/技能牌（15张） ====================
INSERT IGNORE INTO expedition_cards (card_name, card_name_en, card_type, rarity, cost, description, base_damage, base_block, keywords, upgrade_damage, upgrade_block, upgrade_description, show_id) VALUES
-- 21. 皮盾防御 (done above)
-- 22. 铁盾防御 (done above)
-- 23. 举盾
('举盾', 'Shield Up', 'skill', 'common', 1, '获得 6 点格挡。', 0, 6, '[]', 0, 3, '获得 9 点格挡。', 1),
-- 24. 铁壁
('铁壁', 'Iron Wall', 'skill', 'common', 2, '获得 12 点格挡。', 0, 12, '[]', 0, 4, '获得 16 点格挡。', 1),
-- 25. 盾墙
('盾墙', 'Shield Wall', 'skill', 'common', 1, '获得 4 点格挡。获得 1 点敏捷。', 0, 4, '[{"type":"dexterity","value":1}]', 0, 2, '获得 6 点格挡。获得 1 点敏捷。', 1),
-- 26. 战术撤退
('战术撤退', 'Tactical Retreat', 'skill', 'common', 1, '获得 8 点格挡。抽 2 张牌。', 0, 8, '[{"type":"draw","value":2}]', 0, 3, '获得 11 点格挡。抽 2 张牌。', 1),
-- 27. 坚守阵地
('坚守阵地', 'Hold Ground', 'skill', 'uncommon', 2, '获得 10 点格挡。获得 1 点力量。', 0, 10, '[{"type":"strength","value":1}]', 0, 4, '获得 14 点格挡。获得 1 点力量。', 1),
-- 28. 铁甲
('铁甲', 'Iron Armor', 'skill', 'uncommon', 2, '获得 12 点格挡。消耗。', 0, 12, '[{"type":"exhaust"}]', 0, 6, '获得 18 点格挡。消耗。', 1),
-- 29. 反射
('反射', 'Reflection', 'skill', 'uncommon', 2, '获得 6 点格挡。敌人受到本回合造成伤害的一半。', 0, 6, '[]', 0, 3, '获得 9 点格挡。敌人受到本回合造成伤害的一半。', 1),
-- 30. 哨兵
('哨兵', 'Sentinel', 'skill', 'common', 1, '获得 5 点格挡。抽 1 张牌。', 0, 5, '[{"type":"draw","value":1}]', 0, 2, '获得 7 点格挡。抽 1 张牌。', 1),
-- 31. 绊马索
('绊马索', 'Tripwire', 'skill', 'common', 1, '获得 5 点格挡。施加 1 层易伤。', 0, 5, '[{"type":"vulnerable","value":1}]', 0, 2, '获得 7 点格挡。施加 1 层易伤。', 1),
-- 32. 战旗
('战旗', 'Battle Standard', 'skill', 'uncommon', 1, '获得 3 点格挡。本回合所有攻击伤害 +2。', 0, 3, '[]', 0, 2, '获得 5 点格挡。本回合所有攻击伤害 +3。', 1),
-- 33. 铜墙铁壁
('铜墙铁壁', 'Fortress', 'skill', 'uncommon', 3, '获得 18 点格挡。', 0, 18, '[]', 0, 6, '获得 24 点格挡。', 1),
-- 34. 卸力
('卸力', 'Parry', 'skill', 'common', 0, '获得 3 点格挡。', 0, 3, '[]', 0, 2, '获得 5 点格挡。', 1),
-- 35. 盾牌猛击
('盾牌猛击', 'Shield Bash', 'attack', 'uncommon', 2, '获得 6 点格挡。造成等同于格挡的伤害。', 6, 6, '[]', 2, 2, '获得 8 点格挡。造成等同于格挡的伤害。', 1);

-- ==================== 技能牌（10张） ====================
INSERT IGNORE INTO expedition_cards (card_name, card_name_en, card_type, rarity, cost, description, base_damage, base_block, keywords, upgrade_damage, upgrade_block, upgrade_description, show_id) VALUES
('战术谋划', 'Tactical Planning', 'skill', 'common', 1, '抽 2 张牌。', 0, 0, '[{"type":"draw","value":2}]', 0, 0, '抽 3 张牌。', 1),
('集结', 'Rally', 'skill', 'common', 1, '获得 1 点能量。消耗。', 0, 0, '[{"type":"exhaust"}]', 0, 0, '获得 2 点能量。消耗。', 1),
('鼓舞', 'Inspire', 'skill', 'uncommon', 2, '获得 1 点力量。消耗。', 0, 0, '[{"type":"strength","value":1},{"type":"exhaust"}]', 0, 0, '获得 2 点力量。消耗。', 1),
('磨刀石', 'Whetstone', 'skill', 'uncommon', 1, '本回合所有攻击伤害 +2。', 0, 0, '[]', 0, 0, '本回合所有攻击伤害 +3。', 1),
('包扎', 'Bandage', 'skill', 'common', 1, '恢复 4 点生命。消耗。', 0, 0, '[{"type":"heal","value":4},{"type":"exhaust"}]', 0, 0, '恢复 7 点生命。消耗。', 1),
('战前准备', 'Battle Prep', 'skill', 'common', 0, '抽 1 张牌。若手牌小于等于 3 张，再抽 1 张。', 0, 0, '[{"type":"draw","value":2}]', 0, 0, '抽 2 张牌。若手牌小于等于 4 张，再抽 1 张。', 1),
('振奋', 'Energize', 'skill', 'uncommon', 2, '获得 2 点能量。抽 2 张牌。', 0, 0, '[]', 0, 0, '获得 3 点能量。抽 2 张牌。', 1),
('急行军', 'March', 'skill', 'common', 1, '抽 3 张牌。弃置 1 张牌。', 0, 0, '[{"type":"draw","value":3}]', 0, 0, '抽 4 张牌。弃置 1 张牌。', 1),
('突袭命令', 'Assault Order', 'skill', 'uncommon', 1, '本回合所有攻击费用减 1。', 0, 0, '[]', 0, 0, '本回合所有攻击费用减 1。', 1),
('回天', 'Revitalize', 'skill', 'rare', 3, '恢复 12 点生命。消耗。将所有格挡转化为治疗。', 0, 0, '[{"type":"heal","value":12},{"type":"exhaust"}]', 0, 0, '恢复 16 点生命。消耗。将所有格挡转化为治疗。', 1);

-- ==================== 能力牌（5张） ====================
INSERT IGNORE INTO expedition_cards (card_name, card_name_en, card_type, rarity, cost, description, base_damage, base_block, keywords, upgrade_damage, upgrade_block, upgrade_description, show_id) VALUES
('战争艺术', 'Art of War', 'power', 'rare', 2, '每回合开始时获得 1 点力量。', 0, 0, '[{"type":"strength","value":1}]', 0, 0, '每回合开始时获得 2 点力量。', 1),
('铁甲护体', 'Iron Body', 'power', 'uncommon', 2, '每回合开始时获得 2 点格挡。', 0, 0, '[]', 0, 0, '每回合开始时获得 3 点格挡。', 1),
('狂暴', 'Berserker', 'power', 'uncommon', 1, '失去 3 点最大生命。每回合开始时获得 1 点力量。', 0, 0, '[{"type":"strength","value":1}]', 0, 0, '失去 2 点最大生命。每回合开始时获得 1 点力量。', 1),
('寒冰护盾', 'Ice Shield', 'power', 'rare', 1, '每回合开始时获得 1 点格挡。攻击者受到 2 点伤害。', 0, 0, '[]', 0, 0, '每回合开始时获得 2 点格挡。攻击者受到 2 点伤害。', 1),
('复仇意志', 'Vengeance', 'power', 'rare', 2, '本场战斗中每失去 1 点生命，额外造成 1 点伤害。', 0, 0, '[]', 0, 0, '本场战斗中每失去 1 点生命，额外造成 1 点伤害。', 1);

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
(1, 1, 'combat', 0, '长城 - 西门',
 '长城巍然屹立在你面前，一座由寒冰和远古魔法筑成的丰碑。长城之外是真北之地，一片冰雪与秘密的土地。莫尔蒙总司令派你外出巡游，探寻长城之外的秘密。风在城垛间呼啸，你准备踏入未知。西门附近发现了一群野人斥候，必须在他们报告你的行踪之前将其解决。',
 '[2,2,2]', FALSE),
(1, 1, 'combat', 1, '长城 - 东门',
 '通往黑城堡的东侧道路更为安静，但同样危险。积雪厚厚地覆盖着地面，寒冷侵蚀着最厚的毛皮。你收紧斗篷，开始沿着冰面上开凿的蜿蜒阶梯下行。下方，鬼影森林静静等待，树木如同沉默的哨兵。有报告称一只冰原狼因严冬而南下，在此出没。',
 '[2,2]', FALSE),
(1, 1, 'combat', 2, '长城 - 黑城堡',
 '你从黑城堡的正门出发，经过训练场，新兵们正在那里用木剑练习。守夜人兵力紧张，每名游骑兵都需要前往长城之外。大门吱呀作响地打开，寒冷如墙般向你袭来。前方就是真正的北方，你的第一个考验。附近发现了一支野人劫掠队，必须阻止他们。',
 '[2,3]', FALSE);

-- 层2: 战斗/事件岔路
INSERT IGNORE INTO node_content (act, floor, node_type, position, title, content, connected_to, is_boss_node) VALUES
(1, 2, 'combat', 0, '鬼影森林 - 边缘',
 '你进入鬼影森林，树木扭曲怪异，树冠遮蔽了阳光，万物笼罩在诡异的暮色中。积雪掩盖了你的脚步声。空气中弥漫着松木和腐朽的气味。前方的阴影中有什么东西在移动。你握紧武器，继续前进。',
 '[3,3]', FALSE),
(1, 2, 'event', 1, '废弃营地',
 '你发现了一个被遗弃的野人营地。篝火仍有余温，说明居住者匆忙离开。在杂物中，你找到散落的补给和一张画在皮革上的粗糙地图。地图标出穿过森林、避开野人巡逻队的路线。地图旁有一根渡鸦的羽毛，漆黑如夜。',
 '[3,3,3]', FALSE),
(1, 2, 'combat', 2, '冰封溪流',
 '一条冰封的溪流横亘在你的道路上。冰面看起来很结实，但你能听到下方水流的声音。对岸有动静引起了你的注意。野人斥候正在上游渡河。如果你动作快，可以在他们抵达高地之前截住他们。',
 '[3,4]', FALSE);

-- 层3: 战斗节点
INSERT IGNORE INTO node_content (act, floor, node_type, position, title, content, connected_to, is_boss_node) VALUES
(1, 3, 'combat', 0, '狼穴',
 '你发现了一个冰原狼的巢穴。这些巨狼比你见过的任何南方狼都要大，有着厚实的灰色皮毛和在昏暗光线中发光的眼睛。它们正在进食，不欢迎打扰。头狼站起身，露出有你手指那么长的獠牙。',
 '[4,4]', FALSE),
(1, 3, 'combat', 1, '游骑兵塔',
 '一座古老的游骑兵塔矗立在岩石突出部上。它已被废弃多年，但烟囱中升起袅袅青烟。有人——或者什么东西——占据了这里。门敞开着，你能听到里面粗犷的声音。听起来是守夜人的逃兵。',
 '[4,4,5]', FALSE),
(1, 3, 'rest', 2, '神圣树林',
 '你发现了一片鱼梁木树林，白色的树皮和红色的叶子和白雪形成鲜明对比。树干上雕刻的面孔似乎在注视着你。地面铺满落叶，柔软舒适，这片区域有种被保护的感觉。是休息和疗伤的好地方。旧神守护着这片土地。',
 '[4,5]', FALSE);

-- 层4: 战斗/事件
INSERT IGNORE INTO node_content (act, floor, node_type, position, title, content, connected_to, is_boss_node) VALUES
(1, 4, 'combat', 0, '隐蔽小径',
 '你发现一条狭窄的兽道蜿蜒穿过森林最茂密的部分。几乎难以辨认，但通向你要去的方向。当你拨开灌木前行时，听到前方有说话声。野人斥候也在使用这条小路。他们还没有发现你。',
 '[5,5]', FALSE),
(1, 4, 'event', 1, '老妪小屋',
 '在森林深处，你发现一间显得格格不入的小屋。一个老妇人坐在火边搅动锅中的东西。她说的是上古语言，但似乎也能理解通用语。她给你提供温暖和食物，但她的眼中隐藏着秘密。这个地方有些不对劲。',
 '[5,5,6]', FALSE),
(1, 4, 'shop', 2, '流浪商人',
 '一个野人商人在空地中搭起了一个临时摊位。他把皮毛、干肉和各种小物件摊在雪橇上。他警惕地打量着你，但没有伸手去拿武器。"交易？"他用蹩脚的通用语问道。他的货物对于一个野人工匠来说出奇地精良。',
 '[5,6]', FALSE);

-- 层5: 战斗节点
INSERT IGNORE INTO node_content (act, floor, node_type, position, title, content, connected_to, is_boss_node) VALUES
(1, 5, 'combat', 0, '冰封之湖',
 '森林开阔处，露出一个冰封的湖泊。冰面平滑幽暗，如镜子般映照着灰色的天空。湖中央，一群野人正在举行某种仪式。他们还没有注意到你，但你在开阔冰面上的行动会被发现。你必须穿过湖面，或者另寻他路。',
 '[6,6]', FALSE),
(1, 5, 'rest', 1, '洞穴庇护所',
 '你发现了一个隐藏在冰冻瀑布后面的洞穴。洞穴干燥温暖，有之前居住者的痕迹。中央挖了一个火坑，旁边有一堆干柴。流水的声音充满洞穴，营造出出奇宁静的氛围。是恢复体力的好地方。',
 '[6,6,6]', FALSE),
(1, 5, 'combat', 2, '埋伏峡谷',
 '地形收窄成一条岩石隘口，是完美的伏击地点。当箭矢从上方岩石中飞来，你的猜测被证实了。身披白雪的野人从藏身处现身，武器已出鞘。他们对此驾轻就熟。首领用上古语言吼叫着下达命令。',
 '[6,7]', FALSE);

-- 层6: 精英层
INSERT IGNORE INTO node_content (act, floor, node_type, position, title, content, connected_to, is_boss_node) VALUES
(1, 6, 'combat', 0, '古战场',
 '你偶然发现一片古战场，阵亡战士的骸骨如苍白树枝般从雪中伸出。地面上散落着破碎的武器和冻僵的旗帜。中央站着一个孤独的身影，似乎在等待着你。他身着守夜人的黑衣，但斗篷破旧，目光坚毅。班扬·史塔克听说了你的巡游，前来考验你的实力。',
 '[7,7]', FALSE),
(1, 6, 'event', 1, '温泉',
 '一池由地下火山活动供给的泉水散发着蒸汽。温泉被长满苔藓的岩石环绕，这里的空气温暖湿润，与冰冻森林形成鲜明对比。你可以在此休息恢复，但蒸汽也会掩盖敌人的靠近。岩石上的奇异标记表明此地被视为圣地。',
 '[7,7,7]', FALSE),
(1, 6, 'combat', 2, '掠夺者营地',
 '你发现了一个大型野人掠夺者营地。这些人不是简单的斥候，而是身经百战的战士，脸上涂着靛蓝染料，武器锋利。他们一直在掠夺长城以南的富饶土地——赠地。他们的首领是一个挥舞巨斧如同无物的庞然大物。营地组织有序，表明他们不是普通的掠夺者。',
 '[7,8]', FALSE);

-- 层7: 篝火
INSERT IGNORE INTO node_content (act, floor, node_type, position, title, content, connected_to, is_boss_node) VALUES
(1, 7, 'rest', 0, '瞭望塔',
 '你到达了一座比长城本身还古老的瞭望塔。由先民建造，石头上刻满了讲述早已被遗忘的战役的古老符文。塔楼提供庇护，并可以俯瞰周围森林。可以在石墙内安全生火，不会被外面看到。',
 '[8,8]', FALSE),
(1, 7, 'rest', 1, '野人小屋',
 '你发现了一间用草皮和木材建造的废弃野人小屋。简陋但实用。里面有火坑和皮毛床铺。住在这里的人匆忙离开，遗弃了工具和个人物品。小屋可以遮风挡雨，给你一个休息的机会。',
 '[8,8,8]', FALSE),
(1, 7, 'rest', 2, '冰冻瀑布洞穴',
 '在一个冰冻瀑布后面，你发现了另一个洞穴系统。这个更大，有多个洞穴。墙壁上长满了发光的真菌，提供昏暗的照明。空气宁静。是休息和为前方挑战做准备的好地方。',
 '[8,9]', FALSE);

-- 层8-9: 中段战斗
INSERT IGNORE INTO node_content (act, floor, node_type, position, title, content, connected_to, is_boss_node) VALUES
(1, 8, 'combat', 0, '冰霜峡谷',
 '地面骤降，形成一个由远古冰川切割而成的深谷。一座狭窄的冰桥横跨峡谷，在自身重压下吱吱作响。对岸，野人在等候。他们早有预料，准备好了伏击。方圆数里没有其他通路。',
 '[9,9]', FALSE),
(1, 8, 'combat', 1, '暴风雪',
 '突然暴风雪袭来，能见度降到几乎为零。你艰难前行，斗篷上积满了沉重的雪。在白色的混沌中，有影子在移动。野人熟知这种天气并善于利用。你听到他们的战吼与狂风的呼啸声交织在一起。',
 '[9,9,10]', FALSE),
(1, 8, 'event', 2, '埋没神庙',
 '暴风雪揭示了一件意想不到的东西：一座被白雪掩埋了数个世纪的古庙入口。石门上刻着旧神和森林之子的符号。里面隐藏着什么秘密？神庙散发着一种既让你好奇又让你不安的古老力量。',
 '[9,10]', FALSE);

INSERT IGNORE INTO node_content (act, floor, node_type, position, title, content, connected_to, is_boss_node) VALUES
(1, 9, 'combat', 0, '冰晶洞穴',
 '小径通向一个冰洞迷宫。墙壁发出空灵的蓝光，滴水声在隧道中回荡。冰层精美但险恶。有人——或者什么东西——一直把这些洞穴当高速公路使用。冰冻的地面上有新鲜的脚印。',
 '[10,10]', FALSE),
(1, 9, 'rest', 1, '地热裂隙',
 '地面的一道裂隙释放出地底的温暖空气，在冰封的景色中创造出一片温暖区域。苔藓甚至一些耐寒的花朵在这里生长。地面舒适，温暖渗入你冰冷的骨髓。这片天然热点是严寒中难得的喘息之处。',
 '[10,10,11]', FALSE),
(1, 9, 'combat', 2, '山脊伏击',
 '你沿着山脊前进，可以清晰看到周围的森林。视野极佳，但也让你在天空的映衬下成为剪影。野人弓箭手已经在更高的山脊上就位。当你寻找掩护时，箭矢如雨般落下。',
 '[10,11]', FALSE);

-- 层10: 商店
INSERT IGNORE INTO node_content (act, floor, node_type, position, title, content, connected_to, is_boss_node) VALUES
(1, 10, 'shop', 0, '地下市场',
 '在一棵连根拔起的大树下，你发现了一个隐藏的市场。野人正与一个穿着守夜人黑衣的男人进行交易。双方——逃兵们——在商业中找到了共同点。商人向你点头致意。他的货物包括武器、药水和各种可能对前方旅程至关重要的补给品。他说的通用语带着北方口音。',
 '[11,11]', FALSE),
(1, 10, 'shop', 1, '自由民商人',
 '一群自由民在冰封的河边建立了一个贸易站。他们比掠夺者文明得多，似乎愿意做生意。他们的首领是一个头发花白的女人，饶有兴趣地打量着你。"你就是大家都在说的那个人，"她说，"那只独行的乌鸦。"她的价格公道，货物优质。',
 '[11,11,11]', FALSE),
(1, 10, 'combat', 2, '补给储藏点',
 '你发现了一个隐藏的补给储藏点，可能是之前经过的游骑兵留下的。但当你靠近时，你意识到有守卫。一只巨大的雪熊在这里筑巢，不欢迎入侵者。熊用后腿站立起来，足足有十英尺高。它的咆哮在森林中回荡。',
 '[11,12]', FALSE);

-- 层11-13: 后段战斗
INSERT IGNORE INTO node_content (act, floor, node_type, position, title, content, connected_to, is_boss_node) VALUES
(1, 11, 'combat', 0, '蜿蜒隘口',
 '地形变得越发崎岖难行。隘口蜿蜒穿过狭窄的空间，你必须在巨石之间挤过。行进缓慢，寒冷渗入关节。野人猎手熟悉这些隘口，用来陷阱粗心的游骑兵。前方滚石的声音警告你危险临近。',
 '[12,12]', FALSE),
(1, 11, 'event', 1, '逃兵的忏悔',
 '你发现一个穿着破烂黑衣的男人，半冻僵地靠在一棵树上。他是守夜人的逃兵，知道如果被带回去会被处决。他讲述了驱使他逃跑的原因：不是懦弱，而是他在长城之外看到的景象。"死人会走路，"他低声说，眼中充满恐惧，"我看到了。死人会走路。"',
 '[12,12,13]', FALSE),
(1, 11, 'combat', 2, '狼群',
 '一群冰原狼已经跟踪你数英里。它们终于决定采取行动了。它们包围了你，呼吸在冷空气中形成云雾。头狼是一只巨大的野兽，口鼻部有疤痕，仅有一只眼。狼群以致命的协作行动，把你驱向悬崖边缘。',
 '[12,13]', FALSE);

INSERT IGNORE INTO node_content (act, floor, node_type, position, title, content, connected_to, is_boss_node) VALUES
(1, 12, 'combat', 0, '冰封要塞',
 '一座先民的古老要塞从雪中浮现。城墙由黑石砌成，傲然屹立于风雪之中。大门破损，雪飘进了庭院。但塔楼中有灯光闪烁——有人占据了这个地方。野人把这座要塞当成了据点。',
 '[13,13]', FALSE),
(1, 12, 'rest', 1, '避风山谷',
 '你发现了一个被高耸悬崖遮蔽的山谷。阳光居然能照射到谷底，积雪也较浅。一条小溪潺潺流淌，没有被冻结，由地下泉水供给。山谷几乎让人感到安宁，是极北之地的一处隐藏天堂。',
 '[13,13,13]', FALSE),
(1, 12, 'combat', 2, '雪融之河',
 '春天融雪使河流变成了汹涌的急流。唯一的通道是一棵倒下的树，表面覆盖着冰和水花，非常滑。对岸，野人正在等候。他们知道你已经处于劣势。首领用上古语言喊了些什么，你听到了嘲弄的笑声。',
 '[13,14]', FALSE);

INSERT IGNORE INTO node_content (act, floor, node_type, position, title, content, connected_to, is_boss_node) VALUES
(1, 13, 'combat', 0, '精英营地',
 '这不是普通的野人营地。这里的战士都是老兵，武器是钢铁而非骨头。他们佩戴着阵亡游骑兵的战利品。一根杆子上挂着一面由守夜人斗篷制成的巨大旗帜。率领他们的野人掠夺者是长城之外的传奇，以其残忍和战术头脑闻名。',
 '[14,14]', FALSE),
(1, 13, 'combat', 1, '伏击峡谷',
 '一个狭窄的峡谷把你引向杀戮区。当你意识到危险时，战士从藏身的雪中冒了出来。伏击计划周密，近乎完美。但你可不是好对付的目标。野人意识到他们碰到了硬茬。',
 '[14,14,15]', FALSE),
(1, 13, 'event', 2, '森林之子',
 '在森林深处，你遇到了一个你以为只存在于传说中的存在。一个森林之子，身材矮小，肤色黝黑，双眼如蕴藏古老知识的深潭。它说着比先民更古老的语言，但不知为何你能够理解。它给你一个选择：知识或力量。这个决定将塑造你的旅程。',
 '[14,15]', FALSE);

-- 层14: 篝火
INSERT IGNORE INTO node_content (act, floor, node_type, position, title, content, connected_to, is_boss_node) VALUES
(1, 14, 'rest', 0, '最后壁炉',
 '你发现了一座古老厅堂的废墟，曾经是北方家族所在地。屋顶早已不在，但石墙仍能提供遮蔽。一个冰冷了数个世纪的巨大壁炉占据了大厅中央。你收集木柴生起火来。火焰在雕刻的石墙上投下舞动的影子，诉说着一个被遗忘时代的故事。',
 '[15,15]', FALSE),
(1, 14, 'rest', 1, '温泉洞穴',
 '又一个温泉，这次是在一个装饰着原始绘画的洞穴系统中。画作讲述了人类与某种寒冷而苍白的东西之间的战斗。温暖的水舒缓了你疲惫的肌肉，但墙上的画面让你难以入眠。远北之地还有什么在等待着你？',
 '[15,15,15]', FALSE),
(1, 14, 'rest', 2, '游骑兵藏匿点',
 '你发现了一个储备充足的游骑兵藏匿点，藏在标记着守夜人标志的空心树中。里面有干粮、药品和一套干净的保暖衣物。有人正在为北上长途旅行做准备。一张已经部分腐朽的便条警告着"塞外之王"。',
 '[15,16]', FALSE);

-- 层15: 精英
INSERT IGNORE INTO node_content (act, floor, node_type, position, title, content, connected_to, is_boss_node) VALUES
(1, 15, 'combat', 0, '班扬·史塔克 - 最后的游骑兵',
 '一个孤独的身影挡在你的路上，倚靠着一支长矛。他穿着守夜人的黑衣，但斗篷一尘不染，靴子擦得锃亮。首席游骑兵班扬·史塔克从你离开长城起就一直在跟踪你。"你走了很远，"他说，呼吸在寒冷中形成白雾，"但真正的考验还在前方。让我看看你是否准备好了。"他举起长矛发出挑战。',
 '[16]', FALSE),
(1, 15, 'combat', 1, '掠夺者的挑战',
 '野人掠夺者听说了你的进展，亲自前来会你。他是一座山般的男人，身着毛皮和熟皮甲，挥舞着一把能将人劈成两半的巨斧。他的战士围成一圈，用上古语言吟唱。这是一场挑战，一场战斗审判。赢了，你就获得面对他们国王的权利。',
 '[16,16]', FALSE),
(1, 15, 'combat', 2, '守夜人斥候',
 '一个守夜人的斥候骑马向你奔来，他的马浑身是汗。他已经赶了很远的路。"塞外之王，"他气喘吁吁地说，"他正在集结军队。曼斯·雷德打算率领所有自由民向长城进军。还有更糟的……雪中还有别的东西与他同行。"斥候的脸苍白得不只是因为寒冷。',
 '[16]', FALSE);

-- 层16: Boss
INSERT IGNORE INTO node_content (act, floor, node_type, position, title, content, connected_to, is_boss_node) VALUES
(1, 16, 'boss', 0, '曼斯·雷德 - 塞外之王',
 '你终于站在了塞外之王面前。曼斯·雷德坐在一个由鹿角和毛皮制成的临时王座上，他的斗篷由来自长城之外和七大王国的布料拼凑而成。他曾是守夜人的一员，但为了团结自由民而背弃了誓言。他是一位战士、领袖和远见者。在他周围，最凶猛的战士护卫着。他站起身，拔出剑。"那么，"他平静地说，"守夜人派了孩子来阻止我。让我看看你有多大本事。"',
 '[]', TRUE);
