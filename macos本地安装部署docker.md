# macOS 本地安装部署 Docker 手册（Apple Silicon）

> 适用环境：macOS 15.x + Apple 芯片（arm64），**未安装 Homebrew**，`sudo` 需要输入登录密码。
> 本手册基于实机安装过程记录：macOS 15.7.3 / Apple Silicon / 32GB 内存，全程**没有用到一次 sudo**。
> 所有命令在 **终端 (Terminal / iTerm)** 中执行。

## 〇、结论速览

| 步骤 | 是否要密码 | 说明 |
|------|-----------|------|
| 下载 DMG | 否 | 官方源慢，走 DaoCloud 镜像 |
| 装到 /Applications | 否 | `/Applications` 对 admin 组开放写权限 |
| 首次启动 | 否 | Docker Desktop 4.96+ 把 CLI 软链到 `~/.docker/bin`，不再写 `/usr/local/bin`，也不再装特权辅助工具 |
| 配镜像加速器 | 否 | 直接写 `~/.docker/daemon.json`，首次启动前写好即可自动加载 |
| 终端能用 `docker` | 否 | 把 `~/.docker/bin` 加进 `~/.zshrc` 的 PATH |

---

## 一、环境要求

| 项目 | 要求 | 本机实测 |
|------|------|---------|
| 操作系统 | macOS 12 Monterey 及以上 | macOS 15.7.3 |
| 芯片 | Intel / Apple Silicon 均支持，安装包不同 | Apple Silicon (arm64) |
| 内存 | 至少 4GB | 32GB（Docker VM 默认分到 10 CPU / 8GB） |
| 磁盘 | 预留 10GB 以上；安装完 Docker.app 占 2.2GB | 充足 |
| 硬件虚拟化 | 需支持 Hypervisor | `kern.hv_support = 1` |

确认本机信息：

```bash
sw_vers                      # macOS 版本
uname -m                     # arm64 = Apple Silicon；x86_64 = Intel
sysctl -n kern.hv_support    # 输出 1 表示支持虚拟化
```

---

## 二、下载 Docker Desktop 安装包

### 2.1 官方地址

浏览器访问 https://www.docker.com/products/docker-desktop/ ，点 **Download for Mac**，
Apple 芯片自动下 `-apple-chip` 版（约 571MB）。

### 2.2 国内网络：换镜像源（推荐）

官方 `desktop.docker.com` 国内实测只有 **0.5MB/s**，571MB 要十几分钟。
DaoCloud 镜像透传同一份文件，实测 **2.5MB/s，3 分 53 秒下完**：

```bash
# Apple 芯片
curl -L -o ~/Downloads/Docker.dmg \
  "https://files.m.daocloud.io/desktop.docker.com/mac/main/arm64/Docker.dmg"

# Intel 芯片把路径里的 arm64 换成 amd64
```

> - 地址是 Docker 官方路径的透传，会解析到官方最新版；本机下载到的是 **4.96.1 (build 243774)**。
> - 下载中断可断点续传：`curl -L -C - -o ~/Downloads/Docker.dmg "<同一地址>"`。

### 2.3 校验完整性

```bash
ls -lh ~/Downloads/Docker.dmg          # 571MB 左右
hdiutil verify ~/Downloads/Docker.dmg  # 期望输出 checksum ... is VALID
shasum -a 256 ~/Downloads/Docker.dmg   # 本机值见文末附录
```

更硬的保证放在安装后做——Gatekeeper 会校验苹果的公证签名，镜像被篡改过就装不上：

```bash
spctl -a -vv -t install /Applications/Docker.app
# 期望：accepted  source=Notarized Developer ID  origin=Developer ID Application: Docker Inc (9BNSXJN65R)
```

---

## 三、安装到 /Applications

macOS 上 Docker Desktop 就是一个普通 App，**不需要 sudo**
（`/Applications` 属 `root:admin` 且对 admin 组开放写权限，当前用户在 `admin` 组）。

```bash
# 1. 挂载 DMG
hdiutil attach ~/Downloads/Docker.dmg -nobrowse -mountpoint /Volumes/DockerIM

# 2. 复制到应用程序目录
cp -R /Volumes/DockerIM/Docker.app /Applications/

# 3. 卸载镜像 + 清理安装包（省 571MB）
hdiutil detach /Volumes/DockerIM
rm ~/Downloads/Docker.dmg
```

