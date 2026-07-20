package com.opensabre.admin.web.controller.system;

import com.opensabre.admin.common.entity.Result;
import com.opensabre.admin.web.controller.system.request.AddRoleRequest;
import com.opensabre.admin.web.controller.system.request.EditRoleRequest;
import com.opensabre.admin.web.controller.system.response.PageResult;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

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
        return Result.success(new PageResult<>(0L, List.of()));
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
    public Result<Object> add(@Valid @RequestBody AddRoleRequest request) {
        // TODO: 接入 Service 层新增
        return Result.success();
    }

    @Operation(summary = "修改角色")
    @PutMapping
    @PreAuthorize("hasAuthority('system:role:edit')")
    public Result<Object> edit(@Valid @RequestBody EditRoleRequest request) {
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
