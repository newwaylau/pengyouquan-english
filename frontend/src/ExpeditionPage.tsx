import React, { useState, useEffect, useCallback } from 'react';
import './expedition.css';
import ExpeditionMap from './ExpeditionMap';
import ExpeditionCampfire from './ExpeditionCampfire';
import ExpeditionStatus from './ExpeditionStatus';
import ExpeditionPotion from './ExpeditionPotion';
import ExpeditionEvent from './ExpeditionEvent';
import ExpeditionBoss from './ExpeditionBoss';
import ExpeditionReward from './ExpeditionReward';

const API_BASE = '';
function getToken() { return localStorage.getItem('token'); }
async function apiFetch(path: string, options: RequestInit = {}) {
  const headers: Record<string, string> = { 'Content-Type': 'application/json' };
  const token = getToken();
  if (token) headers['Authorization'] = `Bearer ${token}`;
  const res = await fetch(`${API_BASE}${path}`, { ...options, headers });
  const data = await res.json();
  if (data.code === 401) { localStorage.removeItem('token'); window.location.reload(); }
  return data;
}

interface ExpeditionData {
  id: number; showId: number; act: number; node: number; maxAct: number;
  playerHp: number; maxHp: number; gold: number; status: string;
  questionsAnswered: number; questionsTotal: number; enemiesKilled: number;
  mapNodes: string[]; currentNodeType: string;
  deck: any[]; relics: any[]; potions?: any[];
}

interface EnemyData {
  id: number; nameCn: string; nameEn: string; maxHp: number;
  currentHp: number; isBoss: boolean; specialRules?: any;
}

interface CardItem {
  id: number; nameCn: string; nameEn: string; attack: number;
  cost: number; rarity: string; cardType: string;
}

const ACT_LABELS = ['', '绝境长城', '君临城', '龙石岛'];
const NODE_ICONS: Record<string, string> = { combat: '⚔️', event: '❓', rest: '🔥', shop: '🛒', boss: '👑' };
const NODE_LABELS: Record<string, string> = { combat: '战斗', event: '事件', rest: '休息', shop: '商店', boss: 'Boss' };

