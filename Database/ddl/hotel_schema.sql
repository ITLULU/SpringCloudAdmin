-- =============================================
-- SpringCloudAdmin 酒店订单系统 DDL
-- 数据库：MySQL 8.x
-- =============================================
CREATE DATABASE IF NOT EXISTS sca_db DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
use sca_db;
SET NAMES utf8mb4;
SET CHARACTER SET utf8mb4;

-- ----------------------------
-- 1. 酒店表
-- ----------------------------
DROP TABLE IF EXISTS hotel;
CREATE TABLE hotel (
    id          varchar(32)  NOT NULL COMMENT '主键ID（雪花算法）',
    brand       varchar(64)  DEFAULT '' COMMENT '酒店品牌',
    name        varchar(128) NOT NULL COMMENT '酒店名称',
    address     varchar(256) DEFAULT '' COMMENT '酒店地址',
    phone       varchar(20)  DEFAULT '' COMMENT '联系电话',
    logo        varchar(256) DEFAULT '' COMMENT '酒店logo URL',
    description text         COMMENT '酒店简介',
    status      tinyint      DEFAULT 1 COMMENT '状态：0-停业 1-营业',
    created_by  varchar(32)  DEFAULT 'system' COMMENT '创建人',
    created_time datetime    DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_by  varchar(32)  DEFAULT 'system' COMMENT '更新人',
    updated_time datetime    DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='酒店表';


-- ----------------------------
-- 4. 用户行程/入住表
-- ----------------------------
DROP TABLE IF EXISTS hotel_trip;
CREATE TABLE hotel_trip (
    id             varchar(32) NOT NULL COMMENT '主键ID（雪花算法）',
    user_id        varchar(32) NOT NULL COMMENT '用户ID',
    hotel_id       varchar(32) NOT NULL COMMENT '酒店ID',
    check_in_date  date        NOT NULL COMMENT '入住日期',
    check_out_date date        NOT NULL COMMENT '离店日期',
    status         tinyint     DEFAULT 1 COMMENT '状态：0-已取消 1-已入住',
    created_by     varchar(32) DEFAULT 'system' COMMENT '创建人',
    created_time   datetime    DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_by     varchar(32) DEFAULT 'system' COMMENT '更新人',
    updated_time   datetime    DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    KEY idx_user_id (user_id),
    KEY idx_hotel_id (hotel_id),
    KEY idx_user_hotel (user_id, hotel_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='用户行程/入住表';



-- =============================================
-- 初始数据
-- =============================================

-- 酒店
INSERT INTO hotel (id, brand, name, address, phone, logo, description, status) VALUES
('1', '万豪', '万豪大酒店', '北京市朝阳区建国路88号', '010-88888888', '/images/hotel/marriott.png', '五星级豪华酒店，位于市中心繁华地段', 1),
('2', '希尔顿', '希尔顿花园酒店', '上海市浦东新区陆家嘴环路100号', '021-66666666', '/images/hotel/hilton.png', '国际品牌商务酒店，毗邻金融中心', 1),
('3', '如家', '如家精选酒店', '广州市天河区天河路385号', '020-33333333', '/images/hotel/homeinn.png', '经济型连锁酒店，交通便利', 1);


