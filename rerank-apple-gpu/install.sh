#!/usr/bin/env bash
# 安装本机（Apple Silicon / Metal）GPU 精排运行时：llama.cpp 预编译二进制 + bge-reranker GGUF。
# 全程用户目录、无 sudo、无 Homebrew；下载源：GitHub Releases（二进制）+ hf-mirror（模型，国内直连）。
set -eu

HERE="$(cd "$(dirname "$0")" && pwd)"
RUNTIME_DIR="$HERE/runtime"
MODEL_DIR="$HERE/models"
LOG_DIR="$HERE/logs"

LLAMA_TAG="${LLAMA_TAG:-b11836}"
LLAMA_ASSET="llama-${LLAMA_TAG}-bin-macos-arm64.tar.gz"
LLAMA_URL="${LLAMA_URL:-https://github.com/ggml-org/llama.cpp/releases/download/${LLAMA_TAG}/${LLAMA_ASSET}}"

# 模型仓库自带的 SHA256SUMS（转化自 BAAI/bge-reranker-v2-m3，apache-2.0）
# 选 smarttasks 而不是更常见的 gpustack：后者**没有** SHA256SUMS（已实测 404），
# 拿它就只能靠"大小对得上"这种弱判据；这里要求必须有真散列可核。
GGUF_REPO="${GGUF_REPO:-smarttasks/bge-reranker-v2-m3-GGUF}"
HF_BASE="${HF_BASE:-https://hf-mirror.com}"
QUANT="${QUANT:-Q4_K_M}"           # Q4_K_M(418MB) 自测够用；改 Q8_0(606MB)/Q5_K_M(447MB) 换保真与下载量
GGUF_FILE="bge-reranker-v2-m3-${QUANT}.gguf"

mkdir -p "$RUNTIME_DIR" "$MODEL_DIR" "$LOG_DIR"

fetch() { # fetch <url> <目标文件> <重试次数>
  local url="$1" dst="$2" tries="${3:-6}" n=1
  while [ "$n" -le "$tries" ]; do
    # -sS：进度条不进日志（它会淹没诊断），但错误仍然可见——上一版把 stderr 吞了，空转看不出来
    if curl -fL -sS --retry 2 --max-time "${CURL_MAX_TIME:-900}" -o "$dst.part" "$url"; then
      mv "$dst.part" "$dst"; return 0
    fi
    echo "  下载第 $n 次失败（GitHub/HF 在国内会偶发握手失败，自动重试）" >&2
    rm -f "$dst.part"; n=$((n + 1)); sleep 5
  done
  return 1
}

echo "== 1/3 llama.cpp 二进制（${LLAMA_TAG}，macOS arm64 预编译，含 Metal）=="
# 预编译包是**扁平结构**：解出来是 ${LLAMA_TAG}/llama-server，没有 bin/ 这一层（已用 tar -tzf 核实）。
# 所以不写死目录名，解完直接找——版本换了也不会指到不存在的路径。
SERVER_BIN="$(find "$RUNTIME_DIR" -type f -name llama-server -perm +111 2>/dev/null | head -1)"
if [ -n "$SERVER_BIN" ] && [ -x "$SERVER_BIN" ]; then
  echo "  已存在，跳过：$("$SERVER_BIN" --version 2>&1 | head -1)"
