package com.opensabre.admin.dao.entity.po;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * TCC 库存冻结记录表 PO
 */
@Data
@TableName("tcc_stock_record")
public class TccStockRecord implements Serializable {

    @TableId(type = IdType.ASSIGN_ID)
    private String id;

    /** 全局事务ID */
    private String xid;

    /** 规格ID */
    private String specId;

    /** 冻结数量 */
    private Integer quantity;

    /** 状态：0=冻结 1=已确认 2=已取消 */
    private Integer status;

    /** 创建时间 */
    private Date createdTime;
}
