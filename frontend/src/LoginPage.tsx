import React, { useState, useRef, useCallback, useEffect } from 'react';
import { IconKey, IconRocket, IconSave, IconCheckPlain } from './Icons';
import { api, setToken, clearToken } from './api/client';
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
  if (pwd.length < 6) return { label: '太短', color: '#ef4444', percent: 20 };
  const hasLetter = /[a-zA-Z]/.test(pwd);
  const hasNumber = /[0-9]/.test(pwd);
  const hasBoth = hasLetter && hasNumber;
  if (pwd.length >= 8 && hasBoth) return { label: '强', color: '#22c55e', percent: 100 };
  if (pwd.length >= 6 && (hasLetter || hasNumber)) return { label: '中', color: '#f59e0b', percent: 60 };
  return { label: '弱', color: '#ef4444', percent: 35 };
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
  const [showLoginPwd, setShowLoginPwd] = useState(false);
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

  // ── 登录后欢迎面板状态 ──
  const [loggedIn, setLoggedIn] = useState(false);
  const [loginToken, setLoginToken] = useState('');
  const [userData, setUserData] = useState<any>(null);
  const [welcomeView, setWelcomeView] = useState<'welcome' | 'password' | 'pwd-success'>('welcome');

  // ── 修改密码表单状态 ──
  const [oldPassword, setOldPassword] = useState('');
  const [newPwd, setNewPwd] = useState('');
  const [confirmPwd, setConfirmPwd] = useState('');
  const [pwdUpdating, setPwdUpdating] = useState(false);
  const [pwdError, setPwdError] = useState('');
  const [pwdSuccess, setPwdSuccess] = useState('');
  const [showOldPwd, setShowOldPwd] = useState(false);
  const [showNewPwd, setShowNewPwd] = useState(false);
  const [showConfirmPwd, setShowConfirmPwd] = useState(false);

  // ── Ping 测试连接 ──
  const [pingStatus, setPingStatus] = useState<'idle' | 'loading' | 'success' | 'error'>('idle');
  const [pingResult, setPingResult] = useState<string>('');

  const handlePing = async () => {
    setPingStatus('loading');
    setPingResult('');
    try {
      const r = await api.ping();
      if (r.code === 200) {
        setPingStatus('success');
        setPingResult(JSON.stringify(r.data, null, 2));
      } else {
        setPingStatus('error');
        setPingResult(r.message || '请求失败');
      }
    } catch (e: any) {
      setPingStatus('error');
      setPingResult(e?.message || '网络错误');
    }
  };

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
      if (r.code === 200) {
        // 先设置 token，然后获取用户信息，显示欢迎面板
        setToken(r.data.token);
        setLoginToken(r.data.token);
        const user = await api.me();
        if (user.code === 200) {
          setUserData(user.data);
          setLoggedIn(true);
          setWelcomeView('welcome');
        } else {
          setError('获取用户信息失败');
        }
      } else {
        setError(r.message || '登录失败');
      }
    } else if (mode === 'register') {
      if (!email) { setError('请填写邮箱'); return; }
      if (!isValidEmail(email)) { setError('邮箱格式不正确'); return; }
      if (!code) { setError('请填写验证码'); return; }
      if (!password) { setError('请设置密码'); return; }
      if (!isStrongPassword(password)) { setError('密码至少8位，需包含字母和数字'); return; }
      if (password !== password2) { setError('两次密码不一致'); return; }
      if (phone && !isValidPhone(phone)) { setError('手机号格式不正确（11位数字）'); return; }

      const r = await api.register({ email, code, password, phone: phone || undefined, invitedBy: invitedBy || undefined });
      if (r.code === 200) {
        setToken(r.data.token);
        setLoginToken(r.data.token);
        const user = await api.me();
        if (user.code === 200) {
          setUserData(user.data);
          setLoggedIn(true);
          setWelcomeView('welcome');
        } else {
          setError('获取用户信息失败');
        }
      } else {
        setError(r.message || '注册失败');
      }
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

  const handleGoToPractice = () => {
    if (loginToken) onLogin(loginToken);
  };

  const handleLogoutFromWelcome = () => {
    clearToken();
    setLoggedIn(false);
    setLoginToken('');
    setUserData(null);
    setWelcomeView('welcome');
    setPassword('');
  };

  const resetPwdFields = () => {
    setOldPassword('');
    setNewPwd('');
    setConfirmPwd('');
    setPwdError('');
    setPwdSuccess('');
    setShowOldPwd(false);
    setShowNewPwd(false);
    setShowConfirmPwd(false);
  };

  const handleChangePassword = async () => {
    if (!oldPassword) { setPwdError('请输入当前密码'); return; }
    if (!newPwd) { setPwdError('请输入新密码'); return; }
    if (!isStrongPassword(newPwd)) { setPwdError('新密码至少8位，需包含字母和数字'); return; }
    if (newPwd !== confirmPwd) { setPwdError('两次新密码不一致'); return; }
    if (oldPassword === newPwd) { setPwdError('新密码不能与旧密码相同'); return; }
    setPwdError('');
    setPwdSuccess('');
    setPwdUpdating(true);
    const r = await api.updatePassword({ oldPassword, newPassword: newPwd });
    if (r.code === 200) {
      setPwdSuccess('密码修改成功！');
      setOldPassword('');
      setNewPwd('');
      setConfirmPwd('');
      setTimeout(() => { setPwdSuccess(''); setWelcomeView('welcome'); }, 3000);
    } else {
      setPwdError(r.message || '修改失败，旧密码可能不正确');
    }
    setPwdUpdating(false);
  };

  const newPwdStrength = pwdLevel(newPwd);
  const confirmMatch = confirmPwd && newPwd === confirmPwd;

  const pwdStrength = pwdLevel(password);
  const pwd2Match = password2 && password === password2;

  const forgotPwdStrength = pwdLevel(forgotPassword);
  const forgotPwd2Match = forgotPassword2 && forgotPassword === forgotPassword2;

  // ── 登录后欢迎面板 ──
  if (loggedIn) {
    const displayName = userData?.nickname || userData?.email || email;
    const avatarLetter = (displayName || 'U').charAt(0).toUpperCase();
    const userEmail = userData?.email || email;

    return (
      <div className="login-page">
        <div className="login-card-welcome">
          <div className="post-login-content" key={welcomeView}>
            {welcomeView === 'welcome' && (
              <>
                {/* 用户头像区 */}
                <div className="welcome-avatar-section">
                  <div className="welcome-avatar">{avatarLetter}</div>
                  <div className="welcome-greeting">欢迎回来，{displayName}</div>
                  <div className="welcome-email">{userEmail} · 已登录</div>
                </div>

              {/* 登录成功徽章 */}
              <div className="login-success-badge">
                <span><IconCheckPlain /></span>
                <span>登录成功</span>
              </div>

              {/* 修改密码入口卡 */}
              <button className="btn-outline change-pwd-btn" onClick={() => { resetPwdFields(); setWelcomeView('password'); }}>
                <div className="change-pwd-icon"><IconKey /></div>
                <div className="change-pwd-label">
                  <div className="change-pwd-label-main">修改密码</div>
                  <div className="change-pwd-label-sub">定期更换密码可提高账户安全性</div>
                </div>
                <div className="change-pwd-arrow">›</div>
              </button>

              <div className="welcome-divider" />

              <div className="welcome-section-title">快速操作</div>

              <div className="welcome-action-row">
                <button className="btn-primary welcome-primary-btn" onClick={handleGoToPractice}>
                  <IconRocket /> 进入练习
                </button>
                <button className="btn-outline welcome-secondary-btn" onClick={handleLogoutFromWelcome}>
                  退出
                </button>
              </div>
            </>
          )}

          {welcomeView === 'password' && (
            <>
              <h2 style={{ fontSize: '1.25rem', marginBottom: 24, display: 'flex', alignItems: 'center', justifyContent: 'center', gap: 8 }}>
                <IconKey /> 修改密码
              </h2>

              {/* 当前密码 */}
              <div className="pwd-wrapper">
                <input type={showOldPwd ? 'text' : 'password'} placeholder="当前密码" value={oldPassword}
                  onChange={e => { setOldPassword(e.target.value); setPwdError(''); setPwdSuccess(''); }}
                  onKeyDown={preventSpace} required />
                <span className="eye-btn btn-icon" onClick={() => setShowOldPwd(!showOldPwd)}>
                  {showOldPwd ? <EyeOpen /> : <EyeClosed />}
                </span>
              </div>

              {/* 新密码 */}
              <div className="pwd-wrapper">
                <input type={showNewPwd ? 'text' : 'password'} placeholder="新密码（至少8位，含字母和数字）" value={newPwd}
                  onChange={e => { setNewPwd(e.target.value); setPwdError(''); setPwdSuccess(''); }}
                  onKeyDown={preventSpace} required />
                <span className="eye-btn btn-icon" onClick={() => setShowNewPwd(!showNewPwd)}>
                  {showNewPwd ? <EyeOpen /> : <EyeClosed />}
                </span>
              </div>

              {/* 密码强度条 */}
              {newPwd && (
                <div className="pwd-strength-bar" style={{ marginTop: -4, marginBottom: 12 }}>
                  <div className="pwd-strength-fill" style={{
                    width: `${newPwdStrength.percent}%`,
                    background: newPwdStrength.color
                  }} />
                  <span className="pwd-strength-label" style={{ color: newPwdStrength.color }}>
                    {newPwdStrength.label}
                  </span>
                </div>
              )}

              {/* 确认新密码 */}
              <div className="pwd-wrapper">
                <input type={showConfirmPwd ? 'text' : 'password'} placeholder="确认新密码" value={confirmPwd}
                  onChange={e => { setConfirmPwd(e.target.value); setPwdError(''); setPwdSuccess(''); }}
                  onKeyDown={preventSpace} required
                  style={confirmPwd && !confirmMatch ? { borderColor: '#ef4444' } : {}} />
                <span className="eye-btn btn-icon" onClick={() => setShowConfirmPwd(!showConfirmPwd)}>
                  {showConfirmPwd ? <EyeOpen /> : <EyeClosed />}
                </span>
              </div>
              {confirmPwd && !confirmMatch && (
                <div style={{ color: '#ef4444', fontSize: 12, textAlign: 'left', marginTop: -8, marginBottom: 8 }}>两次密码不一致</div>
              )}

              {/* 成功提示 */}
              {pwdSuccess && (
                <div className="pwd-success-toast" style={{ marginBottom: 12 }}>
                  <span><IconCheckPlain /></span>
                  <span>密码修改成功！</span>
                </div>
              )}

              {pwdError && <div className="error-msg">{pwdError}</div>}

              <div className="pwd-btn-row">
                <button className="btn-primary pwd-save-btn" onClick={handleChangePassword} disabled={pwdUpdating}>
                  {pwdUpdating ? '保存中...' : <><IconSave /> 保存密码</>}
                </button>
                <button className="btn-outline pwd-cancel-btn" onClick={() => { resetPwdFields(); setWelcomeView('welcome'); }}>取消</button>
              </div>

              <p className="pwd-back-link" onClick={() => { resetPwdFields(); setWelcomeView('welcome'); }}>← 返回</p>
            </>
          )}
          </div>
        </div>
      </div>
    );
  }

  // ── 未登录：左右分栏布局 ──
  return (
    <div className="login-page">
      {/* 左侧品牌区 */}
      <div className="login-brand">
        <div className="login-brand-icon">🎬</div>
        <h1>听懂每一句台词</h1>
        <p className="brand-subtitle">
          通过精选美剧台词，逐词精听、跟读模仿，<br />真正提升英语听力与口语能力。
        </p>
        <div className="login-brand-features">
          <div className="login-brand-feature">
            <span className="check">✓</span>
            精选《老友记》《生活大爆炸》等经典美剧台词
          </div>
          <div className="login-brand-feature">
            <span className="check">✓</span>
            逐词填空练习 + 智能纠错 + 发音跟读
          </div>
          <div className="login-brand-feature">
            <span className="check">✓</span>
            学习进度追踪，错题自动复习
          </div>
        </div>
      </div>

      {/* 右侧表单区 */}
      <div className="login-form-container">
        <h2>欢迎回来</h2>
        <p className="form-subtitle">登录你的账号继续学习</p>
        <form onSubmit={handleSubmit}>

          {/* ── 登录模式 ── */}
          {mode === 'login' && (
            <>
              <label className="field-label">邮箱 / 手机号</label>
              <input type="text" placeholder="newwaylau@hotmail.com"
                value={email}
                onChange={e => setEmail(e.target.value.replace(/\s/g, ''))} onKeyDown={preventSpace} required
                style={email && email.includes('@') && !isValidEmail(email) ? { borderColor: '#ef4444' } : {}} />

              <label className="field-label">密码</label>
              <div className="pwd-wrapper">
                <input type={showLoginPwd ? 'text' : 'password'} placeholder="请输入密码" value={password}
                  onChange={e => setPassword(e.target.value.replace(/\s/g, ''))} onKeyDown={preventSpace} required />
                <span className="eye-btn btn-icon" onClick={() => setShowLoginPwd(!showLoginPwd)}>
                  {showLoginPwd ? <EyeOpen /> : <EyeClosed />}
                </span>
              </div>

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
                style={email && !isValidEmail(email) ? { borderColor: '#ef4444' } : {}} />

              {/* 验证码 */}
              <div className="code-row">
                <input type="text" placeholder="邮箱验证码" value={code}
                  onChange={e => setCode(e.target.value.replace(/\s/g, ''))} onKeyDown={preventSpace} required maxLength={6}
                  className="code-input" />
                <button type="button" className="btn-outline send-code-btn"
                  onClick={handleSendCode}
                  disabled={codeSending || codeCountdown > 0}>
                  {codeSending ? '发送中...' : codeCountdown > 0 ? `${codeCountdown}s` : '发送验证码'}
                </button>
              </div>

              {/* 密码 */}
              <div className="pwd-wrapper">
                <input type={showPwd ? 'text' : 'password'} placeholder="密码（至少8位，含字母和数字）" value={password}
                  onChange={e => setPassword(e.target.value.replace(/\s/g, ''))} onKeyDown={preventSpace} required />
                <span className="eye-btn btn-icon" onClick={() => setShowPwd(!showPwd)}>
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
                  style={password2 && !pwd2Match ? { borderColor: '#ef4444' } : {}} />
                <span className="eye-btn btn-icon" onClick={() => setShowPwd2(!showPwd2)}>
                  {showPwd2 ? <EyeOpen /> : <EyeClosed />}
                </span>
              </div>

              <input type="tel" placeholder="手机号（可选）" value={phone}
                onChange={e => setPhone(e.target.value.replace(/\s/g, ''))} onKeyDown={preventSpace}
                style={phone && !isValidPhone(phone) ? { borderColor: '#ef4444' } : {}} />

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
                    style={forgotEmail && !isValidEmail(forgotEmail) ? { borderColor: '#ef4444' } : {}} />

                  <div className="code-row">
                    <button type="button" className="btn-outline send-code-btn" style={{ width: '100%' }}
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
                    <button type="button" className="btn-outline send-code-btn"
                      onClick={handleForgotSendCode}
                      disabled={forgotSending || forgotCountdown > 0}>
                      {forgotSending ? '发送中...' : forgotCountdown > 0 ? `${forgotCountdown}s` : '重新发送'}
                    </button>
                  </div>

                  {/* 新密码 */}
                  <div className="pwd-wrapper">
                    <input type={forgotShowPwd ? 'text' : 'password'} placeholder="新密码（至少8位，含字母和数字）" value={forgotPassword}
                      onChange={e => setForgotPassword(e.target.value.replace(/\s/g, ''))} onKeyDown={preventSpace} required />
                    <span className="eye-btn btn-icon" onClick={() => setForgotShowPwd(!forgotShowPwd)}>
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
                      style={forgotPassword2 && !forgotPwd2Match ? { borderColor: '#ef4444' } : {}} />
                    <span className="eye-btn btn-icon" onClick={() => setForgotShowPwd2(!forgotShowPwd2)}>
                      {forgotShowPwd2 ? <EyeOpen /> : <EyeClosed />}
                    </span>
                  </div>
                </>
              )}
            </>
          )}

          {error && <div className="error-msg">{error}</div>}
          {successMsg && <div className="success-msg">{successMsg}</div>}

          <button type="submit" className="btn-primary full submit-btn">
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

        {/* 测试连接 */}
        <div className="ping-section">
          <button className="btn-outline ping-btn" onClick={handlePing} disabled={pingStatus === 'loading'}>
            {pingStatus === 'loading' ? '测试中...' : '🔗 测试连接'}
          </button>
          {pingStatus === 'success' && (
            <div className="ping-result ping-success">✅ 连接成功：{pingResult}</div>
          )}
          {pingStatus === 'error' && (
            <div className="ping-result ping-error">❌ 连接失败：{pingResult}</div>
          )}
        </div>
      </div>
    </div>
  );
}
