-- 为 decks 表添加 is_active 字段
ALTER TABLE decks
  ADD COLUMN is_active BOOLEAN DEFAULT FALSE AFTER card_ids;
