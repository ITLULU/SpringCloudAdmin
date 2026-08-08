#!/bin/bash
# RabbitMQ 单节点启动脚本

cd "$(dirname "$0")" || exit

RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m'

echo "[1/2] 检查 Docker 外部网络 my-custom-network..."
if ! docker network inspect my-custom-network >/dev/null 2>&1; then
  echo -e "${YELLOW}网络不存在，尝试创建...${NC}"
  docker network create --driver=bridge --subnet=172.20.0.0/16 --gateway=172.20.0.1 my-custom-network || {
    echo -e "${RED}创建网络失败，请手动执行 ../network.sh${NC}"
    exit 1
  }
fi

echo "[2/2] 创建数据目录并启动..."
mkdir -p ./data ./logs

# RabbitMQ 容器以 rabbitmq 用户（uid=999）运行，需赋予目录写权限
if [[ "$OSTYPE" == "linux-gnu"* ]]; then
  sudo chown -R 999:999 ./data ./logs
fi

docker-compose up -d

echo ""
echo -e "${GREEN}RabbitMQ 启动中，预计需要 10-20 秒...${NC}"
echo "AMQP 端口:      localhost:5672"
echo "管理控制台:     http://localhost:15672  (admin / admin123)"
