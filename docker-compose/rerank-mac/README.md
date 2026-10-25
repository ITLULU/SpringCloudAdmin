# rerank-mac —— Apple Silicon 本机专用精排（rerank）服务

`rerank/`（CPU）与 `rerank-gpu/`（NVIDIA）都是按 Linux 服务器写的。本机是 Apple Silicon，
两者都不能直接用，故新增本目录。

## 选型结论（为什么是这个镜像）

| 方案 | 结论 | 依据 |
|---|---|---|
| `text-embeddings-inference:cpu-1.9` + `platform: linux/amd64` | **采用** | TEI 官方镜像无 arm64 变体（`docker manifest inspect --verbose` 对 `cpu-1.9`/`cpu-latest`/`1.9`/`1.5` 均只返回 `amd64`）；Docker Desktop 的 Rosetta 转译可跑 amd64 容器（`docker run --platform linux/amd64 alpine uname -m` → `x86_64` 已实测）。对外契约与 `rerank/`、`rerank-gpu/` 完全一致，调用方零改动。 |
| `rerank-gpu`（NVIDIA device reserve） | 不适用 | macOS 版 Docker 没有 GPU 直通，`deploy.resources.devices` 无法生效。 |
| Ollama / LocalAI 等 arm64 原生方案 | 暂不采用 | 芯片原生但 rerank 接口的请求/响应结构与 TEI 的 `POST /rerank`（`{query, texts}` → `[{index, score}]`）不同，需要为项目写适配层；若后续要在本机做性能验证再评估。 |

**代价要清楚**：x86 转译 + 纯 CPU 推理，只够开发联调用；不要用本容器得出的延迟去做压测结论。

## 使用步骤

```bash
cd docker-compose/rerank-mac

# 1) 预下载模型（约 2.3GB，走 hf-mirror.com，可断点续传、可重复执行）
bash fetch-model.sh

# 2) 起服务（离线加载，运行时不再访问 huggingface.co）
docker compose up -d
docker compose ps          # 等待 healthy，start_period 设了 180s

# 3) 冒烟：中文相关性排序
curl -s http://localhost:8088/rerank -H 'Content-Type: application/json' -d '{
  "query": "苹果发布会",
  "texts": ["Apple 发布了新款 iPhone", "今天天气不错"]
}'
```

## 端口与网络

| 项 | 值 | 说明 |
|---|---|---|
| 宿主机端口 | `8088` | `8086`=rerank(cpu)、`8087`=rerank-gpu，均已占用，本容器另开 |
| 精排接口 | `http://localhost:8088/rerank` | 与 `rerank/` 同契约，只是端口不同 |
| 健康检查 | `GET /health` | compose 内置 healthcheck |
| 网络 | `my-custom-network`（external） | 由 `up-all-mac.sh` 统一创建 |

## 常见问题

| 现象 | 处理 |
|---|---|
| `no matching manifest for linux/arm64/v8` | 说明 compose 里的 `platform: linux/amd64` 被删了，补回来。 |
| 启动卡在加载模型 / OOM | VM 内存需 ≥8GB（本项目已调到 16GB）；也可临时用 `MODEL_ID` 换更小模型。 |
| 模型下载中断 | 重跑 `bash fetch-model.sh`（`curl -C -` 续传，按 Content-Length 判完整）。 |
| 想强制重下 | `FORCE=1 bash fetch-model.sh`。 |
