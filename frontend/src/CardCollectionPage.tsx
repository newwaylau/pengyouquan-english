import React, { useState, useEffect } from 'react';
import { cardApi } from './api/cardClient';
import { gameApi } from './api/client';
import DeckBuilderPage from './DeckBuilderPage';
import ChestPanel from './ChestPanel';

const RARITY_ORDER = ['legendary', 'epic', 'rare', 'common'];
const RARITY_CN: Record<string, string> = { legendary: '传说', epic: '史诗', rare: '稀有', common: '普通' };
const CRAFT_COSTS: Record<string, number> = { legendary: 3200, epic: 800, rare: 160, common: 40 };
const DISENCHANT_VALUES: Record<string, number> = { legendary: 400, epic: 100, rare: 20, common: 5 };
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
  const [tab, setTab] = useState<'cards' | 'decks' | 'arena' | 'craft'>('cards');
  const [myCards, setMyCards] = useState<any[]>([]);
  const [allCards, setAllCards] = useState<any[]>([]);
  const [decks, setDecks] = useState<any[]>([]);
  const [loading, setLoading] = useState(true);
  const [selectedCard, setSelectedCard] = useState<any | null>(null);
  const [showDeckBuilder, setShowDeckBuilder] = useState(false);
  const [craftRarityFilter, setCraftRarityFilter] = useState<string | null>(null);

  // Arena data for 战绩 tab
  const [prestige, setPrestige] = useState<any>(null);
  const [history, setHistory] = useState<any[]>([]);

  // 星尘
  const [stardust, setStardust] = useState(0);
  const [disenchantConfirm, setDisenchantConfirm] = useState<any>(null);
  const [craftConfirm, setCraftConfirm] = useState<any>(null);
  const [toastMsg, setToastMsg] = useState('');

  useEffect(() => {
    loadData();
    loadArenaData();
    loadStardust();
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

  const loadStardust = async () => {
    const res = await cardApi.getStardust();
    if (res.code === 200) setStardust(res.data?.stardust || 0);
  };

  const handleDisenchant = async (cardId: number) => {
    setDisenchantConfirm(null);
    const res = await cardApi.disenchantCard(cardId);
    if (res.code === 200) {
      setToastMsg(`✨ 分解成功，获得 ${res.data.stardustGained} 星尘`);
      setTimeout(() => setToastMsg(''), 3000);
      loadData();
      loadStardust();
    } else {
      setToastMsg(res.message || '分解失败');
      setTimeout(() => setToastMsg(''), 3000);
    }
  };

  const handleCraft = async (cardId: number) => {
    setCraftConfirm(null);
    const res = await cardApi.craftCard(cardId);
    if (res.code === 200) {
      setToastMsg(`🎉 合成了新卡牌！消耗 ${res.data.stardustCost} 星尘`);
      setTimeout(() => setToastMsg(''), 3000);
      loadData();
      loadStardust();
    } else {
      setToastMsg(res.message || '合成失败');
      setTimeout(() => setToastMsg(''), 3000);
    }
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
      {/* 宝箱面板 */}
      <ChestPanel user={user} />

      {/* Header */}
      <div className="cc-header">
        <h2 className="cc-title">🎴 我的卡牌</h2>
        <div className="cc-fragments" style={{ display: 'flex', gap: 16, alignItems: 'center' }}>
          <span>✨ 星尘 <span className="cc-frag-count">{stardust}</span></span>
          <span>🧩 碎片 <span className="cc-frag-count">{ownedCount}/{totalCards}</span></span>
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
        <button className={`cst-btn ${tab === 'craft' ? 'active' : ''}`} onClick={() => { setTab('craft'); loadStardust(); }}>
          ✨ 合成
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
              {card.quantity > 0 && (
                <button className="cg-disenchant-btn"
                  onClick={e => { e.stopPropagation(); setDisenchantConfirm(card); }}
                  title="分解"
                  style={{
                    position: 'absolute', top: 4, left: 4, width: 24, height: 24,
                    background: 'rgba(0,0,0,0.5)', border: 'none', borderRadius: '50%',
                    color: '#ef4444', fontSize: 12, cursor: 'pointer', display: 'flex',
                    alignItems: 'center', justifyContent: 'center', padding: 0, zIndex: 2,
                  }}
                >
                  ⛏️
                </button>
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

      {/* ✨ 合成 tab */}
      {tab === 'craft' && (
        <div className="cc-craft-tab">
          <div style={{ color: 'var(--text-secondary)', fontSize: 13, marginBottom: 12, textAlign: 'center' }}>
            ✨ 星尘余额: <strong style={{ color: '#fbbf24', fontSize: 18 }}>{stardust}</strong>
          </div>
          <div style={{ display: 'flex', gap: 8, marginBottom: 16, flexWrap: 'wrap', justifyContent: 'center' }}>
            {RARITY_ORDER.map(r => (
              <button key={r}
                onClick={() => setCraftRarityFilter(r === craftRarityFilter ? null : r)}
                style={{
                  padding: '4px 14px', borderRadius: 'var(--radius-sm)',
                  border: `1px solid ${craftRarityFilter === r ? RARITY_COLORS[r] : 'var(--border)'}`,
                  background: craftRarityFilter === r ? `${RARITY_COLORS[r]}22` : 'transparent',
                  color: craftRarityFilter === r ? RARITY_COLORS[r] : 'var(--text-secondary)',
                  fontSize: 13, cursor: 'pointer', fontWeight: craftRarityFilter === r ? 600 : 400,
                }}
              >
                {RARITY_CN[r]}
              </button>
            ))}
            <button
              onClick={() => setCraftRarityFilter(null)}
              style={{
                padding: '4px 14px', borderRadius: 'var(--radius-sm)',
                border: '1px solid var(--border)',
                background: !craftRarityFilter ? 'var(--hover-bg)' : 'transparent',
                color: 'var(--text-secondary)', fontSize: 13, cursor: 'pointer',
              }}
            >
              全部
            </button>
          </div>
          <div className="cards-grid">
            {mergedCards
              .filter(c => !craftRarityFilter || c.rarity === craftRarityFilter)
              .sort((a, b) => {
                const aHas = a.quantity > 0 ? 1 : 0;
                const bHas = b.quantity > 0 ? 1 : 0;
                return aHas - bHas;
              })
              .map(card => {
                const cost = CRAFT_COSTS[card.rarity] || 9999;
                const canCraft = stardust >= cost;
                return (
                  <div key={card.id} className={`cg-card rarity-${card.rarity} ${card.quantity > 0 ? 'owned' : 'unowned'}`}
                    onClick={() => setSelectedCard(card)}
                    style={{ position: 'relative' }}
                  >
                    <div className="cg-rarity-bar" style={{ background: RARITY_COLORS[card.rarity] }} />
                    <div className="cg-type-icon">{TYPE_ICONS[card.cardType] || '🃏'}</div>
                    <div className="cg-cost">{card.cost}</div>
                    <div className="cg-rarity-label" style={{ color: RARITY_COLORS[card.rarity] }}>
                      {RARITY_CN[card.rarity] || card.rarity}
                    </div>
                    <div className="cg-name">{card.nameCn}</div>
                    <div className="cg-name-en">{card.nameEn}</div>
                    {card.quantity > 0 ? (
                      <div className="cg-owned">拥有 ×{card.quantity}</div>
                    ) : (
                      <div className="cg-unowned">未收集</div>
                    )}
                    <div style={{ marginTop: 4, color: canCraft ? '#fbbf24' : 'var(--danger)', fontSize: 11 }}>
                      ✨ {cost}
                    </div>
                    <button
                      className="btn btn-sm"
                      style={{
                        marginTop: 6, width: '100%', padding: '4px',
                        background: canCraft ? 'var(--teal)' : 'rgba(255,255,255,0.08)',
                        border: 'none', color: canCraft ? '#fff' : 'var(--text-secondary)',
                        borderRadius: 'var(--radius-sm)', cursor: canCraft ? 'pointer' : 'not-allowed',
                        fontSize: 12,
                      }}
                      disabled={!canCraft}
                      onClick={e => { e.stopPropagation(); if (canCraft) setCraftConfirm(card); }}
                    >
                      {canCraft ? '合成' : '星尘不足'}
                    </button>
                  </div>
                );
              })}
          </div>
          {mergedCards.filter(c => !craftRarityFilter || c.rarity === craftRarityFilter).length === 0 && (
            <div className="cc-empty"><div className="cc-empty-text">暂无卡牌</div></div>
          )}
        </div>
      )}

      {/* Match button */}
      <div className="cc-match-btn">
        <button className="btn btn-lg" disabled style={{ width: '100%', opacity: 0.5 }}>
          ⚔️ 匹配对战 · 即将开放
        </button>
      </div>

      {/* 分解确认弹窗 */}
      {disenchantConfirm && (
        <div className="card-detail-overlay" onClick={() => setDisenchantConfirm(null)}>
          <div className="card-detail-modal" onClick={e => e.stopPropagation()} style={{ maxWidth: 340 }}>
            <button className="cd-close" onClick={() => setDisenchantConfirm(null)}>✕</button>
            <div style={{ textAlign: 'center', marginBottom: 16 }}>
              <div style={{ fontSize: 36, marginBottom: 8 }}>⛏️</div>
              <div style={{ color: 'var(--text-primary)', fontSize: 16, fontWeight: 600, marginBottom: 8 }}>
                分解 {disenchantConfirm.nameCn}
              </div>
              <div style={{ color: 'var(--text-secondary)', fontSize: 14, marginBottom: 4 }}>
                分解后将获得 <strong style={{ color: '#fbbf24' }}>{DISENCHANT_VALUES[disenchantConfirm.rarity] || 0} 星尘</strong>
              </div>
              <div style={{ color: 'var(--text-secondary)', fontSize: 12 }}>
                当前拥有 {disenchantConfirm.quantity} 张
              </div>
            </div>
            <div style={{ display: 'flex', gap: 8 }}>
              <button className="btn btn-outline-style" style={{ flex: 1 }}
                onClick={() => setDisenchantConfirm(null)}>取消</button>
              <button className="btn btn-primary" style={{ flex: 1 }}
                onClick={() => handleDisenchant(disenchantConfirm.id)}>确认分解</button>
            </div>
          </div>
        </div>
      )}

      {/* 合成确认弹窗 */}
      {craftConfirm && (
        <div className="card-detail-overlay" onClick={() => setCraftConfirm(null)}>
          <div className="card-detail-modal" onClick={e => e.stopPropagation()} style={{ maxWidth: 340 }}>
            <button className="cd-close" onClick={() => setCraftConfirm(null)}>✕</button>
            <div style={{ textAlign: 'center', marginBottom: 16 }}>
              <div style={{ fontSize: 36, marginBottom: 8 }}>✨</div>
              <div style={{ color: 'var(--text-primary)', fontSize: 16, fontWeight: 600, marginBottom: 8 }}>
                合成 {craftConfirm.nameCn}
              </div>
              <div style={{ color: 'var(--text-secondary)', fontSize: 14, marginBottom: 4 }}>
                消耗 <strong style={{ color: '#fbbf24' }}>{CRAFT_COSTS[craftConfirm.rarity] || 0} 星尘</strong>
              </div>
              <div style={{ color: 'var(--text-secondary)', fontSize: 12 }}>
                星尘余额: {stardust}
              </div>
            </div>
            <div style={{ display: 'flex', gap: 8 }}>
              <button className="btn btn-outline-style" style={{ flex: 1 }}
                onClick={() => setCraftConfirm(null)}>取消</button>
              <button className="btn btn-primary" style={{ flex: 1 }}
                onClick={() => handleCraft(craftConfirm.id)}>确认合成</button>
            </div>
          </div>
        </div>
      )}

      {/* Toast */}
      {toastMsg && (
        <div style={{
          position: 'fixed', bottom: 80, left: '50%', transform: 'translateX(-50%)',
          background: 'rgba(0,0,0,0.85)', color: '#fff', padding: '10px 24px',
          borderRadius: 'var(--radius-sm)', zIndex: 9999, fontSize: 14,
          border: '1px solid var(--teal-glow)', whiteSpace: 'nowrap',
        }}>
          {toastMsg}
        </div>
      )}

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
