package com.opensabre.admin.web.controller.system;

import com.opensabre.admin.common.entity.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * 用户管理 Controller
 * <p>
 * 通过 @PreAuthorize 声明接口所需权限，Spring Security 会在请求到达前校验当前用户是否拥有对应权限标识。
 * 无权限时返回 403（由 AccessDeniedHandlerImpl 处理）。
 * </p>
 */
@Tag(name = "用户管理", description = "系统用户CRUD接口")
@RestController
@RequestMapping("/api/system/user")
public class SysUserController {

    @Operation(summary = "查询用户列表")
    @GetMapping("/list")
    @PreAuthorize("hasAuthority('system:user:list')")
    public Result<Object> list(
            @RequestParam(required = false) String username,
            @RequestParam(required = false) String phone,
            @RequestParam(required = false) Integer status,
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize) {
        // TODO: 接入 Service 层查询
        Map<String, Object> data = new HashMap<>();
        data.put("total", 0);
        data.put("rows", java.util.List.of());
        return Result.success(data);
    }

    @Operation(summary = "查询用户详情")
    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('system:user:query')")
    public Result<Object> getById(@PathVariable String id) {
        // TODO: 接入 Service 层查询
        return Result.success(null);
    }

    @Operation(summary = "新增用户")
    @PostMapping
    @PreAuthorize("hasAuthority('system:user:add')")
    public Result<Object> add(@RequestBody Map<String, Object> body) {
        // TODO: 接入 Service 层新增
        return Result.success();
    }

    @Operation(summary = "修改用户")
    @PutMapping
    @PreAuthorize("hasAuthority('system:user:edit')")
    public Result<Object> edit(@RequestBody Map<String, Object> body) {
        // TODO: 接入 Service 层修改
        return Result.success();
    }

    @Operation(summary = "删除用户")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('system:user:delete')")
    public Result<Object> delete(@PathVariable String id) {
        // TODO: 接入 Service 层删除
        return Result.success();
    }

    @Operation(summary = "重置密码")
    @PutMapping("/resetPwd")
    @PreAuthorize("hasAuthority('system:user:edit')")
    public Result<Object> resetPassword(@RequestBody Map<String, Object> body) {
        // TODO: 接入 Service 层重置密码
        return Result.success();
    }
}
