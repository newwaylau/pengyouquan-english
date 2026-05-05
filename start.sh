#!/usr/bin/env bash
# ====================================
# 朋友圈英语 — 一键启动脚本
# 功能：检查环境 → 启动容器 → 打印地址
# 用法：chmod +x start.sh && ./start.sh
# ====================================
set -e

# 颜色输出
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
CYAN='\033[0;36m'
NC='\033[0m' # No Color

echo -e "${CYAN}==============================${NC}"
echo -e "${CYAN}  朋友圈英语 — 一键启动${NC}"
echo -e "${CYAN}==============================${NC}"
echo ""

# 1. 检查 Docker 是否安装
echo -e "${YELLOW}[1/4] 检查 Docker...${NC}"
if ! command -v docker &> /dev/null; then
    echo -e "${RED}错误：Docker 未安装！${NC}"
    echo "请先安装 Docker："
    echo "  https://docs.docker.com/engine/install/"
    exit 1
fi
echo -e "${GREEN}  ✓ Docker $(docker --version | cut -d' ' -f3 | tr -d ',')${NC}"

# 2. 检查 Docker Compose
echo -e "${YELLOW}[2/4] 检查 Docker Compose...${NC}"
if docker compose version &> /dev/null; then
    echo -e "${GREEN}  ✓ Docker Compose $(docker compose version --short)${NC}"
else
    if docker-compose version &> /dev/null; then
        echo -e "${GREEN}  ✓ docker-compose $(docker-compose version --short)${NC}"
    else
        echo -e "${RED}错误：Docker Compose 未安装！${NC}"
        echo "请先安装 Docker Compose："
        echo "  https://docs.docker.com/compose/install/"
        exit 1
    fi
fi

# 3. 检查 .env 文件
echo -e "${YELLOW}[3/4] 检查环境配置...${NC}"
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ENV_FILE="${SCRIPT_DIR}/docker/.env"

if [ ! -f "$ENV_FILE" ]; then
    echo -e "${YELLOW}  ⚠ docker/.env 不存在，从 .env.example 创建...${NC}"
    if [ -f "${SCRIPT_DIR}/.env.example" ]; then
        cp "${SCRIPT_DIR}/.env.example" "$ENV_FILE"
        echo -e "${GREEN}  ✓ 已从 .env.example 创建 docker/.env${NC}"
        echo -e "${YELLOW}  ⚠ 请检查并修改 docker/.env 中的密码和密钥${NC}"
    else
        echo -e "${RED}  ✗ .env.example 也不存在，请手动创建 docker/.env${NC}"
        exit 1
    fi
else
    echo -e "${GREEN}  ✓ docker/.env 已存在${NC}"
fi

# 4. 启动容器
echo -e "${YELLOW}[4/4] 启动 Docker 容器...${NC}"
cd "$SCRIPT_DIR"
docker compose -f docker/docker-compose.yml --env-file docker/.env up -d --build
echo -e "${GREEN}  ✓ 容器启动完成${NC}"

echo ""
echo -e "${CYAN}==============================${NC}"
echo -e "${GREEN}  🚀 朋友圈英语 部署完成！${NC}"
echo -e "${CYAN}==============================${NC}"
echo ""

# 获取访问地址
BACKEND_PORT="${BACKEND_PORT:-8080}"
FRONTEND_PORT="${FRONTEND_PORT:-3000}"
HOST="localhost"

# 尝试获取宿主机IP
if command -v hostname &> /dev/null; then
    HOST_IP=$(hostname -I 2>/dev/null | awk '{print $1}')
    [ -z "$HOST_IP" ] && HOST_IP=$HOST
fi

echo -e "  ${GREEN}前端访问:${NC}"
echo -e "    本地:    http://localhost:${FRONTEND_PORT}"
echo -e "    网络:    http://${HOST_IP:-localhost}:${FRONTEND_PORT}"
echo ""
echo -e "  ${GREEN}后端 API:${NC}"
echo -e "    本地:    http://localhost:${BACKEND_PORT}"
echo -e "    网络:    http://${HOST_IP:-localhost}:${BACKEND_PORT}"
echo ""
echo -e "  ${GREEN}常用命令:${NC}"
echo -e "    查看日志:   docker compose -f docker/docker-compose.yml logs -f"
echo -e "    停止服务:   docker compose -f docker/docker-compose.yml down"
echo -e "    重启服务:   docker compose -f docker/docker-compose.yml restart"
echo -e "    备份数据:   bash backup.sh"
echo ""
echo -e "${YELLOW}⚠ 注意：首次启动后数据库需要 Flyway 自动迁移建表，请稍等1-2分钟${NC}"
echo -e "${YELLOW}⚠ 生产环境请修改 docker/.env 中的密码和 JWT 密钥！${NC}"
echo ""
