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

  // 设置
  getSettings: () => request('/api/settings'),
  saveSettings: (data: any) =>
    request('/api/settings', { method: 'PUT', body: JSON.stringify(data) }),

  // 资料
  updateProfile: (data: any) =>
    request('/api/users/profile', { method: 'PUT', body: JSON.stringify(data) }),

  // 统计
  stats: () => request('/api/users/me/stats'),

  // 字幕导入
  subtitleUpload: (formData: FormData) => {
    const headers: Record<string, string> = {};
    const token = localStorage.getItem('token');
    if (token) headers['Authorization'] = `Bearer ${token}`;
    return fetch('/api/subtitle/upload', { method: 'POST', body: formData, headers }).then(r => r.json());
  },
  subtitleHistory: () => request('/api/subtitle/history'),
  subtitleDelete: (id: number) => request(`/api/subtitle/${id}`, { method: 'DELETE' }),
  subtitleFormats: () => request('/api/subtitle/formats'),
  subtitleBatchImport: (directoryPath: string) =>
    request('/api/subtitle/batch-import', { method: 'POST', body: JSON.stringify({ directoryPath }) }),
};