手动方式：双击 `Docker.dmg` → 把 **Docker** 图标拖进 **Applications**。

命令行下载的文件不带 quarantine 属性，不会弹"来自互联网下载"的拦截提示；
浏览器下载的如遇拦截，右键 App → **打开** 即可。

---

## 四、首次启动与 PATH

```bash
open -a Docker
```

首次启动流程：

1. **Welcome to Docker Desktop** → **Accept terms of service**；不需要账号，选 **Skip / Use it without signing in**。
2. 状态栏鲸鱼图标变绿、界面显示 **Docker Desktop is running**，引擎就绪。
3. Docker Desktop 会自动做两件事：
   - 在 `~/.docker/bin/` 下创建 `docker`、`docker-compose`、`kubectl` 等软链；
   - 往 `~/.zshrc` 末尾追加一段 Docker CLI 补全配置（带 `# End of Docker CLI completions` 注释）。

### 4.1 让终端能找到 docker

`~/.docker/bin` 默认不在 PATH 里，`docker` 会报 `command not found`。补一行：

```bash
cat >> ~/.zshrc <<'EOF'

# Docker Desktop CLI
export PATH="$HOME/.docker/bin:$PATH"
EOF
source ~/.zshrc
```

验证：

```bash
which docker          # /Users/a/.docker/bin/docker
docker version        # Client + Server 都要有输出，Server 显示 Docker Desktop 4.96.1
docker compose version
docker ps
```

> **如果 `docker version` 只有 Client 段**，说明引擎还没起来，等 10 秒重试。
>
> **老版本 / Intel Mac 可能弹 `Docker wants to install privileged helper tools`**，那是往
> `/usr/local/bin` 建软链、装 `com.docker.vmnetd`，需要输一次登录密码，属正常流程。
> 本机 4.96.1 + Apple Silicon 没有这一步。若确实需要手动建链：
> `sudo ln -s /Applications/Docker.app/Contents/Resources/bin/docker /usr/local/bin/docker`

---

## 五、配置国内镜像加速器（必做）

不配的话拉 Docker Hub 镜像会 `net/http: TLS handshake timeout` 或 `dial tcp ... i/o timeout`。

### 5.1 命令行一次到位

```bash
mkdir -p ~/.docker
cat > ~/.docker/daemon.json <<'EOF'
{
  "registry-mirrors": [
    "https://docker.1ms.run",
    "https://docker.m.daocloud.io",
    "https://docker.xuanyuan.me"
  ]
}
EOF
```

**首次安装时**在 `open -a Docker` 之前写好这个文件，Docker Desktop 启动会直接加载，不用重启。
已经启动过则重启引擎：

```bash
osascript -e 'quit app "Docker"' && sleep 3 && open -a Docker
```

### 5.2 图形界面方式

Docker Desktop → 右上 **齿轮 (Settings)** → **Docker Engine** → 粘贴上面 JSON → **Apply & restart**。

### 5.3 验证

```bash
docker info | grep -A4 "Registry Mirrors"
```

### 5.4 注意事项（含 Windows 手册踩过的坑）

- `docker.1ms.run` **不支持 `bitnami` 命名空间**；官方 `library/` 和 `apache/` 正常。
- `docker.elastic.co/*`（ES、Kibana）走 Elastic 自己的仓库，**不受 registry-mirrors 影响**，国内一般可直拉。
- `ghcr.io/*`（本项目 rerank 镜像 `ghcr.io/huggingface/text-embeddings-inference`）国内不可直连，
  改用带前缀的镜像：`docker pull ghcr.nju.edu.cn/huggingface/text-embeddings-inference:cpu-1.9`。

---

## 六、拉取项目所需镜像

### 6.1 常规镜像（加速器直接可达）

```bash
docker pull mysql:8.0
docker pull nacos/nacos-server:v2.4.1
docker pull redis:6
docker pull rabbitmq:3.13-management
docker pull confluentinc/cp-zookeeper:7.5.0
docker pull confluentinc/cp-kafka:7.5.0
docker pull provectuslabs/kafka-ui:latest
docker pull apache/rocketmq:5.2.0
docker pull apacherocketmq/rocketmq-dashboard:latest
docker pull seataio/seata-server:2.0.0
docker pull pgvector/pgvector:pg16
docker pull qdrant/qdrant:latest
docker pull quay.io/coreos/etcd:v3.5.14
docker pull milvusdb/milvus:v2.4.9
docker pull docker.elastic.co/elasticsearch/elasticsearch:8.13.4
docker pull docker.elastic.co/kibana/kibana:8.13.4
```

