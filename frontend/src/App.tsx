import React, { useState, useEffect, useRef } from 'react';
import { api, setToken, getToken, clearToken } from './api/client';
import PracticePage from './PracticePage';
import LoginPage from './LoginPage';
import WrongPage from './WrongPage';
import SearchPage from './SearchPage';
import BrowsePage from './BrowsePage';
import AdminPage from './AdminPage';
import SubtitlePage from './SubtitlePage';
import DemoPage from './DemoPage';
import Sidebar from './Sidebar';
import { initAudioBase } from './audioBase';
import { useTheme } from './useTheme';
import './index.css';

// 应用启动时检测 IPv6 连通性
initAudioBase();

export default function App() {
  const [page, setPage] = useState<'practice' | 'login' | 'wrong' | 'search' | 'browse' | 'admin' | 'subtitle' | 'demo'>('practice');
  const [user, setUser] = useState<any>(null);
  const [jumpId, setJumpId] = useState<number | null>(null);
  const [announcement, setAnnouncement] = useState('');
  const [notifications, setNotifications] = useState<any[]>([]);
  const [onlineCount, setOnlineCount] = useState<number | null>(null);
  const [wrongCount, setWrongCount] = useState(0);
  const { theme, toggleTheme, isDark } = useTheme();
  const mainRef = useRef<HTMLElement>(null);

  // Focus management: move focus to main on page change
  useEffect(() => {
    mainRef.current?.focus();
  }, [page]);

  // Sync theme-color meta tag
  useEffect(() => {
    const meta = document.querySelector('meta[name="theme-color"]');
    if (meta) meta.setAttribute('content', isDark ? '#0f172a' : '#f8fafc');
  }, [isDark]);

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

  // 管理员实时在线人数 SSE
  useEffect(() => {
    if (user?.role !== 'admin') { setOnlineCount(null); return; }
    const token = localStorage.getItem('token');
    if (!token) return;
    const es = new EventSource(`/api/admin/online/subscribe?token=${encodeURIComponent(token)}`);
    es.addEventListener('online', (e) => {
      try { setOnlineCount(JSON.parse(e.data).onlineCount); } catch {}
    });
    es.addEventListener('init', (e) => {
      try { setOnlineCount(JSON.parse(e.data).onlineCount); } catch {}
    });
    es.onerror = () => {};
    return () => es.close();
  }, [user?.role]);

  const handleLogin = (token: string) => {
    setToken(token);
    api.me().then(r => { if (r.code === 200) setUser(r.data); setPage('practice'); });
  };

  const handleLogout = () => { clearToken(); setUser(null); setOnlineCount(null); setPage('login'); };

  // 处理从实践页底部导航来的跳转
  const handleNavigate = (target: string, data?: any) => {
    setPage(target as any);
    if (data?.jumpId) setJumpId(data.jumpId);
  };

  if (page === 'login' || (!getToken() && page !== 'practice' && page !== 'demo')) {
    return <LoginPage onLogin={handleLogin} onHome={() => setPage('practice')} />;
  }

  return (
    <div className="app">
      <a href="#main-content" className="skip-to-content">跳转到主要内容</a>
      {/* 桌面端侧边栏 */}
      <Sidebar
        page={page}
        onNavigate={handleNavigate}
        user={user}
        onlineCount={onlineCount}
        onLogout={handleLogout}
        isDark={isDark}
        onToggleTheme={toggleTheme}
        wrongCount={wrongCount}
      />

      <div className="app-content app-content-new">
        {/* 手机端精简顶部导航 */}
        <nav className="topnav-mobile">
          <span className="topnav-mobile-logo" onClick={() => setPage('practice')}>
            <span className="topnav-mobile-dot" />
            英语剧场
          </span>
          <div className="topnav-mobile-right">
            {user?.role === 'admin' && (
              <span className="topnav-mobile-online">
                <span className={`online-dot ${onlineCount !== null && onlineCount > 0 ? 'online-dot-active' : ''}`} />
                {onlineCount !== null ? onlineCount : '...'}
              </span>
            )}
            {user ? (
              <button onClick={handleLogout} className="topnav-mobile-logout btn-outline">退出</button>
            ) : (
              <button onClick={() => setPage('login')} className="topnav-mobile-login btn-primary">登录</button>
            )}
          </div>
        </nav>

        {/* 桌面端 Topbar */}
        <header className="topbar">
          <div className="topbar-left">
            <div className="topbar-title">
              {page === 'practice' && '练习'}
              {page === 'wrong' && '错题本'}
              {page === 'search' && '搜索'}
              {page === 'browse' && '浏览'}
              {page === 'admin' && '管理后台'}
              {page === 'subtitle' && '字幕导入'}
              {page === 'demo' && 'Demo'}
            </div>
            <div className="topbar-subtitle">跟读经典美剧台词，逐词精听练习</div>
          </div>
          <div className="topbar-right topbar-actions">
            <button className="topbar-theme-btn" onClick={toggleTheme} title={isDark ? '切换到浅色模式' : '切换到深色模式'}>
              {isDark ? '☀️' : '🌙'}
            </button>
            {user ? (
              <button className="topbar-login-btn btn-outline-style" onClick={handleLogout}>退出</button>
            ) : (
              <button className="topbar-login-btn" onClick={() => setPage('login')}>登录</button>
            )}
          </div>
        </header>

        {/* 公告 & 通知区域 */}
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

        <main className="page-enter main-content-new" key={page} id="main-content" ref={mainRef} tabIndex={-1}>
          {page === 'practice' && <PracticePage user={user} jumpId={jumpId} onNavigate={handleNavigate} onWrongCountChange={setWrongCount} />}
          {page === 'wrong' && <WrongPage onJump={(id) => { setJumpId(id); setPage('practice'); }} onBack={() => setPage('practice')} />}
          {page === 'search' && <SearchPage onJump={(id) => { setJumpId(id); setPage('practice'); }} onBack={() => setPage('practice')} />}
          {page === 'browse' && <BrowsePage onJump={(id) => { setJumpId(id); setPage('practice'); }} onBack={() => setPage('practice')} />}
          {page === 'admin' && <AdminPage onlineCount={onlineCount} />}
          {page === 'subtitle' && <SubtitlePage />}
          {page === 'demo' && <DemoPage onBack={() => setPage('practice')} />}
        </main>
      </div>
    </div>
  );
}
