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
};
