import React, { useState, useEffect } from 'react';
import { cardApi } from './api/cardClient';
import { gameApi } from './api/client';
import DeckBuilderPage from './DeckBuilderPage';

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

export default function CardCollectionPage({ user, onNavigate }: { user: any; onNavigate: (target: string, data?: any) => void }) {
  const [tab, setTab] = useState<'cards' | 'decks' | 'arena'>('cards');
  const [myCards, setMyCards] = useState<any[]>([]);
  const [allCards, setAllCards] = useState<any[]>([]);
  const [decks, setDecks] = useState<any[]>([]);
  const [loading, setLoading] = useState(true);
  const [selectedCard, setSelectedCard] = useState<any | null>(null);
  const [showDeckBuilder, setShowDeckBuilder] = useState(false);

  // Arena data for 战绩 tab
  const [prestige, setPrestige] = useState<any>(null);
  const [history, setHistory] = useState<any[]>([]);

  useEffect(() => {
    loadData();
    loadArenaData();
  }, []);

  const loadData = async () => {
    setLoading(true);
    const [myCardsRes, allCardsRes, decksRes] = await Promise.all([
      cardApi.getMyCards(),
      cardApi.getCards(),
      cardApi.getDecks(),
    ]);
    if (myCardsRes.code === 200) setMyCards(myCardsRes.data || []);
    if (allCardsRes.code === 200) setAllCards(allCardsRes.data || []);
    if (decksRes.code === 200) setDecks(decksRes.data || []);
    setLoading(false);
  };

  const loadArenaData = async () => {
    const [pRes, hRes] = await Promise.all([
      gameApi.getPrestige(),
      gameApi.getHistory(),
    ]);
    if (pRes.code === 200) setPrestige(pRes.data);
    if (hRes.code === 200) setHistory(hRes.data || []);
  };

  // Stats
  const ownedCount = myCards.reduce((sum, c) => sum + c.quantity, 0);
  const totalCards = allCards.length;
  const rarityStats = RARITY_ORDER.map(r => ({
    rarity: r,
    label: RARITY_CN[r],
    count: myCards.filter(c => c.rarity === r).length,
    total: allCards.filter(c => c.rarity === r).length,
    color: RARITY_COLORS[r],
  }));

  // Merge owned quantity into all cards
  const cardMap = new Map(myCards.map(c => [c.id, c]));
  const mergedCards = allCards.map(c => ({
    ...c,
    quantity: cardMap.get(c.id)?.quantity || 0,
  }));

  if (showDeckBuilder) {
    return <DeckBuilderPage
      user={user}
      myCards={myCards}
      onBack={() => setShowDeckBuilder(false)}
      onSave={() => { setShowDeckBuilder(false); loadData(); }}
    />;
  }

  if (loading) return <div className="page-loading">加载中...</div>;

  return (
    <div className="card-collection-page">
      {/* Header */}
      <div className="cc-header">
        <h2 className="cc-title">🎴 我的卡牌</h2>
        <div className="cc-fragments">
          🧩 碎片 <span className="cc-frag-count">{ownedCount}/{totalCards}</span>
        </div>
      </div>

      {/* Stats row */}
      <div className="cards-stats">
        {rarityStats.map(s => (
          <div key={s.rarity} className="cs-item" style={{ borderLeftColor: s.color }}>
            <div className="cs-count">{s.count}<span className="cs-total">/{s.total}</span></div>
            <div className="cs-label" style={{ color: s.color }}>{s.label}</div>
          </div>
        ))}
      </div>

      {/* Tabs */}
      <div className="cards-subtabs">
        <button className={`cst-btn ${tab === 'cards' ? 'active' : ''}`} onClick={() => setTab('cards')}>
          📖 卡册
        </button>
        <button className={`cst-btn ${tab === 'decks' ? 'active' : ''}`} onClick={() => setTab('decks')}>
          📦 卡组 ({decks.length})
        </button>
        <button className={`cst-btn ${tab === 'arena' ? 'active' : ''}`} onClick={() => setTab('arena')}>
          ⚔️ 战绩
        </button>
      </div>

      {/* Tab content */}
      {tab === 'cards' && (
        <div className="cards-grid">
          {mergedCards.map(card => (
            <div
              key={card.id}
              className={`cg-card rarity-${card.rarity} ${card.quantity > 0 ? 'owned' : 'unowned'}`}
              onClick={() => setSelectedCard(card)}
            >
              <div className="cg-rarity-bar" style={{ background: RARITY_COLORS[card.rarity] }} />
              <div className="cg-type-icon">{TYPE_ICONS[card.cardType] || '🃏'}</div>
              <div className="cg-cost">{card.cost}</div>
              <div className="cg-rarity-label" style={{ color: RARITY_COLORS[card.rarity] }}>
                {RARITY_CN[card.rarity] || card.rarity}
              </div>
              <div className="cg-name">{card.nameCn}</div>
              <div className="cg-name-en">{card.nameEn}</div>
              <div className="cg-stats">
                {card.attack !== null && <span className="cg-atk">⚔️{card.attack}</span>}
                {card.health !== null && <span className="cg-hp">❤️{card.health}</span>}
              </div>
              {card.quantity > 0 ? (
                <div className="cg-owned">×{card.quantity}</div>
              ) : (
                <div className="cg-unowned">未收集</div>
              )}
            </div>
          ))}
        </div>
      )}

      {tab === 'decks' && (
        <div className="cc-decks-tab">
          {decks.length === 0 ? (
            <div className="cc-empty">
              <div className="cc-empty-icon">📦</div>
              <div className="cc-empty-text">还没有卡组</div>
              <button className="btn btn-primary" onClick={() => setShowDeckBuilder(true)}>
                创建卡组
              </button>
            </div>
          ) : (
            <>
              <button className="btn btn-primary cc-create-deck" onClick={() => setShowDeckBuilder(true)}>
                ＋ 新建卡组
              </button>
              {decks.map((deck: any) => (
                <div key={deck.id} className="cc-deck-item">
                  <div className="cc-deck-name">{deck.name}</div>
                  <div className="cc-deck-count">{deck.cardCount}/30 张</div>
                </div>
              ))}
            </>
          )}
        </div>
      )}

      {tab === 'arena' && (
        <div className="cc-arena-tab">
          {prestige && (
            <div className="arena-rank-card" style={{
              background: 'linear-gradient(135deg, var(--card), var(--bg))',
              borderRadius: 'var(--radius-lg)', padding: 24, margin: '0 0 16px',
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
                {prestige.titleCn} · {prestige.titleEn}
              </div>
              <div style={{ textAlign: 'center', color: 'var(--text-secondary)', fontSize: 12, marginBottom: 16 }}>
                等级 {prestige.rankTier}/10
              </div>
              <div style={{ marginBottom: 8 }}>
                <div style={{ display: 'flex', justifyContent: 'space-between', fontSize: 12, color: 'var(--text-secondary)', marginBottom: 4 }}>
                  <span>⚡ {prestige.prestige} 威望</span>
                  <span>下一级 {prestige.nextRequiredPrestige}</span>
                </div>
                <div style={{
                  height: 8, background: 'rgba(255,255,255,0.08)', borderRadius: 'var(--radius-sm)',
                  overflow: 'hidden'
                }}>
                  <div style={{
                    width: `${Math.min(100, prestige.progressPercent || 0)}%`,
                    height: '100%',
                    background: 'linear-gradient(90deg, var(--teal), var(--teal-light))',
                    borderRadius: 'var(--radius-sm)',
                    transition: 'width 0.5s ease'
                  }} />
                </div>
              </div>
              <div style={{ display: 'flex', justifyContent: 'space-around', marginTop: 16 }}>
                <div style={{ textAlign: 'center' }}>
                  <div style={{ color: '#fbbf24', fontSize: 18, fontWeight: 600 }}>🔥</div>
                  <div style={{ color: 'var(--text-primary)', fontSize: 16, fontWeight: 600 }}>{prestige.consecutiveDays || 0}</div>
                  <div style={{ color: 'var(--text-secondary)', fontSize: 11 }}>统治天数</div>
                </div>
                <div style={{ textAlign: 'center', cursor: 'pointer' }} onClick={() => onNavigate?.('daily-challenge')}>
                  <div style={{ color: 'var(--teal)', fontSize: 18, fontWeight: 600 }}>⚔️</div>
                  <div style={{ color: 'var(--text-primary)', fontSize: 16, fontWeight: 600 }}>今日</div>
                  <div style={{ color: 'var(--text-secondary)', fontSize: 11 }}>御前挑战</div>
                </div>
                <div style={{ textAlign: 'center' }} onClick={() => onNavigate?.('arena')}>
                  <div style={{ color: '#a78bfa', fontSize: 18, fontWeight: 600 }}>🏆</div>
                  <div style={{ color: 'var(--text-primary)', fontSize: 16, fontWeight: 600 }}>详细</div>
                  <div style={{ color: 'var(--text-secondary)', fontSize: 11 }}>查看全部</div>
                </div>
              </div>
            </div>
          )}

          {/* History */}
          <div className="cc-section-title">📜 战报</div>
          {history.length === 0 ? (
            <div className="cc-empty" style={{ padding: 16 }}>
              <div className="cc-empty-text">暂无战报</div>
            </div>
          ) : (
            history.map((h: any, i: number) => (
              <div key={i} className="cc-history-item">
                <div className="cc-history-date">{h.date}</div>
                <div className="cc-history-score">正确 {h.correctCount}/{h.totalQuestions}</div>
                <div className="cc-history-prestige">+{h.prestigeEarned} ⚡</div>
              </div>
            ))
          )}
        </div>
      )}

      {/* Match button */}
      <div className="cc-match-btn">
        <button className="btn btn-lg" disabled style={{ width: '100%', opacity: 0.5 }}>
          ⚔️ 匹配对战 · 即将开放
        </button>
      </div>

      {/* Card Detail Modal */}
      {selectedCard && (
        <div className="card-detail-overlay" onClick={() => setSelectedCard(null)}>
          <div className="card-detail-modal" onClick={e => e.stopPropagation()}>
            <button className="cd-close" onClick={() => setSelectedCard(null)}>✕</button>
            <div className={`cd-rarity-bar rarity-${selectedCard.rarity}`} style={{ background: RARITY_COLORS[selectedCard.rarity] }} />
            <div className="cd-type-icon">{TYPE_ICONS[selectedCard.cardType] || '🃏'}</div>
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
              <div className="cd-quote">“{selectedCard.quoteText}”</div>
            )}
            <div className="cd-faction">{selectedCard.faction || '中立'}</div>
            <div className="cd-owned">拥有 ×{selectedCard.quantity}</div>
          </div>
        </div>
      )}
    </div>
  );
}
