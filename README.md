# 朋友圈英语 🎬

> 看美剧学英语 — 从美剧字幕中学英语的听写练习平台

基于真实美剧字幕的英语听写练习工具。支持多种练习模式（中译英、听写）、TTS 语音播放、错题本、管理后台、字幕导入等功能。

## ✨ 功能列表

### Phase 1：基础搭建
- Spring Boot 3 后端 + React + Vite 前端
- Docker Compose 一键部署（MySQL + Redis + Backend + Frontend）
- JWT 认证 + CORS 跨域 + 统一异常处理 + 统一响应格式
- Flyway 数据库迁移 + Logback 日志

### Phase 2：用户系统
- 邮箱注册/登录、JWT Token 认证
- 用户资料修改、统计、删除、搜索、禁用
- 微信登录集成、设置持久化

### Phase 3：剧集系统
- 剧集列表加载（含句子数）
- 剧集 · 季 · 集三级联动 + 全部剧集随机出题
- 浏览模式（句子列表 + 编号跳转）
- 中英文关键词搜索（300ms 防抖）
- 句子详情 + 剧集切换保留进度

### Phase 4：练习系统（核心）
- 逐词输入框（自动跳转、Backspace 回退、空格跳转）
- 撇号词/长难词自动预填 + 每句随机提示 1 词
- 两次纠错机制（首次错误 → 提示重试 → 揭示答案）
- 中译英模式 / 听写模式（中文模糊 + 快捷键切换）
- TTS 6 种音色可切换 + 原音播放 + 播放速度控制
- 快捷键系统（专注模式、播放、下一句等）
- 练习记录提交 + localStorage 历史持久化（最近 200 条）
- 出题排除历史（不重复）、剧集筛选出题

### Phase 5：设置面板
- 练习模式 pill 切换、剧集选择下拉框
- 音色选择（默认 Ryan UK Male，6 种可选）
- 播放速度（0.5x / 0.75x / 1x / 1.5x）
- 自动播放（原音/TTS）、语音自动播放勾选
- 设置持久化到服务端 + 重置按钮

### Phase 6：错题本
- 错误自动加入错题本、去重
- 错题列表显示句子内容 + 剧集名 + 错误次数
- 单条移除 / 全部清空 / 点击跳转练习
- 跨设备同步 + 分页 + 不重复出题

### Phase 7：微信小程序
- 待开发 ❌

### Phase 8：管理后台
- 仪表盘总览（总练习数、今日练习、正确率、错题数）
- 剧集概览（句子数列表）、用户管理（禁/启操作）
- 练习量趋势图（日/周/月）、正确率趋势图
- 练习模式分布饼图、剧集热度排行
- 监控：API 请求量、响应时间、错误日志（开发中）

### Phase 9：字幕导入系统
- SRT / ASS / VTT 字幕解析导入
- 剧集名称自动识别 + 去重
- 导入进度反馈 + 文件删除/重新导入

### Phase 10-12（迭代中）
- 测试环节、Docker 部署完善、UI 打磨（响应式、深色模式等）

## 🛠️ 技术栈

| 层级 | 技术 |
|------|------|
| 后端框架 | Spring Boot 3.2.5 + Spring Security + JPA |
| 数据库 | MySQL 8.0 + Flyway 迁移 |
| 缓存 | Redis 7 |
| 认证 | JWT (jjwt 0.12.5) |
| 前端 | React 18 + TypeScript + Vite |
| UI 组件 | Ant Design 6 + Ant Design Mobile |
| 图表 | Recharts |
| 容器化 | Docker + Docker Compose |
| 构建工具 | Maven + npm |

## 🚀 快速开始

### 前置条件
- Docker 和 Docker Compose（推荐）
- 或：Java 17+、Node.js 20+、MySQL 8.0、Redis 7

### Docker Compose 一键启动

```bash
# 1. 克隆项目
git clone https://github.com/your-username/pengyouquan-english.git
cd pengyouquan-english

# 2. 创建环境变量文件
cp .env.example docker/.env
# 编辑 docker/.env 修改密码和密钥

# 3. 一键启动
chmod +x start.sh
./start.sh

# 或手动启动
docker compose -f docker/docker-compose.yml --env-file docker/.env up -d --build
```

启动后访问：
- 前端：http://localhost:3000
- 后端 API：http://localhost:8080

### 本地开发

**后端：**
```bash
cd backend
# 确保 MySQL 和 Redis 已运行
mvn spring-boot:run
```

**前端：**
```bash
cd frontend
npm install
npm run dev
```

## 📦 项目结构

```
pengyouquan-english/
├── backend/                     # Spring Boot 后端
│   ├── src/main/java/.../
│   │   ├── config/             # 配置（安全、CORS、异常处理等）
│   │   ├── controller/         # REST 控制器
│   │   ├── dto/                # 数据传输对象
│   │   ├── model/              # 实体模型
│   │   ├── repository/         # JPA 仓库
│   │   ├── security/           # JWT 认证
│   │   └── service/            # 业务逻辑
│   ├── src/main/resources/
│   │   └── db/migration/       # Flyway 迁移脚本
│   ├── data/audio/             # 音频文件目录
│   └── pom.xml
├── frontend/                    # React + Vite 前端
│   ├── src/
│   │   ├── api/client.ts       # API 客户端
│   │   ├── App.tsx             # 应用入口 + 路由
│   │   ├── PracticePage.tsx    # 练习页
│   │   ├── LoginPage.tsx       # 登录页
│   │   ├── SettingsPanel.tsx   # 设置面板
│   │   ├── AdminPage.tsx       # 管理后台
│   │   ├── BrowsePage.tsx      # 浏览模式
│   │   ├── SearchPage.tsx      # 搜索页
│   │   ├── WrongPage.tsx       # 错题本
│   │   └── SubtitlePage.tsx    # 字幕导入
│   ├── public/                 # 静态资源
│   ├── dist/                   # 构建输出
│   └── package.json
├── docker/                      # Docker 部署配置
│   ├── docker-compose.yml
│   ├── Dockerfile.backend
│   ├── Dockerfile.frontend
│   └── .env
├── database/                    # 数据库迁移
├── weapp/                       # 微信小程序（开发中）
├── start.sh                     # 一键启动脚本
├── backup.sh                    # 数据库备份脚本
├── .env.example                 # 环境变量模板
└── MIGRATION.md                 # 生产环境迁移指南
```

## 📸 截图

> 截图待补充

## 🧑‍💻 开发指南

### 分支规范
- `main` — 稳定版本
- 功能分支命名：`feat/功能名` 或 `fix/问题描述`

### 提交规范
```
type(scope): 描述
```
类型：`feat` / `fix` / `docs` / `style` / `refactor` / `test` / `chore`

### 代码风格
- Java：遵循 Spring Boot 官方风格
- TypeScript/React：ESLint + TypeScript 严格模式
- 中文注释

## 📄 License

MIT License

Copyright (c) 2024 pengyouquan

---

**朋友圈英语** — 让看美剧成为学英语的习惯。