### 6.2 本机实测被卡的两个（加速器白名单/离线限制）

国内公共加速器大多只放行**白名单命名空间**，`minio/*` 与 `bladex/*` 不在其中，
直连 `registry-1.docker.io` 又会被拒（`connection refused`）。实测解法：

```bash
# minio：华为云 SWR 的 docker.io 镜像站（公开、可匿名），拉下来后改回原 tag
docker pull swr.cn-north-4.myhuaweicloud.com/ddn-k8s/docker.io/minio/minio:latest
docker pull swr.cn-north-4.myhuaweicloud.com/ddn-k8s/docker.io/minio/mc:latest
docker tag  swr.cn-north-4.myhuaweicloud.com/ddn-k8s/docker.io/minio/minio:latest minio/minio:latest
docker tag  swr.cn-north-4.myhuaweicloud.com/ddn-k8s/docker.io/minio/mc:latest     minio/mc:latest

# sentinel：1ms.run 放行 bladex 命名空间（daocloud 拒绝、xuanyuan 要付费），用全名前缀拉取后改 tag
docker pull docker.1ms.run/bladex/sentinel-dashboard:1.8.8
docker tag  docker.1ms.run/bladex/sentinel-dashboard:1.8.8 bladex/sentinel-dashboard:1.8.8
```

> 改 tag 只是为了让 compose 里的 `image:` 原样可用；`docker compose up` 不会再去拉已在本地的镜像。
> 选镜像站前先 `docker manifest inspect <前缀>/<镜像>` 验一次，比直接 pull 快得多。

### 6.3 ES 中文分词镜像要本地构建（不在任何 registry 上）

`docker-compose/es` 用的是自造镜像 `es-with-ik:8.13.4`，构建时从 infinilabs 国内源装插件：

```bash
cd docker-compose/es && docker build -t es-with-ik:8.13.4 .   # 实测 3.3s 装好 analysis-ik
```

### 6.4 rerank 模型要预下载（容器运行时不联网）

```bash
cd docker-compose/rerank-mac && bash fetch-model.sh   # 走 hf-mirror，实测 2.1GB
```

---

## 七、启动项目中间件

本项目在 `docker-compose/` 下为每个中间件准备了编排文件（es / kafka / milvus / minio / mysql /
nacos / postgres / qdrant / rabbitmq / rerank / rerank-mac / rocketmq / seata / sentinel）：

```bash
cd /Users/a/source/SpringCloudAdmin

# 0) 共享网络（各 compose 都声明 external: my-custom-network，必须先建）
bash docker-compose/network.sh

# 1) 结构化存储与注册中心
docker compose -f docker-compose/mysql/docker-compose.yml up -d
docker compose -f docker-compose/nacos/docker-compose.yml up -d      # 依赖 MySQL
docker compose -f docker-compose/redis/docker-compose.yml up -d
docker compose -f docker-compose/postgres/docker-compose.yml up -d
docker compose -f docker-compose/seata/docker-compose.yml up -d

# 2) 消息
docker compose -f docker-compose/kafka/docker-compose.yml up -d
docker compose -f docker-compose/rabbitmq/docker-compose.yml up -d
docker compose -f docker-compose/rocketmq/docker-compose.yml up -d

# 3) 检索与向量
docker compose -f docker-compose/es/docker-compose.yml up -d
docker compose -f docker-compose/qdrant/docker-compose.yml up -d

# 4) 对象存储 → Milvus（milvus-standalone 的 MINIO_ADDRESS 指向 minio，必须后起）
docker compose -f docker-compose/minio/docker-compose.yml up -d
docker compose -f docker-compose/milvus/docker-compose.yml up -d

# 5) 精排与流控
docker compose -f docker-compose/rerank-mac/docker-compose.yml up -d
docker compose -f docker-compose/sentinel/docker-compose.yml up -d

# 6) 业务服务 agent-flow（自带 PG16；compose 自包含，秘密/数据都在该目录下）
#    镜像先单独构建：cd /path/to/agent-flow && docker build -t agentflow/server:0.1.0 server/agent-flow-server
#    首跑先生成秘密：cd docker-compose/agent-flow && bash gen-secrets.sh
docker compose -f docker-compose/agent-flow/docker-compose.yml up -d

docker ps --format '{{.Names}}\t{{.Status}}' | sort                  # 逐个等 healthy
```

