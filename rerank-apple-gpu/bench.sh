#!/usr/bin/env bash
# 同题对照：同一批候选分别打两个端点，比排序一致性与耗时。
# 口径写死在这里，避免"两边各测各的"导致数字没法比：同一 payload、串行、warmup 校验通过后取 N 次。
#
# 候选条数默认 20，不是随手取的：rag-kb 的 rag.retrieval.top-n=20，两路融合后一次把整批候选
# 发给 /rerank（TeiReranker.rerank 里 texts = 全部 candidates）。用 3 条做 benchmark 会掩盖
# "服务端 batch 上限比真实候选小"这类配置冲突——那正是本地 CPU 版精排一直静默降级的原因。
set -eu

GPU_URL="${GPU_URL:-http://127.0.0.1:8089/rerank}"
CPU_URL="${CPU_URL:-http://127.0.0.1:8088/rerank}"
ROUNDS="${ROUNDS:-5}"
DOCS="${DOCS:-20}"

query='向量检索里 RRF 融合的作用是什么'
base_texts=(
  "倒数排名融合把多路召回的名次而不是分数作为输入，避免不同评分体系不可比"
  "BM25 是词袋模型，靠词频与逆文档频率给关键词匹配打分"
  "RRF 的平滑常数 k 通常取 60，用来削弱头部名次的过度主导"
  "查询重写可以在召回前把口语化问题改写成检索友好的表达"
  "向量数据库的 HNSW 索引通过多层近邻图换取检索速度与精度的折中"
  "重排模型用 cross-encoder 让 query 与文档做 token 级 attend，因此比双塔更准但更贵"
  "分块策略影响召回质量：块太长会稀释语义，太短会丢上下文"
  "元数据过滤能在向量检索前先按租户或时间范围收窄候选集"
  "混合检索的失败模式之一是两路都命中同一批文档，融合后没有增量覆盖"
  "评测机用召回率、MRR 与 nDCG 三个指标分别看覆盖、排名与位置加权质量"
)
# 不足 DOCS 条时用无关填充补齐（填充项故意与 query 无关，正好检验"相关的仍排前面"）
texts=("${base_texts[@]}")
i=1
while [ "${#texts[@]}" -lt "$DOCS" ]; do
  texts+=("填充语料 $i：今天天气适合把抽屉重新排列一遍，和检索没有关系")
  i=$((i + 1))
done
texts=("${texts[@]:0:$DOCS}")
payload=$(python3 - "$query" "$DOCS" "${texts[@]}" <<'PY'
import json, sys
print(json.dumps({"query": sys.argv[1], "texts": sys.argv[3:]}, ensure_ascii=False))
PY
)
echo "候选条数=$DOCS（payload 字节 $(printf '%s' "$payload" | wc -c | tr -d ' ')）" >&2

run_one() { # run_one <url> <label> <期望条数>；输出最后一行是名次串
  local url="$1" label="$2" expect="$3"
  echo "== $label  $url" >&2
  # warmup 必须**校验过响应**才算成功：上一版只 curl 不验，结果 CPU 侧因为 batch 超限
  # 返回 {"error":...} 只花 60ms，被当成"CPU 比 GPU 快 3 倍"的假绿灯。快有时是拒答，不是推理。
  local body
  body=$(curl -sS --max-time 120 -H 'Content-Type: application/json' -d "$payload" "$url") || {
    echo "  打不通 $url（服务没起？端口对不对？）" >&2; return 1; }
  check_shape "$body" "$expect" "$label warmup" || return 1
  local times="" i
  for i in $(seq 1 "$ROUNDS"); do
    local s e
    s=$(python3 -c 'import time;print(int(time.time()*1000))')
    body=$(curl -sS --max-time 120 -H 'Content-Type: application/json' -d "$payload" "$url")
    e=$(python3 -c 'import time;print(int(time.time()*1000))')
    check_shape "$body" "$expect" "$label round=$i" || return 1
    times="$times $((e - s))"
    echo "$body" > "/tmp/rerank_${label}.json"
  done
  echo "  每轮耗时(ms):$times" >&2
  echo "  平均(ms): $(echo $times | tr ' ' '\n' | grep -v '^$' | awk '{s += $1; c++} END {if (c) printf "%.0f", s/c}')" >&2
  # 名次序列：TEI 回裸数组 [{index,score}]，按 score 降序取 index
  python3 - "$label" <<'PY'
import json, sys
label = sys.argv[1]
d = json.load(open(f"/tmp/rerank_{label}.json"))
rows = d if isinstance(d, list) else d.get("results", [])
pairs = sorted(((float(r.get("relevance_score", r.get("score", 0))), int(r["index"])) for r in rows), reverse=True)
print(" ".join(str(i) for _, i in pairs))
PY
}

# 只认"条数对得上且每条都有数值 score"的响应；其余一律判失败，绝不让错误体混进耗时统计
check_shape() { # check_shape <body> <期望条数> <位置说明>
  local n
  n=$(python3 - "$1" <<'PY' 2>/dev/null || echo bad
import json, sys
try:
    d = json.loads(sys.argv[1])
except Exception:
    print("bad"); raise SystemExit
rows = d if isinstance(d, list) else d.get("results", []) if isinstance(d, dict) else None
if not isinstance(rows, list):
    print("shape:" + (d.get("error", "未知错误") if isinstance(d, dict) else type(d).__name__)); raise SystemExit
ok = all(("score" in r or "relevance_score" in r) and "index" in r for r in rows)
print(len(rows) if ok else "no-score-field")
PY
)
  if [ "$n" != "$2" ]; then
    echo "  !! $3 响应不是 $2 条结果（实际 $n）——这是拒答/契约不符，不是性能数据" >&2
    return 1
  fi
  return 0
}

g=$(run_one "$GPU_URL" gpu "$DOCS")
c=$(run_one "$CPU_URL" cpu "$DOCS")
echo
echo "Metal 名次序列: $g"
echo "CPU   名次序列: $c"
if [ "$g" = "$c" ]; then echo "结论：Top-10 排序完全一致 ⇒ GPU 侧只是加速，没有改变语义判分"; else echo "结论：排序有差异，需要看分数差幅度再决定切不切"; fi
