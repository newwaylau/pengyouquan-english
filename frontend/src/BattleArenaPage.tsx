import React, { useState, useEffect, useRef, useCallback } from 'react';
import BattleWebSocket from './api/battleWebSocket';
import './battle-arena.css';

// ===================== 关键词配置 =====================
const KEYWORD_CONFIG: Record<string, { label: string; color: string }> = {
  taunt: { label: '嘲讽', color: '#e74c3c' },
  divine_shield: { label: '圣盾', color: '#f1c40f' },
  deathrattle: { label: '亡语', color: '#8e44ad' },
  battlecry: { label: '战吼', color: '#3498db' },
  stealth: { label: '潜行', color: '#2ecc71' },
  rush: { label: '突袭', color: '#e67e22' },
  charge: { label: '冲锋', color: '#e67e22' },
};

// 解析关键词数组
function parseKeywords(kw: any): string[] {
  if (!kw) return [];
  if (Array.isArray(kw)) return kw;
  try { const p = JSON.parse(kw); return Array.isArray(p) ? p : []; } catch { return []; }
}

// ===================== Types =====================

interface CardData {
  cardId: number;
  nameCn: string;
  nameEn: string;
  cardType: string;
  rarity: string;
  cost: number;
  attack: number;
  health: number;
  baseAttack: number;
  baseHealth: number;
  canAttack: boolean;
  hasTaunt: boolean;
  keywords?: any;
}

interface GameState {
  myHealth: number;
  myMana: number;
  myMaxMana: number;
  myBoard: CardData[];
  myDeckCount: number;
  myHandCount: number;
  opponentHealth: number;
  opponentBoard: CardData[];
  opponentHandCount: number;
  opponentDeckCount: number;
  turnNumber: number;
  currentPlayerId: number;
  isMyTurn: boolean;
}

interface BattleState {
  phase: 'idle' | 'matching' | 'playing' | 'finished';
  sessionId: string | null;
  matchStartTime: number;

  // 己方
  myHealth: number;
  myMana: number;
  myMaxMana: number;
  myHand: CardData[];
  myBoard: CardData[];
  myDeckCount: number;

  // 对手
  opponentId: number | null;
  opponentName: string;
  opponentHealth: number;
  opponentBoard: CardData[];
  opponentHandCount: number;
  opponentDeckCount: number;
  opponentTrophies: number;

  // 回合
  turnNumber: number;
  isMyTurn: boolean;
  turnTimer: number;

  // 对战结果
  result: 'win' | 'lose' | null;
  resultData: any;

  // 连接状态
  wsConnected: boolean;

  // 攻击模式
  attackMode: boolean;
  selectedAttacker: CardData | null;

  // 提示信息
  battleLog: string[];
}

// ===================== Component =====================

