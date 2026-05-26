-- 成就表
CREATE TABLE IF NOT EXISTS achievements (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  category VARCHAR(30) NOT NULL COMMENT 'collection/battle/expedition/guild/streak',
  key_name VARCHAR(50) NOT NULL UNIQUE COMMENT '成就唯一标识',
  name_cn VARCHAR(100) NOT NULL,
  description_cn VARCHAR(200) NOT NULL,
  icon VARCHAR(50) DEFAULT '' COMMENT '🎴⚔️🗡️🏰🔥等',
  rarity VARCHAR(20) NOT NULL DEFAULT 'common' COMMENT 'common/rare/epic/legendary',
  condition_type VARCHAR(50) NOT NULL COMMENT 'collect_cards/win_battles/expedition_clear/login_days等',
  condition_value INT NOT NULL COMMENT '达成条件值（如收集50张卡）',
  reward_stardust INT NOT NULL DEFAULT 0,
  reward_card_id BIGINT COMMENT '奖励卡牌(null=星尘奖励)',
  sort_order INT NOT NULL DEFAULT 0
);

CREATE TABLE IF NOT EXISTS user_achievements (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  user_id BIGINT NOT NULL,
  achievement_id BIGINT NOT NULL,
  progress INT NOT NULL DEFAULT 0,
  target INT NOT NULL,
  unlocked BOOLEAN NOT NULL DEFAULT FALSE,
  unlocked_at DATETIME,
  UNIQUE KEY uk_user_achievement (user_id, achievement_id)
);

-- 赛季排名表
CREATE TABLE IF NOT EXISTS season_rankings (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  user_id BIGINT NOT NULL,
  season_number INT NOT NULL,
  pvp_score INT NOT NULL DEFAULT 0 COMMENT 'PVP奖杯数',
  expedition_score INT NOT NULL DEFAULT 0 COMMENT '远征评分',
  guild_score INT NOT NULL DEFAULT 0 COMMENT '公会贡献评分',
  total_score INT NOT NULL DEFAULT 0 COMMENT '综合评分',
  title VARCHAR(50) COMMENT '称号(月之王者/月之大师等)',
  rank_position INT COMMENT '排名',
  FOREIGN KEY (user_id) REFERENCES users(id),
  INDEX idx_season_score (season_number, total_score)
);

-- 公会联赛
CREATE TABLE IF NOT EXISTS guild_league_seasons (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  season_number INT NOT NULL,
  start_date DATETIME NOT NULL,
  end_date DATETIME NOT NULL,
  status VARCHAR(20) DEFAULT 'upcoming' COMMENT 'upcoming/active/ended',
  INDEX idx_season (season_number)
);

ALTER TABLE guilds ADD COLUMN league_score INT NOT NULL DEFAULT 0;
ALTER TABLE guilds ADD COLUMN league_rank INT DEFAULT 0;

-- 初始成就数据
INSERT IGNORE INTO achievements (category, key_name, name_cn, description_cn, icon, rarity, condition_type, condition_value, reward_stardust, sort_order) VALUES
-- 收集类
('collection', 'collect_10', '卡牌新手', '收集10张卡牌', '🎴', 'common', 'collect_cards', 10, 50, 1),
('collection', 'collect_30', '卡牌爱好者', '收集30张卡牌', '🎴', 'common', 'collect_cards', 30, 100, 2),
('collection', 'collect_50', '卡牌收藏家', '收集50张卡牌', '🎴', 'rare', 'collect_cards', 50, 200, 3),
('collection', 'collect_70', '全图鉴', '收集全部70张卡牌', '🎴', 'epic', 'collect_cards', 70, 500, 4),
('collection', 'golden_5', '金光闪闪', '拥有5张金卡', '✨', 'rare', 'golden_cards', 5, 300, 5),
('collection', 'golden_10', '金色传说', '拥有10张金卡', '✨', 'epic', 'golden_cards', 10, 600, 6),
('collection', 'legendary_5', '传说集结', '收集5张传说卡牌', '👑', 'rare', 'legendary_cards', 5, 300, 7),
('collection', 'got_full', '凛冬之主', '收集全部GOT卡牌', '🏔️', 'epic', 'show_complete', 1, 500, 8),
('collection', 'da_full', '唐顿伯爵', '收集全部DA卡牌', '🏡', 'epic', 'show_complete', 2, 500, 9),

-- 对战类
('battle', 'win_1', '首胜', '赢得第1场对战', '⚔️', 'common', 'win_battles', 1, 50, 10),
('battle', 'win_10', '十胜勇士', '赢得10场对战', '⚔️', 'common', 'win_battles', 10, 150, 11),
('battle', 'win_50', '百战精英', '赢得50场对战', '⚔️', 'rare', 'win_battles', 50, 400, 12),
('battle', 'win_100', '无双战神', '赢得100场对战', '⚔️', 'epic', 'win_battles', 100, 800, 13),
('battle', 'streak_3', '三连胜', '连续赢得3场对战', '🔥', 'common', 'win_streak', 3, 100, 14),
('battle', 'streak_5', '五连胜', '连续赢得5场对战', '🔥', 'rare', 'win_streak', 5, 250, 15),
('battle', 'streak_10', '十连胜', '连续赢得10场对战', '🔥', 'epic', 'win_streak', 10, 600, 16),
('battle', 'rank_gold', '黄金段位', '达到黄金段位', '🏆', 'rare', 'rank_reach', 3, 300, 17),
('battle', 'rank_diamond', '钻石段位', '达到钻石段位', '🏆', 'epic', 'rank_reach', 5, 500, 18),
('battle', 'rank_legend', '传说段位', '达到传说段位', '🏆', 'legendary', 'rank_reach', 6, 1000, 19),

-- 远征类
('expedition', 'expedition_1', '初次远征', '完成第1次远征', '🗡️', 'common', 'expedition_clear', 1, 100, 20),
('expedition', 'expedition_10', '远征老兵', '完成10次远征', '🗡️', 'rare', 'expedition_clear', 10, 500, 21),
('expedition', 'expedition_50', '远征之王', '完成50次远征', '🗡️', 'epic', 'expedition_clear', 50, 1500, 22),
('expedition', 'boss_kill_20', '弑君者', '击败20个Boss', '🗡️', 'epic', 'boss_kills', 20, 800, 23),
('expedition', 'relic_7', '遗物收藏家', '集齐全部7种遗物', '🪙', 'epic', 'relic_collect', 7, 500, 24),

-- 公会类
('guild', 'join_guild', '加入公会', '加入一个公会', '🏰', 'common', 'join_guild', 1, 100, 25),
('guild', 'guild_war_win', '领地胜利', '在领地战中获胜', '🏰', 'rare', 'guild_war_win', 1, 300, 26),

-- 日常类
('streak', 'login_7', '一周守护', '连续登录7天', '📅', 'rare', 'login_streak', 7, 200, 27),
('streak', 'login_30', '月度常客', '连续登录30天', '📅', 'epic', 'login_streak', 30, 800, 28),
('streak', 'login_365', '年度铁粉', '连续登录365天', '📅', 'legendary', 'login_streak', 365, 5000, 29),
('streak', 'daily_7', '七日挑战', '连续7天完成御前挑战', '⚡', 'rare', 'daily_challenge_streak', 7, 300, 30);
