# 酒店订单系统实现计划

## 一、数据库表设计（Database/ddl/hotel_schema.sql）

### 1. hotel（酒店表）
| 字段 | 类型 | 说明 |
|------|------|------|
| id | varchar(32) | 主键 |
| brand | varchar(64) | 酒店品牌 |
| name | varchar(128) | 酒店名称 |
| address | varchar(256) | 酒店地址 |
| phone | varchar(20) | 联系电话 |
| logo | varchar(256) | 酒店logo URL |
| description | text | 酒店简介 |
| status | tinyint | 状态 0-停业 1-营业 |

### 2. hotel_product（酒店商品表）
| 字段 | 类型 | 说明 |
|------|------|------|
| id | varchar(32) | 主键 |
| hotel_id | varchar(32) | 所属酒店 |
| name | varchar(128) | 商品名称 |
| description | text | 商品描述 |
| price | decimal(10,2) | 价格（当前均为0） |
| cover_image | varchar(256) | 封面图 |
| images | varchar(1024) | 商品图片（逗号分隔） |
| sort | int | 排序 |
| status | tinyint | 状态 0-下架 1-上架 |

### 3. hotel_product_spec（商品规格表，独立库存）
| 字段 | 类型 | 说明 |
|------|------|------|
| id | varchar(32) | 主键 |
| product_id | varchar(32) | 所属商品 |
| spec_name | varchar(64) | 规格名称（如：大份/小份） |
| spec_value | varchar(128) | 规格值描述 |
| stock | int | 库存数量 |
| price | decimal(10,2) | 规格价格（当前均为0） |
| sort | int | 排序 |

### 4. hotel_trip（用户行程/入住表）
| 字段 | 类型 | 说明 |
|------|------|------|
| id | varchar(32) | 主键 |
| user_id | varchar(32) | 用户ID |
| hotel_id | varchar(32) | 酒店ID |
| check_in_date | date | 入住日期 |
| check_out_date | date | 离店日期 |
| status | tinyint | 状态 0-已取消 1-已入住 |

约束：同一用户同一酒店，入住时间段不可重叠。

### 5. hotel_order（订单表）
| 字段 | 类型 | 说明 |
|------|------|------|
| id | varchar(32) | 主键 |
| user_id | varchar(32) | 用户ID |
| hotel_id | varchar(32) | 酒店ID |
| trip_id | varchar(32) | 关联行程ID |
| status | tinyint | 状态 0-已取消 1-已完成 |

### 6. hotel_order_item（订单明细表）
| 字段 | 类型 | 说明 |
|------|------|------|
| id | varchar(32) | 主键 |
| order_id | varchar(32) | 订单ID |
| product_id | varchar(32) | 商品ID |
| spec_id | varchar(32) | 规格ID |
| product_name | varchar(128) | 商品名（冗余） |
| spec_name | varchar(64) | 规格名（冗余） |
| quantity | int | 数量 |
| price | decimal(10,2) | 下单时单价 |

---

## 二、后端实现（SpringCloudAdmin）

### DAO层（sca-dao）
- PO实体类：`Hotel.java`, `HotelProduct.java`, `HotelProductSpec.java`, `HotelTrip.java`, `HotelOrder.java`, `HotelOrderItem.java`
- Mapper接口 + XML：`HotelMapper`, `HotelProductMapper`, `HotelProductSpecMapper`, `HotelTripMapper`, `HotelOrderMapper`, `HotelOrderItemMapper`

### Controller层（sca-web）
新增 `controller/hotel/` 包，包含：

- **HotelController** - `/api/hotel`
  - `GET /list` - 酒店列表（分页）
  - `GET /{id}` - 酒店详情

- **HotelProductController** - `/api/hotel/product`
  - `GET /list?hotelId=` - 某酒店商品列表
  - `GET /{id}` - 商品详情（含规格列表）

- **HotelTripController** - `/api/hotel/trip`
  - `POST /` - 创建行程（0元入住，校验时间段不重叠）
  - `GET /my` - 我的行程列表
  - `PUT /cancel/{id}` - 取消行程

- **HotelOrderController** - `/api/hotel/order`
  - `POST /` - 下单（校验入住状态 + 扣减规格库存，事务保证）
  - `GET /my?tripId=` - 我的订单列表
  - `PUT /cancel/{id}` - 取消订单（归还库存，事务保证）

### 安全配置
- 在 `application.yml` 的 `permit-urls` 中添加酒店浏览相关GET接口（无需登录可浏览）
- 下单/行程操作需要登录

---

## 三、前端实现（sc-order-front）

### 项目初始化
- Vue 3 + Vite + TypeScript
- Vant 4 UI 组件库
- Axios 请求封装（复用认证逻辑，baseURL 指向后端）
- Vue Router 路由配置

### 页面结构
```
sc-order-front/src/views/
├── login/index.vue          -- 登录页（复用注册跳转）
├── hotel/
│   ├── list.vue             -- 酒店列表页（卡片式）
│   └── detail.vue           -- 酒店详情页（品牌/logo/地址/电话 + 商品入口）
├── product/
│   ├── list.vue             -- 商品列表页（某酒店下，需入住才可下单标识）
│   └── detail.vue           -- 商品详情页（规格选择 + 库存展示 + 下单按钮）
├── trip/
│   ├── list.vue             -- 我的行程列表
│   └── create.vue           -- 创建行程（选日期）
├── order/
│   ├── list.vue             -- 我的订单列表
│   └── detail.vue           -- 订单详情页（可取消）
└── user/
    └── profile.vue          -- 个人中心
```

### 核心交互逻辑
1. **浏览模式**：未入住酒店时，可查看商品但下单按钮禁用，提示"请先入住"
2. **入住校验**：创建行程时前端选日期，后端校验时间段不重叠
3. **下单流程**：选规格 → 确认数量 → 提交 → 库存扣减
4. **取消订单**：确认取消 → 库存归还

---

## 四、实现顺序

1. 数据库DDL脚本
2. 后端PO实体 + Mapper
3. 后端Controller（酒店 → 商品 → 行程 → 订单）
4. 前端项目初始化（Vite + Vant + Router）
5. 前端页面开发（酒店列表/详情 → 商品浏览 → 行程管理 → 订单流程）
