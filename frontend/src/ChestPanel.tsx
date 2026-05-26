import React, { useState, useEffect } from 'react';
import { cardApi } from './api/cardClient';
import PackOpeningModal from './PackOpeningModal';

const CHEST_ICONS: Record<string, string> = {
  bronze: '🥉',
  silver: '🥈',
  gold: '🥇',
};

const CHEST_LABELS: Record<string, string> = {
  bronze: '青铜宝箱',
  silver: '白银宝箱',
  gold: '黄金宝箱',
};

const CHEST_REQUIRED: Record<string, number> = {
  bronze: 10,
  silver: 25,
  gold: 50,
};

interface ChestData {
  id: number;
  userId: number;
  chestType: string;
  status: string;
  unlockProgress: number;
  unlockRequired: number;
  source: string;
}

interface ChestPanelProps {
  user: any;
}

export default function ChestPanel({ user }: ChestPanelProps) {
  const [chests, setChests] = useState<ChestData[]>([]);
  const [loading, setLoading] = useState(false);
  const [selectedChest, setSelectedChest] = useState<ChestData | null>(null);
  const [claiming, setClaiming] = useState(false);
  const [packResult, setPackResult] = useState<any>(null);
  const [toastMsg, setToastMsg] = useState('');

  const loadChests = async () => {
    if (!user) return;
    setLoading(true);
    const res = await cardApi.getChests();
    if (res.code === 200) setChests(res.data || []);
    setLoading(false);
  };

  useEffect(() => {
    loadChests();
    // poll every 30s
    const interval = setInterval(loadChests, 30000);
    return () => clearInterval(interval);
  }, [user]);

  const handleClaim = async (chestId: number) => {
    setClaiming(true);
    const res = await cardApi.claimChest(chestId);
    setClaiming(false);
    if (res.code === 200) {
      const data = res.data;
      if (data.cards && data.cards.length > 0) {
        setPackResult({ cards: data.cards, packType: data.chest?.chestType || 'bronze' });
      }
      setSelectedChest(null);
      loadChests();
    } else {
      setToastMsg(res.message || '领取失败');
      setTimeout(() => setToastMsg(''), 3000);
    }
  };

  // Sort: ready first, then unlocking, then locked, then claimed at bottom
  const statusOrder: Record<string, number> = { ready: 0, unlocking: 1, locked: 2, claimed: 3 };
  const sortedChests = [...chests].sort((a, b) => {
    const orderDiff = (statusOrder[a.status] ?? 9) - (statusOrder[b.status] ?? 9);
    if (orderDiff !== 0) return orderDiff;
    return new Date(b.createdAt || 0).getTime() - new Date(a.createdAt || 0).getTime();
  });

  // Show max 4 chests (slots)
  const displayChests = sortedChests.slice(0, 4);

  const getStatusLabel = (chest: ChestData) => {
    switch (chest.status) {
      case 'locked': return '🔒 未解锁';
      case 'unlocking': return `⏳ ${chest.unlockProgress}/${chest.unlockRequired}`;
      case 'ready': return '🎁 可领取';
      case 'claimed': return '✅ 已领取';
      default: return chest.status;
    }
  };

  const getProgressPercent = (chest: ChestData) => {
    if (chest.unlockRequired <= 0) return 0;
    return Math.min(100, Math.round((chest.unlockProgress / chest.unlockRequired) * 100));
  };

  const getSlotLabel = (index: number) => {
    return index === 3 ? '赛季' : `槽位 ${index + 1}`;
  };

  if (!user) return null;

  return (
    <>
      <div className="chest-panel" style={{
        display: 'flex', gap: 10, padding: '12px 16px',
        background: 'rgba(0,0,0,0.2)', borderRadius: 'var(--radius-lg)',
        margin: '0 0 12px', overflow: 'auto',
      }}>
        <style>{`
          .chest-slot {
            display: flex; flex-direction: column; align-items: center;
            gap: 6px; padding: 10px 8px; border-radius: var(--radius-sm);
            background: var(--card); border: 1px solid var(--border);
            cursor: pointer; min-width: 100px; flex-shrink: 0;
            transition: border-color 0.2s, transform 0.15s;
          }
          .chest-slot:hover { border-color: var(--teal); transform: translateY(-2px); }
          .chest-slot.ready { border-color: var(--teal); box-shadow: 0 0 12px var(--teal-glow); }
          .chest-slot.claimed { opacity: 0.5; }
          .chest-slot-icon { font-size: 28px; }
          .chest-slot-label { font-size: 11px; color: var(--text-secondary); }
          .chest-slot-type { font-size: 13px; color: var(--text); font-weight: 600; }
          .chest-slot-status {
            font-size: 11px; padding: 2px 8px; border-radius: 10px;
            background: rgba(255,255,255,0.06); color: var(--text-secondary);
          }
          .chest-slot-status.ready-status { background: var(--teal); color: #fff; }
          .chest-progress-bar {
            width: 80px; height: 5px; background: rgba(255,255,255,0.08);
            border-radius: 3px; overflow: hidden;
          }
          .chest-progress-fill {
            height: 100%; border-radius: 3px; transition: width 0.5s ease;
            background: linear-gradient(90deg, var(--teal), var(--teal-light));
          }
          .chest-progress-fill.ready-fill { background: linear-gradient(90deg, #fbbf24, #f59e0b); }
          .chest-empty-slot {
            display: flex; flex-direction: column; align-items: center;
            justify-content: center; gap: 4px; min-width: 100px;
            padding: 10px 8px; border-radius: var(--radius-sm);
            border: 1px dashed var(--border); background: transparent;
            opacity: 0.4; flex-shrink: 0;
          }
          .chest-empty-icon { font-size: 20px; }
          .chest-empty-text { font-size: 11px; color: var(--text-secondary); }
        `}</style>

        {displayChests.length === 0 && (
          <>
            {[0, 1, 2, 3].map(i => (
              <div key={i} className="chest-empty-slot">
                <div className="chest-empty-icon">📭</div>
                <div className="chest-empty-text">{getSlotLabel(i)}</div>
              </div>
            ))}
          </>
        )}

        {displayChests.map((chest, idx) => {
          const isReady = chest.status === 'ready';
          return (
            <div
              key={chest.id}
              className={`chest-slot ${chest.status}`}
              onClick={() => { if (chest.status !== 'claimed') setSelectedChest(chest); }}
            >
              <div className="chest-slot-icon">{CHEST_ICONS[chest.chestType] || '📦'}</div>
              <div className="chest-slot-type">{CHEST_LABELS[chest.chestType] || chest.chestType}</div>
              <div className="chest-progress-bar">
                <div className={`chest-progress-fill ${isReady ? 'ready-fill' : ''}`}
                  style={{ width: `${getProgressPercent(chest)}%` }} />
              </div>
              <div className={`chest-slot-status ${isReady ? 'ready-status' : ''}`}>
                {getStatusLabel(chest)}
              </div>
            </div>
          );
        })}
      </div>

      {/* Chest detail modal */}
      {selectedChest && (
        <div className="card-detail-overlay" onClick={() => setSelectedChest(null)}>
          <div className="card-detail-modal" onClick={e => e.stopPropagation()} style={{ maxWidth: 360 }}>
            <button className="cd-close" onClick={() => setSelectedChest(null)}>✕</button>
            <div style={{ textAlign: 'center', marginBottom: 16 }}>
              <div style={{ fontSize: 48, marginBottom: 8 }}>
                {CHEST_ICONS[selectedChest.chestType] || '📦'}
              </div>
              <div style={{ color: 'var(--text-primary)', fontSize: 18, fontWeight: 700 }}>
                {CHEST_LABELS[selectedChest.chestType] || selectedChest.chestType}
              </div>
              <div style={{ color: 'var(--text-secondary)', fontSize: 13, marginTop: 4 }}>
                来源: {selectedChest.source === 'pvp_battle' ? 'PvP对战' : selectedChest.source}
              </div>
            </div>

            {selectedChest.status === 'locked' && (
              <div style={{ textAlign: 'center', color: 'var(--text-secondary)', fontSize: 14, marginBottom: 16 }}>
                🔒 宝箱已锁定，继续练习听写以解锁
                <div style={{ marginTop: 8, fontSize: 13 }}>
                  还需要练习 {selectedChest.unlockRequired} 句
                </div>
              </div>
            )}

            {selectedChest.status === 'unlocking' && (
              <div style={{ textAlign: 'center', marginBottom: 16 }}>
                <div style={{ marginBottom: 8 }}>
                  <div style={{ display: 'flex', justifyContent: 'space-between', fontSize: 13, color: 'var(--text-secondary)', marginBottom: 4 }}>
                    <span>解锁进度</span>
                    <span>{selectedChest.unlockProgress}/{selectedChest.unlockRequired}</span>
                  </div>
                  <div style={{
                    height: 10, background: 'rgba(255,255,255,0.08)', borderRadius: 'var(--radius-sm)',
                    overflow: 'hidden'
                  }}>
                    <div style={{
                      width: `${getProgressPercent(selectedChest)}%`, height: '100%',
                      background: 'linear-gradient(90deg, var(--teal), var(--teal-light))',
                      borderRadius: 'var(--radius-sm)', transition: 'width 0.5s ease'
                    }} />
                  </div>
                </div>
                <div style={{ color: 'var(--text-secondary)', fontSize: 13 }}>
                  ⏳ 继续练习听写以解锁
                </div>
              </div>
            )}

            {selectedChest.status === 'ready' && (
              <div style={{ textAlign: 'center', marginBottom: 16 }}>
                <div style={{ color: 'var(--teal)', fontSize: 14, marginBottom: 12 }}>
                  🎉 宝箱已就绪！
                </div>
                <button
                  className="btn btn-primary"
                  style={{ width: '100%', padding: '10px' }}
                  onClick={() => handleClaim(selectedChest.id)}
                  disabled={claiming}
                >
                  {claiming ? '领取中...' : '🎁 领取奖励'}
                </button>
              </div>
            )}

            {selectedChest.status === 'claimed' && (
              <div style={{ textAlign: 'center', color: 'var(--text-secondary)', fontSize: 14, marginBottom: 16 }}>
                ✅ 已领取
              </div>
            )}
          </div>
        </div>
      )}

      {/* Pack opening modal */}
      {packResult && (
        <PackOpeningModal
          cards={packResult.cards}
          packType={packResult.packType}
          onClose={() => setPackResult(null)}
        />
      )}

      {toastMsg && (
        <div style={{
          position: 'fixed', bottom: 80, left: '50%', transform: 'translateX(-50%)',
          background: 'var(--danger)', color: '#fff', padding: '8px 20px',
          borderRadius: 'var(--radius-sm)', zIndex: 9999,
          fontSize: 14
        }}>
          {toastMsg}
        </div>
      )}
    </>
  );
}
