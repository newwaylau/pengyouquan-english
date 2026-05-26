-- 金卡配置：哪些卡有金卡版本
ALTER TABLE cards ADD COLUMN has_golden BOOLEAN NOT NULL DEFAULT FALSE COMMENT '是否有金卡版本';

-- user_cards 加 golden 标记
ALTER TABLE user_cards ADD COLUMN is_golden BOOLEAN NOT NULL DEFAULT FALSE COMMENT '是否是金卡';

-- 套装配置表
CREATE TABLE IF NOT EXISTS equipment_sets (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  set_key VARCHAR(50) NOT NULL UNIQUE COMMENT 'stark/targaryen/downton',
  name_cn VARCHAR(50) NOT NULL,
  two_piece_effect_cn VARCHAR(200) COMMENT '2件套效果',
  five_piece_effect_cn VARCHAR(200) COMMENT '5件套效果',
  two_piece_effect_json JSON COMMENT '{\"type\":\"mana_discount\",\"value\":1,\"condition\":\"ice_cards\"}',
  five_piece_effect_json JSON COMMENT '{\"type\":\"first_card_free\"}'
);

INSERT IGNORE INTO equipment_sets (set_key, name_cn, two_piece_effect_cn, five_piece_effect_cn, two_piece_effect_json, five_piece_effect_json) VALUES
('stark', '史塔克', '冰系卡费用-1', '每回合首张卡免费', '{"type":"mana_discount","value":1,"condition":"ice_cards"}', '{"type":"first_card_free"}'),
('targaryen', '坦格利安', '传说卡费用-1', '答对大招+3伤害', '{"type":"legendary_cost_reduction","value":1}', '{"type":"ult_damage_bonus","value":3}'),
('downton', '唐顿庄园', '休息节点回血+20%', '事件节点额外选项', '{"type":"rest_heal_bonus","pct":20}', '{"type":"event_extra_option"}');

-- 装备重铸相关
ALTER TABLE user_equipment ADD COLUMN level INT NOT NULL DEFAULT 1 COMMENT '装备等级';
ALTER TABLE user_equipment ADD COLUMN bonus_stats JSON COMMENT '附加属性（重铸可变的属性）';
ALTER TABLE user_equipment ADD COLUMN reroll_count INT NOT NULL DEFAULT 0 COMMENT '已重铸次数';

-- 给已有史诗和传说卡牌开启金卡版本
UPDATE cards SET has_golden = TRUE WHERE rarity IN ('epic', 'legendary');