> `agent-flow` 的 compose 是该项目**唯一**一套部署定义（原 agent-flow 仓库根目录的同名文件已删除）；
> 容器名 `agent-flow-server` / `agent-flow-db`，同挂 `my-custom-network`，其它服务可用 `http://agent-flow-server:8080` 直连。

> **建议启动顺序**：network → MySQL → Nacos → Redis → ES → Kafka → 其余（minio 必须先于 milvus）。
> `docker compose -f xxx up -d` 会等 healthcheck 通过再返回，因此可串行执行；
> rerank-mac 的 `start_period` 是 180s，`up -d` 返回时仍是 `health: starting`，属正常。

功能级验证（比 `healthy` 更能证明"能用作业务"）：

```bash
curl -s -X POST 'http://localhost:9200/_analyze' -H 'Content-Type: application/json' \
  -d '{"analyzer":"ik_max_word","text":"上海证券交易所"}'          # 期望切出 上海/证券交易所/证券
curl -s http://localhost:8088/rerank -H 'Content-Type: application/json' \
  -d '{"query":"苹果发布会","texts":["Apple 发布了新款 iPhone","今天天气不错"]}'   # 期望第一条高分
curl -s http://localhost:6333/collections                           # qdrant
docker exec postgres psql -U postgres -d rag -tAc "select extname from pg_extension"   # vector
docker compose -f docker-compose/minio/docker-compose.yml logs minio-init   # askbase / milvus-bucket 已建
```

端口、账号与 Nacos 基础配置见同目录 `部署安装手册.md`；
Windows 侧的对应流程见 `windows本地安装部署docker-ubuntu.md`。

---

## 八、常用管理命令

```bash
docker ps -a                                    # 所有容器（含已停止）
docker logs -f <容器名>                          # 跟踪日志
docker stop <容器名> ; docker restart <容器名>     # 停止 / 重启
docker compose -f <file> down                   # 停止并删除该编排的容器
docker images                                   # 镜像列表
docker system df                                # 磁盘占用
docker system prune -f                          # 清理悬空镜像/容器
docker exec -it <容器名> sh                     # 进入容器
```

---

## 九、macOS 特有设置（建议调整）

Docker Desktop → **Settings**（齿轮）：

| 分类 | 项 | 建议 | 说明 |
|------|-----|------|------|
| Resources | File Sharing | 添加 `/Users/a/source` | 不加则挂载宿主目录时容器读写失败 |
| Resources | CPU / Memory | 本机调到 10 核 / 16GB | 同时跑 ES + Kafka + MySQL + Milvus + rerank 等 20 个容器；8GB 下 rerank-mac（amd64 转译）warmup 阶段必被 OOM kill |
| Resources | Disk image location | 空间不足时迁到外置盘 | 默认在 `~/Library/Containers/com.docker.docker/Data/vms/0/data/Docker.raw` |
| General | Start Docker Desktop when you sign in | 按需关闭 | 省内存 |
| General | Automatic updates | 可关 | 避免大版本更新改动 CLI 路径 |

---

## 十、常见问题排查

