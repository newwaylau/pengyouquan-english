import React, { useState, useEffect } from 'react';
import { api } from './api/client';
import { BarChart, Bar, XAxis, YAxis, Tooltip, ResponsiveContainer, PieChart, Pie, Cell, CartesianGrid } from 'recharts';

const COLORS = ['#1677ff', '#52c41a', '#faad14', '#ff4d4f', '#722ed1', '#13c2c2'];

export default function AdminPage() {
  const [tab, setTab] = useState<'stats' | 'users'>('stats');

  return (
    <div className="admin-page">
      <aside className="admin-sidebar">
        <h3>📊 管理后台</h3>
        <button className={tab === 'stats' ? 'active' : ''} onClick={() => setTab('stats')}>仪表盘</button>
        <button className={tab === 'users' ? 'active' : ''} onClick={() => setTab('users')}>用户管理</button>
      </aside>
      <main className="admin-content">
        {tab === 'stats' && <AdminDashboard />}
        {tab === 'users' && <UserManagement />}
      </main>
    </div>
  );
}

function AdminDashboard() {
  const [stats, setStats] = useState<any>(null);

  useEffect(() => {
    fetch('/api/admin/stats', {
      headers: { Authorization: `Bearer ${localStorage.getItem('token')}` }
    }).then(r => r.json()).then(r => { if (r.code === 200) setStats(r.data); });
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
  const [search, setSearch] = useState('');

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
