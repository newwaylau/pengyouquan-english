-- 公会系统

-- 公会
CREATE TABLE IF NOT EXISTS guilds (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  name VARCHAR(50) NOT NULL UNIQUE COMMENT '公会名',
  leader_id BIGINT NOT NULL COMMENT '会长',
  description VARCHAR(200) DEFAULT '' COMMENT '公会宣言',
  member_count INT NOT NULL DEFAULT 1,
  max_members INT NOT NULL DEFAULT 30,
  total_cards_collected INT NOT NULL DEFAULT 0 COMMENT '公会总卡牌收集数',
  weekly_score INT NOT NULL DEFAULT 0 COMMENT '本周领地战积分',
  rank_points INT NOT NULL DEFAULT 0 COMMENT '公会排名积分',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  FOREIGN KEY (leader_id) REFERENCES users(id)
);

-- 公会成员
CREATE TABLE IF NOT EXISTS guild_members (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  guild_id BIGINT NOT NULL,
  user_id BIGINT NOT NULL UNIQUE,
  role VARCHAR(20) NOT NULL DEFAULT 'member' COMMENT 'leader/officer/member',
  weekly_correct INT NOT NULL DEFAULT 0 COMMENT '本周答对数',
  weekly_score INT NOT NULL DEFAULT 0 COMMENT '本周贡献分',
  joined_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  FOREIGN KEY (guild_id) REFERENCES guilds(id),
  FOREIGN KEY (user_id) REFERENCES users(id)
);

-- 公会宝库里程碑
CREATE TABLE IF NOT EXISTS guild_treasures (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  guild_id BIGINT NOT NULL,
  milestone INT NOT NULL COMMENT '里程碑（总收集数达到500/1000/2000等）',
  chest_type VARCHAR(20) NOT NULL COMMENT 'bronze/silver/gold',
  claimed_count INT NOT NULL DEFAULT 0 COMMENT '已领取人数',
  max_claims INT NOT NULL DEFAULT 30 COMMENT '可领取人数',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  FOREIGN KEY (guild_id) REFERENCES guilds(id)
);

-- 宝库领取记录
CREATE TABLE IF NOT EXISTS guild_treasure_claims (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  treasure_id BIGINT NOT NULL,
  user_id BIGINT NOT NULL,
  claimed_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  FOREIGN KEY (treasure_id) REFERENCES guild_treasures(id),
  UNIQUE KEY uk_claim (treasure_id, user_id)
);
