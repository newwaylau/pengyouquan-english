import React, { useState, useEffect } from 'react';
import { gameApi } from './api/client';

export default function ArenaPage({ user, onNavigate, refreshKey }: { user: any, onNavigate: (target: string, data?: any) => void, refreshKey?: number }) {
  const [prestige, setPrestige] = useState<any>(null);
  const [history, setHistory] = useState<any[]>([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    gameApi.getPrestige().then(r => {
      if (r.code === 200) setPrestige(r.data);
      setLoading(false);
    });
    gameApi.getHistory().then(r => {
      if (r.code === 200) setHistory(r.data || []);
    });
  }, [refreshKey]);

  if (loading) return <div className="page-loading">加载中...</div>;

  return (
    <div className="arena-page">
      {/* 封号卡牌 */}
      <div className="arena-rank-card" style={{
        background: 'linear-gradient(135deg, #1a1a2e, #16213e)',
        borderRadius: 16, padding: 24, margin: 16,
        border: '1px solid rgba(20, 184, 166, 0.3)',
        boxShadow: '0 4px 20px rgba(20, 184, 166, 0.15)'
      }}>
        <div className="arena-rank-badge" style={{
          width: 64, height: 64, borderRadius: '50%',
          background: 'linear-gradient(135deg, #14b8a6, #0d9488)',
          display: 'flex', alignItems: 'center', justifyContent: 'center',
          fontSize: 28, margin: '0 auto 12px',
          boxShadow: '0 0 20px rgba(20, 184, 166, 0.4)'
        }}>
          👑
        </div>
        <div style={{ textAlign: 'center', color: '#14b8a6', fontSize: 14, marginBottom: 4 }}>
          {prestige?.titleCn} · {prestige?.titleEn}
        </div>
        <div style={{ textAlign: 'center', color: '#94a3b8', fontSize: 12, marginBottom: 16 }}>
          等级 {prestige?.rankTier}/10
        </div>

        {/* 威望进度条 */}
        <div style={{ marginBottom: 8 }}>
          <div style={{ display: 'flex', justifyContent: 'space-between', fontSize: 12, color: '#94a3b8', marginBottom: 4 }}>
            <span>⚡ {prestige?.prestige} 威望</span>
            <span>下一级 {prestige?.nextRequiredPrestige}</span>
          </div>
          <div style={{
            height: 8, background: 'rgba(255,255,255,0.08)', borderRadius: 4,
            overflow: 'hidden'
          }}>
            <div style={{
              width: `${Math.min(100, prestige?.progressPercent || 0)}%`,
              height: '100%',
              background: 'linear-gradient(90deg, #14b8a6, #2dd4bf)',
              borderRadius: 4,
              transition: 'width 0.5s ease'
            }} />
          </div>
        </div>

        {/* 三数据 */}
        <div style={{ display: 'flex', justifyContent: 'space-around', marginTop: 16 }}>
          <div style={{ textAlign: 'center' }}>
            <div style={{ color: '#fbbf24', fontSize: 18, fontWeight: 600 }}>🔥</div>
            <div style={{ color: '#e2e8f0', fontSize: 16, fontWeight: 600 }}>{prestige?.consecutiveDays || 0}</div>
            <div style={{ color: '#94a3b8', fontSize: 11 }}>统治天数</div>
          </div>
          <div style={{ textAlign: 'center' }}>
            <div style={{ color: '#14b8a6', fontSize: 18, fontWeight: 600 }}>⚔️</div>
            <div style={{ color: '#e2e8f0', fontSize: 16, fontWeight: 600 }}>今日</div>
            <div style={{ color: '#94a3b8', fontSize: 11 }}>御前挑战</div>
          </div>
          <div style={{ textAlign: 'center' }}>
            <div style={{ color: '#a78bfa', fontSize: 18, fontWeight: 600 }}>🏆</div>
            <div style={{ color: '#e2e8f0', fontSize: 16, fontWeight: 600 }}>#{'?'}</div>
            <div style={{ color: '#94a3b8', fontSize: 11 }}>七国排名</div>
          </div>
        </div>
      </div>

      {/* 快捷入口 */}
      <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 12, padding: '0 16px' }}>
        <button className="arena-quick-btn" onClick={() => onNavigate?.('practice')}
          style={{ background: 'linear-gradient(135deg, #1e3a3a, #0f2a2a)', border: '1px solid rgba(20,184,166,0.2)', borderRadius: 12, padding: 20, textAlign: 'left' }}>
          <div style={{ fontSize: 24, marginBottom: 8 }}>📖</div>
          <div style={{ color: '#14b8a6', fontWeight: 600, fontSize: 14 }}>继续听写</div>
          <div style={{ color: '#94a3b8', fontSize: 11, marginTop: 2 }}>续写征战</div>
        </button>
        <button className="arena-quick-btn" onClick={() => onNavigate?.('daily-challenge')}
          style={{ background: 'linear-gradient(135deg, #2a1e3a, #1a0f2a)', border: '1px solid rgba(167,139,250,0.2)', borderRadius: 12, padding: 20, textAlign: 'left' }}>
          <div style={{ fontSize: 24, marginBottom: 8 }}>🗡️</div>
          <div style={{ color: '#a78bfa', fontWeight: 600, fontSize: 14 }}>御前挑战</div>
          <div style={{ color: '#94a3b8', fontSize: 11, marginTop: 2 }}>每日10题</div>
        </button>
      </div>

      {/* 近期战报 */}
      <div style={{ padding: '0 16px', marginTop: 16 }}>
        <div style={{ color: '#94a3b8', fontSize: 13, marginBottom: 8 }}>📜 近期战报</div>
        {history.length > 0 ? (
          <div>
            {history.map((h: any, i: number) => {
              const pct = h.totalQuestions > 0 ? Math.round(h.correctCount / h.totalQuestions * 100) : 0;
              return (
                <div key={i} style={{
                  background: 'linear-gradient(135deg, #1a1a2e, #16213e)',
                  borderRadius: 12, padding: '12px 16px', marginBottom: 8,
                  border: '1px solid rgba(20,184,166,0.15)',
                  display: 'flex', justifyContent: 'space-between', alignItems: 'center'
                }}>
                  <div>
                    <div style={{ color: '#e2e8f0', fontSize: 13, fontWeight: 500 }}>
                      {h.date || '--'}
                    </div>
                    <div style={{ color: '#94a3b8', fontSize: 12, marginTop: 2 }}>
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
          <div style={{ color: '#64748b', fontSize: 12, textAlign: 'center', padding: 20 }}>
            完成御前挑战后将在此显示战绩
          </div>
        )}
      </div>
    </div>
  );
}
