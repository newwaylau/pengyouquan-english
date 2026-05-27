import React, { useState } from 'react';

// ── Types ─────────────────────────────────────────────
interface CardItem {
  id: number;
  nameCn: string;
  nameEn?: string;
  attack?: number;
  effectiveAttack?: number;
  attackBonus?: number;
  health?: number;
  effectiveHealth?: number;
  cost?: number;
  rarity?: string;
}

interface ExpeditionCampfireProps {
  playerHp: number;
  maxHp: number;
  deck: any[];
  onRest: (action: string, cardId?: number) => void;
}

// ── Component ─────────────────────────────────────────
export default function ExpeditionCampfire({
  playerHp,
  maxHp,
  deck,
  onRest,
}: ExpeditionCampfireProps) {
  const [showUpgrade, setShowUpgrade] = useState(false);
  const [selectedCardId, setSelectedCardId] = useState<number | null>(null);

  const healAmount = Math.ceil(maxHp * 0.3);
  const hasUpgradeCards = deck.length > 0;

  function handleHeal() {
    onRest('heal');
  }

  function handleUpgrade() {
    if (selectedCardId) {
      onRest('upgrade', selectedCardId);
      setShowUpgrade(false);
      setSelectedCardId(null);
    }
  }

  return (
    <div className="expedition-rest">
      <div className="expedition-rest-title">🔥 休息</div>
      <p style={{ color: 'var(--text-secondary)', fontSize: 13 }}>
        当前血量：{playerHp}/{maxHp}
      </p>

      <div className="expedition-rest-buttons">
        <button className="expedition-rest-btn" onClick={handleHeal}>
          ❤️ 回血
          <div className="expedition-rest-heal-amount">+{healAmount}</div>
        </button>
        <button
          className="expedition-rest-btn"
          onClick={() => setShowUpgrade(true)}
          disabled={!hasUpgradeCards}
          style={{ opacity: hasUpgradeCards ? 1 : 0.5, cursor: hasUpgradeCards ? 'pointer' : 'not-allowed' }}
        >
          ⬆️ 强化卡牌
          <div className="expedition-rest-heal-amount">攻/血+1</div>
        </button>
      </div>

      {showUpgrade && (
        <div className="expedition-upgrade-section" style={{ marginTop: 12, width: '100%', maxWidth: 320 }}>
          <div style={{ color: 'var(--text-secondary)', fontSize: 13, marginBottom: 8, textAlign: 'center' }}>
            选择要强化的卡牌（攻击+1，生命+1）：
          </div>
          <div className="expedition-hand" style={{ flexWrap: 'wrap', justifyContent: 'center' }}>
            {deck.map((card: any, i: number) => (
              <div
                key={i}
                className={`expedition-hand-card ${selectedCardId === card.id ? 'selected' : ''}`}
                onClick={() => setSelectedCardId(card.id)}
                style={{
                  cursor: 'pointer',
                  border: selectedCardId === card.id ? '2px solid #ffd700' : '2px solid transparent',
                  transition: 'border 0.2s',
                }}
              >
                <div className="expedition-hand-card-name">{card.nameCn}</div>
                <div className="expedition-hand-card-attack">
                  ⚔️{card.effectiveAttack || card.attack || 0}
                  {card.attackBonus > 0 && (
                    <span style={{ color: '#4ade80', marginLeft: 2 }}>(+{card.attackBonus})</span>
                  )}
                </div>
                <div className="expedition-hand-card-cost">
                  ❤️{card.effectiveHealth || card.health || 0}
                </div>
              </div>
            ))}
          </div>
          <button
            className="expedition-rest-btn"
            style={{
              marginTop: 8,
              opacity: selectedCardId ? 1 : 0.5,
              width: '100%',
            }}
            disabled={!selectedCardId}
            onClick={handleUpgrade}
          >
            ✅ 确认强化
          </button>
        </div>
      )}
    </div>
  );
}
