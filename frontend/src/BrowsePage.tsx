import React, { useEffect, useState } from 'react';
import EmptyStateCard from './EmptyStateCard';
import { api } from './api/client';
import { IconBook, IconEdit } from './Icons';

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

  // 加载剧集列表
  useEffect(() => {
    api.shows().then(r => { if (r.code === 200) setShows(r.data); });
  }, []);

  // 加载选中剧集的句子
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
    <div className="browse-page">
      <div className="browse-header">
        <h2><IconBook /> 句子浏览</h2>
        {onBack && <button className="back-btn" onClick={onBack}>← 返回</button>}
        <div className="browse-controls">
          <select value={selectedShowId ?? ''} onChange={e =>
            handleShowSelect(e.target.value ? Number(e.target.value) : null)}>
            <option value="">-- 选择剧集 --</option>
            {shows.map(s => (
              <option key={s.id} value={s.id}>{s.name} ({s.sentenceCount}句)</option>
            ))}
          </select>
          <div className="jump-input">
            <input type="number" placeholder="句子ID"
              value={jumpNo} onChange={e => setJumpNo(e.target.value)}
              onKeyDown={e => e.key === 'Enter' && handleJump()} />
            <button onClick={handleJump}>练习</button>
          </div>
        </div>
      </div>

      {!selectedShowId && (
        <EmptyStateCard
          variant="book"
          title="请选择剧集"
          subtitle="从上方下拉列表选择一部美剧，开始浏览句子。"
        />
      )}

      {loading && <div className="loading">加载中...</div>}

      {selectedShowId && !loading && sentences.length === 0 && (
        <EmptyStateCard
          variant="document"
          title="该剧集暂无句子"
          subtitle="可能是该剧集正在更新中，请稍后再来。"
        />
      )}

      {sentences.length > 0 && (
        <>
          <div className="browse-info">{showName} · 共 {total} 句 · 第 {page + 1}/{totalPages} 页</div>
          <div className="sentence-list">
            {sentences.map(s => (
              <div key={s.id} className="sentence-item" onClick={() => onJump(s.id)}>
                <span className="sentence-no">#{s.id}</span>
                <div className="sentence-body">
                  <div className="browse-en">{s.textEn || s.text}</div>
                  {s.textCn && <div className="sentence-cn-browse">{s.textCn}</div>}
                </div>
              </div>
            ))}
          </div>
          {totalPages > 1 && (
            <div className="pagination">
              <button disabled={page <= 0} onClick={() => loadSentences(selectedShowId!, page - 1)}>上一页</button>
              <span>第 {page + 1}/{totalPages} 页</span>
              <button disabled={page >= totalPages - 1} onClick={() => loadSentences(selectedShowId!, page + 1)}>下一页</button>
            </div>
          )}
        </>
      )}
    </div>
  );
}
