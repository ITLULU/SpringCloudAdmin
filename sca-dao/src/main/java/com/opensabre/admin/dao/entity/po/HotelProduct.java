package com.opensabre.admin.dao.entity.po;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

/**
 * 酒店商品表 PO
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("hotel_product")
public class HotelProduct extends BasePo {

    /** 所属酒店ID */
    private String hotelId;

    /** 商品名称 */
    private String name;

    /** 商品描述 */
    private String description;

    /** 价格（当前均为0） */
    private BigDecimal price;

    /** 封面图URL */
    private String coverImage;

    /** 商品图片（逗号分隔URL） */
    private String images;

    /** 排序 */
    private Integer sort;

    /** 状态：0-下架 1-上架 */
    private Integer status;
}
