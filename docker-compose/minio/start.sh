# 前置：确保 my-custom-network 已存在（未创建时先执行 network.sh）
# 启动
cd docker-compose/minio
docker compose up -d

# 停止
docker compose down

# 查看日志
docker compose logs -f minio

# 控制台: http://localhost:9001  (minioadmin / minioadmin)
# API:    http://localhost:9000