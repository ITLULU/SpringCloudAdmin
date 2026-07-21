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
| 分布式事务 | Seata | (Spring Cloud Alibaba内置) |
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
├── sca-rpc/                 # RPC远程调用 (Feign客户端 + 共享DTO)
├── sca-web/                 # Web接口层 (可独立部署，:8080)
├── sca-order/               # 订单微服务 (可独立部署，:8081)
├── sca-stock/               # 库存微服务 (可独立部署，:8082)
├── sca-config/              # 配置中心
├── sca-gateway/             # API网关 (可独立部署，:9000)
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
sca-rpc (远程调用)  ←→  sca-web (REST接口，:8080)
                              │
                    OpenFeign │ (跨服务调用)
                    ┌─────────┼─────────┐
                    ↓                   ↓
              sca-order(:8081)    sca-stock(:8082)
    
sca-gateway (API网关，:9000)
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
- **安全工具类** `SecurityUtils` — 从 Spring Security SecurityContext 获取当前登录用户（替代自定义 ThreadLocal）
- **异常体系** — `BaseException`、`ServiceException`、`SystemErrorType`

### sca-dao — 数据库DAO层

- 基于 **MyBatis-Plus 3.5.5** (Spring Boot 3 Starter)
- `BasePo` 持久化基类 — 审计字段自动填充（createdBy/createdTime/updatedBy/updatedTime）
- `PoMetaObjectHandler` — 从 `SecurityUtils` 获取当前操作用户
- 内置插件：分页、防全表更新删除、SQL性能规范

### sca-service — 业务Service层

- 基于 `IService` 扩展的 `BaseService` 接口
- 集成 **JetCache** 多级缓存（本地 + Redis）

### sca-rpc — RPC远程调用

- 基于 **OpenFeign** 的声明式HTTP客户端
- `FeignHeaderInterceptor` — 微服务间调用时自动透传HTTP Header（Token/用户信息）
- 集成 Spring Cloud LoadBalancer 负载均衡
- `OrderFeignClient` — 调用订单服务（sca-order）创建/取消/查询订单
- `StockFeignClient` — 调用库存服务（sca-stock）扣减/归还库存
- `rpc/client/dto/*` — 跨服务共享的请求/响应 DTO

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

### sca-order — 订单微服务（可独立部署）

- 独立 Spring Boot 应用，端口 `8081`
- 独立数据库 `sca_order_db`（hotel_order + hotel_order_item）
- 内部接口 `/inner/order/*`（供 Feign 调用，无需认证）
- 职责：订单创建、订单取消、订单查询
- 支持 Seata 分布式事务（AT 模式）

### sca-stock — 库存微服务（可独立部署）

- 独立 Spring Boot 应用，端口 `8082`
- 独立数据库 `sca_stock_db`（hotel_product_spec）
- 内部接口 `/inner/stock/*`（供 Feign 调用，无需认证）
- 职责：库存扣减（乐观锁）、库存归还、库存查询
- 支持 Seata 分布式事务（AT 模式）

---

## 下单流程（分布式架构）

```
用户请求 → Gateway(:9000) → sca-web(:8080)
                                │
                                │  HotelOrderController.create()
                                │
                  ┌─────────────┼─────────────────┐
                  │             │                 │
                  ↓             ↓                 ↓
           本地校验      Feign调用sca-stock   Feign调用sca-order
         (用户/行程/     /inner/stock/deduct   /inner/order/create
          入住状态)        (批量扣减库存)         (创建订单+明细)
                  │             │                 │
                  └─────────────┴─────────────────┘
                                │
                     [Seata @GlobalTransactional]
                     分布式事务一致性保证
```

### 详细流程

| 步骤 | 服务 | 操作 | 失败处理 |
|------|------|------|----------|
| 1 | sca-web | 校验用户、行程有效性、入住状态 | 直接返回错误 |
| 2 | sca-web | 查询商品/规格（获取名称和价格） | 直接返回错误 |
| 3 | sca-stock | Feign调用：批量扣减库存（乐观锁） | 返回库存不足 |
| 4 | sca-order | Feign调用：创建订单+订单明细 | 补偿归还库存 |

### 取消订单流程

