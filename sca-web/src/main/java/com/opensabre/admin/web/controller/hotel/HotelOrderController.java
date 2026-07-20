package com.opensabre.admin.web.controller.hotel;

import com.opensabre.admin.common.entity.Result;
import com.opensabre.admin.common.util.UserContextHolder;
import com.opensabre.admin.dao.entity.po.*;
import com.opensabre.admin.dao.mapper.*;
import com.opensabre.admin.web.controller.hotel.request.CreateOrderRequest;
import com.opensabre.admin.web.controller.hotel.response.OrderDetailResponse;
import com.opensabre.admin.web.controller.hotel.response.OrderListResponse;
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
 * 酒店订单Controller - 下单、查看订单、取消订单
 */
@Slf4j
@RestController
@RequestMapping("/api/hotel/order")
@Tag(name = "酒店订单", description = "商品下单、订单管理")
public class HotelOrderController {

    @Autowired
    private HotelOrderMapper hotelOrderMapper;

    @Autowired
    private HotelOrderItemMapper hotelOrderItemMapper;

    @Autowired
    private HotelTripMapper hotelTripMapper;

    @Autowired
    private HotelProductMapper hotelProductMapper;

    @Autowired
    private HotelProductSpecMapper hotelProductSpecMapper;

    @Autowired
    private HotelMapper hotelMapper;

    @Autowired
    private SysUserMapper sysUserMapper;

    /**
     * 创建订单（校验入住 + 扣减库存，事务保证）
     */
    @Operation(summary = "创建订单", description = "下单购买商品，需要入住状态，扣减规格库存")
    @PostMapping
    @Transactional(rollbackFor = Exception.class)
    public Result<Object> create(@Valid @RequestBody CreateOrderRequest request) {
        String username = UserContextHolder.getInstance().getUsername();
        SysUser user = sysUserMapper.selectByUsername(username);
        if (user == null) {
            return Result.fail("用户不存在");
        }

        // 校验行程有效性
        HotelTrip trip = hotelTripMapper.selectById(request.getTripId());
        if (trip == null || !trip.getUserId().equals(user.getId()) || trip.getStatus() != 1) {
            return Result.fail("行程无效");
        }

        // 校验当前是否在入住时间段内
        HotelTrip activeTrip = hotelTripMapper.selectActiveTrip(user.getId(), request.getHotelId());
        if (activeTrip == null) {
            return Result.fail("当前不在入住时间段内，无法下单");
        }

        // 创建订单
        HotelOrder order = new HotelOrder();
        order.setUserId(user.getId());
        order.setHotelId(request.getHotelId());
        order.setTripId(request.getTripId());
        order.setStatus(1);
        hotelOrderMapper.insert(order);

        // 创建订单明细 + 扣减库存
        for (CreateOrderRequest.OrderItemRequest item : request.getItems()) {
            // 查询商品和规格
            HotelProduct product = hotelProductMapper.selectById(item.getProductId());
            if (product == null || product.getStatus() != 1) {
                throw new RuntimeException("商品不存在或已下架: " + item.getProductId());
            }

            HotelProductSpec spec = hotelProductSpecMapper.selectById(item.getSpecId());
            if (spec == null || !spec.getProductId().equals(item.getProductId())) {
                throw new RuntimeException("规格不存在: " + item.getSpecId());
            }

            // 扣减库存（乐观锁）
            int affected = hotelProductSpecMapper.deductStock(item.getSpecId(), item.getQuantity());
            if (affected == 0) {
                throw new RuntimeException("库存不足: " + spec.getSpecName());
            }

            // 创建订单明细
            HotelOrderItem orderItem = new HotelOrderItem();
            orderItem.setOrderId(order.getId());
            orderItem.setProductId(item.getProductId());
            orderItem.setSpecId(item.getSpecId());
            orderItem.setProductName(product.getName());
            orderItem.setSpecName(spec.getSpecName());
            orderItem.setQuantity(item.getQuantity());
            orderItem.setPrice(spec.getPrice() != null ? spec.getPrice() : BigDecimal.ZERO);
            hotelOrderItemMapper.insert(orderItem);
        }

        log.info("订单创建成功: orderId={}, userId={}, hotelId={}", order.getId(), user.getId(), request.getHotelId());
        return Result.success(order);
    }

    /**
     * 我的订单列表
     */
    @Operation(summary = "我的订单", description = "查询当前用户的订单列表")
    @GetMapping("/my")
    public Result<Object> myOrders(@RequestParam(required = false) String tripId) {
        String username = UserContextHolder.getInstance().getUsername();
        SysUser user = sysUserMapper.selectByUsername(username);
        if (user == null) {
            return Result.fail("用户不存在");
        }

        List<HotelOrder> orders;
        if (tripId != null && !tripId.isBlank()) {
            orders = hotelOrderMapper.selectByTripId(tripId);
        } else {
            orders = hotelOrderMapper.selectByUserId(user.getId());
        }

        List<OrderListResponse> result = orders.stream().map(order -> {
            List<HotelOrderItem> items = hotelOrderItemMapper.selectByOrderId(order.getId());
            Hotel hotel = hotelMapper.selectById(order.getHotelId());
            return new OrderListResponse(order, items, hotel != null ? hotel.getName() : "");
        }).toList();

        return Result.success(result);
    }

    /**
     * 订单详情
     */
    @Operation(summary = "订单详情", description = "查询订单详情含明细")
    @GetMapping("/{id}")
    public Result<Object> detail(@PathVariable String id) {
        String username = UserContextHolder.getInstance().getUsername();
        SysUser user = sysUserMapper.selectByUsername(username);
        if (user == null) {
            return Result.fail("用户不存在");
        }

        HotelOrder order = hotelOrderMapper.selectById(id);
        if (order == null || !order.getUserId().equals(user.getId())) {
            return Result.fail("订单不存在");
        }

        List<HotelOrderItem> items = hotelOrderItemMapper.selectByOrderId(id);
        Hotel hotel = hotelMapper.selectById(order.getHotelId());
        return Result.success(new OrderDetailResponse(order, items, hotel));
    }

    /**
     * 取消订单（归还库存，事务保证）
     */
    @Operation(summary = "取消订单", description = "取消订单并归还库存")
    @PutMapping("/cancel/{id}")
    @Transactional(rollbackFor = Exception.class)
    public Result<Object> cancel(@PathVariable String id) {
        String username = UserContextHolder.getInstance().getUsername();
        SysUser user = sysUserMapper.selectByUsername(username);
        if (user == null) {
            return Result.fail("用户不存在");
        }

        HotelOrder order = hotelOrderMapper.selectById(id);
        if (order == null || !order.getUserId().equals(user.getId())) {
            return Result.fail("订单不存在");
        }
        if (order.getStatus() == 0) {
            return Result.fail("订单已取消");
        }

        // 归还库存
        List<HotelOrderItem> items = hotelOrderItemMapper.selectByOrderId(id);
        for (HotelOrderItem item : items) {
            hotelProductSpecMapper.restoreStock(item.getSpecId(), item.getQuantity());
        }

        // 更新订单状态
        order.setStatus(0);
        hotelOrderMapper.updateById(order);

        log.info("订单取消成功: orderId={}, userId={}", id, user.getId());
        return Result.success();
    }
}
