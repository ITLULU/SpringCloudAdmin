package com.opensabre.admin.web.controller.system.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 修改菜单请求
 */
@Data
@Schema(description = "修改菜单请求")
public class EditMenuRequest {

    @NotBlank(message = "菜单ID不能为空")
    @Schema(description = "菜单ID", required = true)
    private String id;

    @Schema(description = "父菜单ID（0=顶级）")
    private String parentId;

    @Schema(description = "菜单名称")
    private String menuName;

    @Schema(description = "类型：M-目录 C-菜单 F-按钮")
    private String menuType;

    @Schema(description = "路由路径")
    private String path;

    @Schema(description = "组件路径")
    private String component;

    @Schema(description = "图标名称")
    private String icon;

    @Schema(description = "权限标识")
    private String permission;

    @Schema(description = "显示排序")
    private Integer sort;

    @Schema(description = "状态：0-禁用 1-正常")
    private Integer status;

    @Schema(description = "是否可见：0-隐藏 1-显示")
    private Integer visible;
}
