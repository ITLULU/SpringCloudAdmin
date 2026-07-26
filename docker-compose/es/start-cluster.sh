#!/bin/bash
# Elasticsearch + Kibana 集群启动脚本
# 前置检查：宿主机 vm.max_map_count 与数据目录权限

cd "$(dirname "$0")" || exit

# 颜色输出
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m'

echo "[1/4] 检查 Docker 外部网络 my-custom-network..."
if ! docker network inspect my-custom-network >/dev/null 2>&1; then
  echo -e "${YELLOW}网络不存在，尝试创建...${NC}"
  docker network create --driver=bridge --subnet=172.20.0.0/16 --gateway=172.20.0.1 my-custom-network || {
    echo -e "${RED}创建网络失败，请手动执行 ../network.sh${NC}"
    exit 1
  }
fi

echo "[2/4] 检查宿主机 vm.max_map_count（Elasticsearch 要求 >= 262144）..."
CURRENT_COUNT=$(sysctl -n vm.max_map_count 2>/dev/null || echo 0)
if [ "$CURRENT_COUNT" -lt 262144 ]; then
  echo -e "${YELLOW}当前 vm.max_map_count=$CURRENT_COUNT，正在设置为 262144...${NC}"
  sudo sysctl -w vm.max_map_count=262144
  # 持久化
  if ! grep -q "vm.max_map_count" /etc/sysctl.conf; then
    echo "vm.max_map_count=262144" | sudo tee -a /etc/sysctl.conf >/dev/null
  fi
else
  echo -e "${GREEN}当前 vm.max_map_count=$CURRENT_COUNT，符合要求${NC}"
fi

echo "[3/4] 创建数据目录并设置权限（Elasticsearch 容器使用 uid=1000）..."
for node in es01 es02 es03; do
  mkdir -p "./data/$node" "./logs/$node"
  # Docker Desktop (Windows/Mac) 自动处理权限；Linux 需要显式设置
  if [[ "$OSTYPE" == "linux-gnu"* ]]; then
    sudo chown -R 1000:0 "./data/$node" "./logs/$node"
  fi
done

echo "[4/4] 启动 Elasticsearch + Kibana..."
docker-compose up -d

echo ""
echo -e "${GREEN}Elasticsearch 集群启动中，预计需要 1-2 分钟...${NC}"
echo "Kibana 访问地址: http://localhost:5601"
echo "Elasticsearch 节点:"
echo "  - http://localhost:9200 (es01)"
echo "  - http://localhost:9201 (es02)"
echo "  - http://localhost:9202 (es03)"
