-- Add story fields to expeditions table
ALTER TABLE expeditions
  ADD COLUMN story_intro TEXT DEFAULT NULL AFTER map_data,
  ADD COLUMN node_stories JSON DEFAULT NULL AFTER story_intro;

-- Create expedition_stories table for episode story templates
CREATE TABLE expedition_stories (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  show_id BIGINT NOT NULL,
  episode_season INT NOT NULL,
  episode_number INT NOT NULL,
  story_intro TEXT NOT NULL,
  node_stories JSON NOT NULL,
  boss_name VARCHAR(255) DEFAULT NULL,
  boss_story TEXT DEFAULT NULL,
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
  UNIQUE KEY uk_show_episode (show_id, episode_season, episode_number)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Insert GOT S01E01 story data
INSERT INTO expedition_stories (show_id, episode_season, episode_number, story_intro, node_stories, boss_name, boss_story)
VALUES (
  1,
  1,
  1,
  '你是在绝境长城服役的一名守夜人新兵。北境的传说从不错——这个冬天，真的不一样。今天清晨，边境巡逻队带回了一个可怕的消息……长城之外，有什么东西醒了。',
  '[
    {"title":"长城巡逻","story":"与巡逻队一起骑马穿越鬼影森林。树影中，有什么东西在移动。突然，一声尖叫划破寂静——一队异鬼侦察兵从雪雾中出现！"},
    {"title":"临冬城来使","story":"史塔克家族的传令官抵达长城。总司令莫尔蒙召集守夜人到大厅。劳勃国王正在南下，临冬城动荡不安。","choices":["服从调遣（获得1张临时卡牌）","主动请缨（下回合+2攻击）"]},
    {"title":"守夜人誓词","story":"在长城之巅的塔楼上，你和战友们围坐在火堆旁。老兵讲述着长城下古老的秘密。寒冷的夜风中，你感到了一丝温暖。"},
    {"title":"森林伏击","story":"野人斥候小队在鹿鸣谷埋伏。他们在风雪中如同幽灵般出现，你必须迅速做出反应。"},
    {"title":"物资补给","story":"一个来自南方的补车队抵达了黑城堡。他们带来了武器、药品和来自君临的消息。"},
    {"title":"异鬼现身","story":"夜更深了。巡逻队在树林中发现了被肢解的尸体。突然，草丛中一双蓝色的眼睛睁开——蓝色的光芒撕裂了夜空！"}
  ]',
  '异鬼侦察队长',
  '它比你见过的任何敌人都高大。冰霜覆盖的铠甲上泛着幽蓝的光。它的剑——由寒冰铸成——在月光下闪烁。'
);
