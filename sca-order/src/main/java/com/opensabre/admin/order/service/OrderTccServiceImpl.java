package com.opensabre.admin.order.service;

import com.opensabre.admin.dao.entity.po.HotelOrder;
import com.opensabre.admin.dao.entity.po.HotelOrderItem;
import com.opensabre.admin.dao.mapper.HotelOrderItemMapper;
import com.opensabre.admin.dao.mapper.HotelOrderMapper;
import com.opensabre.admin.rpc.client.dto.OrderCreateRequest;
import com.opensabre.admin.rpc.client.tcc.OrderTccService;
import io.seata.core.context.RootContext;
import io.seata.rm.tcc.api.BusinessActionContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

/**
 * TCC 订单服务实现 - Seata 两阶段提交
 * <p>
 * Try: 创建待确认订单（status=2）
 * Commit: 确认订单（status 2→1）
 * Rollback: 取消订单（status 2→0）
 * <p>
 * 防悬挂、空回滚、幂等由 Seata TCC 框架自动保障。
 */
@Slf4j
@Service
public class OrderTccServiceImpl implements OrderTccService {

    @Autowired
    private HotelOrderMapper hotelOrderMapper;

    @Autowired
    private HotelOrderItemMapper hotelOrderItemMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public String tryCreate(OrderCreateRequest request) {
        String xid = RootContext.getXID();
        log.info("[TCC-Try] 创建待确认订单: xid={}, userId={}", xid, request.getUserId());

        // 创建订单主表（status=2 待确认）
        HotelOrder order = new HotelOrder();
        order.setUserId(request.getUserId());
        order.setHotelId(request.getHotelId());
        order.setTripId(request.getTripId());
        order.setStatus(2); // 待确认
        order.setXid(xid);
        hotelOrderMapper.insert(order);

        // 创建订单明细
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
        }

        log.info("[TCC-Try] 订单创建成功: orderId={}, xid={}", order.getId(), xid);
        return order.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean commit(BusinessActionContext context) {
        String xid = context.getXid();
        log.info("[TCC-Commit] 确认订单: xid={}", xid);

        int affected = hotelOrderMapper.confirmOrderByXid(xid);
        if (affected == 0) {
            log.warn("[TCC-Commit] 未找到待确认订单（可能空回滚或已确认）: xid={}", xid);
        }

        log.info("[TCC-Commit] 订单确认完成: xid={}, affected={}", xid, affected);
        return true;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean rollback(BusinessActionContext context) {
        String xid = context.getXid();
        log.info("[TCC-Rollback] 取消订单: xid={}", xid);

        int affected = hotelOrderMapper.cancelOrderByXid(xid);
        if (affected == 0) {
            // 空回滚：Try 可能还没执行
            log.warn("[TCC-Rollback] 空回滚，Try未执行（或已取消）: xid={}", xid);
        }

        log.info("[TCC-Rollback] 订单取消完成: xid={}, affected={}", xid, affected);
        return true;
    }
}
