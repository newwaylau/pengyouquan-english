import React, { useState, useEffect } from 'react';
import { api } from './api/client';
import { BarChart, Bar, XAxis, YAxis, Tooltip, ResponsiveContainer, PieChart, Pie, Cell, CartesianGrid } from 'recharts';

const COLORS = ['#1677ff', '#52c41a', '#faad14', '#ff4d4f', '#722ed1', '#13c2c2'];

export default function AdminPage({ onlineCount }: { onlineCount: number | null }) {
  const [tab, setTab] = useState<'stats' | 'users' | 'settings' | 'notifications' | 'sentence-flags'>('stats');

  return (
    <div className="admin-page">
      <aside className="admin-sidebar">
        <div className="admin-sidebar-header">
          <span className="admin-sidebar-logo">📊</span>
          <span className="admin-sidebar-title">管理后台</span>
        </div>
        <nav className="admin-sidebar-nav">
          <button className={tab === 'stats' ? 'active' : ''} onClick={() => setTab('stats')}>
            <span className="admin-nav-icon">📈</span>
            <span className="admin-nav-label">仪表盘</span>
          </button>
          <button className={tab === 'users' ? 'active' : ''} onClick={() => setTab('users')}>
            <span className="admin-nav-icon">👤</span>
            <span className="admin-nav-label">用户管理</span>
          </button>
          <button className={tab === 'settings' ? 'active' : ''} onClick={() => setTab('settings')}>
            <span className="admin-nav-icon">⚙️</span>
            <span className="admin-nav-label">系统设置</span>
          </button>
          <button className={tab === 'notifications' ? 'active' : ''} onClick={() => setTab('notifications')}>
            <span className="admin-nav-icon">📢</span>
            <span className="admin-nav-label">通知管理</span>
          </button>
          <button className={tab === 'sentence-flags' ? 'active' : ''} onClick={() => setTab('sentence-flags')}>
            <span className="admin-nav-icon">🚩</span>
            <span className="admin-nav-label">句子报告</span>
          </button>
        </nav>
      </aside>
      <main className="admin-content">
        {tab === 'stats' && <AdminDashboard onlineCount={onlineCount} />}
        {tab === 'users' && <UserManagement />}
        {tab === 'settings' && <SystemSettings />}
        {tab === 'notifications' && <NotificationManagement />}
        {tab === 'sentence-flags' && <SentenceFlagManagement />}
      </main>
    </div>
  );
}

