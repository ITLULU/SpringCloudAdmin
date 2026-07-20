package com.opensabre.admin.dao.entity.po;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

/**
 * 订单明细表 PO
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("hotel_order_item")
public class HotelOrderItem extends BasePo {

    /** 订单ID */
    private String orderId;

    /** 商品ID */
    private String productId;

    /** 规格ID */
    private String specId;

    /** 商品名（冗余） */
    private String productName;

    /** 规格名（冗余） */
    private String specName;

    /** 数量 */
    private Integer quantity;

    /** 下单时单价 */
    private BigDecimal price;
}
