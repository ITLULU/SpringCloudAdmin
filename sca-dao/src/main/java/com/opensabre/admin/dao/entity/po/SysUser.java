package com.opensabre.admin.dao.entity.po;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 用户表 PO
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_user")
public class SysUser extends BasePo {

    /** 登录用户名 */
    private String username;

    /** 密码（BCrypt加密） */
    private String password;

    /** 用户昵称 */
    private String nickname;

    /** 邮箱 */
    private String email;

    /** 手机号 */
    private String phone;

    /** 头像URL */
    private String avatar;

    /** 状态：0-禁用 1-正常 */
    private Integer status;
}