function AdminDashboard({ onlineCount }: { onlineCount: number | null }) {
  const [stats, setStats] = useState<any>(null);
  const [retention, setRetention] = useState<any[]>([]);

  useEffect(() => {
    const token = localStorage.getItem('token');
    const headers = { Authorization: `Bearer ${token}` };

    fetch('/api/admin/stats', { headers })
      .then(r => r.json())
      .then(r => { if (r.code === 200) setStats(r.data); });

    fetch('/api/admin/retention', { headers })
      .then(r => r.json())
      .then(r => { if (r.code === 200) setRetention(r.data || []); });
  }, []);

  if (!stats) return <div className="loading">加载中...</div>;

  const { totalUsers, totalPractices, todayPractices,
    dailyTrend, accuracyTrend, modeDistribution, showRanking } = stats;

  return (
    <div className="admin-dashboard">
      <h2>📈 总览</h2>
      <div className="stats-cards">
        <div className="stat-card">
          <div className="stat-value">{totalUsers}</div>
          <div className="stat-label">总用户数</div>
        </div>
        <div className="stat-card">
          <div className="stat-value">{totalPractices}</div>
          <div className="stat-label">总练习数</div>
        </div>
        <div className="stat-card">
          <div className="stat-value">{todayPractices}</div>
          <div className="stat-label">今日练习</div>
        </div>
        <div className="stat-card">
          <div className="stat-value">{showRanking?.length || 0}</div>
          <div className="stat-label">剧集数</div>
        </div>
      </div>

      {/* 每日练习趋势折线图 */}
      {dailyTrend?.length > 0 && (
        <>
          <h3 style={{ marginTop: 24 }}>📅 每日练习趋势（近30天）</h3>
          <div className="chart-container">
            <ResponsiveContainer width="100%" height={220}>
              <BarChart data={dailyTrend}>
                <CartesianGrid strokeDasharray="3 3" />
                <XAxis dataKey="day" tick={{ fontSize: 11 }} angle={-30} textAnchor="end" height={50} />
                <YAxis tick={{ fontSize: 12 }} />
                <Tooltip />
                <Bar dataKey="count" fill="#1677ff" radius={[4, 4, 0, 0]} name="练习数" />
              </BarChart>
            </ResponsiveContainer>
          </div>
        </>
      )}

      {/* 每日正确率趋势 */}
      {accuracyTrend?.length > 0 && (
        <>
          <h3 style={{ marginTop: 24 }}>🎯 每日正确率趋势（近30天）</h3>
          <div className="chart-container">
            <ResponsiveContainer width="100%" height={220}>
              <BarChart data={accuracyTrend}>
                <CartesianGrid strokeDasharray="3 3" />
                <XAxis dataKey="day" tick={{ fontSize: 11 }} angle={-30} textAnchor="end" height={50} />
                <YAxis tick={{ fontSize: 12 }} unit="%" domain={[0, 100]} />
                <Tooltip formatter={(v: number) => `${v}%`} />
                <Bar dataKey="accuracy" fill="#52c41a" radius={[4, 4, 0, 0]} name="正确率" />
              </BarChart>
            </ResponsiveContainer>
          </div>
        </>
      )}

      {/* 用户留存分析 */}
      {retention.length > 0 && (
        <>
          <h3 style={{ marginTop: 24 }}>👥 用户留存分析（近30天）</h3>
          <div className="chart-container">
            <ResponsiveContainer width="100%" height={220}>
              <BarChart data={retention}>
                <CartesianGrid strokeDasharray="3 3" />
                <XAxis dataKey="day" tick={{ fontSize: 11 }} angle={-30} textAnchor="end" height={50} />
                <YAxis tick={{ fontSize: 12 }} />
                <Tooltip />
                <Bar dataKey="activeUsers" fill="#722ed1" radius={[4, 4, 0, 0]} name="活跃用户" />
              </BarChart>
            </ResponsiveContainer>
          </div>
        </>
      )}

      {/* 练习模式分布 */}
      {modeDistribution?.length > 0 && (
        <div className="chart-row">
          <div className="chart-half">
            <h3>📊 练习模式分布</h3>
            <div className="chart-container chart-pie">
              <ResponsiveContainer width="100%" height={250}>
                <PieChart>
                  <Pie data={modeDistribution} dataKey="count" nameKey="mode"
                    cx="50%" cy="50%" outerRadius={80} label={({ mode, count }) => `${mode}: ${count}`}>
                    {modeDistribution.map((_: any, i: number) => (
                      <Cell key={i} fill={COLORS[i % COLORS.length]} />
                    ))}
                  </Pie>
                  <Tooltip />
                </PieChart>
              </ResponsiveContainer>
            </div>
          </div>

          {/* 剧集热度排行 */}
          <div className="chart-half">
            <h3>🔥 剧集热度排行</h3>
            <div className="show-ranking">
              {showRanking?.slice(0, 10).map((s: any, i: number) => (
                <div key={s.id} className="ranking-item">
                  <span className="ranking-num">{i + 1}</span>
                  <span className="ranking-name">{s.name}</span>
                  <span className="ranking-bar" style={{ width: `${Math.min(100, (s.sentenceCount / Math.max(...showRanking.map((x: any) => x.sentenceCount))) * 100)}%` }} />
                  <span className="ranking-count">{s.sentenceCount}句</span>
                </div>
              ))}
            </div>
          </div>
        </div>
      )}
    </div>
  );
}

