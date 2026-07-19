package com.opensabre.admin.dao.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.opensabre.admin.dao.entity.po.SysUser;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 用户 Mapper
 */
@Mapper
public interface SysUserMapper extends BaseMapper<SysUser> {

    /**
     * 根据用户名查询用户（含角色信息）
     */
    SysUser selectByUsername(@Param("username") String username);

    /**
     * 根据用户ID查询角色标识列表
     */
    List<String> selectRoleKeysByUserId(@Param("userId") String userId);

    /**
     * 根据用户ID查询权限标识列表
     */
    List<String> selectPermissionsByUserId(@Param("userId") String userId);
}
