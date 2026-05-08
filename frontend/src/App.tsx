import React, { useState, useEffect } from 'react';
import { api, setToken, getToken, clearToken } from './api/client';
import PracticePage from './PracticePage';
import LoginPage from './LoginPage';
import WrongPage from './WrongPage';
import SearchPage from './SearchPage';
import BrowsePage from './BrowsePage';
import AdminPage from './AdminPage';
import SubtitlePage from './SubtitlePage';
import { initAudioBase } from './audioBase';
import './index.css';

// 应用启动时检测 IPv6 连通性
initAudioBase();

export default function App() {
  const [page, setPage] = useState<'practice' | 'login' | 'wrong' | 'search' | 'browse' | 'admin' | 'subtitle'>('practice');
  const [user, setUser] = useState<any>(null);
  const [jumpId, setJumpId] = useState<number | null>(null);
  const [announcement, setAnnouncement] = useState('');
  const [notifications, setNotifications] = useState<any[]>([]);

  useEffect(() => {
    if (getToken()) {
      api.me().then(r => { if (r.code === 200) setUser(r.data); });
    }
    // 加载公告（无需登录）
    fetch('/api/admin/public/announcement')
      .then(r => r.json())
      .then(r => { if (r.code === 200) setAnnouncement(r.data?.announcement || ''); });
    // 加载通知列表（公开）
    fetch('/api/notifications')
      .then(r => r.json())
      .then(r => { if (r.code === 200) setNotifications(r.data || []); });
  }, []);

  const handleLogin = (token: string) => {
    setToken(token);
    api.me().then(r => { if (r.code === 200) setUser(r.data); setPage('practice'); });
  };

  const handleLogout = () => { clearToken(); setUser(null); setPage('login'); };

  // 处理从实践页底部导航来的跳转
  const handleNavigate = (target: string, data?: any) => {
    setPage(target as any);
    if (data?.jumpId) setJumpId(data.jumpId);
  };

  if (page === 'login' || (!getToken() && page !== 'practice')) {
    return <LoginPage onLogin={handleLogin} />;
  }

  return (
    <div className="app">
      <nav className="topnav">
        <span className="logo">朋友圈英语</span>
        <div className="nav-links">
          {user ? (
            <>
              <span className="user-badge">{user.nickname}</span>
              <button onClick={handleLogout} className="logout-btn">退出</button>
              {user?.role === 'admin' && (
                <button onClick={() => setPage('admin')} className={page === 'admin' ? 'active' : ''}>管理</button>
              )}
            </>
          ) : (
            <button onClick={() => setPage('login')}>登录</button>
          )}
        </div>
      </nav>
      {announcement && (
        <div className="announcement-bar">
          {announcement}
        </div>
      )}
      {notifications.length > 0 && (
        <div className="notification-list">
          {notifications.map((n: any) => (
            <div key={n.id} className="notification-item">
              <div className="notification-title">{n.title}</div>
              {n.content && <div className="notification-content">{n.content}</div>}
              <div className="notification-time">
                {n.createdAt ? new Date(n.createdAt).toLocaleDateString() : ''}
              </div>
            </div>
          ))}
        </div>
      )}
      <main>
        {page === 'practice' && <PracticePage user={user} jumpId={jumpId} onNavigate={handleNavigate} />}
        {page === 'wrong' && <WrongPage onJump={(id) => { setJumpId(id); setPage('practice'); }} onBack={() => setPage('practice')} />}
        {page === 'search' && <SearchPage onJump={(id) => { setJumpId(id); setPage('practice'); }} onBack={() => setPage('practice')} />}
        {page === 'browse' && <BrowsePage onJump={(id) => { setJumpId(id); setPage('practice'); }} onBack={() => setPage('practice')} />}
        {page === 'admin' && <AdminPage />}
        {page === 'subtitle' && <SubtitlePage />}
      </main>
    </div>
  );
}
