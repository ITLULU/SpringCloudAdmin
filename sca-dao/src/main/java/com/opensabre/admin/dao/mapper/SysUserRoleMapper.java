package com.opensabre.admin.dao.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.opensabre.admin.dao.entity.po.SysUserRole;
import org.apache.ibatis.annotations.Mapper;

/**
 * 用户角色关联 Mapper
 */
@Mapper
public interface SysUserRoleMapper extends BaseMapper<SysUserRole> {
}
