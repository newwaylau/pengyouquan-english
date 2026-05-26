import React, { useState, useEffect } from 'react';
import { gameApi } from './api/client';

export default function ArenaPage({ user, onNavigate, refreshKey }: { user: any, onNavigate: (target: string, data?: any) => void, refreshKey?: number }) {
  const [prestige, setPrestige] = useState<any>(null);
  const [history, setHistory] = useState<any[]>([]);
  const [loading, setLoading] = useState(true);
  const [rewards, setRewards] = useState<any[]>([]);
  const [tiers, setTiers] = useState<any[]>([]);
  const [showRewards, setShowRewards] = useState(false);
  const [showTiers, setShowTiers] = useState(false);

  useEffect(() => {
    gameApi.getPrestige().then(r => {
      if (r.code === 200) setPrestige(r.data);
      setLoading(false);
    });
    gameApi.getHistory().then(r => {
      if (r.code === 200) setHistory(r.data || []);
    });
    gameApi.getStreakRewards().then(r => {
      if (r.code === 200) setRewards(r.data || []);
    });
    gameApi.getRankTiers().then(r => {
      if (r.code === 200) setTiers(r.data || []);
    });
  }, [refreshKey]);

  const claimableRewards = rewards.filter(r => !r.claimed && r.consecutiveDays >= r.daysRequired).length;

  const handleClaim = async (rewardId: number) => {
    const r = await gameApi.claimStreakReward(rewardId);
    if (r.code === 200) {
      const updated = rewards.map(rw => rw.id === rewardId ? { ...rw, claimed: true } : rw);
      setRewards(updated);
    }
  };

  if (loading) return <div className="page-loading">加载中...</div>;

  return (
    <div className="arena-page">
      {/* 封号卡牌 */}
      <div className="arena-rank-card" style={{
        background: 'linear-gradient(135deg, var(--card), var(--bg))',
        borderRadius: 'var(--radius-lg)', padding: 24, margin: 16,
        border: '1px solid var(--teal-glow)',
        boxShadow: '0 4px 20px rgba(20, 184, 166, 0.15)'
      }}>
        <div className="arena-rank-badge" style={{
          width: 64, height: 64, borderRadius: '50%',
          background: 'linear-gradient(135deg, var(--teal), var(--teal-dark))',
          display: 'flex', alignItems: 'center', justifyContent: 'center',
          fontSize: 28, margin: '0 auto 12px',
          boxShadow: '0 0 20px var(--teal-glow)'
        }}>
          👑
        </div>
        <div style={{ textAlign: 'center', color: 'var(--teal)', fontSize: 14, marginBottom: 4 }}>
          {prestige?.titleCn} · {prestige?.titleEn}
        </div>
        <div style={{ textAlign: 'center', color: 'var(--text-secondary)', fontSize: 12, marginBottom: 16 }}>
          等级 {prestige?.rankTier}/10
        </div>

        {/* 威望进度条 */}
        <div style={{ marginBottom: 8 }}>
          <div style={{ display: 'flex', justifyContent: 'space-between', fontSize: 12, color: 'var(--text-secondary)', marginBottom: 4 }}>
            <span>⚡ {prestige?.prestige} 威望</span>
            <span>下一级 {prestige?.nextRequiredPrestige}</span>
          </div>
          <div style={{
            height: 8, background: 'rgba(255,255,255,0.08)', borderRadius: 'var(--radius-sm)',
            overflow: 'hidden'
          }}>
            <div style={{
              width: `${Math.min(100, prestige?.progressPercent || 0)}%`,
              height: '100%',
              background: 'linear-gradient(90deg, var(--teal), var(--teal-light))',
              borderRadius: 'var(--radius-sm)',
              transition: 'width 0.5s ease'
            }} />
          </div>
        </div>

        {/* 三数据 */}
        <div style={{ display: 'flex', justifyContent: 'space-around', marginTop: 16 }}>
          <div style={{ textAlign: 'center', position: 'relative' }}>
            <div style={{ color: '#fbbf24', fontSize: 18, fontWeight: 600 }}>🔥</div>
            <div style={{ color: 'var(--text-primary)', fontSize: 16, fontWeight: 600 }}>{prestige?.consecutiveDays || 0}</div>
            <div style={{ color: 'var(--text-secondary)', fontSize: 11 }}>统治天数</div>
            {claimableRewards > 0 && <div className="reward-dot" />}
          </div>
          <div style={{ textAlign: 'center', cursor: 'pointer' }} onClick={() => onNavigate?.('daily-challenge')}>
            <div style={{ color: 'var(--teal)', fontSize: 18, fontWeight: 600 }}>⚔️</div>
            <div style={{ color: 'var(--text-primary)', fontSize: 16, fontWeight: 600 }}>今日</div>
            <div style={{ color: 'var(--text-secondary)', fontSize: 11 }}>御前挑战</div>
          </div>
          <div style={{ textAlign: 'center' }}>
            <div style={{ color: '#a78bfa', fontSize: 18, fontWeight: 600 }}>🏆</div>
            <div style={{ color: 'var(--text-primary)', fontSize: 16, fontWeight: 600 }}>#{'?'}</div>
            <div style={{ color: 'var(--text-secondary)', fontSize: 11 }}>七国排名</div>
          </div>
        </div>
      </div>

      {/* 快捷入口 */}
      <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 12, padding: '0 16px' }}>
        <button className="arena-quick-btn" onClick={() => onNavigate?.('practice')}
          style={{ background: 'linear-gradient(135deg, #1e3a3a, #0f2a2a)', border: '1px solid rgba(20,184,166,0.2)', borderRadius: 'var(--radius-md)', padding: 20, textAlign: 'left' }}>
          <div style={{ fontSize: 24, marginBottom: 8 }}>📖</div>
          <div style={{ color: 'var(--teal)', fontWeight: 600, fontSize: 14 }}>继续听写</div>
          <div style={{ color: 'var(--text-secondary)', fontSize: 11, marginTop: 2 }}>续写征战</div>
        </button>
        <button className="arena-quick-btn" onClick={() => onNavigate?.('daily-challenge')}
          style={{ background: 'linear-gradient(135deg, #2a1e3a, #1a0f2a)', border: '1px solid rgba(167,139,250,0.2)', borderRadius: 'var(--radius-md)', padding: 20, textAlign: 'left' }}>
          <div style={{ fontSize: 24, marginBottom: 8 }}>🗡️</div>
          <div style={{ color: '#a78bfa', fontWeight: 600, fontSize: 14 }}>御前挑战</div>
          <div style={{ color: 'var(--text-secondary)', fontSize: 11, marginTop: 2 }}>每日10题</div>
        </button>
      </div>

      {/* 封号之路 */}
      <div style={{ padding: '0 16px', marginTop: 16 }}>
        <button onClick={() => setShowTiers(!showTiers)} style={{
          width: '100%', padding: '12px 16px', border: 'none', borderRadius: 'var(--radius-md)',
          background: 'var(--card)', color: 'var(--text-secondary)', cursor: 'pointer',
          display: 'flex', justifyContent: 'space-between', alignItems: 'center'
        }}>
          <span>📊 封号之路</span>
          <span>{showTiers ? '▲' : '▼'}</span>
        </button>

        {showTiers && (
          <div style={{ marginTop: 8 }}>
            {tiers.map((t: any) => (
              <div key={t.tier} style={{
                display: 'flex', alignItems: 'center', gap: 12,
                padding: '10px 16px',
                background: t.current ? 'rgba(20,184,166,0.08)' : 'transparent',
                borderLeft: t.current ? '3px solid var(--teal)' : '3px solid transparent',
                borderRadius: 'var(--radius-sm)',
                marginBottom: 4
              }}>
                <div style={{ width: 24, textAlign: 'center', color: t.current ? 'var(--teal)' : 'var(--text-tertiary)', fontWeight: 600, fontSize: 12 }}>
                  {t.tier}
                </div>
                <div style={{ flex: 1 }}>
                  <div style={{ color: t.current ? 'var(--teal)' : 'var(--text-primary)', fontWeight: t.current ? 600 : 400, fontSize: 14 }}>
                    {t.titleCn}
                  </div>
                  <div style={{ color: 'var(--text-tertiary)', fontSize: 11 }}>{t.titleEn}</div>
                </div>
                <div style={{ color: 'var(--text-tertiary)', fontSize: 11 }}>
                  {t.requiredPrestige}威望
                </div>
                {t.current && <div style={{ color: 'var(--teal)', fontSize: 11, fontWeight: 600 }}>◀ 当前</div>}
              </div>
            ))}
          </div>
        )}
      </div>

      {/* 统治奖励进度 */}
      <div style={{ padding: '0 16px', marginTop: 16 }}>
        <button onClick={() => setShowRewards(!showRewards)} style={{
          width: '100%', padding: '12px 16px', border: 'none', borderRadius: 'var(--radius-md)',
          background: 'var(--card)', color: 'var(--text-secondary)', cursor: 'pointer',
          display: 'flex', justifyContent: 'space-between', alignItems: 'center'
        }}>
          <span>🎯 统治奖励 {claimableRewards > 0 && <span style={{ color: 'var(--teal)', fontSize: 12 }}>({claimableRewards}可领)</span>}</span>
          <span>{showRewards ? '▲' : '▼'}</span>
        </button>

        {showRewards && (
          <div style={{ marginTop: 12 }}>
            {rewards.length === 0 ? (
              <div style={{ color: 'var(--text-tertiary)', fontSize: 12, textAlign: 'center', padding: 20 }}>
                暂无奖励配置
              </div>
            ) : (
              <div style={{ display: 'flex', gap: 8 }}>
                {rewards.map((r: any) => (
                  <div key={r.id} style={{
                    opacity: r.consecutiveDays >= r.daysRequired ? 1 : 0.4,
                    border: `1px solid ${r.consecutiveDays >= r.daysRequired ? 'var(--teal)' : 'var(--border)'}`,
                    borderRadius: 'var(--radius-md)',
                    background: 'var(--card)', padding: 12, textAlign: 'center', flex: 1,
                    cursor: !r.claimed && r.consecutiveDays >= r.daysRequired ? 'pointer' : 'default'
                  }} onClick={() => !r.claimed && r.consecutiveDays >= r.daysRequired && handleClaim(r.id)}>
                    <div style={{ fontSize: 24 }}>{r.rewardIcon}</div>
                    <div style={{ fontSize: 11, color: 'var(--text-secondary)', marginTop: 4 }}>{r.rewardName}</div>
                    <div style={{ fontSize: 10, color: r.consecutiveDays >= r.daysRequired ? 'var(--teal)' : 'var(--text-tertiary)' }}>
                      {r.consecutiveDays >= r.daysRequired ? (r.claimed ? '已领取' : '可领取') : `${r.daysRequired}天`}
                    </div>
                  </div>
                ))}
              </div>
            )}
          </div>
        )}
      </div>

      {/* 近期战报 */}
      <div style={{ padding: '0 16px', marginTop: 16 }}>
        <div style={{ color: 'var(--text-secondary)', fontSize: 13, marginBottom: 8 }}>📜 近期战报</div>
        {history.length > 0 ? (
          <div>
            {history.map((h: any, i: number) => {
              const pct = h.totalQuestions > 0 ? Math.round(h.correctCount / h.totalQuestions * 100) : 0;
              return (
                <div key={i} style={{
                  background: 'linear-gradient(135deg, var(--card), var(--bg))',
                  borderRadius: 'var(--radius-md)', padding: '12px 16px', marginBottom: 8,
                  border: '1px solid rgba(20,184,166,0.15)',
                  display: 'flex', justifyContent: 'space-between', alignItems: 'center'
                }}>
                  <div>
                    <div style={{ color: 'var(--text-primary)', fontSize: 13, fontWeight: 500 }}>
                      {h.date || '--'}
                    </div>
                    <div style={{ color: 'var(--text-secondary)', fontSize: 12, marginTop: 2 }}>
                      ⚔️ {h.correctCount}/{h.totalQuestions} · {pct}%
                    </div>
                  </div>
                  <div style={{ color: '#fbbf24', fontSize: 14, fontWeight: 600 }}>
                    +{h.prestigeEarned}
                  </div>
                </div>
              );
            })}
          </div>
        ) : (
          <div style={{ color: 'var(--text-tertiary)', fontSize: 12, textAlign: 'center', padding: 20 }}>
            完成御前挑战后将在此显示战绩
          </div>
        )}
      </div>
    </div>
  );
}
