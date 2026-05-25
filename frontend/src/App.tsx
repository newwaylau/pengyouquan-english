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
import { IconTarget, IconClose, IconSearch, IconBook, IconSettings } from './Icons';
import './index.css';

// 应用启动时检测 IPv6 连通性
initAudioBase();

const PAGE_LABELS: Record<string, string> = {
  practice: '练习',
  wrong: '错题本',
  search: '搜索',
  browse: '剧库',
  admin: '管理后台',
  subtitle: '字幕导入',
  demo: 'Demo',
};

export default function App() {
  const parseHash = (): 'practice' | 'login' | 'wrong' | 'search' | 'browse' | 'admin' | 'subtitle' | 'demo' => {
    const hash = window.location.hash.replace(/^#\/?/, '');
    const valid: Record<string, any> = { login: 'login', wrong: 'wrong', search: 'search', browse: 'browse', admin: 'admin', subtitle: 'subtitle', demo: 'demo' };
    return valid[hash] || 'practice';
  };
  const [page, setPage] = useState<'practice' | 'login' | 'wrong' | 'search' | 'browse' | 'admin' | 'subtitle' | 'demo'>(parseHash);
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

  // 同步 page 到 URL hash
  useEffect(() => {
    const target = page === 'practice' ? '' : page;
    const current = window.location.hash.replace(/^#\/?/, '');
    if (current !== target) {
      window.location.hash = target ? `#/${target}` : '';
    }
  }, [page]);

  // 监听 hash 变化（浏览器前进/后退/直接输入）
  useEffect(() => {
    const onHashChange = () => setPage(parseHash());
    window.addEventListener('hashchange', onHashChange);
    return () => window.removeEventListener('hashchange', onHashChange);
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

      <div className="app-main-v2">
        <header className="app-header-v2">
          <div className="crumbs-v2">
            <span>主页</span>
            <span className="sep">/</span>
            <strong>{PAGE_LABELS[page]}</strong>
            {page === 'practice' && <><span className="sep">/</span><span>Game of Thrones</span></>}
          </div>
          <div className="head-spacer" />
          <div className="head-actions-v2">
            <button className="btn btn-icon" onClick={toggleTheme} title={isDark ? '切换到浅色模式' : '切换到深色模式'}>
              {isDark ? '☀' : '☾'}
            </button>
            {user ? (
              <button className="btn btn-secondary btn-sm" onClick={handleLogout}>退出</button>
            ) : (
              <button className="btn btn-primary btn-sm" onClick={() => setPage('login')}>登录</button>
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

        <main className="page-enter app-content-v2" key={page} id="main-content" ref={mainRef} tabIndex={-1}>
          {page === 'practice' && <PracticePage user={user} jumpId={jumpId} onNavigate={handleNavigate} onWrongCountChange={setWrongCount} />}
          {page === 'wrong' && <WrongPage onJump={(id) => { setJumpId(id); setPage('practice'); }} onBack={() => setPage('practice')} />}
          {page === 'search' && <SearchPage onJump={(id) => { setJumpId(id); setPage('practice'); }} onBack={() => setPage('practice')} />}
          {page === 'browse' && <BrowsePage onJump={(id) => { setJumpId(id); setPage('practice'); }} onBack={() => setPage('practice')} />}
          {page === 'admin' && <AdminPage onlineCount={onlineCount} />}
          {page === 'subtitle' && <SubtitlePage />}
          {page === 'demo' && <DemoPage onBack={() => setPage('practice')} />}
        </main>
        <nav className="mobile-tabbar" aria-label="底部导航">
          <button className={page === 'practice' ? 'active' : ''} onClick={() => setPage('practice')}><IconTarget />练习</button>
          <button className={page === 'wrong' ? 'active' : ''} onClick={() => setPage('wrong')}><IconClose />错题</button>
          <button className={page === 'search' ? 'active' : ''} onClick={() => setPage('search')}><IconSearch />搜索</button>
          <button className={page === 'browse' ? 'active' : ''} onClick={() => setPage('browse')}><IconBook />剧库</button>
          <button className={page === 'login' ? 'active' : ''} onClick={() => setPage(user ? 'practice' : 'login')}><IconSettings />我的</button>
        </nav>
      </div>
    </div>
  );
}
