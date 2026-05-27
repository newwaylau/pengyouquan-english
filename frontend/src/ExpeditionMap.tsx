// ========== Constants ==========

const NODE_ICONS: Record<string, string> = {
  combat: '⚔️',
  event: '❓',
  rest: '🔥',
  shop: '🛒',
  boss: '👑',
  branch: '🔀',
};

const NODE_LABELS: Record<string, string> = {
  combat: '战斗',
  event: '事件',
  rest: '休息',
  shop: '商店',
  boss: 'Boss',
  branch: '岔路',
};

const ACT_LABELS = ['', '绝境长城', '君临城', '龙石岛'];

// ========== Props Interface ==========

interface ExpeditionMapProps {
  mapNodes: string[];
  currentNodeIndex: number;
  nodeType: string;
  nodeOptions: string[];
  onNodeClick: (nodeIndex: number) => void;
  onChoosePath: (choiceIndex: number) => void;
  act: number;
}

// ========== Layout Constants ==========

const NODE_SIZE = 40;
const NODE_RADIUS = NODE_SIZE / 2;
const SVG_WIDTH = 280;
const SVG_HEIGHT = 600;
const TOP_PAD = 60;
const BOT_PAD = 50;

// ========== Component ==========

export default function ExpeditionMap({
  mapNodes,
  currentNodeIndex,
  nodeType,
  nodeOptions,
  onNodeClick,
  onChoosePath,
  act,
}: ExpeditionMapProps) {
  const nodeCount = mapNodes.length;
  const availHeight = SVG_HEIGHT - TOP_PAD - BOT_PAD;
  const spacing = nodeCount > 1 ? availHeight / (nodeCount - 1) : 0;

  // Calculate absolute Y positions for each node
  const positions = mapNodes.map((_, i) => ({
    x: SVG_WIDTH / 2,
    y: TOP_PAD + i * spacing,
  }));

  // Scroll offset to roughly center the current node in the viewBox
  let translateY = 0;
  if (positions.length > 0 && positions[currentNodeIndex]) {
    const targetY = SVG_HEIGHT / 2;
    translateY = targetY - positions[currentNodeIndex].y - NODE_RADIUS;
    // Clamp so we never show empty space at top or bottom
    const minTranslate = SVG_HEIGHT - BOT_PAD - 10 - (positions[nodeCount - 1]?.y || 0) - NODE_SIZE;
    const maxTranslate = TOP_PAD - 10 - (positions[0]?.y || 0);
    translateY = Math.max(minTranslate, Math.min(maxTranslate, translateY));
  }

  // Generate cubic bezier path between consecutive nodes (alternating stagger)
  function getPathBetween(idx: number): string {
    const from = positions[idx];
    const to = positions[idx + 1];
    if (!from || !to) return '';
    // Alternate curve direction for staggered effect
    const stagger = idx % 2 === 0 ? 35 : -35;
    const cp1x = from.x + stagger;
    const cp1y = from.y + (to.y - from.y) * 0.33;
    const cp2x = to.x + stagger;
    const cp2y = from.y + (to.y - from.y) * 0.66;
    return `M ${from.x},${from.y} C ${cp1x},${cp1y} ${cp2x},${cp2y} ${to.x},${to.y}`;
  }

  const isBranchNode = nodeType === 'branch' && nodeOptions.length > 0;

  return (
    <div
      style={{
        display: 'flex',
        flexDirection: 'column',
        alignItems: 'center',
        gap: 4,
        padding: '20px 0',
        position: 'relative',
        width: '100%',
      }}
    >
      {/* ===== Act label at top ===== */}
      <div
        style={{
          fontSize: 14,
          color: 'var(--text-secondary)',
          marginBottom: 8,
          fontWeight: 500,
        }}
      >
        {ACT_LABELS[act] || `第${act}层`}
      </div>

      {/* ===== SVG Map ===== */}
      <svg
        viewBox={`0 0 ${SVG_WIDTH} ${SVG_HEIGHT}`}
        style={{ width: '100%', maxWidth: SVG_WIDTH, display: 'block' }}
        xmlns="http://www.w3.org/2000/svg"
      >
        <defs>
          {/* Glow filter for the current node */}
          <filter id="node-glow" x="-50%" y="-50%" width="200%" height="200%">
            <feGaussianBlur in="SourceGraphic" stdDeviation="4" result="blur" />
            <feMerge>
              <feMergeNode in="blur" />
              <feMergeNode in="blur" />
              <feMergeNode in="SourceGraphic" />
            </feMerge>
          </filter>

          {/* Glow for completed lines */}
          <filter id="line-glow" x="-20%" y="-20%" width="140%" height="140%">
            <feGaussianBlur in="SourceGraphic" stdDeviation="2" result="blur" />
            <feMerge>
              <feMergeNode in="blur" />
              <feMergeNode in="SourceGraphic" />
            </feMerge>
          </filter>

          {/* CSS animation for current node pulse */}
          <style>
            {`
              @keyframes svg-node-pulse {
                0%, 100% { opacity: 1; }
                50% { opacity: 0.6; }
              }
              .current-node-circle {
                animation: svg-node-pulse 2s ease-in-out infinite;
              }
            `}
          </style>
        </defs>

        {/* Scrollable group */}
        <g transform={`translate(0, ${Math.round(translateY)})`}>
          {/* Connection paths between nodes */}
          {nodeCount > 1 &&
            Array.from({ length: nodeCount - 1 }, (_, i) => {
              const isCompleted = i < currentNodeIndex;
              return (
                <path
                  key={`path-${i}`}
                  d={getPathBetween(i)}
                  fill="none"
                  stroke={isCompleted ? 'var(--teal)' : 'var(--border)'}
                  strokeWidth={isCompleted ? 2.5 : 1.5}
                  strokeDasharray={isCompleted ? 'none' : '5,4'}
                  strokeLinecap="round"
                  opacity={isCompleted ? 0.7 : 0.45}
                  filter={isCompleted ? 'url(#line-glow)' : undefined}
                />
              );
            })}

          {/* Node circles and labels */}
          {mapNodes.map((nt, ndx) => {
            const isCompleted = ndx < currentNodeIndex;
            const isCurrent = ndx === currentNodeIndex;
            const isFuture = ndx > currentNodeIndex;
            const pos = positions[ndx];
            const displayIcon = NODE_ICONS[nt] || '⚪';
            const displayLabel = NODE_LABELS[nt] || nt;

            return (
              <g
                key={`node-${ndx}`}
                onClick={() => {
                  if (isCurrent && nodeType !== 'branch') {
                    onNodeClick(ndx);
                  }
                }}
                style={{
                  cursor:
                    isCurrent && nodeType !== 'branch'
                      ? 'pointer'
                      : 'default',
                }}
              >
                {/* Node circle */}
                <circle
                  className={isCurrent ? 'current-node-circle' : undefined}
                  cx={pos.x}
                  cy={pos.y}
                  r={NODE_RADIUS}
                  fill={
                    isCompleted
                      ? 'var(--card)'
                      : isCurrent
                        ? 'var(--teal)'
                        : 'var(--card)'
                  }
                  stroke={
                    isCompleted
                      ? 'var(--border)'
                      : isCurrent
                        ? 'var(--teal-glow)'
                        : 'var(--border)'
                  }
                  strokeWidth={2}
                  opacity={isFuture ? 0.4 : 1}
                  filter={isCurrent ? 'url(#node-glow)' : undefined}
                />

                {/* Icon (emoji) */}
                <text
                  x={pos.x}
                  y={pos.y + 1}
                  textAnchor="middle"
                  dominantBaseline="central"
                  fontSize={18}
                  fill={isCurrent ? 'white' : 'var(--text)'}
                  style={{ userSelect: 'none', pointerEvents: 'none' }}
                >
                  {displayIcon}
                </text>

                {/* Label below node */}
                <text
                  x={pos.x}
                  y={pos.y + NODE_RADIUS + 14}
                  textAnchor="middle"
                  dominantBaseline="central"
                  fontSize={10}
                  fill={isCurrent ? 'var(--teal)' : 'var(--text-secondary)'}
                  fontWeight={isCurrent ? 600 : 400}
                  style={{ userSelect: 'none', pointerEvents: 'none' }}
                >
                  {displayLabel}
                </text>
              </g>
            );
          })}
        </g>
      </svg>

      {/* ===== Branch route overlay ===== */}
      {isBranchNode && (
        <div
          className="expedition-result-overlay"
          style={{
            position: 'fixed',
            top: 0,
            left: 0,
            right: 0,
            bottom: 0,
            background: 'rgba(0,0,0,0.7)',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
            zIndex: 100,
            animation: 'fadeIn 0.2s',
          }}
        >
          <div
            style={{
              background: 'var(--card)',
              borderRadius: 'var(--radius-lg)',
              padding: '32px 24px',
              maxWidth: 320,
              width: '90%',
              textAlign: 'center',
              border: '1px solid var(--border)',
            }}
          >
            <div style={{ fontSize: 48, marginBottom: 12 }}>🔀</div>
            <div
              style={{
                fontSize: 20,
                fontWeight: 700,
                marginBottom: 8,
                color: 'var(--text)',
              }}
            >
              选择路线
            </div>
            <div
              style={{
                fontSize: 14,
                color: 'var(--text-secondary)',
                marginBottom: 20,
                lineHeight: 1.6,
              }}
            >
              前方岔路，请选择前进方向：
            </div>
            <div
              style={{
                display: 'flex',
                flexDirection: 'column',
                gap: 8,
              }}
            >
              {nodeOptions.map((option: string, i: number) => (
                <button
                  key={i}
                  className="expedition-reward-btn"
                  onClick={() => onChoosePath(i)}
                  style={{
                    padding: 16,
                    background: 'var(--card)',
                    border: '2px solid var(--border)',
                    borderRadius: 'var(--radius-md)',
                    color: 'var(--text)',
                    fontSize: 14,
                    fontWeight: 500,
                    cursor: 'pointer',
                    transition: 'all 0.2s',
                    textAlign: 'center',
                  }}
                  onMouseEnter={(e) => {
                    e.currentTarget.style.borderColor = 'var(--teal)';
                    e.currentTarget.style.background = 'var(--hover-bg)';
                  }}
                  onMouseLeave={(e) => {
                    e.currentTarget.style.borderColor = 'var(--border)';
                    e.currentTarget.style.background = 'var(--card)';
                  }}
                >
                  {NODE_ICONS[option] || '⚪'} {NODE_LABELS[option] || option}
                </button>
              ))}
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
