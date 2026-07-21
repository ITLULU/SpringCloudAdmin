package com.opensabre.admin.web.controller.hotel;

import com.opensabre.admin.common.entity.Result;
import com.opensabre.admin.common.util.SecurityUtils;
import com.opensabre.admin.dao.entity.po.Hotel;
import com.opensabre.admin.dao.entity.po.HotelTrip;
import com.opensabre.admin.dao.entity.po.HotelProduct;
import com.opensabre.admin.dao.entity.po.HotelProductSpec;
import com.opensabre.admin.dao.entity.po.SysUser;
import com.opensabre.admin.dao.mapper.HotelMapper;
import com.opensabre.admin.dao.mapper.HotelTripMapper;
import com.opensabre.admin.dao.mapper.SysUserMapper;
import com.opensabre.admin.rpc.client.OrderFeignClient;
import com.opensabre.admin.rpc.client.ProductFeignClient;
import com.opensabre.admin.rpc.client.StockFeignClient;
import com.opensabre.admin.rpc.client.dto.OrderCancelRequest;
import com.opensabre.admin.rpc.client.dto.OrderCreateRequest;
import com.opensabre.admin.rpc.client.dto.OrderDetailResponse;
import com.opensabre.admin.rpc.client.dto.StockDeductRequest;
import com.opensabre.admin.rpc.client.dto.StockRestoreRequest;
import com.opensabre.admin.web.controller.hotel.request.CreateOrderRequest;
import io.seata.spring.annotation.GlobalTransactional;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 酒店订单Controller - 下单、查看订单、取消订单
 * <p>
 * 下单流程（分布式微服务架构）：
 * 1. 本地校验（用户、行程、入住状态）
 * 2. Feign 调用库存服务扣减库存
 * 3. Feign 调用订单服务创建订单
 * <p>
 * Seata分布式事务：启用后在 create 方法添加 @GlobalTransactional 注解
 */
@Slf4j
@RestController
@RequestMapping("/api/hotel/order")
@Tag(name = "酒店订单", description = "商品下单、订单管理")
public class HotelOrderController {

    @Autowired
    private HotelTripMapper hotelTripMapper;

    @Autowired
    private HotelMapper hotelMapper;

    @Autowired
    private ProductFeignClient productFeignClient;

    @Autowired
    private SysUserMapper sysUserMapper;

    @Autowired
    private OrderFeignClient orderFeignClient;

    @Autowired
    private StockFeignClient stockFeignClient;

