-- V36: 新增远征事件数据
-- 权游事件+唐顿事件 各7个以上
-- 每个事件有 choices JSON(数组,含风险回报)

-- ========== 1. 权游事件 (Game of Thrones) ==========

-- 节点1: 遇见梅丽珊卓
INSERT IGNORE INTO expedition_events (show_id, act, title_cn, description_cn, choices)
SELECT s.id, 1, '红袍女的预言', '梅丽珊卓在路边等你，火光在她眼中跳动。"你的命运我将揭示——但代价是你的血。"',
  '[{"text":"接受预言（随机移除一张牌，加入一张稀有牌）","effect":{"type":"swap_card","rarity":"rare"},"risk":"high","reward":"稀有卡牌"},
    {"text":"拒绝（得25金币）","effect":{"type":"gold","value":25},"risk":"none","reward":"金币"},
    {"text":"攻击她（战斗，胜利获得稀有遗物）","effect":{"type":"combat_reward","value":"rare_relic"},"risk":"战斗风险","reward":"稀有遗物"}]'
FROM shows s WHERE s.name LIKE '%Game of Thrones%' OR s.name LIKE '%权游%' OR s.name LIKE '%GOT%' LIMIT 1;

-- 节点2: 铁金库的使者
INSERT IGNORE INTO expedition_events (show_id, act, title_cn, description_cn, choices)
SELECT s.id, 1, '铁金库的使者', '一名布拉佛斯银行家拦住你。"听说你需要资金？我们可以提供——但债务永远要还。"',
  '[{"text":"借50金币（下个休息节点必须还60金币，否则失去10血）","effect":{"type":"loan","amount":50,"due_heal":60,"penalty":10},"risk":"高利贷","reward":"50金币"},
    {"text":"展示实力（答2题，全对得40金币）","effect":{"type":"challenge_quiz","questions":2,"reward":40},"risk":"答题失败惩罚","reward":"40金币"},
    {"text":"礼貌拒绝","effect":{"type":"nothing"},"risk":"none","reward":"无"}]'
FROM shows s WHERE s.name LIKE '%Game of Thrones%' OR s.name LIKE '%权游%' OR s.name LIKE '%GOT%' LIMIT 1;

-- 节点3: 猎狗的邀请
INSERT IGNORE INTO expedition_events (show_id, act, title_cn, description_cn, choices)
SELECT s.id, 2, '猎狗的决斗', '桑铎·克里冈（猎狗）坐在篝火旁。"来打一场？活下来的拿走另一方的金币。"',
  '[{"text":"接受决斗（答3题，每题正确造成5伤害，错误受到5伤害）","effect":{"type":"duel","questions":3,"damage_per_hit":5},"risk":"答题决定胜负","reward":"30金币"},
    {"text":"请他喝酒（花10金币回15血）","effect":{"type":"heal","value":15},"cost":10,"risk":"花费","reward":"回血"},
    {"text":"偷袭他（战斗，胜利得稀有遗物但失败扣20血）","effect":{"type":"gamble_fight","reward":"rare_relic","penalty":20},"risk":"高风险","reward":"稀有遗物"}]'
FROM shows s WHERE s.name LIKE '%Game of Thrones%' OR s.name LIKE '%权游%' OR s.name LIKE '%GOT%' LIMIT 1;

-- 节点4: 学城的来信
INSERT IGNORE INTO expedition_events (show_id, act, title_cn, description_cn, choices)
SELECT s.id, 2, '学城的渡鸦', '一只学城渡鸦带来了一卷羊皮纸。山姆的笔迹潦草："我在古籍中发现了一个秘密……"',
  '[{"text":"研读古籍（练5句，全对获得传说卡牌）","effect":{"type":"study","sentences":5,"reward":"legendary_card"},"risk":"需要全对","reward":"传说卡牌"},
    {"text":"快速浏览（得30金币）","effect":{"type":"gold","value":30},"risk":"none","reward":"金币"},
    {"text":"寻找隐藏线索（回答3道难题，全对获得稀有遗物）","effect":{"type":"challenge_quiz","questions":3,"reward":"rare_relic"},"risk":"难题","reward":"稀有遗物"}]'
FROM shows s WHERE s.name LIKE '%Game of Thrones%' OR s.name LIKE '%权游%' OR s.name LIKE '%GOT%' LIMIT 1;

