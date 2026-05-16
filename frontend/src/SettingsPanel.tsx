import React, { useEffect, useState } from 'react';
import { api } from './api/client';

interface Props {
  open: boolean;
  onClose: () => void;
  mode: string;
  onModeChange: (m: string) => void;
  voice: string;
  onVoiceChange: (v: string) => void;
  speed: number;
  onSpeedChange: (s: number) => void;
  showId: string;
  onShowChange: (id: string) => void;
  autoPlay: boolean;
  onAutoPlayChange: (v: boolean) => void;
  preferOriginal: boolean;
  onPreferOriginalChange: (v: boolean) => void;
}

const VOICES = [
  { id: 'en-US-JennyNeural', label: '🇺🇸 Jenny 美式女声' },
  { id: 'en-US-GuyNeural', label: '🇺🇸 Guy 美式男声' },
  { id: 'en-GB-SoniaNeural', label: '🇬🇧 Sonia 英式女声' },
  { id: 'en-GB-RyanNeural', label: '🇬🇧 Ryan 英式男声' },
  { id: 'en-AU-NatashaNeural', label: '🇦🇺 Natasha 澳式女声' },
  { id: 'en-AU-WilliamNeural', label: '🇦🇺 William 澳式男声' },
];

const DEFAULTS = {
  mode: 'translation',
  voice: 'en-GB-RyanNeural',
  speed: 0.75,
  showId: '',
  autoPlay: true,
  preferOriginal: false,
};

// 解析剧集名称为层级结构
function parseShowGroups(shows: any[]) {
  const groups: Record<string, any> = {};
  shows.forEach((s: any) => {
    const match = s.name.match(/^(.+?)\s+(S\d+)(E\d+)$/);
    const showTitle = match ? match[1] : s.name;
    const season = match ? match[2] : '';
    const episode = match ? match[3] : '';
    if (!groups[showTitle]) groups[showTitle] = { seasons: {} };
    if (!groups[showTitle].seasons[season]) groups[showTitle].seasons[season] = [];
    groups[showTitle].seasons[season].push({ ...s, episode });
  });
  return groups;
}

