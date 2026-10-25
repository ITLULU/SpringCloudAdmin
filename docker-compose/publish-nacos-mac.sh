#!/usr/bin/env bash
# 把项目所需配置发布到 Nacos（Open API，等价于控制台手工操作）
# 幂等：同 dataId+group 重复发布即覆盖。前置：nacos 容器 healthy
# 用法：bash publish-nacos-mac.sh
set -uo pipefail
cd "$(dirname "$0")" || exit 1
export PATH="$HOME/.docker/bin:$PATH"

NACOS_ADDR=${NACOS_ADDR:-http://localhost:8848}
NACOS_USER=${NACOS_USER:-nacos}
NACOS_PW=${NACOS_PW:-nacos}
# 微服务在宿主机（IDE）启动，因此数据库走 localhost；Seata Server 在容器网络内走 mysql8
MYSQL_HOST_SVC=${MYSQL_HOST_SVC:-localhost}
MYSQL_HOST_CONTAINER=${MYSQL_HOST_CONTAINER:-mysql8}
MYSQL_USER=${MYSQL_USER:-website}
MYSQL_PW=${MYSQL_PW:-website1020}
GREEN=$'\033[0;32m'; RED=$'\033[0;31m'; YELLOW=$'\033[1;33m'; NC=$'\033[0m'
ok() { printf "${GREEN}✓${NC} %s\n" "$*"; }
warn() { printf "${YELLOW}!${NC} %s\n" "$*"; }
bad() { printf "${RED}✗${NC} %s\n" "$*"; }

TOKEN=$(curl -sf -X POST "$NACOS_ADDR/nacos/v1/auth/login" \
  -d "username=$NACOS_USER&password=$NACOS_PW" | sed -n 's/.*"accessToken":"\([^"]*\)".*/\1/p')
[ -n "$TOKEN" ] || { bad "登录 Nacos 失败（$NACOS_ADDR，检查容器是否 healthy / 账号密码）"; exit 1; }
ok "Nacos 登录成功"

# publish <dataId> <group> <文件> [类型]
publish() {
  local dataId=$1 group=$2 f=$3 type=${4:-text}
  local resp
  resp=$(curl -s -X POST "$NACOS_ADDR/nacos/v1/cs/configs" \
    --data-urlencode "dataId=$dataId" --data-urlencode "group=$group" \
    --data-urlencode "type=$type" --data-urlencode "tenant=" \
    --data-urlencode "content@$f" -d "accessToken=$TOKEN")
  if [ "$resp" = "true" ]; then ok "发布 $group / $dataId"; else bad "发布 $group / $dataId 失败: $resp"; return 1; fi
}

TMP=$(mktemp -d); trap 'rm -rf "$TMP"' EXIT

echo "===== 1) 共享基线配置 ====="
cp nacos/sca-common.yml "$TMP/sca-common.yml"
publish sca-common.yml SHARED_GROUP "$TMP/sca-common.yml" yaml
# common-config.yml：仓库里没有模板，但三个服务的 bootstrap.yml 都引用它，
# 且 ${mysql.*} / ${nacos.*} 占位符全靠它解析——缺失会导致服务启动即崩
cat > "$TMP/common-config.yml" <<EOF
# 环境相关占位符（服务在宿主机启动，DB 用 localhost）
mysql:
  host: $MYSQL_HOST_SVC
  port: 3306
  user: $MYSQL_USER
  password: $MYSQL_PW
nacos:
  username: $NACOS_USER
  password: $NACOS_PW
EOF
publish common-config.yml DEFAULT_GROUP "$TMP/common-config.yml" yaml

echo "===== 2) Seata Server 配置（SEATA_GROUP）====="
cat > "$TMP/seataServer.properties" <<EOF
service.vgroupMapping.default_tx_group=default
store.mode=db
store.db.datasource=druid
store.db.dbType=mysql
store.db.driverClassName=com.mysql.cj.jdbc.Driver
store.db.url=jdbc:mysql://$MYSQL_HOST_CONTAINER:3306/seata?useUnicode=true&rewriteBatchedStatements=true&serverTimezone=Asia/Shanghai
store.db.user=$MYSQL_USER
store.db.password=$MYSQL_PW
store.db.minConn=5
store.db.maxConn=30
store.db.globalTable=global_table
store.db.branchTable=branch_table
store.db.distributedLockTable=distributed_lock
store.db.queryLimit=100
store.db.lockTable=lock_table
store.db.maxWait=5000
server.recovery.committingRetryPeriod=1000
server.recovery.asynCommittingRetryPeriod=1000
server.recovery.rollbackingRetryPeriod=1000
server.recovery.timeoutRetryPeriod=1000
server.maxCommitRetryTimeout=-1
server.maxRollbackRetryTimeout=-1
server.rollbackRetryTimeoutUnlockEnable=false
server.distributedLockExpireTime=10000
EOF
publish seataServer.properties SEATA_GROUP "$TMP/seataServer.properties" properties

echo "===== 3) Sentinel 规则（SENTINEL_GROUP，dataId 与 bootstrap.yml 一致，不带 .json）====="
for r in flow degrade authority userflow; do
  f="sentinel/sca-web-sentinel-$r.json"
  [ -f "$f" ] || { warn "缺少 $f，跳过"; continue; }
  publish "sca-web-sentinel-$r" SENTINEL_GROUP "$f" json
done

echo "===== 4) 各服务主配置（DEFAULT_GROUP）====="
# 主配置 sca-{web,order,stock}.yml 缺失不致命（本地 application.yml 优先级更高），
# 但若线上要用 Nacos 管理，需在此补发。
for svc in sca-web sca-order sca-stock sca-authcenter; do
  code=$(curl -s -o /dev/null -w '%{http_code}' "$NACOS_ADDR/nacos/v1/cs/configs?dataId=$svc.yml&group=DEFAULT_GROUP&accessToken=$TOKEN")
  [ "$code" = "200" ] && ok "$svc.yml 已存在" || warn "$svc.yml 未创建（可用控制台按需新增）"
done

echo ""
echo "================= 校验 ================="
for spec in "sca-common.yml|SHARED_GROUP" "common-config.yml|DEFAULT_GROUP" \
            "seataServer.properties|SEATA_GROUP" "sca-web-sentinel-flow|SENTINEL_GROUP" \
            "sca-web-sentinel-degrade|SENTINEL_GROUP" "sca-web-sentinel-authority|SENTINEL_GROUP" \
            "sca-web-sentinel-userflow|SENTINEL_GROUP"; do
  d=${spec%%|*}; g=${spec##*|}
  n=$(curl -s "$NACOS_ADDR/nacos/v1/cs/configs?dataId=$d&group=$g&accessToken=$TOKEN" | wc -c | tr -d ' ')
  if [ "$n" -gt 2 ]; then ok "$g / $d 读取正常（${n} 字节）"; else bad "$g / $d 读取异常"; fi
done
