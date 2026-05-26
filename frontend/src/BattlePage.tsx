import React, { useState, useEffect } from 'react';
import { cardApi } from './api/cardClient';
import ChestPanel from './ChestPanel';

export default function BattlePage({ user, onNavigate }: { user: any; onNavigate: (target: string, data?: any) => void }) {
  const [tab, setTab] = useState<'battle' | 'history' | 'rank' | 'season'>('battle');
  const [friends, setFriends] = useState<any[]>([]);
  const [pendingBattles, setPendingBattles] = useState<any[]>([]);
  const [history, setHistory] = useState<any[]>([]);
  const [rank, setRank] = useState<any>(null);
  const [leaderboard, setLeaderboard] = useState<any[]>([]);
  const [decks, setDecks] = useState<any[]>([]);
  const [searchQuery, setSearchQuery] = useState('');
  const [searchResults, setSearchResults] = useState<any[]>([]);
  const [pendingRequests, setPendingRequests] = useState<any[]>([]);
  const [showChallengeModal, setShowChallengeModal] = useState<any>(null);
  const [showAcceptModal, setShowAcceptModal] = useState<any>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [searching, setSearching] = useState(false);
  const [seasonData, setSeasonData] = useState<any>(null);
  const [seasonReward, setSeasonReward] = useState<any>(null);

  // 挑战表单
  const [challengeDeckId, setChallengeDeckId] = useState<number | null>(null);
  // 接受挑战表单
  const [acceptDeckId, setAcceptDeckId] = useState<number | null>(null);
  const [acceptScore, setAcceptScore] = useState(0);
  const [acceptAccuracy, setAcceptAccuracy] = useState(0);
  const [acceptDifficulty, setAcceptDifficulty] = useState(3);

  useEffect(() => {
    loadAll();
    loadSeason();
  }, []);

  const loadSeason = async () => {
    const [curRes, rewRes] = await Promise.all([
      cardApi.getCurrentSeason(),
      cardApi.getSeasonRewards(),
    ]);
    if (curRes.code === 200) setSeasonData(curRes.data);
    if (rewRes.code === 200) setSeasonReward(rewRes.data);
  };

  const loadAll = async () => {
    setLoading(true);
    try {
      const [friendsRes, pendingRes, historyRes, rankRes, leaderRes, decksRes, pendReqRes] = await Promise.all([
        cardApi.getFriends(),
        cardApi.getPendingBattles(),
        cardApi.getBattleHistory(),
        cardApi.getRank(),
        cardApi.getLeaderboard(),
        cardApi.getDecks(),
        cardApi.getPendingRequests(),
      ]);
      if (friendsRes.code === 200) setFriends(friendsRes.data || []);
      if (pendingRes.code === 200) setPendingBattles(pendingRes.data || []);
      if (historyRes.code === 200) setHistory(historyRes.data || []);
      if (rankRes.code === 200) setRank(rankRes.data);
      if (leaderRes.code === 200) setLeaderboard(leaderRes.data || []);
      if (decksRes.code === 200) setDecks(decksRes.data || []);
      if (pendReqRes.code === 200) setPendingRequests(pendReqRes.data || []);
    } catch (e) {
      setError('加载数据失败');
    }
    setLoading(false);
  };

  const handleSearch = async () => {
    if (!searchQuery.trim()) return;
    setSearching(true);
    try {
      const res = await cardApi.searchUsers(searchQuery.trim());
      if (res.code === 200) setSearchResults(res.data || []);
    } catch {}
    setSearching(false);
  };

  const handleSendRequest = async (friendId: number) => {
    const res = await cardApi.sendFriendRequest(friendId);
    if (res.code === 200) {
      alert('好友请求已发送！');
      setSearchResults([]);
      setSearchQuery('');
    } else {
      alert(res.message || '发送失败');
    }
  };

  const handleAcceptRequest = async (friendId: number) => {
    const res = await cardApi.acceptFriendRequest(friendId);
    if (res.code === 200) {
      loadAll();
    } else {
      alert(res.message || '操作失败');
    }
  };

  const handleChallenge = async () => {
    if (!showChallengeModal || !challengeDeckId) return;
    try {
      const res = await cardApi.challengePlayer(showChallengeModal.friendId, challengeDeckId);
      if (res.code === 200) {
        alert('挑战已发送！等待对方回应');
        setShowChallengeModal(null);
        setChallengeDeckId(null);
        loadAll();
      } else {
        alert(res.message || '发起挑战失败');
      }
    } catch {}
  };

  const handleAcceptBattle = async () => {
    if (!showAcceptModal || !acceptDeckId) return;
    try {
      const res = await cardApi.acceptBattle(showAcceptModal.id, acceptDeckId, {
        score: acceptScore,
        accuracy: acceptAccuracy / 100,
        avgDifficulty: acceptDifficulty,
      });
      if (res.code === 200) {
        const d = res.data;
        let result = '';
        if (d.winnerId === user.id) {
          result = '🎉 你赢了！奖杯 +' + d.trophyChange;
        } else if (d.winnerId === null) {
          result = '🤝 平局！';
        } else {
          result = '😢 你输了... 奖杯 -' + d.trophyChange;
        }
        alert(result + ' 得分：你 ' + d.defenderScore + ' vs ' + d.challengerNickname + ' ' + d.challengerScore);
        setShowAcceptModal(null);
        loadAll();
      } else {
        alert(res.message || '对战失败');
      }
    } catch {}
  };

  const getResultIcon = (b: any) => {
    if (b.status !== 'completed') return '⏳';
    if (b.winnerId === null) return '🤝';
    if (b.winnerId === user.id) return '✅';
    return '❌';
  };

  const getResultText = (b: any) => {
    if (b.status !== 'completed') return '待处理';
    if (b.winnerId === null) return '平局';
    if (b.winnerId === user.id) return '胜利';
    return '败北';
  };

  const getTrophyText = (b: any) => {
    if (b.status !== 'completed') return '';
    if (b.winnerId === user.id) return '+' + b.trophyChange;
    if (b.winnerId === null) return '±0';
    return '-' + b.trophyChange;
  };

  const getTrophyColor = (b: any) => {
    if (b.status !== 'completed') return 'var(--text-secondary)';
    if (b.winnerId === user.id) return 'var(--success)';
    if (b.winnerId === null) return 'var(--text-secondary)';
    return 'var(--danger)';
  };

  if (loading) return <div className="page-loading">加载中...</div>;

  return (
    <div className="card-collection-page">
      {/* Tabs */}
      <div className="cards-subtabs">
        <button className={`cst-btn ${tab === 'battle' ? 'active' : ''}`} onClick={() => setTab('battle')}>
          ⚔️ 对战
        </button>
        <button className={`cst-btn ${tab === 'history' ? 'active' : ''}`} onClick={() => setTab('history')}>
          📜 战报
        </button>
        <button className={`cst-btn ${tab === 'rank' ? 'active' : ''}`} onClick={() => setTab('rank')}>
          🏆 排名
        </button>
        <button className={`cst-btn ${tab === 'season' ? 'active' : ''}`} onClick={() => setTab('season')}>
          🗓️ 赛季
        </button>
      </div>

      {/* 宝箱面板 */}
      <ChestPanel user={user} />

      {/* Tab: ⚔️ 对战 */}
      {tab === 'battle' && (
        <div className="cc-arena-tab">
            {/* 实时对战入口 */}
            <div style={{
              background: 'linear-gradient(135deg, #1a1a2e, #16213e)',
              borderRadius: 'var(--radius-lg)', padding: 16, marginBottom: 16,
              border: '1px solid var(--teal-glow)',
              cursor: 'pointer', textAlign: 'center'
            }}
              onClick={() => onNavigate('battle-arena')}
            >
              <div style={{ fontSize: 28, marginBottom: 4 }}>⚡</div>
              <div style={{ fontSize: 16, fontWeight: 700, color: 'var(--teal)', marginBottom: 4 }}>
                实时对战
              </div>
              <div style={{ fontSize: 13, color: 'var(--text-secondary)' }}>
                即时匹配 · 答题对战 · 赢取奖杯
              </div>
            </div>

          {/* 段位卡片 */}
          {rank && (
            <div className="arena-rank-card" style={{
              background: 'linear-gradient(135deg, var(--card), var(--bg))',
              borderRadius: 'var(--radius-lg)', padding: 24, margin: '0 0 16px',
              border: '1px solid var(--teal-glow)',
              boxShadow: '0 4px 20px rgba(20, 184, 166, 0.15)'
            }}>
              <div style={{ display: 'flex', alignItems: 'center', gap: 16 }}>
                <div style={{
                  width: 56, height: 56, borderRadius: '50%',
                  background: 'linear-gradient(135deg, var(--teal), var(--teal-dark))',
                  display: 'flex', alignItems: 'center', justifyContent: 'center',
                  fontSize: 24, flexShrink: 0,
                  boxShadow: '0 0 16px var(--teal-glow)'
                }}>
                  {rank.tierIcon}
                </div>
                <div style={{ flex: 1 }}>
                  <div style={{ color: 'var(--teal)', fontSize: 16, fontWeight: 600 }}>{rank.tierName}</div>
                  <div style={{ color: 'var(--text-secondary)', fontSize: 13 }}>🏆 {rank.trophies} 奖杯</div>
                  <div style={{ color: 'var(--text-secondary)', fontSize: 12 }}>排名 #{rank.rank} · 胜 {rank.wins} 负 {rank.losses}</div>
                </div>
              </div>
            </div>
          )}

          {/* 待处理挑战 */}
          {pendingBattles.length > 0 && (
            <div style={{ marginBottom: 16 }}>
              <div style={{ color: 'var(--warning)', fontSize: 14, marginBottom: 8, fontWeight: 600 }}>
                ⚠️ 有 {pendingBattles.length} 个待处理的挑战
              </div>
              {pendingBattles.map((b: any) => (
                <div key={b.id} className="cc-deck-item" style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                  <div>
                    <div style={{ color: 'var(--text-primary)' }}>⚔️ {b.challengerNickname}</div>
                    <div style={{ color: 'var(--text-secondary)', fontSize: 12 }}>
                      {b.createdAt ? new Date(b.createdAt).toLocaleString() : ''}
                    </div>
                  </div>
                  <button className="btn btn-primary" style={{ padding: '6px 16px', fontSize: 13 }}
                    onClick={() => { setShowAcceptModal(b); setAcceptDeckId(null); setAcceptScore(0); setAcceptAccuracy(0); setAcceptDifficulty(3); }}>
                    迎战
                  </button>
                </div>
              ))}
            </div>
          )}

          {/* 搜索用户 */}
          <div style={{ marginBottom: 16 }}>
            <div style={{ display: 'flex', gap: 8, marginBottom: 8 }}>
              <input
                type="text"
                placeholder="搜索用户添加好友..."
                value={searchQuery}
                onChange={e => setSearchQuery(e.target.value)}
                onKeyDown={e => e.key === 'Enter' && handleSearch()}
                style={{
                  flex: 1, padding: '8px 12px', borderRadius: 'var(--radius-sm)',
                  border: '1px solid var(--border)', background: 'var(--input-bg)',
                  color: 'var(--text)', fontSize: 14
                }}
              />
              <button className="btn btn-primary" onClick={handleSearch} disabled={searching}>
                {searching ? '搜索中...' : '搜索'}
              </button>
            </div>
            {searchResults.map((u: any) => (
              <div key={u.friendId} className="cc-deck-item" style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                <div>
                  <div style={{ color: 'var(--text-primary)' }}>{u.friendNickname || u.friendEmail}</div>
                  <div style={{ color: 'var(--text-secondary)', fontSize: 12 }}>{u.friendEmail}</div>
                </div>
                <button className="btn btn-outline-style" style={{ padding: '4px 12px', fontSize: 12 }}
                  onClick={() => handleSendRequest(u.friendId)}>
                  + 添加
                </button>
              </div>
            ))}
          </div>

          {/* 待处理的好友请求 */}
          {pendingRequests.length > 0 && (
            <div style={{ marginBottom: 16 }}>
              <div style={{ color: 'var(--text-secondary)', fontSize: 14, marginBottom: 8 }}>📩 好友请求</div>
              {pendingRequests.map((r: any) => (
                <div key={r.id} className="cc-deck-item" style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                  <div>
                    <div style={{ color: 'var(--text-primary)' }}>{r.friendNickname || r.friendEmail}</div>
                    <div style={{ color: 'var(--text-secondary)', fontSize: 12 }}>请求加你为好友</div>
                  </div>
                  <button className="btn btn-primary" style={{ padding: '4px 12px', fontSize: 12 }}
                    onClick={() => handleAcceptRequest(r.friendId)}>
                    接受
                  </button>
                </div>
              ))}
            </div>
          )}

          {/* 好友列表 */}
          <div style={{ color: 'var(--text-secondary)', fontSize: 14, marginBottom: 8 }}>
            👥 好友 ({friends.length})
          </div>
          {friends.length === 0 ? (
            <div className="cc-empty">
              <div className="cc-empty-text">还没有好友，搜索用户添加吧</div>
            </div>
          ) : (
            friends.map((f: any) => (
              <div key={f.id} className="cc-deck-item" style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                <div style={{ display: 'flex', alignItems: 'center', gap: 10 }}>
                  <div style={{
                    width: 36, height: 36, borderRadius: '50%',
                    background: 'linear-gradient(135deg, var(--teal), var(--teal-dark))',
                    display: 'flex', alignItems: 'center', justifyContent: 'center',
                    fontSize: 14, color: '#fff', fontWeight: 600
                  }}>
                    {(f.friendNickname || '?').charAt(0).toUpperCase()}
                  </div>
                  <div>
                    <div style={{ color: 'var(--text-primary)', fontSize: 14 }}>{f.friendNickname}</div>
                    <div style={{ color: 'var(--text-secondary)', fontSize: 11 }}>{f.friendEmail}</div>
                  </div>
                </div>
                <button className="btn btn-outline-style" style={{ padding: '4px 12px', fontSize: 12 }}
                  onClick={() => { setShowChallengeModal(f); setChallengeDeckId(decks[0]?.id || null); }}>
                  ⚔️ 挑战
                </button>
              </div>
            ))
          )}
        </div>
      )}

      {/* Tab: 📜 战报 */}
      {tab === 'history' && (
        <div className="cc-arena-tab">
          {history.length === 0 ? (
            <div className="cc-empty">
              <div className="cc-empty-icon">📜</div>
              <div className="cc-empty-text">暂无战报</div>
            </div>
          ) : (
            history.map((b: any) => {
              const isChallenger = b.challengerId === user.id;
              const opponentName = isChallenger ? b.defenderNickname : b.challengerNickname;
              const myScore = isChallenger ? b.challengerScore : b.defenderScore;
              const oppScore = isChallenger ? b.defenderScore : b.challengerScore;
              return (
                <div key={b.id} className="cc-history-item" style={{ display: 'flex', alignItems: 'center', gap: 12 }}>
                  <div style={{ fontSize: 20, width: 32, textAlign: 'center' }}>{getResultIcon(b)}</div>
                  <div style={{ flex: 1 }}>
                    <div style={{ color: 'var(--text-primary)', fontSize: 14 }}>
                      vs {opponentName}
                    </div>
                    <div style={{ color: 'var(--text-secondary)', fontSize: 12 }}>
                      {myScore} - {oppScore} · {getResultText(b)}
                    </div>
                    <div style={{ color: 'var(--text-secondary)', fontSize: 11 }}>
                      {b.completedAt ? new Date(b.completedAt).toLocaleString() : (b.createdAt ? new Date(b.createdAt).toLocaleString() : '')}
                    </div>
                  </div>
                  <div style={{ color: getTrophyColor(b), fontSize: 14, fontWeight: 600 }}>
                    {getTrophyText(b)}
                  </div>
                </div>
              );
            })
          )}
        </div>
      )}

      {/* Tab: 🏆 排名 */}
      {tab === 'rank' && (
        <div className="cc-arena-tab">
          {/* 我的段位 */}
          {rank && (
            <div className="arena-rank-card" style={{
              background: 'linear-gradient(135deg, var(--card), var(--bg))',
              borderRadius: 'var(--radius-lg)', padding: 20, margin: '0 0 16px',
              border: '1px solid var(--teal-glow)',
              boxShadow: '0 4px 20px rgba(20, 184, 166, 0.15)'
            }}>
              <div style={{ display: 'flex', alignItems: 'center', gap: 12 }}>
                <div style={{
                  width: 48, height: 48, borderRadius: '50%',
                  background: 'linear-gradient(135deg, var(--teal), var(--teal-dark))',
                  display: 'flex', alignItems: 'center', justifyContent: 'center',
                  fontSize: 22, flexShrink: 0,
                  boxShadow: '0 0 12px var(--teal-glow)'
                }}>
                  {rank.tierIcon}
                </div>
                <div style={{ flex: 1 }}>
                  <div style={{ color: 'var(--teal)', fontSize: 15, fontWeight: 600 }}>{rank.tierName}</div>
                  <div style={{ display: 'flex', gap: 16, marginTop: 4 }}>
                    <span style={{ color: 'var(--text-primary)', fontSize: 13 }}>🏆 {rank.trophies}</span>
                    <span style={{ color: 'var(--text-secondary)', fontSize: 13 }}>排名 #{rank.rank}</span>
                    <span style={{ color: 'var(--success)', fontSize: 13 }}>胜 {rank.wins}</span>
                    <span style={{ color: 'var(--danger)', fontSize: 13 }}>负 {rank.losses}</span>
                  </div>
                </div>
              </div>
            </div>
          )}

          {/* 排行榜 */}
          <div style={{ color: 'var(--text-secondary)', fontSize: 14, marginBottom: 8 }}>
            🏆 排行榜 TOP 100
          </div>
          {leaderboard.map((entry: any, i: number) => (
            <div key={i} className="cc-deck-item" style={{ display: 'flex', alignItems: 'center', gap: 10 }}>
              <div style={{
                width: 28, textAlign: 'center',
                color: i < 3 ? ['#ffd700', '#c0c0c0', '#cd7f32'][i] : 'var(--text-secondary)',
                fontSize: i < 3 ? 16 : 13, fontWeight: 600
              }}>
                {i < 3 ? ['🥇', '🥈', '🥉'][i] : `#${i + 1}`}
              </div>
              <div style={{
                width: 32, height: 32, borderRadius: '50%',
                background: 'linear-gradient(135deg, var(--teal), var(--teal-dark))',
                display: 'flex', alignItems: 'center', justifyContent: 'center',
                fontSize: 12, color: '#fff', fontWeight: 600, flexShrink: 0
              }}>
                {'?'}
              </div>
              <div style={{ flex: 1 }}>
                <div style={{ color: 'var(--text-primary)', fontSize: 14 }}>{'玩家' + entry.trophies}</div>
                <div style={{ color: 'var(--text-secondary)', fontSize: 11 }}>
                  {entry.tierIcon} {entry.tierName} · 胜率 {entry.wins + entry.losses > 0 ? Math.round(entry.wins / (entry.wins + entry.losses) * 100) : 0}%
                </div>
              </div>
              <div style={{ color: 'var(--teal)', fontSize: 14, fontWeight: 600 }}>🏆 {entry.trophies}</div>
            </div>
          ))}
          {leaderboard.length === 0 && (
            <div className="cc-empty">
              <div className="cc-empty-text">暂无排名数据</div>
            </div>
          )}
        </div>
      )}

      {/* Tab: 🗓️ 赛季 */}
      {tab === 'season' && (
        <div className="cc-arena-tab">
          {seasonData?.active ? (
            <div style={{
              background: 'linear-gradient(135deg, #1a1a2e, #16213e)',
              borderRadius: 'var(--radius-lg)', padding: 20, marginBottom: 16,
              border: '1px solid var(--teal-glow)',
              boxShadow: '0 4px 20px rgba(20, 184, 166, 0.15)'
            }}>
              <div style={{ textAlign: 'center', marginBottom: 12 }}>
                <div style={{ fontSize: 14, color: 'var(--teal)', fontWeight: 600 }}>
                  {seasonData.titleCn || `S${seasonData.seasonNumber} 赛季`}
                </div>
                {seasonData.daysLeft > 0 && (
                  <div style={{ fontSize: 32, fontWeight: 700, color: 'var(--text-primary)', margin: '8px 0' }}>
                    {seasonData.daysLeft}天
                  </div>
                )}
                <div style={{ fontSize: 13, color: 'var(--text-secondary)' }}>
                  {seasonData.daysLeft > 0 ? `还剩 ${seasonData.daysLeft} 天` : '赛季已结束'}
                </div>
              </div>
              {seasonData.progress > 0 && (
                <div style={{
                  height: 6, background: 'rgba(255,255,255,0.08)', borderRadius: 3,
                  overflow: 'hidden', marginBottom: 12
                }}>
                  <div style={{
                    width: `${Math.min(100, seasonData.progress * 100)}%`,
                    height: '100%',
                    background: 'linear-gradient(90deg, var(--teal), var(--teal-light))',
                    borderRadius: 3,
                    transition: 'width 0.5s ease'
                  }} />
                </div>
              )}
              <div style={{ fontSize: 12, color: 'var(--text-secondary)', textAlign: 'center' }}>
                {seasonData.startDate?.substring(0, 10)} ~ {seasonData.endDate?.substring(0, 10)}
              </div>
            </div>
          ) : (
            <div className="cc-empty">
              <div className="cc-empty-text">暂无赛季信息</div>
            </div>
          )}

          {/* 赛季奖励预览 */}
          <div style={{ color: 'var(--text-secondary)', fontSize: 14, marginBottom: 8, fontWeight: 600 }}>
            🎁 赛季奖励预览
          </div>
          {(seasonData?.rewards || []).map((tier: any, i: number) => (
            <div key={i} className="cc-deck-item" style={{
              display: 'flex', justifyContent: 'space-between', alignItems: 'center',
              borderLeft: `3px solid ${['#ff8c00', '#a335ee', '#a335ee', '#0070dd', '#0070dd', '#9d9d9d'][i]}`
            }}>
              <div>
                <div style={{ color: 'var(--text-primary)', fontSize: 14, fontWeight: 600 }}>
                  {['传说', '钻石', '白金', '黄金', '白银', '青铜'][i] || tier.tier}
                </div>
                <div style={{ color: 'var(--text-secondary)', fontSize: 12 }}>
                  {tier.legendaryCards ? `传说卡×${tier.legendaryCards} ` : ''}
                  {tier.epicCards ? `史诗卡×${tier.epicCards} ` : ''}
                  {tier.rareCards ? `稀有卡×${tier.rareCards} ` : ''}
                  {tier.commonCards ? `普通卡×${tier.commonCards} ` : ''}
                  {tier.randomCards > 0 ? `随机卡×${tier.randomCards} ` : ''}
                  ✨{tier.stardust}
                </div>
              </div>
              <div style={{ fontSize: 20 }}>{['👑', '💎', '🥇', '🥈', '🥉', '🪙'][i]}</div>
            </div>
          ))}

          {/* 当前用户赛季奖励状态 */}
          {seasonReward?.seasonReward?.settled && (
            <div style={{
              background: 'linear-gradient(135deg, #1a1a2e, #16213e)',
              borderRadius: 'var(--radius-lg)', padding: 16, marginTop: 16,
              border: '1px solid var(--teal-glow)'
            }}>
              <div style={{ textAlign: 'center', marginBottom: 12 }}>
                <div style={{ fontSize: 14, color: 'var(--teal)', fontWeight: 600 }}>本赛季结算</div>
                <div style={{ fontSize: 24, fontWeight: 700, color: 'var(--text-primary)', margin: '8px 0' }}>
                  {seasonReward.seasonReward.finalRank}
                </div>
                <div style={{ fontSize: 13, color: 'var(--text-secondary)' }}>
                  奖杯: {seasonReward.seasonReward.finalTrophies}
                </div>
              </div>
              {!seasonReward.seasonReward.rewardClaimed ? (
                <button className="btn btn-primary" style={{ width: '100%', padding: '10px' }}
                  onClick={async () => {
                    const res = await cardApi.claimSeasonReward();
                    if (res.code === 200) {
                      alert(`🎉 领取成功！获得 ${res.data.stardustGained} 星尘`);
                      loadSeason();
                    } else {
                      alert(res.message || '领取失败');
                    }
                  }}>
                  领取赛季奖励
                </button>
              ) : (
                <div style={{ textAlign: 'center', color: 'var(--success)', fontSize: 13 }}>
                  ✅ 奖励已领取
                </div>
              )}
            </div>
          )}
        </div>
      )}

      {/* 挑战弹窗 - 选择卡组 */}
      {showChallengeModal && (
        <div className="card-detail-overlay" onClick={() => setShowChallengeModal(null)}>
          <div className="card-detail-modal" onClick={e => e.stopPropagation()} style={{ maxWidth: 380 }}>
            <button className="cd-close" onClick={() => setShowChallengeModal(null)}>✕</button>
            <div style={{ textAlign: 'center', marginBottom: 16 }}>
              <div style={{ fontSize: 32, marginBottom: 8 }}>⚔️</div>
              <div style={{ color: 'var(--text-primary)', fontSize: 16, fontWeight: 600 }}>
                挑战 {showChallengeModal.friendNickname}
              </div>
            </div>
            <div style={{ marginBottom: 16 }}>
              <div style={{ color: 'var(--text-secondary)', fontSize: 13, marginBottom: 8 }}>选择卡组</div>
              {decks.length === 0 ? (
                <div style={{ color: 'var(--text-secondary)', fontSize: 13 }}>暂无卡组，请先在卡牌页面创建</div>
              ) : (
                decks.map((d: any) => (
                  <label key={d.id} style={{
                    display: 'flex', alignItems: 'center', gap: 8, padding: '10px 12px',
                    borderRadius: 'var(--radius-sm)', marginBottom: 6,
                    background: challengeDeckId === d.id ? 'var(--hover-bg)' : 'var(--card)',
                    border: challengeDeckId === d.id ? '1px solid var(--teal)' : '1px solid var(--border)',
                    cursor: 'pointer'
                  }}>
                    <input type="radio" name="deck" checked={challengeDeckId === d.id}
                      onChange={() => setChallengeDeckId(d.id)} />
                    <span style={{ color: 'var(--text-primary)', fontSize: 14 }}>{d.name}</span>
                    <span style={{ color: 'var(--text-secondary)', fontSize: 12, marginLeft: 'auto'}}>
                      {d.cardCount || '?'}张
                    </span>
                  </label>
                ))
              )}
            </div>
            <button className="btn btn-primary" style={{ width: '100%', padding: '10px' }}
              onClick={handleChallenge} disabled={!challengeDeckId}>
              发送挑战
            </button>
          </div>
        </div>
      )}

      {/* 迎战弹窗 - 选择卡组 + 输入结果 */}
      {showAcceptModal && (
        <div className="card-detail-overlay" onClick={() => setShowAcceptModal(null)}>
          <div className="card-detail-modal" onClick={e => e.stopPropagation()} style={{ maxWidth: 400 }}>
            <button className="cd-close" onClick={() => setShowAcceptModal(null)}>✕</button>
            <div style={{ textAlign: 'center', marginBottom: 16 }}>
              <div style={{ fontSize: 32, marginBottom: 8 }}>⚔️</div>
              <div style={{ color: 'var(--text-primary)', fontSize: 16, fontWeight: 600 }}>
                迎战 {showAcceptModal.challengerNickname}
              </div>
            </div>

            <div style={{ marginBottom: 16 }}>
              <div style={{ color: 'var(--text-secondary)', fontSize: 13, marginBottom: 8 }}>选择卡组</div>
              {decks.map((d: any) => (
                <label key={d.id} style={{
                  display: 'flex', alignItems: 'center', gap: 8, padding: '10px 12px',
                  borderRadius: 'var(--radius-sm)', marginBottom: 6,
                  background: acceptDeckId === d.id ? 'var(--hover-bg)' : 'var(--card)',
                  border: acceptDeckId === d.id ? '1px solid var(--teal)' : '1px solid var(--border)',
                  cursor: 'pointer'
                }}>
                  <input type="radio" name="adeck" checked={acceptDeckId === d.id}
                    onChange={() => setAcceptDeckId(d.id)} />
                  <span style={{ color: 'var(--text-primary)', fontSize: 14 }}>{d.name}</span>
                  <span style={{ color: 'var(--text-secondary)', fontSize: 12, marginLeft: 'auto'}}>
                    {d.cardCount || '?'}张
                  </span>
                </label>
              ))}
            </div>

            <div style={{ marginBottom: 16 }}>
              <div style={{ color: 'var(--text-secondary)', fontSize: 13, marginBottom: 8 }}>你的答题成绩</div>
              <div style={{ display: 'flex', gap: 8, marginBottom: 8 }}>
                <div style={{ flex: 1 }}>
                  <div style={{ color: 'var(--text-secondary)', fontSize: 11, marginBottom: 4 }}>答对数</div>
                  <input type="number" min={0} max={50} value={acceptScore}
                    onChange={e => setAcceptScore(parseInt(e.target.value) || 0)}
                    style={{
                      width: '100%', padding: '8px 10px', borderRadius: 'var(--radius-sm)',
                      border: '1px solid var(--border)', background: 'var(--input-bg)',
                      color: 'var(--text)', fontSize: 14, boxSizing: 'border-box'
                    }} />
                </div>
                <div style={{ flex: 1 }}>
                  <div style={{ color: 'var(--text-secondary)', fontSize: 11, marginBottom: 4 }}>正确率 %</div>
                  <input type="number" min={0} max={100} value={acceptAccuracy}
                    onChange={e => setAcceptAccuracy(parseFloat(e.target.value) || 0)}
                    style={{
                      width: '100%', padding: '8px 10px', borderRadius: 'var(--radius-sm)',
                      border: '1px solid var(--border)', background: 'var(--input-bg)',
                      color: 'var(--text)', fontSize: 14, boxSizing: 'border-box'
                    }} />
                </div>
                <div style={{ flex: 1 }}>
                  <div style={{ color: 'var(--text-secondary)', fontSize: 11, marginBottom: 4 }}>难度 (1-5)</div>
                  <input type="number" min={1} max={5} value={acceptDifficulty}
                    onChange={e => setAcceptDifficulty(parseInt(e.target.value) || 3)}
                    style={{
                      width: '100%', padding: '8px 10px', borderRadius: 'var(--radius-sm)',
                      border: '1px solid var(--border)', background: 'var(--input-bg)',
                      color: 'var(--text)', fontSize: 14, boxSizing: 'border-box'
                    }} />
                </div>
              </div>
            </div>

            <button className="btn btn-primary" style={{ width: '100%', padding: '10px' }}
              onClick={handleAcceptBattle} disabled={!acceptDeckId}>
              开始对战
            </button>
          </div>
        </div>
      )}

      {error && <div style={{ color: 'var(--danger)', textAlign: 'center', padding: 16 }}>{error}</div>}
    </div>
  );
}
