# Windows 本地安装部署 Docker + Ubuntu (WSL2) 手册

## 一、环境要求

| 项目 | 要求 |
|------|------|
| 操作系统 | Windows 10 (64位) Pro/Enterprise/Education 1903+ 或 Windows 11 |
| 内存 | 至少 4GB RAM |
| CPU | 支持虚拟化（BIOS 中需开启 Intel VT-x / AMD-V） |

---

## 二、安装 WSL2

### 2.1 在线安装（网络正常时）

以**管理员身份**打开 PowerShell，执行：

```powershell
wsl --install
```

安装完成后**重启电脑**。

### 2.2 离线手动安装 Ubuntu（网络受限时）

国内网络环境下，`wsl --install` 在线下载 Ubuntu 经常失败，报错如：

```
正在下载: Ubuntu
服务器返回无效或不可识别的响应
错误代码: Wsl/InstallDistro/0x80072f78
```

此时采用**手动离线安装**方式：

**步骤 1：安装 WSL 核心组件（不下载发行版）**

```powershell
wsl --install --no-distribution
```

安装完成后重启电脑。

**步骤 2：手动下载 Ubuntu 安装包**

浏览器访问以下地址，选择 Ubuntu 22.04 或 Ubuntu 24.04 下载：

- https://aka.ms/wslubuntu ---选择这个
- https://learn.microsoft.com/en-us/windows/wsl/install-manual#downloading-distributions

下载文件为 `.AppxBundle` 或 `.msixbundle` 格式。

**步骤 3：安装 Ubuntu**

下载完成后双击安装，或在 PowerShell 中执行：

```powershell
Add-AppxPackage .\Ubuntu_2204.x.x_x64.appx
```

**步骤 4：验证安装**

```powershell
wsl --status
wsl --list --verbose
```

确认默认版本为 2 且 Ubuntu 已安装。

---

## 三、初始化 Ubuntu

首次启动 Ubuntu 时，终端会提示创建 Linux 默认用户：

```
Installing, this may take a few minutes...
Please create a default UNIX user account. The username does not need to match your Windows username.
Enter new UNIX username:
```

按提示操作：

1. **输入用户名**：使用小写字母和数字（如 `huanglulu`），按回车
2. **输入密码**：设置密码（输入时屏幕不显示字符，正常现象），按回车
3. **确认密码**：再次输入密码，按回车

设置完成后出现如下提示符，说明 Ubuntu 就绪：

```
huanglulu@DESKTOP-XXXX:~$
```

> **注意**：首次启动可能出现代理警告：
> `wsl: 检测到 localhost 代理配置，但未镜像到 WSL。`
> 这不影响正常使用，可忽略。

---

## 四、安装 Docker Desktop

### 4.1 下载

访问官网下载：https://www.docker.com/products/docker-desktop/

点击 **Download for Windows**，下载 `Docker Desktop Installer.exe`。

### 4.2 安装

1. 双击安装包
2. 勾选 **Use WSL 2 instead of Hyper-V**（推荐）
3. 点击 **OK** 开始安装
4. 安装完成后**重启电脑**

### 4.3 首次启动

打开 Docker Desktop，接受服务条款，等待 Docker Engine 启动完成（系统托盘出现鲸鱼图标且为绿色）。

---

## 五、开启 WSL 集成（重要）

Docker Desktop 默认不会自动与 WSL 中的 Ubuntu 集成。如果不开启，在 Ubuntu 终端中执行 `docker` 会报错：

```
The command 'docker' could not be found in this WSL 2 distro.
We recommend to activate the WSL integration in Docker Desktop settings.
```

**解决步骤：**

1. 打开 **Docker Desktop**
2. 点击右上角 **齿轮图标（Settings）**
3. 左侧选择 **Resources** → **WSL Integration**
4. 开启 **Enable integration with additional distros**
5. 勾选你的 Ubuntu 发行版（如 `Ubuntu`）
6. 点击 **Apply & restart**

完成后回到 Ubuntu 终端，执行 `docker ps` 即可正常使用。

---

## 六、配置国内镜像加速器

国内直接拉取 Docker Hub 镜像会超时，必须配置镜像加速器。

### 6.1 配置方法

1. 打开 **Docker Desktop** → **Settings**（齿轮图标）
2. 左侧选择 **Docker Engine**
3. 在 JSON 配置中修改/添加：