function UserManagement() {
  const [users, setUsers] = useState<any[]>([]);
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  const [total, setTotal] = useState(0);
  const [roleFilter, setRoleFilter] = useState('');
  const [search, setSearch] = useState('');

  const fetchUsers = async (p: number) => {
    let url = `/api/admin/users?page=${p}&size=20`;
    if (roleFilter) url += `&role=${encodeURIComponent(roleFilter)}`;
    const r = await fetch(url, {
      headers: { Authorization: `Bearer ${localStorage.getItem('token')}` }
    }).then(r => r.json());
    if (r.code === 200) {
      setUsers(r.data.users);
      setPage(r.data.page);
      setTotalPages(r.data.totalPages);
      setTotal(r.data.total);
    }
  };

  useEffect(() => { fetchUsers(0); }, [roleFilter]);

  const doSearch = async () => {
    if (!search || search.length < 2) return;
    const r = await fetch(`/api/users/search?q=${encodeURIComponent(search)}`, {
      headers: { Authorization: `Bearer ${localStorage.getItem('token')}` }
    }).then(r => r.json());
    if (r.code === 200) setUsers(r.data);
  };

  const toggleUser = async (id: number) => {
    await fetch(`/api/users/${id}/toggle-enabled`, {
      method: 'PUT',
      headers: { Authorization: `Bearer ${localStorage.getItem('token')}` }
    });
    fetchUsers(page);
  };

  return (
    <div className="user-management">
      <h2>👤 用户管理 <span className="user-total">（共 {total} 人）</span></h2>

      <div className="user-toolbar">
        {/* 角色筛选下拉框 */}
        <select value={roleFilter} onChange={e => { setRoleFilter(e.target.value); setPage(0); }}
                className="role-filter">
          <option value="">🎭 全部角色</option>
          <option value="admin">管理员</option>
          <option value="user">普通用户</option>
        </select>

        {/* 搜索框 */}
        <div className="search-bar">
          <input value={search} onChange={e => setSearch(e.target.value)}
                 onKeyDown={e => e.key === 'Enter' && doSearch()}
                 placeholder="搜索用户（邮箱/昵称）" />
          <button onClick={doSearch}>搜索</button>
        </div>
      </div>

      <table className="admin-table">
        <thead><tr><th>ID</th><th>邮箱</th><th>昵称</th><th>🎭 角色</th><th>状态</th><th>操作</th></tr></thead>
        <tbody>
          {users.map((u: any) => (
            <tr key={u.id} className={u.role === 'admin' ? 'row-admin' : ''}>
              <td>{u.id}</td><td>{u.email}</td><td>{u.nickname}</td>
              <td>
                <span className={`role-badge role-${u.role}`}>
                  {u.role === 'admin' ? '管理员' : '用户'}
                </span>
              </td>
              <td>{u.enabled !== false ? '正常' : '禁用'}</td>
              <td><button onClick={() => toggleUser(u.id)} className="action-btn">
                {u.enabled !== false ? '禁用' : '启用'}
              </button></td>
            </tr>
          ))}
        </tbody>
      </table>

      {/* 分页控件 */}
      {totalPages > 1 && (
        <div className="pagination admin-pagination">
          <button disabled={page <= 0} onClick={() => fetchUsers(page - 1)}>上一页</button>
          <span className="page-info">第 {page + 1} / {totalPages} 页</span>
          <button disabled={page >= totalPages - 1} onClick={() => fetchUsers(page + 1)}>下一页</button>
        </div>
      )}
    </div>
  );
}

