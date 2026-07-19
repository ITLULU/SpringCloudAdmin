# Security RBAC 数据表设计

## 模型说明

采用经典 RBAC（Role-Based Access Control）模型：
- 用户(User) <-> 角色(Role)：多对多
- 角色(Role) <-> 菜单/权限(Menu)：多对多
- 菜单(Menu)：树形结构（目录 / 菜单 / 按钮），支持权限标识

```
sys_user ──M:N──> sys_role ──M:N──> sys_menu
         (sys_user_role)      (sys_role_menu)
```

## 表设计

### 表1: sys_user（用户表）

| 字段 | 类型 | 说明 |
|------|------|------|
| id | varchar(32) PK | 雪花ID（继承BasePo） |
| username | varchar(64) UNIQUE | 登录用户名 |
| password | varchar(128) | BCrypt加密密码 |
| nickname | varchar(64) | 用户昵称 |
| email | varchar(128) | 邮箱 |
| phone | varchar(20) | 手机号 |
| avatar | varchar(256) | 头像URL |
| status | tinyint | 状态：0-禁用 1-正常 |
| created_by / created_time / updated_by / updated_time | - | 审计字段（继承BasePo） |

### 表2: sys_role（角色表）

| 字段 | 类型 | 说明 |
|------|------|------|
| id | varchar(32) PK | 雪花ID |
| role_name | varchar(64) | 角色名称（如：超级管理员） |
| role_key | varchar(64) UNIQUE | 角色标识（如：admin） |
| sort | int | 显示排序 |
| status | tinyint | 状态：0-禁用 1-正常 |
| remark | varchar(256) | 备注 |
| created_by / created_time / updated_by / updated_time | - | 审计字段 |

### 表3: sys_menu（菜单权限表）

| 字段 | 类型 | 说明 |
|------|------|------|
| id | varchar(32) PK | 雪花ID |
| parent_id | varchar(32) | 父菜单ID（0=顶级） |
| menu_name | varchar(64) | 菜单名称 |
| menu_type | char(1) | 类型：M-目录 C-菜单 F-按钮 |
| path | varchar(200) | 路由路径 |
| component | varchar(200) | 组件路径（如 system/user/index） |
| icon | varchar(64) | 图标名称 |
| permission | varchar(128) | 权限标识（如 system:user:list） |
| sort | int | 显示排序 |
| status | tinyint | 状态：0-禁用 1-正常 |
| visible | tinyint | 是否可见：0-隐藏 1-显示 |
| created_by / created_time / updated_by / updated_time | - | 审计字段 |

### 表4: sys_user_role（用户角色关联表）

| 字段 | 类型 | 说明 |
|------|------|------|
| id | varchar(32) PK | 雪花ID |
| user_id | varchar(32) | 用户ID |
| role_id | varchar(32) | 角色ID |

> 联合唯一索引：(user_id, role_id)

### 表5: sys_role_menu（角色菜单关联表）

| 字段 | 类型 | 说明 |
|------|------|------|
| id | varchar(32) PK | 雪花ID |
| role_id | varchar(32) | 角色ID |
| menu_id | varchar(32) | 菜单ID |

> 联合唯一索引：(role_id, menu_id)

## 初始数据

- admin 用户（BCrypt 加密的 admin123）
- user 用户（BCrypt 加密的 user123）
- 超级管理员角色 + 普通用户角色
- 系统管理目录 + 用户/角色/菜单管理菜单 + 对应按钮权限

## 实现任务

### Task 1: 生成 DDL 脚本
- 文件：`sca-dao/src/main/resources/db/schema.sql`
- 包含 5 张表的建表语句 + 索引 + 初始数据 INSERT

### Task 2: 创建 PO 实体类
- 位置：`sca-dao/src/main/java/com/opensabre/admin/dao/entity/po/`
- `SysUser.java`、`SysRole.java`、`SysMenu.java`、`SysUserRole.java`、`SysRoleMenu.java`
- 继承 `BasePo`，使用 MyBatis-Plus 注解（@TableName, @TableField 等）

### Task 3: 创建 Mapper 接口
- 位置：`sca-dao/src/main/java/com/opensabre/admin/dao/mapper/`
- 继承 `BaseMapper<T>`
- `SysUserMapper` 需自定义方法：根据用户ID查询角色列表、根据用户ID查询权限列表

### Task 4: 替换 MockUserDetailsServiceImpl
- 新建 `UserDetailsServiceImpl` 从数据库查询用户、角色、权限
- 替换或移除 Mock 实现