    /**
     * 创建订单
     * <p>
     * 流程：本地校验 → Feign调用库存服务扣减库存 → Feign调用订单服务创建订单
     * Seata @GlobalTransactional 保证分布式事务一致性，任一步骤失败自动回滚所有分支事务
     */
    @GlobalTransactional(name = "create-order", rollbackFor = Exception.class)
    @Operation(summary = "创建订单", description = "下单购买商品，需要入住状态，扣减规格库存")
    @PostMapping
    public Result<Object> create(@Valid @RequestBody CreateOrderRequest request) {
        String username = SecurityUtils.getCurrentUsername();
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

        // ========== 构建库存扣减请求（同时缓存商品/规格信息供后续构建订单使用） ==========
        List<StockDeductRequest.DeductItem> deductItems = new ArrayList<>();
        List<HotelProduct> cachedProducts = new ArrayList<>();
        List<HotelProductSpec> cachedSpecs = new ArrayList<>();
        for (CreateOrderRequest.OrderItemRequest item : request.getItems()) {
            // Feign 调用库存服务查询商品和规格
            Result<HotelProduct> productResult = productFeignClient.productInfo(item.getProductId());
            if (productResult.isFail() || productResult.getData() == null) {
                return Result.fail("商品不存在或已下架: " + item.getProductId());
            }
            HotelProduct product = productResult.getData();
            if (product.getStatus() != 1) {
                return Result.fail("商品已下架: " + item.getProductId());
            }

            Result<HotelProductSpec> specResult = productFeignClient.specInfo(item.getSpecId());
            if (specResult.isFail() || specResult.getData() == null) {
                return Result.fail("规格不存在: " + item.getSpecId());
            }
            HotelProductSpec spec = specResult.getData();
            if (!spec.getProductId().equals(item.getProductId())) {
                return Result.fail("规格不属于该商品: " + item.getSpecId());
            }

            cachedProducts.add(product);
            cachedSpecs.add(spec);

            StockDeductRequest.DeductItem deductItem = new StockDeductRequest.DeductItem();
            deductItem.setSpecId(item.getSpecId());
            deductItem.setQuantity(item.getQuantity());
            deductItems.add(deductItem);
        }

        // ========== Feign 调用库存服务：批量扣减库存 ==========
        StockDeductRequest deductRequest = new StockDeductRequest();
        deductRequest.setItems(deductItems);
        Result<Object> deductResult = stockFeignClient.deductStock(deductRequest);
        if (deductResult.isFail()) {
            return Result.fail("库存扣减失败: " + deductResult.getMsg());
        }
        log.info("库存扣减成功: hotelId={}", request.getHotelId());

        // ========== 构建订单创建请求 ==========
        OrderCreateRequest orderRequest = new OrderCreateRequest();
        orderRequest.setUserId(user.getId());
        orderRequest.setHotelId(request.getHotelId());
        orderRequest.setTripId(request.getTripId());

        List<OrderCreateRequest.OrderItemRequest> orderItems = new ArrayList<>();
        for (int i = 0; i < request.getItems().size(); i++) {
            CreateOrderRequest.OrderItemRequest item = request.getItems().get(i);
            HotelProduct product = cachedProducts.get(i);
            HotelProductSpec spec = cachedSpecs.get(i);

            OrderCreateRequest.OrderItemRequest orderItem = new OrderCreateRequest.OrderItemRequest();
            orderItem.setProductId(item.getProductId());
            orderItem.setSpecId(item.getSpecId());
            orderItem.setProductName(product.getName());
            orderItem.setSpecName(spec.getSpecName());
            orderItem.setQuantity(item.getQuantity());
            orderItem.setPrice(spec.getPrice() != null ? spec.getPrice() : BigDecimal.ZERO);
            orderItems.add(orderItem);
        }
        orderRequest.setItems(orderItems);

        // ========== Feign 调用订单服务：创建订单 ==========
        Result<OrderDetailResponse> orderResult = orderFeignClient.createOrder(orderRequest);
        if (orderResult.isFail()) {
            // 订单创建失败，需要归还库存（Seata会自动回滚，但防御性编程保留手动归还）
            StockRestoreRequest restoreRequest = new StockRestoreRequest();
            List<StockRestoreRequest.RestoreItem> restoreItems = deductItems.stream().map(d -> {
                StockRestoreRequest.RestoreItem ri = new StockRestoreRequest.RestoreItem();
                ri.setSpecId(d.getSpecId());
                ri.setQuantity(d.getQuantity());
                return ri;
            }).toList();
            restoreRequest.setItems(restoreItems);
            stockFeignClient.restoreStock(restoreRequest);
            log.warn("订单创建失败，已归还库存: {}", orderResult.getMsg());
            return Result.fail("订单创建失败: " + orderResult.getMsg());
        }

        log.info("订单创建成功: orderId={}, userId={}", orderResult.getData().getId(), user.getId());
        return Result.success(orderResult.getData());
    }

    /**
     * 我的订单列表（通过 Feign 调用订单服务）
     */
    @Operation(summary = "我的订单", description = "查询当前用户的订单列表，包含酒店名称")
    @GetMapping("/my")
    public Result<Object> myOrders(@RequestParam(required = false) String tripId) {
        String username = SecurityUtils.getCurrentUsername();
        SysUser user = sysUserMapper.selectByUsername(username);
        if (user == null) {
            return Result.fail("用户不存在");
        }

        Result<List<OrderDetailResponse>> result = orderFeignClient.listOrders(user.getId(), tripId);
        if (result.isFail() || result.getData() == null) {
            log.warn("[我的订单] Feign调用失败或无数据: userId={}, result={}", user.getId(), result.getMsg());
            return Result.success(new ArrayList<>());
        }

        List<OrderDetailResponse> orders = result.getData();
        log.info("[我的订单] 查询到 {} 条订单, userId={}", orders.size(), user.getId());

        // 丰富订单数据：补充酒店名称
        List<Map<String, Object>> enriched = new ArrayList<>();
        for (OrderDetailResponse order : orders) {
            Map<String, Object> item = new HashMap<>();
            item.put("order", order);
            item.put("items", order.getItems() != null ? order.getItems() : new ArrayList<>());
            // 查询酒店名称
            Hotel hotel = hotelMapper.selectById(order.getHotelId());
            item.put("hotelName", hotel != null ? hotel.getName() : "未知酒店");
            item.put("totalAmount", order.getTotalAmount() != null ? order.getTotalAmount() : BigDecimal.ZERO);
            enriched.add(item);
        }
        return Result.success(enriched);
    }

