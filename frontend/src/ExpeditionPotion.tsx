import React, { useState } from 'react';

// ── Types ─────────────────────────────────────────────
interface PotionItem {
  id: number;
  nameCn: string;
  effectCn: string;
  icon?: string;
}

interface ExpeditionPotionProps {
  potions: PotionItem[];
  onUsePotion: (potionId: number) => void;
  disabled?: boolean;
}

// ── Component ─────────────────────────────────────────
export default function ExpeditionPotion({
  potions,
  onUsePotion,
  disabled = false,
}: ExpeditionPotionProps) {
  const [hoveredId, setHoveredId] = useState<number | null>(null);

  if (!potions || potions.length === 0) return null;

  return (
    <div className="expedition-potion-bar">
      <div
        style={{
          display: 'flex',
          gap: 8,
          alignItems: 'center',
          flexWrap: 'wrap',
        }}
      >
        <span style={{ fontSize: 12, color: 'var(--text-secondary)', fontWeight: 600 }}>
          🧪 药水
        </span>
        {potions.map((potion) => (
          <div
            key={potion.id}
            className="expedition-potion-item"
            style={{
              position: 'relative',
              display: 'flex',
              alignItems: 'center',
              gap: 6,
              padding: '6px 10px',
              background: 'var(--card)',
              border: '1px solid var(--border)',
              borderRadius: 'var(--radius-sm)',
              opacity: disabled ? 0.5 : 1,
              cursor: disabled ? 'not-allowed' : 'default',
              transition: 'border-color 0.2s, background 0.2s',
            }}
            onMouseEnter={() => setHoveredId(potion.id)}
            onMouseLeave={() => setHoveredId(null)}
          >
            <span style={{ fontSize: 18 }}>{potion.icon || '🧪'}</span>
            <span style={{ fontSize: 13, fontWeight: 600, color: 'var(--text)' }}>
              {potion.nameCn}
            </span>
            <button
              className="expedition-potion-use-btn"
              style={{
                padding: '4px 10px',
                background: 'var(--teal)',
                color: 'white',
                border: 'none',
                borderRadius: 'var(--radius-sm)',
                fontSize: 11,
                fontWeight: 600,
                cursor: disabled ? 'not-allowed' : 'pointer',
                opacity: disabled ? 0.5 : 1,
              }}
              disabled={disabled}
              onClick={(e) => {
                e.stopPropagation();
                if (!disabled) onUsePotion(potion.id);
              }}
            >
              使用
            </button>

            {hoveredId === potion.id && (
              <div
                style={{
                  position: 'absolute',
                  bottom: 'calc(100% + 8px)',
                  left: '50%',
                  transform: 'translateX(-50%)',
                  background: '#1e293b',
                  color: '#e2e8f0',
                  padding: '8px 12px',
                  borderRadius: 'var(--radius-sm)',
                  fontSize: 11,
                  whiteSpace: 'nowrap',
                  zIndex: 100,
                  boxShadow: '0 4px 12px rgba(0,0,0,0.4)',
                  pointerEvents: 'none',
                  minWidth: 160,
                  textAlign: 'center',
                }}
              >
                <div style={{ fontSize: 13, fontWeight: 700, marginBottom: 2 }}>
                  {potion.icon || '🧪'} {potion.nameCn}
                </div>
                <div style={{ fontSize: 11, color: '#94a3b8', lineHeight: 1.4 }}>
                  {potion.effectCn}
                </div>
                <div
                  style={{
                    content: '',
                    position: 'absolute',
                    top: '100%',
                    left: '50%',
                    transform: 'translateX(-50%)',
                    border: '6px solid transparent',
                    borderTopColor: '#1e293b',
                  }}
                />
              </div>
            )}
          </div>
        ))}
      </div>
    </div>
  );
}
