package com.opensabre.admin.dao.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.opensabre.admin.dao.entity.po.SysUserRole;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 用户角色关联 Mapper
 */
@Mapper
public interface SysUserRoleMapper extends BaseMapper<SysUserRole> {

    /**
     * 根据用户ID删除用户角色关联
     */
    int deleteByUserId(@Param("userId") String userId);

    /**
     * 根据角色ID删除用户角色关联
     */
    int deleteByRoleId(@Param("roleId") String roleId);
}
