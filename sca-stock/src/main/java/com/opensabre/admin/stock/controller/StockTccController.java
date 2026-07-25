package com.opensabre.admin.stock.controller;

import com.opensabre.admin.common.entity.Result;
import com.opensabre.admin.rpc.client.dto.StockFreezeRequest;
import com.opensabre.admin.rpc.client.tcc.StockTccService;
import io.seata.rm.tcc.api.BusinessActionContext;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * TCC 库存服务 Controller - TC 回调入口（供 Feign 内部调用 & 测试）
 * <p>
 * Try: 由 TM 通过 Feign 调用，触发 Seata TCC Try
 * Commit/Rollback: 由 Seata TC 自动调度（通过 RM 回调本地 Bean）
 * <p>
 * 此 Controller 的 confirm/cancel 端点仅用于手动测试/调试，
 * 正式流程中由 Seata TC 自动调度 commit/rollback。
 */
@Slf4j
@RestController
@RequestMapping("/inner/stock/tcc")
@Tag(name = "TCC库存服务(内部)", description = "TCC模式库存冻结、确认、取消")
public class StockTccController {

    @Autowired
    private StockTccService stockTccService;

    /**
     * TCC Try: 冻结库存（TM 通过 Feign 调用）
     */
    @Operation(summary = "TCC冻结库存", description = "Try阶段：冻结库存，Seata TC 自动注册分支")
    @PostMapping("/try")
    public Result<Object> tryFreeze(@Valid @RequestBody StockFreezeRequest request) {
        log.info("[TCC-Controller] tryFreeze 委托给 StockTccService");
        boolean success = stockTccService.tryFreeze(request);
        return success ? Result.success() : Result.fail("冻结失败");
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
        boolean success = stockTccService.commit(context);
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
        boolean success = stockTccService.rollback(context);
        return success ? Result.success() : Result.fail("取消失败");
    }
}
