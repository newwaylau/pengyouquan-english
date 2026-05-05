import React, { useState, useRef, useCallback } from 'react';
import { api } from './api/client';

export default function LoginPage({ onLogin }: { onLogin: (token: string) => void }) {
  const [mode, setMode] = useState<'login' | 'register'>('login');
  const [email, setEmail] = useState('');
  const [phone, setPhone] = useState('');
  const [code, setCode] = useState('');
  const [password, setPassword] = useState('');
  const [invitedBy, setInvitedBy] = useState('');
  const [error, setError] = useState('');
  const [successMsg, setSuccessMsg] = useState('');
  const [codeCountdown, setCodeCountdown] = useState(0);
  const [codeSending, setCodeSending] = useState(false);
  const timerRef = useRef<number | null>(null);

  const startCountdown = useCallback(() => {
    setCodeCountdown(60);
    if (timerRef.current) clearInterval(timerRef.current);
    timerRef.current = window.setInterval(() => {
      setCodeCountdown(prev => {
        if (prev <= 1) {
          if (timerRef.current) clearInterval(timerRef.current);
          timerRef.current = null;
          return 0;
        }
        return prev - 1;
      });
    }, 1000);
  }, []);

  const handleSendCode = async () => {
    if (!email) { setError('请先输入邮箱'); return; }
    setCodeSending(true);
    setError('');
    setSuccessMsg('');
    try {
      const r = await api.sendCode(email);
      if (r.code === 200) {
        setSuccessMsg('验证码已发送到邮箱');
        startCountdown();
      } else {
        setError(r.message || '发送失败');
      }
    } catch {
      setError('网络错误');
    } finally {
      setCodeSending(false);
    }
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setError('');
    setSuccessMsg('');

    if (mode === 'login') {
      const r = await api.login(email, password);
      if (r.code === 200) { onLogin(r.data.token); }
      else { setError(r.message || '登录失败'); }
    } else {
      const r = await api.register({ email, phone, code, password, invitedBy: invitedBy || undefined });
      if (r.code === 200) { onLogin(r.data.token); }
      else { setError(r.message || '注册失败'); }
    }
  };

  const switchMode = () => {
    setMode(m => m === 'login' ? 'register' : 'login');
    setError('');
    setSuccessMsg('');
  };

  // 密码强度提示
  const getPwdStrength = (pwd: string): { label: string; color: string; percent: number } => {
    if (!pwd) return { label: '', color: 'transparent', percent: 0 };
    if (pwd.length < 6) return { label: '太短', color: '#e17055', percent: 20 };
    let score = 0;
    if (pwd.length >= 8) score += 25;
    if (/[a-z]/.test(pwd)) score += 20;
    if (/[A-Z]/.test(pwd)) score += 20;
    if (/[0-9]/.test(pwd)) score += 20;
    if (/[^a-zA-Z0-9]/.test(pwd)) score += 15;
    if (score >= 90) return { label: '强', color: '#00b894', percent: 100 };
    if (score >= 60) return { label: '中', color: '#fdcb6e', percent: 65 };
    return { label: '弱', color: '#e17055', percent: 35 };
  };

  const pwdStrength = getPwdStrength(password);

  return (
    <div className="login-page">
      <div className="login-card">
        <h1>朋友圈英语</h1>
        <p className="subtitle">从美剧中学英语</p>
        <form onSubmit={handleSubmit}>
          {/* 邮箱 */}
          <input type="email" placeholder="邮箱" value={email}
            onChange={e => setEmail(e.target.value)} required />

          {mode === 'register' && (
            <>
              {/* 手机号 */}
              <input type="tel" placeholder="手机号" value={phone}
                onChange={e => setPhone(e.target.value)} required />

              {/* 发送验证码 + 验证码输入 */}
              <div className="code-row">
                <input type="text" placeholder="验证码" value={code}
                  onChange={e => setCode(e.target.value)} required maxLength={6}
                  className="code-input" />
                <button type="button" className="send-code-btn"
                  onClick={handleSendCode}
                  disabled={codeSending || codeCountdown > 0}>
                  {codeSending ? '发送中...' : codeCountdown > 0 ? `${codeCountdown}s` : '发送验证码'}
                </button>
              </div>

              {/* 邀请码（可选） */}
              <input type="text" placeholder="邀请码（可选）" value={invitedBy}
                onChange={e => setInvitedBy(e.target.value)} />
            </>
          )}

          {/* 密码 */}
          <input type="password" placeholder="密码（至少6位）" value={password}
            onChange={e => setPassword(e.target.value)} required minLength={6} />

          {/* 密码强度提示 */}
          {mode === 'register' && password && (
            <div className="pwd-strength-bar">
              <div className="pwd-strength-fill" style={{
                width: `${pwdStrength.percent}%`,
                background: pwdStrength.color
              }} />
              <span className="pwd-strength-label" style={{ color: pwdStrength.color }}>
                {pwdStrength.label}
              </span>
            </div>
          )}

          {error && <div className="error-msg">{error}</div>}
          {successMsg && <div className="success-msg">{successMsg}</div>}
          <button type="submit">{mode === 'login' ? '登录' : '注册'}</button>
        </form>
        <p className="toggle-mode" onClick={switchMode}>
          {mode === 'login' ? '没有账号？点击注册' : '已有账号？点击登录'}
        </p>
      </div>
    </div>
  );
}
