# Seata TCC 真正两阶段提交改造

## 现状问题分析

当前实现本质上是 **手动编排的最终一致性**，而非 Seata TCC：

| 对比项 | 当前实现 | 真正的 Seata TCC |
|--------|---------|-----------------|
| XID | 自生成 UUID | Seata TC 分配的全局事务 ID |
| 协调者 | HotelOrderController 手动 try-catch | Seata TC 自动调度 |
| 注解 | 无 @LocalTCC / @TwoPhaseBusinessAction | 必须有 |
| TM | 无 @GlobalTransactional（TCC方法） | @GlobalTransactional 发起全局事务 |
| 可靠性 | 进程崩溃 = 中间态残留 | TC 持久化，宕机自动恢复 |
| 防悬挂/空回滚 | 业务代码自己写 | Seata 框架层保障 |

`问题记录.md` 中 "@TwoPhaseBusinessAction 只给同JVM用的" 这个结论是错误的。`@LocalTCC` 明确适用于 **SpringCloud+Feign 模式**。

## 目标架构

```
sca-web (TM)                          Seata TC
  |                                      |
  |-- @GlobalTransactional 发起全局事务 --→|
  |                                      |
  |-- Feign: stock.tryFreeze() --------→ sca-stock (Participant 1)
  |        (XID via HTTP Header)         |-- @LocalTCC StockTccService
  |                                      |-- 注册分支到 TC ←--------|
  |                                      |
  |-- Feign: order.tryCreate() --------→ sca-order (Participant 2)
  |        (XID via HTTP Header)         |-- @LocalTCC OrderTccService
  |                                      |-- 注册分支到 TC ←--------|
  |                                      |
  |-- 返回成功 --→ TC 自动调度 Confirm ---→ 各 Participant 的 confirm()
  |-- 返回异常 --→ TC 自动调度 Cancel  ---→ 各 Participant 的 cancel()
```

核心变化：**TM 只调用 Try，Confirm/Cancel 由 Seata TC 自动调度**。

## 改造步骤

### 1. sca-rpc: 新增 TCC 服务接口（含 Seata 注解）

在 `sca-rpc` 模块新增 TCC 接口，添加 `@LocalTCC` 和 `@TwoPhaseBusinessAction`：

**新增文件: `sca-rpc/src/main/java/com/opensabre/admin/rpc/client/tcc/StockTccService.java`**
```java
@LocalTCC
public interface StockTccService {
    @TwoPhaseBusinessAction(name = "stockTccAction", commitMethod = "commit", rollbackMethod = "rollback")
    boolean tryFreeze(@BusinessActionContextParameter(paramName = "request") StockFreezeRequest request);
    boolean commit(BusinessActionContext context);
    boolean rollback(BusinessActionContext context);
}
```

**新增文件: `sca-rpc/src/main/java/com/opensabre/admin/rpc/client/tcc/OrderTccService.java`**
```java
@LocalTCC
public interface OrderTccService {
    @TwoPhaseBusinessAction(name = "orderTccAction", commitMethod = "commit", rollbackMethod = "rollback")
    String tryCreate(@BusinessActionContextParameter(paramName = "request") OrderCreateRequest request,
                     @BusinessActionContextParameter(paramName = "xid") String xid);
    boolean commit(BusinessActionContext context);
    boolean rollback(BusinessActionContext context);
}
```

需要确认 `sca-rpc` 的 pom.xml 是否已引入 seata 依赖（用于 `@LocalTCC` 等注解）。

### 2. sca-stock: 实现 TCC 服务 Bean

**新增文件: `sca-stock/src/main/java/com/opensabre/admin/stock/service/StockTccServiceImpl.java`**

- 实现 `StockTccService` 接口
- `tryFreeze()`: 将现有 `StockTccController.tryFreeze()` 的逻辑迁移到此处，XID 从 `RootContext.getXID()` 获取（Seata 自动传播），不再从 request 传入
- `commit()`: 从 `BusinessActionContext` 获取参数，执行确认扣减
- `rollback()`: 从 `BusinessActionContext` 获取参数，执行释放冻结
- Seata 框架自动处理：防悬挂、空回滚、幂等

**修改文件: `sca-stock/src/main/java/com/opensabre/admin/stock/controller/StockTccController.java`**

- 保留 REST 端点作为 **TC 回调入口**（TC 通过 HTTP 回调 confirm/cancel）
- Controller 方法改为委托给 `StockTccService` bean
- 移除手动防悬挂/空回滚逻辑（由 Seata 框架保障）

