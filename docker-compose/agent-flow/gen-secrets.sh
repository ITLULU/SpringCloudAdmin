#!/usr/bin/env bash
# 生成 agent-flow 容器所需的本机秘密文件（./secrets，目录 700、文件 600，已被 .gitignore 覆盖，绝不入库）。
# 幂等：已存在的文件一律不覆盖。
set -euo pipefail

DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)/secrets"
mkdir -p "$DIR"
chmod 700 "$DIR"

rand() { # rand BYTES -> base64url 无填充
    openssl rand -base64 "$1" | tr -d '=+/' | head -c "$2"
}

write_once() { # write_once NAME CONTENT
    local f="$DIR/$1"
    if [[ -e "$f" ]]; then
        echo "跳过 $1（已存在）"
        return 0
    fi
    umask 077
    printf '%s' "$2" > "$f"
    echo "生成 $1"
}

# 平台自有秘密：可直接随机
write_once db_password "$(rand 48 24)"
write_once jwt_secret "$(rand 64 48)"                      # JWT HS256 要求 ≥32B
write_once master_key "$(openssl rand -base64 32)"          # AES-256-GCM 主密钥：base64(32B) 原样
write_once metrics_token "$(rand 64 48)"                    # Prometheus 抓取专用令牌（P7-2c）
# 主密钥轮换窗口（P7-3#16）：默认空=不启用双读；轮换时把旧 master_key 值写进来，全部凭证重存后再清空
write_once master_key_previous ""

# 外部凭证与管理员口令：只从环境继承或人工写入，脚本不猜
for pair in "ai_api_key:AI_API_KEY" "bailian_api_key:BAILIAN_API_KEY" \
            "wechat_appid:WECHAT_APPID" "wechat_secret:WECHAT_SECRET" \
            "bootstrap_admin_password:AGENTFLOW_BOOTSTRAP_ADMIN_PASSWORD"; do
    name=${pair%%:*}; envvar=${pair##*:}
    if [[ -e "$DIR/$name" ]]; then
        echo "跳过 $name（已存在）"
    elif [[ -n "${!envvar:-}" ]]; then
        write_once "$name" "${!envvar}"
    else
        umask 077
        : > "$DIR/$name"
        echo "占位 $name 为空 —— 部署前必须手工写入（echo -n '值' > $DIR/$name），否则对应节点执行报配置错"
    fi
done

# 对象存储副本同步（P7-3#12b）：默认不启用，空文件即"未配置"（entrypoint 不会拿空串去签名）。
# 要启用（AGENTFLOW_STORAGE_ENABLED=true）时必须先把 AK/SK 手工写进来，否则启动即失败。
for pair in "storage_access_key:AGENTFLOW_STORAGE_ACCESS_KEY" "storage_secret_key:AGENTFLOW_STORAGE_SECRET_KEY"; do
    name=${pair%%:*}; envvar=${pair##*:}
    if [[ -e "$DIR/$name" ]]; then
        echo "跳过 $name（已存在）"
    elif [[ -n "${!envvar:-}" ]]; then
        write_once "$name" "${!envvar}"
    else
        umask 077
        : > "$DIR/$name"
        echo "占位 $name 为空 —— 未启用对象存储同步时保持为空；启用时 echo -n '值' > $DIR/$name"
    fi
done

ls -l "$DIR"
echo "完成。目录权限 700、文件 600；这些文件只被挂进容器 /run/secrets，不进 compose 明文与镜像层。"
