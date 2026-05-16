import React from 'react';

interface Props {
  icon: string;
  title: string;
  subtitle: string;
  actionLabel?: string;
  onAction?: () => void;
}

export default function EmptyStateCard({ icon, title, subtitle, actionLabel, onAction }: Props) {
  return (
    <div className="empty-state-card">
      <div className="empty-state-icon">{icon}</div>
      <div className="empty-state-title">{title}</div>
      <div className="empty-state-subtitle">{subtitle}</div>
      {actionLabel && onAction && (
        <button className="btn-primary" style={{ marginTop: 20 }} onClick={onAction}>
          {actionLabel}
        </button>
      )}
    </div>
  );
}