    /**
     * 订单详情（通过 Feign 调用订单服务）
     */
    @Operation(summary = "订单详情", description = "查询订单详情含明细和酒店信息")
    @GetMapping("/{id}")
    public Result<Object> detail(@PathVariable String id) {
        String username = SecurityUtils.getCurrentUsername();
        SysUser user = sysUserMapper.selectByUsername(username);
        if (user == null) {
            return Result.fail("用户不存在");
        }

        Result<OrderDetailResponse> result = orderFeignClient.getOrderDetail(id);
        if (result.isFail() || result.getData() == null) {
            return Result.fail("订单不存在");
        }

        OrderDetailResponse order = result.getData();
        // 丰富数据：补充酒店信息
        Hotel hotel = hotelMapper.selectById(order.getHotelId());
        Map<String, Object> detail = new HashMap<>();
        detail.put("order", order);
        detail.put("items", order.getItems() != null ? order.getItems() : new ArrayList<>());
        detail.put("hotelName", hotel != null ? hotel.getName() : "未知酒店");
        detail.put("totalAmount", order.getTotalAmount() != null ? order.getTotalAmount() : BigDecimal.ZERO);
        return Result.success(detail);
    }

    /**
     * 取消订单
     * <p>
     * 流程：Feign调用订单服务取消 → Feign调用库存服务归还库存
     * Seata启用后添加 @GlobalTransactional 保证分布式事务一致性
     */
    // @io.seata.spring.annotation.GlobalTransactional(name = "cancel-order", rollbackFor = Exception.class)
    @Operation(summary = "取消订单", description = "取消订单并归还库存")
    @PutMapping("/cancel/{id}")
    public Result<Object> cancel(@PathVariable String id) {
        String username = SecurityUtils.getCurrentUsername();
        SysUser user = sysUserMapper.selectByUsername(username);
        if (user == null) {
            return Result.fail("用户不存在");
        }

        // 先查询订单详情（获取明细用于归还库存）
        Result<OrderDetailResponse> detailResult = orderFeignClient.getOrderDetail(id);
        if (detailResult.isFail()) {
            return Result.fail("订单不存在");
        }

        OrderDetailResponse order = detailResult.getData();
        if (!order.getUserId().equals(user.getId())) {
            return Result.fail("无权操作此订单");
        }
        if (order.getStatus() == 0) {
            return Result.fail("订单已取消");
        }

        // ========== Feign 调用订单服务：取消订单 ==========
        OrderCancelRequest cancelRequest = new OrderCancelRequest();
        cancelRequest.setOrderId(id);
        Result<Object> cancelResult = orderFeignClient.cancelOrder(cancelRequest);
        if (cancelResult.isFail()) {
            return Result.fail(cancelResult.getMsg());
        }

        // ========== Feign 调用库存服务：归还库存 ==========
        if (order.getItems() != null && !order.getItems().isEmpty()) {
            StockRestoreRequest restoreRequest = new StockRestoreRequest();
            List<StockRestoreRequest.RestoreItem> restoreItems = order.getItems().stream().map(item -> {
                StockRestoreRequest.RestoreItem ri = new StockRestoreRequest.RestoreItem();
                ri.setSpecId(item.getSpecId());
                ri.setQuantity(item.getQuantity());
                return ri;
            }).toList();
            restoreRequest.setItems(restoreItems);
            stockFeignClient.restoreStock(restoreRequest);
        }

        log.info("订单取消成功: orderId={}, userId={}", id, user.getId());
        return Result.success();
    }
}
