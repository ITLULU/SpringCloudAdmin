package com.opensabre.admin.security.controller;

import com.opensabre.admin.common.entity.Result;
import com.opensabre.admin.common.exception.SystemErrorType;
import com.opensabre.admin.dao.entity.po.SysUser;
import com.opensabre.admin.dao.entity.po.SysUserRole;
import com.opensabre.admin.dao.mapper.SysUserMapper;
import com.opensabre.admin.dao.mapper.SysUserRoleMapper;
import com.opensabre.admin.security.config.SecurityProperties;
import com.opensabre.admin.security.request.LoginRequest;
import com.opensabre.admin.security.request.RegisterRequest;
import com.opensabre.admin.security.token.JwtTokenProvider;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 认证Controller - 提供登录、刷新Token、获取当前用户信息等接口
 * <p>
 * Token续期策略（根据 security.renewal.mode 配置）：
 * - none: 登录只返回 accessToken
 * - refresh-token: 登录返回 accessToken + refreshToken，通过 /refresh 接口刷新
 * - sliding-window: 登录只返回 accessToken，Filter自动续期（前端从响应头获取新Token）
 * </p>
 */
@Slf4j
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Tag(name = "认证管理", description = "登录、刷新Token、获取当前用户信息")
public class AuthController {

    private final JwtTokenProvider jwtTokenProvider;
    private final UserDetailsService userDetailsService;
    private final PasswordEncoder passwordEncoder;
    private final SecurityProperties securityProperties;
    private final SysUserMapper sysUserMapper;
    private final SysUserRoleMapper sysUserRoleMapper;

    /**
     * 普通用户角色ID（对应 sys_role 表中 role_key='user' 的记录）
     */
    private static final String DEFAULT_ROLE_ID = "2";

    /**
     * 用户注册
     * <p>
     * 注册成功后自动分配“普通用户”角色，需重新登录获取Token。
     * </p>
     */
    @Operation(summary = "用户注册", description = "注册新用户，默认分配普通用户角色")
    @PostMapping("/register")
    public Result<Object> register(@Valid @RequestBody RegisterRequest request) {
        String username = request.getUsername();

        // 检查用户名是否已存在
        SysUser existingUser = sysUserMapper.selectByUsername(username);
        if (existingUser != null) {
            return Result.fail("用户名已存在");
        }

        // 创建用户
        SysUser newUser = new SysUser();
        newUser.setUsername(username);
        newUser.setPassword(passwordEncoder.encode(request.getPassword()));
        newUser.setNickname(username);
        newUser.setStatus(1); // 正常状态
        sysUserMapper.insert(newUser);

        // 分配默认角色：普通用户
        SysUserRole userRole = new SysUserRole();
        userRole.setUserId(newUser.getId());
        userRole.setRoleId(DEFAULT_ROLE_ID);
        sysUserRoleMapper.insert(userRole);

        log.info("用户注册成功: {}, 已分配默认角色[普通用户]", username);
        return Result.success();
    }

    /**
     * 用户登录
     * <p>
     * 根据续期模式返回不同的Token：
     * - mode=none: 返回 {token, username}
     * - mode=refresh-token: 返回 {accessToken, refreshToken, username}
     * - mode=sliding-window: 返回 {token, username}（后续通过响应头自动续期）
     * </p>
     */
    @Operation(summary = "用户登录", description = "通过用户名密码登录，返回JWT Token")
    @PostMapping("/login")
    public Result<Object> login(@Valid @RequestBody LoginRequest request) {
        try {
            UserDetails userDetails = userDetailsService.loadUserByUsername(request.getUsername());

            // 校验密码
            if (!passwordEncoder.matches(request.getPassword(), userDetails.getPassword())) {
                return Result.fail(SystemErrorType.PASSWORD_ERROR);
            }

            // 生成AccessToken
            String accessToken = jwtTokenProvider.createAccessToken(request.getUsername());

            Map<String, Object> data = new HashMap<>();
            data.put("username", request.getUsername());

            // 根据续期模式返回不同的Token
            if (securityProperties.getRenewal().getMode() == SecurityProperties.RenewalMode.REFRESH_TOKEN) {
                // 双Token模式：返回 accessToken + refreshToken
                String refreshToken = jwtTokenProvider.createRefreshToken(request.getUsername());
                data.put("accessToken", accessToken);
                data.put("refreshToken", refreshToken);
                log.info("用户登录成功(RefreshToken模式): {}", request.getUsername());
            } else {
                // 不续期 或 滑动窗口模式：只返回 accessToken
                data.put("token", accessToken);
                log.info("用户登录成功({}模式): {}",
                        securityProperties.getRenewal().getMode() == SecurityProperties.RenewalMode.SLIDING_WINDOW
                                ? "滑动窗口" : "不续期",
                        request.getUsername());
            }

            return Result.success(data);
        } catch (Exception e) {
            log.warn("登录失败: {}", e.getMessage());
            throw new BadCredentialsException("用户名或密码错误");
        }
    }

    /**
     * 刷新Token（仅 refresh-token 模式有效）
     * <p>
     * 使用 refreshToken 换取新的 accessToken。
     * 如果 refreshToken 也已过期，需要重新登录。
     * </p>
     */
    @Operation(summary = "刷新Token", description = "使用RefreshToken换取新的AccessToken（仅refresh-token模式）")
    @PostMapping("/refresh")
    public Result<Object> refresh(@RequestBody Map<String, String> request) {
        // 校验续期模式
        if (securityProperties.getRenewal().getMode() != SecurityProperties.RenewalMode.REFRESH_TOKEN) {
            return Result.fail("当前续期模式不支持刷新操作");
        }

        String refreshToken = request.get("refreshToken");
        if (refreshToken == null || refreshToken.isBlank()) {
            return Result.fail("refreshToken不能为空");
        }

        // 校验是否为有效的RefreshToken
        if (!jwtTokenProvider.validateToken(refreshToken)) {
            log.warn("RefreshToken无效或已过期");
            return Result.fail("RefreshToken已过期，请重新登录");
        }

        if (!jwtTokenProvider.isRefreshToken(refreshToken)) {
            log.warn("尝试使用非RefreshToken刷新");
            return Result.fail("无效的RefreshToken");
        }

        // 从RefreshToken中获取用户名
        String username = jwtTokenProvider.getUsernameFromToken(refreshToken);

        // 签发新的AccessToken
        String newAccessToken = jwtTokenProvider.createAccessToken(username);

        Map<String, Object> data = new HashMap<>();
        data.put("accessToken", newAccessToken);
        data.put("username", username);

        log.debug("Token刷新成功: {}", username);
        return Result.success(data);
    }

    /**
     * 获取当前登录用户信息（含角色和权限列表）
     */
    @Operation(summary = "获取当前用户信息", description = "需要携带Token访问，返回角色和权限标识")
    @GetMapping("/info")
    public Result<Object> getUserInfo(
            @org.springframework.security.core.annotation.AuthenticationPrincipal UserDetails userDetails) {
        Map<String, Object> info = new HashMap<>();
        info.put("username", userDetails.getUsername());

        // 分离角色和权限标识
        List<String> roles = new ArrayList<>();
        List<String> permissions = new ArrayList<>();
        userDetails.getAuthorities().forEach(auth -> {
            String authority = auth.getAuthority();
            if (authority != null && authority.startsWith("ROLE_")) {
                roles.add(authority);
            } else {
                permissions.add(authority);
            }
        });
        info.put("roles", roles);
        info.put("permissions", permissions);
        return Result.success(info);
    }

}
