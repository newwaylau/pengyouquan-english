import React, { useState, useEffect, useCallback } from 'react';
import './expedition.css';

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
  deck: any[]; relics: any[];
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

  // Question state
  const [currentSentence, setCurrentSentence] = useState<any>(null);
  const [answer, setAnswer] = useState('');
  const [feedback, setFeedback] = useState<string | null>(null);
  const [damageNumber, setDamageNumber] = useState<{ text: string; type: string } | null>(null);

  // Node interaction
  const [nodeType, setNodeType] = useState<string>('');

  // Event state
  const [eventData, setEventData] = useState<any>(null);

  // Shop state
  const [shopItems, setShopItems] = useState<any[]>([]);

  // Reward state
  const [rewardChoices, setRewardChoices] = useState<any[]>([]);

  // Settlement state
  const [settlement, setSettlement] = useState<{ cleared: boolean; expedition: any } | null>(null);

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
      fetchSentence();
    }
  }

  // Fetch random sentence
  async function fetchSentence() {
    const res = await apiFetch('/api/expedition/sentence');
    if (res.code === 200) setCurrentSentence(res.data);
  }

  // Submit answer
  async function handleSubmitAnswer() {
    if (!currentSentence || !answer.trim()) return;
    const correct = answer.trim().toLowerCase() === currentSentence.text?.trim().toLowerCase();
    const res = await apiFetch('/api/expedition/answer', {
      method: 'POST',
      body: JSON.stringify({ sentenceId: currentSentence.id, answer, correct }),
    });
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
        if (data.isBoss && data.bossDefeated) {
          // Boss defeated - next node
          setRewardChoices(data.rewards || []);
        } else {
          setRewardChoices(data.rewards || []);
        }
        setFeedback(data.resultText);
        setPhase('reward');
        setAnswer('');
        return;
      }
      // Show damage
      if (data.damageDealt > 0) {
        setDamageNumber({ text: `-${data.damageDealt}`, type: 'damage-dealt' });
        setTimeout(() => setDamageNumber(null), 1000);
      }
      if (data.damageTaken > 0) {
        setDamageNumber({ text: `-${data.damageTaken}`, type: 'damage-taken' });
        setTimeout(() => setDamageNumber(null), 1000);
      }
      if (res.data.expedition) setExpedition(res.data.expedition);
      if (res.data.enemyRemainingHp !== undefined && enemy) {
        setEnemy({ ...enemy, currentHp: res.data.enemyRemainingHp });
      }
      setFeedback(correct ? '✅ 答对了！' : '❌ 答错了');
      setTimeout(() => setFeedback(null), 1500);
      setAnswer('');
      fetchSentence();
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
      // Move to next node or show result
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
      setPhase('shop');
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
      // Refresh shop
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

  // Handle node click
  function handleNodeClick(ndx: number, nodeType: string, completed: boolean, isCurrent: boolean) {
    if (completed || !isCurrent) return;
    switch (nodeType) {
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

  function renderNodeIcon(type: string) {
    return NODE_ICONS[type] || '⚪';
  }

  function renderMap() {
    if (!expedition) return null;
    const nodes = expedition.mapNodes || [];
    return (
      <div className="expedition-map">
        <div className="expedition-map-act">{ACT_LABELS[expedition.act] || `第${expedition.act}层`}</div>
        <div className="expedition-map-container">
          {nodes.map((nt: string, ndx: number) => {
            const isCompleted = ndx + 1 < expedition.node;
            const isCurrent = ndx + 1 === expedition.node;
            const isFuture = ndx + 1 > expedition.node;
            return (
              <React.Fragment key={ndx}>
                <div
                  className={`expedition-node ${isCompleted ? 'completed' : isCurrent ? 'current' : 'future'}`}
                  onClick={() => handleNodeClick(ndx, nt, isCompleted || isFuture, isCurrent)}
                >
                  <span>{renderNodeIcon(nt)}</span>
                  <span>{NODE_LABELS[nt] || nt}</span>
                </div>
                {ndx < nodes.length - 1 && (
                  <div className={`expedition-node-line ${isCompleted ? 'completed' : ''}`} />
                )}
              </React.Fragment>
            );
          })}
        </div>
      </div>
    );
  }

  function renderCombat() {
    if (!enemy) return <div className="expedition-empty">加载中...</div>;
    const hpPercent = enemy.maxHp > 0 ? (enemy.currentHp / enemy.maxHp) * 100 : 0;
    const playerHpPercent = expedition ? (expedition.playerHp / expedition.maxHp) * 100 : 100;

    return (
      <div className="expedition-combat">
        {damageNumber && (
          <div className={`expedition-damage-number ${damageNumber.type}`}>
            {damageNumber.text}
          </div>
        )}

        <div className="expedition-enemy-area">
          {enemy.isBoss && <div className="expedition-boss-tag">BOSS</div>}
          <div className="expedition-enemy-name">{enemy.nameCn}</div>
          <div className="expedition-enemy-sub">{enemy.nameEn}</div>
          <div className="expedition-enemy-hp-bar">
            <div className="expedition-enemy-hp-fill" style={{ width: `${hpPercent}%` }} />
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

        <div className="expedition-question-area">
          {currentSentence ? (
            <>
              <div className="expedition-question-text">{currentSentence.text}</div>
              <div style={{ fontSize: 12, color: 'var(--text-secondary)', textAlign: 'center', marginBottom: 8 }}>
                {currentSentence.translation}
              </div>
            </>
          ) : (
            <div className="expedition-question-text">加载题目中...</div>
          )}
          <div className="expedition-question-input-row">
            <input
              className="expedition-question-input"
              value={answer}
              onChange={e => setAnswer(e.target.value)}
              onKeyDown={e => { if (e.key === 'Enter') handleSubmitAnswer(); }}
              placeholder="输入字幕原文..."
              autoFocus
            />
            <button className="expedition-question-submit" onClick={handleSubmitAnswer} disabled={!answer.trim()}>
              答题
            </button>
          </div>
          {feedback && (
            <div style={{ textAlign: 'center', marginTop: 8, fontSize: 13, fontWeight: 600 }}>{feedback}</div>
          )}
        </div>

        {handCards.length > 0 && (
          <div>
            <div style={{ fontSize: 11, color: 'var(--text-secondary)', marginBottom: 4 }}>手牌 (答对使用)</div>
            <div className="expedition-hand">
              {handCards.map((card, i) => (
                <div key={i} className="expedition-hand-card">
                  <div className="expedition-hand-card-name">{card.nameCn}</div>
                  <div className="expedition-hand-card-attack">⚔️{card.attack}</div>
                  <div className="expedition-hand-card-cost">费用:{card.cost}</div>
                </div>
              ))}
            </div>
          </div>
        )}
      </div>
    );
  }

  function renderEvent() {
    if (!eventData || !Array.isArray(eventData)) return <div className="expedition-empty">加载中...</div>;
    return (
      <div className="expedition-event">
        <div className="expedition-event-title">❓ 事件</div>
        <div className="expedition-event-desc">
          {/* Events might not have title display; use generic */}
          你遇到了一个事件，请做出选择：
        </div>
        <div className="expedition-event-choices">
          {eventData.map((choice: any, i: number) => (
            <button key={i} className="expedition-reward-btn" onClick={() => handleEventChoice(i)}>
              {choice.text || choice.label || `选项${i + 1}`}
            </button>
          ))}
        </div>
      </div>
    );
  }

  function renderRest() {
    if (!expedition) return null;
    return (
      <div className="expedition-rest">
        <div className="expedition-rest-title">🔥 休息</div>
        <p style={{ color: 'var(--text-secondary)', fontSize: 13 }}>
          当前血量：{expedition.playerHp}/{expedition.maxHp}
        </p>
        <div className="expedition-rest-buttons">
          <button className="expedition-rest-btn" onClick={() => handleRest('heal')}>
            ❤️ 回血
            <div className="expedition-rest-heal-amount">+{Math.ceil(expedition.maxHp * 0.3)}</div>
          </button>
          <button className="expedition-rest-btn" onClick={() => {
            const cardId = expedition.deck[expedition.deck.length - 1]?.id;
            if (cardId) handleRest('upgrade', cardId);
          }}>
            ⬆️ 升级卡牌
            <div className="expedition-rest-heal-amount">攻击+2</div>
          </button>
        </div>
      </div>
    );
  }

  function renderShop() {
    if (!expedition) return null;
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
                </div>
              </div>
              <button
                className="expedition-shop-buy-btn"
                disabled={expedition.gold < (item.cost || 999)}
                onClick={() => handleBuy(item.type, item.id, item.cost || 999)}
              >
                🪙{item.cost || '?'}
              </button>
            </div>
          ))}
        </div>
        <button className="expedition-start-btn" onClick={leaveShop} style={{ marginTop: 12 }}>
          离开商店
        </button>
      </div>
    );
  }

  function renderReward() {
    return (
      <div className="expedition-result-overlay">
        <div className="expedition-result-card">
          <div className="expedition-result-icon">🎁</div>
          <div className="expedition-result-title">击败敌人！</div>
          <div className="expedition-result-text">选择一个奖励：</div>
          <div className="expedition-rewards">
            {rewardChoices.map((choice: any, i: number) => (
              <button key={i} className="expedition-reward-btn" onClick={() => handleApplyReward(choice)}>
                {choice.label || '选择'}
              </button>
            ))}
          </div>
        </div>
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
              <div className="expedition-settlement-stat-value">{accuracy}%</div>
              <div className="expedition-settlement-stat-label">正确率</div>
            </div>
            <div className="expedition-settlement-stat">
              <div className="expedition-settlement-stat-value">{exp.questionsAnswered}</div>
              <div className="expedition-settlement-stat-label">答对题数</div>
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
              <div>第{h.act}层 · 击杀{h.enemiesKilled} · 答对{h.questionsAnswered}/{h.questionsTotal}</div>
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
    // Get user's available cards
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
              {shows.filter((s: any) => s.id === 1 || s.id === 2).map((show: any) => (
                <button
                  key={show.id}
                  className={`expedition-show-btn ${selectedShow === show.id ? 'selected' : ''}`}
                  onClick={() => setSelectedShow(show.id)}
                >
                  <div style={{ fontSize: 24, marginBottom: 4 }}>{show.id === 1 ? '🐉' : '🏰'}</div>
                  <div>{show.name || (show.id === 1 ? 'Game of Thrones' : 'Downton Abbey')}</div>
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
            <div className="expedition-active-bar">
              <span className="act-label">
                {ACT_LABELS[expedition.act] || `第${expedition.act}层`} · 节点 {expedition.node}/{expedition.mapNodes?.length || 0}
              </span>
              <span>❤️{expedition.playerHp}/{expedition.maxHp} 🪙{expedition.gold}</span>
              <button className="expedition-abandon-btn" onClick={handleAbandon}>放弃</button>
            </div>
          )}

          {phase === 'lobby' && renderLobby()}
          {phase === 'map' && renderMap()}
          {phase === 'combat' && renderCombat()}
          {phase === 'event' && renderEvent()}
          {phase === 'rest' && renderRest()}
          {phase === 'shop' && renderShop()}
          {phase === 'reward' && renderReward()}
          {phase === 'settlement' && renderSettlement()}
        </>
      )}
    </div>
  );
}
