# 前置：确保 my-custom-network 已存在（未创建时先执行 ../network.sh）
# 前置：Milvus 复用共享 MinIO，需先启动 docker-compose/minio（会创建 milvus-bucket）
# 首次启动前创建数据目录，避免容器以 root 写入产生权限问题
mkdir -p data/etcd data/milvus logs/milvus

# 启动
cd docker-compose/milvus
docker-compose up -d

# 停止
docker compose down

# 查看日志
docker compose logs -f milvus-standalone

# gRPC SDK/API:      localhost:19530
# Metrics/Healthz:   http://localhost:9091/healthz