function SystemSettings() {
  const [registrationEnabled, setRegistrationEnabled] = useState(true);
  const [announcement, setAnnouncement] = useState('');
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [message, setMessage] = useState('');

  useEffect(() => {
    const token = localStorage.getItem('token');
    fetch('/api/admin/settings', {
      headers: { Authorization: `Bearer ${token}` }
    })
      .then(r => r.json())
      .then(r => {
        if (r.code === 200 && r.data) {
          for (const s of r.data) {
            if (s.settingKey === 'registrationEnabled') {
              setRegistrationEnabled(s.settingValue === 'true');
            } else if (s.settingKey === 'announcement') {
              setAnnouncement(s.settingValue || '');
            }
          }
        }
        setLoading(false);
      });
  }, []);

  const handleSave = async () => {
    setSaving(true);
    setMessage('');
    const token = localStorage.getItem('token');
    const r = await fetch('/api/admin/settings', {
      method: 'PUT',
      headers: {
        'Content-Type': 'application/json',
        Authorization: `Bearer ${token}`
      },
      body: JSON.stringify({
        registrationEnabled: registrationEnabled ? 'true' : 'false',
        announcement: announcement
      })
    }).then(r => r.json());
    setSaving(false);
    if (r.code === 200) {
      setMessage('保存成功');
    } else {
      setMessage('保存失败');
    }
  };

  if (loading) return <div className="loading">加载中...</div>;

  return (
    <div className="system-settings">
      <h2>⚙️ 系统设置</h2>

      <div className="settings-section">
        <label>注册开关</label>
        <div className="checkbox-row">
          <input
            type="checkbox"
            checked={registrationEnabled}
            onChange={e => setRegistrationEnabled(e.target.checked)}
          />
          <span>允许新用户注册</span>
        </div>
      </div>

      <div className="settings-section">
        <label>公告内容</label>
        <textarea
          className="settings-textarea"
          value={announcement}
          onChange={e => setAnnouncement(e.target.value)}
          placeholder="输入公告内容（留空则不显示）"
          rows={4}
        />
        <div className="settings-hint">公告将显示在页面顶部导航栏下方</div>
      </div>

      <button className="btn-primary save-btn" onClick={handleSave} disabled={saving}>
        {saving ? '保存中...' : '保存设置'}
      </button>

      {message && (
        <div className={`upload-feedback ${message === '保存成功' ? 'success' : 'error'}`}>
          {message}
        </div>
      )}
    </div>
  );
}

