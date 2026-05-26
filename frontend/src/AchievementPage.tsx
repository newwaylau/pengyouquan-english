import React, { useState, useEffect } from 'react';
import { cardApi } from './api/cardClient';

const RARITY_COLORS: Record<string, string> = {
  legendary: '#ff8c00',
  epic: '#a335ee',
  rare: '#0070dd',
  common: '#9d9d9d',
};

const CATEGORY_LABELS: Record<string, string> = {
  collection: '🎴 收集',
  battle: '⚔️ 对战',
  expedition: '🗡️ 远征',
  guild: '🏰 公会',
  streak: '📅 日常',
};

export default function AchievementPage() {
  const [data, setData] = useState<any>(null);
  const [loading, setLoading] = useState(true);
  const [activeCategory, setActiveCategory] = useState<string | null>(null);
  const [toastMsg, setToastMsg] = useState('');
  const [claimedIds, setClaimedIds] = useState<Set<number>>(new Set());

  useEffect(() => {
    loadAchievements();
  }, []);

  const loadAchievements = async () => {
    setLoading(true);
    const res = await cardApi.getAchievements();
    if (res.code === 200) setData(res.data);
    setLoading(false);
  };

  const handleClaim = async (achievementId: number) => {
    const res = await cardApi.claimAchievement(achievementId);
    if (res.code === 200) {
      setToastMsg(`🎉 领取成功！获得 ${res.data.stardustGained} 星尘`);
      setClaimedIds(prev => new Set(prev).add(achievementId));
      setTimeout(() => setToastMsg(''), 3000);
      loadAchievements();
    } else {
      setToastMsg(res.message || '领取失败');
      setTimeout(() => setToastMsg(''), 3000);
    }
  };

  if (loading) return <div className="page-loading">加载中...</div>;
  if (!data) return null;

  const achievements: any[] = data.achievements || [];
  const { totalCount, unlockedCount } = data;

  const categories = [...new Set(achievements.map(a => a.category))];
  const filtered = activeCategory
    ? achievements.filter(a => a.category === activeCategory)
    : achievements;

  return (
    <div className="achievement-page">
      <div className="achievement-header">
        <h2 className="achievement-title">🏆 成就</h2>
        <div className="achievement-progress">
          <div className="achievement-progress-text">
            {unlockedCount} / {totalCount} 已解锁
          </div>
          <div className="achievement-progress-bar-bg">
            <div className="achievement-progress-bar"
              style={{ width: `${totalCount > 0 ? (unlockedCount / totalCount) * 100 : 0}%` }}
            />
          </div>
        </div>
      </div>

      {/* 分类标签 */}
      <div className="achievement-categories">
        <button
          className={`achievement-cat-btn ${!activeCategory ? 'active' : ''}`}
          onClick={() => setActiveCategory(null)}
        >
          全部
        </button>
        {categories.map(cat => (
          <button
            key={cat}
            className={`achievement-cat-btn ${activeCategory === cat ? 'active' : ''}`}
            onClick={() => setActiveCategory(activeCategory === cat ? null : cat)}
          >
            {CATEGORY_LABELS[cat] || cat}
          </button>
        ))}
      </div>

      {/* 成就列表 */}
      <div className="achievement-grid">
        {filtered.map(a => {
          const isUnlocked = a.unlocked;
          const rarityColor = RARITY_COLORS[a.rarity] || '#9d9d9d';
          const isClaimed = claimedIds.has(a.id) || (a.progress > a.target);

          return (
            <div
              key={a.id}
              className={`achievement-card ${isUnlocked ? 'unlocked' : 'locked'}`}
              style={{ borderColor: isUnlocked ? rarityColor : 'var(--border)' }}
            >
              <div className="achievement-icon">{a.icon || '🏆'}</div>
              <div className="achievement-info">
                <div className="achievement-name">{a.nameCn}</div>
                <div className="achievement-desc">{a.descriptionCn}</div>
                <div className="achievement-progress-row">
                  <div className="achievement-progress-bar-sm-bg">
                    <div className="achievement-progress-bar-sm"
                      style={{
                        width: `${Math.min(100, (a.progress / a.target) * 100)}%`,
                        background: isUnlocked ? rarityColor : 'var(--text-secondary)',
                      }}
                    />
                  </div>
                  <span className="achievement-progress-num">{Math.min(a.progress, a.target)}/{a.target}</span>
                </div>
                {isUnlocked && !isClaimed && (
                  <button
                    className="achievement-claim-btn"
                    onClick={() => handleClaim(a.id)}
                  >
                    领取 {a.rewardStardust} ✨
                  </button>
                )}
                {isClaimed && (
                  <div className="achievement-claimed">✅ 已领取</div>
                )}
              </div>
            </div>
          );
        })}
      </div>

      {filtered.length === 0 && (
        <div className="cc-empty">
          <div className="cc-empty-text">暂无成就</div>
        </div>
      )}

      {toastMsg && (
        <div className="equipment-toast">{toastMsg}</div>
      )}
    </div>
  );
}
