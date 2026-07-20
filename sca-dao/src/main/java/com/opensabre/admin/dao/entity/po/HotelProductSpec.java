package com.opensabre.admin.dao.entity.po;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

/**
 * 商品规格表 PO
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("hotel_product_spec")
public class HotelProductSpec extends BasePo {

    /** 所属商品ID */
    private String productId;

    /** 规格名称（如：大份/小份） */
    private String specName;

    /** 规格值描述 */
    private String specValue;

    /** 库存数量 */
    private Integer stock;

    /** 规格价格（当前均为0） */
    private BigDecimal price;

    /** 排序 */
    private Integer sort;
}
