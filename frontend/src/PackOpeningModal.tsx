import React, { useState, useEffect } from 'react';

const RARITY_COLORS: Record<string, string> = {
  legendary: '#ff8c00',
  epic: '#a335ee',
  rare: '#0070dd',
  common: '#9d9d9d',
};

const RARITY_CN: Record<string, string> = {
  legendary: '传说', epic: '史诗', rare: '稀有', common: '普通',
};

const RARITY_BG: Record<string, string> = {
  legendary: 'linear-gradient(135deg, #ff8c00, #ff4500)',
  epic: 'linear-gradient(135deg, #a335ee, #6b21a8)',
  rare: 'linear-gradient(135deg, #0070dd, #1e40af)',
  common: 'linear-gradient(135deg, #9d9d9d, #64748b)',
};

interface CardData {
  id: number;
  nameCn: string;
  nameEn: string;
  rarity: string;
  cardType: string;
  cost: number;
  attack?: number;
  health?: number;
  quantity: number;
  effectJson?: string;
  quoteText?: string;
  showName?: string;
}

interface PackOpeningProps {
  cards: CardData[];
  onClose: () => void;
  packType?: string;
}

export default function PackOpeningModal({ cards, onClose, packType }: PackOpeningProps) {
  const [phase, setPhase] = useState<'chest' | 'opening' | 'cards'>('chest');
  const [revealedCards, setRevealedCards] = useState<number[]>([]);
  const [selectedCard, setSelectedCard] = useState<CardData | null>(null);
  const [shakeScreen, setShakeScreen] = useState(false);

  const packLabel = packType === 'gold' ? '黄金宝箱'
    : packType === 'silver' ? '白银宝箱'
    : packType === 'bronze' ? '青铜宝箱'
    : packType === 'golden' ? '黄金卡包'
    : packType === 'silver' ? '白银卡包'
    : packType === 'bronze' ? '青铜卡包'
    : '卡包';

  // Phase 1: chest rotating
  useEffect(() => {
    const t1 = setTimeout(() => setPhase('opening'), 1200);
    return () => clearTimeout(t1);
  }, []);

  // Phase 2: opening -> cards revealed one by one
  useEffect(() => {
    if (phase !== 'opening') return;
    const hasLegendary = cards.some(c => c.rarity === 'legendary');
    if (hasLegendary) {
      setShakeScreen(true);
      setTimeout(() => setShakeScreen(false), 600);
    }
    const t2 = setTimeout(() => {
      setPhase('cards');
      // reveal cards one by one
      cards.forEach((_, i) => {
        setTimeout(() => {
          setRevealedCards(prev => [...prev, i]);
        }, i * 400);
      });
    }, 1200);
    return () => clearTimeout(t2);
  }, [phase, cards]);

  const getPackIcon = () => {
    if (packType === 'gold' || packType === 'golden') return '🌟';
    if (packType === 'silver') return '🥈';
    if (packType === 'bronze') return '🥉';
    return '📦';
  };

  const getGlowColor = (rarity: string) => {
    switch (rarity) {
      case 'legendary': return 'rgba(255, 140, 0, 0.6)';
      case 'epic': return 'rgba(163, 53, 238, 0.5)';
      case 'rare': return 'rgba(0, 112, 221, 0.4)';
      default: return 'rgba(255, 255, 255, 0.15)';
    }
  };

  return (
    <div className="pack-opening-overlay" onClick={() => {}}>
      {/* Screen shake effect */}
      <style>{`
        @keyframes pack-chest-rotate {
          0% { transform: rotateY(0deg) scale(1); }
          50% { transform: rotateY(180deg) scale(1.1); }
          100% { transform: rotateY(360deg) scale(1); }
        }
        @keyframes pack-burst {
          0% { transform: scale(1); opacity: 1; }
          50% { transform: scale(1.5); opacity: 0.5; }
          100% { transform: scale(2); opacity: 0; }
        }
        @keyframes pack-glow-pulse {
          0%, 100% { box-shadow: 0 0 20px var(--glow-color); }
          50% { box-shadow: 0 0 60px var(--glow-color), 0 0 120px var(--glow-color); }
        }
        @keyframes pack-card-fly-in {
          0% { transform: translateY(60px) scale(0.5); opacity: 0; }
          60% { transform: translateY(-10px) scale(1.05); }
          100% { transform: translateY(0) scale(1); opacity: 1; }
        }
        @keyframes pack-particle {
          0% { transform: translate(0, 0) scale(1); opacity: 1; }
          100% { transform: translate(var(--tx), var(--ty)) scale(0); opacity: 0; }
        }
        @keyframes pack-screen-shake {
          0%, 100% { transform: translate(0, 0); }
          10% { transform: translate(-4px, 2px); }
          20% { transform: translate(4px, -2px); }
          30% { transform: translate(-3px, 3px); }
          40% { transform: translate(3px, -3px); }
          50% { transform: translate(-2px, 1px); }
          60% { transform: translate(2px, -1px); }
          70% { transform: translate(-1px, 2px); }
          80% { transform: translate(1px, -2px); }
        }
        .pack-opening-overlay {
          position: fixed; inset: 0; z-index: 10000;
          display: flex; align-items: center; justify-content: center;
          background: rgba(0, 0, 0, 0.85);
          ${shakeScreen ? 'animation: pack-screen-shake 0.6s ease-out;' : ''}
        }
        .pack-chest-container {
          display: flex; flex-direction: column; align-items: center; gap: 24px;
        }
        .pack-chest-icon {
          font-size: 96px;
          animation: pack-chest-rotate 1.2s ease-in-out;
          filter: drop-shadow(0 0 30px rgba(20, 184, 166, 0.5));
          cursor: pointer;
        }
        .pack-chest-hint {
          color: var(--text-secondary); font-size: 14px;
          animation: fadeIn 0.5s ease 0.8s both;
        }
        .pack-burst-icon {
          font-size: 128px;
          animation: pack-burst 1.2s ease-out forwards;
        }
        .pack-cards-grid {
          display: flex; flex-wrap: wrap; gap: 16px;
          justify-content: center; padding: 20px;
          max-width: 700px;
        }
        .pack-card-item {
          width: 140px; padding: 12px;
          border-radius: var(--radius-lg);
          border: 2px solid #334155;
          background: var(--card);
          text-align: center;
          position: relative; overflow: hidden;
          animation: pack-card-fly-in 0.5s ease-out both;
          box-shadow: 0 0 20px var(--glow-color, transparent);
          cursor: pointer;
          transition: transform 0.2s;
        }
        .pack-card-item:hover {
          transform: translateY(-4px);
        }
        .pack-card-glow {
          position: absolute; inset: 0;
          background: radial-gradient(circle at center, var(--glow-color), transparent 70%);
          opacity: 0.3; pointer-events: none;
        }
        .pack-card-rarity-bar {
          height: 3px; border-radius: 2px; margin-bottom: 8px;
        }
        .pack-card-type { font-size: 18px; margin-bottom: 4px; }
        .pack-card-cost {
          position: absolute; top: 8px; right: 8px;
          background: var(--bg); border-radius: 50%;
          width: 24px; height: 24px; display: flex;
          align-items: center; justify-content: center;
          font-size: 12px; font-weight: 700; color: var(--text);
        }
        .pack-card-rarity {
          font-size: 11px; font-weight: 600; margin-bottom: 4px;
          text-transform: uppercase; letter-spacing: 1px;
        }
        .pack-card-name { font-size: 15px; font-weight: 700; color: var(--text); margin-bottom: 2px; }
        .pack-card-name-en { font-size: 11px; color: var(--text-secondary); margin-bottom: 6px; }
        .pack-card-stats { display: flex; gap: 12px; justify-content: center; font-size: 12px; margin-bottom: 6px; }
        .pack-card-owned { font-size: 12px; color: var(--text-secondary); }
        .pack-open-title {
          color: var(--text); font-size: 24px; font-weight: 700;
          text-align: center; margin-bottom: 4px;
        }
        .pack-open-subtitle {
          color: var(--text-secondary); font-size: 14px;
          text-align: center; margin-bottom: 20px;
        }
        .pack-confirm-btn {
          display: block; margin: 20px auto 0;
          padding: 10px 32px; border-radius: var(--radius-sm);
          border: 1px solid var(--teal); background: var(--teal);
          color: #fff; font-size: 15px; font-weight: 600; cursor: pointer;
        }
        .pack-confirm-btn:hover { background: var(--teal-dark); }

        /* 传说粒子 */
        .pack-legendary-particles {
          position: absolute; inset: 0; pointer-events: none; overflow: hidden;
        }
        .pack-particle {
          position: absolute; width: 6px; height: 6px;
          background: radial-gradient(circle, #ffd700, #ff8c00);
          border-radius: 50%;
          --tx: 0px; --ty: 0px;
          animation: pack-particle 1.5s ease-out forwards;
        }
        @keyframes fadeIn { from { opacity: 0; } to { opacity: 1; } }
      `}</style>

      {phase === 'chest' && (
        <div className="pack-chest-container" onClick={() => setPhase('opening')}>
          <div className="pack-chest-icon">{getPackIcon()}</div>
          <div className="pack-chest-hint">点击打开</div>
        </div>
      )}

      {phase === 'opening' && (
        <div className="pack-chest-container">
          <div className="pack-burst-icon">{getPackIcon()}</div>
          <div className="pack-open-title">✨ 开包中...</div>
          {/* 传说话粒子 */}
          {cards.some(c => c.rarity === 'legendary') && (
            <div className="pack-legendary-particles">
              {Array.from({ length: 20 }).map((_, i) => (
                <div key={i} className="pack-particle" style={{
                  top: `${20 + Math.random() * 60}%`,
                  left: `${20 + Math.random() * 60}%`,
                  '--tx': `${(Math.random() - 0.5) * 300}px`,
                  '--ty': `${(Math.random() - 0.5) * 300}px`,
                  animationDelay: `${i * 0.1}s`,
                } as React.CSSProperties} />
              ))}
            </div>
          )}
        </div>
      )}

      {phase === 'cards' && (
        <div style={{ maxWidth: 700, width: '90%' }}>
          <div className="pack-open-title">🎉 获得新卡牌！</div>
          <div className="pack-open-subtitle">
            {packLabel === '卡包' ? '' : packLabel + ' '}共 {cards.length} 张
          </div>
          <div className="pack-cards-grid">
            {cards.map((card, idx) => {
              if (revealedCards.indexOf(idx) < 0) return null;
              const color = RARITY_COLORS[card.rarity] || '#9d9d9d';
              return (
                <div
                  key={idx}
                  className="pack-card-item"
                  style={{
                    borderColor: color,
                    animationDelay: `${idx * 0.15}s`,
                    '--glow-color': getGlowColor(card.rarity),
                  } as React.CSSProperties}
                  onClick={() => setSelectedCard(card)}
                >
                  <div className="pack-card-glow" style={{ '--glow-color': getGlowColor(card.rarity) } as React.CSSProperties} />
                  <div className="pack-card-rarity-bar" style={{ background: RARITY_BG[card.rarity] || RARITY_BG.common }} />
                  <div className="pack-card-type">
                    {card.cardType === 'minion' ? '⚔️' : card.cardType === 'spell' ? '✨' : card.cardType === 'location' ? '🏰' : '🃏'}
                  </div>
                  <div className="pack-card-cost">{card.cost}</div>
                  <div className="pack-card-rarity" style={{ color }}>{RARITY_CN[card.rarity] || card.rarity}</div>
                  <div className="pack-card-name">{card.nameCn}</div>
                  <div className="pack-card-name-en">{card.nameEn}</div>
                  {card.attack !== null && (
                    <div className="pack-card-stats">
                      <span>⚔️{card.attack}</span>
                      <span>❤️{card.health}</span>
                    </div>
                  )}
                  <div className="pack-card-owned">×{card.quantity}</div>
                </div>
              );
            })}
          </div>
          {revealedCards.length >= cards.length && (
            <button className="pack-confirm-btn" onClick={onClose}>确认</button>
          )}
        </div>
      )}

      {/* Card detail popup */}
      {selectedCard && (
        <div className="card-detail-overlay" onClick={() => setSelectedCard(null)}>
          <div className="card-detail-modal" onClick={e => e.stopPropagation()}>
            <button className="cd-close" onClick={() => setSelectedCard(null)}>✕</button>
            <div className={`cd-rarity-bar rarity-${selectedCard.rarity}`}
              style={{ background: RARITY_COLORS[selectedCard.rarity] }} />
            <div className="cd-type-icon">
              {selectedCard.cardType === 'minion' ? '⚔️' : selectedCard.cardType === 'spell' ? '✨' : '🃏'}
            </div>
            <div className="cd-cost">{selectedCard.cost}</div>
            <div className="cd-rarity" style={{ color: RARITY_COLORS[selectedCard.rarity] }}>
              {RARITY_CN[selectedCard.rarity] || selectedCard.rarity}
            </div>
            <div className="cd-name">{selectedCard.nameCn}</div>
            <div className="cd-name-en">{selectedCard.nameEn}</div>
            {selectedCard.attack !== null && (
              <div className="cd-stats-row">
                <span className="cd-atk">⚔️ 攻击 {selectedCard.attack}</span>
                <span className="cd-hp">❤️ 生命 {selectedCard.health}</span>
              </div>
            )}
            {selectedCard.effectJson && (() => {
              try {
                const eff = JSON.parse(selectedCard.effectJson);
                return (
                  <div className="cd-effect">
                    <div className="cd-effect-text">{eff.description_cn || ''}</div>
                    <div className="cd-effect-text-en">{eff.description_en || ''}</div>
                    {eff.keywords?.length > 0 && (
                      <div className="cd-keywords">
                        {eff.keywords.map((k: string) => <span key={k} className="cd-keyword">{k}</span>)}
                      </div>
                    )}
                  </div>
                );
              } catch { return null; }
            })()}
            {selectedCard.quoteText && (
              <div className="cd-quote">"{selectedCard.quoteText}"</div>
            )}
            <div className="cd-owned">拥有 ×{selectedCard.quantity}</div>
          </div>
        </div>
      )}
    </div>
  );
}
