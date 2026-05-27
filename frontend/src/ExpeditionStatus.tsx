import React, { useState } from 'react';

// ── Types ─────────────────────────────────────────────
interface RelicItem {
  id?: number;
  nameCn?: string;
  nameEn?: string;
  icon?: string;
  rarity?: string;
  descriptionCn?: string;
  effectCn?: string;
  effectType?: string;
}

interface ExpeditionData {
  playerHp: number;
  maxHp: number;
  gold: number;
  act: number;
  node: number;
  mapNodes: string[];
  enemiesKilled: number;
  relics: any[];
  questionsAnswered: number;
  questionsTotal: number;
}

interface ExpeditionStatusProps {
  expedition: ExpeditionData | null;
  onAbandon: () => void;
}

const ACT_LABELS = ['', '绝境长城', '君临城', '龙石岛'];

// ── Relic Tooltip ─────────────────────────────────────
function RelicTooltip({ relic }: { relic: RelicItem }) {
  return (
    <div className="expedition-relic-tooltip">
      <div className="expedition-relic-tooltip-name">
        {relic.icon || ''} {relic.nameCn || ''}
      </div>
      <div className="expedition-relic-tooltip-rarity">{relic.rarity || ''}</div>
      <div className="expedition-relic-tooltip-desc">
        {relic.descriptionCn || relic.effectCn || relic.effectType || ''}
      </div>
    </div>
  );
}

// ── Component ─────────────────────────────────────────
export default function ExpeditionStatus({
  expedition,
  onAbandon,
}: ExpeditionStatusProps) {
  const [hoveredRelic, setHoveredRelic] = useState<number | null>(null);

  if (!expedition) return null;

  const relics: RelicItem[] = expedition.relics || [];
  const actLabel = ACT_LABELS[expedition.act] || `第${expedition.act}层`;
  const totalNodes = expedition.mapNodes?.length || 0;

  return (
    <div className="expedition-active-bar">
      <div style={{ display: 'flex', flexDirection: 'column', gap: 2, flex: 1 }}>
        <span className="act-label">
          {actLabel} · 节点 {expedition.node}/{totalNodes}
        </span>
        <div style={{ display: 'flex', gap: 8, alignItems: 'center', fontSize: 12 }}>
          <span>❤️{expedition.playerHp}/{expedition.maxHp}</span>
          <span>🪙{expedition.gold}</span>
          <span>☠️{expedition.enemiesKilled}</span>
        </div>
      </div>

      <button className="expedition-abandon-btn" onClick={onAbandon}>
        放弃
      </button>

      {relics.length > 0 && (
        <div className="expedition-relic-bar">
          {relics.map((rel: RelicItem, i: number) => (
            <div
              key={i}
              className={`expedition-relic-icon expedition-relic-${rel.rarity || 'common'}`}
              onMouseEnter={() => setHoveredRelic(i)}
              onMouseLeave={() => setHoveredRelic(null)}
            >
              <span>{rel.icon || '🪙'}</span>
              {hoveredRelic === i && <RelicTooltip relic={rel} />}
            </div>
          ))}
        </div>
      )}
    </div>
  );
}
