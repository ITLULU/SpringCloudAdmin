-- =============================================
-- SpringCloudAdmin 酒店订单系统 DDL
-- 数据库：MySQL 8.x
-- =============================================

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
-- 2. 酒店商品表
-- ----------------------------
DROP TABLE IF EXISTS hotel_product;
CREATE TABLE hotel_product (
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
-- 3. 商品规格表（独立库存）
-- ----------------------------
DROP TABLE IF EXISTS hotel_product_spec;
CREATE TABLE hotel_product_spec (
    id          varchar(32)   NOT NULL COMMENT '主键ID（雪花算法）',
    product_id  varchar(32)   NOT NULL COMMENT '所属商品ID',
    spec_name   varchar(64)   NOT NULL COMMENT '规格名称（如：大份/小份）',
    spec_value  varchar(128)  DEFAULT '' COMMENT '规格值描述',
    stock       int           DEFAULT 0 COMMENT '库存数量',
    price       decimal(10,2) DEFAULT 0.00 COMMENT '规格价格（当前均为0）',
    sort        int           DEFAULT 0 COMMENT '排序',
    created_by  varchar(32)   DEFAULT 'system' COMMENT '创建人',
    created_time datetime     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_by  varchar(32)   DEFAULT 'system' COMMENT '更新人',
    updated_time datetime     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    KEY idx_product_id (product_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='商品规格表';

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

-- ----------------------------
-- 5. 订单表
-- ----------------------------
DROP TABLE IF EXISTS hotel_order;
CREATE TABLE hotel_order (
    id          varchar(32) NOT NULL COMMENT '主键ID（雪花算法）',
    user_id     varchar(32) NOT NULL COMMENT '用户ID',
    hotel_id    varchar(32) NOT NULL COMMENT '酒店ID',
    trip_id     varchar(32) NOT NULL COMMENT '关联行程ID',
    status      tinyint     DEFAULT 1 COMMENT '状态：0-已取消 1-已完成',
    created_by  varchar(32) DEFAULT 'system' COMMENT '创建人',
    created_time datetime   DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_by  varchar(32) DEFAULT 'system' COMMENT '更新人',
    updated_time datetime   DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    KEY idx_user_id (user_id),
    KEY idx_trip_id (trip_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='订单表';

-- ----------------------------
-- 6. 订单明细表
-- ----------------------------
DROP TABLE IF EXISTS hotel_order_item;
CREATE TABLE hotel_order_item (
    id           varchar(32)   NOT NULL COMMENT '主键ID（雪花算法）',
    order_id     varchar(32)   NOT NULL COMMENT '订单ID',
    product_id   varchar(32)   NOT NULL COMMENT '商品ID',
    spec_id      varchar(32)   NOT NULL COMMENT '规格ID',
    product_name varchar(128)  DEFAULT '' COMMENT '商品名（冗余）',
    spec_name    varchar(64)   DEFAULT '' COMMENT '规格名（冗余）',
    quantity     int           NOT NULL COMMENT '数量',
    price        decimal(10,2) DEFAULT 0.00 COMMENT '下单时单价',
    created_by   varchar(32)   DEFAULT 'system' COMMENT '创建人',
    created_time datetime      DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_by   varchar(32)   DEFAULT 'system' COMMENT '更新人',
    updated_time datetime      DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    KEY idx_order_id (order_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='订单明细表';


-- =============================================
-- 初始数据
-- =============================================

-- 酒店
INSERT INTO hotel (id, brand, name, address, phone, logo, description, status) VALUES
('1', '万豪', '万豪大酒店', '北京市朝阳区建国路88号', '010-88888888', '/images/hotel/marriott.png', '五星级豪华酒店，位于市中心繁华地段', 1),
('2', '希尔顿', '希尔顿花园酒店', '上海市浦东新区陆家嘴环路100号', '021-66666666', '/images/hotel/hilton.png', '国际品牌商务酒店，毗邻金融中心', 1),
('3', '如家', '如家精选酒店', '广州市天河区天河路385号', '020-33333333', '/images/hotel/homeinn.png', '经济型连锁酒店，交通便利', 1);

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
