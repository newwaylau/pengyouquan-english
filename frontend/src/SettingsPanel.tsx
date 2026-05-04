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
  showId: number | null;
  onShowChange: (id: number | null) => void;
}

const VOICES = [
  { id: 'en-US-JennyNeural', label: 'Jenny (US Female)' },
  { id: 'en-US-GuyNeural', label: 'Guy (US Male)' },
  { id: 'en-GB-SoniaNeural', label: 'Sonia (UK Female)' },
  { id: 'en-GB-RyanNeural', label: 'Ryan (UK Male)' },
  { id: 'en-AU-NatashaNeural', label: 'Natasha (AU Female)' },
  { id: 'en-AU-WilliamNeural', label: 'William (AU Male)' },
];

export default function SettingsPanel({ open, onClose, mode, onModeChange, voice, onVoiceChange, speed, onSpeedChange, showId, onShowChange }: Props) {
  const [shows, setShows] = useState<any[]>([]);

  useEffect(() => {
    if (open) api.shows().then(r => { if (r.code === 200) setShows(r.data); });
  }, [open]);

  const save = async (key: string, value: string) => {
    await api.saveSettings({ [key]: value });
  };

  if (!open) return null;

  return (
    <div className="settings-overlay" onClick={onClose}>
      <div className="settings-panel" onClick={e => e.stopPropagation()}>
        <div className="settings-header">
          <h3>设置</h3>
          <button className="close-btn" onClick={onClose}>✕</button>
        </div>

        <div className="settings-section">
          <label>练习模式</label>
          <div className="pill-group">
            {['translation', 'dictation'].map(m => (
              <button key={m} className={`pill ${mode === m ? 'active' : ''}`}
                onClick={() => { onModeChange(m); save('mode', m); }}>
                {m === 'translation' ? '📝 中译英' : '🖊️ 听写'}
              </button>
            ))}
          </div>
        </div>

        <div className="settings-section">
          <label>剧集选择</label>
          <select value={showId ?? ''} onChange={e => onShowChange(e.target.value ? Number(e.target.value) : null)}>
            <option value="">🎬 全部剧集</option>
            {shows.map(s => <option key={s.id} value={s.id}>{s.name} ({s.sentenceCount}句)</option>)}
          </select>
        </div>

        <div className="settings-section">
          <label>音色</label>
          <div className="pill-group">
            {VOICES.map(v => (
              <button key={v.id} className={`pill ${voice === v.id ? 'active' : ''}`}
                onClick={() => { onVoiceChange(v.id); save('voice', v.id); }}>
                {v.label}
              </button>
            ))}
          </div>
        </div>

        <div className="settings-section">
          <label>播放速度</label>
          <div className="pill-group">
            {[0.5, 0.75, 1, 1.5].map(s => (
              <button key={s} className={`pill ${speed === s ? 'active' : ''}`}
                onClick={() => { onSpeedChange(s); save('speed', String(s)); }}>
                {s}x
              </button>
            ))}
          </div>
        </div>

        <div className="settings-section shortcuts">
          <label>快捷键</label>
          <div className="shortcut-list">
            <span><kbd>=</kbd> 音色播放</span>
            <span><kbd>-</kbd> 原音播放</span>
            <span><kbd>Enter</kbd> 提交</span>
            <span><kbd>\</kbd> 下一句</span>
            <span><kbd>[</kbd> 中文切换（听写模式）</span>
            <span><kbd>]</kbd> 英文切换</span>
          </div>
        </div>
      </div>
    </div>
  );
}
