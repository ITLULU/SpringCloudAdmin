package com.opensabre.admin.datasync.service;

import com.opensabre.admin.dao.entity.po.HotelOrder;
import com.opensabre.admin.dao.entity.po.HotelOrderItem;
import com.opensabre.admin.dao.mapper.HotelOrderItemMapper;
import com.opensabre.admin.dao.mapper.HotelOrderMapper;
import com.opensabre.admin.es.dao.OrderDocumentDao;
import com.opensabre.admin.es.document.OrderDocument;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;

/**
 * 订单同步服务
 * <p>
 * 根据订单ID查询数据库最新数据，同步到 Elasticsearch（基于官方 ElasticsearchClient 的 OrderDocumentDao）。
 */
@Slf4j
@Service
public class OrderSyncService {

    @Autowired
    private HotelOrderMapper hotelOrderMapper;

    @Autowired
    private HotelOrderItemMapper hotelOrderItemMapper;

    @Autowired
    private OrderDocumentDao orderDocumentDao;

    /**
     * 同步指定订单到 ES
     */
    public void syncOrder(String orderId) {
        HotelOrder order = hotelOrderMapper.selectById(orderId);
        if (order == null) {
            log.warn("[数据同步] 数据库中不存在订单: orderId={}", orderId);
            // ES 中如果存在则删除，保持数据一致（文档ID即订单ID，不存在时静默返回）
            orderDocumentDao.deleteByOrderId(orderId);
            return;
        }

        List<HotelOrderItem> items = hotelOrderItemMapper.selectByOrderId(orderId);

        OrderDocument document = new OrderDocument();
        document.setId(orderId);
        document.setOrderId(orderId);
        document.setUserId(order.getUserId());
        document.setHotelId(order.getHotelId());
        document.setTripId(order.getTripId());
        document.setStatus(order.getStatus());
        document.setTotalAmount(calculateTotalAmount(items));
        document.setCreatedTime(toLocalDateTime(order.getCreatedTime()));
        document.setUpdatedTime(toLocalDateTime(order.getUpdatedTime()));
        document.setItems(convertItems(items));

        orderDocumentDao.save(document);
        log.info("[数据同步] 订单已同步到 ES: orderId={}, status={}, totalAmount={}",
                orderId, order.getStatus(), document.getTotalAmount());
    }

    private BigDecimal calculateTotalAmount(List<HotelOrderItem> items) {
        if (items == null) {
            return BigDecimal.ZERO;
        }
        return items.stream()
                .map(item -> item.getPrice().multiply(BigDecimal.valueOf(item.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private List<OrderDocument.OrderItemDocument> convertItems(List<HotelOrderItem> items) {
        if (items == null) {
            return null;
        }
        return items.stream().map(item -> {
            OrderDocument.OrderItemDocument doc = new OrderDocument.OrderItemDocument();
            doc.setItemId(item.getId());
            doc.setProductId(item.getProductId());
            doc.setSpecId(item.getSpecId());
            doc.setProductName(item.getProductName());
            doc.setSpecName(item.getSpecName());
            doc.setQuantity(item.getQuantity());
            doc.setPrice(item.getPrice());
            return doc;
        }).toList();
    }

    private LocalDateTime toLocalDateTime(Date date) {
        if (date == null) {
            return null;
        }
        return date.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime();
    }
}
