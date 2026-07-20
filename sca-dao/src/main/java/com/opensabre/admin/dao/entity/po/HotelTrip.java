package com.opensabre.admin.dao.entity.po;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDate;

/**
 * 用户行程/入住表 PO
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("hotel_trip")
public class HotelTrip extends BasePo {

    /** 用户ID */
    private String userId;

    /** 酒店ID */
    private String hotelId;

    /** 入住日期 */
    private LocalDate checkInDate;

    /** 离店日期 */
    private LocalDate checkOutDate;

    /** 状态：0-已取消 1-已入住 */
    private Integer status;
}
