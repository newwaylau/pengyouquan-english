import React, { useEffect, useState } from 'react';
import { api } from './api/client';

interface Props {
  onJump: (id: number) => void;
  onBack: () => void;
}

const PAGE_SIZE = 20;

export default function WrongPage({ onJump }: Props) {
  const [items, setItems] = useState<any[]>([]);
  const [page, setPage] = useState(0);
  const totalPages = Math.max(1, Math.ceil(items.length / PAGE_SIZE));
  const paged = items.slice(page * PAGE_SIZE, (page + 1) * PAGE_SIZE);

  useEffect(() => { api.wrongSentences().then(r => { if (r.code === 200) setItems(r.data); }); }, []);

  const remove = async (sid: number) => {
    await api.removeWrong(sid);
    setItems(items.filter(i => i.sentenceId !== sid));
  };

  const clearAll = async () => {
    if (!confirm('确定清空所有错题？')) return;
    for (const item of items) {
      await api.removeWrong(item.sentenceId);
    }
    setItems([]);
  };

  if (items.length === 0) return (
    <div className="wrong-page">
      <button className="back-btn" onClick={onBack}>← 返回练习</button>
      <div className="empty-state">暂无错题 🎉</div>
    </div>
  );

  return (
    <div className="wrong-page">
      <button className="back-btn" onClick={onBack}>← 返回练习</button>
      <div className="wrong-header">
        <h2>📕 错题本（{items.length}题）</h2>
        <button className="clear-btn" onClick={clearAll}>🗑️ 清空全部</button>
      </div>
      {paged.map(item => (
        <div key={item.sentenceId} className="wrong-item clickable" onClick={() => onJump(item.sentenceId)}>
          <div className="wrong-text">{item.text}</div>
          <div className="wrong-meta">{item.showName} · 错{item.errorCount}次</div>
          <button className="small-btn" onClick={e => { e.stopPropagation(); remove(item.sentenceId); }}>移除</button>
        </div>
      ))}
      {totalPages > 1 && (
        <div className="pagination">
          <button disabled={page === 0} onClick={() => setPage(p => p - 1)}>上一页</button>
          <span>第 {page + 1}/{totalPages} 页</span>
          <button disabled={page >= totalPages - 1} onClick={() => setPage(p => p + 1)}>下一页</button>
        </div>
      )}
    </div>
  );
}
