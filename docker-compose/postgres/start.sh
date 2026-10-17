# 前置：确保 my-custom-network 已存在（未创建时先执行 ../network.sh）
# 首次启动前创建数据目录，避免容器以 root 写入产生权限问题
mkdir -p data

# 启动
cd docker-compose/postgres
docker compose up -d

# 停止
docker compose down

# 查看日志
docker compose logs -f postgres

# 连接：localhost:5432  用户/密码：postgres/postgres  数据库：rag
# 进入容器 psql：docker exec -it postgres psql -U postgres -d rag
