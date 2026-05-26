import React, { useState, useEffect, useRef, useCallback } from 'react';
import BattleWebSocket from './api/battleWebSocket';
import './battle-arena.css';

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
}

interface QuestionData {
  cardId: number;
  cardNameCn: string;
  questionType: string;
  questionData: string;
  timeLimit: number;
  sentenceText?: string;
  [key: string]: any;
}

interface DefenseQuestionData {
  attackerId: number;
  attackerName: string;
  attackPower: number;
  targetType: string;
  targetId?: number;
  question: QuestionData;
  [key: string]: any;
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

  // 答题弹窗
  currentQuestion: QuestionData | null;
  answerResult: 'waiting' | 'correct' | 'wrong' | null;

  // 防御弹窗
  defenseQuestion: DefenseQuestionData | null;
  defenseResult: 'waiting' | 'correct' | 'wrong' | null;

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

    currentQuestion: null,
    answerResult: null,

    defenseQuestion: null,
    defenseResult: null,

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
        // 开始回合倒计时
        startTurnTimer();
      },
      onQuestion: (data: any) => {
        const payload = data.payload;
        setState(prev => ({
          ...prev,
          currentQuestion: payload,
          answerResult: 'waiting',
        }));
      },
      onCardPlayResult: (data: any) => {
        const payload = data.payload;
        if (payload.success) {
          setState(prev => ({
            ...prev,
            answerResult: 'correct',
            myMana: payload.manaRemaining,
            battleLog: [...prev.battleLog, `✅ 出牌成功`],
          }));
          // 关闭答题弹窗
          setTimeout(() => {
            setState(prev => ({
              ...prev,
              currentQuestion: null,
              answerResult: null,
            }));
          }, 1000);
        } else {
          setState(prev => ({
            ...prev,
            answerResult: 'wrong',
            battleLog: [...prev.battleLog, `❌ ${payload.errorMessage || '出牌失败'}`],
          }));
          setTimeout(() => {
            setState(prev => ({
              ...prev,
              currentQuestion: null,
              answerResult: null,
            }));
          }, 1500);
        }
      },
      onAttackDeclared: (_data: any) => {
        // 攻击已发起
      },
      onDefenseQuestion: (data: any) => {
        const payload = data.payload;
        setState(prev => ({
          ...prev,
          defenseQuestion: payload,
          defenseResult: 'waiting',
          battleLog: [...prev.battleLog, `⚔️ ${payload.attackerName} 发起攻击!`],
        }));
      },
      onAttackResult: (_data: any) => {},
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
      onCombo: (data: any) => {
        const payload = data.payload;
        setState(prev => ({
          ...prev,
          battleLog: [...prev.battleLog, `🔥 ${payload.comboCount}连击! ${payload.bonus}`],
        }));
      },
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

    // 检查连接状态
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
    // 更新等待时间
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

  // ===================== 出牌操作 =====================

  const handlePlayCard = (card: CardData) => {
    if (!state.isMyTurn || state.attackMode) return;
    if (state.currentQuestion) return;
    if (state.defenseQuestion) return;
    const ws = wsRef.current;
    if (!ws) return;
    ws.playCard(state.sessionId!, card.cardId);
  };

  // ===================== 答题 =====================

  const handleAnswer = (correct: boolean) => {
    const ws = wsRef.current;
    if (!ws || !state.sessionId) return;
    ws.submitAnswer(state.sessionId, correct);
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

  // ===================== 防御 =====================

  const handleDefense = (correct: boolean) => {
    const ws = wsRef.current;
    if (!ws || !state.sessionId) return;
    ws.submitDefense(state.sessionId, correct);
    setState(prev => ({
      ...prev,
      defenseResult: correct ? 'correct' : 'wrong',
    }));
    setTimeout(() => {
      setState(prev => ({
        ...prev,
        defenseQuestion: null,
        defenseResult: null,
      }));
    }, 1000);
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
                  与其他玩家实时英语对战！<br />
                  答题出牌，攻防兼备，赢取奖杯！
                </p>
                <div className="battle-arena-rules">
                  <div className="battle-arena-rule-item">🎯 答对出牌，答错回手</div>
                  <div className="battle-arena-rule-item">💪 3连击免费出牌</div>
                  <div className="battle-arena-rule-item">🛡️ 防御题可减免伤害</div>
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
                <span className="battle-arena-result-stat-detail">正确率: {rd?.winnerStats?.accuracy}%</span>
              </div>
              <div className="battle-arena-result-divider">VS</div>
              <div className="battle-arena-result-stat">
                <span className="battle-arena-result-stat-label">败者</span>
                <span className="battle-arena-result-stat-value">{rd?.loserStats?.nickname || ''}</span>
                <span className="battle-arena-result-stat-detail">血量: {rd?.loserStats?.healthRemaining}</span>
                <span className="battle-arena-result-stat-detail">正确率: {rd?.loserStats?.accuracy}%</span>
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

      {/* ====== 答题弹窗 ====== */}
      {state.currentQuestion && (
        <div className="battle-arena-overlay">
          <div className="battle-arena-question-panel">
            <div className="battle-arena-question-header">
              <span className="battle-arena-question-card-name">{state.currentQuestion.cardNameCn}</span>
              <span className="battle-arena-question-type">
                出牌答题 · {state.currentQuestion.timeLimit}秒
              </span>
            </div>
            <div className="battle-arena-question-body">
              {state.answerResult === 'waiting' ? (
                <QuestionDisplay question={state.currentQuestion} onAnswer={handleAnswer} />
              ) : (
                <div className={`battle-arena-question-result ${state.answerResult}`}>
                  {state.answerResult === 'correct' ? '✅ 答对了！' : '❌ 答错了！'}
                </div>
              )}
            </div>
          </div>
        </div>
      )}

      {/* ====== 防御弹窗 ====== */}
      {state.defenseQuestion && (
        <div className="battle-arena-overlay">
          <div className="battle-arena-question-panel defense">
            <div className="battle-arena-question-header defense">
              <span>🛡️ 防御！</span>
              <span>{state.defenseQuestion.attackerName} 攻击力 {state.defenseQuestion.attackPower}</span>
            </div>
            <div className="battle-arena-question-body">
              {state.defenseResult === 'waiting' ? (
                <div className="battle-arena-defense-section">
                  <p className="battle-arena-defense-prompt">
                    答对减免一半伤害！
                  </p>
                  <DefenseDisplay
                    question={state.defenseQuestion}
                    onAnswer={(correct) => handleDefense(correct)}
                  />
                </div>
              ) : (
                <div className={`battle-arena-question-result ${state.defenseResult}`}>
                  {state.defenseResult === 'correct' ? '✅ 防御成功！伤害减半' : '❌ 防御失败！全额伤害'}
                </div>
              )}
            </div>
          </div>
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

// ===================== 题目展示组件 =====================

function QuestionDisplay({
  question,
  onAnswer,
}: {
  question: QuestionData;
  onAnswer: (correct: boolean) => void;
}) {
  const [selectedOption, setSelectedOption] = useState<number | null>(null);
  const [inputValue, setInputValue] = useState('');

  const questionType = question.questionType;

  // 听力题
  if (questionType === 'listening') {
    let qData: any = {};
    try {
      qData = typeof question.questionData === 'string'
        ? JSON.parse(question.questionData)
        : question.questionData;
    } catch { /* ignore */ }

    const options = qData.options || [];
    const correctIdx = qData.correctIndex ?? 0;

    return (
      <div className="battle-arena-question-content">
        <p className="battle-arena-question-prompt">请选择正确的单词</p>
        {qData.sentence && (
          <p className="battle-arena-question-sentence">{qData.sentence}</p>
        )}
        <div className="battle-arena-options">
          {options.map((opt: string, idx: number) => (
            <button
              key={idx}
              className={`battle-arena-option-btn ${selectedOption === idx ? 'selected' : ''}`}
              onClick={() => {
                setSelectedOption(idx);
                setTimeout(() => onAnswer(idx === correctIdx), 500);
              }}
            >
              {opt}
            </button>
          ))}
        </div>
      </div>
    );
  }

  // 拼写题
  if (questionType === 'spelling') {
    let qData: any = {};
    try {
      qData = typeof question.questionData === 'string'
        ? JSON.parse(question.questionData)
        : question.questionData;
    } catch { /* ignore */ }

    const answer = qData.answer || qData.prompt || '';

    return (
      <div className="battle-arena-question-content">
        <p className="battle-arena-question-prompt">请拼写以下单词</p>
        <p className="battle-arena-question-word">{qData.prompt || qData.answer || ''}</p>
        <input
          className="battle-arena-input"
          type="text"
          value={inputValue}
          onChange={e => setInputValue(e.target.value)}
          placeholder="输入拼写..."
          autoFocus
        />
        <button
          className="battle-arena-btn battle-arena-btn-primary"
          onClick={() => {
            const isCorrect = inputValue.trim().toLowerCase() === answer.trim().toLowerCase();
            onAnswer(isCorrect);
          }}
        >
          提交
        </button>
      </div>
    );
  }

  // 默认为选择题（简化）
  return (
    <div className="battle-arena-question-content">
      <p className="battle-arena-question-prompt">答题出牌！</p>
      <p className="battle-arena-question-sentence">
        {typeof question.questionData === 'string' ? question.questionData.slice(0, 100) : '答题'}
      </p>
      <div className="battle-arena-options">
        <button className="battle-arena-option-btn correct" onClick={() => onAnswer(true)}>
          ✅ 答对
        </button>
        <button className="battle-arena-option-btn wrong" onClick={() => onAnswer(false)}>
          ❌ 答错
        </button>
      </div>
    </div>
  );
}

// ===================== 防御题展示组件 =====================

function DefenseDisplay({
  question,
  onAnswer,
}: {
  question: DefenseQuestionData;
  onAnswer: (correct: boolean) => void;
}) {
  let qData: any = {};
  try {
    qData = typeof question.questionData === 'string'
      ? JSON.parse(question.questionData)
      : (question.question || {});
  } catch { /* ignore */ }

  return (
    <div className="battle-arena-question-content">
      <p className="battle-arena-question-prompt">防御！请在以下句子中找到正确的关键词</p>
      {qData.sentence && (
        <p className="battle-arena-question-sentence">{qData.sentence}</p>
      )}
      <p className="battle-arena-question-keyword">
        关键词: <strong>{qData.keyword || qData.answer || ''}</strong>
      </p>
      <div className="battle-arena-options">
        <button className="battle-arena-option-btn correct" onClick={() => onAnswer(true)}>
          ✅ 答对（伤害减半）
        </button>
        <button className="battle-arena-option-btn wrong" onClick={() => onAnswer(false)}>
          ❌ 答错（全额伤害）
        </button>
      </div>
    </div>
  );
}
