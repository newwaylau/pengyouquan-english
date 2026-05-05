import { defineConfig } from '@playwright/test';

export default defineConfig({
  testDir: './tests',
  timeout: 60000,
  retries: 1,
  use: {
    // 前端开发服务器地址
    baseURL: 'http://localhost:3000',
    // 所有测试使用 Chromium
    browserName: 'chromium',
    // 视口大小（桌面端默认）
    viewport: { width: 1280, height: 800 },
    // 截取失败截图
    screenshot: 'only-on-failure',
    // 录制失败视频
    video: 'retain-on-failure',
    // 允许跨域请求
    bypassCSP: true,
    // 全局导航超时
    navigationTimeout: 30000,
  },
  // 测试报告
  reporter: [
    ['list'],
    ['html', { outputFolder: 'playwright-report' }],
  ],
  // 全局超时
  globalTimeout: 300000,
  // 禁止并行跑，避免状态冲突
  workers: 1,
});