export default function SettingsPanel({ open, onClose, mode, onModeChange, voice, onVoiceChange, speed, onSpeedChange, showId, onShowChange, autoPlay, onAutoPlayChange, preferOriginal, onPreferOriginalChange }: Props) {
  const [shows, setShows] = useState<any[]>([]);
  const [showGroups, setShowGroups] = useState<Record<string, any>>({});
  const [selectedShowTitle, setSelectedShowTitle] = useState('');
  const [selectedSeason, setSelectedSeason] = useState('');
  const [internalShowId, setInternalShowId] = useState('');

  useEffect(() => {
    if (open) api.shows().then(r => {
      if (r.code === 200) {
        setShows(r.data);
        const groups = parseShowGroups(r.data);
        setShowGroups(groups);
        // 从已保存的selectedShow/selectedSeason回显
        api.getSettings().then(sr => {
          if (sr.code === 200) {
            const savedTitle = sr.data.selectedShow;
            const savedSeason = sr.data.selectedSeason;
            if (savedTitle && groups[savedTitle]) {
              setSelectedShowTitle(savedTitle);
              if (savedSeason) setSelectedSeason(savedSeason);
              if (sr.data.showId) setInternalShowId(sr.data.showId);
            }
          }
        });
      }
    });
  }, [open]);

  const [toastMsg, setToastMsg] = useState('');

  // 修改密码
  const [showPwdForm, setShowPwdForm] = useState(false);
  const [oldPassword, setOldPassword] = useState('');
  const [newPassword, setNewPassword] = useState('');
  const [confirmPassword, setConfirmPassword] = useState('');
  const [pwdUpdating, setPwdUpdating] = useState(false);
  const [pwdError, setPwdError] = useState('');
  const [pwdSuccess, setPwdSuccess] = useState('');

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

  const handleUpdatePassword = async () => {
    if (!oldPassword) { setPwdError('请输入当前密码'); return; }
    if (!newPassword) { setPwdError('请输入新密码'); return; }
    if (!isStrongPassword(newPassword)) { setPwdError('新密码至少8位，需包含字母和数字'); return; }
    if (newPassword !== confirmPassword) { setPwdError('两次新密码不一致'); return; }
    if (oldPassword === newPassword) { setPwdError('新密码不能与旧密码相同'); return; }
    setPwdError('');
    setPwdSuccess('');
    setPwdUpdating(true);
    const r = await api.updatePassword({ oldPassword, newPassword });
    if (r.code === 200) {
      setPwdSuccess('密码修改成功！');
      setOldPassword('');
      setNewPassword('');
      setConfirmPassword('');
      setTimeout(() => { setPwdSuccess(''); setShowPwdForm(false); }, 3000);
    } else {
      setPwdError(r.message || '修改失败，旧密码可能不正确');
    }
    setPwdUpdating(false);
  };

  const resetPwdForm = () => {
    setShowPwdForm(false);
    setOldPassword('');
    setNewPassword('');
    setConfirmPassword('');
    setPwdError('');
    setPwdSuccess('');
  };

  const save = async (key: string, value: string) => {
    const r = await api.saveSettings({ [key]: value });
    if (r.code === 401) {
      setToastMsg('请先登录才能保存设置');
      setTimeout(() => setToastMsg(''), 3000);
    }
  };

  // 选剧集时
  const handleShowSelect = (title: string) => {
    setSelectedShowTitle(title);
    setSelectedSeason('');
    save('selectedShow', title);
    save('selectedSeason', '');
    save('showId', '');
    // 传所有匹配的数据库ID
    if (title) {
      const ids = shows.filter(s => s.name.startsWith(title + ' ')).map((s: any) => s.id);
      if (ids.length > 0) onShowChange(ids.join(','));
      else onShowChange('');
    } else {
      onShowChange('');
    }
  };

  // 选季时
  const handleSeasonSelect = (seasonKey: string) => {
    setSelectedSeason(seasonKey);
    save('selectedSeason', seasonKey);
    save('showId', '');
    // 传该季所有集的数据库ID
    if (seasonKey && selectedShowTitle) {
      const ids = shows.filter(s => s.name.startsWith(selectedShowTitle + ' ') && s.name.includes(' ' + seasonKey)).map((s: any) => s.id);
      if (ids.length > 0) onShowChange(ids.join(','));
      else onShowChange('');
    } else {
      onShowChange('');
    }
  };

  // 选具体集时
  // 集下拉框选中时
  const handleEpisodeChange = (val: string) => {
    setInternalShowId(val);
    save('showId', val);
    // 传数据库ID到外部（用于过滤句子）
    if (val && selectedShowTitle && selectedSeason) {
      const ep = showGroups[selectedShowTitle]?.seasons[selectedSeason]
        ?.find((ep: any) => ep.episode === val);
      if (ep) onShowChange(String(ep.id));
      else onShowChange('');
    } else {
      onShowChange('');
    }
  };

  // 选剧集/季时，计算所有匹配的showIds
  const handleShowOrSeasonChange = () => {
    if (!selectedShowTitle) {
      onShowChange('');
      save('showId', '');
      return;
    }
    const ids = shows
      .filter(s => s.name.startsWith(selectedShowTitle + ' '))
      .filter(s => !selectedSeason || s.name.includes(' ' + selectedSeason))
      .map(s => s.id);
    if (ids.length > 0) {
      onShowChange(ids.join(','));
      save('showId', ids.join(','));
    }
  };

  if (!open) return null;

  return (
    <div className="settings-overlay" onClick={onClose}>
      {toastMsg && <div className="toast-msg">{toastMsg}</div>}
      <div className="settings-panel" onClick={e => e.stopPropagation()}>
        <div className="settings-header">
          <h3>⚙️ 设置</h3>
          <button className="close-btn" onClick={onClose}>✕ 关闭</button>
        </div>

        {/* 练习模式 */}
        <div className="settings-section">
          <label>练习模式</label>
          <div className="voice-pills">
            {['translation', 'dictation'].map(m => (
              <label key={m} className={`mode-pill ${mode === m ? 'active' : ''}`}
                onClick={() => { onModeChange(m); save('mode', m); }}>
                <input type="radio" name="mode" checked={mode === m} readOnly />
                <span>{m === 'translation' ? '📝 中译英' : '🖊️ 听写'}</span>
              </label>
            ))}
          </div>
        </div>

        {/* 剧集选择（三级联动） */}
        <div className="settings-section">
          <label>剧集选择</label>
          <div style={{display:'flex',flexDirection:'column',gap:4}}>
            <select className="show-select" style={{width:'100%'}}
              value={selectedShowTitle}
              onChange={e => { handleShowSelect(e.target.value); }}>
              <option value="">🎬 全部剧集</option>
              {Object.keys(showGroups).sort().map(title => (
                <option key={title} value={title}>{title}</option>
              ))}
            </select>
            <select className="show-select" style={{width:'100%'}}
              value={selectedSeason}
              onChange={e => { handleSeasonSelect(e.target.value); }}>
              <option value="">📺 全部季</option>
              {Object.keys(showGroups[selectedShowTitle]?.seasons || {}).sort().map(s => (
                <option key={s} value={s}>{s}</option>
              ))}
            </select>
            <select className="show-select" style={{width:'100%'}}
              value={internalShowId}
              onChange={e => { handleEpisodeChange(e.target.value); }}>
              <option value="">🎬 全部集</option>
              {[...(showGroups[selectedShowTitle]?.seasons[selectedSeason] || [])]
                .sort((a: any, b: any) => parseInt(a.episode.replace('E','')) - parseInt(b.episode.replace('E','')))
                .map((ep: any) => (
                <option key={ep.id} value={ep.episode}>{ep.name} ({ep.sentenceCount}句)</option>
              ))}
            </select>
          </div>
        </div>

        {/* 音色 */}
        <div className="settings-section">
          <label>音色</label>
          <div className="voice-radio-group">
            <div className="voice-section-label">剧集原音</div>
            <div className="voice-pills">
              <label className="voice-pill">
                <input type="checkbox" checked={true} readOnly />
                <span>默认</span>
              </label>
            </div>
            <hr className="voice-radio-divider" />
            <div className="voice-section-label">导播</div>
            <div className="voice-pills voice-pills-grid">
              {VOICES.map(v => (
                <label key={v.id} className="voice-pill">
                  <input type="radio" name="voice" checked={voice === v.id}
                    onChange={() => { onVoiceChange(v.id); save('voice', v.id); }} />
                  <span>{v.label}</span>
                </label>
              ))}
            </div>
          </div>
        </div>

        {/* 自动播放 */}
        <div className="settings-section">
          <label>自动播放（建议选择导播）</label>
          <div className="voice-pills">
            {['browser', 'server'].map(val => (
              <label key={val} className="voice-pill">
                <input type="radio" name="autoPlay"
                  checked={val === 'browser' ? preferOriginal : !preferOriginal}
                  onChange={() => {
                    if (val === 'browser') { onPreferOriginalChange(true); save('preferOriginal', 'true'); }
                    else { onPreferOriginalChange(false); save('preferOriginal', 'false'); }
                  }} />
                <span>{val === 'browser' ? '🔊 剧集原音' : '🎙️ 导播'}</span>
              </label>
            ))}
          </div>
        </div>

        {/* 播放速度 */}
        <div className="settings-section">
          <label>播放速度</label>
          <div className="voice-pills">
            {[0.5, 0.75, 1, 1.5].map(s => (
              <button key={s} className={`btn-sm speed-btn ${speed === s ? 'speed-active' : ''}`}
                onClick={() => { onSpeedChange(s); save('speed', String(s)); }}>
                {s}x
              </button>
            ))}
          </div>
        </div>

        {/* ── 账户安全 ── */}
        <div className="settings-section">
          <label>账户安全</label>
          {!showPwdForm ? (
            <button className="btn-sm" onClick={() => setShowPwdForm(true)}
              style={{ marginTop: 4 }}>
              🔑 修改密码
            </button>
          ) : (
            <div className="password-change-form" style={{ marginTop: 8, display: 'flex', flexDirection: 'column', gap: 8 }}>
              <input type="password" placeholder="当前密码" value={oldPassword}
                onChange={e => { setOldPassword(e.target.value); setPwdError(''); setPwdSuccess(''); }}
                onKeyDown={e => { if (e.key === ' ') e.preventDefault(); }} />
              <input type="password" placeholder="新密码（至少8位，含字母和数字）" value={newPassword}
                onChange={e => { setNewPassword(e.target.value); setPwdError(''); setPwdSuccess(''); }}
                onKeyDown={e => { if (e.key === ' ') e.preventDefault(); }} />
              {newPassword && pwdLevel(newPassword).percent > 0 && (
                <div style={{ display: 'flex', alignItems: 'center', gap: 8, fontSize: 12 }}>
                  <div style={{ flex: 1, height: 4, borderRadius: 2, background: '#eee' }}>
                    <div style={{ width: `${pwdLevel(newPassword).percent}%`, height: '100%', borderRadius: 2,
                      background: pwdLevel(newPassword).color, transition: 'width .2s' }} />
                  </div>
                  <span style={{ color: pwdLevel(newPassword).color, fontWeight: 500 }}>{pwdLevel(newPassword).label}</span>
                </div>
              )}
              <input type="password" placeholder="确认新密码" value={confirmPassword}
                onChange={e => { setConfirmPassword(e.target.value); setPwdError(''); setPwdSuccess(''); }}
                onKeyDown={e => { if (e.key === ' ') e.preventDefault(); }}
                style={confirmPassword && newPassword !== confirmPassword ? { borderColor: '#e17055' } : {}} />
              {confirmPassword && newPassword !== confirmPassword && (
                <div style={{ color: '#e17055', fontSize: 12 }}>两次密码不一致</div>
              )}
              {pwdError && <div style={{ color: '#e17055', fontSize: 13 }}>{pwdError}</div>}
              {pwdSuccess && <div style={{ color: '#00b894', fontSize: 13, fontWeight: 500 }}>{pwdSuccess}</div>}
              <div style={{ display: 'flex', gap: 8 }}>
                <button className="btn-sm btn-primary" onClick={handleUpdatePassword} disabled={pwdUpdating}
                  style={{ flex: 1 }}>
                  {pwdUpdating ? '保存中...' : '💾 保存密码'}
                </button>
                <button className="btn-sm" onClick={resetPwdForm}>取消</button>
              </div>
            </div>
          )}
        </div>

      </div>
    </div>
  );
}
