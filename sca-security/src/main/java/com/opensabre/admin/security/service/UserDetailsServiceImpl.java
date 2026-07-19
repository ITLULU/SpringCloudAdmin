package com.opensabre.admin.security.service;

import com.opensabre.admin.dao.entity.po.SysUser;
import com.opensabre.admin.dao.mapper.SysUserMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * 用户详情服务 - 数据库实现
 * <p>
 * 从 sys_user / sys_user_role / sys_role / sys_role_menu / sys_menu 表
 * 查询用户信息、角色、权限标识
 * </p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserDetailsServiceImpl implements UserDetailsService {

    private final SysUserMapper sysUserMapper;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        // 1. 查询用户
        SysUser sysUser = sysUserMapper.selectByUsername(username);
        if (sysUser == null) {
            log.warn("用户不存在: {}", username);
            throw new UsernameNotFoundException("用户不存在: " + username);
        }

        // 2. 检查用户状态
        if (sysUser.getStatus() != null && sysUser.getStatus() == 0) {
            log.warn("用户已被禁用: {}", username);
            throw new UsernameNotFoundException("用户已被禁用: " + username);
        }

        // 3. 查询角色标识（如 ROLE_ADMIN）
        List<String> roleKeys = sysUserMapper.selectRoleKeysByUserId(sysUser.getId());
        List<SimpleGrantedAuthority> authorities = new ArrayList<>();
        for (String roleKey : roleKeys) {
            authorities.add(new SimpleGrantedAuthority("ROLE_" + roleKey));
        }

        // 4. 查询权限标识（如 system:user:list）
        List<String> permissions = sysUserMapper.selectPermissionsByUserId(sysUser.getId());
        for (String permission : permissions) {
            authorities.add(new SimpleGrantedAuthority(permission));
        }

        return new User(
                sysUser.getUsername(),
                sysUser.getPassword(),
                authorities
        );
    }
}
