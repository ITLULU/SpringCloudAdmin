#!/usr/bin/env bash
# SpringCloudAdmin 数据初始化（macOS / Docker Desktop / mysql8 容器）
# 幂等：可重复执行。前置：bash up-all-mac.sh mysql 已 healthy
# 覆盖：Nacos 库表、业务库 DDL、Seata 库表 + 各业务库 undo_log/tcc_fence_log、
#       手册口径的 website 用户、schema.sql 里跨库的 TCC 字段 ALTER
set -uo pipefail
cd "$(dirname "$0")" || exit 1
export PATH="$HOME/.docker/bin:$PATH"
DBDIR="$(cd .. && pwd)/Database"
MYSQL_ROOT_PW=${MYSQL_ROOT_PW:-root}
GREEN=$'\033[0;32m'; RED=$'\033[0;31m'; YELLOW=$'\033[1;33m'; NC=$'\033[0m'
ok() { printf "${GREEN}✓${NC} %s\n" "$*"; }
warn() { printf "${YELLOW}!${NC} %s\n" "$*"; }
bad() { printf "${RED}✗${NC} %s\n" "$*"; }

M() { docker exec -i mysql8 mysql -uroot -p"$MYSQL_ROOT_PW" --default-character-set=utf8mb4 "$@" 2>&1 | grep -v "Using a password"; }
# 导入到指定库：先建库再切库，避开"部分 schema 无 USE 语句"的坑
import_into() { # $1=db $2=file [过滤sed脚本]
  local db=$1 f=$2 filt=${3:-cat}
  docker exec -i mysql8 mysql -uroot -p"$MYSQL_ROOT_PW" -e "CREATE DATABASE IF NOT EXISTS \`$db\` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;" 2>&1 | grep -v "Using a password"
  local out
  out=$(eval "$filt" < "$f" | docker exec -i mysql8 mysql -uroot -p"$MYSQL_ROOT_PW" --default-character-set=utf8mb4 "$db" 2>&1 | grep -v "Using a password")
  if [ -n "$out" ]; then
    # 幂等：重复数据 / 表已存在，说明该库此前已初始化，不算失败
    if printf '%s\n' "$out" | grep -Eq "Duplicate entry|already exists" &&
       ! printf '%s\n' "$out" | grep -Eqv "Duplicate entry|already exists"; then
      warn "$db ← $(basename "$f") 已初始化（$(printf '%s\n' "$out" | head -1)），跳过"
      return 0
    fi
    bad "$db ← $(basename "$f"): $out"; return 1
  fi
  ok "$db ← $(basename "$f")"
}

docker inspect --format '{{.State.Health.Status}}' mysql8 2>/dev/null | grep -q healthy || { bad "mysql8 未 healthy，先跑 up-all-mac.sh"; exit 1; }

echo "===== 1) 用户与授权（部署安装手册口径：website/website1020）====="
M -e "CREATE USER IF NOT EXISTS 'website'@'%' IDENTIFIED BY 'website1020';
      GRANT ALL PRIVILEGES ON *.* TO 'website'@'%'; FLUSH PRIVILEGES;" >/dev/null
# MySQL 8 默认 caching_sha2_password：Nacos/微服务的 JDBC URL 没开 allowPublicKeyRetrieval，
# 首次认证会走 RSA 取公钥并被拒绝（[db-load-error] / CachingSha2PasswordPlugin）。
# 统一改为 mysql_native_password，避免逐个改各服务的连接串。
M -e "ALTER USER 'website'@'%' IDENTIFIED WITH mysql_native_password BY 'website1020'; FLUSH PRIVILEGES;" >/dev/null
ok "website@'%' 存在并授权（mysql_native_password）"

echo "===== 2) Nacos 配置库 ====="
import_into nacos_config "$DBDIR/nacos-mysql/schema.sql"
ok "nacos_config 表数=$(M -N -e "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema='nacos_config';")"

echo "===== 3) 业务库 ====="
import_into sca_db "$DBDIR/ddl/hotel_schema.sql"
# schema.sql 末尾的 TCC 字段 ALTER 指向其它库的表，这里先剔除，稍后按库执行
import_into sca_db "$DBDIR/ddl/schema.sql" "sed -e '/^ALTER TABLE hotel_/d'"
import_into sca_order_db "$DBDIR/ddl/sca_order_db.sql"
import_into sca_stock_db "$DBDIR/ddl/sca_stock_db.sql"

echo "===== 4) Seata ====="
# seata.sql 尾部是"第三步：往 Nacos 贴配置"的中文说明，不是 SQL，需截断
import_into seata "$DBDIR/ddl/seata.sql" "sed -e '/^-- 第三步/,\$d'"
# undo_log / tcc_fence_log 必须存在于每个业务库（seata.sql 里那段没有 USE 语句）
for db in sca_db sca_order_db sca_stock_db; do
  docker exec -i mysql8 mysql -uroot -p"$MYSQL_ROOT_PW" "$db" <<'SQL' 2>&1 | grep -v "Using a password"
CREATE TABLE IF NOT EXISTS `undo_log` (
  `branch_id` BIGINT NOT NULL, `xid` VARCHAR(128) NOT NULL, `context` VARCHAR(128) NOT NULL,
  `rollback_info` LONGBLOB NOT NULL, `log_status` INT NOT NULL, `log_created` DATETIME(6) NOT NULL,
  `log_modified` DATETIME(6) NOT NULL, UNIQUE KEY `ux_undo_log` (`xid`,`branch_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE TABLE IF NOT EXISTS `tcc_fence_log` (
  `xid` VARCHAR(128) NOT NULL, `branch_id` BIGINT NOT NULL, `operation_context` VARCHAR(128) NOT NULL,
  `action_name` VARCHAR(128) NOT NULL, `status` TINYINT NOT NULL, `gmt_created` TIMESTAMP(3) NOT NULL,
  `gmt_modified` TIMESTAMP(3) NOT NULL, PRIMARY KEY (`xid`,`branch_id`),
  KEY `idx_state_gmt_created` (`status`,`gmt_modified`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
SQL
  ok "$db: undo_log + tcc_fence_log"
done
# seata.sql 的"第二步"在 USE seata 上下文里执行过，会误建到 seata 库，清掉（这两张表只属于业务库）
M seata -e "DROP TABLE IF EXISTS undo_log; DROP TABLE IF EXISTS tcc_fence_log;" >/dev/null
ok "seata 库仅保留 global/branch/lock/distributed_lock"

echo "===== 5) 跨库 TCC 字段（schema.sql 原意，按表所在库执行）====="
M sca_stock_db -e "ALTER TABLE hotel_product_spec ADD COLUMN frozen_stock INT NOT NULL DEFAULT 0 COMMENT '冻结库存(TCC)';" | grep -qv "Duplicate column" || true
M sca_order_db -e "ALTER TABLE hotel_order ADD COLUMN xid VARCHAR(128) DEFAULT NULL COMMENT 'Seata全局事务ID(TCC)';" | grep -qv "Duplicate column" || true
ok "hotel_product_spec.frozen_stock / hotel_order.xid"

echo "===== 6) OAuth 库表（sca-authcenter，落 sca_db）====="
import_into sca_db "$DBDIR/oauth/oauth2.sql"

echo ""
echo "================= 汇总 ================="
M -N -e "SELECT table_schema AS db, COUNT(*) AS tables FROM information_schema.tables
      WHERE table_schema IN ('nacos_config','sca_db','sca_order_db','sca_stock_db','seata')
      GROUP BY table_schema;"
