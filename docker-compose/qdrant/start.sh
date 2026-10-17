# 前置：确保 my-custom-network 已存在（未创建时先执行 network.sh）
# 启动
cd docker-compose/qdrant
docker compose up -d

# 停止
docker compose down

# 查看日志
docker compose logs -f qdrant

# REST API + 控制台: http://localhost:6333/dashboard
# gRPC:                localhost:6334