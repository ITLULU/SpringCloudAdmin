package com.opensabre.admin.dao.entity.po;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 用户角色关联表 PO
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_user_role")
public class SysUserRole extends BasePo {

    /** 用户ID */
    private String userId;

    /** 角色ID */
    private String roleId;
}
