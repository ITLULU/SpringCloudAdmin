package com.opensabre.admin.security.service;

import com.opensabre.admin.dao.entity.po.SysUser;
import com.opensabre.admin.dao.mapper.SysUserMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.DisabledException;
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
 * 查询用户信息、角色、权限标识，构建 Spring Security 的 UserDetails 对象。
 * </p>
 * <p>
 * 用户状态映射（sys_user.status → UserDetails 状态字段）：
 * - status=0（禁用）→ enabled=false, accountNonLocked=false
 * - status=1（正常）→ 所有状态字段均为 true
 * </p>
 */
@Slf4j
@Service
public class UserDetailsServiceImpl implements UserDetailsService {

    /**
     * 用户状态：正常
     */
    private static final int STATUS_ENABLED = 1;

    /**
     * 用户状态：禁用
     */
    private static final int STATUS_DISABLED = 0;

    @Autowired
    private SysUserMapper sysUserMapper;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        // 1. 根据用户名查询用户记录
        SysUser sysUser = sysUserMapper.selectByUsername(username);
        if (sysUser == null) {
            log.warn("用户不存在: {}", username);
            throw new UsernameNotFoundException("用户不存在: " + username);
        }

        // 2. 校验用户状态
        validateUserStatus(sysUser);

        // 3. 查询角色标识（如 ROLE_ADMIN → SimpleGrantedAuthority("ROLE_admin")）
        List<String> roleKeys = sysUserMapper.selectRoleKeysByUserId(sysUser.getId());
        List<SimpleGrantedAuthority> authorities = new ArrayList<>();
        for (String roleKey : roleKeys) {
            authorities.add(new SimpleGrantedAuthority("ROLE_" + roleKey));
        }

        // 4. 查询权限标识（如 system:user:list → SimpleGrantedAuthority("system:user:list")）
        List<String> permissions = sysUserMapper.selectPermissionsByUserId(sysUser.getId());
        for (String permission : permissions) {
            authorities.add(new SimpleGrantedAuthority(permission));
        }

        // 5. 构建 UserDetails，正确设置用户状态字段
        boolean enabled = (sysUser.getStatus() != null && sysUser.getStatus() == STATUS_ENABLED);
        return User.builder()
                .username(sysUser.getUsername())
                .password(sysUser.getPassword())
                .authorities(authorities)
                .disabled(!enabled)           // status=0 时为禁用状态
                .accountLocked(!enabled)      // status=0 时账户锁定
                .build();
    }

    /**
     * 校验用户状态
     * <p>
     * 如果用户状态为禁用，抛出 DisabledException，
     * 该异常会被 JwtAuthenticationFilter 捕获并记录日志，认证不通过。
     * </p>
     *
     * @param sysUser 用户实体
     * @throws DisabledException 用户被禁用时抛出
     */
    private void validateUserStatus(SysUser sysUser) {
        if (sysUser.getStatus() != null && sysUser.getStatus() == STATUS_DISABLED) {
            log.warn("用户已被禁用: {}", sysUser.getUsername());
            throw new DisabledException("用户已被禁用: " + sysUser.getUsername());
        }
    }
}
