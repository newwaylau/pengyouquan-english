# 英语剧场 — E2E 测试

使用 [Playwright](https://playwright.dev/) 进行端到端浏览器测试。

## 前置条件

- 项目已启动（`docker compose up -d` 或本地运行）
- 前端运行在 `http://localhost:3000`
- 后端 API 可用

## 安装依赖

```bash
cd e2e
npm install
npx playwright install chromium
```

## 运行测试

```bash
# 无头模式（CI 默认）
npm test

# 有头模式（观察浏览器操作）
npm run test:headed

# 调试模式（逐步执行）
npm run test:debug
```

## 测试说明

| 文件 | 场景 | 覆盖内容 |
|------|------|----------|
| `tests/practice-flow.spec.ts` | #158 完整练习流程 | 注册→登录→选剧集→逐词输入→提交→正确/错误反馈→下一句 |
| `tests/auth-settings.spec.ts` | #159 注册→登录→设置持久化 | 注册新用户→登录→打开设置→切换模式→保存→刷新→验证持久化 |
| `tests/search-practice-wrong.spec.ts` | #160 搜索→练习→错题 | 关键词搜索→点击结果跳转练习→提交错误答案→错题本出现该句子 |

## 关键假设

- 测试使用 `test@example.com` / `password123` 作为测试账号
- 数据库中已有剧集和句子数据（SRT 已导入）
- 每次测试前会注册新用户以确保干净的初始状态
