#!/usr/bin/env bash
# ====================================
# 朋友圈英语 — 数据库备份脚本
# 功能：mysqldump → tar.gz → 时间戳命名 → 保留最近7天
# 用法：chmod +x backup.sh && ./backup.sh
# 支持 cron 定时执行:
#   0 3 * * * /path/to/backup.sh   # 每天凌晨3点执行
# ====================================
set -e

# 配置（可根据实际修改）
BACKUP_DIR="./backups"                    # 备份文件存放目录
MYSQL_HOST="localhost"                     # MySQL 主机（docker 容器内为 mysql）
MYSQL_PORT="3306"                          # MySQL 端口
MYSQL_USER="root"                          # MySQL 用户名
MYSQL_PASSWORD="pengyouquan123"            # MySQL 密码（请修改为实际密码）
MYSQL_DATABASE="pengyouquan_english"       # 数据库名
RETENTION_DAYS=7                           # 保留天数
TIMESTAMP=$(date +"%Y%m%d_%H%M%S")         # 时间戳（如 20260101_030000）
DUMP_CONTAINER="pengyouquan-mysql"         # Docker 容器名

# 颜色输出
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m'

echo -e "${GREEN}📦 朋友圈英语 — 数据库备份开始${NC}"
echo "  时间: $(date '+%Y-%m-%d %H:%M:%S')"
echo "  数据库: ${MYSQL_DATABASE}"
echo "  保留天数: ${RETENTION_DAYS}"
echo ""

# 1. 创建备份目录
mkdir -p "${BACKUP_DIR}"

# 2. 备份文件路径
DUMP_FILE="${BACKUP_DIR}/${MYSQL_DATABASE}_${TIMESTAMP}.sql"
ARCHIVE_FILE="${BACKUP_DIR}/${MYSQL_DATABASE}_${TIMESTAMP}.tar.gz"

# 3. 导出数据库
echo -e "${YELLOW}[1/3] 导出数据库...${NC}"

# 检测是否在 docker 环境下运行
if docker ps --format '{{.Names}}' 2>/dev/null | grep -q "^${DUMP_CONTAINER}$"; then
    echo "  使用 Docker 容器: ${DUMP_CONTAINER}"
    if docker exec "${DUMP_CONTAINER}" mysqldump \
        -u"${MYSQL_USER}" \
        -p"${MYSQL_PASSWORD}" \
        --databases "${MYSQL_DATABASE}" \
        --add-drop-database \
        --add-drop-table \
        --complete-insert \
        --routines \
        --triggers \
        --single-transaction \
        --quick \
        > "${DUMP_FILE}" 2>&1; then
        echo -e "${GREEN}  ✓ 导出成功: ${DUMP_FILE}${NC}"
    else
        echo -e "${RED}  ✗ 导出失败！${NC}"
        rm -f "${DUMP_FILE}"
        exit 1
    fi
else
    echo "  使用本地 mysqldump..."
    # 尝试本地 mysqldump（或 docker exec 用 localhost）
    if command -v mysqldump &> /dev/null; then
        mysqldump \
            -h"${MYSQL_HOST}" \
            -P"${MYSQL_PORT}" \
            -u"${MYSQL_USER}" \
            -p"${MYSQL_PASSWORD}" \
            --databases "${MYSQL_DATABASE}" \
            --add-drop-database \
            --add-drop-table \
            --complete-insert \
            --routines \
            --triggers \
            --single-transaction \
            --quick \
            > "${DUMP_FILE}" 2>&1 && \
        echo -e "${GREEN}  ✓ 导出成功: ${DUMP_FILE}${NC}" || \
        { echo -e "${RED}  ✗ 导出失败！${NC}"; rm -f "${DUMP_FILE}"; exit 1; }
    else
        echo -e "${RED}  ✗ 未找到 mysqldump 命令，且 Docker 容器 ${DUMP_CONTAINER} 未运行${NC}"
        rm -f "${DUMP_FILE}"
        exit 1
    fi
fi

# 4. 压缩备份
echo -e "${YELLOW}[2/3] 压缩备份文件...${NC}"
tar -czf "${ARCHIVE_FILE}" -C "${BACKUP_DIR}" "$(basename "${DUMP_FILE}")"
rm -f "${DUMP_FILE}"
echo -e "${GREEN}  ✓ 已压缩: ${ARCHIVE_FILE}${NC}"

# 5. 清理旧备份（保留最近 N 天）
echo -e "${YELLOW}[3/3] 清理旧备份(保留最近${RETENTION_DAYS}天)...${NC}"
find "${BACKUP_DIR}" -name "${MYSQL_DATABASE}_*.tar.gz" -type f -mtime +${RETENTION_DAYS} -print -delete 2>/dev/null
echo -e "${GREEN}  ✓ 清理完成${NC}"

echo ""
echo -e "${GREEN}✅ 备份完成！${NC}"
echo "  备份文件: ${ARCHIVE_FILE}"
echo "  文件大小: $(du -h "${ARCHIVE_FILE}" | cut -f1)"

# 显示最近备份列表
echo ""
echo "  最近备份:"
ls -lh "${BACKUP_DIR}" | grep "${MYSQL_DATABASE}" | tail -n 5 | while read line; do
    echo "    ${line}"
done
echo ""
