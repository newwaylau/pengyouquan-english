import React, { useState, useEffect, useRef, useCallback } from 'react';
import BattleWebSocket from './api/battleWebSocket';
import './battle-arena.css';
import './card-keyword-effects.css';
import {
  playCardAnimation,
  elementFlash,
  showDamageNumber,
  hitAnimation,
  screenShake,
  spawnParticles,
  victoryEffect,
  defeatEffect,
  minionChargeAnimation,
  whiteFlash,
  heroPowerEffect,
} from './effects/GaspAnimations';
import { getCardAnimConfig, ELEMENT_STYLES } from './AnimProfileBuilder';

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
  phase: 'idle' | 'matching' | 'mulligan' | 'playing' | 'finished';
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

  // 护甲 & 扩展信息
  myArmor: number;
  myGraveyardCount: number;
  opponentArmor: number;
  opponentSecrets: number;

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

    myArmor: 0,
    myGraveyardCount: 0,
    opponentArmor: 0,
    opponentSecrets: 0,

    battleLog: [],
  });

  const wsRef = useRef<BattleWebSocket | null>(null);
  const timerRef = useRef<ReturnType<typeof setInterval> | null>(null);
  const turnTimerRef = useRef<ReturnType<typeof setInterval> | null>(null);

  // ===================== 动画相关 refs =====================
  const fieldRef = useRef<HTMLDivElement>(null);
  const handCardRefs = useRef<Map<number, HTMLDivElement>>(new Map());
  const myBoardRefs = useRef<Map<number, HTMLDivElement>>(new Map());
  const opponentBoardRefs = useRef<Map<number, HTMLDivElement>>(new Map());
  const lastPlayedCardRef = useRef<CardData | null>(null);
  const lastAttackerRef = useRef<CardData | null>(null);

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
          phase: 'mulligan',
          sessionId: payload.sessionId,
          opponentId: payload.opponentId,
          opponentName: payload.opponentName,
          opponentTrophies: payload.opponentTrophies,
          battleLog: [`⚔️ 匹配到对手: ${payload.opponentName}`],
        }));
      },
      onMulliganStart: (data: any) => {
        const payload = data.payload;
        setState(prev => ({
          ...prev,
          phase: 'mulligan',
          myHand: payload.hand || [],
          myDeckCount: prev.myDeckCount,
          battleLog: [...prev.battleLog, '🔄 选择换牌...'],
        }));
      },
      onMulliganResult: (data: any) => {
        const payload = data.payload;
        if (payload.success) {
          setState(prev => ({
            ...prev,
            myHand: payload.hand || prev.myHand,
          }));
          // 双方都就绪后，等待 onGameStart 进入 PLAYING
          if (payload.bothReady) {
            setState(prev => ({ ...prev, battleLog: [...prev.battleLog, '✅ 换牌完成'] }));
          }
        }
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
          // 播放元素闪屏 + 粒子爆发
          const container = fieldRef.current;
          const lastCard = lastPlayedCardRef.current;
          if (container && lastCard) {
            const config = getCardAnimConfig(lastCard.attack, lastCard.rarity);
            elementFlash(container, config.element);
            const cx = container.offsetWidth / 2;
            const cy = container.offsetHeight / 2;
            spawnParticles(container, cx, cy, config.particleCount, config.element);
          }
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
        const container = fieldRef.current;
        const damage = payload.damage || 0;

        if (container) {
          // 确定目标位置显示伤害数字
          let targetX: number | null = null;
          let targetY: number | null = null;

          if (payload.targetType === 'minion' && payload.targetId != null) {
            // 尝试找到场上的目标随从（对手或己方）
            const targetEl =
              opponentBoardRefs.current.get(payload.targetId) ||
              myBoardRefs.current.get(payload.targetId);
            if (targetEl) {
              const rect = targetEl.getBoundingClientRect();
              const containerRect = container.getBoundingClientRect();
              targetX = rect.left - containerRect.left + rect.width / 2;
              targetY = rect.top - containerRect.top;
              // 受击动画
              hitAnimation(targetEl, damage >= 8 ? 'big' : damage >= 5 ? 'mid' : 'light');
            }
          } else if (payload.targetType === 'hero') {
            // 英雄受击 —— 在对手区域显示伤害数字
            targetX = container.offsetWidth / 2 - 40;
            targetY = 30;
            // 对手头像白闪（大伤害）
            if (damage >= 5) {
              // 尝试获取对手区域元素进行 hitAnimation
              const opponentBar = container.querySelector('.battle-arena-opponent-bar') as HTMLElement | null;
              if (opponentBar) {
                hitAnimation(opponentBar, damage >= 8 ? 'big' : 'mid');
              }
            }
          }

          // 如果没有精确的目标位置，放在战场中央
          if (targetX === null) {
            targetX = container.offsetWidth / 2;
            targetY = container.offsetHeight / 2;
          }

          // 显示伤害数字
          const dmgColor = damage >= 8 ? '#ff4444' : damage >= 5 ? '#ff8844' : '#ffaa44';
          showDamageNumber(container, targetX - 20, targetY - 10, damage, dmgColor);

          // 大伤害带白闪
          if (damage >= 8) {
            whiteFlash(container);
          }

          // 屏幕震动
          screenShake(container, damage);
        }

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

        // 播放胜利/失败特效（延时一点等状态更新）
        const container = fieldRef.current;
        if (container) {
          setTimeout(() => {
            if (isWin) {
              victoryEffect(container);
            } else {
              defeatEffect(container);
            }
          }, 300);
        }
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

  const handlePlayCard = async (card: CardData) => {
    if (!state.isMyTurn || state.attackMode) return;
    if (card.cost > state.myMana) return;
    const ws = wsRef.current;
    if (!ws || !state.sessionId) return;

    // 记录本次出的牌，供后续回调使用
    lastPlayedCardRef.current = card;

    // 出牌动画：手牌飞向战场中心
    const cardEl = handCardRefs.current.get(card.cardId);
    const container = fieldRef.current;
    if (cardEl && container) {
      const config = getCardAnimConfig(card.attack, card.rarity);
      const style = ELEMENT_STYLES[config.element] || ELEMENT_STYLES.fire;
      await playCardAnimation(cardEl, container, style.color);
    }

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

  const handleAttackTarget = async (targetType: 'minion' | 'hero', targetId?: number) => {
    if (!state.selectedAttacker || !state.sessionId) return;
    const ws = wsRef.current;
    if (!ws) return;

    const attacker = state.selectedAttacker;
    lastAttackerRef.current = attacker;

    // 攻击方冲锋动画
    const attackerEl = myBoardRefs.current.get(attacker.cardId);
    if (attackerEl && fieldRef.current) {
      await minionChargeAnimation(attackerEl, attacker.attack);
    }

    ws.attack(state.sessionId, attacker.cardId, targetType, targetId);
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

  // ===================== 渲染: Mulligan 换牌 =====================

  if (state.phase === 'mulligan') {
    const [selectedForReplace, setSelectedForReplace] = React.useState<Set<number>>(new Set());

    const toggleReplace = (cardId: number) => {
      setSelectedForReplace(prev => {
        const next = new Set(prev);
        if (next.has(cardId)) {
          next.delete(cardId);
        } else {
          next.add(cardId);
        }
        return next;
      });
    };

    const confirmMulligan = () => {
      const ws = wsRef.current;
      if (!ws || !state.sessionId) return;
      ws.mulligan(state.sessionId, Array.from(selectedForReplace));
      setState(prev => ({
        ...prev,
        battleLog: [...prev.battleLog, `🔄 换掉 ${selectedForReplace.size} 张牌`],
      }));
    };

    return (
      <div className="battle-arena">
        <div className="battle-arena-header">
          <span className="battle-arena-title">换牌阶段</span>
        </div>
        <div className="battle-arena-matching">
          <div className="battle-arena-matching-card">
            <h2 className="battle-arena-matching-title">选择要换掉的牌</h2>
            <p className="battle-arena-matching-desc">
              点击选择要换掉的卡牌（蓝色高亮），然后确认
            </p>
            <div style={{ display: 'flex', flexWrap: 'wrap', justifyContent: 'center', gap: 8, margin: '16px 0' }}>
              {state.myHand.map(card => (
                <div
                  key={card.cardId}
                  onClick={() => toggleReplace(card.cardId)}
                  style={{
                    width: 80,
                    height: 110,
                    background: selectedForReplace.has(card.cardId)
                      ? 'linear-gradient(180deg, #1e40af, #1e3a5f)'
                      : 'linear-gradient(180deg, #1e293b, #0f172a)',
                    border: selectedForReplace.has(card.cardId)
                      ? '3px solid #3b82f6'
                      : '2px solid #475569',
                    borderRadius: 8,
                    padding: 6,
                    display: 'flex',
                    flexDirection: 'column',
                    alignItems: 'center',
                    justifyContent: 'space-between',
                    cursor: 'pointer',
                    transition: 'all 0.15s',
                    boxShadow: selectedForReplace.has(card.cardId)
                      ? '0 0 16px rgba(59,130,246,0.5)'
                      : 'none',
                  }}
                >
                  <div style={{ fontSize: 10, fontWeight: 800, color: '#e2e8f0' }}>{card.cost}</div>
                  <div style={{ fontSize: 11, fontWeight: 600, color: '#f1f5f9', textAlign: 'center' }}>{card.nameCn}</div>
                  <div style={{ display: 'flex', gap: 6, fontSize: 10 }}>
                    {card.attack > 0 && <span style={{ color: '#ef4444' }}>⚔{card.attack}</span>}
                    {card.health > 0 && <span style={{ color: '#22c55e' }}>❤{card.health}</span>}
                  </div>
                </div>
              ))}
            </div>
            <div style={{ display: 'flex', gap: 12, justifyContent: 'center' }}>
              <button
                className="battle-arena-btn battle-arena-btn-primary"
                onClick={confirmMulligan}
              >
                确认换牌
              </button>
              <button
                className="battle-arena-btn battle-arena-btn-secondary"
                onClick={() => confirmMulligan()}
              >
                全留（跳过）
              </button>
            </div>
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

  // 法力水晶渲染函数
  const renderManaCrystals = (current: number, max: number) => {
    const crystals: JSX.Element[] = [];
    for (let i = 0; i < 10; i++) {
      let cls = 'mana-crystal';
      if (i >= max) cls += ' locked';
      else if (i >= current) cls += ' spent';
      crystals.push(<div key={i} className={cls} />);
    }
    return crystals;
  };

  // 高亮攻击目标：攻击模式下，对手场上可被选中的随从
  const isAttackableTarget = (card: CardData) => {
    return state.attackMode && state.selectedAttacker !== null;
  };

  // 渲染随从卡
  const renderMinionCard = (card: CardData, isMine: boolean, idx: number) => {
    const kws = parseKeywords(card.keywords);
    const kwBadges = kws.length > 0 ? kws.map((kw: string) => {
      const cfg = KEYWORD_CONFIG[kw];
      if (!cfg) return null;
      return (
        <span key={kw} className="cg-keyword-badge" style={{ background: cfg.color }}>
          {cfg.label}
        </span>
      );
    }) : null;

    return (
      <div className="minion-slot" key={`${isMine ? 'my' : 'opp'}-${card.cardId}-${idx}`}>
        <div
          ref={(el) => {
            if (!el) return;
            if (isMine) {
              myBoardRefs.current.set(card.cardId, el);
            } else {
              opponentBoardRefs.current.set(card.cardId, el);
            }
          }}
          className={`minion-card
            ${isMine && card.canAttack ? 'can-attack' : ''}
            ${card.hasTaunt ? 'taunt' : ''}
            ${isMine && state.selectedAttacker?.cardId === card.cardId ? 'selected' : ''}
            ${!isMine && state.attackMode ? 'attackable-target' : ''}
            ${kws.includes('taunt') ? 'minion-taunt' : ''}
            ${kws.includes('divine_shield') ? 'minion-divine-shield' : ''}
            ${kws.includes('stealth') ? 'minion-stealth' : ''}
            ${kws.includes('rush') ? 'minion-rush' : ''}
            ${kws.includes('charge') ? 'minion-charge' : ''}
            ${kws.includes('deathrattle') ? 'minion-deathrattle' : ''}
            ${kws.includes('lifesteal') ? 'minion-lifesteal' : ''}
            ${kws.includes('poisonous') ? 'minion-poisonous' : ''}
            ${kws.includes('windfury') ? 'minion-windfury' : ''}
            ${kws.includes('spell_damage') ? 'minion-spell-damage' : ''}
          `}
          style={{ borderColor: getRarityColor(card.rarity) }}
          onClick={() => {
            if (isMine) {
              handleSelectAttacker(card);
            } else if (state.attackMode && state.selectedAttacker) {
              handleAttackTarget('minion', card.cardId);
            }
          }}
        >
          {/* 费用宝石 */}
          <div className="minion-cost-gem">{card.cost}</div>
          {/* 关键词徽章（右上） */}
          <div className="minion-keyword-badge">
            {card.hasTaunt && <span style={{ fontSize: 10 }}>🛡️</span>}
          </div>
          {/* 随从名 */}
          <div className="minion-name-label">{card.nameCn}</div>
          {/* 关键词 */}
          {kwBadges && (
            <div className="cg-keywords" style={{ marginTop: 'auto' }}>
              {kwBadges}
            </div>
          )}
          {/* 攻击/生命 */}
          <div className="minion-bottom-stats">
            <span className="minion-atk">{card.attack}</span>
            <span className="minion-hp">{card.health}</span>
          </div>
          {isMine && card.canAttack && (
            <div className="minion-keyword-badge" style={{ position: 'absolute', top: -4, left: '50%', transform: 'translateX(-50%)', fontSize: 12 }}>
              ⚡
            </div>
          )}
        </div>
      </div>
    );
  };

  const heroRarity = 'epic';
  const heroRarityClass = heroRarity;

  return (
    <div className="battle-arena" ref={fieldRef}>
      {/* ====== 顶部：对手信息区域 ====== */}
      <div className="battle-arena-opponent-area">
        <div className="opponent-portrait-area">
          <div className={`hero-portrait ${heroRarityClass}`}>
            <span className="hero-icon">🧙</span>
          </div>
          <div>
            <div className="opponent-name">{state.opponentName || '对手'}</div>
            <div style={{ display: 'flex', alignItems: 'center', gap: 6 }}>
              <span className="hero-hp">{state.opponentHealth}</span>
              {state.opponentArmor > 0 && (
                <span className="hero-armor">🛡️{state.opponentArmor}</span>
              )}
            </div>
          </div>
        </div>
        <div className="opponent-meta-stats">
          {/* 手牌数量 */}
          <div className="opponent-stat-badge">
            <span className="stat-icon">✋</span>
            <span>{state.opponentHandCount}</span>
          </div>
          {/* 牌库数量 */}
          <div className="opponent-stat-badge">
            <span className="stat-icon">🃏</span>
            <span>{state.opponentDeckCount}</span>
          </div>
          {/* 奥秘槽位 */}
          {state.opponentSecrets > 0 && (
            <div className="opponent-secrets">
              {Array.from({ length: state.opponentSecrets }).map((_, i) => (
                <div key={i} className="secret-slot">?</div>
              ))}
            </div>
          )}
        </div>
      </div>

      {/* ====== 回合计时器（进度条） ====== */}
      <div className="turn-timer-container">
        <div
          className={`turn-timer-fill ${state.turnTimer <= 10 && state.isMyTurn ? 'urgent' : ''}`}
          style={{ width: `${state.isMyTurn ? (state.turnTimer / 75) * 100 : 0}%` }}
        />
      </div>

      {/* ====== 对手随从区 ====== */}
      <div className="battle-minion-zone">
        {state.opponentBoard.length === 0 ? (
          <div style={{ opacity: 0.2, fontSize: 11, padding: '24px 0' }}>空</div>
        ) : (
          state.opponentBoard.map((card, idx) => renderMinionCard(card, false, idx))
        )}
      </div>

      {/* ====== 中间信息区 ====== */}
      <div className="battle-center-info">
        <span className="battle-turn-number">回合 {state.turnNumber}</span>
        <span className={`battle-turn-status ${state.isMyTurn ? 'my-turn' : ''}`}>
          {state.isMyTurn ? `⏱️${state.turnTimer}s` : '等待对手...'}
        </span>
      </div>

      {/* ====== 己方随从区 ====== */}
      <div className="battle-minion-zone">
        {state.myBoard.length === 0 ? (
          <div style={{ opacity: 0.2, fontSize: 11, padding: '24px 0' }}>空</div>
        ) : (
          state.myBoard.map((card, idx) => renderMinionCard(card, true, idx))
        )}
      </div>

      {/* ====== 法力水晶条（菱形） ====== */}
      <div className="mana-crystal-row">
        {renderManaCrystals(state.myMana, state.myMaxMana)}
        <span style={{ fontSize: 10, color: '#64748b', marginLeft: 4 }}>
          {state.myMana}/{state.myMaxMana}
        </span>
      </div>

      {/* ====== 己方状态条（HP/护甲/牌库/墓地） ====== */}
      <div style={{
        display: 'flex',
        justifyContent: 'space-between',
        alignItems: 'center',
        padding: '2px 12px',
        fontSize: 13,
      }}>
        <div style={{ display: 'flex', alignItems: 'center', gap: 6 }}>
          <span className="hero-hp" style={{ fontSize: 20 }}>{state.myHealth}</span>
          {state.myArmor > 0 && (
            <span className="hero-armor">🛡️{state.myArmor}</span>
          )}
        </div>
        <div className="deck-graveyard-area">
          <div className="deck-count">
            <div className="deck-icon">🃏</div>
            <span className="deck-label">{state.myDeckCount}</span>
          </div>
          <div className="graveyard-count">
            <div className="graveyard-icon">💀</div>
            <span className="graveyard-label">{state.myGraveyardCount}</span>
          </div>
        </div>
      </div>

      {/* ====== 手牌区（扇形展開） ====== */}
      <div className="hand-fan-container">
        {state.myHand.map((card, idx) => {
          const isPlayable = card.cost <= state.myMana && state.isMyTurn;
          const kws = parseKeywords(card.keywords);
          const kwBadges = kws.length > 0 ? kws.map((kw: string) => {
            const cfg = KEYWORD_CONFIG[kw];
            if (!cfg) return null;
            return (
              <span key={kw} className="cg-keyword-badge" style={{ background: cfg.color, fontSize: 6 }}>
                {cfg.label}
              </span>
            );
          }) : null;

          // 扇形角度计算: 中间牌直，两边牌旋转
          const totalCards = state.myHand.length;
          const fanAngle = Math.min(totalCards * 3, 20); // 最多20度
          const centerIdx = (totalCards - 1) / 2;
          const angle = (idx - centerIdx) * (fanAngle / Math.max(totalCards - 1, 1));
          const zIndex = idx;
          // 底部偏移: 角度越大越靠下
          const bottomOffset = Math.abs(angle) * 0.8;

          return (
            <div
              key={`hand-${card.cardId}-${idx}`}
              ref={(el) => { if (el) handCardRefs.current.set(card.cardId, el); else handCardRefs.current.delete(card.cardId); }}
              className={`hand-card-wrapper ${!isPlayable ? 'mobile' : ''}`}
              style={{
                zIndex,
                transform: `rotate(${angle}deg) translateY(${bottomOffset}px)`,
                marginLeft: idx === 0 ? 0 : -8,
              }}
              onClick={() => handlePlayCard(card)}
            >
              <div
                className={`mini-card ${isPlayable ? 'playable' : 'unplayable'}`}
                style={{ borderColor: getRarityColor(card.rarity) }}
              >
                {/* 费用宝石 */}
                <div className="hand-cost-gem">{card.cost}</div>
                {/* 卡牌名 */}
                <div className="hand-card-name">{card.nameCn}</div>
                {/* 关键词 */}
                {kwBadges && (
                  <div className="cg-keywords" style={{ marginTop: 1 }}>
                    {kwBadges}
                  </div>
                )}
                {/* 属性 */}
                <div className="hand-stats-row">
                  {card.attack > 0 && <span style={{ color: '#ef4444' }}>{card.attack}</span>}
                  {card.health > 0 && <span style={{ color: '#22c55e' }}>{card.health}</span>}
                </div>
                {/* 稀有度色条 */}
                <div className="rarity-bar" style={{ background: getRarityColor(card.rarity) }} />
              </div>
            </div>
          );
        })}
      </div>

      {/* ====== 英雄技能按钮（左下） ====== */}
      <button
        className={`hero-power-btn ${state.isMyTurn ? 'active' : 'inactive'}`}
        onClick={() => {
          // 英雄技能占位 - 可绑定实际技能逻辑
          if (!state.isMyTurn) return;
        }}
      >
        <span className="hp-icon">⚡</span>
        <span className="hp-cost">2</span>
      </button>

      {/* ====== 结束回合按钮（右下） ====== */}
      <button
        className="end-turn-btn"
        disabled={!state.isMyTurn || state.attackMode}
        onClick={() => {
          if (state.attackMode) return;
          handleEndTurn();
        }}
      >
        结束回合
      </button>

      {/* ====== 攻击模式提示 ====== */}
      {state.attackMode && (
        <>
          <div className="attack-mode-indicator">选择攻击目标</div>
          <button className="cancel-attack-btn" onClick={handleCancelAttack}>
            取消
          </button>
        </>
      )}

      {/* ====== 战斗日志 ====== */}
      <div className="battle-arena-log">
        {state.battleLog.slice(-3).map((log, i) => (
          <div key={i} className="battle-arena-log-item">{log}</div>
        ))}
      </div>
    </div>
  );
}
