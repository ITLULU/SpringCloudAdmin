package com.opensabre.admin.web.controller.system.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 修改用户请求
 */
@Data
@Schema(description = "修改用户请求")
public class EditUserRequest {

    @NotBlank(message = "用户ID不能为空")
    @Schema(description = "用户ID", required = true)
    private String id;

    @Schema(description = "昵称")
    private String nickname;

    @Schema(description = "邮箱")
    private String email;

    @Schema(description = "手机号")
    private String phone;

    @Schema(description = "头像URL")
    private String avatar;

    @Schema(description = "状态：0-禁用 1-正常")
    private Integer status;
}
