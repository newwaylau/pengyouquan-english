import React, { useState, useEffect, useRef, useCallback } from 'react';
import SettingsPanel from './SettingsPanel';
import { api } from './api/client';
import { getAudioUrl, getTtsUrl } from './audioBase';

/** 从字幕文本中提取英文（含大小写校正） */
function extractEn(text: string) {
  const raw = text.includes(' / ') ? text.split(' / ')[0].replace(/^#\d+\s+/, '') : text.replace(/^#\d+\s+/, '');
  return normalizeCase(raw);
}
/** 从字幕文本中提取中文 */
function extractCn(text: string) { return text.includes(' / ') ? text.split(' / ')[1] : ''; }

/** 把单词拆成字母+前后标点（撇号词不拆分，保持完整） */
function splitWordParts(w: string): { letters: string; prefix: string; suffix: string } {
  // 带撇号的词（I'm, don't）和连字符词（blue-eyed）保持完整，不拆分
  if (w.includes("'") || w.includes('-')) return { letters: w, prefix: '', suffix: '' };
  const m = w.match(/^([^a-zA-Z]*)([a-zA-Z]+)(.*)$/);
  if (!m) return { letters: w, prefix: '', suffix: '' };
  return { letters: m[2], prefix: m[1], suffix: m[3] };
}

/** 校正大小写（和老版5000项目一致） */
function normalizeCase(s: string) {
  const words = s.split(/\s+/).filter(Boolean);
  if (words.length === 0) return s;
  const upperCount = words.filter(w => w.length > 0 && w[0] === w[0].toUpperCase() && w[0] !== w[0].toLowerCase()).length;
  const isTitleCase = upperCount >= words.length * 0.6;
  if (!isTitleCase) return s;
  return words.map((w, i) => {
    if (!w) return w;
    if (/^I(?:'[mvd]|'ll)?$/i.test(w)) return w[0].toUpperCase() + w.slice(1).toLowerCase();
    if (i === 0) return w[0].toUpperCase() + w.slice(1).toLowerCase();
    return w.toLowerCase();
  }).join(' ');
}
/** 模式中文名 */
const MODE_LABELS: Record<string, string> = {
  translation: '📝 中译英模式',
  dictation: '🖊️ 纯听写模式',
  wrong: '❌ 错题复习模式',
};

const VOICES = [
  { id: 'en-US-JennyNeural', label: 'Jenny' },
  { id: 'en-US-GuyNeural', label: 'Guy' },
  { id: 'en-GB-SoniaNeural', label: 'Sonia' },
  { id: 'en-GB-RyanNeural', label: 'Ryan' },
  { id: 'en-AU-NatashaNeural', label: 'Natasha' },
  { id: 'en-AU-WilliamNeural', label: 'William' },
];

const SPEEDS = [0.5, 0.75, 1, 1.5];

export default function PracticePage({
  user,
  jumpId,
  onNavigate,
}: {
  user: any;
  jumpId?: number | null;
  onNavigate?: (page: string, data?: any) => void;
}) {
  // 句子
  const [sentence, setSentence] = useState<any>(null);
  const [mode, setMode] = useState<'translation' | 'dictation' | 'wrong'>('translation');
  // 错题练习相关
  const [wrongSentences, setWrongSentences] = useState<any[]>([]);
  const [wrongIndex, setWrongIndex] = useState(0);
  const [showCn, setShowCn] = useState(false);
  const [showEn, setShowEn] = useState(false);
  const [speed, setSpeed] = useState(0.75);
  const [voice, setVoice] = useState('en-GB-RyanNeural');
  const [answered, setAnswered] = useState(false);
  const [retryCount, setRetryCount] = useState(0);
  // 举报状态
  const [flaggedSentenceIds, setFlaggedSentenceIds] = useState<Set<number>>(new Set());
  const [flagLoading, setFlagLoading] = useState(false);
  const [revealed, setRevealed] = useState(false);
  const [words, setWords] = useState<string[]>([]);
  const [inputs, setInputs] = useState<string[]>([]);
  const [hints, setHints] = useState<Set<number>>(new Set());
  const [correctWords, setCorrectWords] = useState<Set<number>>(new Set());
  const [wrongWords, setWrongWords] = useState<Set<number>>(new Set());
  const inputRefs = useRef<(HTMLInputElement | null)[]>([]);
  const hiddenInputRef = useRef<HTMLInputElement | null>(null);
  const [historyIds, setHistoryIds] = useState<number[]>(
    () => JSON.parse(localStorage.getItem('historyIds') || '[]')
  );
  const [showList, setShowList] = useState<any[]>([]);
  const [showIdsParam, setShowIdsParam] = useState<string>('');
  const audioRef = useRef<HTMLAudioElement | null>(null);
  const submitRef = useRef<HTMLButtonElement | null>(null);
  const [settingsOpen, setSettingsOpen] = useState(false);
  const [searchOpen, setSearchOpen] = useState(false);
  const [focusMode, setFocusMode] = useState(false);
  const [phoneMode, setPhoneMode] = useState(false);
  // 手机模式锁住页面滚动（iOS 键盘弹出时不滑动）
  useEffect(() => {
    if (phoneMode) {
      document.body.style.overflow = 'hidden';
      document.body.style.position = 'fixed';
      document.body.style.width = '100%';
      document.body.style.top = '0';
      document.body.style.left = '0';
    } else {
      document.body.style.overflow = '';
      document.body.style.position = '';
      document.body.style.width = '';
      document.body.style.top = '';
      document.body.style.left = '';
    }
    return () => {
      document.body.style.overflow = '';
      document.body.style.position = '';
      document.body.style.width = '';
      document.body.style.top = '';
      document.body.style.left = '';
    };
  }, [phoneMode]);
  const [autoPlay, setAutoPlay] = useState(true);
  const [preferOriginal, setPreferOriginal] = useState(false);
  const jumpDoneRef = useRef(false);
  const preferOriginalRef = useRef(preferOriginal);
  const speedRef = useRef(speed);
  const voiceRef = useRef(voice);
  // 句子队列（API 一次返回 limit=15，逐个消费，预加载下一句 TTS）
  const [sentenceQueue, setSentenceQueue] = useState<any[]>([]);
  const [queueIndex, setQueueIndex] = useState(0);

  // 统计
  const [stats, setStats] = useState({ totalPractices: 0, totalCorrect: 0 });
  const [wrongCount, setWrongCount] = useState(0);
  // 本次错题复习的正确统计
  const [wrongReviewStats, setWrongReviewStats] = useState({ correct: 0, total: 0 });

  // 刷新统计
  const refreshStats = () => {
    if (!user) return;
    api.stats().then(r => {
      if (r.code === 200) setStats({ totalPractices: r.data.totalPractices, totalCorrect: r.data.totalCorrect });
    });
    api.wrongSentenceStats().then(r => { if (r.code === 200) setWrongCount(r.data.due + r.data.upcoming); });
  };

  const [toastMsg, setToastMsg] = useState('');
  // 错题再练提示
  const [wrongBookPrompt, setWrongBookPrompt] = useState<{ sentenceId: number; errorCount: number; newErrorCount: number } | null>(null);

  // 检查是否登录
  const checkLogin = () => {
    if (!user) {
      setToastMsg('请先登录才能查看');
      setTimeout(() => setToastMsg(''), 3000);
      return false;
    }
    return true;
  };

  // 搜索
  const [searchQuery, setSearchQuery] = useState('');
  const [searchResults, setSearchResults] = useState<any[]>([]);
  const searchTimer = useRef<ReturnType<typeof setTimeout>>();

  // 加载剧集列表
  useEffect(() => {
    api.shows().then(r => { if (r.code === 200) setShowList(r.data); });
  }, []);

  // 加载已保存的设置 + 统计
  useEffect(() => {
    if (!user) return;
    api.stats().then(r => {
      if (r.code === 200) setStats({ totalPractices: r.data.totalPractices, totalCorrect: r.data.totalCorrect });
    });
    api.wrongSentenceStats().then(r => { if (r.code === 200) setWrongCount(r.data.due + r.data.upcoming); });
    api.getSettings().then(r => {
      if (r.code !== 200) return;
      const s = r.data;
      if (s.mode) setMode(s.mode as any);
      if (s.voice) setVoice(s.voice);
      if (s.autoPlay !== undefined) setAutoPlay(s.autoPlay === 'true');
      if (s.speed) setSpeed(Number(s.speed));
      if (s.preferOriginal !== undefined) setPreferOriginal(s.preferOriginal === 'true');
    });
  }, [user]);

  // 持久化历史到 localStorage（保留最近200条）
  useEffect(() => {
    localStorage.setItem('historyIds', JSON.stringify(historyIds.slice(-200)));
  }, [historyIds]);

  // 从showId转换到数据库ID（showList加载完成后）
  // 页面加载时先从showId转数据库ID，再加载句子
  useEffect(() => {
    if (showList.length === 0 || !user) return;
    (async () => {
      const r = await api.getSettings();
      if (r.code !== 200) return;
      const s = r.data;
      let id = '';
      if (s.showId && s.selectedShow && s.selectedSeason) {
        const found = showList.find((sh: any) => {
          const m = sh.name.match(/^(.+?)\s+(S\d+)(E\d+)$/);
          return m && m[1] === s.selectedShow && m[2] === s.selectedSeason && m[3] === s.showId;
        });
        if (found) id = String(found.id);
      }
      setShowIdsParam(id);
      setHistoryIds([]);
    })();
  }, [showList, user]);

  // 加载句子
  const loadSentenceRef = useRef<((specificId?: number) => Promise<void>) | null>(null);
  const loadSentence = async (specificId?: number, skipAutoPlay?: boolean) => {
    // 错题练习模式
    if (mode === 'wrong') {
      const r = await api.wrongPractice(20);
      if (r.code !== 200) return;
      const sentences = r.data?.sentences || [];
      if (sentences.length === 0) return;
      setWrongSentences(sentences);
      setWrongIndex(0);
      if (specificId) {
        // 如果传了 specificId，找到它在列表里的索引
        const idx = sentences.findIndex((s: any) => s.sentenceId === specificId);
        if (idx >= 0) {
          setWrongIndex(idx);
          setupSentence(sentences[idx], skipAutoPlay);
          return;
        }
      }
      setupSentence(sentences[0], skipAutoPlay);
      return;
    }

    if (specificId) {
      const r = await api.sentence(specificId);
      if (r.code !== 200 || !r.data) return;
      setSentenceQueue([r.data]);
      setQueueIndex(0);
      setupSentence(r.data, skipAutoPlay);
      return;
    }
    const exclude = historyIds.join(',');
    let url = `limit=15&exclude=${encodeURIComponent(exclude)}`;
    if (showIdsParam) url += `&showIds=${showIdsParam}`;
    const r = await api.random(url);
    if (r.code !== 200 || !r.data?.length) return;
    setSentenceQueue(r.data);
    setQueueIndex(0);
    setupSentence(r.data[0], skipAutoPlay);

    // 预加载下一句 TTS（用户练当前句时触发后端生成，不影响用户体验）
    if (r.data.length > 1) {
      const next = r.data[1];
      const nextEn = extractEn(next.text);
      if (nextEn && (!preferOriginal || !next.audioFile)) {
        fetch(getTtsUrl(nextEn, voice)).catch(() => {});
      }
    }
  };
  loadSentenceRef.current = loadSentence;

  // 处理跳转ID
  useEffect(() => {
    if (jumpId && !jumpDoneRef.current) {
      jumpDoneRef.current = true;
      loadSentenceRef.current?.(jumpId);
    }
  }, [jumpId]);

  // 第一次加载（等showList加载完后通过另一个useEffect触发）
  // 切换剧集时重新加载句子（不自动播放音频）
  // showIdsParam变化时加载句子（初始''不加载，避免随机）
  useEffect(() => {
    if (showIdsParam) {
      setHistoryIds([]);
      loadSentence(undefined, true);
    }
  }, [showIdsParam]);

  // 首次加载句子（登录/未登录都触发）
  const initialLoadRef = useRef(false);
  useEffect(() => {
    if (initialLoadRef.current) return;
    if (showList.length === 0) return;
    initialLoadRef.current = true;
    (async () => {
      if (user) {
        const r = await api.getSettings();
        if (r.code === 200) {
          const s = r.data;
          if (s.showId && s.selectedShow && s.selectedSeason) {
            const found = showList.find((sh: any) => {
              const m = sh.name.match(/^(.+?)\s+(S\d+)(E\d+)$/);
              return m && m[1] === s.selectedShow && m[2] === s.selectedSeason && m[3] === s.showId;
            });
            if (found) { setShowIdsParam(String(found.id)); return; }
          }
        }
      }
      // 未登录或没有保存的设置，加载随机句子
      loadSentence(undefined, true);
    })();
  }, [showList]);

  // 切换模式时更新中文显示状态
  useEffect(() => {
    setShowCn(mode === 'translation');
  }, [mode]);

  // 新句子加载后把焦点从隐藏输入框移到第一个单词输入框（手机键盘保持弹出）
  useEffect(() => {
    if (!sentence || words.length === 0) return;
    // 用 RAF 确保 DOM 已完成渲染
    const raf = requestAnimationFrame(() => {
      let first = 0;
      while (first < words.length && hints.has(first)) first++;
      const el = inputRefs.current[first];
      if (el && document.activeElement !== el) {
        el.focus();
      }
    });
    return () => cancelAnimationFrame(raf);
  }, [sentence]);

  // 提交报错后把焦点从隐藏输入框移到第一个单词输入框
  useEffect(() => {
    if (retryCount !== 1) return;
    const raf = requestAnimationFrame(() => {
      let first = 0;
      while (first < words.length && hints.has(first)) first++;
      const el = inputRefs.current[first];
      if (el && document.activeElement !== el) {
        el.focus();
      }
    });
    return () => cancelAnimationFrame(raf);
  }, [retryCount]);

  // 进入错题模式时加载错题句子
  useEffect(() => {
    if (mode === 'wrong' && user) {
      loadSentence();
    }
  }, [mode, user]);

  // 从 WrongPage 批量练习入口进入时自动切换错题模式
  useEffect(() => {
    const practiceMode = localStorage.getItem('practiceMode');
    if (practiceMode === 'wrong') {
      localStorage.removeItem('practiceMode');
      setMode('wrong');
      // 已有 showList 时直接加载
      if (showList.length > 0 && user) {
        loadSentence();
      }
    }
  }, [showList, user]);

  /** 加载错题句子（下一题） */
  const loadWrongNext = () => {
    if (wrongSentences.length === 0) return;
    const nextIdx = wrongIndex + 1;
    if (nextIdx >= wrongSentences.length) {
      // 没有更多错题了，显示结束状态
      setSentence(null);
      return;
    }
    setWrongIndex(nextIdx);
    setupSentence(wrongSentences[nextIdx], false);
  };

  // 设置句子
  const setupSentence = (s: any, skipAutoPlay?: boolean) => {
    setSentence(s);
    setAnswered(false);
    setRetryCount(0);
    setRevealed(false);
    setCorrectWords(new Set());
    setWrongWords(new Set());
    setShowEn(false);
    // 中译英模式：中文始终可见；听写模式：中文默认模糊
    setShowCn(mode === 'translation');

    const en = extractEn(s.text);
    const wds = en.split(/\s+/).filter(Boolean);
    setWords(wds);
    setInputs(wds.map(() => ''));

    // 撇号词/连字符词预填 + 随机提示
    const hintsSet = new Set<number>();
    wds.forEach((w, i) => {
      if ((w.includes("'") || w.includes('-')) && w.length > 2) hintsSet.add(i);
    });
    const nonHint = wds.map((_, i) => i).filter(i => !hintsSet.has(i));
    if (nonHint.length > 0) {
      hintsSet.add(nonHint[Math.floor(Math.random() * nonHint.length)]);
    }
    setHints(hintsSet);

    // 自动播放（下一句时触发，切剧集时不触发）
    if (!skipAutoPlay) {
      preferOriginalRef.current = preferOriginal;
      speedRef.current = speed;
      voiceRef.current = voice;
      setTimeout(() => {
        if (preferOriginalRef.current && s.audioFile) {
          if (audioRef.current) audioRef.current.pause();
          const a = new Audio(getAudioUrl('/api/audio/' + encodeURIComponent(s.audioFile)));
          a.playbackRate = speedRef.current;
          a.play().catch(() => {});
          audioRef.current = a;
        } else {
          const clean = extractEn(s.text);
          if (clean) playTts(clean);
        }
      }, 500);
    }
  };

  // TTS
  const playTts = (text: string) => {
    if (audioRef.current) audioRef.current.pause();
    const audio = new Audio(getTtsUrl(text, voice));
    audio.playbackRate = speed;
    audio.play().catch(() => {});
    audioRef.current = audio;
  };

  // 播放原音
  const playOriginal = () => {
    if (!sentence?.audioFile) { playTts(extractEn(sentence?.text || '')); return; }
    if (audioRef.current) audioRef.current.pause();
    const a = new Audio(getAudioUrl('/api/audio/' + encodeURIComponent(sentence.audioFile)));
    a.playbackRate = speed;
    a.play().catch(() => {});
    audioRef.current = a;
  };

  // 提交
  const handleSubmit = (inputOverrides?: string[]) => {
    const currentInputs = inputOverrides || inputs;
    const correct = new Set<number>();
    const wrong = new Set<number>();
    words.forEach((w, i) => {
      if (hints.has(i)) { correct.add(i); return; }
      const cleanWord = w.replace(/[^\w]/g, '').toLowerCase();
      if (currentInputs[i]?.trim().toLowerCase() === cleanWord) correct.add(i);
      else wrong.add(i);
    });
    const userInputCount = words.filter((_, i) => !hints.has(i)).length;
    const userCorrectCount = words.filter((_, i) => !hints.has(i) && correct.has(i)).length;
    setCorrectWords(correct);
    setWrongWords(wrong);

    if (wrong.size === 0) {
      setAnswered(true);
      setShowEn(true);
      setShowCn(true);
      // 调用练习日志
      api.logPractice({ sentenceId: sentence.id, correct: true, correctCount: userCorrectCount, totalWords: userInputCount, mode }).then(r => {
        // 错题模式下答对：更新间隔复习 + 自动下一题
        if (mode === 'wrong' && r.data?.inWrongBook) {
          // 更新间隔复习（SM-2）
          api.updateWrongReview({ sentenceId: sentence.id, correct: true }).then(() => {
            setWrongReviewStats(prev => ({ correct: prev.correct + 1, total: prev.total + 1 }));
            refreshStats();
            // 如果 review_count >= 5，会自动标记已掌握并从下次错题练习移除
            // 立即从列表移除
            setWrongSentences(prev => prev.filter(s => s.sentenceId !== sentence.id));
            // 自动加载下一题
            setTimeout(() => goNext(), 600);
          });
        } else if (r.data?.inWrongBook) {
          // 普通模式下错题答对，显示再练提示
          setWrongBookPrompt({
            sentenceId: sentence.id,
            errorCount: r.data.errorCount,
            newErrorCount: r.data.newErrorCount,
          });
          refreshStats();
        } else {
          refreshStats();
        }
      });
    } else if (retryCount >= 1) {
      setAnswered(true);
      setShowEn(true);
      setShowCn(true);
      api.logPractice({ sentenceId: sentence.id, correct: false, correctCount: userCorrectCount, totalWords: userInputCount, mode });
      // 错题模式下答错：重置间隔复习
      if (mode === 'wrong') {
        api.updateWrongReview({ sentenceId: sentence.id, correct: false });
        setWrongReviewStats(prev => ({ correct: prev.correct, total: prev.total + 1 }));
        // 不过这里只是更新间隔，句子还在错题本，但需要从本次复习列表中移除
        setWrongSentences(prev => prev.filter(s => s.sentenceId !== sentence.id));
        setTimeout(() => goNext(), 600);
      }
      refreshStats();
    } else {
      setRetryCount(1);
      // 清空错误输入 + 聚焦到第一个错误框
      const firstWrongIdx = Array.from(wrong).sort()[0];
      const clearedInputs = [...currentInputs];
      wrong.forEach(idx => { clearedInputs[idx] = ''; });
      setInputs(clearedInputs);
      if (firstWrongIdx !== undefined) {
        // 先聚焦隐藏输入框（在用户手势上下文中，确保手机键盘弹出）
        // 下一句场景已证实此方式有效
        hiddenInputRef.current?.focus();
      }
    }
  };

  // 再练一遍（同一句重新练习）
  const retryWrongSentence = () => {
    if (!wrongBookPrompt) return;
    setWrongBookPrompt(null);
    // 重置回答状态，保持当前句子不变
    setAnswered(false);
    setRetryCount(0);
    setRevealed(false);
    setCorrectWords(new Set());
    setWrongWords(new Set());
    setShowEn(false);
    setInputs(words.map(() => ''));
    // 重置提示（撇号词+随机提示保持不变，已在hints中）
    let first = 0;
  };

  // 跳过再练，继续下一句
  const skipRetry = () => {
    setWrongBookPrompt(null);
    goNext();
  };

  // 下一句
  const goNext = () => {
    if (mode === 'wrong') {
      loadWrongNext();
      return;
    }

    const nextIdx = queueIndex + 1;
    if (nextIdx < sentenceQueue.length) {
      // 从队列取下一句（TTS 已预先在后台生成）
      setQueueIndex(nextIdx);
      setupSentence(sentenceQueue[nextIdx], false);
      setHistoryIds(h => [...h, sentenceQueue[nextIdx].id]);

      // 预加载再下一句
      if (nextIdx + 1 < sentenceQueue.length) {
        const next2 = sentenceQueue[nextIdx + 1];
        const nextEn = extractEn(next2.text);
        if (nextEn && (!preferOriginal || !next2.audioFile)) {
          fetch(getTtsUrl(nextEn, voice)).catch(() => {});
        }
      }
      return;
    }

    // 队列用完，加载新一批
    hiddenInputRef.current?.focus();
    setHistoryIds(h => [...h, sentence.id]);
    loadSentence();
  };

  // 举报/取消举报句子
  const toggleFlag = async () => {
    if (!user || !sentence?.id || flagLoading || !answered) return;
    setFlagLoading(true);
    const isFlagged = flaggedSentenceIds.has(sentence.id);
    const r = await api.flagSentence(sentence.id, !isFlagged);
    setFlagLoading(false);
    if (r.code === 200) {
      if (isFlagged) {
        setFlaggedSentenceIds(prev => { const s = new Set(prev); s.delete(sentence.id); return s; });
      } else {
        setFlaggedSentenceIds(prev => new Set(prev).add(sentence.id));
      }
    }
  };

  // 快捷键
  // 快捷键（在输入框内按快捷键不会输入字符）
  useEffect(() => {
    const handler = (e: KeyboardEvent) => {
      if (e.key === 'Enter') { e.preventDefault(); if (answered) goNext(); else handleSubmit(); return; }
      if (e.key === '=') { e.preventDefault(); const clean = extractEn(sentence?.text || ''); playTts(clean); return; }
      if (e.key === '-') { e.preventDefault(); playOriginal(); return; }
      if (e.key === '\\') { e.preventDefault(); goNext(); return; }
      if (e.key === '[') {
        e.preventDefault();
        if (mode === 'dictation') setShowCn(c => !c);
        return;
      }
      if (e.key === ']') { e.preventDefault(); setShowEn(e => !e); return; }
    };
    window.addEventListener('keydown', handler);
    return () => window.removeEventListener('keydown', handler);
  }, [sentence, words, inputs, retryCount, mode, answered, goNext]);

  // 输入跳转
  const handleInputChange = (i: number, val: string) => {
    const newInputs = [...inputs];
    newInputs[i] = val;
    setInputs(newInputs);
    // 标点/撇号在输入框外，只用字母长度判断是否跳转
    const parts = splitWordParts(words[i] || '');
    if (val.length >= parts.letters.length) {
      if (i < words.length - 1) {
        // 跳过后面的提示/已正确输入框，跳到下一个需要输入的
        let next = i + 1;
        while (next < words.length && (hints.has(next) || (retryCount === 1 && newInputs[next]?.length > 0))) next++;
        if (next < words.length) inputRefs.current[next]?.focus();
        else submitRef.current?.focus();
      } else {
        // 最后一个词输入完，跳到提交按钮
        submitRef.current?.focus();
      }
    }
  };
  const handleKeyDown = (i: number, e: React.KeyboardEvent) => {
    if (e.key === 'Backspace' && !inputs[i] && i > 0) {
      // 跳到前一个需要输入的框（retry时跳过已正确的）
      let prev = i - 1;
      while (prev > 0 && (hints.has(prev) || (retryCount === 1 && !wrongWords.has(prev) && inputs[prev]?.length > 0))) prev--;
      inputRefs.current[prev]?.focus();
    }
    if (e.key === ' ' && i < words.length - 1) {
      e.preventDefault();
      inputRefs.current[i + 1]?.focus();
    }
  };

  // 搜索
  const handleSearch = (q: string) => {
    setSearchQuery(q);
    clearTimeout(searchTimer.current);
    if (q.length < 2) { setSearchResults([]); return; }
    searchTimer.current = setTimeout(async () => {
      const r = await api.search(q);
      if (r.code === 200) setSearchResults(r.data || []);
    }, 300);
  };

  // 从搜索结果跳转练习
  const jumpToSearchResult = (id: number) => {
    setSearchOpen(false);
    setSearchQuery('');
    setSearchResults([]);
    loadSentence(id);
  };

  // 计算正确率
  const accuracy = stats.totalPractices > 0
    ? Math.round((stats.totalCorrect / stats.totalPractices) * 100)
    : 0;

  // 骨架屏 / 错题练习已完成
  if (!sentence) {
    // 错题练习完成状态
    if (mode === 'wrong' && wrongSentences.length > 0) {
      return (
        <div className="practice-page">
          {toastMsg && <div className="toast-msg">{toastMsg}</div>}
          {user && (
            <div className="stats-bar">
              <div className="stat-card">
                <span className="stat-value">{stats.totalPractices}</span>
                <span className="stat-label">总句子</span>
              </div>
              <div className="stat-card">
                <span className="stat-value">{stats.totalPractices}</span>
                <span className="stat-label">今日练习</span>
              </div>
              <div className="stat-card">
                <span className="stat-value">{accuracy}%</span>
                <span className="stat-label">正确率</span>
              </div>
              <div className="stat-card clickable" onClick={() => checkLogin() && onNavigate?.('wrong')}>
                <span className="stat-value">{wrongCount}</span>
                <span className="stat-label">错题</span>
              </div>
            </div>
          )}
          <div className="wrong-review-done">
            <div className="wrong-review-icon">🎉</div>
            <div className="wrong-review-title">错题复习完成！</div>
            <div className="wrong-review-subtitle">复习摘要：正确 {wrongReviewStats.correct}/{wrongReviewStats.total} 句</div>
            <div className="wrong-review-subtitle">共 {wrongSentences.length} 句错题</div>
            <div className="wrong-review-actions">
              <button className="btn-primary" onClick={() => onNavigate?.('wrong')}>
                📕 返回错题本
              </button>
              <button className="btn-action" onClick={() => setMode('translation')}>
                📝 继续普通练习
              </button>
            </div>
          </div>
          <div className="bottom-section">
            <div className="bottom-nav">
              <button className="bottom-nav-btn" disabled style={{opacity:0.5,cursor:'not-allowed'}}>
                <span className="bottom-nav-icon">🚧</span>
                <span className="bottom-nav-label">浏览(建设中)</span>
              </button>
              <button className="bottom-nav-btn" onClick={() => checkLogin() && onNavigate?.('wrong')}>
                <span className="bottom-nav-icon">❌</span>
                <span className="bottom-nav-label">错题</span>
              </button>
              <button className="bottom-nav-btn" onClick={() => setSettingsOpen(true)}>
                <span className="bottom-nav-icon">⚙️</span>
                <span className="bottom-nav-label">设置</span>
              </button>
              <button className="bottom-nav-btn" disabled style={{opacity:0.5,cursor:'not-allowed'}}>
                <span className="bottom-nav-icon">🚧</span>
                <span className="bottom-nav-label">搜索(建设中)</span>
              </button>
              <button className="bottom-nav-btn" onClick={() => { setFocusMode(f => !f); if (focusMode) setPhoneMode(false); }}>
                <span className="bottom-nav-icon">🧘</span>
                <span className="bottom-nav-label">{focusMode ? '退出' : '专注'}</span>
              </button>
            </div>
          </div>
        </div>
      );
    }

    return (
      <div className="practice-page">
        {toastMsg && <div className="toast-msg">{toastMsg}</div>}
        <div className="stats-bar">
          <div className="stat-card skeleton" style={{ height: 60 }} />
          <div className="stat-card skeleton" style={{ height: 60 }} />
          <div className="stat-card skeleton" style={{ height: 60 }} />
          <div className="stat-card skeleton" style={{ height: 60 }} />
        </div>
        <div className="mode-badge skeleton" style={{ height: 24, width: 120, marginBottom: 16 }} />
        <div className="skeleton" style={{ height: 40, width: '100%', marginBottom: 12 }} />
        <div className="skeleton" style={{ height: 60, width: '100%', marginBottom: 16 }} />
        <div style={{ display: 'flex', gap: 8, marginBottom: 16 }}>
          <div className="skeleton" style={{ height: 36, width: 80 }} />
          <div className="skeleton" style={{ height: 36, width: 80 }} />
          <div className="skeleton" style={{ height: 36, width: 100 }} />
        </div>
      </div>
    );
  }

  const en = extractEn(sentence.text);
  const cn = extractCn(sentence.text);

  // 用户实际输入词统计
  const userTotal = words.filter((_, i) => !hints.has(i)).length;
  const userCorrectCount = words.filter((_, i) => !hints.has(i) && correctWords.has(i)).length;

  return (
    <div className={`practice-page ${focusMode ? 'focus-mode' : ''} ${phoneMode ? 'phone-mode' : ''}`}>
      {toastMsg && <div className="toast-msg">{toastMsg}</div>}
      {/* 手机模式：三行占满上半屏，下半屏是键盘 */}
      {phoneMode && (
        <div className="phone-mode-overlay">
          {/* 专注模式按钮 */}
          <div style={{textAlign:'center',marginBottom:4,display:'flex',justifyContent:'center',gap:6,flexShrink:0}}>
            <button className={`exit-focus-btn ${!phoneMode ? 'active' : ''}`} onClick={() => setPhoneMode(false)}>🧘 专注</button>
            <button className={`exit-focus-btn ${phoneMode ? 'active' : ''}`} onClick={() => setPhoneMode(true)}>📱 手机</button>
            <button className="exit-focus-btn" onClick={() => { setFocusMode(false); setPhoneMode(false); }}>✕ 退出</button>
          </div>

          {/* 第1行: 英文(3行截断) + 中文(2行灰色) */}
          <div className={`sentence-en ${!showEn ? 'blurred' : ''}`}>
            {en}
          </div>
          {cn && (
            <div className={`sentence-cn ${mode === 'dictation' && !showCn && !answered ? 'blurred' : ''}`}>
              {cn}
            </div>
          )}

          {/* 隐藏输入框（手机键盘触发用） */}
          <input ref={hiddenInputRef}
            style={{ position: "fixed", left: "-9999px", width: "1px", height: "1px", opacity: 0 }}
            onBlur={() => {
              let first = 0;
              while (first < words.length && hints.has(first)) first++;
              inputRefs.current[first]?.focus();
            }}
          />

          {/* 第2行: 逐词输入框(自动换行) */}
          <div className="word-inputs">
            {words.map((w, i) => {
              const parts = splitWordParts(w);
              return (
                <div key={i} style={{ display: 'flex', alignItems: 'center', gap: 2 }}>
                  {parts.prefix && <span className="word-sep">{parts.prefix}</span>}
                  <input
                    ref={el => { inputRefs.current[i] = el; }}
                    className={`word-input ${hints.has(i) ? 'hint-word' : ''} ${correctWords.has(i) ? 'correct' : ''} ${wrongWords.has(i) ? 'wrong' : ''}`}
                    size={Math.max(1, parts.letters.length)}
                    placeholder={Array(parts.letters.length).fill('_').join(' ')}
                    value={hints.has(i) ? parts.letters : inputs[i]}
                    onChange={e => { if (!hints.has(i)) handleInputChange(i, e.target.value); }}
                    onKeyDown={e => handleKeyDown(i, e)}
                    disabled={hints.has(i) || answered}
                    autoFocus={i === 0}
                  />
                  {parts.suffix && <span className="word-sep">{parts.suffix}</span>}
                  {i < words.length - 1 && <span className="word-sep"> </span>}
                </div>
              );
            })}
          </div>

          {/* 第3行: 按钮区（一行排满） */}
          <div className="phone-mode-actions">
            <button className="btn-phone-submit" onClick={handleSubmit}>⏎提交</button>
            <button className="btn-phone-next" onClick={goNext}>⏭下一句</button>
            <button className="btn-phone-icon" onClick={playOriginal} title="原音">🎬原音</button>
            <button className="btn-phone-icon" onClick={() => playTts(en)} title="TTS">🎙{VOICES.find(v => v.id === voice)?.label || ''}</button>
            <button className="btn-phone-icon" onClick={() => setShowCn(s => !s)} title={showCn ? '隐藏中文' : '显示中文'}>
              👁️中文
            </button>
            <button className="btn-phone-icon" onClick={() => setShowEn(s => !s)} title={showEn ? '隐藏英文' : '显示英文'}>
              👁️英文
            </button>
          </div>
        </div>
      )}
      {/* Stats Bar — 4卡片: 总句子 / 今日练习 / 正确率 / 错题 */}
      {user && (
        <div className="stats-bar">
          <div className="stat-card" onClick={() => {}}>
            <span className="stat-value">{stats.totalPractices}</span>
            <span className="stat-label">总句子</span>
          </div>
          <div className="stat-card">
            <span className="stat-value">{stats.totalPractices}</span>
            <span className="stat-label">今日练习</span>
          </div>
          <div className="stat-card">
            <span className="stat-value">{accuracy}%</span>
            <span className="stat-label">正确率</span>
          </div>
          <div className="stat-card clickable" onClick={() => checkLogin() && onNavigate?.('wrong')}>
            <span className="stat-value">{wrongCount}</span>
            <span className="stat-label">错题</span>
          </div>
        </div>
      )}

      {/* Mode Badge */}
      <div className="mode-badge">{MODE_LABELS[mode] || '📝 练习模式'}</div>

      {/* 错题练习进度 */}
      {mode === 'wrong' && wrongSentences.length > 0 && (
        <div className="wrong-progress-bar">
          <div className="wrong-progress-text">
            第 {wrongIndex + 1}/{wrongSentences.length} 句
          </div>
          <div className="wrong-progress-track">
            <div
              className="wrong-progress-fill"
              style={{ width: `${((wrongIndex + 1) / wrongSentences.length) * 100}%` }}
            />
          </div>
        </div>
      )}

      {/* 专注模式：专注 | 手机 | 退出（非手机模式时显示） */}
      {focusMode && !phoneMode && (
        <div style={{textAlign:'center',marginBottom:8,display:'flex',justifyContent:'center',gap:6}}>
          <button className={`exit-focus-btn ${!phoneMode ? 'active' : ''}`} onClick={() => setPhoneMode(false)}>🧘 专注</button>
          <button className={`exit-focus-btn ${phoneMode ? 'active' : ''}`} onClick={() => setPhoneMode(true)}>📱 手机</button>
          <button className="exit-focus-btn" onClick={() => { setFocusMode(false); setPhoneMode(false); }}>✕ 退出</button>
        </div>
      )}

      {/* 主卡片 */}
      <div className="practice-card">
        <div className="card-body">
        {/* 剧集名 + ID */}
        <div className="sentence-meta">
          {sentence.showName} · #{sentence.id}
          {mode === 'wrong' && sentence.errorCount !== undefined && (
            <span className="wrong-error-badge">❌ 错{sentence.errorCount}次</span>
          )}
        </div>

        {/* 英文显示区 */}
        <div className={`sentence-en sentence-fade-in ${!showEn ? 'blurred' : ''}`}>
          {en}
        </div>

        {/* 中文显示区 */}
        {cn && (
          <div className={`sentence-cn sentence-fade-in ${mode === 'dictation' && !showCn && !answered ? 'blurred' : ''}`}>
            {cn}
          </div>
        )}

        {/* 逐词输入（未完成时显示） */}
        {/* 输入框始终显示，回答后变为只读 */}
          {/* 隐藏输入框（手机键盘触发用） */}
          <input ref={hiddenInputRef}
            style={{ position: "fixed", left: "-9999px", width: "1px", height: "1px", opacity: 0 }}
            onBlur={() => {
              // 隐藏输入框失焦时，聚焦到实际输入框
              let first = 0;
              while (first < words.length && hints.has(first)) first++;
              inputRefs.current[first]?.focus();
            }}
          />
          <div className="word-inputs">
            {words.map((w, i) => {
              const parts = splitWordParts(w);
              return (
                <div key={i} style={{ display: 'flex', alignItems: 'center', gap: 2 }}>
                  {parts.prefix && <span className="word-sep">{parts.prefix}</span>}
                  <input
                    ref={el => { inputRefs.current[i] = el; }}
                    className={`word-input ${hints.has(i) ? 'hint-word' : ''} ${correctWords.has(i) ? 'correct' : ''} ${wrongWords.has(i) ? 'wrong' : ''}`}
                    size={Math.max(1, parts.letters.length)}
                    placeholder={Array(parts.letters.length).fill('_').join(' ')}
                    value={hints.has(i) ? parts.letters : inputs[i]}
                    onChange={e => { if (!hints.has(i)) handleInputChange(i, e.target.value); }}
                    onKeyDown={e => handleKeyDown(i, e)}
                    disabled={hints.has(i) || answered}
                    autoFocus={i === 0}
                  />
                  {parts.suffix && <span className="word-sep">{parts.suffix}</span>}
                  {i < words.length - 1 && <span className="word-sep"> </span>}
                </div>
              );
            })}
          </div>

        {/* 反馈区域 */}
        {answered && (
          <div className={`feedback ${wrongWords.size === 0 ? 'correct' : 'wrong'}`}>
            {wrongWords.size === 0
              ? '✅ 完全正确！'
              : `❌ 正确 ${userCorrectCount}/${userTotal} 个词`}
          </div>
        )}
        {retryCount === 1 && !answered && (
          <div className="feedback retry">❌ 有错误，再试一次 ({userTotal > 0 ? Math.round(userCorrectCount/userTotal*100) : 0}%)</div>
        )}

        {/* 举报按钮（仅答完题后显示） */}
        {answered && user && (
          <div className="flag-section">
            <button
              className={`flag-btn ${flaggedSentenceIds.has(sentence.id) ? 'flagged' : ''}`}
              onClick={toggleFlag}
              disabled={flagLoading}
              title={flaggedSentenceIds.has(sentence.id) ? '取消举报' : '标记：这句台词跟原音对不上'}
            >
              🚩 {flaggedSentenceIds.has(sentence.id) ? '已标记' : '报告问题'}
            </button>
          </div>
        )}

        </div>
      </div>

      {/* 按钮区 */}
      <div className="bottom-section">
      <div className="button-area">
        <hr className="action-divider" />

        {/* 操作行1：提交 */}
        <div className="action-row">
          {!answered ? (
            <button className="btn-primary" ref={submitRef} onClick={handleSubmit}>⏎ 提交</button>
          ) : (
            <button className="btn-primary" onClick={goNext}>⏭️ 下一句</button>
          )}
        </div>

        {/* 操作行2：剧集原音 / 服务器音色 / 下一句 */}
        <div className="action-row">
          <button className="btn-action" onClick={playOriginal}>🎬 剧集原音</button>
          <button className="btn-action" onClick={() => playTts(en)}>🎙️ {VOICES.find(v => v.id === voice)?.label || '导播'}</button>
          <button className="btn-action" onClick={goNext}>⏭️ 下一句</button>
        </div>

        {/* 操作行3：速度(50%) + 显示中文/英文(50%) */}
        <div className="action-row-split">
          <div className="action-row-half">
            {SPEEDS.map(s => (
              <button
                key={s}
                className={`btn-speed ${speed === s ? 'active' : ''}`}
                onClick={() => setSpeed(s)}
              >
                {s}x
              </button>
            ))}
          </div>
          <div className="action-row-half">
            {mode === 'dictation' && (
              <button className="btn-action" onClick={() => setShowCn(s => !s)}>
                {showCn ? '🙈 隐藏中文' : '👁️ 显示中文'}
              </button>
            )}
            <button className="btn-action" onClick={() => setShowEn(s => !s)}>
              {showEn ? '🙈 隐藏英文' : '👁️ 显示英文'}
            </button>
          </div>
        </div>

        {/* 快捷键提示 */}
        <div className="shortcuts-hint">
          <kbd>-</kbd> 原音 <kbd>=</kbd> 音色 <kbd>\</kbd> 下一句 <kbd>[</kbd> 中文 <kbd>]</kbd> 英文 <kbd>Enter</kbd> 提交
        </div>
      </div>

      {/* 底部导航 */}
      <div className="bottom-nav">
        <button className="bottom-nav-btn" disabled style={{opacity:0.5,cursor:'not-allowed'}}>
          <span className="bottom-nav-icon">🚧</span>
          <span className="bottom-nav-label">浏览(建设中)</span>
        </button>
        <button className="bottom-nav-btn" onClick={() => checkLogin() && onNavigate?.('wrong')}>
          <span className="bottom-nav-icon">❌</span>
          <span className="bottom-nav-label">错题</span>
        </button>
        <button className="bottom-nav-btn" onClick={() => setSettingsOpen(true)}>
          <span className="bottom-nav-icon">⚙️</span>
          <span className="bottom-nav-label">设置</span>
        </button>
        <button className="bottom-nav-btn" disabled style={{opacity:0.5,cursor:'not-allowed'}}>
          <span className="bottom-nav-icon">🚧</span>
          <span className="bottom-nav-label">搜索(建设中)</span>
        </button>
        <button className="bottom-nav-btn" onClick={() => { setFocusMode(f => !f); if (focusMode) setPhoneMode(false); }}>
          <span className="bottom-nav-icon">🧘</span>
          <span className="bottom-nav-label">{focusMode ? '退出' : '专注'}</span>
        </button>
      </div>

      </div>
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
        showId={showIdsParam}
        onShowChange={setShowIdsParam}
        autoPlay={autoPlay}
        onAutoPlayChange={setAutoPlay}
        preferOriginal={preferOriginal}
        onPreferOriginalChange={setPreferOriginal}
      />

      {/* 搜索面板 */}
      {searchOpen && (
        <div className="settings-overlay" onClick={() => setSearchOpen(false)}>
          <div className="search-panel" onClick={e => e.stopPropagation()}>
            <div className="settings-header">
              <h3>🔍 搜索句子</h3>
              <button className="close-btn" onClick={() => { setSearchOpen(false); setSearchQuery(''); setSearchResults([]); }}>✕</button>
            </div>
            <input
              className="search-input"
              type="text"
              placeholder="搜索句子（中英文）..."
              value={searchQuery}
              onChange={e => handleSearch(e.target.value)}
              autoFocus
            />
            <div className="search-results-inline">
              {searchResults.map((s: any) => (
                <div key={s.id} className="search-result-item" onClick={() => jumpToSearchResult(s.id)}>
                  <div className="result-text">{s.text}</div>
                  <div className="result-meta">{s.showName} · #{s.id}</div>
                </div>
              ))}
              {searchQuery.length >= 2 && searchResults.length === 0 && (
                <div className="empty-state">无结果</div>
              )}
            </div>
          </div>
        </div>
      )}

      {/* 错题再练提示 */}
      {wrongBookPrompt && (
        <div className="settings-overlay">
          <div className="search-panel wrong-retry-panel">
            <div className="wrong-retry-content">
              <div className="wrong-retry-icon">📝</div>
              <div className="wrong-retry-title">这句之前错过 {wrongBookPrompt.errorCount} 次</div>
              <div className="wrong-retry-subtitle">再练一遍，巩固记忆？</div>
              <div className="wrong-retry-actions">
                <button className="btn-primary" onClick={retryWrongSentence}>
                  🔄 再练一遍
                </button>
                <button className="btn-action" onClick={skipRetry}>
                  ⏭️ 跳过
                </button>
              </div>
            </div>
          </div>
        </div>
      )}

    </div>
  );
}
