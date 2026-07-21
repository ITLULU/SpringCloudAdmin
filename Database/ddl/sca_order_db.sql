-- =============================================
-- 订单微服务数据库 DDL (sca_order_db)
-- Seata AT模式需要在每个参与库中创建 undo_log 表
-- =============================================
CREATE DATABASE IF NOT EXISTS sca_order_db;
USE sca_order_db;

SET NAMES utf8mb4;
SET CHARACTER SET utf8mb4;

-- ----------------------------
-- 1. 订单主表
-- ----------------------------
CREATE TABLE IF NOT EXISTS hotel_order (
    id          varchar(32)  NOT NULL COMMENT '主键ID（雪花算法）',
    user_id     varchar(32)  NOT NULL COMMENT '用户ID',
    hotel_id    varchar(32)  NOT NULL COMMENT '酒店ID',
    trip_id     varchar(32)  DEFAULT NULL COMMENT '关联行程ID',
    status      tinyint      DEFAULT 1 COMMENT '状态：0-已取消 1-已完成',
    created_by  varchar(32)  DEFAULT 'system' COMMENT '创建人',
    created_time datetime    DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_by  varchar(32)  DEFAULT 'system' COMMENT '更新人',
    updated_time datetime    DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    KEY idx_user_id (user_id),
    KEY idx_trip_id (trip_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='订单表';

-- ----------------------------
-- 2. 订单明细表
-- ----------------------------
CREATE TABLE IF NOT EXISTS hotel_order_item (
    id           varchar(32)   NOT NULL COMMENT '主键ID（雪花算法）',
    order_id     varchar(32)   NOT NULL COMMENT '订单ID',
    product_id   varchar(32)   NOT NULL COMMENT '商品ID',
    spec_id      varchar(32)   NOT NULL COMMENT '规格ID',
    product_name varchar(128)  DEFAULT '' COMMENT '商品名（冗余）',
    spec_name    varchar(64)   DEFAULT '' COMMENT '规格名（冗余）',
    quantity     int           DEFAULT 0 COMMENT '数量',
    price        decimal(10,2) DEFAULT 0.00 COMMENT '下单时单价',
    created_by   varchar(32)   DEFAULT 'system' COMMENT '创建人',
    created_time datetime      DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_by   varchar(32)   DEFAULT 'system' COMMENT '更新人',
    updated_time datetime      DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    KEY idx_order_id (order_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='订单明细表';

-- ----------------------------
-- 3. Seata undo_log 表
-- ----------------------------
CREATE TABLE IF NOT EXISTS undo_log (
    id            bigint(20)   NOT NULL AUTO_INCREMENT COMMENT '主键',
    branch_id     bigint(20)   NOT NULL COMMENT '分支事务ID',
    xid           varchar(100) NOT NULL COMMENT '全局事务ID',
    context       varchar(128) NOT NULL COMMENT '上下文',
    rollback_info longblob     NOT NULL COMMENT '回滚信息',
    log_status    int(11)      NOT NULL COMMENT '日志状态',
    log_created   datetime(6)  NOT NULL COMMENT '创建时间',
    log_modified  datetime(6)  NOT NULL COMMENT '修改时间',
    PRIMARY KEY (id),
    UNIQUE KEY ux_undo_log (xid, branch_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Seata AT模式 undo_log';
