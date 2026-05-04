import React, { useState } from 'react';
import { api } from './api/client';

export default function LoginPage({ onLogin }: { onLogin: (token: string) => void }) {
  const [mode, setMode] = useState<'login' | 'register'>('login');
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [error, setError] = useState('');

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setError('');
    const fn = mode === 'login' ? api.login : api.register;
    const r = await fn(email, password);
    if (r.code === 200) { onLogin(r.data.token); }
    else { setError(r.message || '操作失败'); }
  };

  return (
    <div className="login-page">
      <div className="login-card">
        <h1>朋友圈英语</h1>
        <p className="subtitle">从美剧中学英语</p>
        <form onSubmit={handleSubmit}>
          <input type="email" placeholder="邮箱" value={email}
            onChange={e => setEmail(e.target.value)} required />
          <input type="password" placeholder="密码（至少6位）" value={password}
            onChange={e => setPassword(e.target.value)} required minLength={6} />
          {error && <div className="error-msg">{error}</div>}
          <button type="submit">{mode === 'login' ? '登录' : '注册'}</button>
        </form>
        <p className="toggle-mode" onClick={() => setMode(m => m === 'login' ? 'register' : 'login')}>
          {mode === 'login' ? '没有账号？点击注册' : '已有账号？点击登录'}
        </p>
      </div>
    </div>
  );
}
