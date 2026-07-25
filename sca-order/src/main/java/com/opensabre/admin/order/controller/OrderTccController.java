package com.opensabre.admin.order.controller;

import com.opensabre.admin.common.entity.Result;
import com.opensabre.admin.rpc.client.dto.OrderCreateRequest;
import com.opensabre.admin.rpc.client.tcc.OrderTccService;
import io.seata.rm.tcc.api.BusinessActionContext;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * TCC 订单服务 Controller - TC 回调入口（供 Feign 内部调用 & 测试）
 * <p>
 * Try: 由 TM 通过 Feign 调用，触发 Seata TCC Try
 * Commit/Rollback: 由 Seata TC 自动调度（通过 RM 回调本地 Bean）
 * <p>
 * 此 Controller 的 confirm/cancel 端点仅用于手动测试/调试，
 * 正式流程中由 Seata TC 自动调度 commit/rollback。
 */
@Slf4j
@RestController
@RequestMapping("/inner/order/tcc")
@Tag(name = "TCC订单服务(内部)", description = "TCC模式订单创建、确认、取消")
public class OrderTccController {

    @Autowired
    private OrderTccService orderTccService;

    /**
     * TCC Try: 创建待确认订单（TM 通过 Feign 调用）
     */
    @Operation(summary = "TCC创建待确认订单", description = "Try阶段：创建订单，Seata TC 自动注册分支")
    @PostMapping("/try")
    public Result<String> tryCreate(@Valid @RequestBody OrderCreateRequest request) {
        log.info("[TCC-Controller] tryCreate 委托给 OrderTccService");
        String orderId = orderTccService.tryCreate(request);
        return Result.success(orderId);
    }

    /**
     * TCC Confirm: 手动触发确认（仅用于测试/调试，正式由 TC 自动调度）
     */
    @Operation(summary = "手动触发TCC确认", description = "仅用于测试，正式流程由Seata TC自动调度")
    @PostMapping("/confirm/{xid}")
    public Result<Object> confirm(@PathVariable String xid) {
        log.info("[TCC-Controller] 手动触发 confirm: xid={}", xid);
        BusinessActionContext context = new BusinessActionContext();
        context.setXid(xid);
        boolean success = orderTccService.commit(context);
        return success ? Result.success() : Result.fail("确认失败");
    }

    /**
     * TCC Cancel: 手动触发取消（仅用于测试/调试，正式由 TC 自动调度）
     */
    @Operation(summary = "手动触发TCC取消", description = "仅用于测试，正式流程由Seata TC自动调度")
    @PostMapping("/cancel/{xid}")
    public Result<Object> cancel(@PathVariable String xid) {
        log.info("[TCC-Controller] 手动触发 cancel: xid={}", xid);
        BusinessActionContext context = new BusinessActionContext();
        context.setXid(xid);
        boolean success = orderTccService.rollback(context);
        return success ? Result.success() : Result.fail("取消失败");
    }
}
