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
  mode: 'sentry',
  voice: 'en-GB-RyanNeural',
  speed: 0.75,
  showId: null as number | null,
  autoPlay: true,
  preferOriginal: false,
};

export default function SettingsPanel({ open, onClose, mode, onModeChange, voice, onVoiceChange, speed, onSpeedChange, showId, onShowChange, autoPlay, onAutoPlayChange, preferOriginal, onPreferOriginalChange }: Props) {
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
          <h3>⚙️ 设置</h3>
          <button className="close-btn" onClick={onClose}>✕ 关闭</button>
        </div>

        {/* 练习模式 */}
        <div className="settings-section">
          <label>练习模式</label>
          <div className="voice-pills">
            {['sentry', 'dictation'].map(m => (
              <label key={m} className={`mode-pill ${mode === m ? 'active' : ''}`}
                onClick={() => { onModeChange(m); save('mode', m); }}>
                <input type="radio" name="mode" checked={mode === m} readOnly />
                <span>{m === 'sentry' ? '📝 中译英听写模式' : '🖊️ 纯听写模式'}</span>
              </label>
            ))}
          </div>
        </div>

        {/* 剧集选择 */}
        <div className="settings-section">
          <label>剧集选择</label>
          <select className="show-select" value={showId ?? ''} onChange={e => onShowChange(e.target.value ? Number(e.target.value) : null)}>
            <option value="">🎬 全部剧集</option>
            {shows.map(s => <option key={s.id} value={s.id}>{s.name} ({s.sentenceCount}句)</option>)}
          </select>
        </div>

        {/* 音色 */}
        <div className="settings-section">
          <label>音色</label>
          <div className="voice-radio-group">
            <div className="voice-section-label">剧集原音</div>
            <div className="voice-pills">
              <label className="voice-pill">
                <input type="checkbox" checked={preferOriginal}
                  onChange={e => { onPreferOriginalChange(e.target.checked); save('preferOriginal', String(e.target.checked)); }} />
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

        {/* 快捷键 */}
        <div className="settings-section">
          <label>快捷键</label>
          <div style={{ fontSize: '0.8rem', color: 'var(--text-muted)', lineHeight: 2 }}>
            <kbd>-</kbd> 剧集原音 &nbsp; <kbd>=</kbd> 选中的音色 / 默认 Ryan &nbsp; <kbd>\</kbd> 下一句<br />
            <kbd>[</kbd> 中文 &nbsp; <kbd>]</kbd> 答案<br />
            <kbd>Enter</kbd> 提交
          </div>
        </div>
      </div>
    </div>
  );
}
