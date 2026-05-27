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

interface BattleCard {
  uid: number;
  cardId: number;
  cardName: string;
  cardNameEn: string;
  cardType: string;
  cost: number;
  baseDamage: number;
  baseBlock: number;
  description: string;
  rarity: string;
  isXCost: boolean;
  upgraded: boolean;
}

interface EnemyData {
  id: number;
  name?: string;
  nameCn?: string;
  hp: number;
  maxHp: number;
  block: number;
}

interface PlayerData {
  hp: number;
  maxHp: number;
}

interface BattleState {
  battleId: number;
  status: string;
  turnNumber: number;
  energy: number;
  maxEnergy: number;
  playerBlock: number;
  player: PlayerData;
  enemy: EnemyData;
  hand: BattleCard[];
  playerBuffs: { name: string; stacks: number }[];
  enemyBuffs: { name: string; stacks: number }[];
  drawPileCount: number;
  discardPileCount: number;
}

const BUFF_LABELS: Record<string, string> = {
  strength: 'Str',
  dexterity: 'Dex',
  vulnerable: 'Vuln',
  weak: 'Weak',
  frail: 'Frail',
  poison: 'Poison',
};

const BUFF_COLORS: Record<string, string> = {
  strength: '#ff6b6b',
  dexterity: '#69db7c',
  vulnerable: '#ffd43b',
  weak: '#adb5bd',
  frail: '#e599f7',
  poison: '#7c3aed',
};

