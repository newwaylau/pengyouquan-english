import React, { useState, useEffect, useRef } from 'react';
import { api, setToken, getToken, clearToken } from './api/client';
import PracticePage from './PracticePage';
import LoginPage from './LoginPage';
import WrongPage from './WrongPage';
import SearchPage from './SearchPage';
import BrowsePage from './BrowsePage';
import AdminPage from './AdminPage';
import DemoPage from './DemoPage';
import ArenaPage from './ArenaPage';
import LeaderboardPage from './LeaderboardPage';
import ChangePasswordModal from './ChangePasswordModal';
import { initAudioBase } from './audioBase';
import { useTheme } from './useTheme';
import { IconTarget, IconClose, IconSettings } from './Icons';
import './index.css';
import './v2-missing.css';

// 应用启动时检测 IPv6 连通性
initAudioBase();

export default function App() {
  const [page, setPage] = useState<'practice' | 'login' | 'wrong' | 'search' | 'browse' | 'admin' | 'demo' | 'arena' | 'clan'>('practice');
  const [user, setUser] = useState<any>(null);
  const [jumpId, setJumpId] = useState<number | null>(null);
  const [announcement, setAnnouncement] = useState('');
  const [notifications, setNotifications] = useState<any[]>([]);
  const [onlineCount, setOnlineCount] = useState<number | null>(null);
  const [wrongCount, setWrongCount] = useState(0);
  const [showUserMenu, setShowUserMenu] = useState(false);
  const [showLogoutConfirm, setShowLogoutConfirm] = useState(false);
  const [showChangePassword, setShowChangePassword] = useState(false);
  const [loginMode, setLoginMode] = useState<'login' | 'register' | 'forgot'>('login');
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
    return <LoginPage onLogin={(token) => { setLoginMode('login'); handleLogin(token); }} onHome={() => setPage('practice')} initialMode={loginMode} />;
  }

  return (
    <div className="app">
      <a href="#main-content" className="skip-to-content">跳转到主要内容</a>

      <div className="app-content app-content-new">
        {/* 手机端精简顶部导航 */}
        <nav className="topnav-mobile">
          <span className="topnav-mobile-logo" onClick={() => setPage('practice')}>
            <span className="topnav-mobile-dot" />
            剧场
          </span>
          <div className="topnav-mobile-right">
            {user?.role === 'admin' && (
              <button className={`topnav-mobile-admin-btn ${page === 'admin' ? 'active' : ''}`} onClick={() => setPage('admin')} title="管理后台">
                <IconSettings size={14} />
                {onlineCount !== null && <span className="topbar-badge">{onlineCount}</span>}
              </button>
            )}
            {user ? (
              <div className="topnav-mobile-avatar" onClick={() => setShowUserMenu(true)} title={user.nickname}>
                {user.nickname?.charAt(0)?.toUpperCase() || '?'}
              </div>
            ) : (
              <button onClick={() => setPage('login')} className="topnav-mobile-login-btn">登录</button>
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
              {page === 'demo' && 'Demo'}
              {page === 'arena' && '演武场'}
              {page === 'clan' && '七国铁王座'}
            </div>
            <div className="topbar-subtitle">跟读经典美剧台词，逐词精听练习</div>
          </div>
          <div className="topbar-right hide-on-desktop">
            <button className="topbar-theme-btn" onClick={toggleTheme} title={isDark ? '切换到浅色模式' : '切换到深色模式'}>
              {isDark ? '☀️' : '🌙'}
            </button>
            {user ? (
              <button className="topbar-login-btn btn-outline-style" onClick={handleLogout}>退出</button>
            ) : (
              <button className="topbar-login-btn" onClick={() => setPage('login')}>登录</button>
            )}
          </div>
          <div className="topbar-right show-on-desktop">
            {user?.role === 'admin' && (
              <button className={`topbar-admin-btn ${page === 'admin' ? 'active' : ''}`} onClick={() => setPage('admin')}>
                <IconSettings size={16} /> Admin
                {onlineCount !== null && <span className="topbar-badge">{onlineCount}</span>}
              </button>
            )}
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
          {page === 'demo' && <DemoPage onBack={() => setPage('practice')} />}
          {page === 'arena' && <ArenaPage user={user} onNavigate={handleNavigate} />}
          {page === 'clan' && <LeaderboardPage user={user} onNavigate={handleNavigate} />}
        </main>

        {/* 手机端底部导航 */}
        <nav className="mobile-bottom-nav">
          {[
            { key: 'practice', icon: <IconTarget size={20} />, label: 'Practice' },
            { key: 'wrong', icon: <IconClose size={20} />, label: 'Review', requiresLogin: true, badge: true },
            { key: 'arena', icon: <span>⚔️</span>, label: '演武', requiresLogin: true },
            { key: 'clan', icon: <span>👑</span>, label: '封臣', requiresLogin: true },
          ].map(item => {
            const disabled = item.requiresLogin && !user;
            return (
              <button
                key={item.key}
                className={`mb-nav-btn ${page === item.key ? 'active' : ''}`}
                onClick={() => { if (disabled) return; setPage(item.key as any); }}
                disabled={disabled}
              >
                <span className="mb-icon">{item.icon}</span>
                <span className="mb-label">{item.label}</span>
                {item.badge && wrongCount > 0 && <span className="mb-badge">{wrongCount}</span>}
              </button>
            );
          })}
        </nav>

        {/* 底部用户菜单面板 */}
        {showUserMenu && (
          <div className="user-menu-overlay" onClick={() => setShowUserMenu(false)} />
        )}
        {user && (
        <div className={`user-menu-panel ${showUserMenu ? 'open' : ''}`}>
          <div className="user-menu-drag" />
          <div className="user-menu-header">
            <div className="user-menu-avatar">{user?.nickname?.charAt(0)?.toUpperCase() || '?'}</div>
            <div className="user-menu-info">
              <div className="user-menu-name">{user?.nickname || '用户'}</div>
              <div className="user-menu-email">{user?.email || ''}</div>
            </div>
          </div>
          <div className="user-menu-items">
            <div className="user-menu-item" onClick={() => { setShowUserMenu(false); setShowChangePassword(true); }}>
              <span className="user-menu-item-icon key">🔑</span>
              <span className="user-menu-item-text">修改密码</span>
              <span className="user-menu-arrow">›</span>
            </div>
            <div className="user-menu-item" onClick={toggleTheme}>
              <span className="user-menu-item-icon theme">🌙</span>
              <span className="user-menu-item-text">{isDark ? '夜间模式' : '白天模式'}</span>
              <span className="user-menu-toggle-label">{isDark ? '已开启' : '已关闭'}</span>
              <button className={`user-menu-toggle ${isDark ? 'on' : ''}`} onClick={(e) => { e.stopPropagation(); toggleTheme(); }} />
            </div>
            <div className="user-menu-item" onClick={() => { setShowUserMenu(false); setShowLogoutConfirm(true); }}>
              <span className="user-menu-item-icon logout">🚪</span>
              <span className="user-menu-item-text">退出登录</span>
              <span className="user-menu-arrow">›</span>
            </div>
          </div>
        </div>
        )}
        {/* 修改密码弹窗 */}
        {showChangePassword && (
          <ChangePasswordModal onClose={() => setShowChangePassword(false)} />
        )}
        {/* 退出确认弹窗 */}
        {showLogoutConfirm && (
          <div className="logout-overlay" onClick={() => setShowLogoutConfirm(false)}>
            <div className="logout-modal" onClick={e => e.stopPropagation()}>
              <div className="logout-modal-icon">🚪</div>
              <div className="logout-modal-title">退出登录</div>
              <div className="logout-modal-desc">确定要退出当前账号吗？</div>
              <div className="logout-modal-actions">
                <button className="logout-modal-btn cancel" onClick={() => setShowLogoutConfirm(false)}>取消</button>
                <button className="logout-modal-btn confirm" onClick={() => { setShowLogoutConfirm(false); handleLogout(); }}>确定退出</button>
              </div>
            </div>
          </div>
        )}
      </div>
    </div>
  );
}
