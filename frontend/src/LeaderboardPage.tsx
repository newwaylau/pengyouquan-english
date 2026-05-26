import React, { useState, useEffect } from 'react';
import { gameApi } from './api/client';

const TABS = [
  { key: 'today', label: '今日' },
  { key: 'week', label: '本周' },
  { key: 'month', label: '本月' },
  { key: 'friends', label: '狼群' },
];

const PAGE_TABS = [
  { key: 'leaderboard', label: '🏆 排行榜' },
  { key: 'clan', label: '👥 封臣' },
];

export default function LeaderboardPage({ user, onNavigate }: { user: any, onNavigate: (target: string, data?: any) => void }) {
  const [activePageTab, setActivePageTab] = useState('leaderboard');
  const [activeTab, setActiveTab] = useState('today');
  const [entries, setEntries] = useState<any[]>([]);
  const [loading, setLoading] = useState(true);
  const [inviteCode, setInviteCode] = useState('');
  const [clanMembers, setClanMembers] = useState<any[]>([]);
  const [recruitCode, setRecruitCode] = useState('');
  const [recruitMsg, setRecruitMsg] = useState('');
  const [recruitError, setRecruitError] = useState('');
  const [copied, setCopied] = useState(false);

  useEffect(() => {
    if (activePageTab === 'leaderboard') {
      setLoading(true);
      gameApi.getLeaderboard(activeTab).then(r => {
        if (r.code === 200) setEntries(r.data || []);
        setLoading(false);
      });
    } else {
      setLoading(true);
      gameApi.getInviteCode().then(r => {
        if (r.code === 200) setInviteCode(r.data?.inviteCode || '');
      });
      gameApi.getClan().then(r => {
        if (r.code === 200) setClanMembers(r.data || []);
        setLoading(false);
      });
    }
  }, [activePageTab, activeTab]);

  const handleCopy = () => {
    navigator.clipboard.writeText(inviteCode).then(() => {
      setCopied(true);
      setTimeout(() => setCopied(false), 2000);
    });
  };

  const handleRecruit = async () => {
    if (!recruitCode.trim()) return;
    setRecruitMsg('');
    setRecruitError('');
    try {
      const r = await gameApi.recruit(recruitCode.trim());
      if (r.code === 200) {
        setRecruitMsg(`成功招募 ${r.data?.recruited || '未知'}!`);
        setRecruitCode('');
        gameApi.getClan().then(r2 => {
          if (r2.code === 200) setClanMembers(r2.data || []);
        });
      } else {
        setRecruitError(r.message || '招募失败');
      }
    } catch {
      setRecruitError('招募失败');
    }
  };

  const getRankDisplay = (rank: number) => {
    if (rank === 1) return <span style={{ fontSize: 20 }}>👑</span>;
    if (rank === 2) return <span style={{ fontSize: 18 }}>🥈</span>;
    if (rank === 3) return <span style={{ fontSize: 18 }}>🥉</span>;
    return <span style={{ color: 'var(--text-tertiary)', fontWeight: 600, fontSize: 14 }}>{rank}</span>;
  };

  return (
    <div className="leaderboard-page">
      {/* 页面级 Tab 导航 */}
      <div style={{ display: 'flex', gap: 0, padding: '16px 16px 0', borderBottom: '1px solid rgba(255,255,255,0.06)' }}>
        {PAGE_TABS.map(tab => (
          <button
            key={tab.key}
            onClick={() => setActivePageTab(tab.key)}
            style={{
              flex: 1, padding: '10px 0', border: 'none', cursor: 'pointer',
              background: 'transparent', borderRadius: 0,
              color: activePageTab === tab.key ? 'var(--teal)' : 'var(--text-tertiary)',
              fontWeight: activePageTab === tab.key ? 600 : 400,
              fontSize: 14,
              borderBottom: activePageTab === tab.key ? '2px solid var(--teal)' : '2px solid transparent',
              transition: 'all 0.2s'
            }}
          >
            {tab.label}
          </button>
        ))}
      </div>

      {activePageTab === 'leaderboard' ? (
        <>
          {/* 排行榜 */}
          <div style={{ padding: '16px 16px 8px' }}>
            <div style={{ color: 'var(--text-primary)', fontSize: 18, fontWeight: 700, marginBottom: 4 }}>🏆 七国铁王座排行榜</div>
            <div style={{ color: 'var(--text-secondary)', fontSize: 12 }}>角逐英语之王</div>
          </div>

          {/* 周期 Tab */}
          <div style={{ display: 'flex', gap: 0, padding: '8px 16px', borderBottom: '1px solid rgba(255,255,255,0.06)' }}>
            {TABS.map(tab => (
              <button
                key={tab.key}
                onClick={() => setActiveTab(tab.key)}
                style={{
                  flex: 1, padding: '8px 0', border: 'none', cursor: 'pointer',
                  background: 'transparent', borderRadius: 0,
                  color: activeTab === tab.key ? 'var(--teal)' : 'var(--text-tertiary)',
                  fontWeight: activeTab === tab.key ? 600 : 400,
                  fontSize: 13,
                  borderBottom: activeTab === tab.key ? '2px solid var(--teal)' : '2px solid transparent',
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
              <div style={{ textAlign: 'center', padding: 40, color: 'var(--text-tertiary)', fontSize: 14 }}>加载中...</div>
            ) : entries.length === 0 ? (
              <div style={{ textAlign: 'center', padding: 40, color: 'var(--text-tertiary)', fontSize: 14 }}>
                🏜️ 暂无上榜者
              </div>
            ) : (
              entries.map((entry: any) => (
                <div key={entry.userId} style={{
                  display: 'flex', alignItems: 'center', gap: 12,
                  padding: '12px 16px',
                  background: entry.userId === user?.id ? 'rgba(20,184,166,0.06)' : 'transparent',
                  borderLeft: entry.userId === user?.id ? '3px solid var(--teal)' : '3px solid transparent'
                }}>
                  {/* 排名 */}
                  <div style={{ width: 32, textAlign: 'center' }}>
                    {getRankDisplay(entry.rank)}
                  </div>

                  {/* 头像 */}
                  <div style={{
                    width: 36, height: 36, borderRadius: '50%',
                    background: entry.avatar ? 'transparent' : 'linear-gradient(135deg, var(--teal), var(--teal-dark))',
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
                    <div style={{ color: 'var(--text-primary)', fontSize: 14, fontWeight: 500, overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>
                      {entry.nickname || '匿名用户'}
                    </div>
                    <div style={{ color: 'var(--teal)', fontSize: 11, marginTop: 1 }}>
                      {entry.titleCn} · ⚡{entry.prestige}
                    </div>
                  </div>

                  {/* 战绩 */}
                  <div style={{ textAlign: 'right', flexShrink: 0 }}>
                    <div style={{ color: '#fbbf24', fontSize: 14, fontWeight: 600 }}>{entry.correctCount}</div>
                    <div style={{ color: 'var(--text-tertiary)', fontSize: 10 }}>正确</div>
                  </div>
                </div>
              ))
            )}
          </div>
        </>
      ) : (
        /* 封臣 Tab */
        <div style={{ padding: '16px' }}>
          {/* 邀请码 */}
          <div style={{
            background: 'var(--card)', borderRadius: 'var(--radius-md)', padding: 16,
            border: '1px solid var(--border)', marginBottom: 16
          }}>
            <div style={{ color: 'var(--text-secondary)', fontSize: 12, marginBottom: 8 }}>📜 我的邀请码</div>
            <div style={{ display: 'flex', gap: 8, alignItems: 'center' }}>
              <div style={{
                flex: 1, padding: '10px 14px', background: 'rgba(255,255,255,0.04)',
                borderRadius: 'var(--radius-sm)', color: 'var(--teal)', fontWeight: 700,
                fontSize: 16, fontFamily: 'monospace', letterSpacing: 1
              }}>
                {inviteCode || '加载中...'}
              </div>
              <button onClick={handleCopy} style={{
                padding: '10px 16px', border: '1px solid var(--teal)', borderRadius: 'var(--radius-sm)',
                background: 'transparent', color: 'var(--teal)', cursor: 'pointer',
                fontWeight: 600, fontSize: 13, whiteSpace: 'nowrap'
              }}>
                {copied ? '✓ 已复制' : '复制'}
              </button>
            </div>
          </div>

          {/* 招募入口 */}
          <div style={{
            background: 'var(--card)', borderRadius: 'var(--radius-md)', padding: 16,
            border: '1px solid var(--border)', marginBottom: 16
          }}>
            <div style={{ color: 'var(--text-secondary)', fontSize: 12, marginBottom: 8 }}>🤝 招募封臣</div>
            <div style={{ display: 'flex', gap: 8 }}>
              <input
                type="text"
                value={recruitCode}
                onChange={e => setRecruitCode(e.target.value)}
                onKeyDown={e => e.key === 'Enter' && handleRecruit()}
                placeholder="输入对方的邀请码"
                style={{
                  flex: 1, padding: '10px 14px',
                  background: 'rgba(255,255,255,0.04)',
                  border: '1px solid rgba(255,255,255,0.1)',
                  borderRadius: 'var(--radius-sm)', outline: 'none',
                  color: 'var(--text-primary)', fontSize: 14,
                  fontFamily: 'monospace'
                }}
              />
              <button onClick={handleRecruit} style={{
                padding: '10px 16px', border: 'none', borderRadius: 'var(--radius-sm)',
                background: 'var(--teal)', color: '#fff', cursor: 'pointer',
                fontWeight: 600, fontSize: 13, whiteSpace: 'nowrap'
              }}>
                招募
              </button>
            </div>
            {recruitMsg && <div style={{ color: 'var(--teal)', fontSize: 12, marginTop: 8 }}>{recruitMsg}</div>}
            {recruitError && <div style={{ color: 'var(--danger)', fontSize: 12, marginTop: 8 }}>{recruitError}</div>}
          </div>

          {/* 封臣列表 */}
          <div>
            <div style={{ color: 'var(--text-secondary)', fontSize: 12, marginBottom: 8 }}>
              👥 已招募封臣 ({clanMembers.length})
            </div>
            {loading ? (
              <div style={{ textAlign: 'center', padding: 40, color: 'var(--text-tertiary)', fontSize: 14 }}>加载中...</div>
            ) : clanMembers.length === 0 ? (
              <div style={{
                background: 'var(--card)', borderRadius: 'var(--radius-md)', padding: 24,
                textAlign: 'center', border: '1px solid var(--border)'
              }}>
                <div style={{ fontSize: 32, marginBottom: 8 }}>🐺</div>
                <div style={{ color: 'var(--text-secondary)', fontSize: 14 }}>招募好友组成狼群，一起学习</div>
                <div style={{ color: 'var(--text-tertiary)', fontSize: 12, marginTop: 4 }}>分享邀请码即可招募封臣</div>
              </div>
            ) : (
              clanMembers.map((m: any) => (
                <div key={m.userId} style={{
                  display: 'flex', alignItems: 'center', gap: 12,
                  padding: '12px 16px',
                  background: 'var(--card)',
                  borderRadius: 'var(--radius-sm)',
                  marginBottom: 6
                }}>
                  <div style={{
                    width: 36, height: 36, borderRadius: '50%',
                    background: 'linear-gradient(135deg, var(--teal), var(--teal-dark))',
                    display: 'flex', alignItems: 'center', justifyContent: 'center',
                    fontSize: 14, fontWeight: 600, color: '#fff', flexShrink: 0
                  }}>
                    {m.nickname?.charAt(0)?.toUpperCase() || '?'}
                  </div>
                  <div style={{ flex: 1, minWidth: 0 }}>
                    <div style={{ color: 'var(--text-primary)', fontSize: 14, fontWeight: 500 }}>
                      {m.nickname || '匿名用户'}
                    </div>
                    <div style={{ color: 'var(--teal)', fontSize: 11, marginTop: 1 }}>
                      {m.titleCn} · ⚡{m.prestige}
                    </div>
                  </div>
                  <div style={{ color: 'var(--text-tertiary)', fontSize: 11, textAlign: 'right' }}>
                    <div>加入</div>
                    <div>{m.joinDate || '--'}</div>
                  </div>
                </div>
              ))
            )}
          </div>
        </div>
      )}
    </div>
  );
}
