//package com.opensabre.admin.security.token;
//
//import org.springframework.core.convert.converter.Converter;
//import org.springframework.security.authentication.AbstractAuthenticationToken;
//import org.springframework.security.core.GrantedAuthority;
//import org.springframework.security.core.authority.SimpleGrantedAuthority;
//import org.springframework.security.oauth2.jwt.Jwt;
//import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
//import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
//
//import java.util.ArrayList;
//import java.util.Collection;
//import java.util.List;
//
///**
// * JWT 权限转换器 - 从授权服务器签发的 JWT claims 还原用户权限
// * <p>
// * 授权服务器（sca-authcenter）在签发 AccessToken 时，通过 OAuth2TokenCustomizer
// * 将用户的角色/权限写入 claims：
// * - roles: ROLE_ 开头的角色标识（如 ROLE_admin）
// * - permissions: 权限标识（如 system:user:list）
// * 本转换器将其还原为 GrantedAuthority，供 @PreAuthorize、hasRole 等使用。
// * </p>
// * <p>
// * principal 取 sub 声明（即用户名），保证 SecurityContextHolder 中
// * 的用户名与原自研 JWT 体系一致（UserFlowSlot、审计日志等依赖此行为）。
// * </p>
// */
//public class JwtAuthorityConverter implements Converter<Jwt, AbstractAuthenticationToken> {
//
//    /**
//     * 默认转换器：解析标准 scope/scp 声明（本项目未使用，但保留兼容性）
//     */
//    private final JwtGrantedAuthoritiesConverter defaultConverter = new JwtGrantedAuthoritiesConverter();
//
//    @Override
//    public AbstractAuthenticationToken convert(Jwt jwt) {
//        Collection<GrantedAuthority> authorities = new ArrayList<>(defaultConverter.convert(jwt));
//
//        // 还原角色（ROLE_admin）
//        List<String> roles = jwt.getClaimAsStringList("roles");
//        if (roles != null) {
//            for (String role : roles) {
//                authorities.add(new SimpleGrantedAuthority(role));
//            }
//        }
//
//        // 还原权限标识（system:user:list）
//        List<String> permissions = jwt.getClaimAsStringList("permissions");
//        if (permissions != null) {
//            for (String permission : permissions) {
//                authorities.add(new SimpleGrantedAuthority(permission));
//            }
//        }
//
//        // principal 使用用户名（sub 声明）
//        return new JwtAuthenticationToken(jwt, authorities, jwt.getSubject());
//    }
//}
