#!/usr/bin/env bash
# macOS 本机无需 sudo（Docker Desktop 的 docker.sock 属当前用户）；Linux 上请按需要加回 sudo。
set -euo pipefail
docker network inspect my-custom-network >/dev/null 2>&1 && { echo "my-custom-network 已存在"; exit 0; }
docker network create \
  --driver=bridge \
  --subnet=172.20.0.0/16 \
  --gateway=172.20.0.1 \
  my-custom-network