export default function BattleArenaPage({
  user,
  onBack,
}: {
  user: any;
  onBack: () => void;
}) {
  const getToken = () => localStorage.getItem('token') || '';

  const [state, setState] = useState<BattleState>({
    phase: 'idle',
    sessionId: null,
    matchStartTime: 0,

    myHealth: 30,
    myMana: 0,
    myMaxMana: 0,
    myHand: [],
    myBoard: [],
    myDeckCount: 0,

    opponentId: null,
    opponentName: '',
    opponentHealth: 30,
    opponentBoard: [],
    opponentHandCount: 0,
    opponentDeckCount: 0,
    opponentTrophies: 0,

    turnNumber: 0,
    isMyTurn: false,
    turnTimer: 0,

    result: null,
    resultData: null,

    wsConnected: false,

    attackMode: false,
    selectedAttacker: null,

    battleLog: [],
  });

  const wsRef = useRef<BattleWebSocket | null>(null);
  const timerRef = useRef<ReturnType<typeof setInterval> | null>(null);
  const turnTimerRef = useRef<ReturnType<typeof setInterval> | null>(null);

  // ===================== WebSocket 连接 =====================

  useEffect(() => {
    const token = getToken();
    if (!token) return;

    const ws = new BattleWebSocket(token);
    wsRef.current = ws;

    ws.setCallbacks({
      onMatchFound: (data: any) => {
        const payload = data.payload;
        setState(prev => ({
          ...prev,
          phase: 'playing',
          sessionId: payload.sessionId,
          opponentId: payload.opponentId,
          opponentName: payload.opponentName,
          opponentTrophies: payload.opponentTrophies,
          battleLog: [`⚔️ 匹配到对手: ${payload.opponentName}`],
        }));
      },
      onGameStart: (data: any) => {
        const payload = data.payload;
        setState(prev => ({
          ...prev,
          sessionId: payload.sessionId,
          myHealth: payload.you?.health ?? 30,
          myHand: payload.hand || [],
          myDeckCount: payload.you?.deckCount ?? 0,
          opponentName: payload.opponent?.nickname ?? prev.opponentName,
          opponentHealth: payload.opponent?.health ?? 30,
          opponentDeckCount: payload.opponent?.deckCount ?? 0,
          myMana: payload.startingMana ?? 3,
          myMaxMana: payload.startingMana ?? 3,
          isMyTurn: payload.goingFirst,
          battleLog: [...prev.battleLog, '🎮 游戏开始！'],
        }));
      },
      onTurnStart: (data: any) => {
        const payload = data.payload;
        setState(prev => ({
          ...prev,
          turnNumber: payload.turnNumber,
          myMana: payload.mana,
          myMaxMana: payload.maxMana,
          myHand: payload.hand || prev.myHand,
          isMyTurn: true,
          attackMode: false,
          selectedAttacker: null,
          battleLog: [...prev.battleLog, `📋 回合 ${payload.turnNumber} 开始`],
        }));
        startTurnTimer();
      },
      onQuestion: (_data: any) => {},
      onCardPlayResult: (data: any) => {
        const payload = data.payload;
        if (payload.success) {
          setState(prev => ({
            ...prev,
            myMana: payload.manaRemaining,
            battleLog: [...prev.battleLog, `✅ 出牌成功`],
          }));
        }
      },
      onAttackDeclared: (_data: any) => {},
      onDefenseQuestion: (_data: any) => {},
      onAttackResult: (data: any) => {
        const payload = data.payload;
        setState(prev => ({
          ...prev,
          battleLog: [...prev.battleLog, `⚡ 攻击造成 ${payload.damage || 0} 点伤害`],
        }));
      },
      onDefenseResult: (_data: any) => {},
      onGameState: (data: any) => {
        const payload = data.payload;
        setState(prev => ({
          ...prev,
          myHealth: payload.myHealth,
          myMana: payload.myMana,
          myMaxMana: payload.myMaxMana,
          myBoard: payload.myBoard || [],
          myDeckCount: payload.myDeckCount,
          opponentHealth: payload.opponentHealth,
          opponentBoard: payload.opponentBoard || [],
          opponentHandCount: payload.opponentHandCount,
          opponentDeckCount: payload.opponentDeckCount,
          turnNumber: payload.turnNumber,
          isMyTurn: payload.isMyTurn,
        }));
      },
      onGameOver: (data: any) => {
        const payload = data.payload;
        const isWin = payload.result === 'win';
        setState(prev => ({
          ...prev,
          phase: 'finished',
          result: isWin ? 'win' : 'lose',
          resultData: payload,
          battleLog: [...prev.battleLog, isWin ? '🏆 胜利!' : '💀 失败'],
        }));
        stopTurnTimer();
      },
      onCombo: (_data: any) => {},
      onError: (_data: any) => {},
      onOpponentAction: (data: any) => {
        setState(prev => ({
          ...prev,
          isMyTurn: false,
        }));
      },
      onOpponentTurn: (_data: any) => {
        setState(prev => ({
          ...prev,
          isMyTurn: false,
        }));
      },
    });

    ws.connect();
    setState(prev => ({ ...prev, wsConnected: false }));

    const connectTimer = setInterval(() => {
      if (wsRef.current?.isConnected()) {
        setState(prev => ({ ...prev, wsConnected: true }));
        clearInterval(connectTimer);
      }
    }, 500);

    return () => {
      clearInterval(connectTimer);
      ws.disconnect();
      stopTurnTimer();
    };
  }, []);

  // ===================== 计时器 =====================

  const startTurnTimer = () => {
    stopTurnTimer();
    let seconds = 60;
    setState(prev => ({ ...prev, turnTimer: seconds }));
    turnTimerRef.current = setInterval(() => {
      seconds--;
      setState(prev => ({ ...prev, turnTimer: seconds }));
      if (seconds <= 0) {
        stopTurnTimer();
        handleEndTurn();
      }
    }, 1000);
  };

  const stopTurnTimer = () => {
    if (turnTimerRef.current) {
      clearInterval(turnTimerRef.current);
      turnTimerRef.current = null;
    }
  };

  // ===================== 匹配 =====================

  const handleStartMatching = () => {
    setState(prev => ({
      ...prev,
      phase: 'matching',
      matchStartTime: Date.now(),
      battleLog: ['🔍 正在匹配对手...'],
    }));
    wsRef.current?.joinQueue();
    timerRef.current = setInterval(() => {
      setState(prev => ({ ...prev }));
    }, 1000);
  };

  const handleCancelMatching = () => {
    wsRef.current?.cancelQueue();
    setState(prev => ({ ...prev, phase: 'idle', battleLog: [] }));
    if (timerRef.current) {
      clearInterval(timerRef.current);
      timerRef.current = null;
    }
  };

  // ===================== 出牌操作（纯点击，无答题） =====================

  const handlePlayCard = (card: CardData) => {
    if (!state.isMyTurn || state.attackMode) return;
    if (card.cost > state.myMana) return;
    const ws = wsRef.current;
    if (!ws || !state.sessionId) return;
    ws.playCard(state.sessionId, card.cardId);
    // 从手牌中移除（乐观更新）
    setState(prev => ({
      ...prev,
      myHand: prev.myHand.filter(c => c.cardId !== card.cardId),
      myMana: prev.myMana - card.cost,
    }));
  };

  // ===================== 攻击 =====================

  const handleSelectAttacker = (card: CardData) => {
    if (!state.isMyTurn || !card.canAttack) return;
    setState(prev => ({
      ...prev,
      attackMode: !prev.attackMode,
      selectedAttacker: prev.selectedAttacker?.cardId === card.cardId ? null : card,
    }));
  };

  const handleAttackTarget = (targetType: 'minion' | 'hero', targetId?: number) => {
    if (!state.selectedAttacker || !state.sessionId) return;
    const ws = wsRef.current;
    if (!ws) return;
    ws.attack(state.sessionId, state.selectedAttacker.cardId, targetType, targetId);
    setState(prev => ({
      ...prev,
      attackMode: false,
      selectedAttacker: null,
      battleLog: [...prev.battleLog, `⚡ ${prev.selectedAttacker?.nameCn} 发起攻击`],
    }));
  };

  const handleCancelAttack = () => {
    setState(prev => ({
      ...prev,
      attackMode: false,
      selectedAttacker: null,
    }));
  };

  // ===================== 回合控制 =====================

  const handleEndTurn = () => {
    const ws = wsRef.current;
    if (!ws) return;
    ws.endTurn();
    setState(prev => ({
      ...prev,
      isMyTurn: false,
      attackMode: false,
      selectedAttacker: null,
      battleLog: [...prev.battleLog, '⏭️ 结束回合'],
    }));
    stopTurnTimer();
  };

  const handleConcede = () => {
    if (!window.confirm('确定要认输吗？')) return;
    const ws = wsRef.current;
    if (!ws) return;
    ws.concede();
  };

  // ===================== 辅助函数 =====================

  const getRarityColor = (rarity: string) => {
    switch (rarity) {
      case 'legendary': return '#ff8c00';
      case 'epic': return '#a335ee';
      case 'rare': return '#0070dd';
      default: return '#888';
    }
  };

  const formatTime = (seconds: number) => {
    const m = Math.floor(seconds / 60);
    const s = seconds % 60;
    return `${m}:${s.toString().padStart(2, '0')}`;
  };

  const getMatchingWaitTime = () => {
    if (state.phase !== 'matching') return 0;
    return Math.floor((Date.now() - state.matchStartTime) / 1000);
  };

  // ===================== 渲染: 匹配界面 =====================

  if (state.phase === 'idle' || state.phase === 'matching') {
    return (
      <div className="battle-arena">
        <div className="battle-arena-header">
          <button className="battle-arena-back" onClick={onBack}>← 返回</button>
          <span className="battle-arena-title">实时对战</span>
        </div>
        <div className="battle-arena-matching">
          <div className="battle-arena-matching-card">
            {state.phase === 'idle' ? (
              <>
                <div className="battle-arena-matching-icon">⚔️</div>
                <h2 className="battle-arena-matching-title">实时对战</h2>
                <p className="battle-arena-matching-desc">
                  与其他玩家实时卡牌对战！<br />
                  纯策略操作，出牌攻击，赢取奖杯！
                </p>
                <div className="battle-arena-rules">
                  <div className="battle-arena-rule-item">🎯 点击手牌出牌</div>
                  <div className="battle-arena-rule-item">⚡ 点击随从攻击</div>
                  <div className="battle-arena-rule-item">🛡️ 圣盾/嘲讽/潜行关键词生效</div>
                  <div className="battle-arena-rule-item">🏆 胜利+30奖杯，失败-25</div>
                </div>
                <button className="battle-arena-btn battle-arena-btn-primary" onClick={handleStartMatching}>
                  开始匹配
                </button>
              </>
            ) : (
              <>
                <div className="battle-arena-matching-spinner" />
                <h2 className="battle-arena-matching-title">正在匹配...</h2>
                <p className="battle-arena-matching-desc">已等待 {getMatchingWaitTime()} 秒</p>
                <p className="battle-arena-matching-hint">
                  系统将根据奖杯数匹配实力相近的对手
                </p>
                <button className="battle-arena-btn battle-arena-btn-secondary" onClick={handleCancelMatching}>
                  取消匹配
                </button>
              </>
            )}
          </div>
        </div>
      </div>
    );
  }

  // ===================== 渲染: 游戏结束 =====================

  if (state.phase === 'finished') {
    const rd = state.resultData;
    return (
      <div className="battle-arena">
        <div className="battle-arena-finished">
          <div className="battle-arena-result-card">
            <div className={`battle-arena-result-icon ${state.result}`}>
              {state.result === 'win' ? '🏆' : '💀'}
            </div>
            <h2 className={`battle-arena-result-title ${state.result}`}>
              {state.result === 'win' ? '胜利！' : '失败'}
            </h2>
            <div className="battle-arena-result-trophy">
              奖杯变化: {state.result === 'win' ? '+' : ''}{rd?.trophyChange || 0}
            </div>
            <div className="battle-arena-result-stats">
              <div className="battle-arena-result-stat">
                <span className="battle-arena-result-stat-label">胜者</span>
                <span className="battle-arena-result-stat-value">{rd?.winnerStats?.nickname || ''}</span>
                <span className="battle-arena-result-stat-detail">血量: {rd?.winnerStats?.healthRemaining}</span>
              </div>
              <div className="battle-arena-result-divider">VS</div>
              <div className="battle-arena-result-stat">
                <span className="battle-arena-result-stat-label">败者</span>
                <span className="battle-arena-result-stat-value">{rd?.loserStats?.nickname || ''}</span>
                <span className="battle-arena-result-stat-detail">血量: {rd?.loserStats?.healthRemaining}</span>
              </div>
            </div>
            <div className="battle-arena-result-buttons">
              <button className="battle-arena-btn battle-arena-btn-primary" onClick={() => {
                setState(prev => ({ ...prev, phase: 'idle', result: null, resultData: null, battleLog: [] }));
              }}>
                再来一局
              </button>
              <button className="battle-arena-btn battle-arena-btn-secondary" onClick={onBack}>
                返回
              </button>
            </div>
          </div>
        </div>
      </div>
    );
  }

  // ===================== 渲染: 游戏进行中 =====================

  return (
    <div className="battle-arena">
      {/* ====== 对手信息 ====== */}
      <div className="battle-arena-opponent-bar">
        <div className="battle-arena-opponent-info">
          <span className="battle-arena-opponent-name">{state.opponentName}</span>
          <div className="battle-arena-health-bar">
            <div className="battle-arena-health-fill opponent" style={{ width: `${(state.opponentHealth / 30) * 100}%` }} />
            <span className="battle-arena-health-text">♥{state.opponentHealth}</span>
          </div>
        </div>
        <div className="battle-arena-opponent-stats">
          <span className="battle-arena-stat-item">✋{state.opponentHandCount}</span>
          <span className="battle-arena-stat-item">🃏{state.opponentDeckCount}</span>
        </div>
      </div>

      {/* ====== 对手场上随从 ====== */}
      <div className="battle-arena-board opponent-board">
        {state.opponentBoard.map((card, idx) => (
          <div
            key={`opp-${card.cardId}-${idx}`}
            className={`battle-arena-minion ${card.hasTaunt ? 'taunt' : ''}`}
            style={{ borderColor: getRarityColor(card.rarity) }}
            onClick={() => {
              if (state.attackMode && state.selectedAttacker) {
                handleAttackTarget('minion', card.cardId);
              }
            }}
          >
            <div className="battle-arena-minion-cost">{card.cost}</div>
            <div className="battle-arena-minion-name">{card.nameCn}</div>
            <div className="battle-arena-minion-stats">
              <span className="battle-arena-minion-attack">⚔️{card.attack}</span>
              <span className="battle-arena-minion-health">♥{card.health}</span>
            </div>
            {card.hasTaunt && <div className="battle-arena-minion-keyword" style={{ color: '#e74c3c', fontSize: 9, fontWeight: 600 }}>🛡️</div>}
          </div>
        ))}
      </div>

      {/* ====== 回合状态栏 ====== */}
      <div className="battle-arena-turn-bar">
        <span className="battle-arena-turn-number">回合 {state.turnNumber}</span>
        <span className={`battle-arena-turn-status ${state.isMyTurn ? 'my-turn' : ''}`}>
          {state.isMyTurn ? `你的回合 ⏱️${state.turnTimer}s` : '等待对手...'}
        </span>
        {state.isMyTurn && (
          <div className="battle-arena-turn-actions">
            {state.attackMode ? (
              <button className="battle-arena-btn battle-arena-btn-danger-sm" onClick={handleCancelAttack}>
                取消攻击
              </button>
            ) : (
              <button className="battle-arena-btn battle-arena-btn-primary-sm" onClick={handleEndTurn}>
                结束回合
              </button>
            )}
            <button className="battle-arena-btn battle-arena-btn-ghost-sm" onClick={handleConcede}>
              认输
            </button>
          </div>
        )}
      </div>

      {/* ====== 己方场上随从 ====== */}
      <div className="battle-arena-board my-board">
        {state.myBoard.map((card, idx) => (
          <div
            key={`my-${card.cardId}-${idx}`}
            className={`battle-arena-minion ${card.canAttack ? 'can-attack' : ''} ${state.selectedAttacker?.cardId === card.cardId ? 'selected' : ''} ${card.hasTaunt ? 'taunt' : ''}`}
            style={{ borderColor: getRarityColor(card.rarity) }}
            onClick={() => handleSelectAttacker(card)}
          >
            <div className="battle-arena-minion-cost">{card.cost}</div>
            <div className="battle-arena-minion-name">{card.nameCn}</div>
            <div className="battle-arena-minion-stats">
              <span className="battle-arena-minion-attack">⚔️{card.attack}</span>
              <span className="battle-arena-minion-health">♥{card.health}</span>
            </div>
            {card.canAttack && <div className="battle-arena-minion-ready">⚡</div>}
            {/* 关键词徽章 */}
            {(() => {
              const kws = parseKeywords(card.keywords);
              return kws.length > 0 ? (
                <div className="cg-keywords" style={{ marginTop: 2 }}>
                  {kws.map((kw: string) => {
                    const cfg = KEYWORD_CONFIG[kw];
                    return cfg ? (
                      <span key={kw} className="cg-keyword-badge" style={{ background: cfg.color, fontSize: 7 }}>
                        {cfg.label}
                      </span>
                    ) : null;
                  })}
                </div>
              ) : null;
            })()}
          </div>
        ))}
      </div>

      {/* ====== 己方状态栏 ====== */}
      <div className="battle-arena-player-bar">
        <div className="battle-arena-player-health">
          <div className="battle-arena-health-bar">
            <div className="battle-arena-health-fill" style={{ width: `${(state.myHealth / 30) * 100}%` }} />
            <span className="battle-arena-health-text">♥{state.myHealth}</span>
          </div>
        </div>
        <div className="battle-arena-player-mana">
          <span className="battle-arena-mana-text">⚡{state.myMana}/{state.myMaxMana}</span>
        </div>
        <div className="battle-arena-player-deck">
          <span className="battle-arena-deck-text">🃏{state.myDeckCount}</span>
        </div>
      </div>

      {/* ====== 手牌区 ====== */}
      <div className="battle-arena-hand">
        {state.myHand.map((card, idx) => (
          <div
            key={`hand-${card.cardId}-${idx}`}
            className={`battle-arena-hand-card ${card.cost <= state.myMana && state.isMyTurn ? 'playable' : 'unplayable'}`}
            style={{ borderColor: getRarityColor(card.rarity) }}
            onClick={() => handlePlayCard(card)}
          >
            <div className="battle-arena-hand-cost">{card.cost}</div>
            <div className="battle-arena-hand-name">{card.nameCn}</div>
            <div className="battle-arena-hand-type">{card.cardType}</div>
            {/* 关键词徽章 */}
            {(() => {
              const kws = parseKeywords(card.keywords);
              return kws.length > 0 ? (
                <div className="cg-keywords" style={{ marginTop: 1 }}>
                  {kws.map((kw: string) => {
                    const cfg = KEYWORD_CONFIG[kw];
                    return cfg ? (
                      <span key={kw} className="cg-keyword-badge" style={{ background: cfg.color, fontSize: 7 }}>
                        {cfg.label}
                      </span>
                    ) : null;
                  })}
                </div>
              ) : null;
            })()}
            <div className="battle-arena-hand-stats">
              {card.attack > 0 && <span>⚔️{card.attack}</span>}
              {card.health > 0 && <span>♥{card.health}</span>}
            </div>
          </div>
        ))}
      </div>

      {/* ====== 操作提示 ====== */}
      {state.attackMode && (
        <div className="battle-arena-attack-hint">
          选择目标进行攻击
        </div>
      )}

      {/* ====== 战斗日志（浮动小区域） ====== */}
      <div className="battle-arena-log">
        {state.battleLog.slice(-3).map((log, i) => (
          <div key={i} className="battle-arena-log-item">{log}</div>
        ))}
      </div>
    </div>
  );
}