export default function ExpeditionPage({ onNavigate }: { user?: any; onNavigate?: (page: string, data?: any) => void }) {
  const [tab, setTab] = useState<'current' | 'history'>('current');
  const [loading, setLoading] = useState(true);

  // Expedition state
  const [expedition, setExpedition] = useState<ExpeditionData | null>(null);
  const [hasExpedition, setHasExpedition] = useState(false);
  const [history, setHistory] = useState<any[]>([]);
  const [enemy, setEnemy] = useState<EnemyData | null>(null);
  const [handCards, setHandCards] = useState<CardItem[]>([]);

  // Lobby state
  const [shows, setShows] = useState<any[]>([]);
  const [selectedShow, setSelectedShow] = useState<number | null>(null);
  const [userCards, setUserCards] = useState<any[]>([]);
  const [selectedCards, setSelectedCards] = useState<number[]>([]);
  const [deckSelectMode, setDeckSelectMode] = useState(false);

  // Combat state (pure click-based, no questions)
  const [feedback, setFeedback] = useState<string | null>(null);
  const [damageNumber, setDamageNumber] = useState<{ text: string; type: string } | null>(null);
  const [attackingCard, setAttackingCard] = useState<number | null>(null);
  const [combatLoading, setCombatLoading] = useState(false);

  // Node interaction
  const [nodeType, setNodeType] = useState<string>('');

  // Branch state
  const [nodeOptions, setNodeOptions] = useState<string[]>([]);

  // Event state
  const [eventData, setEventData] = useState<any>(null);

  // Shop state
  const [shopItems, setShopItems] = useState<any[]>([]);
  const [removeMode, setRemoveMode] = useState(false);

  // Reward state
  const [rewardChoices, setRewardChoices] = useState<any[]>([]);

  // Settlement state
  const [settlement, setSettlement] = useState<{ cleared: boolean; expedition: any } | null>(null);

  // Relic acquisition popup
  const [relicPopup, setRelicPopup] = useState<{ id: number; nameCn: string; nameEn: string; descriptionCn: string; icon: string; rarity: string } | null>(null);

  // Hovered relic for tooltip (moved to ExpeditionStatus)
  // const [hoveredRelic, setHoveredRelic] = useState<number | null>(null);

  // Game phase
  type Phase = 'lobby' | 'map' | 'combat' | 'event' | 'rest' | 'shop' | 'reward' | 'settlement';
  const [phase, setPhase] = useState<Phase>('lobby');

  // Load
  useEffect(() => {
    loadInitialData();
  }, []);

  async function loadInitialData() {
    setLoading(true);
    const [showsRes, cardsRes, expRes] = await Promise.all([
      apiFetch('/api/shows'),
      apiFetch('/api/cards/my'),
      apiFetch('/api/expedition'),
    ]);
    if (showsRes.code === 200) setShows(showsRes.data || []);
    if (cardsRes.code === 200) setUserCards(cardsRes.data || []);
    if (expRes.code === 200) {
      const data = expRes.data;
      if (data.hasExpedition) {
        setExpedition(data.expedition);
        setHasExpedition(true);
        setNodeType(data.currentNodeType);
        setPhase('map');
        if (data.nodeOptions) setNodeOptions(data.nodeOptions);
        if (data.enemy) setEnemy(data.enemy);
      } else {
        setHasExpedition(false);
        setPhase('lobby');
        if (data.history) setHistory(data.history);
      }
    }
    setLoading(false);
  }

  // Start expedition
  async function handleStartExpedition() {
    if (!selectedShow || selectedCards.length < 5) return;
    const res = await apiFetch('/api/expedition/start', {
      method: 'POST',
      body: JSON.stringify({ showId: selectedShow, deckCardIds: selectedCards }),
    });
    if (res.code === 200) {
      setExpedition(res.data.expedition);
      setHasExpedition(true);
      setPhase('map');
      setNodeType(res.data.expedition.currentNodeType);
      if (res.data.expedition.nodeOptions) setNodeOptions(res.data.expedition.nodeOptions);
    } else {
      alert(res.message);
    }
  }

  // Enter combat node
  async function enterCombat() {
    const res = await apiFetch('/api/expedition/enter-combat', { method: 'POST' });
    if (res.code === 200) {
      setEnemy(res.data.enemy);
      setHandCards(res.data.hand || []);
      setPhase('combat');
    } else {
      alert(res.message || '进入战斗失败，请重试');
    }
  }

  // Play card in combat (click a card to attack - no questions)
  async function handlePlayCard(cardId: number) {
    if (combatLoading) return;
    setCombatLoading(true);
    setAttackingCard(cardId);
    const res = await apiFetch('/api/expedition/play-card', {
      method: 'POST',
      body: JSON.stringify({ cardId }),
    });
    setAttackingCard(null);
    setCombatLoading(false);
    if (res.code === 200) {
      const data = res.data;
      if (data.playerDead) {
        setExpedition(data.expedition);
        setSettlement({ cleared: false, expedition: data.expedition });
        setPhase('settlement');
        return;
      }
      if (data.enemyDefeated) {
        setExpedition(data.expedition);
        setEnemy(null);
        setHandCards([]);
        setRewardChoices(data.rewards || []);
        setFeedback(data.resultText);
        setPhase('reward');
        return;
      }
      // Show damage numbers
      if (data.damageDealt > 0) {
        setDamageNumber({ text: `-${data.damageDealt}`, type: 'damage-dealt' });
        setTimeout(() => setDamageNumber(null), 1000);
      }
      if (data.damageTaken > 0) {
        setDamageNumber({ text: `-${data.damageTaken}`, type: 'damage-taken' });
        setTimeout(() => setDamageNumber(null), 1000);
      }
      if (res.data.expedition) setExpedition(res.data.expedition);
      if (data.enemyRemainingHp !== undefined && enemy) {
        setEnemy({ ...enemy, currentHp: data.enemyRemainingHp });
      }
      setFeedback(data.resultText);
      setTimeout(() => setFeedback(null), 1500);
    } else {
      alert(res.message || '出牌失败');
    }
  }

  // Apply reward
  async function handleApplyReward(choice: any) {
    const res = await apiFetch('/api/expedition/apply-reward', {
      method: 'POST',
      body: JSON.stringify(choice),
    });
    if (res.code === 200) {
      setExpedition(res.data.expedition);
      setRewardChoices([]);

      if (choice.type === 'new_relic' && choice.relic) {
        setRelicPopup({
          id: choice.relic.id,
          nameCn: choice.relic.nameCn,
          nameEn: choice.relic.nameEn,
          descriptionCn: choice.relic.descriptionCn || choice.relic.effectCn || '',
          icon: choice.relic.icon || '🪙',
          rarity: choice.relic.rarity || 'common',
        });
        setTimeout(async () => {
          setRelicPopup(null);
          await moveToNextNode();
        }, 3000);
        return;
      }

      await moveToNextNode();
    }
  }

  async function moveToNextNode() {
    const nextRes = await apiFetch('/api/expedition/next-node', { method: 'POST' });
    if (nextRes.code === 200) {
      if (nextRes.data.cleared) {
        setSettlement({ cleared: true, expedition: nextRes.data.expedition });
        setPhase('settlement');
      } else {
        setExpedition(nextRes.data.expedition);
        setNodeType(nextRes.data.nodeType);
        setNodeOptions(nextRes.data.expedition?.nodeOptions || []);
        setPhase('map');
      }
    }
  }

  // Enter event node
  async function handleEventChoice(choiceIndex: number) {
    const res = await apiFetch('/api/expedition/event', {
      method: 'POST',
      body: JSON.stringify({ choiceIndex }),
    });
    if (res.code === 200) {
      setExpedition(res.data.expedition);
      alert(res.data.resultText || '事件完成');
      const nextRes = await apiFetch('/api/expedition/next-node', { method: 'POST' });
      if (nextRes.code === 200) {
        if (nextRes.data.cleared) {
          setSettlement({ cleared: true, expedition: nextRes.data.expedition });
          setPhase('settlement');
        } else {
          setExpedition(nextRes.data.expedition);
          setNodeType(nextRes.data.nodeType);
          setPhase('map');
        }
      }
    }
  }

  // Enter rest node
  async function handleRest(action: string, cardId?: number) {
    const res = await apiFetch('/api/expedition/rest', {
      method: 'POST',
      body: JSON.stringify({ action, cardId }),
    });
    if (res.code === 200) {
      setExpedition(res.data.expedition);
      const nextRes = await apiFetch('/api/expedition/next-node', { method: 'POST' });
      if (nextRes.code === 200) {
        if (nextRes.data.cleared) {
          setSettlement({ cleared: true, expedition: nextRes.data.expedition });
          setPhase('settlement');
        } else {
          setExpedition(nextRes.data.expedition);
          setNodeType(nextRes.data.nodeType);
          setPhase('map');
        }
      }
    }
  }

  // Enter shop
  async function enterShop() {
    const res = await apiFetch('/api/expedition/shop');
    if (res.code === 200) {
      setShopItems(res.data.items || []);
      setRemoveMode(false);
      setPhase('shop');
    }
  }

  // Choose branch path
  async function handleChoosePath(choiceIndex: number) {
    const res = await apiFetch('/api/expedition/choose-path', {
      method: 'POST',
      body: JSON.stringify({ choiceIndex }),
    });
    if (res.code === 200) {
      setExpedition(res.data.expedition);
      setNodeType(res.data.nodeType);
      setNodeOptions([]);
    } else {
      alert(res.message || '路径选择失败');
    }
  }

  async function handleBuy(type: string, itemId: number, cost: number) {
    if (!expedition || expedition.gold < cost) { alert('金币不足'); return; }
    const res = await apiFetch('/api/expedition/buy', {
      method: 'POST',
      body: JSON.stringify({ type, itemId }),
    });
    if (res.code === 200) {
      setExpedition(res.data.expedition);
      alert('购买成功！');
      const shopRes = await apiFetch('/api/expedition/shop');
      if (shopRes.code === 200) setShopItems(shopRes.data.items || []);
    } else {
      alert(res.message);
    }
  }

  // Leave shop
  async function leaveShop() {
    const nextRes = await apiFetch('/api/expedition/next-node', { method: 'POST' });
    if (nextRes.code === 200) {
      if (nextRes.data.cleared) {
        setSettlement({ cleared: true, expedition: nextRes.data.expedition });
        setPhase('settlement');
      } else {
        setExpedition(nextRes.data.expedition);
        setNodeType(nextRes.data.nodeType);
        setPhase('map');
      }
    }
  }

  // Handle node click (called from ExpeditionMap)
  function handleMapNodeClick(ndx: number) {
    if (!expedition) return;
    const nt = expedition.mapNodes[ndx];
    if (!nt) return;
    switch (nt) {
      case 'combat':
      case 'boss':
        enterCombat();
        break;
      case 'event':
        fetchEventData();
        break;
      case 'rest':
        setPhase('rest');
        break;
      case 'shop':
        enterShop();
        break;
    }
  }

  async function fetchEventData() {
    if (!expedition) return;
    const res = await apiFetch(`/api/expedition`);
    if (res.code === 200 && res.data.events) {
      setEventData(res.data.events);
      setPhase('event');
    }
  }

  // Use potion
  async function handleUsePotion(potionId: number) {
    const res = await apiFetch('/api/expedition/use-potion', {
      method: 'POST',
      body: JSON.stringify({ potionId }),
    });
    if (res.code === 200) {
      if (res.data.expedition) setExpedition(res.data.expedition);
    } else {
      alert(res.message || '使用药水失败');
    }
  }

  // Abandon
  async function handleAbandon() {
    if (!confirm('确定要放弃当前远征吗？')) return;
    const res = await apiFetch('/api/expedition/abandon', { method: 'POST' });
    if (res.code === 200) {
      setHasExpedition(false);
      setExpedition(null);
      setPhase('lobby');
      loadInitialData();
    }
  }

  // Continue after settlement
  function handleSettlementDone() {
    setSettlement(null);
    setHasExpedition(false);
    setExpedition(null);
    setPhase('lobby');
    loadInitialData();
  }

  // Toggle card selection
  function toggleCard(cardId: number) {
    setSelectedCards(prev => {
      if (prev.includes(cardId)) return prev.filter(id => id !== cardId);
      if (prev.length >= 10) return prev;
      return [...prev, cardId];
    });
  }

  // ==================== RENDER HELPERS ====================

  function renderCombat() {
    if (!enemy) return <div className="expedition-empty">加载中...</div>;
    const playerHpPercent = expedition ? (expedition.playerHp / expedition.maxHp) * 100 : 100;

    return (
      <div className="expedition-combat">
        {/* Boss area uses ExpeditionBoss when isBoss */}
        {enemy.isBoss ? (
          <ExpeditionBoss
            boss={enemy}
            playerHp={expedition?.playerHp || 0}
            maxHp={expedition?.maxHp || 1}
            damageNumber={damageNumber}
            feedback={feedback}
          />
        ) : (
          <div className="expedition-enemy-area">
            {damageNumber && (
              <div className={`expedition-damage-number ${damageNumber.type}`}>
                {damageNumber.text}
              </div>
            )}
            <div className="expedition-enemy-name">{enemy.nameCn}</div>
            <div className="expedition-enemy-sub">{enemy.nameEn}</div>
            <div className="expedition-enemy-hp-bar">
              <div className="expedition-enemy-hp-fill" style={{ width: `${(enemy.currentHp / enemy.maxHp) * 100}%` }} />
            </div>
            <div className="expedition-enemy-hp-text">{enemy.currentHp} / {enemy.maxHp}</div>
            {enemy.specialRules && Object.keys(enemy.specialRules).length > 0 && (
              <div className="expedition-special-rules">
                {Object.entries(enemy.specialRules).map(([k, v]) => (
                  <div key={k}>{k}: {String(v)}</div>
                ))}
              </div>
            )}
          </div>
        )}

        <div className="expedition-player-area">
          <div className="expedition-player-hp-bar">
            <div className="expedition-player-hp-fill" style={{ width: `${playerHpPercent}%` }} />
          </div>
          <div className="expedition-player-stats">
            <span>❤️ {expedition?.playerHp}/{expedition?.maxHp}</span>
            <span>🪙 {expedition?.gold}</span>
            <span>☠️ {expedition?.enemiesKilled}</span>
          </div>
        </div>

        {feedback && (
          <div style={{ textAlign: 'center', margin: '8px 0', fontSize: 13, fontWeight: 600, color: '#4ade80' }}>{feedback}</div>
        )}

        {/* Potion bar in combat */}
        <ExpeditionPotion
          potions={expedition?.potions || []}
          onUsePotion={handleUsePotion}
          disabled={combatLoading}
        />

        <div style={{ fontSize: 11, color: 'var(--text-secondary)', marginBottom: 4 }}>手牌（点击出牌攻击）</div>
        <div className="expedition-hand">
          {handCards.map((card, i) => (
            <div
              key={i}
              className={`expedition-hand-card ${attackingCard === card.id ? 'attacking' : ''}`}
              onClick={() => !combatLoading && handlePlayCard(card.id)}
              style={{ cursor: combatLoading ? 'wait' : 'pointer', opacity: combatLoading ? 0.6 : 1 }}
            >
              <div className="expedition-hand-card-name">{card.nameCn}</div>
              <div className="expedition-hand-card-attack">⚔️{card.attack}</div>
              <div className="expedition-hand-card-cost">费用:{card.cost}</div>
            </div>
          ))}
        </div>
      </div>
    );
  }

  function renderShop() {
    if (!expedition) return null;
    const deck = expedition.deck || [];
    return (
      <div className="expedition-shop">
        <div className="expedition-shop-header">
          <span>🛒 商店</span>
          <span className="expedition-shop-gold">🪙 {expedition.gold}</span>
        </div>
        <div className="expedition-shop-items">
          {shopItems.map((item: any, i: number) => (
            <div key={i} className="expedition-shop-item">
              <div className="expedition-shop-item-info">
                <div className="expedition-shop-item-name">{item.nameCn || item.type}</div>
                <div className="expedition-shop-item-desc">
                  {item.type === 'card' && `⚔️${item.attack || 0}  ${item.rarity}`}
                  {item.type === 'relic' && item.effectCn}
                  {item.type === 'heal' && `恢复${item.healAmount}点血量`}
                  {item.type === 'remove' && (item.description || '移除一张卡牌')}
                </div>
              </div>
              <button
                className="expedition-shop-buy-btn"
                disabled={expedition.gold < (item.cost || 999)}
                onClick={() => {
                  if (item.type === 'remove') {
                    setRemoveMode(true);
                  } else {
                    handleBuy(item.type, item.id, item.cost || 999);
                  }
                }}
              >
                🪙{item.cost || '?'}
              </button>
            </div>
          ))}
        </div>
        {removeMode && (
          <div className="expedition-remove-section" style={{ marginTop: 12 }}>
            <div style={{ color: 'var(--text-secondary)', fontSize: 13, marginBottom: 8 }}>
              选择要移除的卡牌（消耗30金币）：
            </div>
            <div className="expedition-hand" style={{ flexWrap: 'wrap' }}>
              {deck.map((card: any, i: number) => (
                <div
                  key={i}
                  className="expedition-hand-card"
                  style={{ cursor: 'pointer' }}
                  onClick={() => {
                    if (expedition.gold < 30) { alert('金币不足'); return; }
                    handleBuy('remove', card.id, 30);
                    setRemoveMode(false);
                  }}
                >
                  <div className="expedition-hand-card-name">{card.nameCn}</div>
                  <div className="expedition-hand-card-attack">⚔️{card.attack || 0}</div>
                  <div style={{ fontSize: 10, color: '#ef4444' }}>点击移除</div>
                </div>
              ))}
            </div>
            <button
              className="expedition-start-btn"
              style={{ marginTop: 8 }}
              onClick={() => setRemoveMode(false)}
            >
              取消
            </button>
          </div>
        )}
        <button className="expedition-start-btn" onClick={leaveShop} style={{ marginTop: 12 }}>
          离开商店
        </button>
      </div>
    );
  }

  function renderSettlement() {
    if (!settlement || !settlement.expedition) return null;
    const exp = settlement.expedition;
    const isCleared = settlement.cleared;
    const accuracy = exp.questionsTotal > 0 ? Math.round((exp.questionsAnswered / exp.questionsTotal) * 100) : 0;

    return (
      <div className="expedition-result-overlay">
        <div className="expedition-result-card">
          <div className="expedition-settlement-icon">{isCleared ? '🏆' : '💀'}</div>
          <div className="expedition-settlement-title">
            {isCleared ? '远征胜利！' : '远征失败'}
          </div>
          <div className="expedition-settlement-stats">
            <div className="expedition-settlement-stat">
              <div className="expedition-settlement-stat-value">{exp.maxAct}</div>
              <div className="expedition-settlement-stat-label">到达层数</div>
            </div>
            <div className="expedition-settlement-stat">
              <div className="expedition-settlement-stat-value">{exp.enemiesKilled}</div>
              <div className="expedition-settlement-stat-label">击杀数</div>
            </div>
            <div className="expedition-settlement-stat">
              <div className="expedition-settlement-stat-value">{exp.questionsAnswered}</div>
              <div className="expedition-settlement-stat-label">出牌数</div>
            </div>
          </div>
          <button className="expedition-settlement-btn" onClick={handleSettlementDone} style={{ marginTop: 16 }}>
            返回
          </button>
        </div>
      </div>
    );
  }

  function renderHistory() {
    if (history.length === 0) {
      return <div className="expedition-empty">暂无远征记录</div>;
    }
    return (
      <div className="expedition-history-list">
        {history.map((h: any, i: number) => (
          <div key={i} className="expedition-history-item">
            <div>
              <div>第{h.act}层 · 击杀{h.enemiesKilled} · 出牌{h.questionsAnswered}/{h.questionsTotal}</div>
              <div style={{ fontSize: 11, color: 'var(--text-secondary)' }}>
                {h.createdAt ? new Date(h.createdAt).toLocaleDateString() : ''}
              </div>
            </div>
            <span className={`expedition-history-status ${h.status === 'cleared' ? 'cleared' : 'dead'}`}>
              {h.status === 'cleared' ? '通关' : '阵亡'}
            </span>
          </div>
        ))}
      </div>
    );
  }

  function renderLobby() {
    const availableCards: any[] = [];
    if (userCards && Array.isArray(userCards)) {
      userCards.forEach((uc: any) => {
        const card = uc.card || uc;
        if (card.id) availableCards.push(card);
      });
    }

    return (
      <div className="expedition-lobby">
        <div className="expedition-lobby-title">🗡️ 远征出发</div>

        {!selectedShow ? (
          <>
            <p style={{ textAlign: 'center', color: 'var(--text-secondary)', fontSize: 13, marginBottom: 8 }}>
              选择剧集
            </p>
            <div className="expedition-show-select">
              {shows.map((show: any) => (
                <button
                  key={show.id}
                  className={`expedition-show-btn ${selectedShow === show.id ? 'selected' : ''}`}
                  onClick={() => setSelectedShow(show.id)}
                >
                  <div style={{ fontSize: 24, marginBottom: 4 }}>{show.name?.includes('Game of Thrones') ? '🐉' : '🏰'}</div>
                  <div>{show.name || '未知剧集'}</div>
                </button>
              ))}
            </div>
          </>
        ) : (
          <>
            <div className="expedition-deck-section">
              <h3>选择起始卡牌（{selectedCards.length}/10）</h3>
              <div className="expedition-card-grid">
                {availableCards.map((card: any) => (
                  <div
                    key={card.id}
                    className={`expedition-card-item ${selectedCards.includes(card.id) ? 'selected' : ''} expedition-card-rarity-${card.rarity || 'common'}`}
                    onClick={() => toggleCard(card.id)}
                  >
                    <div className="expedition-card-name">{card.nameCn}</div>
                    <div className="expedition-card-info">
                      <span>⚔️{card.attack || 0}</span>
                      <span>{card.rarity}</span>
                    </div>
                  </div>
                ))}
              </div>
            </div>

            <div style={{ display: 'flex', gap: 8, marginTop: 8 }}>
              <button
                className="expedition-start-btn"
                style={{ flex: 1, padding: 10, fontSize: 14 }}
                onClick={() => { setSelectedShow(null); setSelectedCards([]); }}
              >
                返回
              </button>
              <button
                className="expedition-start-btn"
                style={{ flex: 1, padding: 10, fontSize: 14 }}
                disabled={selectedCards.length < 5}
                onClick={handleStartExpedition}
              >
                出发！（{selectedCards.length}张）
              </button>
            </div>
          </>
        )}
      </div>
    );
  }

  // Main render
  if (loading) {
    return <div className="expedition-page"><div className="expedition-empty">加载中...</div></div>;
  }

  return (
    <div className="expedition-page">
      <div className="expedition-tabs">
        <button className={`expedition-tab ${tab === 'current' ? 'active' : ''}`} onClick={() => setTab('current')}>
          当前远征
        </button>
        <button className={`expedition-tab ${tab === 'history' ? 'active' : ''}`} onClick={() => {
          setTab('history');
          if (tab !== 'history') loadInitialData();
        }}>
          远征历史
        </button>
      </div>

      {tab === 'history' ? renderHistory() : (
        <>
          {hasExpedition && expedition && (
            <>
              {/* New: ExpeditionStatus component replaces inline status bar */}
              <ExpeditionStatus expedition={expedition} onAbandon={handleAbandon} />

              {/* New: ExpeditionPotion component */}
              <ExpeditionPotion
                potions={expedition.potions || []}
                onUsePotion={handleUsePotion}
                disabled={phase === 'combat'}
              />
            </>
          )}

          {phase === 'lobby' && renderLobby()}

          {phase === 'map' && expedition && (
            <ExpeditionMap
              mapNodes={expedition.mapNodes || []}
              currentNodeIndex={expedition.node - 1}
              nodeType={nodeType}
              nodeOptions={nodeOptions}
              onNodeClick={handleMapNodeClick}
              onChoosePath={handleChoosePath}
              act={expedition.act}
            />
          )}

          {phase === 'combat' && renderCombat()}

          {phase === 'event' && (
            <ExpeditionEvent eventData={eventData} onChoice={handleEventChoice} />
          )}

          {phase === 'rest' && expedition && (
            <ExpeditionCampfire
              playerHp={expedition.playerHp}
              maxHp={expedition.maxHp}
              deck={expedition.deck || []}
              onRest={handleRest}
            />
          )}

          {phase === 'shop' && renderShop()}

          {phase === 'reward' && (
            <ExpeditionReward rewardChoices={rewardChoices} onChoose={handleApplyReward} />
          )}

          {phase === 'settlement' && renderSettlement()}

          {relicPopup && (
            <div className="expedition-result-overlay" onClick={() => setRelicPopup(null)}>
              <div className="expedition-result-card expedition-relic-popup-card">
                <div className="expedition-relic-popup-icon">{relicPopup.icon}</div>
                <div className="expedition-relic-popup-title">获得遗物！</div>
                <div className="expedition-relic-popup-name">{relicPopup.nameCn}</div>
                <div className={`expedition-relic-popup-rarity expedition-relic-${relicPopup.rarity}`}>
                  {relicPopup.rarity === 'legendary' ? '传说' : relicPopup.rarity === 'epic' ? '史诗' : relicPopup.rarity === 'rare' ? '稀有' : '普通'}
                </div>
                <div className="expedition-relic-popup-desc">{relicPopup.descriptionCn}</div>
                <button className="expedition-start-btn" style={{ marginTop: 12 }} onClick={() => setRelicPopup(null)}>
                  确认
                </button>
              </div>
            </div>
          )}
        </>
      )}
    </div>
  );
}
