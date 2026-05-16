import React from 'react';

interface SidebarProps {
  page: string;
  onNavigate: (page: string) => void;
  user: any;
  onlineCount: number | null;
  onLogout: () => void;
}

const NAV_ITEMS: { key: string; icon: string; label: string; requiresLogin?: boolean }[] = [
  { key: 'practice', icon: '🎯', label: '练习' },
  { key: 'wrong', icon: '❌', label: '错题本', requiresLogin: true },
  { key: 'search', icon: '🔍', label: '搜索' },
  { key: 'browse', icon: '📖', label: '浏览' },
];

export default function Sidebar({ page, onNavigate, user, onlineCount, onLogout }: SidebarProps) {
  return (
    <aside className="sidebar">
      {/* Logo */}
      <div className="sidebar-logo" onClick={() => onNavigate('practice')}>
        <span className="sidebar-logo-dot" />
        <span className="sidebar-logo-text">英语剧场</span>
      </div>

      {/* Navigation */}
      <nav className="sidebar-nav">
        {NAV_ITEMS.map(item => {
          const disabled = item.requiresLogin && !user;
          return (
            <button
              key={item.key}
              className={`sidebar-nav-item ${page === item.key ? 'active' : ''}`}
              onClick={() => {
                if (disabled) return;
                onNavigate(item.key);
              }}
              disabled={disabled}
              title={disabled ? '请先登录' : item.label}
            >
              <span className="sidebar-nav-icon">{item.icon}</span>
              <span className="sidebar-nav-label">{item.label}</span>
            </button>
          );
        })}
      </nav>

      {/* Admin link */}
      {user?.role === 'admin' && (
        <div className="sidebar-admin-row">
          <button
            className={`sidebar-nav-item sidebar-nav-admin ${page === 'admin' ? 'active' : ''}`}
            onClick={() => onNavigate('admin')}
          >
            <span className="sidebar-nav-icon">⚙️</span>
            <span className="sidebar-nav-label">管理后台</span>
          </button>
        </div>
      )}

      {/* Spacer to push user to bottom */}
      <div className="sidebar-spacer" />

      {/* Divider */}
      <div className="sidebar-divider" />

      {/* User section */}
      {user ? (
        <div className="sidebar-user-section">
          <div className="sidebar-user">
            <div className="sidebar-avatar">
              {user.nickname?.charAt(0)?.toUpperCase() || '?'}
            </div>
            <div className="sidebar-user-info">
              <span className="sidebar-username">{user.nickname}</span>
              {user?.role === 'admin' && onlineCount !== null && (
                <span className="sidebar-online">
                  <span className={`sidebar-online-dot ${onlineCount > 0 ? 'active' : ''}`} />
                  {onlineCount}
                </span>
              )}
            </div>
          </div>
          <button className="sidebar-logout-btn" onClick={onLogout}>
            退出
          </button>
        </div>
      ) : (
        <div className="sidebar-user-section">
          <button className="sidebar-login-btn" onClick={() => onNavigate('login')}>
            登录
          </button>
        </div>
      )}
    </aside>
  );
}
