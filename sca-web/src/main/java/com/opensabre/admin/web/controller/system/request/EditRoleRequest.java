package com.opensabre.admin.web.controller.system.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 修改角色请求
 */
@Data
@Schema(description = "修改角色请求")
public class EditRoleRequest {

    @NotBlank(message = "角色ID不能为空")
    @Schema(description = "角色ID", required = true)
    private String id;

    @Schema(description = "角色名称")
    private String roleName;

    @Schema(description = "角色标识")
    private String roleKey;

    @Schema(description = "显示排序")
    private Integer sort;

    @Schema(description = "状态：0-禁用 1-正常")
    private Integer status;

    @Schema(description = "备注")
    private String remark;
}
