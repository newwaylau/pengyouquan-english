import React, { useState, useEffect } from 'react';
import { api } from './api/client';
import { BarChart, Bar, XAxis, YAxis, Tooltip, ResponsiveContainer, PieChart, Pie, Cell, CartesianGrid } from 'recharts';

const COLORS = ['#1677ff', '#52c41a', '#faad14', '#ff4d4f', '#722ed1', '#13c2c2'];

export default function AdminPage() {
  const [tab, setTab] = useState<'stats' | 'users' | 'export'>('stats');

  return (
    <div className="admin-page">
      <aside className="admin-sidebar">
        <h3>📊 管理后台</h3>
        <button className={tab === 'stats' ? 'active' : ''} onClick={() => setTab('stats')}>仪表盘</button>
        <button className={tab === 'users' ? 'active' : ''} onClick={() => setTab('users')}>用户管理</button>
        <button className={tab === 'export' ? 'active' : ''} onClick={() => setTab('export')}>数据导出</button>
      </aside>
      <main className="admin-content">
        {tab === 'stats' && <AdminDashboard />}
        {tab === 'users' && <UserManagement />}
        {tab === 'export' && <DataExport />}
      </main>
    </div>
  );
}

function AdminDashboard() {
  const [stats, setStats] = useState<any>(null);
  const [apiStats, setApiStats] = useState<any>(null);
  const [logs, setLogs] = useState<any>(null);

  useEffect(() => {
    fetch('/api/admin/stats', {
      headers: { Authorization: `Bearer ${localStorage.getItem('token')}` }
    }).then(r => r.json()).then(r => { if (r.code === 200) setStats(r.data); });

    fetch('/api/admin/api-stats', {
      headers: { Authorization: `Bearer ${localStorage.getItem('token')}` }
    }).then(r => r.json()).then(r => { if (r.code === 200) setApiStats(r.data); });

    fetch('/api/admin/logs?lines=50', {
      headers: { Authorization: `Bearer ${localStorage.getItem('token')}` }
    }).then(r => r.json()).then(r => { if (r.code === 200 && r.data) setLogs(r.data); });
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

      {/* API 请求统计 */}
      {apiStats && (
        <div className="api-stats-section" style={{ marginTop: 24 }}>
          <h3>🔌 API 请求统计</h3>
          <div className="stats-cards" style={{ gap: 12, marginBottom: 12 }}>
            <div className="stat-card" style={{ padding: '8px 16px' }}>
              <div className="stat-value" style={{ fontSize: 20 }}>{apiStats.totalRequests}</div>
              <div className="stat-label" style={{ fontSize: 12 }}>总请求数</div>
            </div>
          </div>
          <div className="api-stats-table-wrapper" style={{ maxHeight: 300, overflowY: 'auto' }}>
            <table className="admin-table" style={{ fontSize: 12 }}>
              <thead>
                <tr>
                  <th>#</th>
                  <th>请求路径</th>
                  <th>调用次数</th>
                  <th>最后调用</th>
                </tr>
              </thead>
              <tbody>
                {Object.entries(apiStats.pathCounts || {}).slice(0, 20).map(([path, count], i) => {
                  const lastCalled = (apiStats.lastCalledAt || {})[path];
                  const lastTime = lastCalled ? new Date(lastCalled).toLocaleString('zh-CN') : '-';
                  return (
                    <tr key={path}>
                      <td>{i + 1}</td>
                      <td style={{ maxWidth: 300, overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>{path}</td>
                      <td>{count as number}</td>
                      <td style={{ fontSize: 11 }}>{lastTime}</td>
                    </tr>
                  );
                })}
                {(!apiStats.pathCounts || Object.keys(apiStats.pathCounts).length === 0) && (
                  <tr><td colSpan={4} style={{ textAlign: 'center', color: '#999' }}>暂无统计数据</td></tr>
                )}
              </tbody>
            </table>
          </div>
        </div>
      )}

      {/* 日志查看 */}
      {logs && logs.lines && logs.lines.length > 0 && (
        <div className="logs-section" style={{ marginTop: 24 }}>
          <h3>📋 最近日志</h3>
          <div style={{ fontSize: 11, color: '#999', marginBottom: 8 }}>
            来源: {logs.file || '未知'} | 共 {logs.totalLines} 行
          </div>
          <div className="log-viewer" style={{
            background: '#1e1e1e',
            color: '#d4d4d4',
            fontFamily: 'monospace',
            fontSize: 11,
            padding: 12,
            borderRadius: 6,
            maxHeight: 400,
            overflowY: 'auto',
            lineHeight: 1.6
          }}>
            {logs.lines.map((line: string, i: number) => (
              <div key={i} style={{ whiteSpace: 'pre-wrap', wordBreak: 'break-all' }}>
                {line || '\u00A0'}
              </div>
            ))}
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

function DataExport() {
  const token = localStorage.getItem('token');
  const today = new Date().toISOString().slice(0, 10);

  return (
    <div className="data-export" style={{ padding: '0 4px' }}>
      <h2>📥 数据导出</h2>
      <p style={{ color: '#666', marginBottom: 24 }}>导出数据为 CSV 格式，可用 Excel / WPS 打开</p>

      <div className="export-cards" style={{ display: 'flex', flexDirection: 'column', gap: 16 }}>
        {/* 练习记录导出 */}
        <div className="export-card" style={{
          border: '1px solid #e8e8e8',
          borderRadius: 8,
          padding: 20,
          background: '#fafafa'
        }}>
          <h3 style={{ margin: '0 0 8px 0' }}>📝 我的练习记录</h3>
          <p style={{ fontSize: 13, color: '#888', margin: '0 0 16px 0' }}>
            导出当前用户的练习记录，包含日期、句子 ID、句子文本、正确率、练习模式
          </p>
          <a
            href={`/api/practice/export?t=${Date.now()}`}
            onClick={e => {
              e.preventDefault();
              const link = document.createElement('a');
              link.href = `/api/practice/export?t=${Date.now()}`;
              link.setAttribute('download', `pengyouquan-practice-${today}.csv`);
              // 直接打开，让浏览器处理下载
              fetch(`/api/practice/export`, {
                headers: { Authorization: `Bearer ${token}` }
              })
                .then(r => {
                  if (!r.ok) throw new Error('导出失败');
                  return r.blob();
                })
                .then(blob => {
                  const url = URL.createObjectURL(blob);
                  const a = document.createElement('a');
                  a.href = url;
                  a.download = `pengyouquan-practice-${today}.csv`;
                  a.click();
                  URL.revokeObjectURL(url);
                })
                .catch(err => alert('导出失败: ' + err.message));
            }}
            style={{
              display: 'inline-block',
              padding: '8px 20px',
              background: '#1677ff',
              color: '#fff',
              borderRadius: 4,
              textDecoration: 'none',
              fontSize: 14,
              cursor: 'pointer'
            }}
          >
            ⬇️ 导出练习记录
          </a>
        </div>

        {/* 用户数据导出（仅管理员） */}
        <div className="export-card" style={{
          border: '1px solid #e8e8e8',
          borderRadius: 8,
          padding: 20,
          background: '#fafafa'
        }}>
          <h3 style={{ margin: '0 0 8px 0' }}>👥 全部用户数据（管理员）</h3>
          <p style={{ fontSize: 13, color: '#888', margin: '0 0 16px 0' }}>
            导出所有用户信息，包含 ID、邮箱、昵称、角色、启用状态、注册时间
          </p>
          <button
            onClick={() => {
              fetch(`/api/admin/export/users`, {
                headers: { Authorization: `Bearer ${token}` }
              })
                .then(r => {
                  if (!r.ok) throw new Error('导出失败');
                  return r.blob();
                })
                .then(blob => {
                  const url = URL.createObjectURL(blob);
                  const a = document.createElement('a');
                  a.href = url;
                  a.download = `pengyouquan-users-${today}.csv`;
                  a.click();
                  URL.revokeObjectURL(url);
                })
                .catch(err => alert('导出失败: ' + err.message));
            }}
            style={{
              display: 'inline-block',
              padding: '8px 20px',
              background: '#722ed1',
              color: '#fff',
              borderRadius: 4,
              border: 'none',
              fontSize: 14,
              cursor: 'pointer'
            }}
          >
            ⬇️ 导出用户数据
          </button>
        </div>
      </div>
    </div>
  );
}
