# 朋友圈英语 — 迁移说明文档

本文档说明如何将朋友圈英语从开发环境迁移到生产部署，以及从旧版本迁移的步骤。

---

## 目录

1. [开发环境 → Docker 部署](#1-开发环境--docker-部署)
2. [从旧版迁移](#2-从旧版迁移)
3. [HTTPS 配置](#3-https-配置)
4. [日常运维](#4-日常运维)
5. [故障排查](#5-故障排查)

---

## 1. 开发环境 → Docker 部署

### 前置条件

- **Docker** ≥ 24.0（[安装指南](https://docs.docker.com/engine/install/)）
- **Docker Compose** ≥ 2.20

### 步骤

#### Step 1：克隆项目

```bash
git clone <项目仓库地址>
cd pengyouquan-english
```

#### Step 2：配置环境变量

```bash
# 复制环境变量模板
cp .env.example docker/.env

# 编辑 docker/.env，修改以下关键值：
# - MYSQL_ROOT_PASSWORD：改为强密码
# - JWT_SECRET：改为随机字符串
# - WECHAT_APP_ID / WECHAT_APP_SECRET：如有需要
vim docker/.env
```

#### Step 3：一键启动

```bash
# 赋予执行权限
chmod +x start.sh backup.sh

# 一键启动
./start.sh
```

#### Step 4：验证部署

```bash
# 查看容器状态
docker compose -f docker/docker-compose.yml ps

# 查看启动日志
docker compose -f docker/docker-compose.yml logs -f

# 测试 API
curl http://localhost:8080/api/health

# 浏览器访问
open http://localhost:3000
```

#### Step 5：数据初始化（首次部署）

如果数据库为空，需要通过 Flyway 自动建表：

```bash
# Flyway 会在后端启动时自动执行迁移脚本
# 查看日志确认迁移成功
docker logs pengyouquan-backend
```

如果看到 `Flyway migration completed` 表示建表成功。

> **如果没有 Flyway 迁移文件**，可以手动导入：
> ```bash
> # 获取 SQL 文件（从旧版数据库导出）
> docker exec -i pengyouquan-mysql mysql -uroot -p"${MYSQL_ROOT_PASSWORD}" pengyouquan_english < /path/to/init.sql
> ```

---

## 2. 从旧版迁移

### 2.1 迁移数据库

```bash
# 1. 在旧版服务器上导出数据库
mysqldump -h旧版IP -uroot -p \
  --databases pengyouquan_english \
  --add-drop-database \
  --routines \
  --triggers \
  --single-transaction \
  > old_database.sql

# 2. 将 SQL 文件传输到新服务器
scp old_database.sql user@新服务器:/tmp/

# 3. 在新服务器上导入
docker cp /tmp/old_database.sql pengyouquan-mysql:/tmp/
docker exec -i pengyouquan-mysql mysql -uroot -p"${MYSQL_ROOT_PASSWORD}" < /tmp/old_database.sql
```

### 2.2 迁移字幕数据（如有）

```bash
# 如果旧版有自定义字幕文件
tar -czf subtitles_backup.tar.gz /path/to/subtitles/
scp subtitles_backup.tar.gz user@新服务器:/tmp/
tar -xzf /tmp/subtitles_backup.tar.gz -C /path/to/new/subtitles/
```

### 2.3 版本兼容性检查

| 旧版 | 新版 | 兼容性 |
|------|------|--------|
| v1.x（单体应用）| v2.x（前后端分离）| 数据库兼容，前端需重新部署 |
| v2.x 之前 | v2.x 当前 | API 接口变化，建议全量迁移 |

迁移后请完整测试：注册 → 登录 → 练习 → 错题本 → 设置持久化。

---

## 3. HTTPS 配置

> 强烈建议生产环境启用 HTTPS。推荐使用 Nginx 反代 + Certbot（Let's Encrypt）。

### 3.1 Nginx 反代配置

在宿主机（或独立 Nginx 容器）上配置 Nginx 反代：

```nginx
# /etc/nginx/sites-available/pengyouquan

# 前端（静态文件）
server {
    listen 80;
    server_name your-domain.com;
    return 301 https://$server_name$request_uri;
}

server {
    listen 443 ssl http2;
    server_name your-domain.com;

    # SSL 证书（由 Certbot 自动管理）
    ssl_certificate     /etc/letsencrypt/live/your-domain.com/fullchain.pem;
    ssl_certificate_key /etc/letsencrypt/live/your-domain.com/privkey.pem;
    ssl_protocols       TLSv1.2 TLSv1.3;
    ssl_ciphers         HIGH:!aNULL:!MD5;

    # 安全头部
    add_header X-Frame-Options DENY;
    add_header X-Content-Type-Options nosniff;
    add_header X-XSS-Protection "1; mode=block";

    # 前端静态文件（反向代理到前端容器）
    location / {
        proxy_pass http://localhost:3000;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
    }

    # 后端 API
    location /api/ {
        proxy_pass http://localhost:8080;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;

        # 超时设置（长连接）
        proxy_connect_timeout 60s;
        proxy_read_timeout 60s;
    }

    # 静态资源缓存
    location ~* \.(jpg|jpeg|png|gif|ico|css|js|woff2?)$ {
        expires 30d;
        add_header Cache-Control "public, immutable";
    }
}
```

### 3.2 使用 Certbot 申请证书

```bash
# 安装 Certbot
sudo apt install certbot python3-certbot-nginx

# 申请证书（自动配置 Nginx）
sudo certbot --nginx -d your-domain.com

# 证书自动续期（Certbot 默认已添加 systemd timer）
sudo certbot renew --dry-run  # 测试续期

# 查看证书状态
sudo certbot certificates
```

### 3.3 Docker Compose + Nginx 方案（可选）

也可以在 Docker Compose 中添加 Nginx 服务：

```yaml
nginx:
  image: nginx:alpine
  container_name: pengyouquan-nginx
  ports:
    - "80:80"
    - "443:443"
  volumes:
    - ./docker/nginx.conf:/etc/nginx/conf.d/default.conf:ro
    - ./docker/ssl:/etc/nginx/ssl:ro
    - certbot-data:/var/www/certbot
  depends_on:
    - frontend
    - backend
```

对应的 `docker/nginx.conf` 配置可参考 3.1 节。

### 3.4 生产环境安全检查清单

- [ ] 已修改 MySQL 默认密码
- [ ] 已修改 JWT 密钥
- [ ] 已启用 HTTPS
- [ ] 已配置防火墙（仅开放 80/443）
- [ ] 已关闭服务器远程 root 登录
- [ ] 已配置日志轮转
- [ ] 已配置自动备份
- [ ] 已修改 docker-compose.yml 中暴露的端口（可选改为非标准端口防扫描）

---

## 4. 日常运维

### 4.1 查看日志

```bash
# 所有服务日志
docker compose -f docker/docker-compose.yml logs -f

# 仅后端日志
docker compose -f docker/docker-compose.yml logs -f backend

# 仅查看最近100行
docker compose -f docker/docker-compose.yml logs --tail=100 backend

# 后端应用日志（挂载的 volume）
ls /var/lib/docker/volumes/pengyouquan_backend-logs/_data/
```

### 4.2 备份与恢复

```bash
# 手动备份
./backup.sh

# 定时备份（crontab）
# 每天凌晨3点执行
0 3 * * * /path/to/pengyouquan-english/backup.sh

# 从备份恢复
tar -xzf backups/pengyouquan_english_20260101_030000.tar.gz -C /tmp/
cat /tmp/pengyouquan_english_20260101_030000.sql | docker exec -i pengyouquan-mysql mysql -uroot -p"${MYSQL_ROOT_PASSWORD}"
```

### 4.3 更新部署

```bash
# 拉取最新代码
git pull

# 重新构建并启动
docker compose -f docker/docker-compose.yml up -d --build

# 查看更新日志
docker compose -f docker/docker-compose.yml logs -f backend
```

### 4.4 停止与清理

```bash
# 停止服务
docker compose -f docker/docker-compose.yml down

# 停止并删除卷（谨慎！会丢失数据）
docker compose -f docker/docker-compose.yml down -v

# 清理未使用的镜像
docker image prune -a
```

---

## 5. 故障排查

### 容器无法启动

```bash
# 查看错误日志
docker compose -f docker/docker-compose.yml logs

# 检查端口是否被占用
sudo lsof -i :8080
sudo lsof -i :3000
sudo lsof -i :3306

# 检查 Docker 磁盘空间
docker system df

# 检查 Docker 守护进程状态
systemctl status docker
```

### 数据库连接失败

```bash
# 手动测试数据库连接
docker exec -it pengyouquan-mysql mysql -uroot -p"${MYSQL_ROOT_PASSWORD}" -e "SELECT 1"

# 检查数据库是否存在
docker exec -it pengyouquan-mysql mysql -uroot -p"${MYSQL_ROOT_PASSWORD}" -e "SHOW DATABASES;"

# 如果 Flyway 迁移失败，检查迁移状态
docker logs pengyouquan-backend | grep flyway
```

### 前端打不开

```bash
# 检查 Nginx 是否正常运行
docker exec -it pengyouquan-frontend nginx -t

# 检查前端静态文件
docker exec -it pengyouquan-frontend ls /usr/share/nginx/html

# 测试后端是否可达（从前端容器）
docker exec -it pengyouquan-frontend wget -qO- http://backend:8080/api/health
```

### 其他常见问题

| 问题 | 原因 | 解决 |
|------|------|------|
| 端口冲突 | 本地已有服务占用端口 | 修改 docker/.env 中的端口映射 |
| 磁盘空间不足 | 日志或数据卷过大 | 清理日志 `docker compose logs --tail=0` 或 `docker system prune` |
| 迁移 SQL 太大 | 默认 max_allowed_packet 太小 | 在 MySQL 容器中执行 `SET GLOBAL max_allowed_packet=512M` |
| 跨域问题 | Nginx 未正确配置代理 | 检查 proxy_set_header 配置是否正确 |
