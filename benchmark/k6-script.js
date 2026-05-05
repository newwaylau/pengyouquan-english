/**
 * 朋友圈英语 — k6 压力测试脚本
 *
 * 用法：
 *   1. 安装 k6: https://k6.io/docs/getting-started/installation/
 *   2. 启动后端服务（本地或 Docker）
 *   3. 运行：k6 run benchmark/k6-script.js
 *
 * 如果后端不在 localhost:8080，设置环境变量：
 *   K6_HOST=http://your-host:port k6 run benchmark/k6-script.js
 */

import http from 'k6/http';
import { sleep, check, group } from 'k6';
import { Rate, Trend } from 'k6/metrics';

// ---- 自定义指标 ----
const errorRate = new Rate('errors');
const apiLatency = new Trend('api_latency_ms');

// ---- 并发场景配置 ----
export const options = {
  stages: [
    { duration: '30s', target: 10 },   // 逐步升温到 10 并发
    { duration: '1m',  target: 50 },   // 持续升到 50 并发
    { duration: '30s', target: 0 },    // 逐渐下降
  ],
  thresholds: {
    http_req_duration: ['p(95)<2000'], // 95% 请求应在 2s 内
    errors: ['rate<0.1'],              // 错误率低于 10%
  },
};

// ---- 基础 URL（可覆盖） ----
const BASE_URL = __ENV.K6_HOST || 'http://localhost:8080';

export default function () {
  group('健康检查', () => {
    const res = http.get(`${BASE_URL}/api/health`);
    const ok = check(res, {
      '健康检查 状态 200': (r) => r.status === 200,
    });
    errorRate.add(!ok);
    apiLatency.add(res.timings.duration);
  });

  group('剧集列表', () => {
    const res = http.get(`${BASE_URL}/api/shows`);
    const ok = check(res, {
      '剧集列表 状态 200': (r) => r.status === 200,
      '返回 JSON (shows)': (r) => r.json('code') === 200,
    });
    errorRate.add(!ok);
    apiLatency.add(res.timings.duration);
  });

  group('随机句子', () => {
    // 随机取 10 条句子
    const res = http.get(`${BASE_URL}/api/random?limit=10`);
    const ok = check(res, {
      '随机句子 状态 200': (r) => r.status === 200,
      '返回数组': (r) => {
        try { return Array.isArray(JSON.parse(r.body).data); } catch { return false; }
      },
    });
    errorRate.add(!ok);
    apiLatency.add(res.timings.duration);
  });

  group('搜索（选中）', () => {
    const res = http.get(`${BASE_URL}/api/search?q=the`);
    const ok = check(res, {
      '搜索 状态 200': (r) => r.status === 200,
    });
    errorRate.add(!ok);
    apiLatency.add(res.timings.duration);
  });

  sleep(1);
}
