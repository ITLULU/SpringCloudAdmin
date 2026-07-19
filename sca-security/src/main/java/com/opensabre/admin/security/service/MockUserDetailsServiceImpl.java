package com.opensabre.admin.security.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 用户详情服务 - 临时实现（内存Mock用户数据）
 * <p>
 * 后续接入数据库时，替换为从 sys_user / sys_role / sys_menu 表查询
 * </p>
 */
@Slf4j
@Service
public class MockUserDetailsServiceImpl implements UserDetailsService {

    private final Map<String, MockUser> userStore = new ConcurrentHashMap<>();

    public MockUserDetailsServiceImpl(PasswordEncoder passwordEncoder) {
        // 管理员: admin / admin123
        userStore.put("admin", new MockUser(
                "admin",
                passwordEncoder.encode("admin123"),
                List.of("ROLE_ADMIN", "system:user:list", "system:role:list", "system:menu:list")
        ));
        // 普通用户: user / user123
        userStore.put("user", new MockUser(
                "user",
                passwordEncoder.encode("user123"),
                List.of("ROLE_USER")
        ));
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        MockUser mockUser = userStore.get(username);
        if (mockUser == null) {
            log.warn("用户不存在: {}", username);
            throw new UsernameNotFoundException("用户不存在: " + username);
        }

        List<SimpleGrantedAuthority> authorities = mockUser.permissions().stream()
                .map(SimpleGrantedAuthority::new)
                .toList();

        return new User(
                mockUser.username(),
                mockUser.encodedPassword(),
                authorities
        );
    }

    private record MockUser(String username, String encodedPassword, List<String> permissions) {}
}
