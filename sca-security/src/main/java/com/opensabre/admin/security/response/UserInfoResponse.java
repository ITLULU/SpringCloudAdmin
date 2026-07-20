package com.opensabre.admin.security.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 当前用户信息响应
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "当前用户信息响应")
public class UserInfoResponse {

    @Schema(description = "用户名")
    private String username;

    @Schema(description = "角色列表")
    private List<String> roles;

    @Schema(description = "权限标识列表")
    private List<String> permissions;
}
