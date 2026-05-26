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
};
