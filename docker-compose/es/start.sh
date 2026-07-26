#!/bin/bash

# 创建数据目录
mkdir -p ./data/es01 ./data/es02 ./data/es03
mkdir -p ./logs/es01 ./logs/es02 ./logs/es03

# 设置权限（ES 需要特定权限）
chmod -R 777 ./data
chmod -R 777 ./logs

# 启动 ES 集群
docker-compose up -d

echo "Elasticsearch 集群启动中..."
echo "节点1: http://localhost:9200"
echo "节点2: http://localhost:9201"
echo "节点3: http://localhost:9202"
echo ""
echo "查看集群健康状态: curl http://localhost:9200/_cluster/health?pretty"
