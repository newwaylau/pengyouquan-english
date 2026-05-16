import React, { useState, useRef, useCallback, useEffect } from 'react';
import { api } from './api/client';
import { EyeOpen, EyeClosed } from './eye-icons';

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

/** 阻止空格键输入 */
function preventSpace(e: React.KeyboardEvent) { if (e.key === ' ') e.preventDefault(); }

export default function LoginPage({ onLogin, onHome }: { onLogin: (token: string) => void; onHome?: () => void }) {
  const [mode, setMode] = useState<'login' | 'register' | 'forgot'>('login');
  const [email, setEmail] = useState('');
  const [code, setCode] = useState('');
  const [password, setPassword] = useState('');
  const [password2, setPassword2] = useState('');
  const [phone, setPhone] = useState('');
  const [invitedBy, setInvitedBy] = useState('');
  const [error, setError] = useState('');
  const [successMsg, setSuccessMsg] = useState('');
  const [showPwd, setShowPwd] = useState(false);
  const [showPwd2, setShowPwd2] = useState(false);
  const [codeCountdown, setCodeCountdown] = useState(0);
  const [codeSending, setCodeSending] = useState(false);
  const timerRef = useRef<number | null>(null);

  // 忘记密码状态
  const [forgotStep, setForgotStep] = useState<1 | 2>(1);
  const [forgotEmail, setForgotEmail] = useState('');
  const [forgotCode, setForgotCode] = useState('');
  const [forgotPassword, setForgotPassword] = useState('');
  const [forgotPassword2, setForgotPassword2] = useState('');
  const [forgotShowPwd, setForgotShowPwd] = useState(false);
  const [forgotShowPwd2, setForgotShowPwd2] = useState(false);
  const [forgotCountdown, setForgotCountdown] = useState(0);
  const [forgotSending, setForgotSending] = useState(false);
  const forgotTimerRef = useRef<number | null>(null);

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

  const startForgotCountdown = useCallback(() => {
    setForgotCountdown(60);
    if (forgotTimerRef.current) clearInterval(forgotTimerRef.current);
    forgotTimerRef.current = window.setInterval(() => {
      setForgotCountdown(prev => {
        if (prev <= 1) {
          if (forgotTimerRef.current) clearInterval(forgotTimerRef.current);
          forgotTimerRef.current = null;
          return 0;
        }
        return prev - 1;
      });
    }, 1000);
  }, []);

  // 组件卸载时清理定时器
  useEffect(() => {
    return () => {
      if (timerRef.current) clearInterval(timerRef.current);
      if (forgotTimerRef.current) clearInterval(forgotTimerRef.current);
    };
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

  const handleForgotSendCode = async () => {
    if (!forgotEmail) { setError('请先输入邮箱'); return; }
    if (!isValidEmail(forgotEmail)) { setError('邮箱格式不正确'); return; }
    setForgotSending(true);
    setError('');
    setSuccessMsg('');
    try {
      const r = await api.forgotPasswordSendCode(forgotEmail);
      if (r.code === 200) {
        setSuccessMsg('验证码已发送到邮箱');
        startForgotCountdown();
        setForgotStep(2);
      } else {
        setError(r.message || '发送失败');
      }
    } catch {
      setError('网络错误');
    } finally {
      setForgotSending(false);
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
    } else if (mode === 'register') {
      if (!email) { setError('请填写邮箱'); return; }
      if (!isValidEmail(email)) { setError('邮箱格式不正确'); return; }
      if (!code) { setError('请填写验证码'); return; }
      if (!password) { setError('请设置密码'); return; }
      if (!isStrongPassword(password)) { setError('密码至少8位，需包含字母和数字'); return; }
      if (password !== password2) { setError('两次密码不一致'); return; }
      if (phone && !isValidPhone(phone)) { setError('手机号格式不正确（11位数字）'); return; }

      const r = await api.register({ email, code, password, phone: phone || undefined, invitedBy: invitedBy || undefined });
      if (r.code === 200) { onLogin(r.data.token); }
      else { setError(r.message || '注册失败'); }
    } else if (mode === 'forgot' && forgotStep === 2) {
      if (!forgotCode) { setError('请填写验证码'); return; }
      if (!forgotPassword) { setError('请设置新密码'); return; }
      if (!isStrongPassword(forgotPassword)) { setError('密码至少8位，需包含字母和数字'); return; }
      if (forgotPassword !== forgotPassword2) { setError('两次密码不一致'); return; }

      const r = await api.resetPassword({ email: forgotEmail, code: forgotCode, password: forgotPassword });
      if (r.code === 200) {
        setSuccessMsg('密码重置成功！即将自动登录...');
        setTimeout(() => onLogin(r.data.token), 1000);
      } else {
        setError(r.message || '重置失败');
      }
    }
  };

  const switchMode = () => {
    if (mode === 'forgot') {
      setMode('login');
    } else {
      setMode(m => m === 'login' ? 'register' : 'login');
    }
    setError('');
    setSuccessMsg('');
  };

  const goToForgot = () => {
    setMode('forgot');
    setForgotStep(1);
    setForgotEmail(email);
    setForgotCode('');
    setForgotPassword('');
    setForgotPassword2('');
    setForgotShowPwd(false);
    setForgotShowPwd2(false);
    setForgotCountdown(0);
    if (forgotTimerRef.current) clearInterval(forgotTimerRef.current);
    setError('');
    setSuccessMsg('');
  };

  const pwdStrength = pwdLevel(password);
  const pwd2Match = password2 && password === password2;

  const forgotPwdStrength = pwdLevel(forgotPassword);
  const forgotPwd2Match = forgotPassword2 && forgotPassword === forgotPassword2;

  return (
    <div className="login-page">
      <div className="login-card">
        <h1 style={{ cursor: 'pointer' }} onClick={onHome}>英语剧场</h1>
        <p className="subtitle">听懂每一句台词</p>
        <form onSubmit={handleSubmit}>

          {/* ── 登录模式 ── */}
          {mode === 'login' && (
            <>
              <input type="text" placeholder="邮箱/手机号"
                value={email}
                onChange={e => setEmail(e.target.value.replace(/\s/g, ''))} onKeyDown={preventSpace} required
                style={email && email.includes('@') && !isValidEmail(email) ? { borderColor: '#e17055' } : {}} />

              <input type="password" placeholder="密码" value={password}
                onChange={e => setPassword(e.target.value.replace(/\s/g, ''))} onKeyDown={preventSpace} required />

              <div className="forgot-link">
                <span onClick={goToForgot}>忘记密码？</span>
              </div>
            </>
          )}

          {/* ── 注册模式 ── */}
          {mode === 'register' && (
            <>
              <input type="email" placeholder="邮箱"
                value={email}
                onChange={e => setEmail(e.target.value.replace(/\s/g, ''))} onKeyDown={preventSpace} required
                style={email && !isValidEmail(email) ? { borderColor: '#e17055' } : {}} />

              {/* 验证码 */}
              <div className="code-row">
                <input type="text" placeholder="邮箱验证码" value={code}
                  onChange={e => setCode(e.target.value.replace(/\s/g, ''))} onKeyDown={preventSpace} required maxLength={6}
                  className="code-input" />
                <button type="button" className="send-code-btn"
                  onClick={handleSendCode}
                  disabled={codeSending || codeCountdown > 0}>
                  {codeSending ? '发送中...' : codeCountdown > 0 ? `${codeCountdown}s` : '发送验证码'}
                </button>
              </div>

              {/* 密码 */}
              <div className="pwd-wrapper">
                <input type={showPwd ? 'text' : 'password'} placeholder="密码（至少8位，含字母和数字）" value={password}
                  onChange={e => setPassword(e.target.value.replace(/\s/g, ''))} onKeyDown={preventSpace} required />
                <span className="eye-btn" onClick={() => setShowPwd(!showPwd)}>
                  {showPwd ? <EyeOpen /> : <EyeClosed />}
                </span>
              </div>
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
              <div className="pwd-wrapper">
                <input type={showPwd2 ? 'text' : 'password'} placeholder="确认密码" value={password2}
                  onChange={e => setPassword2(e.target.value.replace(/\s/g, ''))} onKeyDown={preventSpace} required
                  style={password2 && !pwd2Match ? { borderColor: '#e17055' } : {}} />
                <span className="eye-btn" onClick={() => setShowPwd2(!showPwd2)}>
                  {showPwd2 ? <EyeOpen /> : <EyeClosed />}
                </span>
              </div>

              <input type="tel" placeholder="手机号（可选）" value={phone}
                onChange={e => setPhone(e.target.value.replace(/\s/g, ''))} onKeyDown={preventSpace}
                style={phone && !isValidPhone(phone) ? { borderColor: '#e17055' } : {}} />

              <input type="text" placeholder="邀请码（可选）" value={invitedBy}
                onChange={e => setInvitedBy(e.target.value.replace(/\s/g, ''))} onKeyDown={preventSpace} />
            </>
          )}

          {/* ── 忘记密码模式 ── */}
          {mode === 'forgot' && (
            <>
              {forgotStep === 1 && (
                <>
                  <input type="email" placeholder="邮箱"
                    value={forgotEmail}
                    onChange={e => setForgotEmail(e.target.value.replace(/\s/g, ''))} onKeyDown={preventSpace} required
                    autoFocus
                    style={forgotEmail && !isValidEmail(forgotEmail) ? { borderColor: '#e17055' } : {}} />

                  <div className="code-row">
                    <button type="button" className="send-code-btn" style={{ width: '100%' }}
                      onClick={handleForgotSendCode}
                      disabled={forgotSending || forgotCountdown > 0}>
                      {forgotSending ? '发送中...' : forgotCountdown > 0 ? `${forgotCountdown}s` : '发送验证码'}
                    </button>
                  </div>
                </>
              )}

              {forgotStep === 2 && (
                <>
                  {/* 验证码 */}
                  <div className="code-row">
                    <input type="text" placeholder="邮箱验证码" value={forgotCode}
                      onChange={e => setForgotCode(e.target.value.replace(/\s/g, ''))} onKeyDown={preventSpace} required maxLength={6}
                      className="code-input" />
                    <button type="button" className="send-code-btn"
                      onClick={handleForgotSendCode}
                      disabled={forgotSending || forgotCountdown > 0}>
                      {forgotSending ? '发送中...' : forgotCountdown > 0 ? `${forgotCountdown}s` : '重新发送'}
                    </button>
                  </div>

                  {/* 新密码 */}
                  <div className="pwd-wrapper">
                    <input type={forgotShowPwd ? 'text' : 'password'} placeholder="新密码（至少8位，含字母和数字）" value={forgotPassword}
                      onChange={e => setForgotPassword(e.target.value.replace(/\s/g, ''))} onKeyDown={preventSpace} required />
                    <span className="eye-btn" onClick={() => setForgotShowPwd(!forgotShowPwd)}>
                      {forgotShowPwd ? <EyeOpen /> : <EyeClosed />}
                    </span>
                  </div>
                  {forgotPassword && (
                    <div className="pwd-strength-bar">
                      <div className="pwd-strength-fill" style={{
                        width: `${forgotPwdStrength.percent}%`,
                        background: forgotPwdStrength.color
                      }} />
                      <span className="pwd-strength-label" style={{ color: forgotPwdStrength.color }}>
                        {forgotPwdStrength.label}
                      </span>
                    </div>
                  )}

                  {/* 确认新密码 */}
                  <div className="pwd-wrapper">
                    <input type={forgotShowPwd2 ? 'text' : 'password'} placeholder="确认新密码" value={forgotPassword2}
                      onChange={e => setForgotPassword2(e.target.value.replace(/\s/g, ''))} onKeyDown={preventSpace} required
                      style={forgotPassword2 && !forgotPwd2Match ? { borderColor: '#e17055' } : {}} />
                    <span className="eye-btn" onClick={() => setForgotShowPwd2(!forgotShowPwd2)}>
                      {forgotShowPwd2 ? <EyeOpen /> : <EyeClosed />}
                    </span>
                  </div>
                </>
              )}
            </>
          )}

          {error && <div className="error-msg">{error}</div>}
          {successMsg && <div className="success-msg">{successMsg}</div>}

          <button type="submit" className="submit">
            {mode === 'login' ? '登录' : mode === 'register' ? '注册' : '重置密码'}
          </button>
        </form>

        {mode === 'forgot' ? (
          <p className="toggle-mode" onClick={switchMode}>返回登录</p>
        ) : (
          <p className="toggle-mode" onClick={switchMode}>
            {mode === 'login' ? '没有账号？点击注册' : '已有账号？点击登录'}
          </p>
        )}
      </div>
    </div>
  );
}
