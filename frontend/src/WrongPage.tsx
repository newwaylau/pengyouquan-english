import React, { useEffect, useState } from 'react';
import EmptyStateCard from './EmptyStateCard';
import { api } from './api/client';

interface Props {
  onJump: (id: number) => void;
  onBack: () => void;
}

const PAGE_SIZE = 15;

export default function WrongPage({ onJump, onBack }: Props) {
  // 分组数据
  const [dueItems, setDueItems] = useState<any[]>([]);
  const [upcomingItems, setUpcomingItems] = useState<any[]>([]);
  const [dueCount, setDueCount] = useState(0);
  const [upcomingCount, setUpcomingCount] = useState(0);
  const [total, setTotal] = useState(0);

  const [showNames, setShowNames] = useState<string[]>([]);
  const [filterShow, setFilterShow] = useState('');

  const [page, setPage] = useState(0);
  const [loading, setLoading] = useState(true);

  // 合并列表用于分页和筛选
  const allItems = [...dueItems, ...upcomingItems];
  const totalPages = Math.max(1, Math.ceil(allItems.length / PAGE_SIZE));
  const paged = allItems.slice(page * PAGE_SIZE, (page + 1) * PAGE_SIZE);

  // 加载分组错题列表
  const loadData = async () => {
    setLoading(true);
    try {
      const r = await api.wrongSentencesGrouped();
      if (r.code === 200) {
        const data = r.data;
        let due = data.due || [];
        let upcoming = data.upcoming || [];

        // 可选按剧集筛选
        if (filterShow) {
          due = due.filter((d: any) => d.showName === filterShow);
          upcoming = upcoming.filter((d: any) => d.showName === filterShow);
        }

        setDueItems(due);
        setUpcomingItems(upcoming);
        setDueCount(due.length);
        setUpcomingCount(upcoming.length);
        setTotal(due.length + upcoming.length);
      }
    } finally {
      setLoading(false);
    }
  };

  // 加载剧集列表
  useEffect(() => {
    api.wrongSentenceShows().then(r => {
      if (r.code === 200) setShowNames(r.data || []);
    });
  }, []);

  // 筛选变化时重新加载
  useEffect(() => {
    setPage(0);
    loadData();
  }, [filterShow]);

  // 批量练习入口 — 只出今天要复习的
  const handleBatchPractice = () => {
    if (dueItems.length === 0) return;
    localStorage.setItem('practiceMode', 'wrong');
    onJump(dueItems[0].sentenceId);
  };

  // 空态
  if (!loading && total === 0) {
    return (
      <div className="wrong-page">
        <button className="back-btn" onClick={onBack}>← 返回练习</button>
        <EmptyStateCard
          icon="🎉"
          title="暂无错题"
          subtitle="继续保持！做错的句子会自动加入错题本。在练习页遇到不会的句子？多练几遍就会了。"
          actionLabel="← 返回练习"
          onAction={onBack}
        />
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
          <span className="wrong-count">（共{total}句）</span>
        </div>
      </div>

      {/* 筛选工具栏 */}
      <div className="wrong-toolbar">
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

        {/* 批量复习 — 只出今天要复习的 */}
        <button
          className="batch-review-btn"
          onClick={handleBatchPractice}
          disabled={dueItems.length === 0}
        >
          🎯 开始错题复习（{dueCount}句）
        </button>
      </div>

      {/* 加载态 */}
      {loading && (
        <div className="loading" style={{ padding: '40px 0' }}>
          加载中...
        </div>
      )}

      {!loading && (
        <>
          {/* ⭐ 今天要复习分组 */}
          {dueItems.length > 0 && (
            <div className="wrong-section">
              <h3 className="wrong-section-title due-section">
                📅 今天要复习（{dueCount}句）
              </h3>
              <div className="wrong-section-items">
                {renderItems(dueItems)}
              </div>
            </div>
          )}

          {/* ⚪ 以后复习分组 */}
          {upcomingItems.length > 0 && (
            <div className="wrong-section">
              <h3 className="wrong-section-title upcoming-section">
                🗓️ 以后复习（{upcomingCount}句）
              </h3>
              <div className="wrong-section-items">
                {renderItems(upcomingItems)}
              </div>
            </div>
          )}
        </>
      )}
    </div>
  );

  function renderItems(items: any[]) {
    // 分页
    const start = page * PAGE_SIZE;
    const end = start + PAGE_SIZE;
    const displayItems = allItems.slice(start, end);
    // 过滤出属于这个分组的
    const itemIds = new Set(items.map((i: any) => i.sentenceId));
    const pagedGroup = displayItems.filter((i: any) => itemIds.has(i.sentenceId));

    if (pagedGroup.length === 0) return null;

    return (
      <>
        {pagedGroup.map((item: any) => (
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
                {item.showName && (
                  <span className="wrong-meta-tag show-name">{item.showName}</span>
                )}
                <span className="wrong-meta-tag error-count">
                  ❌ 错{item.errorCount}次
                </span>
                {/* 复习间隔标签 */}
                <span className="wrong-meta-tag review-label">
                  ⏰ {getReviewLabel(item.nextReviewAt)}
                </span>
                {item.isDisabled && (
                  <span className="wrong-meta-tag disabled-label">
                    ⛔ 该句子已停用
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
                [练习]
              </button>
            </div>
          </div>
        ))}

        {/* 分页 */}
        {totalPages > 1 && (
          <div className="pagination">
            <button disabled={page === 0} onClick={() => setPage(p => p - 1)}>上一页</button>
            <span>第 {page + 1}/{totalPages} 页</span>
            <button disabled={page >= totalPages - 1} onClick={() => setPage(p => p + 1)}>下一页</button>
          </div>
        )}
      </>
    );
  }
}

/** 根据 next_review_at 生成可读标签 */
function getReviewLabel(nextReviewAt: string): string {
  if (!nextReviewAt) return '今天复习';
  try {
    const d = new Date(nextReviewAt);
    const now = new Date();
    const diffMs = d.getTime() - now.getTime();
    const diffDays = Math.ceil(diffMs / 86400000);

    if (diffDays <= 0) return '今天复习';
    if (diffDays === 1) return '明天复习';
    return `${diffDays}天后复习`;
  } catch {
    return nextReviewAt;
  }
}
