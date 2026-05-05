# 📊 朋友圈英语 — 压力测试

使用 [k6](https://k6.io) 进行 API 压测。

## 前置条件

### 1. 安装 k6

```bash
# macOS
brew install k6

# Ubuntu/Debian
sudo gpg -k
sudo gpg --no-default-keyring --keyring /usr/share/keyrings/k6-archive-keyring.gpg --keyserver hkp://keyserver.ubuntu.com:80 --recv-keys C5AD17C747E3415A3642D57D77C6C491D6AC1D69
echo "deb [signed-by=/usr/share/keyrings/k6-archive-keyring.gpg] https://dl.k6.io/deb stable main" | sudo tee /etc/apt/sources.list.d/k6.list
sudo apt-get update
sudo apt-get install k6

# Windows (PowerShell)
winget install k6

# Docker
docker pull grafana/k6
```

### 2. 启动后端

确保后端服务在 `http://localhost:8080` 运行：

```bash
# 方式一：直接启动
cd backend
mvn spring-boot:run

# 方式二：Docker Compose
./start.sh
```

## 运行压测

### 本地直接运行

```bash
# 使用默认地址 http://localhost:8080
k6 run benchmark/k6-script.js

# 指定后端地址
K6_HOST=http://192.168.1.100:8080 k6 run benchmark/k6-script.js
```

### 使用 Docker

```bash
docker run --rm -i grafana/k6 run - <benchmark/k6-script.js
```

### 输出解读

运行后你会看到类似输出：

```
     ✓ 健康检查 状态 200
     ✓ 剧集列表 状态 200
     ✓ 随机句子 状态 200
     ✓ 搜索 状态 200

     checks.........................: 100.00% ✓ 400   ✗ 0
     data_received..................: 1.2 MB  ... 
     http_req_duration..............: avg=45ms  p(95)=120ms
     iterations.....................: 100
     vus............................: 10
```

重点关注：
- **checks** — 通过率，应为 100%
- **http_req_duration p(95)** — 95% 请求的响应时间
- **errors** — 错误率，应接近 0

## 场景说明

脚本包含四个压力场景：

| 场景 | 说明 |
|------|------|
| 健康检查 | `GET /api/health` |
| 剧集列表 | `GET /api/shows` |
| 随机句子 | `GET /api/random?limit=10` |
| 搜索 | `GET /api/search?q=the` |

并发配置：10 → 50 → 0，阶梯式升温共 2 分钟。

## 自定义测试

编辑 `benchmark/k6-script.js` 修改 `options.stages` 调整并发量：

```javascript
stages: [
  { duration: '1m',  target: 100 }, // 1分钟升到 100 并发
  { duration: '2m',  target: 100 }, // 保持 2 分钟
  { duration: '1m',  target: 0   }, // 下降
]
```
