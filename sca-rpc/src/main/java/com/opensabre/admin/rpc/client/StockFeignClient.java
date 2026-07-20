package com.opensabre.admin.rpc.client;

import com.opensabre.admin.common.entity.Result;
import com.opensabre.admin.rpc.client.dto.StockDeductRequest;
import com.opensabre.admin.rpc.client.dto.StockRestoreRequest;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

/**
 * 库存服务 Feign 客户端 - 调用 sca-stock 微服务
 */
@FeignClient(name = "sca-stock", path = "/inner/stock")
public interface StockFeignClient {

    /**
     * 批量扣减库存
     */
    @PostMapping("/deduct")
    Result<Object> deductStock(@RequestBody StockDeductRequest request);

    /**
     * 批量归还库存
     */
    @PostMapping("/restore")
    Result<Object> restoreStock(@RequestBody StockRestoreRequest request);
}
