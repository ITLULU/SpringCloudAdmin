#!/usr/bin/env bash
# 启动本机 Metal 精排服务（llama-server + bge-reranker GGUF）。
# 对外契约与 TEI 完全一致：POST /rerank  {"query":"...","texts":[...]} -> [{"index":0,"score":0.1}, ...]
# rag-kb 侧只改一个配置值即可切过来，Java 代码不动（见 README 的"接入"一节）。
set -eu

HERE="$(cd "$(dirname "$0")" && pwd)"
# 预编译包解出来是扁平的 runtime/${LLAMA_TAG}/llama-server（没有 bin/ 一层），所以直接找而不是猜目录名
SERVER_BIN="${SERVER_BIN:-$(find "$HERE/runtime" -type f -name llama-server -perm +111 2>/dev/null | head -1)}"
QUANT="${QUANT:-Q4_K_M}"   # 与 install.sh 的默认必须一致，否则"装完却起不来"：找的是没下载过的那个量化档
MODEL="${MODEL:-$HERE/models/bge-reranker-v2-m3-${QUANT}.gguf}"
HOST="${HOST:-127.0.0.1}"
PORT="${PORT:-8089}"
CTX="${CTX:-4092}"
# -ngl 99 = 把所有层都放到 Metal；bge-reranker-v2-m3(568M) Q4_K_M 约 0.42GB，统一内存完全吃得下
NGL="${NGL:-99}"
PID_FILE="$HERE/llama.pid"
LOG_FILE="$HERE/logs/llama-server.log"

[ -x "$SERVER_BIN" ] || { echo "找不到 $SERVER_BIN，先跑 install.sh"; exit 1; }
[ -f "$MODEL" ] || { echo "找不到 $MODEL，先跑 install.sh"; exit 1; }
mkdir -p "$HERE/logs"

if [ -f "$PID_FILE" ] && kill -0 "$(cat "$PID_FILE")" 2>/dev/null; then
  echo "已在运行（pid=$(cat "$PID_FILE")），要重启先 bash $HERE/stop.sh"
  exit 0
fi

# Metal 编译要在启动早期访问 GPU；后台拉起失败必须当场发现，不能留一个"看着起来了"的空壳
"$SERVER_BIN" -m "$MODEL" --reranking -c "$CTX" -ngl "$NGL" \
  --host "$HOST" --port "$PORT" --cont-batching -np 4 \
  >>"$LOG_FILE" 2>&1 &
echo $! > "$PID_FILE"

ready=""
for i in $(seq 1 60); do
  if curl -sf --max-time 3 "http://$HOST:$PORT/health" >/dev/null 2>&1; then ready=yes; break; fi
  kill -0 "$(cat "$PID_FILE")" 2>/dev/null || break   # 进程已退出 ⇒ 别再等，直接把日志尾巴给人看
  sleep 2
done

if [ -z "$ready" ]; then
  echo "启动失败（pid=$(cat "$PID_FILE")），日志尾部：" >&2
  tail -25 "$LOG_FILE" >&2
  rm -f "$PID_FILE"
  exit 1
fi

echo "Metal 精排就绪：http://$HOST:$PORT/rerank  (pid=$(cat "$PID_FILE"), model=$(basename "$MODEL"), ctx=$CTX, ngl=$NGL)"
echo "自检（两条候选，期望相关的分明显更高）："
curl -sS --max-time 30 -H 'Content-Type: application/json' \
  -d '{"query":"如何在水池边保证儿童安全","texts":["家长必须全程陪同并保持一臂距离","泳池的水温会影响过滤器寿命"]}' \
  "http://$HOST:$PORT/rerank"
echo
