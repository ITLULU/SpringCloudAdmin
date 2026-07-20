package com.opensabre.admin.security.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 登录响应
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "登录响应")
public class LoginResponse {

    @Schema(description = "用户名")
    private String username;

    @Schema(description = "访问令牌（非refresh-token模式）")
    private String token;

    @Schema(description = "访问令牌（refresh-token模式）")
    private String accessToken;

    @Schema(description = "刷新令牌（refresh-token模式）")
    private String refreshToken;
}
