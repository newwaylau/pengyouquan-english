import React from 'react';

interface Props {
  icon?: string;
  title: string;
  subtitle: string;
  actionLabel?: string;
  onAction?: () => void;
  variant?: 'celebration' | 'search' | 'book' | 'document';
}

const illustrations: Record<string, React.ReactNode> = {
  celebration: (
    <svg width="100" height="100" viewBox="0 0 100 100" fill="none" xmlns="http://www.w3.org/2000/svg">
      {/* Circle check */}
      <circle cx="50" cy="52" r="28" stroke="#14b8a6" strokeWidth="2.5" fill="none" opacity="0.3" />
      <circle cx="50" cy="52" r="18" stroke="#14b8a6" strokeWidth="2" fill="none" opacity="0.5" />
      <path d="M41 52l6 6 12-12" stroke="#14b8a6" strokeWidth="2.5" strokeLinecap="round" strokeLinejoin="round" fill="none" />
      {/* Sparkles */}
      <circle cx="26" cy="28" r="2.5" fill="#14b8a6" opacity="0.6" />
      <circle cx="74" cy="24" r="2" fill="#14b8a6" opacity="0.4" />
      <circle cx="78" cy="58" r="2" fill="#14b8a6" opacity="0.5" />
      <circle cx="22" cy="70" r="1.8" fill="#14b8a6" opacity="0.35" />
      <circle cx="28" cy="80" r="1.5" fill="#14b8a6" opacity="0.4" />
      <circle cx="72" cy="78" r="1.5" fill="#14b8a6" opacity="0.3" />
      {/* Confetti dots */}
      <rect x="32" y="22" width="2" height="4" rx="1" fill="#14b8a6" opacity="0.5" transform="rotate(-30 33 24)" />
      <rect x="68" y="34" width="2" height="5" rx="1" fill="#14b8a6" opacity="0.4" transform="rotate(20 69 36)" />
      <rect x="62" y="84" width="2" height="4" rx="1" fill="#14b8a6" opacity="0.35" transform="rotate(-15 63 86)" />
    </svg>
  ),
  search: (
    <svg width="100" height="100" viewBox="0 0 100 100" fill="none" xmlns="http://www.w3.org/2000/svg">
      {/* Grid dots */}
      <circle cx="30" cy="30" r="1.5" fill="#334155" />
      <circle cx="42" cy="30" r="1.5" fill="#334155" />
      <circle cx="54" cy="30" r="1.5" fill="#334155" />
      <circle cx="30" cy="42" r="1.5" fill="#334155" />
      <circle cx="42" cy="42" r="1.5" fill="#334155" />
      <circle cx="54" cy="42" r="1.5" fill="#334155" />
      {/* Magnifying glass */}
      <circle cx="48" cy="52" r="18" stroke="#14b8a6" strokeWidth="2.5" fill="none" opacity="0.5" />
      <circle cx="48" cy="52" r="11" stroke="#14b8a6" strokeWidth="2" fill="none" opacity="0.8" />
      <line x1="63" y1="67" x2="72" y2="76" stroke="#14b8a6" strokeWidth="2.5" strokeLinecap="round" opacity="0.6" />
      {/* Eyeball */}
      <circle cx="48" cy="52" r="3" fill="#14b8a6" opacity="0.4" />
    </svg>
  ),
  book: (
    <svg width="100" height="100" viewBox="0 0 100 100" fill="none" xmlns="http://www.w3.org/2000/svg">
      {/* Bottom line */}
      <line x1="18" y1="78" x2="82" y2="78" stroke="#334155" strokeWidth="2" strokeLinecap="round" />
      {/* Open book */}
      <path d="M17 78V28c0-3 2-5 5-5h24c4 0 7.5 2.5 9 6" stroke="#14b8a6" strokeWidth="2.5" strokeLinecap="round" strokeLinejoin="round" fill="none" opacity="0.7" />
      <path d="M83 78V28c0-3-2-5-5-5H54c-4 0-7.5 2.5-9 6" stroke="#14b8a6" strokeWidth="2.5" strokeLinecap="round" strokeLinejoin="round" fill="none" opacity="0.7" />
      {/* Spine */}
      <line x1="50" y1="23" x2="50" y2="78" stroke="#14b8a6" strokeWidth="2" opacity="0.3" />
      {/* Page lines left */}
      <line x1="25" y1="40" x2="46" y2="40" stroke="#475569" strokeWidth="1.5" strokeLinecap="round" opacity="0.4" />
      <line x1="25" y1="48" x2="46" y2="48" stroke="#475569" strokeWidth="1.5" strokeLinecap="round" opacity="0.3" />
      <line x1="25" y1="56" x2="46" y2="56" stroke="#475569" strokeWidth="1.5" strokeLinecap="round" opacity="0.2" />
      {/* Page lines right */}
      <line x1="54" y1="40" x2="75" y2="40" stroke="#475569" strokeWidth="1.5" strokeLinecap="round" opacity="0.4" />
      <line x1="54" y1="48" x2="75" y2="48" stroke="#475569" strokeWidth="1.5" strokeLinecap="round" opacity="0.3" />
      <line x1="54" y1="56" x2="75" y2="56" stroke="#475569" strokeWidth="1.5" strokeLinecap="round" opacity="0.2" />
      {/* Bookmark */}
      <path d="M50 23l-4 6-4-6" stroke="#14b8a6" strokeWidth="1.8" strokeLinecap="round" strokeLinejoin="round" fill="none" opacity="0.5" />
    </svg>
  ),
  document: (
    <svg width="100" height="100" viewBox="0 0 100 100" fill="none" xmlns="http://www.w3.org/2000/svg">
      {/* Document */}
      <path d="M28 22h30l18 18v38c0 3-2 5-5 5H28c-3 0-5-2-5-5V27c0-3 2-5 5-5z" stroke="#14b8a6" strokeWidth="2.5" strokeLinejoin="round" fill="none" opacity="0.7" />
      {/* Fold */}
      <path d="M58 22v18h18" stroke="#14b8a6" strokeWidth="2" strokeLinejoin="round" fill="none" opacity="0.4" />
      {/* Text lines */}
      <line x1="35" y1="58" x2="65" y2="58" stroke="#475569" strokeWidth="2" strokeLinecap="round" opacity="0.4" />
      <line x1="35" y1="66" x2="55" y2="66" stroke="#475569" strokeWidth="2" strokeLinecap="round" opacity="0.3" />
      <line x1="35" y1="74" x2="60" y2="74" stroke="#475569" strokeWidth="2" strokeLinecap="round" opacity="0.2" />
      {/* Pencil */}
      <line x1="70" y1="45" x2="54" y2="72" stroke="#14b8a6" strokeWidth="2" strokeLinecap="round" opacity="0.5" />
      <circle cx="71" cy="43" r="4" stroke="#14b8a6" strokeWidth="1.5" fill="none" opacity="0.4" />
    </svg>
  ),
};

const variantFromIcon: Record<string, string> = {
  '🎉': 'celebration',
  '🔍': 'search',
  '📖': 'book',
  '📝': 'document',
};

export default function EmptyStateCard({ icon, title, subtitle, actionLabel, onAction, variant }: Props) {
  const resolvedVariant = variant || (icon ? variantFromIcon[icon] : undefined) || 'celebration';
  const illustration = illustrations[resolvedVariant];

  return (
    <div className="empty-state-card">
      <div className="empty-state-illustration">{illustration}</div>
      <div className="empty-state-title">{title}</div>
      <div className="empty-state-subtitle">{subtitle}</div>
      {actionLabel && onAction && (
        <button className="action-btn primary" style={{ marginTop: 20 }} onClick={onAction}>
          {actionLabel}
        </button>
      )}
    </div>
  );
}
