package com.opensabre.admin.security.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 刷新Token响应
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "刷新Token响应")
public class RefreshTokenResponse {

    @Schema(description = "新的访问令牌")
    private String accessToken;

    @Schema(description = "用户名")
    private String username;
}
