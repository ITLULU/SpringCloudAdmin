package com.opensabre.admin.dao.entity.po;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 菜单权限表 PO
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_menu")
public class SysMenu extends BasePo {

    /** 父菜单ID（0=顶级） */
    private String parentId;

    /** 菜单名称 */
    private String menuName;

    /** 类型：M-目录 C-菜单 F-按钮 */
    private String menuType;

    /** 路由路径 */
    private String path;

    /** 组件路径 */
    private String component;

    /** 图标名称 */
    private String icon;

    /** 权限标识（如 system:user:list） */
    private String permission;

    /** 显示排序 */
    private Integer sort;

    /** 状态：0-禁用 1-正常 */
    private Integer status;

    /** 是否可见：0-隐藏 1-显示 */
    private Integer visible;
}
