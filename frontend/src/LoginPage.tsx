import React, { useState, useRef, useCallback } from 'react';
import { api } from './api/client';

/** 校验邮箱格式 */
function isValidEmail(v: string) { return /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(v); }
/** 校验手机号格式 */
function isValidPhone(v: string) { return !v || /^1[3-9]\d{9}$/.test(v); }
/** 密码强度：至少8位，含字母+数字 */
function isStrongPassword(v: string) { return v.length >= 8 && /[a-zA-Z]/.test(v) && /[0-9]/.test(v); }
/** 密码强度等级 */
function pwdLevel(pwd: string): { label: string; color: string; percent: number } {
  if (!pwd) return { label: '', color: 'transparent', percent: 0 };
  if (pwd.length < 6) return { label: '太短', color: '#e17055', percent: 20 };
  const hasLetter = /[a-zA-Z]/.test(pwd);
  const hasNumber = /[0-9]/.test(pwd);
  const hasBoth = hasLetter && hasNumber;
  if (pwd.length >= 8 && hasBoth) return { label: '强', color: '#00b894', percent: 100 };
  if (pwd.length >= 6 && (hasLetter || hasNumber)) return { label: '中', color: '#fdcb6e', percent: 60 };
  return { label: '弱', color: '#e17055', percent: 35 };
}

export default function LoginPage({ onLogin }: { onLogin: (token: string) => void }) {
  const [mode, setMode] = useState<'login' | 'register'>('login');
  const [email, setEmail] = useState('');
  const [code, setCode] = useState('');
  const [password, setPassword] = useState('');
  const [password2, setPassword2] = useState('');
  const [phone, setPhone] = useState('');
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
    if (!isValidEmail(email)) { setError('邮箱格式不正确'); return; }
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
      if (!email || !password) { setError('请填写邮箱和密码'); return; }
      const r = await api.login(email, password);
      if (r.code === 200) { onLogin(r.data.token); }
      else { setError(r.message || '登录失败'); }
    } else {
      // 注册校验
      if (!email) { setError('请填写邮箱'); return; }
      if (!isValidEmail(email)) { setError('邮箱格式不正确'); return; }
      if (!code) { setError('请填写验证码'); return; }
      if (!password) { setError('请设置密码'); return; }
      if (!isStrongPassword(password)) { setError('密码至少8位，需包含字母和数字'); return; }
      if (password !== password2) { setError('两次密码不一致'); return; }
      if (phone && !isValidPhone(phone)) { setError('手机号格式不正确（11位数字）'); return; }

      const r = await api.register({ email, code, password, phone, invitedBy: invitedBy || undefined });
      if (r.code === 200) { onLogin(r.data.token); }
      else { setError(r.message || '注册失败'); }
    }
  };

  const switchMode = () => {
    setMode(m => m === 'login' ? 'register' : 'login');
    setError('');
    setSuccessMsg('');
  };

  const pwdStrength = pwdLevel(password);
  const pwd2Match = password2 && password === password2;

  return (
    <div className="login-page">
      <div className="login-card">
        <h1>朋友圈英语</h1>
        <p className="subtitle">从美剧中学英语</p>
        <form onSubmit={handleSubmit}>

          {/* ── 邮箱（必填） ── */}
          <input type="email" placeholder="邮箱" value={email}
            onChange={e => setEmail(e.target.value)} required
            style={email && !isValidEmail(email) ? { borderColor: '#e17055' } : {}} />

          {mode === 'register' && (
            <>
              {/* ── 验证码（必填） ── */}
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

              {/* ── 密码（必填，至少8位含字母+数字） ── */}
              <input type="password" placeholder="密码（至少8位，含字母和数字）" value={password}
                onChange={e => setPassword(e.target.value)} required />
              {password && (
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
              <input type="password" placeholder="确认密码" value={password2}
                onChange={e => setPassword2(e.target.value)} required
                style={password2 && !pwd2Match ? { borderColor: '#e17055' } : {}} />

              {/* ── 手机号（可选） ── */}
              <input type="tel" placeholder="手机号" required value={phone}
                onChange={e => setPhone(e.target.value)}
                style={phone && !isValidPhone(phone) ? { borderColor: '#e17055' } : {}} />

              {/* ── 邀请码（可选） ── */}
              <input type="text" placeholder="邀请码（可选）" value={invitedBy}
                onChange={e => setInvitedBy(e.target.value)} />
            </>
          )}

          {mode === 'login' && (
            <input type="password" placeholder="密码" value={password}
              onChange={e => setPassword(e.target.value)} required />
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