```json
{
  "registry-mirrors": [
    "https://docker.1ms.run",
    "https://docker.xuanyuan.me"
  ]
}
```

4. 点击 **Apply & restart**，等待 Docker 重启

### 6.2 验证

```powershell
docker info
```

输出中包含 `Registry Mirrors` 字样即为生效。

### 6.3 注意事项

- `docker.1ms.run` 代理**不支持** `bitnami` 仓库镜像，但支持 `library/`（官方）和 `apache/` 命名空间。
- 使用 `apache/kafka` 时，tag 必须为完整语义版本（如 `3.6.0`），不能省略 `.0`。

---

## 七、文件系统路径说明

### 7.1 在 Windows 中访问 Ubuntu 文件

在 Windows 资源管理器地址栏输入：

```
\\wsl$\Ubuntu
```

即可浏览 Ubuntu 文件系统。输入 `\\wsl$\` 可查看所有已安装的 WSL 发行版。

### 7.2 在 Ubuntu 中访问 Windows 磁盘

WSL 中 Windows 磁盘自动挂载在 `/mnt/` 下：

```bash
/mnt/c/       # C盘
/mnt/d/       # D盘
/mnt/e/       # E盘
```

### 7.3 Docker 数据挂载建议

| 挂载方式 | 示例路径 | 优点 | 缺点 |
|----------|---------|------|------|
| 挂载 Windows 目录 | `/mnt/e/docker-data/mysql` | Windows 下可直接查看数据 | 跨文件系统 IO 性能略低 |
| 挂载 Ubuntu 目录 | `/home/huanglulu/docker-data/mysql` | 性能更好 | 查看数据需通过 `\\wsl$` |

**建议**：MySQL、Elasticsearch 等中间件数据放在 **Ubuntu 目录**下，性能更好。

docker-compose 挂载写法示例：

```yaml
volumes:
  - /home/huanglulu/docker-data/mysql:/var/lib/mysql
```

---

## 八、拉取镜像与启动服务

### 8.1 拉取项目所需镜像

```bash
# MySQL
docker pull mysql:8.0

# Nacos
docker pull nacos/nacos-server:v2.3.2

# Redis
docker pull redis:7

# Elasticsearch
docker pull elasticsearch:7.17.10

# Kafka (Apache官方)
docker pull apache/kafka:3.6.0

# Kibana
docker pull kibana:7.17.10
```

### 8.2 使用 docker-compose 一键启动（推荐）

项目已为各中间件准备了 docker-compose 文件，进入对应目录执行：

```bash
# 启动 MySQL
docker compose -f docker-compose/mysql/docker-compose.yml up -d

# 启动 Nacos
docker compose -f docker-compose/nacos/docker-compose.yml up -d

# 启动 Redis
docker compose -f docker-compose/redis/docker-compose.yml up -d

# 启动 Elasticsearch
docker compose -f docker-compose/es/docker-compose.yml up -d

# 启动 Kafka
docker compose -f docker-compose/kafka/docker-compose.yml up -d
```

> **建议启动顺序**：MySQL → Nacos → Redis → ES → Kafka（因为 Nacos 依赖 MySQL）

---

## 九、常用管理命令

```bash
# 查看所有容器状态
docker ps -a

# 查看运行中的容器
docker ps

# 查看容器日志
docker logs <容器名>

# 停止某个容器
docker stop <容器名>

# 重启某个容器
docker restart <容器名>

# 停止并删除容器（docker-compose）
docker compose down

# 查看镜像列表
docker images
```

---

## 十、常见问题排查

| 问题 | 解决方案 |
|------|---------|
| `wsl --install` 报错 `0x80072f78` | 网络问题，改用手动离线安装（见 2.2） |
| WSL 提示虚拟化未开启 | 重启进入 BIOS，开启 Intel VT-x / AMD-V |
| Ubuntu 终端中 `docker` 命令不存在 | 在 Docker Desktop 中开启 WSL Integration（见第五节） |
| 拉取镜像超时 | 配置国内镜像加速器（见第六节） |
| `bitnami` 镜像拉取失败 | 改用 `apache/` 命名空间镜像 |
| Docker Desktop 启动卡住 | 确保 WSL2 为默认后端，关闭 Hyper-V 冲突 |
