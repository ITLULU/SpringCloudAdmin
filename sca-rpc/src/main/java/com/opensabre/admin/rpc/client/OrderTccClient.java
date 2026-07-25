package com.opensabre.admin.rpc.client;

import com.opensabre.admin.common.entity.Result;
import com.opensabre.admin.rpc.client.dto.OrderCreateRequest;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

/**
 * TCC 订单服务 Feign 客户端 - TM 只调用 Try，Confirm/Cancel 由 Seata TC 自动调度
 */
@FeignClient(name = "sca-order", path = "/inner/order/tcc")
public interface OrderTccClient {

    /**
     * TCC Try: 创建待确认订单（status=2）
     * XID 由 Seata 自动注入 Feign HTTP Header，无需手动传递
     */
    @PostMapping("/try")
    Result<String> tryCreate(@RequestBody OrderCreateRequest request);
}
