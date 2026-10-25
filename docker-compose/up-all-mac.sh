#!/usr/bin/env bash
# SpringCloudAdmin 中间件一键启动（macOS / Apple Silicon / Docker Desktop）
# 用法：
#   bash up-all-mac.sh              # 按依赖顺序启动全部栈
#   bash up-all-mac.sh mysql nacos  # 只启动指定栈
#   SKIP_ES_BUILD=1 bash up-all-mac.sh   # 跳过 ES+IK 镜像构建
# 依赖：docker compose v2+，已存在外部网络 my-custom-network（脚本会自动创建）
set -uo pipefail
cd "$(dirname "$0")" || exit 1
export PATH="$HOME/.docker/bin:$PATH"

GREEN=$'\033[0;32m'; RED=$'\033[0;31m'; YELLOW=$'\033[1;33m'; NC=$'\033[0m'

# 依赖顺序：Nacos/Seata 依赖 MySQL；Kibana 依赖 ES；Milvus 依赖 etcd + MinIO；rerank-mac 需预下载模型
STACKS=(mysql nacos redis rabbitmq kafka rocketmq seata sentinel es postgres qdrant minio milvus rerank-mac)
# 每个栈需要等待就绪的容器名（macOS 自带 bash 3.2 无关联数组，用 case）
stack_containers() {
  case "$1" in
    mysql)    echo "mysql8" ;;
    nacos)    echo "nacos" ;;
    redis)    echo "redis" ;;
    rabbitmq) echo "rabbitmq" ;;
    kafka)    echo "zookeeper kafka kafka-ui" ;;
    rocketmq) echo "rmqnamesrv rmqbroker rmqdashboard" ;;
    seata)    echo "seata-server" ;;
    sentinel) echo "sentinel-dashboard" ;;
    es)       echo "es kibana" ;;
    postgres) echo "postgres" ;;
    qdrant)   echo "qdrant" ;;
    minio)    echo "minio" ;;
    milvus)   echo "milvus-etcd milvus-standalone" ;;
    rerank-mac) echo "rerank-mac" ;;
    *)        echo "" ;;
  esac
}
HEALTH_TIMEOUT=${HEALTH_TIMEOUT:-300}

log()  { printf '%s\n' "$*"; }
ok()   { log "${GREEN}✓${NC} $*"; }
warn() { log "${YELLOW}!${NC} $*"; }
bad()  { log "${RED}✗${NC} $*"; }

docker info >/dev/null 2>&1 || { bad "Docker 引擎未就绪，先 open -a Docker 并等待鲸鱼图标变绿"; exit 1; }

docker network inspect my-custom-network >/dev/null 2>&1 || {
  warn "创建外部网络 my-custom-network (172.20.0.0/16)"
  docker network create --driver=bridge --subnet=172.20.0.0/16 --gateway=172.20.0.1 my-custom-network >/dev/null
}

# compose 文件里所有宿主机侧挂载目录/文件（Docker 自动创建的目录属 root，预建避免权限问题）
ensure_mounts() {
  mkdir -p \
    es/data/es es/logs/es \
    milvus/data/etcd milvus/data/milvus milvus/logs/milvus \
    minio/data minio/logs \
    mysql/conf/conf.d mysql/data mysql/logs \
    nacos/logs postgres/data \
    qdrant/data/qdrant_storage qdrant/data/qdrant_snapshots \
    rabbitmq/data rabbitmq/logs redis/data redis/logs \
    rocketmq/broker/logs rocketmq/broker/store rocketmq/namesrv/logs \
    sentinel/logs
  # ES 容器内以 uid 1000 运行，宿主机绑定目录必须可写
  chmod -R a+rwX es/data es/logs 2>/dev/null || true
}

wait_container() {
  local name=$1 deadline=$(( $(date +%s) + HEALTH_TIMEOUT )) state
  while [ "$(date +%s)" -lt "$deadline" ]; do
    state=$(docker inspect --format '{{if .State.Health}}{{.State.Health.Status}}{{else}}{{.State.Status}}{{end}}' "$name" 2>/dev/null || echo missing)
    case "$state" in
      healthy|running) [ "$state" = healthy ] && { ok "$name → healthy"; return 0; } ;;
      missing) warn "$name 尚未创建"; sleep 3; continue ;;
      exited|dead) bad "$name → $state"; docker logs --tail 25 "$name" 2>&1 | sed 's/^/    /'; return 1 ;;
    esac
    # 无 healthcheck 的容器：running 且端口可连即认为就绪
    if [ "$state" = running ]; then
      ok "$name → running（无 healthcheck）"; return 0
    fi
    sleep 3
  done
  bad "$name 等待超时（${HEALTH_TIMEOUT}s），最后状态=$state"; docker logs --tail 25 "$name" 2>&1 | sed 's/^/    /'; return 1
}

start_stack() {
  local s=$1 f="$1/docker-compose.yml"
  [ -f "$f" ] || { warn "$s 无 docker-compose.yml，跳过"; return 0; }
  log ""
  log "===== $s ====="
  if [ "$s" = "es" ] && [ "${SKIP_ES_BUILD:-0}" != "1" ]; then
    if docker image inspect es-with-ik:8.13.4 >/dev/null 2>&1; then
      ok "es-with-ik:8.13.4 已存在，跳过构建"
    else
      warn "构建含 IK 中文分词插件的 ES 镜像（首次约 2-5 分钟，源：get.infini.cloud）"
      if ! docker compose -f "$f" build; then
        bad "ES+IK 镜像构建失败。可先起官方镜像：SKIP_ES_BUILD=1 并把 compose 里 build 注释掉；或手动下载 IK 插件后重试"
        return 1
      fi
    fi
  fi
  if [ "$s" = "rerank-mac" ] && [ ! -s "rerank-mac/model/model.safetensors" ]; then
    bad "rerank-mac/model 缺 model.safetensors：先跑 bash rerank-mac/fetch-model.sh（约 2.1GB，走 hf-mirror）再起该栈"
    return 1
  fi
  docker compose -f "$f" up -d || { bad "$s 启动失败"; return 1; }
  for c in $(stack_containers "$s"); do wait_container "$c" || return 1; done
}

ensure_mounts
FAILED=()
if [ "$#" -gt 0 ]; then SELECTED=("$@"); else SELECTED=("${STACKS[@]}"); fi
for s in "${SELECTED[@]}"; do start_stack "$s" || FAILED+=("$s"); done

log ""
log "================ 容器状态汇总 ================"
docker ps -a --format 'table {{.Names}}\t{{.Image}}\t{{.Status}}\t{{.Ports}}' | sed 's/^/  /'
log ""
log "内存/磁盘占用:"; docker stats --no-stream --format 'table {{.Name}}\t{{.MemUsage}}\t{{.CPUPerc}}' 2>/dev/null | sed 's/^/  /'
log "docker system df:"; docker system df | sed 's/^/  /'

if [ "${#FAILED[@]}" -gt 0 ]; then
  log ""
  bad "失败栈：${FAILED[*]}"
  log "排查：docker logs <容器名>；逐项修复后重跑 bash up-all-mac.sh ${FAILED[*]}"
  exit 1
fi
log ""
ok "全部中间件已启动。下一步：bash init-data-mac.sh（导入 DDL + 发布 Nacos 配置）"
