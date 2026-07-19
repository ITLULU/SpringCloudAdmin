package com.opensabre.admin.dao.entity.po;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 角色菜单关联表 PO
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_role_menu")
public class SysRoleMenu extends BasePo {

    /** 角色ID */
    private String roleId;

    /** 菜单ID */
    private String menuId;
}
