#!/bin/bash
echo "=== 启动 Seata Server ==="
docker compose up -d
echo "=== 查看启动日志 ==="
docker logs -f seata-server



# 查看容器状态
docker ps | grep seata

# 查看日志（出现 "Server started" 表示成功）
docker logs seata-server

# 验证 Nacos 注册（访问 Nacos 控制台）
# http://服务器IP:8848/nacos → 服务列表 → 应出现 seata-server

# 验证管理控制台（Seata 2.x 内置）
# http://服务器IP:7091
# 默认账号：seata / seata