export default function BattlePageSit({ onBack }: { onBack: () => void }) {
  const [battleState, setBattleState] = useState<BattleState | null>(null);
  const [loading, setLoading] = useState(true);
  const [animatingCard, setAnimatingCard] = useState<number | null>(null);
  const [result, setResult] = useState<'won' | 'lost' | null>(null);
  const [rewardCards, setRewardCards] = useState<any[]>([]);
  const [turnLog, setTurnLog] = useState<string[]>([]);

  const fetchState = useCallback(async () => {
    const res = await apiFetch('/api/expedition/battle/state');
    if (res.code === 200) {
      setBattleState(res.data);
      if (res.data.status === 'won') {
        setResult('won');
        loadRewards();
      } else if (res.data.status === 'lost') {
        setResult('lost');
      }
    }
  }, []);

  const startBattle = useCallback(async () => {
    setLoading(true);

    // First check if there's an active expedition
    let expRes = await apiFetch('/api/expedition');
    if (expRes.code !== 200 || !expRes.data?.hasExpedition) {
      // Need to create a test expedition first
      // Get show ID 1
      const showsRes = await apiFetch('/api/shows');
      let showId = 1;
      if (showsRes.code === 200 && showsRes.data?.length > 0) {
        showId = showsRes.data[0].id;
      }

      // Get user's cards for expedition
      const cardsRes = await apiFetch('/api/cards/my');
      let deckIds: number[] = [];
      if (cardsRes.code === 200 && cardsRes.data?.length > 0) {
        deckIds = cardsRes.data.slice(0, 8).map((c: any) => c.cardId || c.id);
      }

      if (deckIds.length < 5) {
        // Need basic cards, use fallback IDs 1-8
        deckIds = [1, 1, 1, 3, 3, 4, 5, 6];
      }

      // Start expedition
      expRes = await apiFetch('/api/expedition/start', {
        method: 'POST',
        body: JSON.stringify({ showId, deckCardIds: deckIds }),
      });

      if (expRes.code !== 200) {
        alert(expRes.message || '无法创建测试远征');
        onBack();
        return;
      }
    }

    // Enter combat
    const combatRes = await apiFetch('/api/expedition/enter-combat', { method: 'POST' });
    if (combatRes.code !== 200) {
      alert(combatRes.message || '进入战斗失败');
      onBack();
      return;
    }

    // Start new battle system
    const res = await apiFetch('/api/expedition/battle/start', { method: 'POST' });
    if (res.code === 200) {
      setBattleState(res.data);
      if (res.data.status === 'won') {
        setResult('won');
        loadRewards();
      } else if (res.data.status === 'lost') {
        setResult('lost');
      }
    } else {
      alert(res.message || '开始战斗失败');
      onBack();
    }
    setLoading(false);
  }, [onBack]);

  const loadRewards = async () => {
    const res = await apiFetch('/api/expedition/reward-choices');
    if (res.code === 200) {
      setRewardCards(res.data.choices || []);
    }
  };

  useEffect(() => {
    startBattle();
  }, [startBattle]);

  const handlePlayCard = async (cardUid: number, cost: number) => {
    if (!battleState || battleState.status !== 'fighting') return;
    if (battleState.energy < cost) return;
    if (animatingCard !== null) return;

    setAnimatingCard(cardUid);
    const res = await apiFetch('/api/expedition/battle/play-card', {
      method: 'POST',
      body: JSON.stringify({ cardUid, targetIndex: 0 }),
    });
    setAnimatingCard(null);

    if (res.code === 200) {
      const data = res.data;
      setBattleState(data.battleState);

      if (data.combatResult?.damageDealt) {
        addLog(`Dealt ${data.combatResult.damageDealt} damage`);
      }
      if (data.combatResult?.blockGained) {
        addLog(`Gained ${data.combatResult.blockGained} Block`);
      }
      if (data.combatResult?.gainedStrength) {
        addLog(`Gained ${data.combatResult.gainedStrength} Strength`);
      }
      if (data.combatResult?.drewCards) {
        addLog(`Drew ${data.combatResult.drewCards} cards`);
      }
      if (data.combatResult?.appliedVulnerable) {
        addLog(`Applied ${data.combatResult.appliedVulnerable} Vulnerable`);
      }
      if (data.combatResult?.appliedWeak) {
        addLog(`Applied ${data.combatResult.appliedWeak} Weak`);
      }
      if (data.combatResult?.appliedPoison) {
        addLog(`Applied ${data.combatResult.appliedPoison} Poison`);
      }
      if (data.combatResult?.healed) {
        addLog(`Healed ${data.combatResult.healed} HP`);
      }

      if (data.battleState.status === 'won') {
        setResult('won');
        loadRewards();
      } else if (data.battleState.status === 'lost') {
        setResult('lost');
      }
    } else {
      addLog(`Error: ${res.message}`);
    }
  };

  const handleEndTurn = async () => {
    if (!battleState || battleState.status !== 'fighting') return;
    if (animatingCard !== null) return;

    const res = await apiFetch('/api/expedition/battle/end-turn', { method: 'POST' });
    if (res.code === 200) {
      const data = res.data;
      setBattleState(data.battleState);

      if (data.enemyAction) {
        const a = data.enemyAction;
        if (a.damageToPlayer > 0) {
          addLog(`Enemy dealt ${a.damageToPlayer} damage`);
        } else {
          addLog('Enemy attacked, all blocked');
        }
        if (a.enemyBuffApplied) addLog(`Enemy gained ${a.enemyBuffApplied}`);
        if (a.playerDebuffApplied) addLog(`You received ${a.playerDebuffApplied}`);
      }

      if (data.battleState.status === 'won') {
        setResult('won');
        loadRewards();
      } else if (data.battleState.status === 'lost') {
        setResult('lost');
      }
    }
  };

  const addLog = (msg: string) => {
    setTurnLog(prev => [...prev.slice(-19), msg]);
  };

  const handleChooseReward = async (choice: any) => {
    const res = await apiFetch('/api/expedition/apply-reward', {
      method: 'POST',
      body: JSON.stringify(choice),
    });
    if (res.code === 200) {
      onBack();
    }
  };

  if (loading) {
    return (
      <div className="expedition-container">
        <div className="expedition-loading">⚔️ Preparing for battle...</div>
      </div>
    );
  }

  if (!battleState) {
    return (
      <div className="expedition-container">
        <div className="expedition-loading">No battle data</div>
      </div>
    );
  }

  const { player, enemy, hand, energy, maxEnergy, playerBlock, playerBuffs, enemyBuffs, drawPileCount, discardPileCount, turnNumber } = battleState;
  const hpPercent = (player.hp / player.maxHp) * 100;
  const enemyHpPercent = (enemy.hp / enemy.maxHp) * 100;

  return (
    <div className="expedition-container">
      {/* === Result Modal === */}
      {result && (
        <div className="expedition-overlay">
          <div className={`expedition-modal ${result === 'won' ? 'victory' : 'defeat'}`}>
            <div className="expedition-modal-icon">{result === 'won' ? '🏆' : '💀'}</div>
            <div className="expedition-modal-title">
              {result === 'won' ? 'Victory!' : 'Defeated'}
            </div>
            {result === 'won' && rewardCards.length > 0 && (
              <div className="expedition-reward-cards">
                <div className="expedition-reward-label">Choose a reward card:</div>
                <div className="expedition-reward-choices">
                  {rewardCards.map((card: any, i: number) => (
                    <button key={i} className="battle-card-reward-btn" onClick={() => handleChooseReward({ type: 'card', cardId: card.cardId })}>
                      <div className={`battle-card-rarity ${card.rarity}`}>{card.rarity}</div>
                      <div className="battle-card-name">{card.cardNameEn}</div>
                      <div className="battle-card-desc">{card.description}</div>
                      <div className="battle-card-cost">{card.cost} Energy</div>
                    </button>
                  ))}
                </div>
                <button className="expedition-btn" onClick={onBack}>Skip</button>
              </div>
            )}
            {result === 'won' && rewardCards.length === 0 && (
              <button className="expedition-btn" onClick={onBack}>Continue</button>
            )}
            {result === 'lost' && (
              <button className="expedition-btn" onClick={onBack}>Return</button>
            )}
          </div>
        </div>
      )}

      {/* === Main Battle UI === */}
      <div className="expedition-battle-layout">
        {/* Top: Enemy area */}
        <div className="battle-enemy-section">
          <div className="battle-enemy-info">
            <div className="battle-enemy-name">{enemy.name || enemy.nameCn || 'Enemy'}</div>
            <div className="battle-enemy-hp-row">
              <div className="battle-hp-bar">
                <div className="battle-hp-fill enemy-hp-fill" style={{ width: `${enemyHpPercent}%` }} />
              </div>
              <span className="battle-hp-text">{enemy.hp}/{enemy.maxHp}</span>
            </div>
            {enemy.block > 0 && (
              <div className="battle-block-display">
                <span className="battle-block-icon">🛡️</span> {enemy.block}
              </div>
            )}
            <div className="battle-buffs-row">
              {enemyBuffs?.map((buff, i) => (
                <span key={i} className="battle-buff-tag" style={{ background: BUFF_COLORS[buff.name] || '#666' }}>
                  {BUFF_LABELS[buff.name] || buff.name} {buff.stacks}
                </span>
              ))}
            </div>
          </div>
        </div>

        {/* Middle: Battle log */}
        <div className="battle-log">
          <div className="battle-log-turn">Turn {turnNumber}</div>
          {turnLog.slice(-5).map((msg, i) => (
            <div key={i} className="battle-log-entry">{msg}</div>
          ))}
        </div>

        {/* Bottom: Player area */}
        <div className="battle-player-section">
          <div className="battle-player-stats">
            <div className="battle-player-hp-block">
              <div className="battle-player-hp">
                <span className="battle-hp-label">HP</span>
                <div className="battle-hp-bar">
                  <div className="battle-hp-fill player-hp-fill" style={{ width: `${hpPercent}%` }} />
                </div>
                <span className="battle-hp-text">{player.hp}/{player.maxHp}</span>
              </div>
              {playerBlock > 0 && (
                <div className="battle-block-display">
                  <span className="battle-block-icon">🛡️</span> {playerBlock}
                </div>
              )}
            </div>
            <div className="battle-energy-display">
              {'⚡'.repeat(energy)}{'·'.repeat(Math.max(0, maxEnergy - energy))}
              <span className="battle-energy-text">{energy}/{maxEnergy}</span>
            </div>
            <div className="battle-pile-counts">
              <span className="battle-pile-count draw">Draw: {drawPileCount}</span>
              <span className="battle-pile-count discard">Discard: {discardPileCount}</span>
            </div>
            <div className="battle-buffs-row">
              {playerBuffs?.map((buff, i) => (
                <span key={i} className="battle-buff-tag" style={{ background: BUFF_COLORS[buff.name] || '#666' }}>
                  {BUFF_LABELS[buff.name] || buff.name} {buff.stacks}
                </span>
              ))}
            </div>
          </div>

          {/* Hand */}
          <div className="expedition-hand battle-hand">
            {hand.map((card) => {
              const canPlay = battleState?.status === 'fighting' && energy >= card.cost && animatingCard === null;
              const isAnimating = animatingCard === card.uid;
              return (
                <button
                  key={card.uid}
                  className={`expedition-hand-card battle-card ${canPlay ? 'playable' : ''} ${isAnimating ? 'playing' : ''} ${card.cardType}`}
                  onClick={() => canPlay && handlePlayCard(card.uid, card.cost)}
                  disabled={!canPlay}
                  data-card-id={card.uid}
                >
                  <div className="battle-card-cost-display">{card.isXCost ? 'X' : card.cost}</div>
                  <div className="battle-card-type-icon">
                    {card.cardType === 'attack' ? '⚔️' : card.cardType === 'skill' ? '🛡️' : '⭐'}
                  </div>
                  <div className="battle-card-name">{card.cardNameEn}</div>
                  <div className="battle-card-effects">
                    {card.baseDamage > 0 && <span className="battle-card-damage">{card.baseDamage} dmg</span>}
                    {card.baseBlock > 0 && <span className="battle-card-block">{card.baseBlock} blk</span>}
                  </div>
                  <div className="battle-card-desc">{card.description}</div>
                </button>
              );
            })}
          </div>

          {/* End Turn button */}
          {battleState?.status === 'fighting' && (
            <button className="battle-end-turn-btn" onClick={handleEndTurn} disabled={animatingCard !== null}>
              End Turn
            </button>
          )}
        </div>
      </div>
    </div>
  );
}
