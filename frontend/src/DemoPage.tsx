import React, { useState } from 'react';
import { api } from './api/client';
import { IconRocket, IconArrowLeft } from './Icons';

export default function DemoPage({ onBack }: { onBack: () => void }) {
  const [name, setName] = useState('');
  const [greeting, setGreeting] = useState<string | null>(null);
  const [serverTime, setServerTime] = useState<string | null>(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');

  const handleGreet = async () => {
    setLoading(true);
    setError('');
    setGreeting(null);
    setServerTime(null);
    try {
      const r = await api.demoGreeting(name.trim() || undefined);
      if (r.code === 200) {
        setGreeting(r.data.message);
        setServerTime(r.data.serverTime);
      } else {
        setError(r.message || '请求失败');
      }
    } catch (e: any) {
      setError('网络错误：' + (e.message || '未知错误'));
    } finally {
      setLoading(false);
    }
  };

  const handleKeyDown = (e: React.KeyboardEvent) => {
    if (e.key === 'Enter') handleGreet();
  };

  return (
    <div className="demo-page">
      <div className="demo-header">
        <button className="demo-back-btn" onClick={onBack}>
          <IconArrowLeft />
          <span>返回</span>
        </button>
        <h2><IconRocket /> Demo 联调测试</h2>
      </div>

      <div className="demo-card">
        <p className="demo-desc">
          调用后端 <code>GET /api/demo/greeting?name=xxx</code> 接口，
          返回中英文问候语 + 服务器时间。
        </p>

        <div className="demo-input-row">
          <input
            className="demo-input"
            value={name}
            onChange={e => setName(e.target.value)}
            onKeyDown={handleKeyDown}
            placeholder="输入你的名字（可选）"
            disabled={loading}
          />
          <button className="demo-btn" onClick={handleGreet} disabled={loading}>
            {loading ? '请求中...' : '打招呼'}
          </button>
        </div>

        {error && (
          <div className="demo-error">{error}</div>
        )}

        {greeting && (
          <div className="demo-result">
            <div className="demo-result-icon"><IconRocket /></div>
            <div className="demo-result-msg">{greeting}</div>
            {serverTime && (
              <div className="demo-result-time">🕐 服务器时间：{serverTime}</div>
            )}
          </div>
        )}

        <div className="demo-status">
          <span className={`demo-status-dot ${greeting ? 'online' : ''}`} />
          API 状态：{greeting ? '连接正常' : '等待请求'}
        </div>
      </div>
    </div>
  );
}
