# 前置：确保 my-custom-network 已存在（未创建时先执行 ../network.sh）
# 首次启动会拉取 TEI 镜像并下载 bge-reranker-v2-m3 模型(~2GB)到 ./data，耗时较长，耐心等待 healthcheck 变 healthy
mkdir -p data

# 启动
cd docker-compose/rerank
docker compose up -d

# 停止
docker compose down

# 查看日志（可观察模型加载进度）
docker compose logs -f rerank

# 精排接口:  http://localhost:8086/rerank   （Spring AI rag.rerank.base-url 指向此地址）
# 健康检查:  http://localhost:8086/health
# 冒烟测试:
#   curl -X POST http://localhost:8086/rerank -H "Content-Type: application/json" \
#     -d '{"query":"什么是RAG","texts":["RAG是检索增强生成","今天天气很好"]}'
