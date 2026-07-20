package com.opensabre.admin.stock.controller;

import com.opensabre.admin.common.entity.Result;
import com.opensabre.admin.dao.mapper.HotelProductSpecMapper;
import com.opensabre.admin.stock.dto.StockDeductRequest;
import com.opensabre.admin.stock.dto.StockRestoreRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

/**
 * 库存服务 Controller - 提供库存扣减、归还接口（供 Feign 内部调用）
 */
@Slf4j
@RestController
@RequestMapping("/inner/stock")
@Tag(name = "库存服务(内部)", description = "库存扣减、归还，供微服务间 Feign 调用")
public class StockController {

    @Autowired
    private HotelProductSpecMapper hotelProductSpecMapper;

    /**
     * 批量扣减库存（乐观锁）
     */
    @Operation(summary = "批量扣减库存", description = "根据规格ID和数量扣减库存，任一失败则全部回滚")
    @PostMapping("/deduct")
    @Transactional(rollbackFor = Exception.class)
    public Result<Object> deduct(@Valid @RequestBody StockDeductRequest request) {
        for (StockDeductRequest.DeductItem item : request.getItems()) {
            int affected = hotelProductSpecMapper.deductStock(item.getSpecId(), item.getQuantity());
            if (affected == 0) {
                throw new RuntimeException("库存不足: specId=" + item.getSpecId() + ", quantity=" + item.getQuantity());
            }
            log.info("[库存服务] 扣减库存成功: specId={}, quantity={}", item.getSpecId(), item.getQuantity());
        }
        return Result.success();
    }

    /**
     * 批量归还库存
     */
    @Operation(summary = "批量归还库存", description = "取消订单时归还库存")
    @PostMapping("/restore")
    @Transactional(rollbackFor = Exception.class)
    public Result<Object> restore(@Valid @RequestBody StockRestoreRequest request) {
        for (StockRestoreRequest.RestoreItem item : request.getItems()) {
            int affected = hotelProductSpecMapper.restoreStock(item.getSpecId(), item.getQuantity());
            if (affected == 0) {
                log.warn("[库存服务] 归还库存失败（规格可能不存在）: specId={}", item.getSpecId());
            }
            log.info("[库存服务] 归还库存成功: specId={}, quantity={}", item.getSpecId(), item.getQuantity());
        }
        return Result.success();
    }
}
