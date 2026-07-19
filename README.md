# SpringCloudAdmin

基于 **Spring Boot 3.2 + Spring Cloud 2023 + Spring Cloud Alibaba** 的微服务后台管理平台。

项目采用多模块架构，涵盖数据持久化、业务逻辑、远程调用、API网关、安全认证、全文检索等企业级能力，开箱即用。

---

## 技术栈

| 层面 | 技术 | 版本 |
|------|------|------|
| 语言 | Java | 17 |
| 基础框架 | Spring Boot | 3.2.4 |
| 微服务 | Spring Cloud | 2023.0.1 |
| 微服务(阿里) | Spring Cloud Alibaba | 2023.0.1.0 |
| 服务注册/配置 | Nacos | 2.3.2 (SCA内置) |
| 服务调用 | OpenFeign | (Spring Cloud内置) |
| API网关 | Spring Cloud Gateway | (Spring Cloud内置) |
| 持久层 | MyBatis-Plus | 3.5.5 |
| 数据库 | MySQL | 8.3.0 |
| 缓存 | JetCache + Redis | 2.7.6 |
| 安全认证 | Spring Security 6 | (Spring Boot内置) |
| 全文检索 | Spring Data Elasticsearch | (Spring Boot内置) |
| 消息队列 | RabbitMQ (Spring AMQP) | (Spring Boot内置) |
| API文档 | SpringDoc OpenAPI | 2.3.0 |
| 工具库 | Lombok / Hutool / Guava | 1.18.32 / 5.8.26 / 33.0.0 |
| 配置加密 | Jasypt | 3.0.5 |
| 测试 | JUnit 5 | 5.10.2 |
| 构建工具 | Maven | 3.8+ |

---

## 项目结构

```
SpringCloudAdmin/
├── pom.xml                  # 父POM (统一依赖版本管理)
├── sca-common/              # 公共模块
├── sca-dao/                 # 数据库DAO层
├── sca-service/             # 业务Service层
├── sca-rpc/                 # RPC远程调用
├── sca-web/                 # Web接口层 (可独立部署)
├── sca-config/              # 配置中心
├── sca-gateway/             # API网关 (可独立部署)
├── sca-security/            # 安全认证层
└── sca-es/                  # Elasticsearch检索层
```

### 模块依赖关系

```
sca-common (基础公共模块)
    ↓
sca-dao (数据库层)
    ↓
sca-service (业务逻辑层)
    ↓
sca-rpc (远程调用)  ←→  sca-web (REST接口，可部署应用)
    
sca-gateway (API网关，独立部署)
sca-security (安全认证，被web引用)
sca-es (检索层，被service/web引用)
sca-config (配置中心，共享配置)
```

---

## 模块说明

### sca-common — 公共模块

提供全项目共享的基础能力：

- **统一响应封装** `Result<T>` — 所有REST接口返回统一的 `{code, mesg, time, data}` 格式
- **全局异常处理** `GlobalExceptionHandlerAdvice` — 自动捕获参数校验、业务、系统异常
- **响应体自动包装** `RestResponseBodyAdvice` — Controller返回值自动包装为 `Result`
- **用户上下文** `UserContextHolder` — 基于 ThreadLocal 的线程级用户信息存储
- **异常体系** — `BaseException`、`ServiceException`、`SystemErrorType`

### sca-dao — 数据库DAO层

- 基于 **MyBatis-Plus 3.5.5** (Spring Boot 3 Starter)
- `BasePo` 持久化基类 — 审计字段自动填充（createdBy/createdTime/updatedBy/updatedTime）
- `PoMetaObjectHandler` — 从 `UserContextHolder` 获取当前操作用户
- 内置插件：分页、防全表更新删除、SQL性能规范

### sca-service — 业务Service层

- 基于 `IService` 扩展的 `BaseService` 接口
- 集成 **JetCache** 多级缓存（本地 + Redis）

### sca-rpc — RPC远程调用

- 基于 **OpenFeign** 的声明式HTTP客户端
- `FeignHeaderInterceptor` — 微服务间调用时自动透传HTTP Header（Token/用户信息）
- 集成 Spring Cloud LoadBalancer 负载均衡

### sca-web — Web接口层（可部署应用）

- Spring MVC REST Controller
- SpringDoc OpenAPI 文档（`/swagger-ui.html`）
- Nacos 服务注册发现
- 应用入口：`WebApplication`，端口 `8080`

### sca-config — 配置中心

- 基于 **Nacos Config** 的分布式配置管理
- 通过 `bootstrap.yml` 连接 Nacos 配置中心

### sca-gateway — API网关（可部署应用）

- 基于 **Spring Cloud Gateway** (WebFlux)
- `AuthGlobalFilter` — 全局鉴权过滤器（Token校验、白名单放行）
- 支持动态路由、服务发现负载均衡
- 应用入口：`GatewayApplication`，端口 `9000`

### sca-security — 安全认证层

- 基于 **Spring Security 6** (Spring Boot 3.x)
- 无状态JWT认证架构（禁用Session/CSRF）
- `AuthenticationEntryPointHandler` — 401未授权响应
- `AccessDeniedHandlerImpl` — 403权限不足响应
- 方法级权限控制 `@EnableMethodSecurity`

### sca-es — Elasticsearch检索层

- 基于 **Spring Data Elasticsearch**
- `BaseDocument` 文档基类
- 自动扫描 Repository 接口

---

## 依赖管理

所有依赖版本统一由根 `pom.xml` 的 `<dependencyManagement>` 集中管理，子模块只需声明依赖名称，无需指定版本号。

通过 import 以下 BOM 实现版本统一：
- `spring-boot-dependencies`
- `spring-cloud-dependencies`
- `spring-cloud-alibaba-dependencies`
- `hutool-bom`

---

## 快速开始

### 环境要求

- JDK 17+
- Maven 3.8+
- MySQL 8.x
- Nacos 2.3.x
- Redis（可选）
- Elasticsearch（可选）

### 编译

```bash
mvn clean compile
```

### 启动Web服务

```bash
cd sca-web
mvn spring-boot:run
```

### 启动网关

```bash
cd sca-gateway
mvn spring-boot:run
```

### API文档

Web服务启动后访问：http://localhost:8080/swagger-ui.html

---

## 配置说明

### 数据库（sca-web/application.yml）

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/sca_db
    username: root
    password: root
```

### Nacos注册中心

```yaml
spring:
  cloud:
    nacos:
      discovery:
        server-addr: localhost:8848
```

### 网关路由（sca-gateway/application.yml）

```yaml
spring:
  cloud:
    gateway:
      routes:
        - id: sca-web
          uri: lb://sca-web
          predicates:
            - Path=/api/web/**
```

---

## 附录

### Git代理配置（解决GitHub访问超时）

```bash
# 设置代理
git config --global http.proxy http://127.0.0.1:7897

# 取消代理
git config --global --unset http.proxy
git config --global --unset https.proxy
```


### docker镜像拉取问题
Error response from daemon: Get "https://registry-1.docker.io/v2/": dial tcp 31.13.84.34:443: i/o timeout

https://github.com/DaoCloud/public-image-mirror


 vi /etc/docker/daemon.json


```
{
  "registry-mirrors": [
    "https://docker.m.daocloud.io"
  ]
}
```


```
# 加载配置
sudo systemctl daemon-reload 
# 重启 docker
sudo systemctl restart docker 
#查看 docker 状态
sudo systemctl status docker
```

docker run -d -P m.daocloud.io/docker.io/library/nginx

docker pull m.daocloud.io/docker.io/nacos/nacos-server:v2.4.1

