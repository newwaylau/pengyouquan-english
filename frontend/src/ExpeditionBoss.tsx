import React from 'react';
import './expedition.css';

interface BossData {
  id: number;
  nameCn: string;
  nameEn: string;
  maxHp: number;
  currentHp: number;
  specialRules?: any;
}

interface ExpeditionBossProps {
  boss: BossData;
  playerHp: number;
  maxHp: number;
  damageNumber: { text: string; type: string } | null;
  feedback: string | null;
}

export default function ExpeditionBoss({
  boss,
  playerHp,
  maxHp,
  damageNumber,
  feedback,
}: ExpeditionBossProps) {
  const hpPercent = boss.maxHp > 0 ? (boss.currentHp / boss.maxHp) * 100 : 0;
  const playerHpPercent = maxHp > 0 ? (playerHp / maxHp) * 100 : 100;

  return (
    <div className="expedition-enemy-area">
      {damageNumber && (
        <div className={`expedition-damage-number ${damageNumber.type}`}>
          {damageNumber.text}
        </div>
      )}

      <div className="expedition-boss-tag">BOSS</div>

      <div className="expedition-enemy-name">{boss.nameCn}</div>
      <div className="expedition-enemy-sub">{boss.nameEn}</div>

      <div className="expedition-enemy-hp-bar">
        <div
          className="expedition-enemy-hp-fill"
          style={{ width: `${hpPercent}%` }}
        />
      </div>
      <div className="expedition-enemy-hp-text">
        {boss.currentHp} / {boss.maxHp}
      </div>

      {boss.specialRules &&
        typeof boss.specialRules === 'object' &&
        Object.keys(boss.specialRules).length > 0 && (
          <div className="expedition-special-rules">
            {Object.entries(boss.specialRules).map(([k, v]) => (
              <div key={k}>
                {k}: {String(v)}
              </div>
            ))}
          </div>
        )}

      <div className="expedition-player-hp-bar" style={{ marginTop: 16 }}>
        <div
          className="expedition-player-hp-fill"
          style={{ width: `${playerHpPercent}%` }}
        />
      </div>

      <div className="expedition-player-stats">
        <span>
          ❤️ {playerHp}/{maxHp}
        </span>
      </div>

      {feedback && (
        <div
          style={{
            textAlign: 'center',
            margin: '8px 0',
            fontSize: 13,
            fontWeight: 600,
            color: '#4ade80',
          }}
        >
          {feedback}
        </div>
      )}
    </div>
  );
}
