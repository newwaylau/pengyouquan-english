/** 卡牌系统 API 客户端 */
const BASE = '';
const TOKEN_KEY = 'token';

function getToken() {
  return localStorage.getItem(TOKEN_KEY);
}

async function request(path: string, options: RequestInit = {}) {
  const headers: Record<string, string> = { 'Content-Type': 'application/json' };
  const token = getToken();
  if (token) headers['Authorization'] = `Bearer ${token}`;
  const res = await fetch(`${BASE}${path}`, { ...options, headers });
  return res.json();
}

export const cardApi = {
  getCards: async (showId?: number) => {
    const params = showId ? `?showId=${showId}` : '';
    return request(`/api/cards${params}`);
  },
  getMyCards: async (showId?: number) => {
    const params = showId ? `?showId=${showId}` : '';
    return request(`/api/cards/my${params}`);
  },
  grantPack: async (showId: number, accuracy: number) => {
    return request('/api/cards/grant-pack', {
      method: 'POST',
      body: JSON.stringify({ showId, accuracy }),
    });
  },
  saveDeck: async (name: string, cardIds: number[]) => {
    return request('/api/decks', {
      method: 'POST',
      body: JSON.stringify({ name, cardIds }),
    });
  },
  getDecks: async () => {
    return request('/api/decks');
  },

  // 好友系统
  searchUsers: async (query: string) =>
    request(`/api/friends/search?q=${encodeURIComponent(query)}`),
  sendFriendRequest: async (friendId: number) =>
    request('/api/friends/request', { method: 'POST', body: JSON.stringify({ friendId }) }),
  acceptFriendRequest: async (friendId: number) =>
    request('/api/friends/accept', { method: 'POST', body: JSON.stringify({ friendId }) }),
  getFriends: async () => request('/api/friends'),
  getPendingRequests: async () => request('/api/friends/pending'),

  // 对战系统
  challengePlayer: async (defenderId: number, deckId: number) =>
    request('/api/battle/challenge', { method: 'POST', body: JSON.stringify({ defenderId, deckId }) }),
  getPendingBattles: async () => request('/api/battle/pending'),
  acceptBattle: async (battleId: number, deckId: number, result: { score: number; accuracy: number; avgDifficulty: number }) =>
    request(`/api/battle/${battleId}/accept`, { method: 'POST', body: JSON.stringify({ deckId, ...result }) }),
  getBattleHistory: async () => request('/api/battle/history'),
  getRank: async () => request('/api/battle/rank'),
  getLeaderboard: async () => request('/api/battle/leaderboard'),

  // 宝箱系统
  getChests: async () => request('/api/chests'),
  claimChest: async (chestId: number) =>
    request(`/api/chests/claim/${chestId}`, { method: 'POST', body: JSON.stringify({}) }),

  // 星尘系统
  disenchantCard: async (cardId: number) =>
    request('/api/cards/disenchant', { method: 'POST', body: JSON.stringify({ cardId }) }),
  craftCard: async (cardId: number) =>
    request('/api/cards/craft', { method: 'POST', body: JSON.stringify({ cardId }) }),
  getStardust: async () => request('/api/cards/stardust'),

  // 装备系统
  getEquipment: async () => request('/api/equipment'),
  getMyGear: async () => request('/api/equipment/mine'),
  equipItem: async (equipmentId: number, slot: string) =>
    request(`/api/equipment/equip-by-id/${equipmentId}/${slot}`, { method: 'POST' }),
  unequipItem: async (slot: string) =>
    request(`/api/equipment/unequip/${slot}`, { method: 'POST' }),
  upgradeEquipment: async (userEquipmentId: number) =>
    request(`/api/equipment/upgrade/${userEquipmentId}`, { method: 'POST' }),
  rerollEquipment: async (userEquipmentId: number) =>
    request(`/api/equipment/reroll/${userEquipmentId}`, { method: 'POST' }),
  getEquipmentStats: async () => request('/api/equipment/stats'),

  // 英雄系统
  getHeroes: async () => request('/api/heroes'),
  selectHero: async (heroId: number) =>
    request(`/api/heroes/select/${heroId}`, { method: 'POST' }),
  getActiveHero: async () => request('/api/heroes/active'),
  upgradeHeroSkill: async () =>
    request('/api/heroes/upgrade-skill', { method: 'POST' }),

  // 赛季系统
  getCurrentSeason: async () => request('/api/season/current'),
  getSeasonHistory: async () => request('/api/season/history'),
  settleSeason: async () => request('/api/season/settle', { method: 'POST' }),
  claimSeasonReward: async () => request('/api/season/claim', { method: 'POST' }),
  getSeasonRewards: async () => request('/api/season/rewards'),

  // 金卡系统
  craftGoldenCard: async (cardId: number) =>
    request('/api/cards/craft-golden', { method: 'POST', body: JSON.stringify({ cardId }) }),
  getGoldenCards: async () => request('/api/cards/golden'),
  getCraftableGoldenCards: async () => request('/api/cards/craftable-golden'),

  // 成就系统
  getAchievements: async () => request('/api/achievements'),
  claimAchievement: async (achievementId: number) =>
    request(`/api/achievements/claim/${achievementId}`, { method: 'POST' }),
  getAchievementStats: async () => request('/api/achievements/stats'),

  // 赛季排行
  getSeasonRanking: async () => request('/api/season/ranking'),
  getSeasonTop100: async () => request('/api/season/ranking/top100'),

  // 公会联赛
  getGuildLeague: async () => request('/api/guilds/league'),
  getGuildLeagueInfo: async (guildId: number) => request(`/api/guilds/${guildId}/league`),

  // ===== P2 Features =====

  // 赛季倒计时
  getSeasonCountdown: async () => request('/api/season/countdown'),

  // 好友切磋
  getFriendsWithDetails: async () => request('/api/friends/list'),
  sendFriendChallenge: async (friendId: number) =>
    request(`/api/friends/challenge/${friendId}`, { method: 'POST' }),
  acceptFriendChallenge: async (challengeId: number) =>
    request(`/api/friends/challenge/${challengeId}/accept`, { method: 'POST' }),
  rejectFriendChallenge: async (challengeId: number) =>
    request(`/api/friends/challenge/${challengeId}/reject`, { method: 'POST' }),
  getPendingChallenges: async () => request('/api/friends/challenges/pending'),
  getAcceptedChallenges: async () => request('/api/friends/challenges/accepted'),

  // 公会部落战
  getGuildWarStatus: async () => request('/api/guilds/war/status'),
  contributeWarCards: async (cardCount: number) =>
    request('/api/guilds/war/contribute', { method: 'POST', body: JSON.stringify({ cardCount }) }),
  recordWarBattleResult: async (won: boolean) =>
    request('/api/guilds/war/battle-result', { method: 'POST', body: JSON.stringify({ won }) }),

  // 公会换卡
  sendTradeRequest: async (receiverId: number, requestedCardId: number, offeredCardId?: number) => {
    const body: any = { receiverId, requestedCardId };
    if (offeredCardId) body.offeredCardId = offeredCardId;
    return request('/api/cards/trade/request', { method: 'POST', body: JSON.stringify(body) });
  },
  acceptTradeRequest: async (tradeId: number) =>
    request(`/api/cards/trade/${tradeId}/accept`, { method: 'POST' }),
  rejectTradeRequest: async (tradeId: number) =>
    request(`/api/cards/trade/${tradeId}/reject`, { method: 'POST' }),
  getReceivedTradeRequests: async () => request('/api/cards/trade/received'),
  getSentTradeRequests: async () => request('/api/cards/trade/sent'),
  getTradeHistory: async () => request('/api/cards/trade/history'),
  getTradeDailyLimit: async () => request('/api/cards/trade/daily-limit'),
};
