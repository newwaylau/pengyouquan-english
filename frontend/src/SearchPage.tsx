import React, { useState, useRef } from 'react';
import { api } from './api/client';

export default function SearchPage({ onJump, onBack }: { onJump: (id: number) => void; onBack?: () => void }) {
  const [query, setQuery] = useState('');
  const [results, setResults] = useState<any[]>([]);
  const [loading, setLoading] = useState(false);
  const timer = useRef<ReturnType<typeof setTimeout>>();

  const handleSearch = (q: string) => {
    setQuery(q);
    clearTimeout(timer.current);
    if (q.length < 2) { setResults([]); return; }
    timer.current = setTimeout(async () => {
      setLoading(true);
      const r = await api.search(q);
      if (r.code === 200) setResults(r.data || []);
      setLoading(false);
    }, 300);
  };

  return (
    <div className="search-page">
      {onBack && <button className="back-btn" onClick={onBack}>← 返回</button>}
      <input className="search-input" type="text" placeholder="搜索句子（中英文）..."
        value={query} onChange={e => handleSearch(e.target.value)} autoFocus />
      {loading && <div className="search-loading">搜索中...</div>}
      {!loading && results.length === 0 && query.length >= 2 && (
        <div className="empty-state">无结果</div>
      )}
      {results.map(s => (
        <div key={s.id} className="search-result" onClick={() => onJump(s.id)}>
          <div className="result-text">{s.text}</div>
          <div className="result-meta">{s.showName} · #{s.id}</div>
        </div>
      ))}
    </div>
  );
}
