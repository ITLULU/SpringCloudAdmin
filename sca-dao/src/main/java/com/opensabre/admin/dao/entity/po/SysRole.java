package com.opensabre.admin.dao.entity.po;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 角色表 PO
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_role")
public class SysRole extends BasePo {

    /** 角色名称 */
    private String roleName;

    /** 角色标识（如 admin） */
    private String roleKey;

    /** 显示排序 */
    private Integer sort;

    /** 状态：0-禁用 1-正常 */
    private Integer status;

    /** 备注 */
    private String remark;
}
