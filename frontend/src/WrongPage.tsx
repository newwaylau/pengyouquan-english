import React, { useEffect, useState } from 'react';
import { api } from './api/client';

export default function WrongPage() {
  const [items, setItems] = useState<any[]>([]);

  useEffect(() => { api.wrongSentences().then(r => { if (r.code === 200) setItems(r.data); }); }, []);

  const remove = async (sid: number) => {
    await api.removeWrong(sid);
    setItems(items.filter(i => i.sentenceId !== sid));
  };

  if (items.length === 0) return <div className="empty-state">暂无错题 🎉</div>;

  return (
    <div className="wrong-page">
      <h2>📕 错题本</h2>
      {items.map(item => (
        <div key={item.sentenceId} className="wrong-item">
          <div className="wrong-text">{item.text}</div>
          <div className="wrong-meta">{item.showName} · 错{item.errorCount}次</div>
          <button onClick={() => remove(item.sentenceId)} className="small-btn">移除</button>
        </div>
      ))}
    </div>
  );
}
