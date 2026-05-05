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

  // 当选择变化时，自动计算showIds
  useEffect(() => {
    if (!selectedShowTitle) { onShowChange(''); save('showId', ''); return; }
    const ids = shows
      .filter(s => s.name.startsWith(selectedShowTitle + ' '))
      .filter(s => !selectedSeason || s.name.includes(' ' + selectedSeason))
      .map((s: any) => s.id);
    if (ids.length > 0) { onShowChange(ids.join(',')); save('showId', ids.join(',')); }
  }, [selectedShowTitle, selectedSeason]);

  useEffect(() => {
    if (open) api.shows().then(r => {
      if (r.code === 200) {
        setShows(r.data);
        const groups = parseShowGroups(r.data);
        setShowGroups(groups);
        // 从已保存的showId反解析出剧集/季
        if (showId && showId !== '') {
          const ids = showId.split(',').map(Number);
          for (const titleKey of Object.keys(groups)) {
            for (const seasonKey of Object.keys(groups[titleKey].seasons)) {
              for (const ep of groups[titleKey].seasons[seasonKey]) {
                if (ids.includes(ep.id)) {
                  setSelectedShowTitle(titleKey);
                  setSelectedSeason(seasonKey);
                  return;
                }
              }
            }
          }
        }
      }
    });
  }, [open]);

  const save = async (key: string, value: string) => {
    await api.saveSettings({ [key]: value });
  };

  // 当选择变化时更新外部showId
  const handleEpisodeSelect = (ep: any) => {
    if (ep) {
      setSelectedSeason(ep.parentSeason);
      if (ep) {
        onShowChange(String(ep.id));
        save('showId', String(ep.id));
      }
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
              onChange={e => {
                setSelectedShowTitle(e.target.value);
                setSelectedSeason('');
              }}>
              <option value="">🎬 全部剧集</option>
              {Object.keys(showGroups).sort().map(title => (
                <option key={title} value={title}>{title}</option>
              ))}
            </select>
            <select className="show-select" style={{width:'100%'}}
              value={selectedSeason}
              onChange={e => {
                setSelectedSeason(e.target.value);
              }}>
              <option value="">📺 全部季</option>
              {Object.keys(showGroups[selectedShowTitle]?.seasons || {}).sort().map(s => (
                <option key={s} value={s}>{s}</option>
              ))}
            </select>
            <select className="show-select" style={{width:'100%'}}
              value={showId ?? ''}
              onChange={e => {
                const ep = showGroups[selectedShowTitle]?.seasons[selectedSeason]
                  ?.find((ep: any) => ep.id === Number(e.target.value));
                if (ep) {
                  onShowChange(String(ep.id));
                  save('showId', String(ep.id));
                }
              }}>
              <option value="">🎬 全部集</option>
              {[...(showGroups[selectedShowTitle]?.seasons[selectedSeason] || [])]
                .sort((a: any, b: any) => parseInt(a.episode.replace('E','')) - parseInt(b.episode.replace('E','')))
                .map((ep: any) => (
                <option key={ep.id} value={ep.id}>{ep.name} ({ep.sentenceCount}句)</option>
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

        {/* 快捷键 */}
        <div className="settings-section">
          <label>快捷键</label>
          <div style={{ fontSize: '0.8rem', color: 'var(--text-muted)', lineHeight: 2 }}>
            <kbd>-</kbd> 剧集原音 &nbsp; <kbd>=</kbd> 音色(默认 Ryan) &nbsp; <kbd>\</kbd> 下一句<br />
            <kbd>[</kbd> 中文 &nbsp; <kbd>]</kbd> 答案 &nbsp; <kbd>Enter</kbd> 提交
          </div>
        </div>
      </div>
    </div>
  );
}
