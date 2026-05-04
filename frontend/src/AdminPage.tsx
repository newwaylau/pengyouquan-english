import React, { useState, useEffect } from 'react';
import { api } from './api/client';

export default function AdminPage() {
  const [tab, setTab] = useState<'users' | 'stats'>('stats');

  return (
    <div className="admin-page">
      <aside className="admin-sidebar">
        <h3>📊 管理后台</h3>
        <button className={tab === 'stats' ? 'active' : ''} onClick={() => setTab('stats')}>仪表盘</button>
        <button className={tab === 'users' ? 'active' : ''} onClick={() => setTab('users')}>用户管理</button>
      </aside>
      <main className="admin-content">
        {tab === 'stats' && <StatsDashboard />}
        {tab === 'users' && <UserManagement />}
      </main>
    </div>
  );
}

function StatsDashboard() {
  const [stats, setStats] = useState<any>(null);
  const [users, setUsers] = useState<any[]>([]);

  useEffect(() => {
    api.stats().then(r => { if (r.code === 200) setStats(r.data); });
    api.shows().then(r => { if (r.code === 200) setUsers(r.data); });
  }, []);

  return (
    <div className="stats-dashboard">
      <h2>📈 练习统计</h2>
      {stats && (
        <div className="stats-cards">
          <div className="stat-card"><div className="stat-value">{stats.totalPractices}</div><div className="stat-label">总练习数</div></div>
          <div className="stat-card"><div className="stat-value">{stats.todayPractices}</div><div className="stat-label">今日</div></div>
          <div className="stat-card"><div className="stat-value">{stats.accuracy}%</div><div className="stat-label">正确率</div></div>
          <div className="stat-card"><div className="stat-value">{stats.wrongCount}</div><div className="stat-label">错题</div></div>
        </div>
      )}
      <h2 style={{marginTop:24}}>📺 剧集概览</h2>
      {users.length > 0 && (
        <table className="admin-table">
          <thead><tr><th>剧集</th><th>句子数</th></tr></thead>
          <tbody>
            {users.map((s: any) => (
              <tr key={s.id}><td>{s.name}</td><td>{s.sentenceCount}</td></tr>
            ))}
          </tbody>
        </table>
      )}
    </div>
  );
}

function UserManagement() {
  const [users, setUsers] = useState<any[]>([]);
  const [search, setSearch] = useState('');
  const [me, setMe] = useState<any>(null);

  useEffect(() => { api.me().then(r => { if (r.code === 200) setMe(r.data); }); }, []);

  const doSearch = async () => {
    if (!search || search.length < 2) return;
    const r = await api.me().then(() => fetch(`/api/users/search?q=${encodeURIComponent(search)}`, {
      headers: { Authorization: `Bearer ${localStorage.getItem('token')}` }
    }).then(r => r.json()));
    if (r.code === 200) setUsers(r.data);
  };

  const toggleUser = async (id: number) => {
    await fetch(`/api/users/${id}/toggle-enabled`, {
      method: 'PUT',
      headers: { Authorization: `Bearer ${localStorage.getItem('token')}` }
    });
    doSearch();
  };

  return (
    <div className="user-management">
      <h2>👤 用户管理</h2>
      <div className="search-bar">
        <input value={search} onChange={e => setSearch(e.target.value)} placeholder="搜索用户（邮箱/昵称）" />
        <button onClick={doSearch}>搜索</button>
      </div>
      <table className="admin-table">
        <thead><tr><th>ID</th><th>邮箱</th><th>昵称</th><th>角色</th><th>状态</th><th>操作</th></tr></thead>
        <tbody>
          {users.map((u: any) => (
            <tr key={u.id}>
              <td>{u.id}</td><td>{u.email}</td><td>{u.nickname}</td>
              <td>{u.role}</td>
              <td>{u.enabled !== false ? '正常' : '禁用'}</td>
              <td><button onClick={() => toggleUser(u.id)} className="small-btn">
                {u.enabled !== false ? '禁用' : '启用'}
              </button></td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
}
