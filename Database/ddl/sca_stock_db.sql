-- =============================================
-- 库存微服务数据库 DDL (sca_stock_db)
-- Seata 启用后会自动创建 undo_log 表
-- =============================================
CREATE DATABASE IF NOT EXISTS sca_stock_db;
USE sca_stock_db;

SET NAMES utf8mb4;
SET CHARACTER SET utf8mb4;

-- ----------------------------
-- 1. 商品规格表（库存核心）
-- ----------------------------
CREATE TABLE IF NOT EXISTS hotel_product_spec (
    id          varchar(32)   NOT NULL COMMENT '主键ID（雪花算法）',
    product_id  varchar(32)   NOT NULL COMMENT '所属商品ID',
    spec_name   varchar(64)   DEFAULT '' COMMENT '规格名称（如：大份/小份）',
    spec_value  varchar(256)  DEFAULT '' COMMENT '规格值描述',
    stock       int           DEFAULT 0 COMMENT '库存数量',
    price       decimal(10,2) DEFAULT 0.00 COMMENT '规格价格',
    sort        int           DEFAULT 0 COMMENT '排序',
    created_by  varchar(32)   DEFAULT 'system' COMMENT '创建人',
    created_time datetime     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_by  varchar(32)   DEFAULT 'system' COMMENT '更新人',
    updated_time datetime     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    KEY idx_product_id (product_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='商品规格表（库存）';

-- ----------------------------
-- 2. Seata undo_log 表（启用 Seata 时执行）
-- ----------------------------
-- CREATE TABLE IF NOT EXISTS undo_log (
--     id            bigint(20)   NOT NULL AUTO_INCREMENT COMMENT '主键',
--     branch_id     bigint(20)   NOT NULL COMMENT '分支事务ID',
--     xid           varchar(100) NOT NULL COMMENT '全局事务ID',
--     context       varchar(128) NOT NULL COMMENT '上下文',
--     rollback_info longblob     NOT NULL COMMENT '回滚信息',
--     log_status    int(11)      NOT NULL COMMENT '日志状态',
--     log_created   datetime(6)  NOT NULL COMMENT '创建时间',
--     log_modified  datetime(6)  NOT NULL COMMENT '修改时间',
--     PRIMARY KEY (id),
--     UNIQUE KEY ux_undo_log (xid, branch_id)
-- ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Seata AT模式 undo_log';
