import React, { useState, useEffect } from 'react';
import { api, setToken, getToken, clearToken } from './api/client';
import PracticePage from './PracticePage';
import LoginPage from './LoginPage';
import WrongPage from './WrongPage';
import SearchPage from './SearchPage';
import BrowsePage from './BrowsePage';
import AdminPage from './AdminPage';
import SubtitlePage from './SubtitlePage';
import './index.css';

export default function App() {
  const [page, setPage] = useState<'practice' | 'login' | 'wrong' | 'search' | 'browse' | 'admin' | 'subtitle'>('practice');
  const [user, setUser] = useState<any>(null);
  const [jumpId, setJumpId] = useState<number | null>(null);
  const [announcement, setAnnouncement] = useState('');

  useEffect(() => {
    if (getToken()) {
      api.me().then(r => { if (r.code === 200) setUser(r.data); });
    }
    // 加载公告（无需登录）
    fetch('/api/admin/public/announcement')
      .then(r => r.json())
      .then(r => { if (r.code === 200) setAnnouncement(r.data?.announcement || ''); });
  }, []);

  const handleLogin = (token: string) => {
    setToken(token);
    api.me().then(r => { if (r.code === 200) setUser(r.data); setPage('practice'); });
  };

  const handleLogout = () => { clearToken(); setUser(null); setPage('login'); };

  if (page === 'login' || (!getToken() && page !== 'practice')) {
    return <LoginPage onLogin={handleLogin} />;
  }

  return (
    <div className="app">
      <nav className="topnav">
        <span className="logo">朋友圈英语</span>
        <div className="nav-links">
          <button onClick={() => setPage('practice')} className={page === 'practice' ? 'active' : ''}>练习</button>
          <button onClick={() => setPage('browse')} className={page === 'browse' ? 'active' : ''}>浏览</button>
          <button onClick={() => setPage('wrong')} className={page === 'wrong' ? 'active' : ''}>错题本</button>
          <button onClick={() => setPage('search')} className={page === 'search' ? 'active' : ''}>搜索</button>
          <button onClick={() => setPage('subtitle')} className={page === 'subtitle' ? 'active' : ''}>导入</button>
          {user?.role === 'admin' && (
            <button onClick={() => setPage('admin')} className={page === 'admin' ? 'active' : ''}>管理</button>
          )}
          {user ? (
            <>
              <span className="user-badge">{user.nickname}</span>
              <button onClick={handleLogout} className="logout-btn">退出</button>
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
      <main>
        {page === 'practice' && <PracticePage user={user} jumpId={jumpId} />}
        {page === 'wrong' && <WrongPage onJump={(id) => { setJumpId(id); setPage('practice'); }} />}
        {page === 'search' && <SearchPage onJump={(id) => { setJumpId(id); setPage('practice'); }} />}
        {page === 'browse' && <BrowsePage onJump={(id) => { setJumpId(id); setPage('practice'); }} />}
        {page === 'admin' && <AdminPage />}
        {page === 'subtitle' && <SubtitlePage />}
      </main>
    </div>
  );
}
