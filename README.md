# 朋友圈英语 🎬

> 看美剧学英语 — 从美剧字幕中学英语的听写练习平台

基于真实美剧字幕的英语听写练习工具。支持多种练习模式、TTS 语音播放、错题本、管理后台、字幕导入等功能。

## 功能列表

- **中译英模式** — 看中文写英文，逐词输入
- **听写模式** — 播放英文原音或 TTS，逐词听写
- **逐词输入** — 自动跳转、Backspace 回退、空格跳转
- **TTS + 原音播放** — 6 种音色可选，播放速度可调
- **错题本** — 错误自动收集、跨设备同步、分页、不重复出题
- **关键词搜索** — 中英文双向搜索，300ms 防抖
- **句子浏览** — 剧集·季·集三级浏览，编号跳转
- **设置面板** — 模式/剧集/音色/速度/自动播放全部可配并持久化
- **微信登录** — 微信 OAuth 集成
- **管理后台** — 仪表盘、用户管理、趋势图、API 监控、数据导出

## 技术栈

- 后端：Spring Boot 3 + Spring Security + JPA + MySQL 8.0 + Redis 7
- 前端：React 18 + TypeScript + Vite + Ant Design
- 容器化：Docker + Docker Compose
- 构建：Maven + npm

## 快速开始

### Docker Compose 一键启动

```bash
# 克隆项目
git clone https://github.com/your-username/pengyouquan-english.git
cd pengyouquan-english

# 配置环境变量
cp .env.example docker/.env
# 按需编辑 docker/.env

# 一键启动
docker compose -f docker/docker-compose.yml --env-file docker/.env up -d --build
```

启动后访问：
- 前端：http://localhost:3000
- 后端 API：http://localhost:8080

### 本地开发

**后端：**
```bash
cd backend
# 确保 MySQL + Redis 已运行
mvn spring-boot:run
```

**前端：**
```bash
cd frontend
npm install
npm run dev
```

## 项目结构

```
pengyouquan-english/
├── backend/                  # Spring Boot 后端
│   ├── src/main/java/        # Java 源码
│   ├── src/test/java/        # 单元测试
│   ├── data/audio/           # 音频文件
│   └── pom.xml
├── frontend/                 # React + Vite 前端
│   ├── src/                  # TypeScript/React 源码
│   ├── public/               # 静态资源
│   └── package.json
├── docker/                   # Docker 部署配置
│   ├── docker-compose.yml
│   ├── Dockerfile.backend
│   └── Dockerfile.frontend
├── database/                 # 数据库迁移脚本
├── weapp/                    # 微信小程序
├── start.sh                  # 一键启动脚本
├── backup.sh                 # 数据库备份脚本
├── .env.example              # 环境变量模板
├── PROGRESS.md               # 开发进度清单
├── COMPATIBILITY.md          # 兼容性说明
└── MIGRATION.md              # 生产迁移指南
```

## 开发指南

### 分支规范
- `main` — 稳定版本
- 功能分支命名：`feat/功能名` 或 `fix/问题描述`

### 提交规范
```
type(scope): 描述
```
类型：`feat` / `fix` / `docs` / `style` / `refactor` / `test` / `chore`

### 代码风格
- Java：Spring Boot 官方风格
- TypeScript：ESLint + 严格模式
- 所有代码写中文注释

## License

MIT License

---

**朋友圈英语** — 让看美剧成为学英语的习惯。
