# 英语剧场 🎬

看美剧学英语 — 中译英/听写练习工具

## 功能

- **两种练习模式**：中译英 / 听写（逐词输入）
- **TTS 语音**：6 种音色（美式/英式/澳式男女声）
- **原音播放**：支持剧集原声音频
- **错题本**：自动收录错题，支持复习/清空
- **句子搜索**：中英文关键词搜索
- **浏览模式**：按剧集浏览句子列表
- **设置面板**：模式/音色/速度/自动播放/原音优先
- **专注模式**：无干扰练习
- **管理后台**：仪表盘/用户管理/图表统计/API监控/数据导出
- **微信登录**：支持 Web 和微信小程序

## 技术栈

| 层 | 技术 |
|----|------|
| 前端 | React 19 + TypeScript + Vite + Recharts |
| 后端 | Spring Boot 3 + JPA + Flyway |
| 数据库 | MySQL 8.0 + Redis 7 |
| 部署 | Docker Compose + Nginx |

## 快速开始

```bash
# 一键启动
docker compose -f docker/docker-compose.yml --env-file docker/.env up -d

# 访问
# 前端：http://localhost:3000
# 后端：http://localhost:8080
# 管理后台：登录后导航栏点击"管理"
```

## 本地开发

```bash
# 后端
cd backend && mvn spring-boot:run

# 前端
cd frontend && npm install && npm run dev
```

## 项目结构

```
pengyouquan-english/
├── backend/          # Spring Boot 后端 (8个Controller)
├── frontend/         # React 前端 (9个页面组件)
├── weapp/            # 微信小程序 (5个页面)
├── docker/           # Docker/Docker Compose/Nginx
├── start.sh          # 一键启动脚本
├── backup.sh         # 数据库备份脚本
├── MIGRATION.md      # 部署迁移文档
└── PROGRESS.md       # 项目进度
```
