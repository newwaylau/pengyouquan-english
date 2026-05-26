import React, { useState, useEffect } from 'react';
import { cardApi } from './api/cardClient';

const RARITY_ORDER = ['legendary', 'epic', 'rare', 'common'];
const RARITY_CN: Record<string, string> = { legendary: '传说', epic: '史诗', rare: '稀有', common: '普通' };
const RARITY_COLORS: Record<string, string> = {
  legendary: '#ff8c00',
  epic: '#a335ee',
  rare: '#0070dd',
  common: '#9d9d9d',
};
const TYPE_ICONS: Record<string, string> = {
  minion: '⚔️',
  spell: '✨',
  equipment: '🛡️',
  location: '🏰',
};

export default function DeckBuilderPage({
  user, myCards, onBack, onSave,
}: {
  user: any;
  myCards: any[];
  onBack: () => void;
  onSave: () => void;
}) {
  const [deckName, setDeckName] = useState('未命名卡组');
  const [selectedIds, setSelectedIds] = useState<number[]>([]);
  const [allCards, setAllCards] = useState<any[]>([]);
  const [showFilter, setShowFilter] = useState<string>('all');
  const [saving, setSaving] = useState(false);

  useEffect(() => {
    cardApi.getCards().then(r => {
      if (r.code === 200) setAllCards(r.data || []);
    });
  }, []);

  // Available cards: merge myCards quantities
  const ownedMap = new Map(myCards.map(c => [c.id, c.quantity]));
  const available = allCards.filter(c => (ownedMap.get(c.id) || 0) > 0);
  const filteredAvailable = showFilter === 'all'
    ? available
    : available.filter(c => c.rarity === showFilter);

  // Card count in deck
  const countByCardId = selectedIds.reduce((acc, id) => {
    acc.set(id, (acc.get(id) || 0) + 1);
    return acc;
  }, new Map<number, number>());

  // Mana curve
  const manaCurve = Array.from({ length: 11 }, (_, i) => ({
    cost: i,
    count: selectedIds.filter(id => {
      const card = allCards.find(c => c.id === id);
      return card && (card.cost === i || (i === 10 && card.cost >= 10));
    }).length,
  }));

  const maxCurve = Math.max(1, ...manaCurve.map(m => m.count));

  const addCard = (cardId: number) => {
    if (selectedIds.length >= 30) return;
    const card = allCards.find(c => c.id === cardId);
    if (!card) return;

    // Check legendary duplicate
    if (card.rarity === 'legendary' && countByCardId.get(cardId) >= 1) return;

    // Check max copies by rarity
    const maxCopies = card.rarity === 'legendary' ? 1 : card.rarity === 'epic' ? 2 : card.rarity === 'rare' ? 2 : 3;
    if ((countByCardId.get(cardId) || 0) >= maxCopies) return;
    if ((countByCardId.get(cardId) || 0) >= (ownedMap.get(cardId) || 0)) return;

    setSelectedIds(prev => [...prev, cardId]);
  };

  const removeCard = (index: number) => {
    setSelectedIds(prev => prev.filter((_, i) => i !== index));
  };

  const handleSave = async () => {
    if (selectedIds.length === 0) return;
    setSaving(true);
    const r = await cardApi.saveDeck(deckName, selectedIds);
    setSaving(false);
    if (r.code === 200) {
      onSave();
    }
  };

  return (
    <div className="deck-builder-page">
      {/* Header */}
      <div className="db-header">
        <button className="db-back" onClick={onBack}>← 返回</button>
        <input
          className="db-name-input"
          value={deckName}
          onChange={e => setDeckName(e.target.value)}
          maxLength={50}
        />
        <div className="db-count">{selectedIds.length}/30</div>
      </div>

      {/* Mana curve */}
      <div className="deck-curve">
        <div className="dc-title">费用曲线</div>
        <div className="dc-bars">
          {manaCurve.map(m => (
            <div key={m.cost} className="dc-bar-col">
              <div className="dc-bar-val">{m.count > 0 ? m.count : ''}</div>
              <div className="dc-bar" style={{ height: `${(m.count / maxCurve) * 60}px`, opacity: m.count > 0 ? 1 : 0.2 }}>
                <div className="dc-bar-fill" />
              </div>
              <div className="dc-bar-label">{m.cost}</div>
            </div>
          ))}
        </div>
      </div>

      {/* Content */}
      <div className="db-content">
        {/* Left: Available cards */}
        <div className="db-pool">
          <div className="db-section-title">可用卡牌</div>
          <div className="db-filter-row">
            {['all', ...RARITY_ORDER].map(r => (
              <button key={r} className={`db-filter-btn ${showFilter === r ? 'active' : ''}`}
                onClick={() => setShowFilter(r)}>
                {r === 'all' ? '全部' : RARITY_CN[r]}
              </button>
            ))}
          </div>
          <div className="db-pool-list">
            {filteredAvailable.sort((a, b) => a.cost - b.cost).map(card => (
              <div key={card.id} className="db-card-row" onClick={() => addCard(card.id)}
                style={{ opacity: (countByCardId.get(card.id) || 0) >= (ownedMap.get(card.id) || 0) ? 0.4 : 1 }}>
                <span className="db-cost-badge">{card.cost}</span>
                <span className="db-type-icon">{TYPE_ICONS[card.cardType] || '🃏'}</span>
                <span className="db-card-name">{card.nameCn}</span>
                <span className="db-card-type">{card.cardType}</span>
                {card.attack !== null && <span className="db-atk">⚔️{card.attack}</span>}
                {card.health !== null && <span className="db-hp">❤️{card.health}</span>}
                <span className="db-owned-count" style={{ color: RARITY_COLORS[card.rarity] }}>
                  ×{ownedMap.get(card.id) || 0}
                </span>
              </div>
            ))}
          </div>
        </div>

        {/* Right: Selected deck */}
        <div className="db-selected">
          <div className="db-section-title">已选卡组 ({selectedIds.length})</div>
          <div className="db-selected-list">
            {selectedIds.length === 0 ? (
              <div className="db-empty">点击左侧卡牌添加到卡组</div>
            ) : (
              selectedIds.map((cardId, index) => {
                const card = allCards.find(c => c.id === cardId);
                if (!card) return null;
                return (
                  <div key={`${cardId}-${index}`} className="db-card-row selected">
                    <span className="db-cost-badge">{card.cost}</span>
                    <span className="db-type-icon">{TYPE_ICONS[card.cardType] || '🃏'}</span>
                    <span className="db-card-name">{card.nameCn}</span>
                    {card.attack !== null && <span className="db-atk">⚔️{card.attack}</span>}
                    {card.health !== null && <span className="db-hp">❤️{card.health}</span>}
                    <button className="db-remove-btn" onClick={() => removeCard(index)}>✕</button>
                  </div>
                );
              })
            )}
          </div>
        </div>
      </div>

      {/* Footer */}
      <div className="db-footer">
        <button className="btn btn-outline" onClick={onBack}>取消</button>
        <button className="btn btn-primary" onClick={handleSave} disabled={selectedIds.length === 0 || saving}>
          {saving ? '保存中...' : '保存卡组'}
        </button>
      </div>
    </div>
  );
}