else
  # 三种来源按优先级：仓库里已有包 > 外部暂存包（慢链路断点续传的产物）> 现下载。
  # 上一版把"已有包"漏在判断外，结果明明有 16MB 的包还去重下——慢链路上等于卡死。
  if [ ! -s "$HERE/$LLAMA_ASSET" ] && [ -n "${STAGED_TGZ:-}" ] && [ -s "$STAGED_TGZ" ]; then
    cp "$STAGED_TGZ" "$HERE/$LLAMA_ASSET"
    echo "  用已暂存的包：$STAGED_TGZ"
  elif [ ! -s "$HERE/$LLAMA_ASSET" ]; then
    fetch "$LLAMA_URL" "$HERE/$LLAMA_ASSET" || { echo "二进制下载失败，检查网络后重跑"; exit 1; }
  else
    echo "  用仓库里已有的包：$LLAMA_ASSET"
  fi
  # 包体积是硬指标：16MB 量级，截断的 tar.gz 会在解包时报 unexpected EOF
  tar -tzf "$HERE/$LLAMA_ASSET" >/dev/null || { echo "  包不完整（解包测试失败），删除后重跑：$HERE/$LLAMA_ASSET" >&2; exit 1; }
  grep -q "libggml-metal" <(tar -tzf "$HERE/$LLAMA_ASSET") || { echo "  这个包里没有 Metal 后端，装了也只是 CPU 跑" >&2; exit 1; }
  tar -xzf "$HERE/$LLAMA_ASSET" -C "$RUNTIME_DIR"
  # curl 下载不带 com.apple.quarantine，正常无需处理；从浏览器拿过的补一次即可
  xattr -dr com.apple.quarantine "$RUNTIME_DIR" 2>/dev/null || true
  # 解包之后才找得到二进制：上面那次 find 是"是否已装"的判定，这里必须重新取，否则 $SERVER_BIN 是空的，
  # 后面那句 "$SERVER_BIN --version" 就变成拿 --version 当命令执行（实测踩过）
  SERVER_BIN="$(find "$RUNTIME_DIR" -type f -name llama-server -perm +111 2>/dev/null | head -1)"
  [ -x "$SERVER_BIN" ] || { echo "  解包后仍找不到 llama-server，检查包结构" >&2; exit 1; }
  echo "  解出：$("$SERVER_BIN" --version 2>&1 | head -1)"
fi

echo "== 2/3 模型权重（${GGUF_FILE}）=="
if [ -f "$MODEL_DIR/$GGUF_FILE" ]; then
  echo "  已存在，跳过下载"
else
  fetch "$HF_BASE/$GGUF_REPO/resolve/main/$GGUF_FILE" "$MODEL_DIR/$GGUF_FILE" || { echo "模型下载失败"; exit 1; }
fi

echo "== 3/3 校验 SHA256（不匹配直接失败，绝不带着坏权重启动）=="
# 期望值来自模型仓库自带的 SHA256SUMS，不在脚本里手抄散列——手抄错一位就会把正常下载判成坏文件
fetch "$HF_BASE/$GGUF_REPO/resolve/main/SHA256SUMS" "$HERE/SHA256SUMS" 3 || { echo "取不到 SHA256SUMS，拒绝在无校验状态下继续" >&2; exit 1; }
# 这个仓库的 SHA256SUMS 是 CRLF 换行（已实测），不去掉 \r 的话 $2 永远比不过文件名、会把好文件判成"查无此项"
EXPECTED_SHA=$(tr -d '\r' <"$HERE/SHA256SUMS" | awk -v f="$GGUF_FILE" '$2 == f {print $1}')
if [ -z "$EXPECTED_SHA" ]; then
  echo "  SHA256SUMS 里没有 $GGUF_FILE（QUANT 名写错了？可选 Q4_K_M / Q5_K_M / Q8_0）" >&2
  exit 1
fi
actual=$(shasum -a 256 "$MODEL_DIR/$GGUF_FILE" | awk '{print $1}')
if [ "$actual" != "$EXPECTED_SHA" ]; then
  echo "  校验失败 expected=$EXPECTED_SHA actual=$actual" >&2
  echo "  已把坏文件改名保留现场：$GGUF_FILE.bad" >&2
  mv "$MODEL_DIR/$GGUF_FILE" "$MODEL_DIR/$GGUF_FILE.bad"
  exit 1
fi
echo "  校验通过：$actual"
echo "  Metal 设备枚举：$($SERVER_BIN --list-devices 2>/dev/null | tr '\n' ' ' || echo '(该版本无 --list-devices，看 start.sh 日志里的 Metal 行)')"
echo
echo "下一步：bash $HERE/start.sh   （默认 127.0.0.1:8089/rerank）"
