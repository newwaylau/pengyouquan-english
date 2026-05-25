import React, { useState, useRef } from 'react';
import EmptyStateCard from './EmptyStateCard';
import { api } from './api/client';
import { IconClose, IconSearch } from './Icons';

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

  const clearSearch = () => handleSearch('');

  return (
    <div className="search-page search-v2-page">
      <div className="search-v2-wrap">
        {onBack && <button className="btn btn-ghost btn-sm" onClick={onBack}>← 返回</button>}
        <header className="search-v2-header">
          <div className="page-eyebrow">SEARCH</div>
          <h1 className="page-title page-title-serif">找到想练的那一句</h1>
          <p className="page-sub">支持中英文关键词，也可以用快捷筛选查看错题和剧集。</p>
        </header>

        <div className="search-v2-box">
          <IconSearch />
          <input
            type="text"
            placeholder="搜索句子（中英文）..."
            value={query}
            onChange={e => handleSearch(e.target.value)}
            autoFocus
          />
          {query && <button className="btn btn-icon btn-sm" onClick={clearSearch} aria-label="清空"><IconClose /></button>}
        </div>

        <div className="search-v2-suggestions">
          {[':wrong', ':got', 'winter', 'king', 'love'].map(item => (
            <button key={item} className="chip" onClick={() => handleSearch(item)}>{item}</button>
          ))}
        </div>

        {loading && <div className="search-loading">搜索中...</div>}
        {!loading && results.length === 0 && query.length >= 2 && (
          <EmptyStateCard variant="search" title="未找到结果" subtitle="试试其他关键词，中英文都可以搜索。" />
        )}

        <div className="search-v2-results">
          {results.map(s => (
            <article key={s.id} className="card card-interactive search-v2-result" onClick={() => onJump(s.id)}>
              <p>{highlight(s.text, query)}</p>
              <div className="search-v2-meta">
                <span className="chip">{s.showName || '英语剧场'}</span>
                <span className="mono">#{s.id}</span>
                <span className="spacer" />
                <button className="btn btn-ghost btn-sm" onClick={e => { e.stopPropagation(); onJump(s.id); }}>练习 →</button>
              </div>
            </article>
          ))}
        </div>

        {query.length < 2 && (
          <section className="card search-v2-recent">
            <div className="cap">最近搜索</div>
            <div className="search-v2-suggestions">
              {['the night', 'winter is coming', '龙母', 'Game of Thrones'].map(item => (
                <button key={item} className="chip" onClick={() => handleSearch(item)}>{item}</button>
              ))}
            </div>
          </section>
        )}
      </div>
    </div>
  );
}

function highlight(text: string, query: string) {
  if (!query || query.startsWith(':')) return text;
  const index = text.toLowerCase().indexOf(query.toLowerCase());
  if (index < 0) return text;
  return <>{text.slice(0, index)}<mark>{text.slice(index, index + query.length)}</mark>{text.slice(index + query.length)}</>;
}
