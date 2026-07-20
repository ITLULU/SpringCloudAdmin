package com.opensabre.admin.order.controller;

import com.opensabre.admin.common.entity.Result;
import com.opensabre.admin.dao.entity.po.HotelOrder;
import com.opensabre.admin.dao.entity.po.HotelOrderItem;
import com.opensabre.admin.dao.mapper.HotelOrderItemMapper;
import com.opensabre.admin.dao.mapper.HotelOrderMapper;
import com.opensabre.admin.order.dto.OrderCancelRequest;
import com.opensabre.admin.order.dto.OrderCreateRequest;
import com.opensabre.admin.order.dto.OrderDetailResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

/**
 * 订单服务 Controller - 提供订单的创建、查询、取消接口（供 Feign 内部调用）
 */
@Slf4j
@RestController
@RequestMapping("/inner/order")
@Tag(name = "订单服务(内部)", description = "订单创建、查询、取消，供微服务间 Feign 调用")
public class OrderController {

    @Autowired
    private HotelOrderMapper hotelOrderMapper;

    @Autowired
    private HotelOrderItemMapper hotelOrderItemMapper;

    /**
     * 创建订单
     */
    @Operation(summary = "创建订单", description = "创建订单及订单明细")
    @PostMapping("/create")
    @Transactional(rollbackFor = Exception.class)
    public Result<OrderDetailResponse> create(@Valid @RequestBody OrderCreateRequest request) {
        // 创建订单主表
        HotelOrder order = new HotelOrder();
        order.setUserId(request.getUserId());
        order.setHotelId(request.getHotelId());
        order.setTripId(request.getTripId());
        order.setStatus(1);
        hotelOrderMapper.insert(order);

        // 创建订单明细
        BigDecimal totalAmount = BigDecimal.ZERO;
        List<OrderDetailResponse.ItemDetail> itemDetails = new java.util.ArrayList<>();

        for (OrderCreateRequest.OrderItemRequest item : request.getItems()) {
            HotelOrderItem orderItem = new HotelOrderItem();
            orderItem.setOrderId(order.getId());
            orderItem.setProductId(item.getProductId());
            orderItem.setSpecId(item.getSpecId());
            orderItem.setProductName(item.getProductName());
            orderItem.setSpecName(item.getSpecName());
            orderItem.setQuantity(item.getQuantity());
            orderItem.setPrice(item.getPrice() != null ? item.getPrice() : BigDecimal.ZERO);
            hotelOrderItemMapper.insert(orderItem);

            // 累加金额
            BigDecimal lineTotal = orderItem.getPrice().multiply(BigDecimal.valueOf(item.getQuantity()));
            totalAmount = totalAmount.add(lineTotal);

            itemDetails.add(new OrderDetailResponse.ItemDetail(
                    item.getProductId(), item.getSpecId(),
                    item.getProductName(), item.getSpecName(),
                    item.getQuantity(), item.getPrice()
            ));
        }

        log.info("[订单服务] 订单创建成功: orderId={}, userId={}", order.getId(), request.getUserId());

        // 构建响应
        OrderDetailResponse response = new OrderDetailResponse();
        response.setId(order.getId());
        response.setUserId(order.getUserId());
        response.setHotelId(order.getHotelId());
        response.setTripId(order.getTripId());
        response.setStatus(order.getStatus());
        response.setCreatedTime(order.getCreatedTime());
        response.setItems(itemDetails);
        response.setTotalAmount(totalAmount);

        return Result.success(response);
    }

    /**
     * 取消订单（仅更新状态，库存归还在库存服务处理）
     */
    @Operation(summary = "取消订单", description = "更新订单状态为已取消")
    @PostMapping("/cancel")
    @Transactional(rollbackFor = Exception.class)
    public Result<Object> cancel(@Valid @RequestBody OrderCancelRequest request) {
        HotelOrder order = hotelOrderMapper.selectById(request.getOrderId());
        if (order == null) {
            return Result.fail("订单不存在");
        }
        if (order.getStatus() == 0) {
            return Result.fail("订单已取消");
        }

        order.setStatus(0);
        hotelOrderMapper.updateById(order);

        log.info("[订单服务] 订单取消成功: orderId={}", request.getOrderId());
        return Result.success();
    }

    /**
     * 查询订单详情
     */
    @Operation(summary = "订单详情", description = "查询订单及其明细")
    @GetMapping("/detail/{id}")
    public Result<OrderDetailResponse> detail(@PathVariable String id) {
        HotelOrder order = hotelOrderMapper.selectById(id);
        if (order == null) {
            return Result.fail("订单不存在");
        }

        List<HotelOrderItem> items = hotelOrderItemMapper.selectByOrderId(id);
        List<OrderDetailResponse.ItemDetail> itemDetails = items.stream()
                .map(item -> new OrderDetailResponse.ItemDetail(
                        item.getProductId(), item.getSpecId(),
                        item.getProductName(), item.getSpecName(),
                        item.getQuantity(), item.getPrice()
                ))
                .toList();

        BigDecimal totalAmount = items.stream()
                .map(item -> item.getPrice().multiply(BigDecimal.valueOf(item.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        OrderDetailResponse response = new OrderDetailResponse();
        response.setId(order.getId());
        response.setUserId(order.getUserId());
        response.setHotelId(order.getHotelId());
        response.setTripId(order.getTripId());
        response.setStatus(order.getStatus());
        response.setCreatedTime(order.getCreatedTime());
        response.setItems(itemDetails);
        response.setTotalAmount(totalAmount);

        return Result.success(response);
    }

    /**
     * 查询用户的订单列表
     */
    @Operation(summary = "用户订单列表", description = "根据用户ID或行程ID查询订单")
    @GetMapping("/list")
    public Result<List<OrderDetailResponse>> list(
            @RequestParam String userId,
            @RequestParam(required = false) String tripId) {

        List<HotelOrder> orders;
        if (tripId != null && !tripId.isBlank()) {
            orders = hotelOrderMapper.selectByTripId(tripId);
        } else {
            orders = hotelOrderMapper.selectByUserId(userId);
        }

        List<OrderDetailResponse> result = orders.stream().map(order -> {
            List<HotelOrderItem> items = hotelOrderItemMapper.selectByOrderId(order.getId());
            List<OrderDetailResponse.ItemDetail> itemDetails = items.stream()
                    .map(item -> new OrderDetailResponse.ItemDetail(
                            item.getProductId(), item.getSpecId(),
                            item.getProductName(), item.getSpecName(),
                            item.getQuantity(), item.getPrice()
                    ))
                    .toList();

            BigDecimal totalAmount = items.stream()
                    .map(item -> item.getPrice().multiply(BigDecimal.valueOf(item.getQuantity())))
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            OrderDetailResponse resp = new OrderDetailResponse();
            resp.setId(order.getId());
            resp.setUserId(order.getUserId());
            resp.setHotelId(order.getHotelId());
            resp.setTripId(order.getTripId());
            resp.setStatus(order.getStatus());
            resp.setCreatedTime(order.getCreatedTime());
            resp.setItems(itemDetails);
            resp.setTotalAmount(totalAmount);
            return resp;
        }).toList();

        return Result.success(result);
    }
}
