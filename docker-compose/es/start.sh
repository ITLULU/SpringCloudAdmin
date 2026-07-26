#!/bin/bash
# Elasticsearch + Kibana 单节点启动脚本

cd "$(dirname "$0")" || exit

RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m'

echo "[1/3] 检查 Docker 外部网络 my-custom-network..."
if ! docker network inspect my-custom-network >/dev/null 2>&1; then
  echo -e "${YELLOW}网络不存在，尝试创建...${NC}"
  docker network create --driver=bridge --subnet=172.20.0.0/16 --gateway=172.20.0.1 my-custom-network || {
    echo -e "${RED}创建网络失败，请手动执行 ../network.sh${NC}"
    exit 1
  }
fi

echo "[2/3] 检查宿主机 vm.max_map_count（Elasticsearch 要求 >= 262144）..."
CURRENT_COUNT=$(sysctl -n vm.max_map_count 2>/dev/null || echo 0)
if [ "$CURRENT_COUNT" -lt 262144 ]; then
  echo -e "${YELLOW}当前 vm.max_map_count=$CURRENT_COUNT，正在设置为 262144...${NC}"
  sudo sysctl -w vm.max_map_count=262144
  if ! grep -q "vm.max_map_count" /etc/sysctl.conf 2>/dev/null; then
    echo "vm.max_map_count=262144" | sudo tee -a /etc/sysctl.conf >/dev/null
  fi
else
  echo -e "${GREEN}当前 vm.max_map_count=$CURRENT_COUNT，符合要求${NC}"
fi

echo "[3/3] 创建数据目录并启动..."
mkdir -p ./data/es ./logs/es

# Linux 宿主机需设置 ES 容器 uid=1000 的目录权限
if [[ "$OSTYPE" == "linux-gnu"* ]]; then
  sudo chown -R 1000:0 ./data/es ./logs/es
fi

docker-compose up -d

echo ""
echo -e "${GREEN}Elasticsearch 单节点启动中，预计需要 30-60 秒...${NC}"
echo "Elasticsearch: http://localhost:9200"
echo "Kibana:        http://localhost:5601"
