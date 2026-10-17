# 前置1：确保 my-custom-network 已存在（未创建时先执行 ../network.sh）
# 前置2：宿主机需装好 NVIDIA 驱动 + nvidia-container-toolkit（docker 能看到 GPU）
#   验证：docker run --rm --gpus all nvidia/cuda:12.4.1-base-ubuntu22.04 nvidia-smi
# 首次启动会从 huggingface.co 拉取 ~2GB 模型到 ./data
mkdir -p data

# 启动
cd docker-compose/rerank-gpu
docker compose up -d

# 停止
docker compose down

# 查看日志（可观察模型加载进度）
docker compose logs -f rerank

# 精排接口:  http://localhost:8087/rerank   （Spring AI rag.rerank.base-url 指向此地址）
# 健康检查:  http://localhost:8087/health
# 冒烟测试:
#   curl -X POST http://localhost:8087/rerank -H "Content-Type: application/json" \
#     -d '{"query":"什么是RAG","texts":["RAG是检索增强生成","今天天气很好"]}'
