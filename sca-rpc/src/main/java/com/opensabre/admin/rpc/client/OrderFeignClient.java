package com.opensabre.admin.rpc.client;

import com.opensabre.admin.common.entity.Result;
import com.opensabre.admin.rpc.client.dto.OrderCancelRequest;
import com.opensabre.admin.rpc.client.dto.OrderCreateRequest;
import com.opensabre.admin.rpc.client.dto.OrderDetailResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 订单服务 Feign 客户端 - 调用 sca-order 微服务
 */
@FeignClient(name = "sca-order", path = "/inner/order")
public interface OrderFeignClient {

    /**
     * 创建订单
     */
    @PostMapping("/create")
    Result<OrderDetailResponse> createOrder(@RequestBody OrderCreateRequest request);

    /**
     * 取消订单
     */
    @PostMapping("/cancel")
    Result<Object> cancelOrder(@RequestBody OrderCancelRequest request);

    /**
     * 订单详情
     */
    @GetMapping("/detail/{id}")
    Result<OrderDetailResponse> getOrderDetail(@PathVariable("id") String id);

    /**
     * 用户订单列表
     */
    @GetMapping("/list")
    Result<List<OrderDetailResponse>> listOrders(
            @RequestParam("userId") String userId,
            @RequestParam(value = "tripId", required = false) String tripId);
}
