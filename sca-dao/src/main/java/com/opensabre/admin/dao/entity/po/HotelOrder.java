package com.opensabre.admin.dao.entity.po;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 订单表 PO
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("hotel_order")
public class HotelOrder extends BasePo {

    /** 用户ID */
    private String userId;

    /** 酒店ID */
    private String hotelId;

    /** 关联行程ID */
    private String tripId;

    /** 状态：0-已取消 1-已完成 */
    private Integer status;
}
