#!/bin/bash
# RocketMQ (NameServer + Broker + Dashboard) 启动脚本

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
mkdir -p ./namesrv/logs ./broker/logs ./broker/store

# 赋予挂载目录写权限，避免容器内 Permission denied
if [[ "$OSTYPE" == "linux-gnu"* ]]; then
  sudo chmod -R 777 ./namesrv ./broker
fi

docker-compose up -d

echo ""
echo -e "${GREEN}RocketMQ 启动中，NameServer 就绪后 Broker 才会启动...${NC}"
echo "NameServer:  localhost:9876"
echo "Broker:      localhost:10911"
echo "控制台:      http://localhost:18080"
echo ""
echo -e "${YELLOW}提示：若微服务运行在宿主机而非 docker 网络内，请在 broker.conf 中配置 brokerIP1 为宿主机局域网 IP${NC}"
