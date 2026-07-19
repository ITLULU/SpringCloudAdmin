package com.opensabre.admin.web.controller.system;

import com.opensabre.admin.common.entity.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * 角色管理 Controller
 */
@Tag(name = "角色管理", description = "系统角色CRUD接口")
@RestController
@RequestMapping("/api/system/role")
public class SysRoleController {

    @Operation(summary = "查询角色列表")
    @GetMapping("/list")
    @PreAuthorize("hasAuthority('system:role:list')")
    public Result<Object> list(
            @RequestParam(required = false) String roleName,
            @RequestParam(required = false) Integer status,
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize) {
        // TODO: 接入 Service 层查询
        Map<String, Object> data = new HashMap<>();
        data.put("total", 0);
        data.put("rows", java.util.List.of());
        return Result.success(data);
    }

    @Operation(summary = "查询角色详情")
    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('system:role:query')")
    public Result<Object> getById(@PathVariable String id) {
        // TODO: 接入 Service 层查询
        return Result.success(null);
    }

    @Operation(summary = "新增角色")
    @PostMapping
    @PreAuthorize("hasAuthority('system:role:add')")
    public Result<Object> add(@RequestBody Map<String, Object> body) {
        // TODO: 接入 Service 层新增
        return Result.success();
    }

    @Operation(summary = "修改角色")
    @PutMapping
    @PreAuthorize("hasAuthority('system:role:edit')")
    public Result<Object> edit(@RequestBody Map<String, Object> body) {
        // TODO: 接入 Service 层修改
        return Result.success();
    }

    @Operation(summary = "删除角色")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('system:role:delete')")
    public Result<Object> delete(@PathVariable String id) {
        // TODO: 接入 Service 层删除
        return Result.success();
    }
}
