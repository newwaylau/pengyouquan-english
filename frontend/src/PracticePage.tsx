import React, { useState, useEffect, useRef, useCallback } from 'react';
import SettingsPanel from './SettingsPanel';
import { api } from './api/client';

/** 从字幕文本中提取英文 */
function extractEn(text: string) { return text.includes(' / ') ? text.split(' / ')[0].replace(/^#\d+\s+/, '') : text.replace(/^#\d+\s+/, ''); }
/** 从字幕文本中提取中文 */
function extractCn(text: string) { return text.includes(' / ') ? text.split(' / ')[1] : ''; }

const VOICES = [
  { id: 'en-US-JennyNeural', label: 'Jenny' },
  { id: 'en-US-GuyNeural', label: 'Guy' },
  { id: 'en-GB-SoniaNeural', label: 'Sonia' },
  { id: 'en-GB-RyanNeural', label: 'Ryan' },
  { id: 'en-AU-NatashaNeural', label: 'Natasha' },
  { id: 'en-AU-WilliamNeural', label: 'William' },
];

export default function PracticePage({ user }: { user: any }) {
  const [sentence, setSentence] = useState<any>(null);
  const [mode, setMode] = useState<'translation' | 'dictation'>('translation');
  // 中译英模式：中文始终显示；听写模式：中文默认模糊
  const [showCn, setShowCn] = useState(true);
  const [showEn, setShowEn] = useState(false);
  const [speed, setSpeed] = useState(0.75);
  const [voice, setVoice] = useState('en-GB-RyanNeural');
  const [answered, setAnswered] = useState(false);
  const [retryCount, setRetryCount] = useState(0);
  const [revealed, setRevealed] = useState(false);
  const [words, setWords] = useState<string[]>([]);
  const [inputs, setInputs] = useState<string[]>([]);
  const [hints, setHints] = useState<Set<number>>(new Set());
  const [correctWords, setCorrectWords] = useState<Set<number>>(new Set());
  const [wrongWords, setWrongWords] = useState<Set<number>>(new Set());
  const inputRefs = useRef<(HTMLInputElement | null)[]>([]);
  const [historyIds, setHistoryIds] = useState<number[]>([]);
  const [showList, setShowList] = useState<any[]>([]);
  const [selectedShowId, setSelectedShowId] = useState<number | null>(null);
  const audioRef = useRef<HTMLAudioElement | null>(null);
  const [settingsOpen, setSettingsOpen] = useState(false);
  const [focusMode, setFocusMode] = useState(false);
  const [dailyStats, setDailyStats] = useState({ done: 0, correct: 0 });

  // 加载剧集列表
  useEffect(() => {
    api.shows().then(r => { if (r.code === 200) setShowList(r.data); });
  }, []);

  // 加载已保存的设置 + 统计
  useEffect(() => {
    if (!user) return;
    api.stats().then(r => { if (r.code === 200) setDailyStats({ done: r.data.totalPractices, correct: r.data.totalCorrect }); });
    api.getSettings().then(r => {
      if (r.code !== 200) return;
      const s = r.data;
      if (s.mode) setMode(s.mode as any);
      if (s.voice) setVoice(s.voice);
      if (s.speed) setSpeed(Number(s.speed));
      if (s.showId) setSelectedShowId(Number(s.showId));
    });
  }, [user]);

  // 加载句子
  const loadSentence = useCallback(async () => {
    const exclude = historyIds.join(',');
    let url = `limit=15&exclude=${encodeURIComponent(exclude)}`;
    if (selectedShowId) url += `&showId=${selectedShowId}`;
    const r = await api.random(url);
    if (r.code !== 200 || !r.data?.length) return;
    setSentence(r.data[0]);
    setAnswered(false);
    setRetryCount(0);
    setRevealed(false);
    setCorrectWords(new Set());
    setWrongWords(new Set());
    setShowEn(false);
    // 中译英模式：中文始终可见；听写模式：中文默认模糊
    setShowCn(mode === 'translation');

    const en = extractEn(r.data[0].text);
    const wds = en.split(/\s+/).filter(Boolean);
    setWords(wds);
    setInputs(wds.map(() => ''));

    // 撇号词预填+随机提示
    const hintsSet = new Set<number>();
    wds.forEach((w, i) => {
      if (w.includes("'") && w.length > 2) { hintsSet.add(i); }
    });
    // 随机提示一个非撇号词
    const nonHint = wds.map((_, i) => i).filter(i => !hintsSet.has(i));
    if (nonHint.length > 0) {
      hintsSet.add(nonHint[Math.floor(Math.random() * nonHint.length)]);
    }
    setHints(hintsSet);

    // 自动播放
    setTimeout(() => {
      const clean = extractEn(r.data[0].text);
      if (clean) playTts(clean);
    }, 500);
  }, [historyIds, selectedShowId]);

  useEffect(() => { loadSentence(); }, []);

  // TTS
  const playTts = (text: string) => {
    if (audioRef.current) audioRef.current.pause();
    const audio = new Audio(`/api/tts?text=${encodeURIComponent(text)}&voice=${voice}`);
    audio.playbackRate = speed;
    audio.play();
    audioRef.current = audio;
  };

  // 提交
  const handleSubmit = () => {
    const correct = new Set<number>();
    const wrong = new Set<number>();
    words.forEach((w, i) => {
      if (hints.has(i)) { correct.add(i); return; }
      if (inputs[i]?.trim().toLowerCase() === w.toLowerCase()) { correct.add(i); }
      else { wrong.add(i); }
    });
    setCorrectWords(correct);
    setWrongWords(wrong);

    if (wrong.size === 0) {
      // 全部正确 → 完成
      setAnswered(true);
      setShowEn(true);
      setShowCn(true);
      api.logPractice({ sentenceId: sentence.id, correct: true, correctCount: correct.size, totalWords: words.length, mode });
    } else if (retryCount >= 1) {
      // 第二次错误 → 显示答案
      setAnswered(true);
      setShowEn(true);
      setShowCn(true);
      setRevealed(true);
      api.logPractice({ sentenceId: sentence.id, correct: false, correctCount: correct.size, totalWords: words.length, mode });
    } else {
      // 第一次错误 → 标红重试
      setRetryCount(1);
    }
  };

  // 快捷键
  useEffect(() => {
    const handler = (e: KeyboardEvent) => {
      if (e.key === 'Enter') { handleSubmit(); return; }
      if (e.key === '=') { const clean = extractEn(sentence?.text || ''); playTts(clean); return; }
      if (e.key === '-') { /* 原音 - 后续实现 */ return; }
      if (e.key === '\\') { loadSentence(); return; }
      if (e.key === '[' && mode === 'dictation') { setShowCn(c => !c); return; }
      if (e.key === ']') { setShowEn(e => !e); return; }
    };
    window.addEventListener('keydown', handler);
    return () => window.removeEventListener('keydown', handler);
  }, [sentence, words, inputs, retryCount, mode]);

  // 输入跳转
  const handleInputChange = (i: number, val: string) => {
    const newInputs = [...inputs];
    newInputs[i] = val;
    setInputs(newInputs);
    // 输完自动跳下一框
    if (val.length >= (words[i] || '').length && i < words.length - 1) {
      inputRefs.current[i + 1]?.focus();
    }
  };
  const handleKeyDown = (i: number, e: React.KeyboardEvent) => {
    if (e.key === 'Backspace' && !inputs[i] && i > 0) {
      inputRefs.current[i - 1]?.focus();
    }
    if (e.key === ' ' && i < words.length - 1) {
      e.preventDefault();
      inputRefs.current[i + 1]?.focus();
    }
  };

  if (!sentence) return <div className="loading">加载中...</div>;

  const en = extractEn(sentence.text);
  const cn = extractCn(sentence.text);

  return (
    <div className={`practice-page ${focusMode ? 'focus-mode' : ''}`}>
      {/* 进度条 */}
      {user && (
        <div className="progress-bar">
          <span>📊 今日练习 {dailyStats.done} 次</span>
          <span className="focus-toggle" onClick={() => setFocusMode(f => !f)}>
            {focusMode ? '🎯 退出专注' : '🎯 专注'}
          </span>
        </div>
      )}
      {/* 控制栏 */}
      <div className="controls">
        <select value={mode} onChange={e => setMode(e.target.value as any)}>
          <option value="translation">📝 中译英</option>
          <option value="dictation">🖊️ 听写</option>
        </select>
        <select value={selectedShowId || ''} onChange={e => {
          const v = e.target.value ? Number(e.target.value) : null;
          setSelectedShowId(v);
          setHistoryIds([]);
          if (user) api.saveSettings({ showId: String(v || '') });
        }}>
          <option value="">🎬 全部剧集</option>
          {showList.map(s => <option key={s.id} value={s.id}>{s.name}</option>)}
        </select>
        <select value={voice} onChange={e => setVoice(e.target.value)}>
          {VOICES.map(v => <option key={v.id} value={v.id}>{v.label}</option>)}
        </select>
        <select value={speed} onChange={e => setSpeed(Number(e.target.value))}>
          {[0.5, 0.75, 1, 1.5].map(s => <option key={s} value={s}>{s}x</option>)}
        </select>
      </div>

      {/* 剧集名 */}
      <div className="sentence-meta">
        {sentence.showName} · #{sentence.id}
      </div>

      {/* 英文显示区（模糊遮住） */}
      <div className={`sentence-en ${!showEn ? 'blurred' : ''}`}>
        {en}
      </div>

      {/* 中文显示区
          中译英模式：中文始终清晰显示（不可隐藏）
          听写模式：中文默认模糊，点击可切换 */}
      {cn && (
        <div className={`sentence-cn ${mode === 'dictation' && !showCn && !answered ? 'blurred' : ''}`}>
          {cn}
        </div>
      )}

      {/* 逐词输入（未完成时显示） */}
      {!answered && (
        <div className="word-inputs">
          {words.map((w, i) => (
            <div key={i} className="word-input-wrapper">
              {hints.has(i) ? (
                <span className="hint-word">{w}</span>
              ) : (
                <input
                  ref={el => { inputRefs.current[i] = el; }}
                  className={`word-input ${correctWords.has(i) ? 'correct' : ''} ${wrongWords.has(i) ? 'wrong' : ''}`}
                  value={inputs[i]}
                  onChange={e => handleInputChange(i, e.target.value)}
                  onKeyDown={e => handleKeyDown(i, e)}
                  disabled={hints.has(i) || answered}
                  autoFocus={i === 0}
                />
              )}
            </div>
          ))}
        </div>
      )}

      {/* 设置按钮 */}
      <button className="settings-btn" onClick={() => setSettingsOpen(true)}>⚙️ 设置</button>

      {/* 操作按钮 */}
      <div className="actions">
        {!answered ? (
          <button className="btn-primary" onClick={handleSubmit}>⏎ 提交</button>
        ) : (
          <button className="btn-primary" onClick={() => { setHistoryIds(h => [...h, sentence.id]); loadSentence(); }}>⏭️ 下一句</button>
        )}
        <button onClick={() => playTts(en)}>🔊 音色</button>
        <button onClick={() => setShowEn(s => !s)}>
          {showEn ? '🙈 隐藏英文' : '👁️ 显示英文'}
        </button>
        {mode === 'dictation' && !answered && (
          <button onClick={() => setShowCn(s => !s)}>
            {showCn ? '🙈 隐藏中文' : '👁️ 显示中文'}
          </button>
        )}
      </div>

      {/* 反馈 */}
      {answered && (
        <div className={`feedback ${wrongWords.size === 0 ? 'correct' : 'wrong'}`}>
          {wrongWords.size === 0 ? '✅ 完全正确！' : `❌ 正确 ${correctWords.size}/${words.length} 个词`}
          {revealed && <div className="answer-reveal">正确答案：{en}</div>}
        </div>
      )}
      {retryCount === 1 && !answered && (
        <div className="feedback retry">⚠️ 有错误，再试一次</div>
      )}

      {/* 设置面板 */}
      <SettingsPanel
        open={settingsOpen}
        onClose={() => setSettingsOpen(false)}
        mode={mode}
        onModeChange={setMode}
        voice={voice}
        onVoiceChange={setVoice}
        speed={speed}
        onSpeedChange={setSpeed}
        showId={selectedShowId}
        onShowChange={setSelectedShowId}
      />

      {/* 快捷键提示 */}
      <div className="shortcuts-hint">
        <kbd>=</kbd> 音色 <kbd>-</kbd> 原音 <kbd>Enter</kbd> 提交 <kbd>\</kbd> 下一句
        <kbd>[</kbd> 中文 <kbd>]</kbd> 英文
      </div>
    </div>
  );
}
