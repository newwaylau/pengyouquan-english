import React, { useEffect, useState } from 'react';
import EmptyStateCard from './EmptyStateCard';
import { api } from './api/client';
import { IconBook, IconEdit, IconSearch } from './Icons';

interface Props {
  onJump: (id: number) => void;
  onBack?: () => void;
}

export default function BrowsePage({ onJump, onBack }: Props) {
  const [shows, setShows] = useState<any[]>([]);
  const [selectedShowId, setSelectedShowId] = useState<number | null>(null);
  const [sentences, setSentences] = useState<any[]>([]);
  const [total, setTotal] = useState(0);
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  const [showName, setShowName] = useState('');
  const [loading, setLoading] = useState(false);
  const [jumpNo, setJumpNo] = useState('');

  useEffect(() => {
    api.shows().then(r => { if (r.code === 200) setShows(r.data); });
  }, []);

  const loadSentences = (showId: number, pg: number) => {
    setLoading(true);
    fetch(`/api/show/${showId}/sentences?page=${pg}&size=20`)
      .then(r => r.json())
      .then(r => {
        if (r.code === 200) {
          setSentences(r.data.sentences || []);
          setTotal(r.data.total);
          setTotalPages(r.data.totalPages);
          setShowName(r.data.showName);
          setPage(pg);
        }
        setLoading(false);
      });
  };

  const handleShowSelect = (showId: number | null) => {
    setSelectedShowId(showId);
    if (showId) loadSentences(showId, 0);
    else setSentences([]);
  };

  const handleJump = () => {
    const n = parseInt(jumpNo);
    if (!isNaN(n) && n > 0) onJump(n);
  };

  return (
    <div className="browse-page browse-v2-page">
      <header className="browse-v2-header">
        <div>
          <div className="page-eyebrow">LIBRARY</div>
          <h1 className="page-title">句子浏览</h1>
          <p className="page-sub">按剧集浏览字幕句库，快速跳转到指定句子练习。</p>
        </div>
        <div className="browse-v2-search">
          <IconSearch />
          <input
            className="input"
            type="number"
            placeholder="句子 ID"
            value={jumpNo}
            onChange={e => setJumpNo(e.target.value)}
            onKeyDown={e => e.key === 'Enter' && handleJump()}
          />
          <button className="btn btn-primary" onClick={handleJump}>练习</button>
        </div>
      </header>

      <div className="browse-v2-filters">
        <div className="seg">
          <button className="active">全部</button>
          <button>学习中</button>
          <button>已收藏</button>
          <button>热门</button>
          <button>新增</button>
        </div>
        {onBack && <button className="btn btn-ghost btn-sm" onClick={onBack}>返回</button>}
      </div>

      <section className="browse-v2-show-grid">
        {shows.map((s, index) => (
          <button
            key={s.id}
            className={`card card-interactive browse-v2-show ${selectedShowId === s.id ? 'active' : ''}`}
            onClick={() => handleShowSelect(s.id)}
          >
            <span className="browse-v2-poster" style={{ ['--poster-hue' as any]: `${170 + index * 24}deg` }}>
              <span>{abbr(s.name)}</span>
            </span>
            <span className="browse-v2-show-title">{s.name}</span>
            <span className="browse-v2-show-meta">{s.sentenceCount || 0} 句 · S2</span>
            <span className="progress"><span className="progress-fill" style={{ width: `${Math.min(100, (s.sentenceCount || 0) % 100)}%` }} /></span>
          </button>
        ))}
      </section>

      {!selectedShowId && (
        <EmptyStateCard variant="book" title="请选择剧集" subtitle="从上方卡片选择一部美剧，开始浏览句子。" />
      )}

      {loading && <div className="loading">加载中...</div>}

      {selectedShowId && !loading && sentences.length === 0 && (
        <EmptyStateCard variant="document" title="该剧集暂无句子" subtitle="可能是该剧集正在更新中，请稍后再来。" />
      )}

      {sentences.length > 0 && (
        <section className="card browse-v2-table-card">
          <div className="browse-v2-info">
            <span className="cap">{showName}</span>
            <span className="chip">共 {total} 句</span>
          </div>
          <table className="table browse-v2-table">
            <tbody>
              {sentences.map(s => (
                <tr key={s.id} onClick={() => onJump(s.id)}>
                  <td className="mono">#{s.id}</td>
                  <td>
                    <div className="browse-v2-en">{s.textEn || s.text}</div>
                    {s.textCn && <div className="browse-v2-cn">{s.textCn}</div>}
                  </td>
                  <td><button className="btn btn-ghost btn-sm" onClick={e => { e.stopPropagation(); onJump(s.id); }}><IconEdit /> 练习 →</button></td>
                </tr>
              ))}
            </tbody>
          </table>
          {totalPages > 1 && (
            <div className="pagination browse-v2-pagination">
              <button className="btn btn-ghost btn-sm" disabled={page <= 0} onClick={() => loadSentences(selectedShowId!, page - 1)}>上一页</button>
              <span className="chip">{page + 1} / {totalPages}</span>
              <button className="btn btn-ghost btn-sm" disabled={page >= totalPages - 1} onClick={() => loadSentences(selectedShowId!, page + 1)}>下一页</button>
            </div>
          )}
        </section>
      )}
    </div>
  );
}

function abbr(name: string) {
  return name.split(/\s+/).map(part => part[0]).join('').slice(0, 3).toUpperCase() || 'TV';
}
