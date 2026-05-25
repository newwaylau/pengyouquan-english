import React, { useEffect, useState } from 'react';
import { api } from './api/client';
import { IconSettings, IconClose, IconPen, IconFilm, IconSpeaker, IconMic } from './Icons';

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
  { id: 'en-US-JennyNeural', label: 'Jenny 美式女声' },
  { id: 'en-US-GuyNeural', label: 'Guy 美式男声' },
  { id: 'en-GB-SoniaNeural', label: 'Sonia 英式女声' },
  { id: 'en-GB-RyanNeural', label: 'Ryan 英式男声' },
  { id: 'en-AU-NatashaNeural', label: 'Natasha 澳式女声' },
  { id: 'en-AU-WilliamNeural', label: 'William 澳式男声' },
];

const DEFAULTS = {
  mode: 'dictation',
  voice: 'en-GB-RyanNeural',
  speed: 0.75,
  showId: '',
  autoPlay: true,
  preferOriginal: false,
};

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
  const [toastMsg, setToastMsg] = useState('');

  useEffect(() => {
    if (open) api.shows().then(r => {
      if (r.code === 200) {
        setShows(r.data);
        const groups = parseShowGroups(r.data);
        setShowGroups(groups);
        const token = localStorage.getItem('token');
        if (token) {
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
      }
    });
  }, [open]);

  const save = async (key: string, value: string) => {
    if (!localStorage.getItem('token')) {
      setToastMsg('请先登录才能保存设置');
      setTimeout(() => setToastMsg(''), 3000);
      return;
    }
    const r = await api.saveSettings({ [key]: value });
    if (r.code === 401) {
      setToastMsg('请先登录才能保存设置');
      setTimeout(() => setToastMsg(''), 3000);
    }
  };

  const handleShowSelect = (title: string) => {
    setSelectedShowTitle(title);
    setSelectedSeason('');
    save('selectedShow', title);
    save('selectedSeason', '');
    save('showId', '');
    if (title) {
      const ids = shows.filter(s => s.name.startsWith(title + ' ')).map((s: any) => s.id);
      if (ids.length > 0) onShowChange(ids.join(','));
      else onShowChange('');
    } else {
      onShowChange('');
    }
  };

  const handleSeasonSelect = (seasonKey: string) => {
    setSelectedSeason(seasonKey);
    save('selectedSeason', seasonKey);
    save('showId', '');
    if (seasonKey && selectedShowTitle) {
      const ids = shows.filter(s => s.name.startsWith(selectedShowTitle + ' ') && s.name.includes(' ' + seasonKey)).map((s: any) => s.id);
      if (ids.length > 0) onShowChange(ids.join(','));
      else onShowChange('');
    } else {
      onShowChange('');
    }
  };

  const handleEpisodeChange = (val: string) => {
    setInternalShowId(val);
    save('showId', val);
    if (val && selectedShowTitle && selectedSeason) {
      const ep = showGroups[selectedShowTitle]?.seasons[selectedSeason]
        ?.find((ep: any) => ep.episode === val);
      if (ep) onShowChange(String(ep.id));
      else onShowChange('');
    } else {
      onShowChange('');
    }
  };

  if (!open) return null;

  return (
    <div className="settings-overlay settings-v2-overlay" onClick={onClose}>
      {toastMsg && <div className="toast-msg">{toastMsg}</div>}
      <div className="settings-panel settings-v2-panel" onClick={e => e.stopPropagation()}>
        <header className="settings-v2-header">
          <div>
            <div className="page-eyebrow">SETTINGS</div>
            <h3><IconSettings /> 设置</h3>
          </div>
          <button className="btn btn-icon" onClick={onClose}><IconClose /></button>
        </header>

        <section className="settings-v2-section">
          <div className="cap">模式</div>
          <button className={`settings-v2-radio ${mode === 'dictation' ? 'active' : ''}`} onClick={() => { onModeChange('dictation'); save('mode', 'dictation'); }}>
            <IconPen size={16} />
            <span>听写模式</span>
            <span className="chip chip-teal">推荐</span>
          </button>
        </section>

        <section className="settings-v2-section">
          <div className="cap">剧集</div>
          <select className="select" value={selectedShowTitle} onChange={e => handleShowSelect(e.target.value)}>
            <option value="">全部剧集</option>
            {Object.keys(showGroups).sort().map(title => <option key={title} value={title}>{title}</option>)}
          </select>
          <select className="select" value={selectedSeason} onChange={e => handleSeasonSelect(e.target.value)}>
            <option value="">全部季</option>
            {Object.keys(showGroups[selectedShowTitle]?.seasons || {}).sort().map(s => <option key={s} value={s}>{s}</option>)}
          </select>
          <select className="select" value={internalShowId} onChange={e => handleEpisodeChange(e.target.value)}>
            <option value="">全部集</option>
            {[...(showGroups[selectedShowTitle]?.seasons[selectedSeason] || [])]
              .sort((a: any, b: any) => parseInt(a.episode.replace('E','')) - parseInt(b.episode.replace('E','')))
              .map((ep: any) => <option key={ep.id} value={ep.episode}>{ep.name} ({ep.sentenceCount}句)</option>)}
          </select>
        </section>

        <section className="settings-v2-section">
          <div className="cap">语音</div>
          <select className="select" value={voice} onChange={e => { onVoiceChange(e.target.value); save('voice', e.target.value); }}>
            {VOICES.map(v => <option key={v.id} value={v.id}>{v.label}</option>)}
          </select>
          <div className="seg settings-v2-speed">
            {[0.5, 0.75, 1, 1.25, 1.5].map(s => (
              <button key={s} className={speed === s ? 'active' : ''} onClick={() => { onSpeedChange(s); save('speed', String(s)); }}>{s}x</button>
            ))}
          </div>
          <label className="settings-v2-toggle-row">
            <span><IconSpeaker /> 原音优先<small>有剧集原音时优先播放原音</small></span>
            <button className={`toggle ${preferOriginal ? 'on' : ''}`} onClick={e => { e.preventDefault(); const next = !preferOriginal; onPreferOriginalChange(next); save('preferOriginal', String(next)); }} />
          </label>
          <label className="settings-v2-toggle-row">
            <span><IconMic /> 自动播放<small>切换句子后自动播放音频</small></span>
            <button className={`toggle ${autoPlay ? 'on' : ''}`} onClick={e => { e.preventDefault(); const next = !autoPlay; onAutoPlayChange(next); save('autoPlay', String(next)); }} />
          </label>
        </section>
      </div>
    </div>
  );
}
