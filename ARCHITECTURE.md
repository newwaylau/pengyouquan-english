# 英语剧场 — 架构文档

> 项目架构概览、数据库设计、API 路由、前端组件结构

---

## 1. 整体架构

```
┌─────────────────────────────────────────────────┐
│                   前端 (React + Vite)             │
│  localhost:5173 (dev) / :80 (docker)            │
│                                                  │
│  App.tsx ─► PracticePage / LoginPage / AdminPage │
│           BrowsePage / SearchPage / WrongPage    │
│           SubtitlePage / SettingsPanel           │
│                                                  │
│  api/client.ts ── HTTP fetch ──► /api/*          │
└──────────────────────┬──────────────────────────┘
                       │
              HTTP (JSON, multipart)
                       │
┌──────────────────────▼──────────────────────────┐
│               后端 (Spring Boot 3)                │
│  localhost:8080 (dev) / :8080 (docker)          │
│                                                  │
│  Controller → Service → Repository → JPA → MySQL │
│  JwtAuthFilter (Security) → Spring Security      │
│  TtsService → OpenAI TTS API                     │
│  SubtitleService → SRT/ASS/VTT parser            │
│  RequestLoggingInterceptor → request monitoring  │
└──────┬──────────────────────────┬───────────────┘
       │                          │
       ▼                          ▼
    MySQL 8.0                  Redis 7
  (Flyway migrations)      (session/cache)
```

### 前后端分离
- 前端通过 HTTP JSON API 与后端通信
- 认证采用 JWT Bearer Token
- 开发阶段前端代理到后端 8080 端口
- 生产环境通过 Nginx 反向代理合并到同一域名

---

## 2. 数据库表结构

### 2.1 `users` — 用户表
| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT PK | 用户 ID |
| email | VARCHAR(255) UNIQUE | 邮箱 |
| password | VARCHAR(255) | 加密密码 |
| nickname | VARCHAR(255) | 昵称 |
| role | VARCHAR(20) | 角色：user/admin |
| status | VARCHAR(20) | 状态：active/banned |
| avatar | VARCHAR(500) | 头像 URL |
| openid | VARCHAR(255) UNIQUE | 微信 openid |
| created_at | DATETIME | 注册时间 |
| updated_at | DATETIME | 更新时间 |

### 2.2 `shows` — 剧集表
| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT PK | 剧集 ID |
| name | VARCHAR(255) | 剧集名称（中文） |
| name_en | VARCHAR(255) | 剧集名称（英文） |
| season | INT | 季数 |
| episode | INT | 集数 |
| episode_count | INT | 本集句子数 |
| created_at | DATETIME | 创建时间 |

### 2.3 `sentences` — 句子表
| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT PK | 句子 ID |
| show_id | BIGINT FK → shows | 所属剧集 |
| idx | INT | 在剧集中的序号 |
| english | TEXT | 英文原文 |
| chinese | TEXT | 中文翻译 |
| audio_path | VARCHAR(500) | 音频文件路径（可选） |
| created_at | DATETIME | 创建时间 |

### 2.4 `practice_logs` — 练习记录表
| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT PK | 记录 ID |
| user_id | BIGINT FK → users | 用户 ID |
| sentence_id | BIGINT FK → sentences | 句子 ID |
| mode | VARCHAR(20) | 练习模式 |
| errors | INT | 错误次数 |
| correct | BOOLEAN | 是否完全正确 |
| duration | INT | 用时（秒） |
| practiced_at | DATETIME | 练习时间 |

### 2.5 `wrong_sentences` — 错题表
| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT PK | 记录 ID |
| user_id | BIGINT FK → users | 用户 ID |
| sentence_id | BIGINT FK → sentences | 句子 ID |
| error_count | INT | 累计错误次数 |
| last_wrong_at | DATETIME | 最后错误时间 |

### 2.6 `user_settings` — 用户设置表
| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT PK | 记录 ID |
| user_id | BIGINT FK → users | 用户 ID |
| settings_json | JSON/TEXT | 设置 JSON 字符串 |
| updated_at | DATETIME | 更新时间 |

### 2.7 `subtitle_import_logs` — 字幕导入日志
| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT PK | 记录 ID |
| user_id | BIGINT FK → users | 上传者 |
| filename | VARCHAR(255) | 文件名 |
| format | VARCHAR(10) | 格式：SRT/ASS/VTT |
| show_name | VARCHAR(255) | 识别出的剧集名 |
| total_sentences | INT | 总句子数 |
| status | VARCHAR(20) | 状态：success/failed |
| created_at | DATETIME | 导入时间 |

### 2.8 `system_settings` — 系统设置
| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT PK | 记录 ID |
| setting_key | VARCHAR(100) UNIQUE | 设置键 |
| setting_value | TEXT | 设置值 |
| description | VARCHAR(500) | 描述 |
| updated_at | DATETIME | 更新时间 |

---

## 3. API 路由概览

### 认证 (`/api/auth`)
| 方法 | 路径 | 说明 | 认证 |
|------|------|------|------|
| POST | /api/auth/register | 注册 | 否 |
| POST | /api/auth/login | 登录 | 否 |
| GET | /api/auth/me | 当前用户信息 | 是 |

### 用户 (`/api/users`)
| 方法 | 路径 | 说明 | 认证 |
|------|------|------|------|
| PUT | /api/users/profile | 更新资料 | 是 |
| GET | /api/users/me/stats | 个人统计 | 是 |
| GET | /api/users | 用户列表（管理） | 是(admin) |
| DELETE | /api/users/{id} | 删除用户 | 是(admin) |

