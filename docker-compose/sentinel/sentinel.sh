#!/bin/bash
echo "=== 启动 Sentinel Dashboard ==="
docker compose up -d
echo "=== 查看启动日志 ==="
docker logs -f sentinel-dashboard


# 查看容器状态
docker ps | grep sentinel

# 查看日志（出现 "Started" 表示成功）
docker logs sentinel-dashboard

# 访问 Sentinel Dashboard 控制台
# http://服务器IP:8858
# 默认账号：sentinel / sentinel123
