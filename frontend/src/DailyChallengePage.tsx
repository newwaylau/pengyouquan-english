import React, { useState, useEffect, useRef } from 'react';
import { gameApi } from './api/client';
import { getAudioUrl, getTtsUrl } from './audioBase';

interface Question {
  id: number;
  sentenceId: number;
  englishText: string;
  chineseText: string;
  audioFile: string;
  answered: boolean;
  isCorrect: boolean | null;
  userAnswer: string | null;
}

interface ChallengeData {
  challengeId: number;
  questions: Question[];
  answeredCount: number;
  correctCount: number;
  completed: boolean;
  comboCount: number;
}

export default function DailyChallengePage({ onBack }: { onBack: () => void }) {
  const [challenge, setChallenge] = useState<ChallengeData | null>(null);
  const [currentIndex, setCurrentIndex] = useState(0);
  const [answer, setAnswer] = useState('');
  const [feedback, setFeedback] = useState<{ correct: boolean; message: string } | null>(null);
  const [combo, setCombo] = useState(0);
  const [maxCombo, setMaxCombo] = useState(0);
  const [submitting, setSubmitting] = useState(false);
  const [completed, setCompleted] = useState(false);
  const [completionResult, setCompletionResult] = useState<any>(null);
  const inputRef = useRef<HTMLInputElement>(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    gameApi.getDailyChallenge().then(r => {
      if (r.code === 200) {
        setChallenge(r.data);
        // Check answered status - skip already answered questions
        const questions = r.data.questions;
        const firstUnanswered = questions.findIndex((q: Question) => !q.answered);
        if (firstUnanswered >= 0) setCurrentIndex(firstUnanswered);
        // Recalculate combo from answered questions
        let c = 0;
        for (const q of questions) {
          if (q.answered && q.isCorrect) c++;
          else if (q.answered && !q.isCorrect) c = 0;
        }
        setCombo(c);
        if (r.data.completed) setCompleted(true);
      }
      setLoading(false);
    });
  }, []);

  useEffect(() => {
    if (!feedback) inputRef.current?.focus();
  }, [currentIndex, feedback]);

  const currentQuestion = challenge?.questions[currentIndex];
  const allAnswered = challenge?.questions.every(q => q.answered);
  const correctSoFar = challenge?.questions.filter(q => q.answered && q.isCorrect).length || 0;

  const handleSubmit = async () => {
    if (!challenge || !currentQuestion || submitting) return;
    const trimmed = answer.trim().toLowerCase();
    if (!trimmed) return;

    setSubmitting(true);
    try {
      const r = await gameApi.submitAnswer(challenge.challengeId, currentQuestion.id, trimmed);
      if (r.code === 200) {
        const correct = r.data.correct;
        setFeedback({ correct, message: r.data.message });
        const newCombo = correct ? combo + 1 : 0;
        setCombo(newCombo);
        if (newCombo > maxCombo) setMaxCombo(newCombo);

        // Update local state
        const updated = { ...challenge };
        updated.questions = [...updated.questions];
        updated.questions[currentIndex] = {
          ...updated.questions[currentIndex],
          answered: true,
          isCorrect: correct,
          userAnswer: trimmed,
        };
        updated.answeredCount++;
        if (correct) updated.correctCount++;
        setChallenge(updated);
      }
    } finally {
      setSubmitting(false);
    }
  };

  const handleNext = () => {
    setFeedback(null);
    setAnswer('');
    if (currentIndex < (challenge?.questions.length || 1) - 1) {
      setCurrentIndex(currentIndex + 1);
    }
  };

  const handleComplete = async () => {
    if (!challenge) return;
    try {
      const r = await gameApi.completeChallenge(challenge.challengeId);
      if (r.code === 200) {
        setCompletionResult(r.data);
        setCompleted(true);
      }
    } catch (e) {
      console.error('complete failed', e);
    }
  };

  const handleKeyDown = (e: React.KeyboardEvent) => {
    if (e.key === 'Enter') {
      e.preventDefault();
      if (feedback) {
        handleNext();
      } else {
        handleSubmit();
      }
    }
  };

  if (loading) {
    return (
      <div className="daily-challenge-page" style={{ padding: 24, maxWidth: 600, margin: '0 auto' }}>
        <div style={{ textAlign: 'center', color: 'var(--text-secondary)', padding: 40 }}>⚔️ 集结兵力...</div>
      </div>
    );
  }

  if (!challenge) {
    return (
      <div className="daily-challenge-page" style={{ padding: 24, maxWidth: 600, margin: '0 auto' }}>
        <div style={{ textAlign: 'center', color: 'var(--danger)', padding: 40 }}>加载御前挑战失败</div>
        <button onClick={onBack} style={{ display: 'block', margin: '0 auto', padding: '10px 24px', background: 'var(--teal)', color: '#fff', border: 'none', borderRadius: 'var(--radius-sm)', cursor: 'pointer' }}>返回演武场</button>
      </div>
    );
  }

  // Completion screen
  if (completed && completionResult) {
    const correctCount = completionResult.correctCount || correctSoFar;
    const prestigeEarned = completionResult.totalReward || 0;
    const comboBonus = completionResult.comboBonus || 0;
    const allCorrect = correctCount === completionResult.totalQuestions;

    return (
      <div className="daily-challenge-page" style={{ padding: 24, maxWidth: 600, margin: '0 auto', textAlign: 'center' }}>
        {/* 结算标题 */}
        <div style={{ fontSize: 48, marginBottom: 8 }}>
          {allCorrect ? '🏆' : correctCount >= 8 ? '⚔️' : '🛡️'}
        </div>
        <div style={{ color: 'var(--text-primary)', fontSize: 22, fontWeight: 700, marginBottom: 4 }}>
          御前挑战 {allCorrect ? '大捷' : '完成'}!
        </div>
        <div style={{ color: 'var(--text-secondary)', fontSize: 14, marginBottom: 24 }}>
          {challenge.completed ? '今日挑战已结算' : '战绩已载入史册'}
        </div>

        {/* 战绩卡片 */}
        <div style={{
          background: 'linear-gradient(135deg, var(--card), var(--bg))',
          borderRadius: 'var(--radius-lg)', padding: 24, marginBottom: 20,
          border: `1px solid ${allCorrect ? 'rgba(251,191,36,0.3)' : 'rgba(20,184,166,0.2)'}`
        }}>
          <div style={{ display: 'flex', justifyContent: 'space-around', marginBottom: 20 }}>
            <div style={{ textAlign: 'center' }}>
              <div style={{ color: 'var(--text-primary)', fontSize: 28, fontWeight: 700 }}>{correctCount}/10</div>
              <div style={{ color: 'var(--text-secondary)', fontSize: 12 }}>正确</div>
            </div>
            <div style={{ textAlign: 'center' }}>
              <div style={{ color: '#fbbf24', fontSize: 28, fontWeight: 700 }}>+{prestigeEarned}</div>
              <div style={{ color: 'var(--text-secondary)', fontSize: 12 }}>威望</div>
            </div>
            <div style={{ textAlign: 'center' }}>
              <div style={{ color: 'var(--teal)', fontSize: 28, fontWeight: 700 }}>🔥 {maxCombo}</div>
              <div style={{ color: 'var(--text-secondary)', fontSize: 12 }}>最大连击</div>
            </div>
          </div>

          {/* 威望变化 */}
          <div style={{
            background: 'rgba(255,255,255,0.04)',
            borderRadius: 'var(--radius-md)', padding: 16
          }}>
            <div style={{ display: 'flex', justifyContent: 'space-between', color: 'var(--text-secondary)', fontSize: 13, marginBottom: 8 }}>
              <span>⚡ 威望变化</span>
              <span style={{ color: '#fbbf24', fontWeight: 600 }}>+{prestigeEarned}</span>
            </div>
            {comboBonus > 0 && (
              <div style={{ color: '#fbbf24', fontSize: 12, marginBottom: 6 }}>
                ✨ 全对连击奖励 +{comboBonus} 威望
              </div>
            )}
          </div>
        </div>

        {/* 封号晋升通知 */}
        {completionResult.oldRankTier !== undefined && completionResult.oldRankTier !== completionResult.newRankTier && (
          <div style={{
            textAlign: 'center',
            background: 'linear-gradient(135deg, rgba(251,191,36,0.1), rgba(251,191,36,0.05))',
            borderRadius: 'var(--radius-lg)', padding: 20, marginBottom: 16,
            border: '1px solid rgba(251,191,36,0.3)'
          }}>
            <div style={{ fontSize: 40, marginBottom: 4 }}>👑</div>
            <div style={{ color: '#fbbf24', fontSize: 18, fontWeight: 700 }}>封号晋升!</div>
            <div style={{ color: 'var(--text-primary)', fontSize: 14 }}>
              {completionResult.oldTitleCn || ''} → <span style={{ color: '#fbbf24', fontWeight: 600, fontSize: 18 }}>{completionResult.newTitleCn}</span>
            </div>
          </div>
        )}

        {/* 返回按钮 */}
        <button onClick={onBack}
          style={{
            width: '100%', padding: 14, border: 'none', borderRadius: 'var(--radius-md)', cursor: 'pointer',
            background: 'linear-gradient(135deg, var(--teal), var(--teal-dark))',
            color: '#fff', fontWeight: 600, fontSize: 15
          }}>
          返回演武场
        </button>
      </div>
    );
  }

  // In-progress screen
  const totalQuestions = challenge.questions.length;

  return (
    <div className="daily-challenge-page" style={{ padding: 16, maxWidth: 600, margin: '0 auto' }}>
      {/* 顶栏 */}
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 16 }}>
        <button onClick={onBack} style={{ background: 'transparent', border: 'none', color: 'var(--text-secondary)', fontSize: 14, cursor: 'pointer', padding: 4 }}>
          ← 退出
        </button>
        <div style={{ color: 'var(--text-primary)', fontSize: 14, fontWeight: 600 }}>
          ⚔️ 御前挑战
        </div>
        {combo >= 3 && (
          <div style={{
            background: 'linear-gradient(135deg, #fbbf24, var(--warning))',
            color: 'var(--card)', fontSize: 11, fontWeight: 700, padding: '4px 10px',
            borderRadius: 20
          }}>
            🔥 {combo}连击!
          </div>
        )}
        {combo < 3 && <div style={{ width: 60 }} />}
      </div>

      {/* 进度条 */}
      <div style={{ marginBottom: 16 }}>
        <div style={{ display: 'flex', justifyContent: 'space-between', fontSize: 12, color: 'var(--text-secondary)', marginBottom: 4 }}>
          <span>进度 {challenge.answeredCount}/{totalQuestions}</span>
          <span>正确 {correctSoFar}</span>
        </div>
        <div style={{ height: 6, background: 'rgba(255,255,255,0.06)', borderRadius: 3, overflow: 'hidden' }}>
          <div style={{
            width: `${(challenge.answeredCount / totalQuestions) * 100}%`,
            height: '100%',
            background: 'linear-gradient(90deg, var(--teal), var(--teal-light))',
            borderRadius: 3,
            transition: 'width 0.4s ease'
          }} />
        </div>
      </div>

      {/* 当前题目 */}
      {currentQuestion && !completed && (
        <div style={{
          background: 'linear-gradient(135deg, var(--card), var(--bg))',
          borderRadius: 'var(--radius-lg)', padding: 24,
          border: '1px solid rgba(20,184,166,0.2)'
        }}>
          {/* 题号 */}
          <div style={{ color: 'var(--teal)', fontSize: 12, fontWeight: 600, marginBottom: 12 }}>
            第 {currentIndex + 1} 题
          </div>

          {/* 英文 */}
          <div style={{ color: 'var(--text-primary)', fontSize: 16, fontWeight: 500, lineHeight: 1.5, marginBottom: 12 }}>
            {currentQuestion.englishText}
          </div>

          {/* 音频播放按钮 */}
          <div style={{ display: 'flex', gap: 8, marginBottom: 12 }}>
            <button onClick={() => {
              if (currentQuestion.audioFile) {
                const a = new Audio(getAudioUrl('/api/audio/' + encodeURIComponent(currentQuestion.audioFile)));
                a.play().catch(() => {});
              }
            }} style={{
              padding: '6px 14px', borderRadius: 'var(--radius-sm)', border: '1px solid rgba(20,184,166,0.3)',
              background: 'rgba(20,184,166,0.08)', color: 'var(--teal)', cursor: 'pointer',
              fontSize: 13, fontWeight: 500
            }}>
              🔊 原音
            </button>
            <button onClick={() => {
              if (currentQuestion.englishText) {
                // 提取英文（去除中文字符和编号标记）
                const clean = currentQuestion.englishText.replace(/#\d+\s*/, '').trim();
                const a = new Audio(getTtsUrl(clean, 'default'));
                a.play().catch(() => {});
              }
            }} style={{
              padding: '6px 14px', borderRadius: 'var(--radius-sm)', border: '1px solid rgba(167,139,250,0.3)',
              background: 'rgba(167,139,250,0.08)', color: '#a78bfa', cursor: 'pointer',
              fontSize: 13, fontWeight: 500
            }}>
              🎤 TTS
            </button>
          </div>

          {/* 中文提示 */}
          <div style={{ color: 'var(--text-tertiary)', fontSize: 13, marginBottom: 20, fontStyle: 'italic' }}>
            💡 {currentQuestion.chineseText || '无中文提示'}
          </div>

          {/* 输入区域 */}
          {!feedback ? (
            <>
              <div style={{ marginBottom: 12 }}>
                <input
                  ref={inputRef}
                  type="text"
                  value={answer}
                  onChange={e => setAnswer(e.target.value)}
                  onKeyDown={handleKeyDown}
                  placeholder="输入英文原文..."
                  autoFocus
                  style={{
                    width: '100%', padding: '14px 16px',
                    background: 'rgba(255,255,255,0.04)',
                    border: '1px solid rgba(255,255,255,0.1)',
                    borderRadius: 'var(--radius-md)', outline: 'none',
                    color: 'var(--text-primary)', fontSize: 16,
                    boxSizing: 'border-box'
                  }}
                />
              </div>
              <button
                onClick={handleSubmit}
                disabled={submitting || !answer.trim()}
                style={{
                  width: '100%', padding: 14, border: 'none', borderRadius: 'var(--radius-md)', cursor: 'pointer',
                  background: submitting || !answer.trim()
                    ? 'rgba(20,184,166,0.3)' : 'linear-gradient(135deg, var(--teal), var(--teal-dark))',
                  color: '#fff', fontWeight: 600, fontSize: 15,
                  opacity: submitting || !answer.trim() ? 0.5 : 1
                }}
              >
                {submitting ? '判题中...' : '提交 ⚔️'}
              </button>
            </>
          ) : (
            /* 反馈区域 */
            <div>
              <div style={{
                padding: 16, borderRadius: 'var(--radius-md)', marginBottom: 12,
                background: feedback.correct ? 'rgba(20,184,166,0.08)' : 'rgba(239,68,68,0.08)',
                border: `1px solid ${feedback.correct ? 'rgba(20,184,166,0.2)' : 'rgba(239,68,68,0.2)'}`
              }}>
                <div style={{ color: feedback.correct ? 'var(--teal-light)' : '#f87171', fontSize: 18, fontWeight: 700, marginBottom: 4 }}>
                  {feedback.correct ? '✅ 正确!' : '❌ 答错了'}
                </div>
                {!feedback.correct && (
                  <div style={{ color: 'var(--text-primary)', fontSize: 14, lineHeight: 1.5 }}>
                    正确答案: <span style={{ color: 'var(--teal-light)', fontWeight: 600 }}>{currentQuestion.englishText}</span>
                  </div>
                )}
                {combo >= 3 && feedback.correct && (
                  <div style={{ color: '#fbbf24', fontSize: 13, marginTop: 4 }}>
                    🔥 {combo} 连击! 再接再厉
                  </div>
                )}
              </div>
              <button
                onClick={handleNext}
                style={{
                  width: '100%', padding: 14, border: 'none', borderRadius: 'var(--radius-md)', cursor: 'pointer',
                  background: currentIndex < totalQuestions - 1
                    ? 'linear-gradient(135deg, var(--teal), var(--teal-dark))'
                    : 'linear-gradient(135deg, #a78bfa, #8b5cf6)',
                  color: '#fff', fontWeight: 600, fontSize: 15
                }}
              >
                {currentIndex < totalQuestions - 1 ? '下一题 →' : allAnswered ? '结算战绩 🏆' : '下一题 →'}
              </button>
            </div>
          )}
        </div>
      )}

      {/* 全部答完 → 结算按钮 */}
      {allAnswered && !completed && (
        <div style={{ marginTop: 16, textAlign: 'center' }}>
          <div style={{ color: 'var(--text-secondary)', fontSize: 13, marginBottom: 8 }}>
            已答完全部 {totalQuestions} 题，获得 {correctSoFar} 个胜场
          </div>
          <button onClick={handleComplete}
            style={{
              width: '100%', padding: 16, border: 'none', borderRadius: 'var(--radius-md)', cursor: 'pointer',
              background: 'linear-gradient(135deg, #fbbf24, var(--warning))',
              color: 'var(--card)', fontWeight: 700, fontSize: 16
            }}>
            🏆 结算御前挑战
          </button>
        </div>
      )}
    </div>
  );
}
