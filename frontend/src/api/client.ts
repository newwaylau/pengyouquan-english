/** API 客户端 */
const BASE = '';

let token: string | null = localStorage.getItem('token');

export function setToken(t: string) { token = t; localStorage.setItem('token', t); }
export function getToken() { return token; }
export function clearToken() { token = null; localStorage.removeItem('token'); }

async function request(path: string, options: RequestInit = {}) {
  const headers: Record<string, string> = { 'Content-Type': 'application/json' };
  if (token) headers['Authorization'] = `Bearer ${token}`;
  const res = await fetch(`${BASE}${path}`, { ...options, headers });
  const data = await res.json();
  if (data.code === 401) { clearToken(); window.location.hash = '#/login'; }
  return data;
}

export const api = {
  // 认证
  sendCode: (email: string) =>
    request('/api/auth/send-code', { method: 'POST', body: JSON.stringify({ email }) }),
  register: (data: { email: string; phone: string; code: string; password: string; invitedBy?: string }) =>
    request('/api/auth/register', { method: 'POST', body: JSON.stringify(data) }),
  login: (account: string, password: string) =>
    request('/api/auth/login', { method: 'POST', body: JSON.stringify({ account, password }) }),
  me: () => request('/api/auth/me'),

  // 剧集
  shows: () => request('/api/shows'),
  random: (params: string) => request(`/api/random?${params}`),
  search: (q: string) => request(`/api/search?q=${encodeURIComponent(q)}`),
  sentence: (id: number) => request(`/api/sentence/${id}`),

  // 练习
  logPractice: (data: any) =>
    request('/api/practice/log', { method: 'POST', body: JSON.stringify(data) }),
  flagSentence: (sentenceId: number, flag: boolean) =>
    request(`/api/sentences/${sentenceId}/flag`, { method: 'POST', body: JSON.stringify({ flag }) }),

  // 错题
  wrongSentences: (params?: { showName?: string; sortBy?: string; sortDir?: string; includeMastered?: boolean }) => {
    const q = params ? Object.entries(params)
      .filter(([_, v]) => v !== undefined && v !== null && v !== '')
      .map(([k, v]) => `${encodeURIComponent(k)}=${encodeURIComponent(String(v))}`)
      .join('&') : '';
    return request(`/api/wrong-sentences${q ? '?' + q : ''}`);
  },
  wrongSentenceShows: () => request('/api/wrong-sentences/shows'),
  masterSentence: (sid: number) =>
    request(`/api/wrong-sentences/${sid}/master`, { method: 'PUT' }),
  unmasterSentence: (sid: number) =>
    request(`/api/wrong-sentences/${sid}/unmaster`, { method: 'PUT' }),
  wrongPractice: (limit?: number) =>
    request(`/api/wrong-sentences/practice${limit ? `?limit=${limit}` : ''}`),
  removeWrong: (sid: number) =>
    request(`/api/wrong-sentences/${sid}`, { method: 'DELETE' }),
  wrongSentencesGrouped: () => request('/api/wrong-sentences/grouped'),
  wrongSentencesWithReview: () => request('/api/wrong-sentences/with-review'),
  updateWrongReview: (data: { sentenceId: number; correct: boolean }) =>
    request('/api/wrong-sentences/review', { method: 'POST', body: JSON.stringify(data) }),
  wrongSentenceStats: () => request('/api/wrong-sentences/stats'),

  // 设置
  getSettings: () => request('/api/settings'),
  saveSettings: (data: any) =>
    request('/api/settings', { method: 'PUT', body: JSON.stringify(data) }),

  // 资料
  updateProfile: (data: any) =>
    request('/api/users/profile', { method: 'PUT', body: JSON.stringify(data) }),

  // 统计
  stats: () => request('/api/users/me/stats'),

  // 修改密码
  updatePassword: (data: { oldPassword: string; newPassword: string }) =>
    request('/api/auth/password', { method: 'PUT', body: JSON.stringify(data) }),

  // 忘记密码
  forgotPasswordSendCode: (email: string) =>
    request('/api/auth/forgot-password/send-code', { method: 'POST', body: JSON.stringify({ email }) }),
  resetPassword: (data: { email: string; code: string; password: string }) =>
    request('/api/auth/reset-password', { method: 'POST', body: JSON.stringify(data) }),

  // 管理后台——句子报告
  adminFlaggedSentences: () =>
    request('/api/admin/sentence-flags'),
  adminDisableSentence: (sentenceId: number, reason?: string) =>
    request(`/api/admin/sentences/${sentenceId}/disable`, { method: 'PUT', body: JSON.stringify({ reason: reason || '' }) }),
  adminEnableSentence: (sentenceId: number) =>
    request(`/api/admin/sentences/${sentenceId}/enable`, { method: 'PUT' }),

  // 连接测试
  ping: () => request('/api/auth/ping'),

  // Demo 问候接口
  demoGreeting: (name?: string) =>
    request(`/api/demo/greeting?name=${encodeURIComponent(name || '访客')}`),
};

// 游戏化相关 API
export const gameApi = {
  getPrestige: () => request('/api/game/prestige'),
  getDailyChallenge: () => request('/api/game/daily-challenge'),
  submitAnswer: (challengeId: number, questionId: number, answer: string) =>
    request(`/api/game/daily-challenge/${challengeId}/submit`, { method: 'POST', body: JSON.stringify({ questionId, answer }) }),
  completeChallenge: (challengeId: number) =>
    request(`/api/game/daily-challenge/${challengeId}/complete`, { method: 'POST', body: JSON.stringify({}) }),
  getLeaderboard: (period: string = 'today') =>
    request(`/api/game/leaderboard?period=${period}`),
  getHistory: () => request('/api/game/history'),
};
