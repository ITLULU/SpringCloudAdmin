package com.opensabre.admin.web.controller.system;

import com.opensabre.admin.common.entity.Result;
import com.opensabre.admin.web.controller.system.request.AddMenuRequest;
import com.opensabre.admin.web.controller.system.request.EditMenuRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 菜单管理 Controller
 */
@Tag(name = "菜单管理", description = "系统菜单CRUD接口")
@RestController
@RequestMapping("/api/system/menu")
public class SysMenuController {

    @Operation(summary = "查询菜单列表")
    @GetMapping("/list")
    @PreAuthorize("hasAuthority('system:menu:list')")
    public Result<Object> list() {
        // TODO: 接入 Service 层查询菜单树
        return Result.success(List.of());
    }

    @Operation(summary = "查询菜单详情")
    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('system:menu:query')")
    public Result<Object> getById(@PathVariable String id) {
        // TODO: 接入 Service 层查询
        return Result.success(null);
    }

    @Operation(summary = "新增菜单")
    @PostMapping
    @PreAuthorize("hasAuthority('system:menu:add')")
    public Result<Object> add(@Valid @RequestBody AddMenuRequest request) {
        // TODO: 接入 Service 层新增
        return Result.success();
    }

    @Operation(summary = "修改菜单")
    @PutMapping
    @PreAuthorize("hasAuthority('system:menu:edit')")
    public Result<Object> edit(@Valid @RequestBody EditMenuRequest request) {
        // TODO: 接入 Service 层修改
        return Result.success();
    }

    @Operation(summary = "删除菜单")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('system:menu:delete')")
    public Result<Object> delete(@PathVariable String id) {
        // TODO: 接入 Service 层删除
        return Result.success();
    }
}