### 剧集 (`/api/shows`)
| 方法 | 路径 | 说明 | 认证 |
|------|------|------|------|
| GET | /api/shows | 剧集列表 | 否 |
| GET | /api/random?show=&mode=&exclude= | 随机出题 | 否 |
| GET | /api/search?q= | 搜索句子 | 否 |
| GET | /api/sentence/{id} | 句子详情 | 否 |

### 练习 (`/api/practice`)
| 方法 | 路径 | 说明 | 认证 |
|------|------|------|------|
| POST | /api/practice/log | 提交练习记录 | 是 |

### 错题 (`/api/wrong-sentences`)
| 方法 | 路径 | 说明 | 认证 |
|------|------|------|------|
| GET | /api/wrong-sentences | 错题列表 | 是 |
| DELETE | /api/wrong-sentences/{id} | 移除错题 | 是 |
| DELETE | /api/wrong-sentences | 清空错题本 | 是 |

### 设置 (`/api/settings`)
| 方法 | 路径 | 说明 | 认证 |
|------|------|------|------|
| GET | /api/settings | 获取设置 | 是 |
| PUT | /api/settings | 保存设置 | 是 |

### TTS (`/api/tts`)
| 方法 | 路径 | 说明 | 认证 |
|------|------|------|------|
| GET | /api/tts/voices | 可用音色列表 | 否 |
| GET | /api/tts/play/{sentenceId}?voice=&speed= | 播放 TTS | 否 |

### 字幕导入 (`/api/subtitle`)
| 方法 | 路径 | 说明 | 认证 |
|------|------|------|------|
| POST | /api/subtitle/upload | 上传字幕文件 | 是 |
| GET | /api/subtitle/history | 导入历史 | 是 |
| DELETE | /api/subtitle/{id} | 删除导入记录 | 是 |
| GET | /api/subtitle/formats | 支持格式 | 否 |

### 管理后台 (`/api/admin`)
| 方法 | 路径 | 说明 | 认证 |
|------|------|------|------|
| GET | /api/admin/dashboard | 仪表盘数据 | 是(admin) |
| GET | /api/admin/users | 用户列表 | 是(admin) |
| PUT | /api/admin/users/{id}/status | 禁用/启用用户 | 是(admin) |
| GET | /api/admin/practice-trend | 练习趋势 | 是(admin) |
| GET | /api/admin/accuracy-trend | 正确率趋势 | 是(admin) |
| GET | /api/admin/mode-distribution | 模式分布 | 是(admin) |
| GET | /api/admin/show-ranking | 剧集热度排行 | 是(admin) |
| GET | /api/admin/monitor/api-requests | API 请求量 | 是(admin) |

### 系统状态
| 方法 | 路径 | 说明 | 认证 |
|------|------|------|------|
| GET | /api/health | 健康检查 | 否 |

---

## 4. 前端组件树

```
App.tsx
├── LoginPage.tsx          — 登录/注册页面
├── PracticePage.tsx       — 核心练习页面（逐词输入、播放、进度）
│   └── SettingsPanel.tsx  — 设置面板（模式/音色/速度/自动播放）
├── BrowsePage.tsx         — 剧集浏览模式（句子列表 + 编号跳转）
├── SearchPage.tsx         — 句子搜索（中英文关键词 + 防抖）
├── WrongPage.tsx          — 错题本（列表/移除/清空/点击练习）
├── AdminPage.tsx          — 管理后台（仪表盘/图表/用户管理）
└── SubtitlePage.tsx       — 字幕导入页面

共享层:
├── api/client.ts          — API 客户端（fetch 封装 + JWT 自动注入）
├── index.css              — 全局样式
└── main.tsx               — 应用入口
```

### 数据流
1. **登录流程**: LoginPage → /api/auth/login → 获取 JWT → 保存到 localStorage → 重定向到练习页
2. **练习流程**: PracticePage → /api/random?show=&mode= → 获取句子 → 逐词输入 → 提交 → /api/practice/log
3. **设置流程**: PracticePage → SettingsPanel → /api/settings (GET/PUT) → 实时生效
4. **错题流程**: WrongPage → /api/wrong-sentences (GET/DELETE) → 点击跳转句子到 PracticePage
5. **搜索流程**: SearchPage → /api/search?q= (300ms 防抖) → 点击结果跳转到 PracticePage

---

## 5. TTS 语音服务

- 后端集成 OpenAI TTS API，支持 6 种音色
- 缓存已生成的音频文件到 `backend/data/audio/`
- 异步生成 + 流式返回
- 支持播放速度调节（0.5x ~ 1.5x）
- 原音播放优先（存在音频文件时播放原音，否则 fallback 到 TTS）

---

## 6. 安全设计

- **认证**: JWT Bearer Token，无状态会话
- **密码**: BCrypt 加密存储
- **授权**: 控制器层通过 `@CurrentUserId` 注解和角色校验
- **CORS**: 全放通（开发）/ Nginx 限制（生产）
- **CSRF**: 禁用（前后端分离 + JWT）
- **XSS**: React 默认转义输出

---

## 7. Docker 部署

```yaml
Services:
  mysql:8.0      # 数据库（健康检查 + 数据持久化）
  redis:7-alpine # 缓存（健康检查 + 数据持久化）
  backend        # Spring Boot（依赖 MySQL/Redis 健康）
  frontend       # Nginx 静态文件服务
```

详见 `docker/docker-compose.yml` 和 `MIGRATION.md`。
