# Elasticsearch + Kibana 集群 Docker 部署

## 概述

本目录包含用于部署 3 节点 Elasticsearch 8.13.4 集群和 Kibana 的 Docker Compose 配置。

## 集群架构

- **ES 节点数量**: 3 个节点
- **Kibana**: 1 个实例
- **版本**: Elasticsearch/Kibana 8.13.4
- **集群名称**: es-docker-cluster
- **安全认证**: 已禁用（开发环境）

## 节点信息

### Elasticsearch 节点

| 节点 | HTTP 端口 | 传输端口 | 容器名称 |
|------|-----------|----------|----------|
| es01 | 9200      | 9300     | es01     |
| es02 | 9201      | 9301     | es02     |
| es03 | 9202      | 9302     | es03     |

### Kibana

| 服务 | 端口 | 容器名称 | 访问地址 |
|------|------|----------|----------|
| kibana | 5601 | kibana | http://localhost:5601 |

## 快速启动

### 方式一：一键启动（推荐）

```powershell
# 启动 ES 集群 + Kibana
.\start-all.ps1

# 停止所有服务
.\stop-all.ps1
```

### 方式二：分别启动

#### 启动 Elasticsearch 集群

```powershell
# 启动 ES 集群
.\start.ps1

# 停止 ES 集群
.\stop.ps1
```

#### 启动 Kibana

```powershell
# 启动 Kibana
.\start-kibana.ps1

# 停止 Kibana
.\stop-kibana.ps1
```

### Linux/Mac (Bash)

```bash
# 启动 ES 集群
chmod +x start.sh
./start.sh

# 启动 Kibana
docker-compose -f docker-compose.kibana.yml up -d

# 停止所有
docker-compose -f docker-compose.kibana.yml down
docker-compose down
```

## 验证集群状态

### Elasticsearch

```bash
# 查看集群健康状态
curl http://localhost:9200/_cluster/health?pretty

# 查看节点信息
curl http://localhost:9200/_cat/nodes?v

# 查看集群状态
curl http://localhost:9200/_cluster/state?pretty
```

### Kibana

```bash
# 查看 Kibana 状态
curl http://localhost:5601/api/status
```

或者直接访问 http://localhost:5601 查看 Web 界面。

## 配置说明

### Elasticsearch 内存设置

每个节点分配 512MB 堆内存：
- `ES_JAVA_OPTS=-Xms512m -Xmx512m`

可根据实际服务器配置调整，建议不超过物理内存的 50%。

### Kibana 配置

- **ES 连接**: 自动连接到 es01, es02, es03 三个节点
- **语言**: 已设置为中文 (zh-CN)
- **加密密钥**: 使用预设的 32 字节密钥（生产环境请更换）

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

## Kibana 使用指南

### 首次访问

1. 打开浏览器访问 http://localhost:5601
2. 由于禁用了安全认证，直接进入 Kibana 主界面

### 创建索引模式

1. 进入 **Management** > **Stack Management**
2. 选择 **Kibana** > **Index Patterns**
3. 点击 **Create index pattern**
4. 输入索引名称模式（如 `logs-*`）
5. 选择时间字段（如 `@timestamp`）
6. 点击 **Create index pattern**

### 探索数据

1. 进入 **Analytics** > **Discover**
2. 选择已创建的索引模式
3. 开始查询和分析数据

### 创建可视化

1. 进入 **Analytics** > **Visualize Library**
2. 点击 **Create visualization**
3. 选择图表类型（如柱状图、饼图等）
4. 配置数据源和聚合方式
5. 保存可视化

## 生产环境建议

1. **启用安全认证**: 
   - ES: 设置 `xpack.security.enabled=true`
   - Kibana: 配置正确的用户名密码
   
2. **更换加密密钥**: 修改 `XPACK_ENCRYPTEDSAVEDOBJECTS_ENCRYPTIONKEY`

3. **增加内存**: 根据数据量调整 `ES_JAVA_OPTS`

4. **配置副本**: 默认每个索引 1 个副本，可根据需要调整

5. **监控告警**: 集成 Prometheus + Grafana 监控

6. **备份策略**: 配置快照和恢复机制

## 故障排查

### 查看日志

```bash
# 查看 ES 日志
docker logs es01
docker logs es02
docker logs es03

# 查看 Kibana 日志
docker logs kibana

# 查看所有日志
docker-compose logs -f
docker-compose -f docker-compose.kibana.yml logs -f
```

### 常见问题

1. **Kibana 无法连接 ES**
   - 确保 ES 集群已启动且健康
   - 检查网络配置是否正确
   - 查看 Kibana 日志获取详细错误信息

2. **内存不足**
   - 增加 Docker 内存限制
   - 调整 `ES_JAVA_OPTS` 减少堆内存

3. **节点无法加入集群**
   - 检查网络配置和 `discovery.seed_hosts` 设置
   - 确保所有节点使用相同的集群名称

4. **Kibana 启动慢**
   - 首次启动需要 1-2 分钟进行优化
   - 检查 ES 集群是否完全就绪

## 参考文档

- [Elasticsearch 官方文档](https://www.elastic.co/guide/en/elasticsearch/reference/8.13/index.html)
- [Kibana 官方文档](https://www.elastic.co/guide/en/kibana/8.13/index.html)
- [Docker 部署指南](https://www.elastic.co/guide/en/elasticsearch/reference/8.13/docker.html)
