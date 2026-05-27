import React, { useState, useCallback } from 'react';
import './expedition.css';

const RARITY_COLORS: Record<string, string> = {
  common: '#9d9d9d',
  rare: '#0070dd',
  epic: '#a335ee',
  legendary: '#ff8c00',
};

interface Particle {
  id: number;
  x: number;
  y: number;
  color: string;
  style: string;
}

interface ExpeditionRewardProps {
  rewardChoices: any[];
  onChoose: (choice: any) => void;
}

export default function ExpeditionReward({ rewardChoices, onChoose }: ExpeditionRewardProps) {
  const [flipped, setFlipped] = useState<Set<number>>(new Set());
  const [chosen, setChosen] = useState<boolean>(false);
  const [particles, setParticles] = useState<Particle[]>([]);
  const [screenFlash, setScreenFlash] = useState<boolean>(false);

  const generateParticles = useCallback((index: number, rarity: string) => {
    const color = RARITY_COLORS[rarity] || '#9d9d9d';
    const cardEl = document.querySelector(`[data-card-index="${index}"]`);
    if (!cardEl) return [];

    const rect = cardEl.getBoundingClientRect();
    const cx = rect.left + rect.width / 2;
    const cy = rect.top + rect.height / 2;
    const count = rarity === 'legendary' ? 20 : 12;

    return Array.from({ length: count }, (_, i) => ({
      id: Date.now() + i + index * 100,
      x: cx + (Math.random() - 0.5) * 80,
      y: cy + (Math.random() - 0.5) * 80,
      color,
      style: rarity === 'legendary' ? 'golden' : 'normal',
    }));
  }, []);

  const handleCardClick = (clickedIndex: number) => {
    if (chosen) return;
    setChosen(true);

    let allParticles: Particle[] = [];

    rewardChoices.forEach((choice: any, i: number) => {
      setTimeout(() => {
        setFlipped(prev => new Set([...prev, i]));
        const cardParticles = generateParticles(i, choice.rarity || 'common');
        allParticles = [...allParticles, ...cardParticles];
        setParticles([...allParticles]);

        // Screen flash for legendary rarity
        if ((choice.rarity || 'common') === 'legendary') {
          setScreenFlash(true);
          setTimeout(() => setScreenFlash(false), 500);
        }
      }, i * 200); // 0.2s delay chain per card
    });

    // Remove particles after all flip animations finish (600ms each)
    setTimeout(() => {
      setParticles([]);
    }, rewardChoices.length * 200 + 600);

    // Call onChoose after animations complete
    setTimeout(() => {
      onChoose(rewardChoices[clickedIndex]);
    }, rewardChoices.length * 200 + 400);
  };

  if (!rewardChoices || rewardChoices.length === 0) {
    return null;
  }

  return (
    <div className="expedition-result-overlay">
      {screenFlash && <div className="expedition-screen-flash" />}
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
            flexWrap: 'wrap',
          }}
        >
          {rewardChoices.map((choice: any, i: number) => (
            <div
              key={i}
              data-card-index={i}
              className="expedition-reward-card-item"
              style={{
                perspective: '1000px',
                width: 100,
                height: 140,
                cursor: chosen && !flipped.has(i) ? 'default' : 'pointer',
                flexShrink: 0,
              }}
              onClick={() => handleCardClick(i)}
            >
              <div
                className="expedition-reward-card"
                style={{
                  position: 'relative',
                  width: '100%',
                  height: '100%',
                  transition: 'transform 0.6s ease',
                  transformStyle: 'preserve-3d',
                  transform: flipped.has(i) ? 'rotateY(180deg)' : 'rotateY(0deg)',
                  animationDelay: chosen ? `${i * 0.2}s` : '0s',
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
      {particles.length > 0 && (
        <div className="expedition-particle-container">
          {particles.map(p => (
            <div
              key={p.id}
              className={`expedition-particle${p.style === 'golden' ? ' expedition-golden-particle' : ''}`}
              style={{
                left: p.x,
                top: p.y,
                '--particle-color': p.color,
              } as React.CSSProperties}
            />
          ))}
        </div>
      )}
    </div>
  );
}
