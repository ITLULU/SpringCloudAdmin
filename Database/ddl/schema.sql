-- =============================================
-- SpringCloudAdmin RBAC 权限模型 DDL
-- 数据库：MySQL 8.x
-- =============================================

SET NAMES utf8mb4;
SET CHARACTER SET utf8mb4;

-- ----------------------------
-- 1. 用户表
-- ----------------------------
DROP TABLE IF EXISTS sys_user;
CREATE TABLE sys_user (
    id          varchar(32)  NOT NULL COMMENT '主键ID（雪花算法）',
    username    varchar(64)  NOT NULL COMMENT '登录用户名',
    password    varchar(128) NOT NULL COMMENT '密码（BCrypt加密）',
    nickname    varchar(64)  DEFAULT '' COMMENT '用户昵称',
    email       varchar(128) DEFAULT '' COMMENT '邮箱',
    phone       varchar(20)  DEFAULT '' COMMENT '手机号',
    avatar      varchar(256) DEFAULT '' COMMENT '头像URL',
    status      tinyint      DEFAULT 1 COMMENT '状态：0-禁用 1-正常',
    created_by  varchar(32)  DEFAULT 'system' COMMENT '创建人',
    created_time datetime     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_by  varchar(32)  DEFAULT 'system' COMMENT '更新人',
    updated_time datetime     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_username (username)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='用户表';

-- ----------------------------
-- 2. 角色表
-- ----------------------------
DROP TABLE IF EXISTS sys_role;
CREATE TABLE sys_role (
    id          varchar(32)  NOT NULL COMMENT '主键ID（雪花算法）',
    role_name   varchar(64)  NOT NULL COMMENT '角色名称',
    role_key    varchar(64)  NOT NULL COMMENT '角色标识（如 admin）',
    sort        int          DEFAULT 0 COMMENT '显示排序',
    status      tinyint      DEFAULT 1 COMMENT '状态：0-禁用 1-正常',
    remark      varchar(256) DEFAULT '' COMMENT '备注',
    created_by  varchar(32)  DEFAULT 'system' COMMENT '创建人',
    created_time datetime     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_by  varchar(32)  DEFAULT 'system' COMMENT '更新人',
    updated_time datetime     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_role_key (role_key)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='角色表';

-- ----------------------------
-- 3. 菜单权限表
-- ----------------------------
DROP TABLE IF EXISTS sys_menu;
CREATE TABLE sys_menu (
    id          varchar(32)  NOT NULL COMMENT '主键ID（雪花算法）',
    parent_id   varchar(32)  DEFAULT '0' COMMENT '父菜单ID（0=顶级）',
    menu_name   varchar(64)  NOT NULL COMMENT '菜单名称',
    menu_type   char(1)      DEFAULT 'M' COMMENT '类型：M-目录 C-菜单 F-按钮',
    path        varchar(200) DEFAULT '' COMMENT '路由路径',
    component   varchar(200) DEFAULT '' COMMENT '组件路径',
    icon        varchar(64)  DEFAULT '' COMMENT '图标名称',
    permission  varchar(128) DEFAULT '' COMMENT '权限标识（如 system:user:list）',
    sort        int          DEFAULT 0 COMMENT '显示排序',
    status      tinyint      DEFAULT 1 COMMENT '状态：0-禁用 1-正常',
    visible     tinyint      DEFAULT 1 COMMENT '是否可见：0-隐藏 1-显示',
    created_by  varchar(32)  DEFAULT 'system' COMMENT '创建人',
    created_time datetime     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_by  varchar(32)  DEFAULT 'system' COMMENT '更新人',
    updated_time datetime     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    KEY idx_parent_id (parent_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='菜单权限表';

-- ----------------------------
-- 4. 用户角色关联表
-- ----------------------------
DROP TABLE IF EXISTS sys_user_role;
CREATE TABLE sys_user_role (
    id          varchar(32)  NOT NULL COMMENT '主键ID（雪花算法）',
    user_id     varchar(32)  NOT NULL COMMENT '用户ID',
    role_id     varchar(32)  NOT NULL COMMENT '角色ID',
    PRIMARY KEY (id),
    UNIQUE KEY uk_user_role (user_id, role_id),
    KEY idx_role_id (role_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='用户角色关联表';

-- ----------------------------
-- 5. 角色菜单关联表
-- ----------------------------
DROP TABLE IF EXISTS sys_role_menu;
CREATE TABLE sys_role_menu (
    id          varchar(32)  NOT NULL COMMENT '主键ID（雪花算法）',
    role_id     varchar(32)  NOT NULL COMMENT '角色ID',
    menu_id     varchar(32)  NOT NULL COMMENT '菜单ID',
    PRIMARY KEY (id),
    UNIQUE KEY uk_role_menu (role_id, menu_id),
    KEY idx_menu_id (menu_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='角色菜单关联表';


-- =============================================
-- 初始数据
-- =============================================

-- 用户（密码为 BCrypt 加密）
-- admin / admin123
INSERT INTO sys_user (id, username, password, nickname, email, phone, status) VALUES
('1', 'admin', '$2a$10$7JB720yubVSZvUI0rEqW/.VqGOZTH.ulu33dHOiBE8ByOhJIrdAu2', '超级管理员', 'admin@example.com', '13800138000', 1);
-- user / user123
INSERT INTO sys_user (id, username, password, nickname, email, phone, status) VALUES
('2', 'user', '$2a$10$WiKsGhOYBwKXZkHkZkHkHuYqGv5JXkHkHkHkHkHkHkHkHkHkHkHk', '普通用户', 'user@example.com', '13800138001', 1);

-- 角色
INSERT INTO sys_role (id, role_name, role_key, sort, status, remark) VALUES
('1', '超级管理员', 'admin', 1, 1, '拥有所有权限'),
('2', '普通用户',   'user',  2, 1, '普通用户角色');

-- 菜单权限
-- 一级目录：系统管理
INSERT INTO sys_menu (id, parent_id, menu_name, menu_type, path, icon, sort, status, visible) VALUES
('100', '0', '系统管理', 'M', '/system', 'Setting', 1, 1, 1);

-- 二级菜单
INSERT INTO sys_menu (id, parent_id, menu_name, menu_type, path, component, icon, permission, sort, status, visible) VALUES
('101', '100', '用户管理', 'C', 'user', 'system/user/index', 'User', 'system:user:list', 1, 1, 1),
('102', '100', '角色管理', 'C', 'role', 'system/role/index', 'UserFilled', 'system:role:list', 2, 1, 1),
('103', '100', '菜单管理', 'C', 'menu', 'system/menu/index', 'Menu', 'system:menu:list', 3, 1, 1);

-- 按钮权限：用户管理
INSERT INTO sys_menu (id, parent_id, menu_name, menu_type, permission, sort, status, visible) VALUES
('1001', '101', '用户查询', 'F', 'system:user:query',  1, 1, 1),
('1002', '101', '用户新增', 'F', 'system:user:add',    2, 1, 1),
('1003', '101', '用户修改', 'F', 'system:user:edit',   3, 1, 1),
('1004', '101', '用户删除', 'F', 'system:user:delete', 4, 1, 1);

-- 按钮权限：角色管理
INSERT INTO sys_menu (id, parent_id, menu_name, menu_type, permission, sort, status, visible) VALUES
('1005', '102', '角色查询', 'F', 'system:role:query',  1, 1, 1),
('1006', '102', '角色新增', 'F', 'system:role:add',    2, 1, 1),
('1007', '102', '角色修改', 'F', 'system:role:edit',   3, 1, 1),
('1008', '102', '角色删除', 'F', 'system:role:delete', 4, 1, 1);

-- 按钮权限：菜单管理
INSERT INTO sys_menu (id, parent_id, menu_name, menu_type, permission, sort, status, visible) VALUES
('1009', '103', '菜单查询', 'F', 'system:menu:query',  1, 1, 1),
('1010', '103', '菜单新增', 'F', 'system:menu:add',    2, 1, 1),
('1011', '103', '菜单修改', 'F', 'system:menu:edit',   3, 1, 1),
('1012', '103', '菜单删除', 'F', 'system:menu:delete', 4, 1, 1);

-- 用户角色关联
INSERT INTO sys_user_role (id, user_id, role_id) VALUES
('1', '1', '1'),  -- admin -> 超级管理员
('2', '2', '2');  -- user  -> 普通用户

-- 角色菜单关联（超级管理员拥有所有菜单权限）
INSERT INTO sys_role_menu (id, role_id, menu_id) VALUES
('1',  '1', '100'),
('2',  '1', '101'),
('3',  '1', '102'),
('4',  '1', '103'),
('5',  '1', '1001'),
('6',  '1', '1002'),
('7',  '1', '1003'),
('8',  '1', '1004'),
('9',  '1', '1005'),
('10', '1', '1006'),
('11', '1', '1007'),
('12', '1', '1008'),
('13', '1', '1009'),
('14', '1', '1010'),
('15', '1', '1011'),
('16', '1', '1012');

-- 普通用户只有查看权限
INSERT INTO sys_role_menu (id, role_id, menu_id) VALUES
('17', '2', '100'),
('18', '2', '101'),
('19', '2', '1001');
