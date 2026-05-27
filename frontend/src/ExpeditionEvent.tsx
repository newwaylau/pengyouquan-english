import React from 'react';
import './expedition.css';

interface ExpeditionEventProps {
  eventData: any[];
  onChoice: (choiceIndex: number) => void;
}

export default function ExpeditionEvent({ eventData, onChoice }: ExpeditionEventProps) {
  if (!eventData || !Array.isArray(eventData) || eventData.length === 0) {
    return <div className="expedition-empty">加载中...</div>;
  }

  return (
    <div className="expedition-event">
      <div className="expedition-event-title">❓ 事件</div>
      <div className="expedition-event-desc">
        {eventData[0]?.description || eventData[0]?.desc || '你遇到了一个事件，请做出选择：'}
      </div>
      <div className="expedition-event-choices">
        {eventData.map((choice: any, i: number) => (
          <button
            key={i}
            className="expedition-reward-btn"
            onClick={() => onChoice(i)}
          >
            {choice.text || choice.label || `选项${i + 1}`}
          </button>
        ))}
      </div>
    </div>
  );
}
