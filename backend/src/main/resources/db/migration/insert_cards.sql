-- GOT 更多卡牌 (show_id=1)
INSERT INTO cards (show_id, name_cn, name_en, card_type, rarity, cost, attack, health, effect_json, faction, quote_text) VALUES
(1, '艾德·史塔克', 'Ned Stark', 'minion', 'legendary', 5, 5, 5, '{"keywords":["battlecry"],"description_cn":"召唤一个 2/2 的冰原狼","description_en":"Summon a 2/2 Direwolf"}', 'stark', ''),
(1, '琼恩·雪诺', 'Jon Snow', 'minion', 'epic', 4, 4, 4, '{"keywords":["battlecry"],"description_cn":"其他史塔克随从 +1/+1","description_en":"Other Stark minions +1/+1"}', 'stark', ''),
(1, '丹妮莉丝·坦格利安', 'Daenerys Targaryen', 'minion', 'legendary', 6, 4, 5, '{"keywords":["challenge","battlecry"],"description_cn":"召唤一条 3/3 的龙","description_en":"Summon a 3/3 Dragon"}', 'targaryen', 'Dracarys'),
(1, '提利昂·兰尼斯特', 'Tyrion Lannister', 'minion', 'epic', 3, 2, 3, '{"keywords":["battlecry"],"description_cn":"抽 2 张牌","description_en":"Draw 2 cards"}', 'lannister', ''),
(1, '小指头', 'Petyr Baelish', 'minion', 'epic', 2, 1, 1, '{"keywords":["battlecry"],"description_cn":"复制对手一张手牌","description_en":"Copy an opponent card"}', 'neutral', ''),
(1, '烈火燎原', 'Fire and Blood', 'spell', 'epic', 4, NULL, NULL, '{"keywords":["challenge"],"description_cn":"对敌方全体造成 3 点伤害","description_en":"Deal 3 damage to all enemies"}', 'targaryen', 'Fire and blood.'),
(1, '暗影匕首', 'Valyrian Steel Dagger', 'equipment', 'rare', 2, NULL, NULL, '{"keywords":[],"description_cn":"装备随从 +2 ATK，2 耐久","description_en":"Equip minion +2 ATK, 2 durability"}', 'neutral', ''),
(1, '龙晶匕首', 'Dragonglass Dagger', 'equipment', 'epic', 3, NULL, NULL, '{"keywords":[],"description_cn":"装备随从 +3 ATK，3 耐久，对亡语随从伤害翻倍","description_en":"Equip minion +3 ATK, 3 durability, double damage vs Deathrattle"}', 'neutral', ''),
(1, '血色婚礼', 'The Red Wedding', 'spell', 'legendary', 8, NULL, NULL, '{"keywords":["challenge"],"description_cn":"消灭所有敌方随从","description_en":"Destroy all enemy minions"}', 'neutral', 'The Lannisters send their regards.'),
(1, '史塔克家族', 'House Stark', 'location', 'rare', 2, NULL, NULL, '{"keywords":[],"description_cn":"每回合，你的第一个史塔克随从费用 -1","description_en":"Your first Stark minion each turn costs 1 less"}', 'stark', 'Winter is coming.');

-- DA 更多卡牌 (show_id=2)
INSERT INTO cards (show_id, name_cn, name_en, card_type, rarity, cost, attack, health, effect_json, faction, quote_text) VALUES
(2, '罗伯特·卡劳利', 'Robert Crawley', 'minion', 'legendary', 5, 4, 6, '{"keywords":["taunt","battlecry"],"description_cn":"恢复所有友方角色 2 点生命","description_en":"Restore 2 Health to all friendly characters"}', 'crawley', ''),
(2, '珂拉·卡劳利', 'Cora Crawley', 'minion', 'epic', 4, 3, 5, '{"keywords":["battlecry"],"description_cn":"恢复 4 点生命","description_en":"Restore 4 Health"}', 'crawley', ''),
(2, '玛丽·卡劳利', 'Mary Crawley', 'minion', 'epic', 3, 3, 3, '{"keywords":[],"description_cn":"每当你打出卡劳利随从，抽 1 张牌","description_en":"Whenever you play a Crawley minion, draw 1 card"}', 'crawley', ''),
(2, '管家卡森', 'Mr. Carson', 'minion', 'rare', 2, 2, 3, '{"keywords":["taunt"],"description_cn":"","description_en":""}', 'crawley', ''),
(2, '精致茶具', 'Fine Tea Set', 'equipment', 'rare', 1, NULL, NULL, '{"keywords":[],"description_cn":"装备随从 +1 HP，回合结束恢复 1 HP","description_en":"Equip minion +1 HP, restore 1 HP at end of turn"}', 'neutral', ''),
(2, '唐顿的舞会', 'Downton Ball', 'spell', 'rare', 3, NULL, NULL, '{"keywords":[],"description_cn":"所有友方随从 +1/+1","description_en":"All friendly minions +1/+1"}', 'crawley', ''),
(2, '女仆安娜', 'Anna the Maid', 'minion', 'common', 1, 2, 1, '{"keywords":["battlecry"],"description_cn":"获得 1 点临时法力","description_en":"Gain 1 temporary mana"}', 'crawley', ''),
(2, '庄园晚宴', 'Estate Dinner', 'spell', 'rare', 4, NULL, NULL, '{"keywords":[],"description_cn":"抽 3 张牌","description_en":"Draw 3 cards"}', 'crawley', '');
