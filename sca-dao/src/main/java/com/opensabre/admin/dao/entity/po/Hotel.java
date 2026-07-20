package com.opensabre.admin.dao.entity.po;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 酒店表 PO
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("hotel")
public class Hotel extends BasePo {

    /** 酒店品牌 */
    private String brand;

    /** 酒店名称 */
    private String name;

    /** 酒店地址 */
    private String address;

    /** 联系电话 */
    private String phone;

    /** 酒店logo URL */
    private String logo;

    /** 酒店简介 */
    private String description;

    /** 状态：0-停业 1-营业 */
    private Integer status;
}