-- 节点5: 多恩的礼物
INSERT IGNORE INTO expedition_events (show_id, act, title_cn, description_cn, choices)
SELECT s.id, 2, '多恩的毒药', '一名多恩人递给你一个精致的小瓶。"一滴就能让最强壮的骑士倒下。用不用随你。"',
  '[{"text":"收下毒药（本场战斗首张牌伤害x2，但后续抽牌减少1张）","effect":{"type":"blessing_curse","blessing":"first_card_double","curse":"draw_minus_1"},"risk":"副作用","reward":"首伤翻倍"},
    {"text":"拒绝并用解毒知识（得25经验金币）","effect":{"type":"gold","value":25},"risk":"none","reward":"金币"},
    {"text":"喝下它测试效果（获得攻击+5持续2场战斗，但失去5血）","effect":{"type":"buff_with_cost","value":5,"cost":5},"risk":"失去5血","reward":"临时攻击+5"}]'
FROM shows s WHERE s.name LIKE '%Game of Thrones%' OR s.name LIKE '%权游%' OR s.name LIKE '%GOT%' LIMIT 1;

-- 节点6: 龙晶洞穴
INSERT IGNORE INTO expedition_events (show_id, act, title_cn, description_cn, choices)
SELECT s.id, 3, '龙晶矿洞', '你在山中发现一座废弃的龙晶矿。空气中弥漫着古老魔法的气息。',
  '[{"text":"开采龙晶（战斗后获得对Boss伤害+30%的遗物）","effect":{"type":"relic"}, "risk":"遭遇洞穴生物"},
    {"text":"建立临时营地（回满血，但花费15金币）","effect":{"type":"heal","value":999},"cost":15,"risk":"花费金币","reward":"回满血"},
    {"text":"探索深处（答2题，全对获得传说遗物，答错受10伤害）","effect":{"type":"gamble_quiz","questions":2,"reward":"legendary_relic","penalty":10},"risk":"高风险","reward":"传说遗物"}]'
FROM shows s WHERE s.name LIKE '%Game of Thrones%' OR s.name LIKE '%权游%' OR s.name LIKE '%GOT%' LIMIT 1;

-- 节点7: 无面者试炼
INSERT IGNORE INTO expedition_events (show_id, act, title_cn, description_cn, choices)
SELECT s.id, 3, '无面者试炼', '一扇黑色大门上刻着："凡人皆需侍奉。"一个声音从门后传来："想通过吗？证明你自己。"',
  '[{"text":"接受试炼（连续答对3题，获得传奇遗物）","effect":{"type":"challenge_quiz","questions":3,"reward":"legendary_relic"},"risk":"需要全对","reward":"传说遗物"},
    {"text":"献上金币（花30金币安全通过）","effect":{"type":"gold","value":-30},"risk":"花费","reward":"安全通过"},
    {"text":"强行开门（战斗，胜利得稀有卡牌，失败失去10血）","effect":{"type":"combat_reward","value":"rare_card"},"risk":"战斗风险","reward":"稀有卡牌"}]'
FROM shows s WHERE s.name LIKE '%Game of Thrones%' OR s.name LIKE '%权游%' OR s.name LIKE '%GOT%' LIMIT 1;

-- 节点8: 野人部落
INSERT IGNORE INTO expedition_events (show_id, act, title_cn, description_cn, choices)
SELECT s.id, 1, '野人部落', '一队野人正在长城外扎营。托蒙德看到你后咧嘴一笑。',
  '[{"text":"加入宴会（回10血，但醉酒下张牌费用+1）","effect":{"type":"buff_with_cost","value":10,"cost":1},"risk":"费用增加","reward":"回血"},
    {"text":"比试力量（答2题，全对获得攻击+2全体加成）","effect":{"type":"challenge_quiz","questions":2,"reward":"attack_buff_all"},"risk":"答题风险","reward":"攻击加成"},
    {"text":"悄悄离开","effect":{"type":"nothing"},"risk":"none","reward":"无"}]'
FROM shows s WHERE s.name LIKE '%Game of Thrones%' OR s.name LIKE '%权游%' OR s.name LIKE '%GOT%' LIMIT 1;

-- 节点9: 女巫的预言
INSERT IGNORE INTO expedition_events (show_id, act, title_cn, description_cn, choices)
SELECT s.id, 3, '森林女巫', '一位枯槁的女巫在树林中摆弄骨头。"来听听你的命运？免费——但真相有时比死亡更痛苦。"',
  '[{"text":"听预言（失去5血，但得知下一个Boss的弱点，伤害+30%）","effect":{"type":"blessing_curse","blessing":"boss_weakness","curse":"lose_5_hp"},"risk":"失去5血","reward":"Boss伤害+30%"},
    {"text":"用金币换取祝福（花25金币获得+10最大生命值）","effect":{"type":"buff_max_hp","value":10},"cost":25,"risk":"花费","reward":"+10最大生命"},
    {"text":"无视她","effect":{"type":"nothing"},"risk":"none","reward":"无"}]'
