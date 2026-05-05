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

  const save = async (key: string, value: string) => {
    const r = await api.saveSettings({ [key]: value });
    if (r.code === 401) {
      alert('请先登录才能保存设置');
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
