import React, { useEffect, useState } from 'react';
import { api } from './api/client';

interface Props {
  onJump: (id: number) => void;
  onBack: () => void;
}

const PAGE_SIZE = 15;

export default function WrongPage({ onJump, onBack }: Props) {
  const [items, setItems] = useState<any[]>([]);
  const [showNames, setShowNames] = useState<string[]>([]);
  const [filterShow, setFilterShow] = useState('');
  const [sortBy, setSortBy] = useState<'errorCount' | 'lastPracticedAt'>('errorCount');
  const [sortDir, setSortDir] = useState<'desc' | 'asc'>('desc');
  const [page, setPage] = useState(0);
  const [loading, setLoading] = useState(true);

  const totalPages = Math.max(1, Math.ceil(items.length / PAGE_SIZE));
  const paged = items.slice(page * PAGE_SIZE, (page + 1) * PAGE_SIZE);

  // 加载错题列表
  const loadData = async () => {
    setLoading(true);
    const params: any = { sortBy, sortDir };
    if (filterShow) params.showName = filterShow;
    const r = await api.wrongSentences(params);
    if (r.code === 200) setItems(r.data);
    setLoading(false);
  };

  // 加载剧集列表
  useEffect(() => {
    api.wrongSentenceShows().then(r => {
      if (r.code === 200) setShowNames(r.data || []);
    });
  }, []);

  // 筛选/排序变化时重新加载
  useEffect(() => {
    setPage(0);
    loadData();
  }, [filterShow, sortBy, sortDir]);

  // 标记已掌握（从列表即时移除）
  const handleMaster = async (e: React.MouseEvent, sid: number) => {
    e.stopPropagation();
    await api.masterSentence(sid);
    setItems(items.filter(i => i.sentenceId !== sid));
  };

  // 批量练习入口
  const handleBatchPractice = () => {
    // 跳转到 PracticePage 并传递 wrong 模式标记
    // 通过 localStorage 传递模式标记
    localStorage.setItem('practiceMode', 'wrong');
    // 如果没有错题，不跳转
    if (items.length === 0) return;
    // 跳转到第一个错题的练习
    onJump(items[0].sentenceId);
  };

  // 空态
  if (!loading && items.length === 0) {
    return (
      <div className="wrong-page">
        <button className="back-btn" onClick={onBack}>← 返回练习</button>
        <div className="empty-state">
          <div style={{ fontSize: '2.5rem', marginBottom: 12 }}>🎉</div>
          <div style={{ fontSize: '1.25rem', fontWeight: 600, marginBottom: 8 }}>暂无错题</div>
          <div style={{ color: 'var(--text-secondary)', fontSize: '0.875rem', lineHeight: 1.6 }}>
            继续保持！做错的句子会自动加入错题本<br />
            在练习页遇到不会的句子？多练几遍就会了
          </div>
          <button
            className="btn-primary"
            style={{ marginTop: 20 }}
            onClick={onBack}
          >
            ← 返回练习
          </button>
        </div>
      </div>
    );
  }

  return (
    <div className="wrong-page">
      {/* 顶部导航 */}
      <div className="wrong-header">
        <div style={{ display: 'flex', alignItems: 'center', gap: 12 }}>
          <button className="back-btn" onClick={onBack}>← 返回</button>
          <h2>📕 错题复习</h2>
          <span className="wrong-count">({items.length}题)</span>
        </div>
      </div>

      {/* 筛选 & 排序工具栏 */}
      <div className="wrong-toolbar">
        {/* 剧集筛选 */}
        <div className="wrong-filter-group">
          <label className="wrong-filter-label">剧集</label>
          <select
            className="wrong-filter-select"
            value={filterShow}
            onChange={e => setFilterShow(e.target.value)}
          >
            <option value="">全部剧集</option>
            {showNames.map(name => (
              <option key={name} value={name}>{name}</option>
            ))}
          </select>
        </div>

        {/* 排序 */}
        <div className="wrong-filter-group">
          <label className="wrong-filter-label">排序</label>
          <select
            className="wrong-filter-select"
            value={`${sortBy}:${sortDir}`}
            onChange={e => {
              const [b, d] = e.target.value.split(':');
              setSortBy(b as any);
              setSortDir(d as any);
            }}
          >
            <option value="errorCount:desc">错误次数 ↓</option>
            <option value="errorCount:asc">错误次数 ↑</option>
            <option value="lastPracticedAt:desc">最近练习 ↓</option>
            <option value="lastPracticedAt:asc">最近练习 ↑</option>
          </select>
        </div>

        {/* 批量复习 */}
        <button
          className="batch-review-btn"
          onClick={handleBatchPractice}
          disabled={items.length === 0}
        >
          🎯 开始错题复习
        </button>
      </div>

      {/* 加载态 */}
      {loading && (
        <div className="loading" style={{ padding: '40px 0' }}>
          加载中...
        </div>
      )}

      {/* 错题列表 */}
      {!loading && paged.map(item => (
        <div
          key={item.sentenceId}
          className="wrong-item clickable"
          onClick={() => onJump(item.sentenceId)}
        >
          <div className="wrong-item-body">
            {/* 英文原文 */}
            <div className="wrong-text">{item.text}</div>

            {/* 元信息行 */}
            <div className="wrong-meta-row">
              {/* 剧集名 */}
              {item.showName && (
                <span className="wrong-meta-tag show-name">{item.showName}</span>
              )}
              {/* 错误次数 */}
              <span className="wrong-meta-tag error-count">
                ❌ 错{item.errorCount}次
              </span>
              {/* 最后练习时间 */}
              {item.lastPracticedAt && (
                <span className="wrong-meta-tag last-practice">
                  🕐 {formatTime(item.lastPracticedAt)}
                </span>
              )}
            </div>
          </div>

          {/* 操作按钮组 */}
          <div className="wrong-item-actions">
            <button
              className="small-btn practice-btn"
              onClick={e => { e.stopPropagation(); onJump(item.sentenceId); }}
            >
              练习
            </button>
            <button
              className="small-btn master-btn"
              onClick={e => handleMaster(e, item.sentenceId)}
            >
              ✅ 已掌握
            </button>
          </div>
        </div>
      ))}

      {/* 分页 */}
      {!loading && totalPages > 1 && (
        <div className="pagination">
          <button disabled={page === 0} onClick={() => setPage(p => p - 1)}>上一页</button>
          <span>第 {page + 1}/{totalPages} 页</span>
          <button disabled={page >= totalPages - 1} onClick={() => setPage(p => p + 1)}>下一页</button>
        </div>
      )}
    </div>
  );
}

/** 格式化时间显示 */
function formatTime(t: string): string {
  try {
    const d = new Date(t);
    const now = new Date();
    const diffMs = now.getTime() - d.getTime();
    const diffDays = Math.floor(diffMs / 86400000);

    if (diffDays === 0) {
      const h = d.getHours().toString().padStart(2, '0');
      const m = d.getMinutes().toString().padStart(2, '0');
      return `今天 ${h}:${m}`;
    }
    if (diffDays === 1) {
      const h = d.getHours().toString().padStart(2, '0');
      const m = d.getMinutes().toString().padStart(2, '0');
      return `昨天 ${h}:${m}`;
    }
    if (diffDays < 7) return `${diffDays}天前`;
    return `${d.getMonth() + 1}/${d.getDate()}`;
  } catch {
    return t.substring(0, 10);
  }
}