FROM shows s WHERE s.name LIKE '%Game of Thrones%' OR s.name LIKE '%权游%' OR s.name LIKE '%GOT%' LIMIT 1;


-- ========== 2. 唐顿事件 (Downton Abbey) ==========

-- 节点1: 仆人的秘密
INSERT IGNORE INTO expedition_events (show_id, act, title_cn, description_cn, choices)
SELECT s.id, 1, '楼下流言', '厨房里，黛西压低声音对你说："我听到老爷和夫人在争吵……好像和一笔秘密资金有关。"',
  '[{"text":"打探更多（答2题，全对得30金币）","effect":{"type":"challenge_quiz","questions":2,"reward":30},"risk":"答题失败惩罚","reward":"30金币"},
    {"text":"报告管家（获得声望，回10血）","effect":{"type":"heal","value":10},"risk":"none","reward":"回血"},
    {"text":"保守秘密（得15金币）","effect":{"type":"gold","value":15},"risk":"none","reward":"金币"}]'
FROM shows s WHERE s.name LIKE '%Downton Abbey%' OR s.name LIKE '%唐顿%' OR s.name LIKE '%DA%' LIMIT 1;

-- 节点2: 楼上邀请
INSERT IGNORE INTO expedition_events (show_id, act, title_cn, description_cn, choices)
SELECT s.id, 1, '伯爵的晚宴', '罗伯特·克劳利伯爵邀请你共进晚餐。"听说你见多识广，和我聊聊外面的世界吧。"',
  '[{"text":"展现学识（答3题，全对获得稀有卡牌）","effect":{"type":"study","sentences":3,"reward":"rare_card"},"risk":"需要全对","reward":"稀有卡牌"},
    {"text":"谈论时事（得20金币）","effect":{"type":"gold","value":20},"risk":"none","reward":"金币"},
    {"text":"请求资助（回15血，伯爵慷慨相助）","effect":{"type":"heal","value":15},"risk":"none","reward":"回血"}]'
FROM shows s WHERE s.name LIKE '%Downton Abbey%' OR s.name LIKE '%唐顿%' OR s.name LIKE '%DA%' LIMIT 1;

-- 节点3: 花园幽会
INSERT IGNORE INTO expedition_events (show_id, act, title_cn, description_cn, choices)
SELECT s.id, 2, '秘密信件', '安娜偷偷塞给你一封信。"这是给贝茨先生的——但路上可能有危险。"',
  '[{"text":"亲自送信（答2题，全对获得稀有遗物）","effect":{"type":"challenge_quiz","questions":2,"reward":"rare_relic"},"risk":"答题风险","reward":"稀有遗物"},
    {"text":"花钱雇人送（花10金币安全送达得20金币+经验）","effect":{"type":"gold","value":10},"cost":10,"risk":"花费","reward":"20金币"},
    {"text":"偷看信件后再送（得25金币，但可能被诅咒）","effect":{"type":"blessing_curse","blessing":"gold_25","curse":"curse_unknown"},"risk":"可能被诅咒","reward":"25金币"}]'
FROM shows s WHERE s.name LIKE '%Downton Abbey%' OR s.name LIKE '%唐顿%' OR s.name LIKE '%DA%' LIMIT 1;

-- 节点4: 马修的建议
INSERT IGNORE INTO expedition_events (show_id, act, title_cn, description_cn, choices)
SELECT s.id, 2, '马修的投资', '"我在想，也许你应该把一部分金币投资到我的项目里。"马修·克劳利热情地说。',
  '[{"text":"投资30金币（50%几率获得60金币，50%几率损失全部）","effect":{"type":"gamble","investment":30,"win":60},"risk":"高风险投资","reward":"可能双倍回报"},
    {"text":"谨慎地提供建议（得20金币）","effect":{"type":"gold","value":20},"risk":"none","reward":"金币"},
    {"text":"婉拒并请求帮助回血","effect":{"type":"heal","value":8},"risk":"none","reward":"回血"}]'
FROM shows s WHERE s.name LIKE '%Downton Abbey%' OR s.name LIKE '%唐顿%' OR s.name LIKE '%DA%' LIMIT 1;

-- 节点5: 帕特莫太太的厨房
INSERT IGNORE INTO expedition_events (show_id, act, title_cn, description_cn, choices)
SELECT s.id, 2, '帕特莫的秘方', '帕特莫太太神秘地招手。"我改良了司康饼的配方——吃了它，你会精力充沛。"',
  '[{"text":"品尝司康饼（回15血+下张牌免费）","effect":{"type":"blessing_curse","blessing":"buff_heal","curse":"none"},"risk":"none","reward":"回血+免费牌"},
    {"text":"学习厨艺（答2题，全对获得攻击+2全体加成）","effect":{"type":"challenge_quiz","questions":2,"reward":"attack_buff_all"},"risk":"答题风险","reward":"攻击加成"},
    {"text":"打包带走（获得一瓶小型生命药水）","effect":{"type":"potion","potion":"small_health"},"risk":"none","reward":"生命药水"}]'
