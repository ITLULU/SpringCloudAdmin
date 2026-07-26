#!/bin/bash
# 启动 Kafka 集群


docker-compose up -d

echo "Kafka 启动中..."
sleep 5

echo "创建订单相关 Topic"
docker exec sca-kafka kafka-topics.sh --create --topic topic-order-create --bootstrap-server localhost:9092 --partitions 3 --replication-factor 1 --if-not-exists
docker exec sca-kafka kafka-topics.sh --create --topic topic-order-status-change --bootstrap-server localhost:9092 --partitions 3 --replication-factor 1 --if-not-exists

echo "Kafka UI: http://localhost:8085"
echo "Kafka Bootstrap: localhost:9092"