function NotificationManagement() {
  const [notifications, setNotifications] = useState<any[]>([]);
  const [title, setTitle] = useState('');
  const [content, setContent] = useState('');
  const [published, setPublished] = useState(false);
  const [message, setMessage] = useState('');

  const token = localStorage.getItem('token');
  const headers = { Authorization: `Bearer ${token}`, 'Content-Type': 'application/json' };

  const fetchNotifications = async () => {
    const r = await fetch('/api/admin/notifications', { headers }).then(r => r.json());
    if (r.code === 200) setNotifications(r.data || []);
  };

  useEffect(() => { fetchNotifications(); }, []);

  const handleCreate = async () => {
    if (!title.trim()) { setMessage('请输入通知标题'); return; }
    setMessage('');
    const r = await fetch('/api/admin/notifications', {
      method: 'POST',
      headers,
      body: JSON.stringify({ title: title.trim(), content: content.trim(), published })
    }).then(r => r.json());
    if (r.code === 200) {
      setMessage('创建成功');
      setTitle('');
      setContent('');
      setPublished(false);
      fetchNotifications();
    } else {
      setMessage('创建失败');
    }
  };

  const handleDelete = async (id: number) => {
    const r = await fetch(`/api/admin/notifications/${id}`, {
      method: 'DELETE',
      headers
    }).then(r => r.json());
    if (r.code === 200) {
      fetchNotifications();
    }
  };

  return (
    <div className="notification-management">
      <h2>📢 通知管理</h2>

      <div className="notification-form">
        <h3>新建通知</h3>
        <div className="settings-section">
          <label>标题</label>
          <input
            className="subtitle-input"
            value={title}
            onChange={e => setTitle(e.target.value)}
            placeholder="通知标题"
          />
        </div>
        <div className="settings-section">
          <label>内容</label>
          <textarea
            className="settings-textarea"
            value={content}
            onChange={e => setContent(e.target.value)}
            placeholder="通知内容（可选）"
            rows={4}
          />
        </div>
        <div className="settings-section">
          <label>发布状态</label>
          <div className="checkbox-row">
            <input
              type="checkbox"
              checked={published}
              onChange={e => setPublished(e.target.checked)}
            />
            <span>创建后立即发布</span>
          </div>
        </div>
        <button className="btn-primary save-btn" onClick={handleCreate}>创建通知</button>
        {message && (
          <div className={`upload-feedback ${message === '创建成功' ? 'success' : 'error'}`}>
            {message}
          </div>
        )}
      </div>

      <div className="settings-section" style={{ marginTop: 32 }}>
        <h3>通知列表</h3>
        {notifications.length === 0 ? (
          <div className="empty-state" style={{ padding: '20px 0' }}>暂无通知</div>
        ) : (
          <table className="admin-table">
            <thead>
              <tr>
                <th>ID</th>
                <th>标题</th>
                <th>状态</th>
                <th>发布时间</th>
                <th>操作</th>
              </tr>
            </thead>
            <tbody>
              {notifications.map((n: any) => (
                <tr key={n.id}>
                  <td>{n.id}</td>
                  <td>{n.title}</td>
                  <td>
                    <span className={`role-badge ${n.published ? 'role-admin' : ''}`}>
                      {n.published ? '已发布' : '草稿'}
                    </span>
                  </td>
                  <td style={{ fontSize: 13, color: 'var(--text-secondary)' }}>
                    {n.createdAt ? new Date(n.createdAt).toLocaleString() : '-'}
                  </td>
                  <td>
                    <button onClick={() => handleDelete(n.id)} className="action-btn danger">
                      删除
                    </button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        )}
      </div>
    </div>
  );
}

/** 🚩 句子报告管理 */
function SentenceFlagManagement() {
  const [flaggedSentences, setFlaggedSentences] = useState<any[]>([]);
  const [loading, setLoading] = useState(true);
  const [message, setMessage] = useState('');

  const token = localStorage.getItem('token');
  const headers = { Authorization: `Bearer ${token}`, 'Content-Type': 'application/json' };

  const fetchFlagged = async () => {
    setLoading(true);
    const r = await api.adminFlaggedSentences();
    setLoading(false);
    if (r.code === 200) setFlaggedSentences(r.data || []);
  };

  useEffect(() => { fetchFlagged(); }, []);

  const handleDisable = async (sentenceId: number) => {
    const reason = prompt('请输入停用原因：');
    if (reason === null) return; // 用户取消
    setMessage('');
    const r = await api.adminDisableSentence(sentenceId, reason || '');
    if (r.code === 200) {
      setMessage('已停用该句子');
      fetchFlagged();
    } else {
      setMessage('操作失败');
    }
  };

  const handleEnable = async (sentenceId: number) => {
    setMessage('');
    const r = await api.adminEnableSentence(sentenceId);
    if (r.code === 200) {
      setMessage('已恢复该句子');
      fetchFlagged();
    } else {
      setMessage('操作失败');
    }
  };

  const handleApprove = async (sentenceId: number) => {
    // 审核通过：不做任何修改，只是标记为已审核（在提示中记录）
    setMessage('已标记为审核通过');
  };

  if (loading) return <div className="loading">加载中...</div>;

  return (
    <div className="notification-management">
      <h2>🚩 句子报告</h2>
      {message && (
        <div className={`upload-feedback ${message.includes('失败') ? 'error' : 'success'}`}>
          {message}
        </div>
      )}

      {flaggedSentences.length === 0 ? (
        <div className="empty-state" style={{ padding: '40px 0' }}>暂无被举报的句子</div>
      ) : (
        <table className="admin-table">
          <thead>
            <tr>
              <th>ID</th>
              <th>句子原文</th>
              <th>剧集名</th>
              <th>举报数</th>
              <th>状态</th>
              <th>操作</th>
            </tr>
          </thead>
          <tbody>
            {flaggedSentences.map((s: any) => (
              <tr key={s.sentenceId} className={s.isDisabled ? 'row-disabled' : ''}>
                <td>{s.sentenceId}</td>
                <td style={{ maxWidth: 300, overflow: 'hidden', textOverflow: 'ellipsis' }}>
                  {s.text}
                </td>
                <td>{s.showName}</td>
                <td><span className="flag-count-badge">{s.flagCount}</span></td>
                <td>
                  {s.isDisabled ? (
                    <span className="role-badge" style={{ background: 'var(--danger)', color: '#fff' }}>
                      已停用
                    </span>
                  ) : (
                    <span className="role-badge role-admin">正常</span>
                  )}
                </td>
                <td>
                  {s.isDisabled ? (
                    <button
                      className="action-btn primary"
                      onClick={() => handleEnable(s.sentenceId)}
                    >
                      恢复
                    </button>
                  ) : (
                    <>
                      <button
                        className="action-btn primary"
                        onClick={() => handleApprove(s.sentenceId)}
                      >
                        ✅ 审核通过
                      </button>
                      <button
                        className="action-btn danger"
                        onClick={() => handleDisable(s.sentenceId)}
                      >
                        ⛔ 停用
                      </button>
                    </>
                  )}
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      )}
    </div>
  );
}