FROM shows s WHERE s.name LIKE '%Downton Abbey%' OR s.name LIKE '%唐顿%' OR s.name LIKE '%DA%' LIMIT 1;

-- 节点6: 别墅拍卖会
INSERT IGNORE INTO expedition_events (show_id, act, title_cn, description_cn, choices)
SELECT s.id, 3, '庄园拍卖', '克劳利家族正在拍卖一些古董来筹集资金。你看到了一些有趣的东西。',
  '[{"text":"竞拍神秘古剑（答3题，全对获得传说卡牌）","effect":{"type":"challenge_quiz","questions":3,"reward":"legendary_card"},"risk":"需要全对","reward":"传说卡牌"},
    {"text":"购买旧铠甲（花20金币获得10点格挡遗物）","effect":{"type":"relic"},"cost":20,"risk":"花费金币","reward":"格挡遗物"},
    {"text":"淘便宜货（得25金币的小饰品转卖）","effect":{"type":"gold","value":25},"risk":"none","reward":"金币"}]'
FROM shows s WHERE s.name LIKE '%Downton Abbey%' OR s.name LIKE '%唐顿%' OR s.name LIKE '%DA%' LIMIT 1;

-- 节点7: 忠实的老管家
INSERT IGNORE INTO expedition_events (show_id, act, title_cn, description_cn, choices)
SELECT s.id, 3, '卡森的建议', '管家卡森严肃地看着你。"在这种困难时期，你需要一个清晰的头脑和忠诚的伙伴。"',
  '[{"text":"接受训练（答3道礼仪题，全对获得稀有遗物）","effect":{"type":"challenge_quiz","questions":3,"reward":"rare_relic"},"risk":"答题风险","reward":"稀有遗物"},
    {"text":"请求庇护（回20血，但花费20金币作为捐赠）","effect":{"type":"heal","value":20},"cost":20,"risk":"花费金币","reward":"回血"},
    {"text":"询问情报（获得下一场战斗敌人弱点信息）","effect":{"type":"blessing_curse","blessing":"enemy_weakness","curse":"none"},"risk":"none","reward":"战斗优势"}]'
FROM shows s WHERE s.name LIKE '%Downton Abbey%' OR s.name LIKE '%唐顿%' OR s.name LIKE '%DA%' LIMIT 1;

-- 节点8: 战地医院
INSERT IGNORE INTO expedition_events (show_id, act, title_cn, description_cn, choices)
SELECT s.id, 1, '战地医院', '唐顿庄园被临时征用为战地医院。护士长对你喊道："需要人手！"',
  '[{"text":"帮忙护理（答2题，全对回20血并获得药水）","effect":{"type":"challenge_quiz","questions":2,"reward":"potion_heal"},"risk":"答题风险","reward":"回血+药水"},
    {"text":"捐赠物资（花15金币，获得声望+回10血）","effect":{"type":"heal","value":10},"cost":15,"risk":"花费","reward":"回血"},
    {"text":"帮忙搬运伤员（获得10金币报酬）","effect":{"type":"gold","value":10},"risk":"none","reward":"金币"}]'
FROM shows s WHERE s.name LIKE '%Downton Abbey%' OR s.name LIKE '%唐顿%' OR s.name LIKE '%DA%' LIMIT 1;

-- 节点9: 玛丽小姐的挑战
INSERT IGNORE INTO expedition_events (show_id, act, title_cn, description_cn, choices)
SELECT s.id, 3, '玛丽小姐的赌约', '玛丽·克劳利微笑着走过来。"我打赌你不敢接受这个挑战。"',
  '[{"text":"接受挑战（答5题，全部正确获得传奇遗物，错任何一题失去15血）","effect":{"type":"gamble_quiz","questions":5,"reward":"legendary_relic","penalty":15},"risk":"极高风险","reward":"传说遗物"},
    {"text":"谦虚退让（得15金币）","effect":{"type":"gold","value":15},"risk":"none","reward":"金币"},
    {"text":"提议打牌（花10金币赌一把，胜率50%）","effect":{"type":"gamble","investment":10,"win":30},"risk":"赌博风险","reward":"可能30金币"}]'
FROM shows s WHERE s.name LIKE '%Downton Abbey%' OR s.name LIKE '%唐顿%' OR s.name LIKE '%DA%' LIMIT 1;
