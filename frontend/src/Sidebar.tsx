import React from 'react';
import { IconTarget, IconClose, IconSearch, IconBook, IconSettings, IconSun, IconMoon, IconTV } from './Icons';

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
  { key: 'browse', icon: <IconBook />, label: '剧库' },
];

export default function Sidebar({ page, onNavigate, user, onlineCount, onLogout, isDark, onToggleTheme, wrongCount = 0 }: SidebarProps) {
  return (
    <aside className="sidebar-v2">
      <button className="brand-v2" onClick={() => onNavigate('practice')}>
        <span className="brand-logo-v2">剧</span>
        <span className="brand-copy-v2">
          <span className="brand-name-v2">英语剧场</span>
          <span className="brand-tag-v2">DICTATION · S2</span>
        </span>
      </button>

      <div className="cap sidebar-cap">主菜单</div>
      <nav className="sidebar-nav-v2">
        {NAV_ITEMS.map(item => {
          const disabled = item.requiresLogin && !user;
          return (
            <button
              key={item.key}
              className={`nav-item-v2 ${page === item.key ? 'active' : ''}`}
              onClick={() => {
                if (disabled) return;
                onNavigate(item.key);
              }}
              disabled={disabled}
              title={disabled ? '请先登录' : item.label}
            >
              <span className="nav-icon-v2">{item.icon}</span>
              <span className="nav-label-v2">{item.label}</span>
              {item.badge === 'wrongCount' && wrongCount > 0 && <span className="nav-badge-v2">{wrongCount}</span>}
            </button>
          );
        })}
      </nav>

      {user?.role === 'admin' && (
        <>
          <div className="cap sidebar-cap">管理</div>
          <nav className="sidebar-nav-v2">
            <button className={`nav-item-v2 ${page === 'admin' ? 'active' : ''}`} onClick={() => onNavigate('admin')}>
              <span className="nav-icon-v2"><IconSettings /></span>
              <span className="nav-label-v2">管理后台</span>
              {onlineCount !== null && <span className="nav-badge-v2">{onlineCount}</span>}
            </button>
            <button className={`nav-item-v2 ${page === 'subtitle' ? 'active' : ''}`} onClick={() => onNavigate('subtitle')}>
              <span className="nav-icon-v2"><IconTV /></span>
              <span className="nav-label-v2">字幕导入</span>
            </button>
          </nav>
        </>
      )}

      <div className="sidebar-fill-v2" />

      <button className="nav-item-v2" onClick={onToggleTheme} title={isDark ? '切换到浅色模式' : '切换到深色模式'}>
        <span className="nav-icon-v2">{isDark ? <IconSun /> : <IconMoon />}</span>
        <span className="nav-label-v2">{isDark ? '浅色模式' : '深色模式'}</span>
      </button>

      {user ? (
        <div className="user-mini-v2">
          <button className="user-face-v2" onClick={() => onNavigate('practice')}>
            {user.nickname?.charAt(0)?.toUpperCase() || '?'}
          </button>
          <button className="user-copy-v2" onClick={() => onNavigate('practice')}>
            <span className="user-name-v2">{user.nickname}</span>
            <span className="user-meta-v2">连续天数 · {wrongCount} 错题</span>
          </button>
          <button className="btn btn-ghost btn-sm" onClick={onLogout}>退出</button>
        </div>
      ) : (
        <div className="user-mini-v2">
          <button className="user-face-v2" onClick={() => onNavigate('login')}>访</button>
          <button className="user-copy-v2" onClick={() => onNavigate('login')}>
            <span className="user-name-v2">未登录</span>
            <span className="user-meta-v2">登录同步进度</span>
          </button>
        </div>
      )}
    </aside>
  );
}
