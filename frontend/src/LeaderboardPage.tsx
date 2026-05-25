import React, { useState, useEffect } from 'react';
import { gameApi } from './api/client';

const TABS = [
  { key: 'today', label: '今日' },
  { key: 'week', label: '本周' },
  { key: 'month', label: '本月' },
  { key: 'friends', label: '狼群' },
];

export default function LeaderboardPage({ user, onNavigate }: { user: any, onNavigate: (target: string, data?: any) => void }) {
  const [activeTab, setActiveTab] = useState('today');
  const [entries, setEntries] = useState<any[]>([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    setLoading(true);
    gameApi.getLeaderboard(activeTab).then(r => {
      if (r.code === 200) setEntries(r.data || []);
      setLoading(false);
    });
  }, [activeTab]);

  const getRankDisplay = (rank: number) => {
    if (rank === 1) return <span style={{ fontSize: 20 }}>👑</span>;
    if (rank === 2) return <span style={{ fontSize: 18 }}>🥈</span>;
    if (rank === 3) return <span style={{ fontSize: 18 }}>🥉</span>;
    return <span style={{ color: '#64748b', fontWeight: 600, fontSize: 14 }}>{rank}</span>;
  };

  return (
    <div className="leaderboard-page">
      {/* 顶部标题 */}
      <div style={{ padding: '16px 16px 8px' }}>
        <div style={{ color: '#f1f5f9', fontSize: 18, fontWeight: 700, marginBottom: 4 }}>🏆 七国铁王座排行榜</div>
        <div style={{ color: '#94a3b8', fontSize: 12 }}>角逐英语之王</div>
      </div>

      {/* Tab 导航 */}
      <div style={{ display: 'flex', gap: 0, padding: '8px 16px', borderBottom: '1px solid rgba(255,255,255,0.06)' }}>
        {TABS.map(tab => (
          <button
            key={tab.key}
            onClick={() => setActiveTab(tab.key)}
            style={{
              flex: 1, padding: '8px 0', border: 'none', cursor: 'pointer',
              background: 'transparent', borderRadius: 0,
              color: activeTab === tab.key ? '#14b8a6' : '#64748b',
              fontWeight: activeTab === tab.key ? 600 : 400,
              fontSize: 13,
              borderBottom: activeTab === tab.key ? '2px solid #14b8a6' : '2px solid transparent',
              transition: 'all 0.2s'
            }}
          >
            {tab.label}
          </button>
        ))}
      </div>

      {/* 排行榜内容 */}
      <div style={{ padding: '8px 0' }}>
        {loading ? (
          <div style={{ textAlign: 'center', padding: 40, color: '#64748b', fontSize: 14 }}>加载中...</div>
        ) : entries.length === 0 ? (
          <div style={{ textAlign: 'center', padding: 40, color: '#64748b', fontSize: 14 }}>
            🏜️ 暂无上榜者
          </div>
        ) : (
          entries.map((entry: any) => (
            <div key={entry.userId} style={{
              display: 'flex', alignItems: 'center', gap: 12,
              padding: '12px 16px',
              background: entry.userId === user?.id ? 'rgba(20,184,166,0.06)' : 'transparent',
              borderLeft: entry.userId === user?.id ? '3px solid #14b8a6' : '3px solid transparent'
            }}>
              {/* 排名 */}
              <div style={{ width: 32, textAlign: 'center' }}>
                {getRankDisplay(entry.rank)}
              </div>

              {/* 头像 */}
              <div style={{
                width: 36, height: 36, borderRadius: '50%',
                background: entry.avatar ? 'transparent' : 'linear-gradient(135deg, #14b8a6, #0d9488)',
                display: 'flex', alignItems: 'center', justifyContent: 'center',
                fontSize: 14, fontWeight: 600, color: '#fff', flexShrink: 0,
                overflow: 'hidden'
              }}>
                {entry.avatar ? (
                  <img src={entry.avatar} alt="" style={{ width: '100%', height: '100%', objectFit: 'cover' }} />
                ) : (
                  entry.nickname?.charAt(0)?.toUpperCase() || '?'
                )}
              </div>

              {/* 昵称 + 封号 */}
              <div style={{ flex: 1, minWidth: 0 }}>
                <div style={{ color: '#e2e8f0', fontSize: 14, fontWeight: 500, overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>
                  {entry.nickname || '匿名用户'}
                </div>
                <div style={{ color: '#14b8a6', fontSize: 11, marginTop: 1 }}>
                  {entry.titleCn} · ⚡{entry.prestige}
                </div>
              </div>

              {/* 战绩 */}
              <div style={{ textAlign: 'right', flexShrink: 0 }}>
                <div style={{ color: '#fbbf24', fontSize: 14, fontWeight: 600 }}>{entry.correctCount}</div>
                <div style={{ color: '#64748b', fontSize: 10 }}>正确</div>
              </div>
            </div>
          ))
        )}
      </div>
    </div>
  );
}
