# Elasticsearch 集群 Docker 部署

## 概述

本目录包含用于部署 3 节点 Elasticsearch 8.13.4 集群的 Docker Compose 配置。

## 集群架构

- **节点数量**: 3 个节点
- **版本**: Elasticsearch 8.13.4
- **集群名称**: es-docker-cluster
- **安全认证**: 已禁用（开发环境）

## 节点信息

| 节点 | HTTP 端口 | 传输端口 | 容器名称 |
|------|-----------|----------|----------|
| es01 | 9200      | 9300     | es01     |
| es02 | 9201      | 9301     | es02     |
| es03 | 9202      | 9302     | es03     |

## 快速启动

### Windows (PowerShell)

```powershell
# 启动集群
.\start.ps1

# 停止集群
.\stop.ps1
```

### Linux/Mac (Bash)

```bash
# 启动集群
chmod +x start.sh
./start.sh

# 停止集群
docker-compose down
```

## 验证集群状态

```bash
# 查看集群健康状态
curl http://localhost:9200/_cluster/health?pretty

# 查看节点信息
curl http://localhost:9200/_cat/nodes?v

# 查看集群状态
curl http://localhost:9200/_cluster/state?pretty
```

## 配置说明

### 内存设置

每个节点分配 512MB 堆内存：
- `ES_JAVA_OPTS=-Xms512m -Xmx512m`

可根据实际服务器配置调整，建议不超过物理内存的 50%。

### 数据持久化

数据目录挂载到本地：
- `./data/es01` - es01 节点数据
- `./data/es02` - es02 节点数据
- `./data/es03` - es03 节点数据

日志目录挂载到本地：
- `./logs/es01` - es01 节点日志
- `./logs/es02` - es02 节点日志
- `./logs/es03` - es03 节点日志

### 网络配置

使用外部网络 `my-custom-network`，确保已创建该网络：

```bash
docker network create \
  --driver=bridge \
  --subnet=172.20.0.0/16 \
  --gateway=172.20.0.1 \
  my-custom-network
```

## Spring Boot 集成

在 `application.yml` 中配置 ES 集群地址：

```yaml
spring:
  elasticsearch:
    uris: 
      - http://localhost:9200
      - http://localhost:9201
      - http://localhost:9202
```

## 生产环境建议

1. **启用安全认证**: 设置 `xpack.security.enabled=true`
2. **增加内存**: 根据数据量调整 `ES_JAVA_OPTS`
3. **配置副本**: 默认每个索引 1 个副本，可根据需要调整
4. **监控告警**: 集成 Prometheus + Grafana 监控
5. **备份策略**: 配置快照和恢复机制

## 故障排查

### 查看日志

```bash
# 查看 es01 日志
docker logs es01

# 查看所有节点日志
docker-compose logs -f
```

### 常见问题

1. **内存不足**: 增加 Docker 内存限制或调整 `ES_JAVA_OPTS`
2. **节点无法加入集群**: 检查网络配置和 `discovery.seed_hosts` 设置
3. **权限问题**: 确保数据目录有正确权限（Linux/Mac 需要 `chmod 777`）

## 参考文档

- [Elasticsearch 官方文档](https://www.elastic.co/guide/en/elasticsearch/reference/8.13/index.html)
- [Docker 部署指南](https://www.elastic.co/guide/en/elasticsearch/reference/8.13/docker.html)
