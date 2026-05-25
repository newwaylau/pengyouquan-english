import React, { useState } from 'react';
import { api } from './api/client';

function pwdLevel(pwd: string): { label: string; color: string; percent: number } {
  if (!pwd) return { label: '', color: 'transparent', percent: 0 };
  let score = 0;
  if (pwd.length >= 8) score += 1;
  if (pwd.length >= 10) score += 1;
  if (/[A-Z]/.test(pwd)) score += 1;
  if (/[a-z]/.test(pwd)) score += 1;
  if (/[0-9]/.test(pwd)) score += 1;
  if (/[^a-zA-Z0-9]/.test(pwd)) score += 1;
  if (pwd.length >= 14) score += 1;
  if (score <= 2) return { label: '弱', color: '#ef4444', percent: 25 };
  if (score <= 4) return { label: '中', color: '#f59e0b', percent: 50 };
  if (score <= 6) return { label: '强', color: '#14b8a6', percent: 75 };
  return { label: '非常强', color: '#10b981', percent: 100 };
}

export default function ChangePasswordModal({ onClose }: { onClose: () => void }) {
  const [currentPwd, setCurrentPwd] = useState('');
  const [newPwd, setNewPwd] = useState('');
  const [confirmPwd, setConfirmPwd] = useState('');
  const [showCurrent, setShowCurrent] = useState(false);
  const [showNew, setShowNew] = useState(false);
  const [showConfirm, setShowConfirm] = useState(false);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState('');
  const [success, setSuccess] = useState(false);

  const strength = pwdLevel(newPwd);
  const matchOk = confirmPwd === '' ? null : confirmPwd === newPwd;
  const canSave = !!(currentPwd.length > 0 && newPwd.length >= 8 && /[a-zA-Z]/.test(newPwd) && /[0-9]/.test(newPwd) && confirmPwd === newPwd);

  const handleSave = async () => {
    if (!canSave) return;
    setSaving(true);
    setError('');
    try {
      const res = await api.updatePassword({ oldPassword: currentPwd, newPassword: newPwd });
      if (res.code === 200) {
        setSuccess(true);
        setCurrentPwd('');
        setNewPwd('');
        setConfirmPwd('');
        setTimeout(() => onClose(), 2000);
      } else {
        setError(res.message || '修改失败');
      }
    } catch {
      setError('网络错误，请重试');
    } finally {
      setSaving(false);
    }
  };

  return (
    <>
      <div className="change-pwd-overlay" onClick={onClose} />
      <div className="change-pwd-modal">
        <div className="change-pwd-header">
          <h2 className="change-pwd-title">修改密码</h2>
          <button className="change-pwd-close" onClick={onClose}>✕</button>
        </div>

        {success ? (
          <div className="change-pwd-success">
            <div className="change-pwd-success-icon">✓</div>
            <div className="change-pwd-success-text">密码修改成功</div>
          </div>
        ) : (
          <div className="change-pwd-form">
            {error && <div className="change-pwd-error">{error}</div>}

            <label className="change-pwd-label">当前密码</label>
            <div className="change-pwd-input-wrap">
              <input
                type={showCurrent ? 'text' : 'password'}
                value={currentPwd}
                onChange={e => setCurrentPwd(e.target.value)}
                placeholder="输入当前密码"
                autoFocus
              />
              <button
                className="change-pwd-eye"
                onClick={() => setShowCurrent(!showCurrent)}
                tabIndex={-1}
                type="button"
              >
                {showCurrent ? '🙈' : '👁'}
              </button>
            </div>

            <label className="change-pwd-label">新密码</label>
            <div className="change-pwd-input-wrap">
              <input
                type={showNew ? 'text' : 'password'}
                value={newPwd}
                onChange={e => setNewPwd(e.target.value)}
                placeholder="至少8位，含字母和数字"
              />
              <button
                className="change-pwd-eye"
                onClick={() => setShowNew(!showNew)}
                tabIndex={-1}
                type="button"
              >
                {showNew ? '🙈' : '👁'}
              </button>
            </div>
            {newPwd && (
              <div className="change-pwd-strength">
                <div className="change-pwd-strength-bar">
                  <div
                    className="change-pwd-strength-fill"
                    style={{ width: `${strength.percent}%`, background: strength.color }}
                  />
                </div>
                <span className="change-pwd-strength-label" style={{ color: strength.color }}>
                  {strength.label}
                </span>
              </div>
            )}

            <label className="change-pwd-label">确认新密码</label>
            <div className="change-pwd-input-wrap">
              <input
                type={showConfirm ? 'text' : 'password'}
                value={confirmPwd}
                onChange={e => setConfirmPwd(e.target.value)}
                placeholder="再次输入新密码"
              />
              <button
                className="change-pwd-eye"
                onClick={() => setShowConfirm(!showConfirm)}
                tabIndex={-1}
                type="button"
              >
                {showConfirm ? '🙈' : '👁'}
              </button>
              {matchOk !== null && (
                <span className={`change-pwd-match ${matchOk ? 'ok' : 'fail'}`}>
                  {matchOk ? '✓' : '✗'}
                </span>
              )}
            </div>
            {matchOk !== null && !matchOk && (
              <div className="change-pwd-hint">两次输入的密码不一致</div>
            )}

            <div className="change-pwd-actions">
              <button className="change-pwd-btn cancel" onClick={onClose}>取消</button>
              <button
                className="change-pwd-btn save"
                disabled={!canSave || saving}
                onClick={handleSave}
              >
                {saving ? '保存中...' : '保存修改'}
              </button>
            </div>
          </div>
        )}
      </div>
    </>
  );
}