| 步骤 | 服务 | 操作 |
|------|------|------|
| 1 | sca-web | 查询订单详情（获取明细用于归还库存） |
| 2 | sca-order | Feign调用：更新订单状态为已取消 |
| 3 | sca-stock | Feign调用：批量归还库存 |

---

## Seata 分布式事务

项目使用 **Seata AT 模式** 保证下单流程的分布式事务一致性。

### 架构角色

| 角色 | 服务 | 说明 |
|------|------|------|
| TC (Transaction Coordinator) | Seata Server | 协调全局事务的提交/回滚 |
| TM (Transaction Manager) | sca-web | 全局事务发起者（`@GlobalTransactional`） |
| RM (Resource Manager) | sca-order / sca-stock | 分支事务参与者（本地数据库操作） |

### 事务流程

```
TM(sca-web)                    TC(Seata Server)              RM(sca-order/stock)
    │                               │                              │
    ├── 1. begin ─────────────────→ │                              │
    │                               │                              │
    ├── 2. Feign ──────────────────────────────────────────────→ 执行本地SQL
    │                               │  ←── 3. 注册分支事务 ─────── │
    │                               │  ──→ 4. 确认/回滚 ─────────→ │
    │                               │                              │
    ├── 5. commit/rollback ────────→│  ──→ 6. 异步清理undo_log ──→ │
    │                               │                              │
```

### 启用步骤

1. **部署 Seata Server**（TC）
2. **数据库建 undo_log 表**（每个参与服务对应的数据库）
3. **取消 pom.xml 中 Seata 依赖注释**（sca-web、sca-order、sca-stock）
4. **取消 application.yml 中 Seata 配置注释**（三个服务）
5. **取消 Controller 中 `@GlobalTransactional` 注解注释**

### 配置示例（已预留在各服务 application.yml 中）

```yaml
seata:
  enabled: true
  application-id: ${spring.application.name}
  tx-service-group: sca-tx-group
  service:
    vgroup-mapping:
      sca-tx-group: default
  registry:
    type: nacos
    nacos:
      server-addr: 47.116.45.17:8848
      group: SEATA_GROUP
```

### undo_log 表（每个数据库都需要）

```sql
CREATE TABLE IF NOT EXISTS undo_log (
    id            bigint(20)   NOT NULL AUTO_INCREMENT,
    branch_id     bigint(20)   NOT NULL,
    xid           varchar(100) NOT NULL,
    context       varchar(128) NOT NULL,
    rollback_info longblob     NOT NULL,
    log_status    int(11)      NOT NULL,
    log_created   datetime(6)  NOT NULL,
    log_modified  datetime(6)  NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY ux_undo_log (xid, branch_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Seata AT模式 undo_log';
```

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
- Seata Server（可选，启用分布式事务时需要）

### 编译

```bash
mvn clean compile
```

### 启动Web服务

```bash
cd sca-web
mvn spring-boot:run
```

### 启动订单服务

```bash
cd sca-order
mvn spring-boot:run
```

### 启动库存服务

```bash
cd sca-stock
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
          filters:
            - StripPrefix=1
        - id: sca-order
          uri: lb://sca-order
          predicates:
            - Path=/api/order/**
          filters:
            - StripPrefix=1
        - id: sca-stock
          uri: lb://sca-stock
          predicates:
            - Path=/api/stock/**
          filters:
            - StripPrefix=1
```

## 网关选型


在 Spring Cloud Alibaba 生态中，网关选型有两个主要选项：

| 对比项 | Spring Cloud Gateway | Higress (阿里新一代网关) |
|--------|---------------------|-------------------------|
| 本质 | 基于 Spring WebFlux + Netty | 基于 Envoy + Istio 的云原生网关 |
| 语言 | Java | C++ (核心) + Java (插件) |
| 定位 | Spring 生态标准网关 | K8s Ingress / 微服务网关二合一 |
| 编程模型 | Reactive 响应式 | 声明式路由 + Wasm 插件 |
| 与 Nacos 集成 | 需要 Spring Cloud LoadBalancer | 原生支持 Nacos 服务发现 |
| 与 Seata 集成 | 无直接关联 | 无直接关联 |
| 学习曲线 | 低（Spring 开发者熟悉） | 中（需要了解 K8s/Helm 概念） |
| 社区成熟度 | 非常成熟，文档丰富 | 较新，但发展快 |





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

