package com.opensabre.admin.security.controller;

import com.opensabre.admin.common.entity.Result;
import com.opensabre.admin.common.exception.SystemErrorType;
import com.opensabre.admin.security.request.LoginRequest;
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

import java.util.HashMap;
import java.util.Map;

/**
 * 认证Controller - 提供登录、获取当前用户信息等接口
 */
@Slf4j
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Tag(name = "认证管理", description = "登录、登出、获取当前用户信息")
public class AuthController {

    private final JwtTokenProvider jwtTokenProvider;
    private final UserDetailsService userDetailsService;
    private final PasswordEncoder passwordEncoder;

    /**
     * 用户登录
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

            // 生成Token
            String token = jwtTokenProvider.createToken(request.getUsername());

            Map<String, String> data = new HashMap<>();
            data.put("token", token);
            data.put("username", request.getUsername());

            log.info("用户登录成功: {}", request.getUsername());
            return Result.success(data);
        } catch (Exception e) {
            log.warn("登录失败: {}", e.getMessage());
            throw new BadCredentialsException("用户名或密码错误");
        }
    }

    /**
     * 获取当前登录用户信息
     */
    @Operation(summary = "获取当前用户信息", description = "需要携带Token访问")
    @GetMapping("/info")
    public Result<Object> getUserInfo(
            @org.springframework.security.core.annotation.AuthenticationPrincipal UserDetails userDetails) {
        Map<String, Object> info = new HashMap<>();
        info.put("username", userDetails.getUsername());
        info.put("roles", userDetails.getAuthorities().stream()
                .map(Object::toString)
                .toList());
        return Result.success(info);
    }

}
