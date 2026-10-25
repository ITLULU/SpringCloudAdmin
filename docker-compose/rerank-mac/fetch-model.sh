#!/usr/bin/env bash
# 预下载 BAAI/bge-reranker-v2-m3 到 ./model（走 hf-mirror.com，国内直连可用）
# 幂等：已存在且大小一致的文件跳过；可用 FORCE=1 强制重下
# 用法：bash fetch-model.sh
set -uo pipefail
cd "$(dirname "$0")" || exit 1
DIR=${DIR:-./model}
BASE=${BASE:-https://hf-mirror.com}
REPO=${REPO:-BAAI/bge-reranker-v2-m3}
GREEN=$'\033[0;32m'; RED=$'\033[0;31m'; NC=$'\033[0m'

mkdir -p "$DIR"
# 该仓库无 onnx/ 分支，TEI 会用 PyTorch 权重（约 2.3GB）
FILES=(config.json tokenizer_config.json special_tokens_map.json sentencepiece.bpe.model tokenizer.json model.safetensors)

for f in "${FILES[@]}"; do
  url="$BASE/$REPO/resolve/main/$f"
  # 先拿期望大小（Content-Length），避免上次中断留下的半截文件被当成已完成
  want=$(curl -sIL --max-time 20 "$url" | awk 'tolower($1)=="content-length:"{v=$2} END{gsub(/\r/,"",v); print v}')
  have=$(stat -f%z "$DIR/$f" 2>/dev/null || echo 0)
  if [ "${FORCE:-0}" != "1" ] && [ -n "$want" ] && [ "$have" = "$want" ]; then
    printf "${GREEN}✓${NC} %s 已存在（%s 字节）\n" "$f" "$have"; continue
  fi
  printf '! 下载 %s（期望 %s 字节，当前 %s）\n' "$f" "${want:-未知}" "$have"
  if ! curl -L --fail --retry 3 --retry-delay 2 -C - -o "$DIR/$f" "$url"; then
    printf "${RED}✗${NC} %s 下载失败：$url\n" "$f"; exit 1
  fi
done

echo "--- 校验 ---"
need_ok=1
for f in "${FILES[@]}"; do
  [ -s "$DIR/$f" ] || { printf "${RED}✗${NC} 缺少 %s\n" "$f"; need_ok=0; }
done
[ "$need_ok" = 1 ] || exit 1
du -sh "$DIR"
printf "${GREEN}✓${NC} 模型就绪，可执行：docker compose -f docker-compose.yml up -d\n"
