import React, { useEffect, useState } from 'react';
import EmptyStateCard from './EmptyStateCard';
import { api } from './api/client';
import { IconCelebration, IconBookClosed, IconTarget, IconCalendar, IconCalendarUpcoming, IconClose, IconClock, IconForbidden } from './Icons';

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
          variant="celebration"
          title="暂无错题"
          subtitle="继续保持！做错的句子会自动加入错题本。在练习页遇到不会的句子？多练几遍就会了。"
          actionLabel="← 返回练习"
          onAction={onBack}
        />
      </div>
    );
  }

  return (
    <div className="wrong-page wrong-v2-page">
      <header className="wrong-v2-header">
        <div>
          <div className="page-eyebrow">WRONG BOOK</div>
          <h1 className="page-title">错题复习</h1>
          <p className="page-sub">按间隔重复节奏复习听写错误的句子。</p>
        </div>
        <button className="btn btn-primary" onClick={handleBatchPractice} disabled={dueItems.length === 0}>
          <IconTarget /> 立即复习
        </button>
      </header>

      <section className="wrong-v2-stats">
        <div className="card card-elevated wrong-v2-stat err"><span className="cap">今日到期</span><strong>{dueCount}</strong></div>
        <div className="card card-elevated wrong-v2-stat warn"><span className="cap">即将到期</span><strong>{upcomingCount}</strong></div>
        <div className="card card-elevated wrong-v2-stat"><span className="cap">累计错题</span><strong>{total}</strong></div>
      </section>

      <div className="wrong-v2-toolbar">
        <div className="seg">
          <button className={!filterShow ? 'active' : ''} onClick={() => setFilterShow('')}>全部</button>
          <button className={filterShow ? 'active' : ''} disabled>剧集筛选</button>
        </div>
        <select className="select wrong-v2-select" value={filterShow} onChange={e => setFilterShow(e.target.value)}>
          <option value="">全部剧集</option>
          {showNames.map(name => <option key={name} value={name}>{name}</option>)}
        </select>
        <button className="btn btn-ghost btn-sm" onClick={onBack}>返回练习</button>
      </div>

      {loading && <div className="loading" style={{ padding: '40px 0' }}>加载中...</div>}

      {!loading && (
        <>
          {dueItems.length > 0 && (
            <section className="wrong-v2-section">
              <h2 className="cap"><IconCalendar /> 到期 · {dueCount} 句</h2>
              <div className="wrong-v2-list">{renderItems(dueItems)}</div>
            </section>
          )}
          {upcomingItems.length > 0 && (
            <section className="wrong-v2-section">
              <h2 className="cap"><IconCalendarUpcoming /> 即将到期 · {upcomingCount} 句</h2>
              <div className="wrong-v2-list">{renderItems(upcomingItems)}</div>
            </section>
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
          <div key={item.sentenceId} className="wrong-v2-row" onClick={() => onJump(item.sentenceId)}>
            <span className="mono wrong-v2-id">#{item.sentenceId}</span>
            <div className="wrong-v2-body">
              <div className="wrong-v2-text">{item.text}</div>
              <div className="wrong-v2-meta">
                {item.showName && <span className="chip">{item.showName}</span>}
                <span className="chip chip-err"><IconClose /> 错{item.errorCount}次</span>
                <span className="chip chip-teal"><IconClock /> {getReviewLabel(item.nextReviewAt)}</span>
                {item.isDisabled && <span className="chip chip-warn"><IconForbidden /> 已停用</span>}
              </div>
            </div>
            <button className="btn btn-primary btn-sm" onClick={e => { e.stopPropagation(); onJump(item.sentenceId); }}>复习</button>
            <button className="btn btn-icon btn-sm" onClick={e => e.stopPropagation()}>···</button>
          </div>
        ))}

        {totalPages > 1 && (
          <div className="pagination wrong-v2-pagination">
            <button className="btn btn-ghost btn-sm" disabled={page === 0} onClick={() => setPage(p => p - 1)}>上一页</button>
            <span className="chip">{page + 1} / {totalPages}</span>
            <button className="btn btn-ghost btn-sm" disabled={page >= totalPages - 1} onClick={() => setPage(p => p + 1)}>下一页</button>
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