### 3. sca-order: 实现 TCC 服务 Bean

**新增文件: `sca-order/src/main/java/com/opensabre/admin/order/service/OrderTccServiceImpl.java`**

- 实现 `OrderTccService` 接口
- `tryCreate()`: 创建待确认订单，XID 从 `RootContext.getXID()` 获取
- `commit()`: 确认订单 (status 2→1)
- `rollback()`: 取消订单 (status 2→0)

**修改文件: `sca-order/src/main/java/com/opensabre/admin/order/controller/OrderTccController.java`**

- 保留 REST 端点作为 TC 回调入口
- 委托给 `OrderTccService` bean

### 4. sca-web: TM 改造（核心变化）

**修改文件: `sca-web/src/main/java/com/opensabre/admin/web/controller/hotel/HotelOrderController.java`**

`createByTcc()` 方法改造为：
```java
@GlobalTransactional(name = "tcc-create-order", rollbackFor = Exception.class)
public Result<Object> createByTcc(@Valid @RequestBody CreateOrderRequest request) {
    // ... 前置校验不变 ...
    
    // 1. Try: 冻结库存（Feign 调用，XID 由 Seata 自动注入 HTTP Header）
    stockTccClient.tryFreeze(freezeRequest);  // 无需手动传 xid
    
    // 2. Try: 创建待确认订单
    orderTccClient.tryCreate(orderRequest);   // 无需手动传 xid
    
    // 3. 没有 Confirm/Cancel 手动调用！
    // Seata TC 会自动根据方法是否异常来调度 Confirm 或 Cancel
    
    return Result.success(orderId);
}
```

关键变化：
- 添加 `@GlobalTransactional`
- 移除手动 UUID 生成
- 移除手动 Confirm/Cancel 调用
- 移除整个 catch 块中的补偿逻辑
- 移除 `compensateConfirm` 调用

### 5. Feign Client 调整

**修改文件: `sca-rpc/src/main/java/com/opensabre/admin/rpc/client/StockTccClient.java`**
- `tryFreeze()`: 移除 xid 参数（Seata 自动通过 HTTP Header 传播）
- 移除 `confirm()`、`cancel()`、`compensateConfirm()` 方法（TM 不再手动调用）

**修改文件: `sca-rpc/src/main/java/com/opensabre/admin/rpc/client/OrderTccClient.java`**
- `tryCreate()`: 移除 xid 参数
- 移除 `confirm()`、`cancel()` 方法

### 6. TccAtExcludeFilter 处理

**修改文件: `sca-common/src/main/java/com/opensabre/admin/common/filter/TccAtExcludeFilter.java`**

此 Filter 当前用于在 TCC 路径请求前 unbind XID 以排除 AT 代理。改造后：
- 对于 **Try 阶段**（Feign 调用到达下游）：需要保留 XID，让 TCC 分支能注册到 TC
- 对于 **TC 回调**（confirm/cancel）：由 Seata 框架自行管理 XID

需要重新评估此 Filter 的逻辑，可能需要移除或调整为仅在特定场景下 unbind。

### 7. DTO 调整

**修改文件: `sca-rpc/src/main/java/com/opensabre/admin/rpc/client/dto/StockFreezeRequest.java`**
- 移除 `xid` 字段（XID 由 Seata 自动传播，业务层无需感知）

### 8. 问题记录更新

修正 `问题记录.md` 中关于 "@TwoPhaseBusinessAction 只适合同 JVM" 的错误结论。

## 风险与注意事项

1. **Seata TC 必须正常运行**：改造后依赖 Seata TC 做协调，TC 不可用则全局事务不可用
2. **TC 回调地址**：确保 TC 能通过 HTTP 回调到 sca-stock/sca-order 的 confirm/cancel 端点
3. **超时配置**：需要在 Seata TC 配置中设置合理的 TCC 超时时间
4. **向前兼容**：原有 AT 模式接口（create/cancel/deduct/restore）不受影响，保持不动
5. **幂等性**：虽然 Seata 框架提供防悬挂/空回滚保障，但 confirm/rollback 方法本身仍需保持幂等

## 验证方案

1. 正常下单：Try 全部成功 → TC 自动调度 Confirm → 订单状态=1，库存扣减
2. 库存不足：Try 失败 → TC 自动调度 Cancel → 无残留数据
3. 订单创建失败：Try 部分成功 → TC 自动调度 Cancel → 库存释放
4. 进程崩溃恢复：Try 后 TM 崩溃 → TC 超时后自动调度 Cancel
