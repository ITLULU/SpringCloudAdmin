package com.opensabre.admin.web.controller.hotel;

import com.opensabre.admin.common.entity.Result;
import com.opensabre.admin.common.util.UserContextHolder;
import com.opensabre.admin.dao.entity.po.*;
import com.opensabre.admin.dao.mapper.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
    public Result<Object> create(@RequestBody Map<String, Object> request) {
        String username = UserContextHolder.getInstance().getUsername();
        SysUser user = sysUserMapper.selectByUsername(username);
        if (user == null) {
            return Result.fail("用户不存在");
        }

        String tripId = (String) request.get("tripId");
        String hotelId = (String) request.get("hotelId");

        if (tripId == null || hotelId == null) {
            return Result.fail("参数不完整");
        }

        // 校验行程有效性
        HotelTrip trip = hotelTripMapper.selectById(tripId);
        if (trip == null || !trip.getUserId().equals(user.getId()) || trip.getStatus() != 1) {
            return Result.fail("行程无效");
        }

        // 校验当前是否在入住时间段内
        HotelTrip activeTrip = hotelTripMapper.selectActiveTrip(user.getId(), hotelId);
        if (activeTrip == null) {
            return Result.fail("当前不在入住时间段内，无法下单");
        }

        // 解析商品明细
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> items = (List<Map<String, Object>>) request.get("items");
        if (items == null || items.isEmpty()) {
            return Result.fail("订单商品不能为空");
        }

        // 创建订单
        HotelOrder order = new HotelOrder();
        order.setUserId(user.getId());
        order.setHotelId(hotelId);
        order.setTripId(tripId);
        order.setStatus(1);
        hotelOrderMapper.insert(order);

        // 创建订单明细 + 扣减库存
        for (Map<String, Object> item : items) {
            String productId = (String) item.get("productId");
            String specId = (String) item.get("specId");
            int quantity = item.get("quantity") instanceof Integer
                    ? (Integer) item.get("quantity")
                    : Integer.parseInt(item.get("quantity").toString());

            // 查询商品和规格
            HotelProduct product = hotelProductMapper.selectById(productId);
            if (product == null || product.getStatus() != 1) {
                throw new RuntimeException("商品不存在或已下架: " + productId);
            }

            HotelProductSpec spec = hotelProductSpecMapper.selectById(specId);
            if (spec == null || !spec.getProductId().equals(productId)) {
                throw new RuntimeException("规格不存在: " + specId);
            }

            // 扣减库存（乐观锁）
            int affected = hotelProductSpecMapper.deductStock(specId, quantity);
            if (affected == 0) {
                throw new RuntimeException("库存不足: " + spec.getSpecName());
            }

            // 创建订单明细
            HotelOrderItem orderItem = new HotelOrderItem();
            orderItem.setOrderId(order.getId());
            orderItem.setProductId(productId);
            orderItem.setSpecId(specId);
            orderItem.setProductName(product.getName());
            orderItem.setSpecName(spec.getSpecName());
            orderItem.setQuantity(quantity);
            orderItem.setPrice(spec.getPrice() != null ? spec.getPrice() : BigDecimal.ZERO);
            hotelOrderItemMapper.insert(orderItem);
        }

        log.info("订单创建成功: orderId={}, userId={}, hotelId={}", order.getId(), user.getId(), hotelId);
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

        // 附加订单明细和酒店信息
        List<Map<String, Object>> result = orders.stream().map(order -> {
            Map<String, Object> map = new HashMap<>();
            map.put("order", order);
            map.put("items", hotelOrderItemMapper.selectByOrderId(order.getId()));
            Hotel hotel = hotelMapper.selectById(order.getHotelId());
            map.put("hotelName", hotel != null ? hotel.getName() : "");
            return map;
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

        Map<String, Object> data = new HashMap<>();
        data.put("order", order);
        data.put("items", hotelOrderItemMapper.selectByOrderId(id));
        Hotel hotel = hotelMapper.selectById(order.getHotelId());
        data.put("hotel", hotel);
        return Result.success(data);
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
