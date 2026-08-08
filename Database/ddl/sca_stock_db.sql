-- =============================================
-- 库存微服务数据库 DDL (sca_stock_db)
-- Seata AT模式需要在每个参与库中创建 undo_log 表
-- =============================================
CREATE DATABASE IF NOT EXISTS sca_stock_db;
USE sca_stock_db;

SET NAMES utf8mb4;
SET CHARACTER SET utf8mb4;

-- ----------------------------
-- 1. 酒店商品表
-- ----------------------------
CREATE TABLE IF NOT EXISTS hotel_product (
    id          varchar(32)  NOT NULL COMMENT '主键ID（雪花算法）',
    hotel_id    varchar(32)  NOT NULL COMMENT '所属酒店ID',
    name        varchar(128) NOT NULL COMMENT '商品名称',
    description text         COMMENT '商品描述',
    price       decimal(10,2) DEFAULT 0.00 COMMENT '价格（当前均为0）',
    cover_image varchar(256) DEFAULT '' COMMENT '封面图URL',
    images      varchar(1024) DEFAULT '' COMMENT '商品图片（逗号分隔URL）',
    sort        int          DEFAULT 0 COMMENT '排序',
    status      tinyint      DEFAULT 1 COMMENT '状态：0-下架 1-上架',
    created_by  varchar(32)  DEFAULT 'system' COMMENT '创建人',
    created_time datetime    DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_by  varchar(32)  DEFAULT 'system' COMMENT '更新人',
    updated_time datetime    DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    KEY idx_hotel_id (hotel_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='酒店商品表';

-- ----------------------------
-- 2. 商品规格表（库存核心）
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


-- 酒店商品
INSERT INTO hotel_product (id, hotel_id, name, description, price, cover_image, sort, status) VALUES
                                                                                                  ('1', '1', '自助早餐券', '酒店一楼餐厅自助早餐，品种丰富', 0.00, '/images/product/breakfast.png', 1, 1),
                                                                                                  ('2', '1', 'SPA体验券', '酒店三楼水疗中心，放松身心', 0.00, '/images/product/spa.png', 2, 1),
                                                                                                  ('3', '1', '健身房次卡', '酒店负一层健身中心，器械齐全', 0.00, '/images/product/gym.png', 3, 1),
                                                                                                  ('4', '2', '商务会议室', '可容纳20人的会议室，配备投影设备', 0.00, '/images/product/meeting.png', 1, 1),
                                                                                                  ('5', '2', '下午茶套餐', '大堂吧精选下午茶，含饮品和甜点', 0.00, '/images/product/afternoon_tea.png', 2, 1),
                                                                                                  ('6', '3', '行李寄存服务', '前台行李寄存，安全便捷', 0.00, '/images/product/luggage.png', 1, 1);
-- 商品规格
INSERT INTO hotel_product_spec (id, product_id, spec_name, spec_value, stock, price, sort) VALUES
                                                                                               ('1', '1', '成人', '成人自助早餐', 100, 0.00, 1),
                                                                                               ('2', '1', '儿童', '儿童自助早餐（12岁以下）', 50, 0.00, 2),
                                                                                               ('3', '2', '单人', '单人SPA 60分钟', 30, 0.00, 1),
                                                                                               ('4', '2', '双人', '双人SPA 90分钟', 20, 0.00, 2),
                                                                                               ('5', '3', '单次', '健身房单次使用', 200, 0.00, 1),
                                                                                               ('6', '3', '日卡', '健身房全天使用', 50, 0.00, 2),
                                                                                               ('7', '4', '半天', '会议室使用4小时', 10, 0.00, 1),
                                                                                               ('8', '4', '全天', '会议室使用8小时', 5, 0.00, 2),
                                                                                               ('9', '5', '单人', '单人下午茶套餐', 40, 0.00, 1),
                                                                                               ('10', '5', '双人', '双人下午茶套餐', 25, 0.00, 2),
                                                                                               ('11', '6', '小件', '行李尺寸≤28寸', 100, 0.00, 1),
                                                                                               ('12', '6', '大件', '行李尺寸>28寸', 50, 0.00, 2);