| 问题 | 原因 / 解决方案 |
|------|----------------|
| `zsh: command not found: docker` | `~/.docker/bin` 不在 PATH，见第四节 4.1；改完记得 `source ~/.zshrc` 或重开终端 |
| `Cannot connect to the Docker daemon at unix:///...docker.sock` | Docker Desktop 没启动或还在初始化；看鲸鱼图标，等 10 秒重试 |
| `docker version` 只有 Client 段 | 同上，引擎未就绪 |
| 拉取镜像 `TLS handshake timeout` / `i/o timeout` | 加速器没生效，见第五节，改完必须重启引擎 |
| 拉取镜像 `denied: ... 这镜像不在白名单` | 加速器只放行白名单命名空间（`minio/*`、`bladex/*` 常被拒）。换 6.2 的源或换 tag 前缀，别反复重试同一个 |
| `docker manifest inspect registry-1.docker.io/...` → `connection refused` | 直连 Docker Hub 被网络阻断，本机只能靠加速器；先 `manifest inspect` 探哪个源可用再 pull |
| 挂载目录容器内为空 / `permission denied` | Settings → Resources → **File Sharing** 没加该路径 |
| rerank-mac 起来就 `health: starting` 后反复重启，`exit=137 OOMKilled=true` | amd64 转译下 TEI 默认 warmup（`max-batch-tokens=16384` + 9 个分词 worker）瞬时 6GB+，与其他 19 个容器抢 16GB VM 必被 OOM。已在 compose 里收紧为 `--max-batch-tokens 1024 --tokenization-workers 2 --max-concurrent-requests 16`，实测 healthy 且中文排序正确 |
| `docker.elastic.co` 拉取慢 | 加速器无效，走 Elastic 官方仓库；可挂代理 |
| `ghcr.io` 拉取失败 | 换 `ghcr.nju.edu.cn` 前缀，或配代理 |
| 端口冲突 `Address already in use` | `lsof -i :<端口>` 找占用进程；或改 compose 里的宿主端口映射 |
| 下载 DMG 只有几百 KB/s | 官方源限速，改用 DaoCloud 镜像（见 2.2） |
| Docker Desktop 起不来 / 一直 Starting | 鲸鱼菜单 → Troubleshoot → **Clean / Purge data**（会删本地镜像与卷，慎用） |
| 磁盘被 Docker 撑爆 | `docker system prune -a`；或迁移第九节的 Disk image location |
| 卸载 | Docker → Troubleshoot → **Uninstall**；再删 `/Applications/Docker.app`、`~/.docker`，并清掉 `~/.zshrc` 里 Docker 加的两段 |

---

## 附录：本机安装实测数据

| 项 | 值 |
|----|-----|
| 安装日期 | 2026-10-21 |
| Docker Desktop | 4.96.1 (build 243774) |
| Docker Engine / CLI | 29.9.2 (API 1.56) |
| Docker Compose | v5.6.0 |
| containerd | v2.4.2 |
| DMG 大小 | 571MB，镜像下载耗时 3 分 53 秒 @2.5MB/s |
| DMG sha256 | `7f99f0bfd921f141f4dc7a146617b56666a4156f4bc5b0d19f2671c8290c3ff0` |
| Docker.app 占用 | 2.2GB（`/Applications/Docker.app`） |
| Docker VM 默认规格 | 10 CPU / 16384 MiB（`docker info` 实测 16745824256 B；8GB 跑不起 rerank-mac） |
| CLI 路径 | `~/.docker/bin/docker` → `/Applications/Docker.app/Contents/Resources/bin/docker` |
| 配置文件 | `~/.docker/daemon.json`（registry-mirrors） |
| sudo 使用次数 | 0 |

### 中间件全栈实测（2026-10-22 00:16）

| 容器 | 端口 | 状态 | 备注 |
|---|---|---|---|
| mysql8 | 3306 | healthy | DDL 已导入，见 `init-data-mac.sh` |
| nacos | 8848/9848 | healthy | 配置项已发布，见 `publish-nacos-mac.sh` |
| redis | 6379 | healthy | |
| postgres | 5432 | healthy | pgvector 扩展随镜像自带 |
| seata-server | 7091/8091 | healthy | |
| zookeeper / kafka / kafka-ui | 2181 / 9092 / 8090 | Up | |
| rabbitmq | 5672/15672 | healthy | |
| rmqnamesrv / rmqbroker / rmqdashboard | 9876 / 10911 / 8080 | healthy / Up | |
| es（`es-with-ik:8.13.4`） | 9200 | healthy | 本地构建，`_analyze` 实测 `ik_max_word` 出词正确 |
| kibana | 5601 | healthy | |
| qdrant | 6333/6334 | healthy | |
| minio / minio-init | 9000/9001 / 一次性 | healthy / Exited(0) | 已建 `askbase`、`milvus-bucket` |
| milvus-etcd / milvus-standalone | 2379 / 19530、9091 | healthy | 对象存储走共享 minio |
| rerank-mac | 8088 | healthy | amd64 转译；收紧 warmup 后 `/rerank` 中文排序实测正确 |
| sentinel-dashboard | 8858 | 待镜像 | 加速器白名单拒 `bladex/*`，走 6.2 的 1ms.run 前缀拉取 |
