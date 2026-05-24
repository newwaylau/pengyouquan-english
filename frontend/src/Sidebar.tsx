import React from 'react';
import { IconTarget, IconClose, IconSearch, IconBook, IconSettings, IconSun, IconMoon } from './Icons';

interface SidebarProps {
  page: string;
  onNavigate: (page: string) => void;
  user: any;
  onlineCount: number | null;
  onLogout: () => void;
  isDark: boolean;
  onToggleTheme: () => void;
  wrongCount?: number;
}

const NAV_ITEMS: { key: string; icon: React.ReactNode; label: string; requiresLogin?: boolean; badge?: 'wrongCount' }[] = [
  { key: 'practice', icon: <IconTarget />, label: '练习' },
  { key: 'wrong', icon: <IconClose />, label: '错题本', requiresLogin: true, badge: 'wrongCount' },
  { key: 'search', icon: <IconSearch />, label: '搜索' },
  { key: 'browse', icon: <IconBook />, label: '浏览' },
];

const OTHER_ITEMS: { key: string; icon: React.ReactNode; label: string; requiresLogin?: boolean }[] = [];

export default function Sidebar({ page, onNavigate, user, onlineCount, onLogout, isDark, onToggleTheme, wrongCount = 0 }: SidebarProps) {
  return (
    <aside className="sidebar-new">
      {/* Logo */}
      <div className="sidebar-logo-new" onClick={() => onNavigate('practice')}>
        <div className="sidebar-logo-circle">🎬</div>
        <div className="sidebar-logo-text-group">
          <span className="sidebar-logo-title">英语剧场</span>
          <span className="sidebar-logo-subtitle">跟读经典美剧台词</span>
        </div>
      </div>

      {/* Section: 主菜单 */}
      <div className="sidebar-section-header">主菜单</div>
      <nav className="sidebar-nav-new">
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
              <span className="nav-icon">{item.icon}</span>
              <span className="nav-label">{item.label}</span>
              {item.badge === 'wrongCount' && wrongCount > 0 && (
                <span className="sidebar-nav-badge">{wrongCount}</span>
              )}
            </button>
          );
        })}

        {/* Admin link */}
        {user?.role === 'admin' && (
          <button
            className={`sidebar-nav-item ${page === 'admin' ? 'active' : ''}`}
            onClick={() => onNavigate('admin')}
          >
            <span className="nav-icon"><IconSettings /></span>
            <span className="nav-label">管理后台</span>
          </button>
        )}
      </nav>

      {/* Section: 其他 */}
      {OTHER_ITEMS.length > 0 && (
        <>
          <div className="sidebar-section-header">其他</div>
          <nav className="sidebar-nav-new">
            {OTHER_ITEMS.map(item => {
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
                >
                  <span className="nav-icon">{item.icon}</span>
                  <span className="nav-label">{item.label}</span>
                </button>
              );
            })}
          </nav>
        </>
      )}

      {/* Spacer */}
      <div className="sidebar-spacer" />

      {/* Theme toggle */}
      <div className="sidebar-theme-row">
        <button className="sidebar-nav-item" onClick={onToggleTheme} title={isDark ? '切换到浅色模式' : '切换到深色模式'}>
          <span className="nav-icon">{isDark ? <IconSun /> : <IconMoon />}</span>
          <span className="nav-label">{isDark ? '浅色模式' : '深色模式'}</span>
        </button>
      </div>

      {/* Divider */}
      <div className="sidebar-footer-divider" />

      {/* User section */}
      {user ? (
        <div className="sidebar-user-new">
          <div className="sidebar-avatar-new">
            {user.nickname?.charAt(0)?.toUpperCase() || '?'}
          </div>
          <div className="sidebar-user-info-new">
            <span className="sidebar-username-new">{user.nickname}</span>
            {user?.role === 'admin' && onlineCount !== null && (
              <span className="sidebar-email-new">在线 {onlineCount}</span>
            )}
          </div>
          <button className="sidebar-nav-item" onClick={onLogout} style={{ padding: '6px 10px', fontSize: 12, marginLeft: 'auto' }}>
            退出
          </button>
        </div>
      ) : (
        <div className="sidebar-user-new">
          <button className="sidebar-nav-item" onClick={() => onNavigate('login')} style={{ width: '100%', justifyContent: 'center', background: '#14b8a6', color: '#fff', fontWeight: 600 }}>
            登录
          </button>
        </div>
      )}
    </aside>
  );
}
