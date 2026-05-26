import React, { useState, useEffect } from 'react';
import './guild.css';

const API_BASE = '';
function getToken() { return localStorage.getItem('token'); }
async function apiFetch(path: string, options: RequestInit = {}) {
  const headers: Record<string, string> = { 'Content-Type': 'application/json' };
  const token = getToken();
  if (token) headers['Authorization'] = `Bearer ${token}`;
  const res = await fetch(`${API_BASE}${path}`, { ...options, headers });
  const data = await res.json();
  if (data.code === 401) { localStorage.removeItem('token'); window.location.reload(); }
  return data;
}

export default function GuildPage({ user, onNavigate }: { user?: any; onNavigate?: (page: string, data?: any) => void }) {
  const [tab, setTab] = useState<string>('overview');
  const [loading, setLoading] = useState(true);
  const [guildData, setGuildData] = useState<any>(null);
  const [inGuild, setInGuild] = useState(false);

  // Search state
  const [searchQuery, setSearchQuery] = useState('');
  const [searchResults, setSearchResults] = useState<any[]>([]);

  // Create state
  const [createName, setCreateName] = useState('');
  const [createDesc, setCreateDesc] = useState('');
  const [presetNames, setPresetNames] = useState<string[]>([]);

  // Leaderboard
  const [leaderboard, setLeaderboard] = useState<any[]>([]);

  // League
  const [leagueData, setLeagueData] = useState<any[]>([]);

  // War state
  const [warData, setWarData] = useState<any>(null);
  const [contributeCount, setContributeCount] = useState<number>(1);

  // Trade state
  const [tradeReceived, setTradeReceived] = useState<any[]>([]);
  const [tradeSent, setTradeSent] = useState<any[]>([]);
  const [tradeHistory, setTradeHistory] = useState<any[]>([]);
  const [tradeDailyLimit, setTradeDailyLimit] = useState<any>(null);
  const [showTradeModal, setShowTradeModal] = useState<any>(null);
  const [tradeReceiverId, setTradeReceiverId] = useState<number | null>(null);
  const [tradeRequestedCardId, setTradeRequestedCardId] = useState<number | null>(null);
  const [tradeOfferedCardId, setTradeOfferedCardId] = useState<number | null>(null);

  useEffect(() => {
    loadMyGuild();
    loadLeaderboard();
  }, []);

  async function loadMyGuild() {
    setLoading(true);
    const res = await apiFetch('/api/guilds/mine');
    if (res.code === 200) {
      const data = res.data;
      setInGuild(data.inGuild);
      setGuildData(data);
      if (data.presetNames) setPresetNames(data.presetNames);
    }
    setLoading(false);
  }

  async function loadLeaderboard() {
    const res = await apiFetch('/api/guilds/leaderboard');
    if (res.code === 200) setLeaderboard(res.data.leaderboard || []);
  }

  async function handleSearch() {
    const res = await apiFetch(`/api/guilds/search?q=${encodeURIComponent(searchQuery)}`);
    if (res.code === 200) setSearchResults(res.data.guilds || []);
  }

  async function handleCreate() {
    if (!createName.trim()) return;
    if (user && user.stardust < 500) { alert('星尘不足500，无法创建公会'); return; }
    const res = await apiFetch('/api/guilds/create', {
      method: 'POST',
      body: JSON.stringify({ name: createName.trim(), description: createDesc.trim() }),
    });
    if (res.code === 200) {
      alert('公会创建成功！');
      loadMyGuild();
      setTab('overview');
    } else {
      alert(res.message);
    }
  }

  async function handleJoin(guildId: number) {
    const res = await apiFetch(`/api/guilds/${guildId}/join`, { method: 'POST' });
    if (res.code === 200) {
      alert('加入成功！');
      loadMyGuild();
      setTab('overview');
    } else {
      alert(res.message);
    }
  }

  async function handleLeave() {
    if (!confirm('确定要退出公会吗？')) return;
    const res = await apiFetch('/api/guilds/leave', { method: 'POST' });
    if (res.code === 200) {
      setInGuild(false);
      setGuildData(null);
      setTab('search');
    } else {
      alert(res.message);
    }
  }

  async function handleKick(userId: number) {
    if (!confirm('确定要踢出该成员吗？')) return;
    const res = await apiFetch(`/api/guilds/${guildData?.id}/kick/${userId}`, { method: 'POST' });
    if (res.code === 200) {
      alert('已踢出');
      loadMyGuild();
    } else {
      alert(res.message);
    }
  }

  async function handleTransfer(userId: number) {
    if (!confirm('确定要转让会长吗？')) return;
    const res = await apiFetch(`/api/guilds/${guildData?.id}/transfer/${userId}`, { method: 'POST' });
    if (res.code === 200) {
      alert('会长已转让');
      loadMyGuild();
    } else {
      alert(res.message);
    }
  }

  async function loadLeague() {
    const res = await apiFetch('/api/guilds/league');
    if (res.code === 200) setLeagueData(res.data || []);
  }

  async function handleClaimTreasure(treasureId: number) {
    const res = await apiFetch(`/api/guilds/treasure/${treasureId}/claim`, { method: 'POST' });
    if (res.code === 200) {
      alert(`领取成功！获得${res.data.reward}星尘`);
      loadMyGuild();
    } else {
      alert(res.message);
    }
  }

  // ==================== War Functions ====================

  async function loadWar() {
    const res = await apiFetch('/api/guilds/war/status');
    if (res.code === 200) setWarData(res.data);
  }

  async function handleContribute() {
    const res = await apiFetch('/api/guilds/war/contribute', {
      method: 'POST',
      body: JSON.stringify({ cardCount: contributeCount }),
    });
    if (res.code === 200) {
      alert(`贡献成功！共贡献 ${res.data.cardsContributed} 张卡牌`);
      loadWar();
    } else {
      alert(res.message);
    }
  }

  async function handleRecordBattleResult(won: boolean) {
    const res = await apiFetch('/api/guilds/war/battle-result', {
      method: 'POST',
      body: JSON.stringify({ won }),
    });
    if (res.code === 200) {
      alert(`记录成功！本场${won ? '胜利' : '失败'}，还有 ${res.data.battlesLeft} 场剩余`);
      loadWar();
    } else {
      alert(res.message);
    }
  }

  // ==================== Trade Functions ====================

  async function loadTrade() {
    const [rec, sent, hist, limit] = await Promise.all([
      apiFetch('/api/cards/trade/received'),
      apiFetch('/api/cards/trade/sent'),
      apiFetch('/api/cards/trade/history'),
      apiFetch('/api/cards/trade/daily-limit'),
    ]);
    if (rec.code === 200) setTradeReceived(rec.data || []);
    if (sent.code === 200) setTradeSent(sent.data || []);
    if (hist.code === 200) setTradeHistory(hist.data || []);
    if (limit.code === 200) setTradeDailyLimit(limit.data);
  }

  async function handleSendTrade() {
    if (!tradeReceiverId || !tradeRequestedCardId) return;
    const res = await apiFetch('/api/cards/trade/request', {
      method: 'POST',
      body: JSON.stringify({
        receiverId: tradeReceiverId,
        requestedCardId: tradeRequestedCardId,
        offeredCardId: tradeOfferedCardId || undefined,
      }),
    });
    if (res.code === 200) {
      alert('换卡请求已发送！');
      setShowTradeModal(null);
      loadTrade();
    } else {
      alert(res.message);
    }
  }

  async function handleAcceptTrade(tradeId: number) {
    const res = await apiFetch(`/api/cards/trade/${tradeId}/accept`, { method: 'POST' });
    if (res.code === 200) {
      alert(res.data.message || '换卡成功！');
      loadTrade();
      loadMyGuild();
    } else {
      alert(res.message);
    }
  }

  async function handleRejectTrade(tradeId: number) {
    const res = await apiFetch(`/api/cards/trade/${tradeId}/reject`, { method: 'POST' });
    if (res.code === 200) {
      loadTrade();
    } else {
      alert(res.message);
    }
  }

  // ==================== RENDER ====================

  function renderOverview() {
    if (!guildData) return <div className="guild-empty">加载中...</div>;

    const myRole = guildData.myRole || 'member';

    return (
      <div>
        {/* Header */}
        <div className="guild-header-card">
          <div className="guild-name">🏰 {guildData.name}</div>
          <div className="guild-desc">{guildData.description || '暂无宣言'}</div>
          <div style={{ marginBottom: 12 }}>
            <span className={`guild-role-badge ${myRole}`}>
              {myRole === 'leader' ? '👑 会长' : myRole === 'officer' ? '🗡️ 官员' : '🛡️ 成员'}
            </span>
          </div>
          <div className="guild-stats-row">
            <div className="guild-stat-item">
              <div className="guild-stat-value">{guildData.memberCount}</div>
              <div className="guild-stat-label">成员</div>
            </div>
            <div className="guild-stat-item">
              <div className="guild-stat-value">{guildData.totalCardsCollected}</div>
              <div className="guild-stat-label">收集卡牌</div>
            </div>
            <div className="guild-stat-item">
              <div className="guild-stat-value">{guildData.weeklyScore}</div>
              <div className="guild-stat-label">本周积分</div>
            </div>
          </div>
        </div>

        {/* My contribution */}
        <div className="guild-section">
          <div className="guild-section-title">我的贡献</div>
          <div style={{ display: 'flex', gap: 12, fontSize: 13 }}>
            <span>本周答对：{guildData.myWeeklyCorrect || 0}</span>
            <span>本周贡献：{guildData.myWeeklyScore || 0}</span>
          </div>
        </div>

        {/* Top Contributors */}
        {guildData.topContributors && guildData.topContributors.length > 0 && (
          <div className="guild-section">
            <div className="guild-section-title">🏆 贡献排行 Top 3</div>
            <div className="guild-top3">
              {guildData.topContributors.map((t: any, i: number) => {
                const rankClass = i === 0 ? 'gold' : i === 1 ? 'silver' : 'bronze';
                const rankIcon = i === 0 ? '🥇' : i === 1 ? '🥈' : '🥉';
                return (
                  <div key={i} className="guild-top3-item">
                    <div className={`guild-top3-rank ${rankClass}`}>{rankIcon}</div>
                    <span style={{ fontWeight: 500 }}>{t.nickname || '未知'}</span>
                    <span style={{ marginLeft: 'auto', color: 'var(--teal)', fontWeight: 600 }}>
                      {t.weeklyScore}分
                    </span>
                  </div>
                );
              })}
            </div>
          </div>
        )}

        {/* Members */}
        <div className="guild-section">
          <div className="guild-section-title">
            <span>成员 ({guildData.memberCount})</span>
          </div>
          <div className="guild-member-list">
            {guildData.members && guildData.members.map((m: any, i: number) => (
              <div key={i} className="guild-member-item">
                <div className="guild-member-name">
                  <span className={`guild-role-badge ${m.role}`}>
                    {m.role === 'leader' ? '👑' : m.role === 'officer' ? '🗡️' : '🛡️'}
                  </span>
                  {m.nickname || '未知'}
                </div>
                <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
                  <span className="guild-member-score">{m.weeklyScore}分</span>
                  {myRole === 'leader' && m.role !== 'leader' && (
                    <>
                      <button
                        style={{ fontSize: 11, padding: '2px 6px', background: 'var(--info)', color: 'white', border: 'none', borderRadius: 4, cursor: 'pointer' }}
                        onClick={() => handleTransfer(m.userId)}
                      >
                        转让
                      </button>
                      <button
                        style={{ fontSize: 11, padding: '2px 6px', background: 'var(--danger)', color: 'white', border: 'none', borderRadius: 4, cursor: 'pointer' }}
                        onClick={() => handleKick(m.userId)}
                      >
                        踢出
                      </button>
                    </>
                  )}
                </div>
              </div>
            ))}
          </div>
        </div>

        {/* Treasures */}
        {guildData.treasures && guildData.treasures.length > 0 && (
          <div className="guild-section">
            <div className="guild-section-title">🎁 公会宝库</div>
            <div className="guild-treasures">
              {guildData.treasures.map((t: any, i: number) => {
                const chestIcons: Record<string, string> = { bronze: '🥉', silver: '🥈', gold: '🥇' };
                const isUnlocked = t.unlocked;
                const isClaimed = t.claimedCount >= t.maxClaims;
                return (
                  <div key={i} className="guild-treasure-item">
                    <div className="guild-treasure-info">
                      <div className="guild-treasure-name">
                        {chestIcons[t.chestType] || '📦'} {t.milestone}张收集
                      </div>
                      <div className="guild-treasure-progress">
                        已领取 {t.claimedCount}/{t.maxClaims}
                      </div>
                    </div>
                    <button
                      className={`guild-treasure-claim-btn ${!isUnlocked ? 'locked' : isClaimed ? 'claimed' : 'ready'}`}
                      disabled={!isUnlocked || isClaimed}
                      onClick={() => handleClaimTreasure(t.id)}
                    >
                      {!isUnlocked ? '未达成' : isClaimed ? '已领完' : '领取'}
                    </button>
                  </div>
                );
              })}
            </div>
          </div>
        )}

        {/* Territory War */}
        <div className="guild-section">
          <div className="guild-war-card">
            <div className="guild-war-title">⚔️ 领地战</div>
            <div className="guild-war-score">{guildData.weeklyScore} 分</div>
            <div className="guild-war-hint">每周六结算 · 排名前10有奖励</div>
          </div>
        </div>

        {/* Leave button (only for non-leader or empty guild) */}
        <div style={{ textAlign: 'center', marginTop: 12 }}>
          <button
            style={{ padding: '8px 24px', background: 'transparent', color: 'var(--danger)', border: '1px solid var(--danger)', borderRadius: 'var(--radius-sm)', fontSize: 13, cursor: 'pointer' }}
            onClick={handleLeave}
          >
            退出公会
          </button>
        </div>
      </div>
    );
  }

  function renderSearch() {
    return (
      <div>
        <div className="guild-search-box">
          <input
            className="guild-search-input"
            value={searchQuery}
            onChange={e => setSearchQuery(e.target.value)}
            onKeyDown={e => { if (e.key === 'Enter') handleSearch(); }}
            placeholder="搜索公会名..."
          />
          <button className="guild-search-btn" onClick={handleSearch}>搜索</button>
        </div>
        <div className="guild-result-list">
          {searchResults.map((g: any) => (
            <div key={g.id} className="guild-result-item">
              <div className="guild-result-info">
                <div className="guild-result-name">🏰 {g.name}</div>
                <div className="guild-result-detail">
                  会长：{g.leaderName} · 人数：{g.memberCount}/{g.maxMembers} · 周积分：{g.weeklyScore}
                </div>
              </div>
              <button className="guild-join-btn" onClick={() => handleJoin(g.id)}>
                加入
              </button>
            </div>
          ))}
          {searchResults.length === 0 && searchQuery && (
            <div className="guild-empty">未找到公会</div>
          )}
        </div>
      </div>
    );
  }

  function renderCreate() {
    return (
      <div>
        <div className="guild-create-form">
          <div className="guild-section-title">预设名称</div>
          <div style={{ display: 'flex', flexWrap: 'wrap', gap: 6, marginBottom: 8 }}>
            {presetNames.map((name: string) => (
              <button
                key={name}
                style={{
                  padding: '6px 12px',
                  border: `2px solid ${createName === name ? 'var(--teal)' : 'var(--border)'}`,
                  borderRadius: 'var(--radius-sm)',
                  background: createName === name ? 'var(--hover-bg)' : 'var(--card)',
                  color: 'var(--text)',
                  cursor: 'pointer',
                  fontSize: 13,
                  fontWeight: createName === name ? 600 : 400,
                }}
                onClick={() => setCreateName(name)}
              >
                {name}
              </button>
            ))}
          </div>
          <div className="guild-section-title">或自定义名称</div>
          <input
            className="guild-create-input"
            value={createName}
            onChange={e => setCreateName(e.target.value)}
            placeholder="输入公会名..."
            maxLength={50}
          />
          <textarea
            className="guild-create-textarea"
            value={createDesc}
            onChange={e => setCreateDesc(e.target.value)}
            placeholder="公会宣言（可选）"
            maxLength={200}
          />
          <div className="guild-create-hint">
            创建公会需要消耗 500 ⭐ 星尘
            {user && <span> · 当前星尘：{user.stardust || 0}</span>}
          </div>
          <button
            className="guild-create-btn"
            disabled={!createName.trim()}
            onClick={handleCreate}
          >
            创建公会
          </button>
        </div>
      </div>
    );
  }

  function renderLeaderboard() {
    return (
      <div>
        <div className="guild-ranking-list">
          {leaderboard.map((g: any) => {
            let posClass = '';
            if (g.rank === 1) posClass = 'top1';
            else if (g.rank === 2) posClass = 'top2';
            else if (g.rank === 3) posClass = 'top3';
            return (
              <div key={g.id} className="guild-ranking-item">
                <div className={`guild-ranking-pos ${posClass}`}>
                  {g.rank <= 3 ? ['🥇', '🥈', '🥉'][g.rank - 1] : g.rank}
                </div>
                <div className="guild-ranking-name">🏰 {g.name}</div>
                <div className="guild-ranking-stats">
                  <div>{g.weeklyScore}分</div>
                  <div style={{ fontSize: 11 }}>{g.memberCount}人 · {g.leaderName}</div>
                </div>
              </div>
            );
          })}
          {leaderboard.length === 0 && (
            <div className="guild-empty">暂无公会排行</div>
          )}
        </div>
      </div>
    );
  }

  function renderLeague() {
    return (
      <div>
        <div className="guild-section-title" style={{ marginBottom: 12 }}>🏆 公会联赛</div>
        {leagueData.length === 0 ? (
          <div className="guild-empty">暂无联赛数据</div>
        ) : (
          <div className="guild-ranking-list">
            {leagueData.map((g: any) => {
              let posClass = '';
              if (g.rank === 1) posClass = 'top1';
              else if (g.rank === 2) posClass = 'top2';
              else if (g.rank === 3) posClass = 'top3';
              const isMyGuild = inGuild && guildData?.id === g.id;
              return (
                <div key={g.id} className={`guild-ranking-item ${isMyGuild ? 'my-guild' : ''}`}
                  style={isMyGuild ? { borderColor: 'var(--teal)', background: 'rgba(20,184,166,0.08)' } : {}}>
                  <div className={`guild-ranking-pos ${posClass}`}>
                    {g.rank <= 3 ? ['🥇', '🥈', '🥉'][g.rank - 1] : g.rank}
                  </div>
                  <div className="guild-ranking-name">
                    🏰 {g.name}
                    {isMyGuild && <span style={{ color: 'var(--teal)', fontSize: 11, marginLeft: 6 }}>(我的)</span>}
                  </div>
                  <div className="guild-ranking-stats">
                    <div style={{ color: '#ffd700', fontWeight: 600 }}>{g.leagueScore} 分</div>
                    <div style={{ fontSize: 11, color: 'var(--text-secondary)' }}>{g.memberCount}人 · {g.leaderName}</div>
                  </div>
                </div>
              );
            })}
          </div>
        )}
      </div>
    );
  }

  // ⚔️ 部落战 render
  function renderWar() {
    if (!warData) return <div className="guild-empty" style={{padding:24}}>暂无部落战数据</div>;
    const phase = warData.phase || 'preparation';
    const phaseNames: Record<string, string> = {preparation:'备战', battle:'战斗', settlement:'结算'};
    return (
      <div style={{padding:16}}>
        <div className="guild-section-title">⚔️ 部落战 ({phaseNames[phase]})</div>
        {warData.opponent && (
          <div className="card" style={{padding:12,marginBottom:12}}>
            <div style={{fontWeight:600}}>对手: {warData.opponent.guildName || warData.opponent.name}</div>
            <div style={{fontSize:13,color:'var(--text-secondary)'}}>比分: 己方 {warData.ourScore??0} : {warData.theirScore??0} 对方</div>
          </div>
        )}
        {phase === 'preparation' && (
          <div style={{marginTop:12}}>
            <p style={{fontSize:13,color:'var(--text-secondary)',marginBottom:8}}>备战阶段：贡献卡牌增强公会战力</p>
            <div style={{display:'flex',gap:8,alignItems:'center'}}>
              <input type="number" min={1} max={10} value={contributeCount}
                onChange={e=>setContributeCount(Number(e.target.value))}
                style={{width:60,padding:'4px 8px',borderRadius:6,border:'1px solid var(--border)',background:'var(--card)',color:'var(--text)'}} />
              <button className="btn btn-sm" onClick={handleContribute}>贡献</button>
            </div>
          </div>
        )}
        {phase === 'settlement' && warData.reward && (
          <div style={{marginTop:12,padding:12,background:'rgba(255,215,0,0.1)',borderRadius:8}}>
            🎉 获得 {warData.reward.stardust||0} 星尘 + 公会经验！
          </div>
        )}
      </div>
    );
  }

  // 🔄 换卡 render
  function renderTrade() {
    return (
      <div style={{padding:16}}>
        <div className="guild-section-title">🔄 公会换卡</div>
        <div style={{fontSize:12,color:'var(--text-secondary)',marginBottom:12}}>
          今日已换 {tradeDailyLimit?.count||0}/{tradeDailyLimit?.max||3} 次
        </div>
        {tradeReceived.length>0 && (
          <div style={{marginBottom:16}}>
            <div style={{fontWeight:600,fontSize:14,marginBottom:8}}>收到的请求</div>
            {tradeReceived.map((t:any)=>(
              <div key={t.id} className="card" style={{padding:12,marginBottom:8}}>
                <div>来自: {t.requesterName||'?'}</div>
                <div style={{fontSize:12,color:'var(--text-secondary)'}}>请求: {t.requestedCardName||'?'}</div>
                <div style={{display:'flex',gap:8,marginTop:8}}>
                  <button className="btn btn-sm" style={{background:'rgba(34,197,94,0.2)',color:'#22c55e'}}
                    onClick={async()=>{await apiFetch('/api/card-trade/'+t.id+'/accept',{method:'POST'});loadTrade();}}>接受</button>
                  <button className="btn btn-sm" style={{background:'rgba(239,68,68,0.2)',color:'#ef4444'}}
                    onClick={async()=>{await apiFetch('/api/card-trade/'+t.id+'/reject',{method:'POST'});loadTrade();}}>拒绝</button>
                </div>
              </div>
            ))}
          </div>
        )}
        {tradeSent.length>0 && (
          <div>
            <div style={{fontWeight:600,fontSize:14,marginBottom:8}}>已发送</div>
            {tradeSent.map((t:any)=>(
              <div key={t.id} className="card" style={{padding:12,marginBottom:8}}>
                <div>给: {t.receiverName||'?'}</div>
                <div style={{fontSize:12,color:'var(--text-secondary)'}}>{t.requestedCardName||'?'} · {t.status}</div>
              </div>
            ))}
          </div>
        )}
        {tradeReceived.length===0 && tradeSent.length===0 && (
          <div style={{textAlign:'center',padding:24,color:'var(--text-secondary)',fontSize:14}}>暂无换卡记录</div>
        )}
      </div>
    );
  }

  if (loading) {
    return <div className="guild-page"><div className="guild-empty">加载中...</div></div>;
  }

  return (
    <div className="guild-page">
      <div className="guild-tabs">
        <button className={`guild-tab ${tab === 'overview' ? 'active' : ''}`} onClick={() => setTab('overview')}>
          🏰 公会
        </button>
        {!inGuild && (
          <button className={`guild-tab ${tab === 'search' ? 'active' : ''}`} onClick={() => setTab('search')}>
            🔍 搜索
          </button>
        )}
        {!inGuild && (
          <button className={`guild-tab ${tab === 'create' ? 'active' : ''}`} onClick={() => setTab('create')}>
            ✨ 创建
          </button>
        )}
        <button className={`guild-tab ${tab === 'leaderboard' ? 'active' : ''}`} onClick={() => { setTab('leaderboard'); loadLeaderboard(); }}>
          📊 排行
        </button>
        <button className={`guild-tab ${tab === 'league' ? 'active' : ''}`} onClick={() => { setTab('league'); loadLeague(); }}>
          🏆 联赛
        </button>
        {inGuild && (
          <button className={`guild-tab ${tab === 'war' ? 'active' : ''}`} onClick={() => { setTab('war'); loadWar(); }}>
            ⚔️ 部落战
          </button>
        )}
        {inGuild && (
          <button className={`guild-tab ${tab === 'trade' ? 'active' : ''}`} onClick={() => { setTab('trade'); loadTrade(); }}>
            🔄 换卡
          </button>
        )}
      </div>

      {tab === 'overview' && (inGuild ? renderOverview() : (
        <div className="guild-empty">
          <div style={{ textAlign: 'center' }}>
            <div style={{ fontSize: 48, marginBottom: 12 }}>🏰</div>
            <p>你还没有加入公会</p>
            <p style={{ fontSize: 12, color: 'var(--text-secondary)', marginTop: 8 }}>
              搜索公会加入或创建一个新公会
            </p>
            <button
              className="guild-search-btn"
              style={{ marginTop: 12 }}
              onClick={() => setTab('search')}
            >
              搜索公会
            </button>
          </div>
        </div>
      ))}
      {tab === 'search' && renderSearch()}
      {tab === 'create' && renderCreate()}
      {tab === 'leaderboard' && renderLeaderboard()}
      {tab === 'league' && renderLeague()}
      {tab === 'war' && inGuild && renderWar()}
      {tab === 'trade' && inGuild && renderTrade()}
    </div>
  );
}
