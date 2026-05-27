import React, { useState } from 'react';
import './expedition.css';

interface ExpeditionRewardProps {
  rewardChoices: any[];
  onChoose: (choice: any) => void;
}

export default function ExpeditionReward({ rewardChoices, onChoose }: ExpeditionRewardProps) {
  const [flipped, setFlipped] = useState<number | null>(null);
  const [chosen, setChosen] = useState<boolean>(false);

  const handleCardClick = (index: number) => {
    if (chosen) return;
    setFlipped(index);
    setChosen(true);
    setTimeout(() => {
      onChoose(rewardChoices[index]);
    }, 400);
  };

  if (!rewardChoices || rewardChoices.length === 0) {
    return null;
  }

  return (
    <div className="expedition-result-overlay">
      <div className="expedition-result-card">
        <div className="expedition-result-icon">🎁</div>
        <div className="expedition-result-title">击败敌人！</div>
        <div className="expedition-result-text">选择一个奖励</div>

        <div
          style={{
            display: 'flex',
            gap: 12,
            justifyContent: 'center',
            marginTop: 20,
          }}
        >
          {rewardChoices.map((choice: any, i: number) => (
            <div
              key={i}
              style={{
                perspective: '1000px',
                width: 100,
                height: 140,
                cursor: chosen && flipped !== i ? 'default' : 'pointer',
              }}
              onClick={() => handleCardClick(i)}
            >
              <div
                className="card-flip-inner"
                style={{
                  position: 'relative',
                  width: '100%',
                  height: '100%',
                  transition: 'transform 0.6s ease',
                  transformStyle: 'preserve-3d',
                  transform: flipped === i ? 'rotateY(180deg)' : 'rotateY(0deg)',
                }}
              >
                {/* Card back */}
                <div
                  style={{
                    position: 'absolute',
                    width: '100%',
                    height: '100%',
                    backfaceVisibility: 'hidden',
                    background: 'var(--card)',
                    border: '2px solid var(--border)',
                    borderRadius: 'var(--radius-md)',
                    display: 'flex',
                    alignItems: 'center',
                    justifyContent: 'center',
                    fontSize: 32,
                    color: 'var(--text-secondary)',
                  }}
                >
                  🎴
                </div>

                {/* Card front */}
                <div
                  style={{
                    position: 'absolute',
                    width: '100%',
                    height: '100%',
                    backfaceVisibility: 'hidden',
                    transform: 'rotateY(180deg)',
                    background: 'var(--card)',
                    border: '2px solid var(--teal)',
                    borderRadius: 'var(--radius-md)',
                    display: 'flex',
                    flexDirection: 'column',
                    alignItems: 'center',
                    justifyContent: 'center',
                    padding: 8,
                    gap: 4,
                    fontSize: 13,
                    fontWeight: 600,
                    color: 'var(--text)',
                    textAlign: 'center',
                    boxShadow: '0 0 12px rgba(20,184,166,0.3)',
                  }}
                >
                  <span style={{ fontSize: 24, marginBottom: 4 }}>
                    {choice.type === 'new_card'
                      ? '🃏'
                      : choice.type === 'new_relic'
                      ? '🪙'
                      : choice.type === 'gold'
                      ? '💰'
                      : choice.type === 'heal'
                      ? '❤️'
                      : '🎁'}
                  </span>
                  {choice.label || '选择'}
                </div>
              </div>
            </div>
          ))}
        </div>
      </div>
    </div>
  );
}
