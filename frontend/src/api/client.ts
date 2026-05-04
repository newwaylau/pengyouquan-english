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
  register: (email: string, password: string) =>
    request('/api/auth/register', { method: 'POST', body: JSON.stringify({ email, password }) }),
  login: (email: string, password: string) =>
    request('/api/auth/login', { method: 'POST', body: JSON.stringify({ email, password }) }),
  me: () => request('/api/auth/me'),

  // 剧集
  shows: () => request('/api/shows'),
  random: (params: string) => request(`/api/random?${params}`),
  search: (q: string) => request(`/api/search?q=${encodeURIComponent(q)}`),
  sentence: (id: number) => request(`/api/sentence/${id}`),

  // 练习
  logPractice: (data: any) =>
    request('/api/practice/log', { method: 'POST', body: JSON.stringify(data) }),

  // 错题
  wrongSentences: () => request('/api/wrong-sentences'),
  removeWrong: (sid: number) =>
    request(`/api/wrong-sentences/${sid}`, { method: 'DELETE' }),

  // 设置
  getSettings: () => request('/api/settings'),
  saveSettings: (data: any) =>
    request('/api/settings', { method: 'PUT', body: JSON.stringify(data) }),

  // 资料
  updateProfile: (data: any) =>
    request('/api/users/profile', { method: 'PUT', body: JSON.stringify(data) }),

  // 统计
  stats: () => request('/api/users/me/stats'),
};
