package com.opensabre.admin.es.document;

import com.opensabre.admin.es.repository.BaseDocument;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;
import org.springframework.data.elasticsearch.annotations.Setting;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 订单 ES 文档
 * <p>
 * 索引名：order_index
 * 用于订单全文检索、用户订单查询、行程订单查询等场景。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Document(indexName = "order_index")
@Setting(settingPath = "es/order-index-setting.json")
public class OrderDocument extends BaseDocument {

    private static final long serialVersionUID = 1L;

    /** 订单ID */
    @Field(type = FieldType.Keyword)
    private String orderId;

    /** 用户ID */
    @Field(type = FieldType.Keyword)
    private String userId;

    /** 酒店ID */
    @Field(type = FieldType.Keyword)
    private String hotelId;

    /** 关联行程ID */
    @Field(type = FieldType.Keyword)
    private String tripId;

    /** 订单状态：0-已取消 1-已完成 2-待确认(TCC) */
    @Field(type = FieldType.Integer)
    private Integer status;

    /** 订单总金额 */
    @Field(type = FieldType.Scaled_Float, scalingFactor = 100)
    private BigDecimal totalAmount;

    /** 订单创建时间 */
    @Field(type = FieldType.Date, format = {}, pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime createdTime;

    /** 订单更新时间 */
    @Field(type = FieldType.Date, format = {}, pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime updatedTime;

    /** 订单明细 */
    @Field(type = FieldType.Nested)
    private List<OrderItemDocument> items;

    /**
     * 订单明细文档
     */
    @Data
    public static class OrderItemDocument {

        /** 明细ID */
        @Field(type = FieldType.Keyword)
        private String itemId;

        /** 商品ID */
        @Field(type = FieldType.Keyword)
        private String productId;

        /** 规格ID */
        @Field(type = FieldType.Keyword)
        private String specId;

        /** 商品名（支持分词检索） */
        @Field(type = FieldType.Text, analyzer = "ik_max_word", searchAnalyzer = "ik_smart")
        private String productName;

        /** 规格名 */
        @Field(type = FieldType.Keyword)
        private String specName;

        /** 数量 */
        @Field(type = FieldType.Integer)
        private Integer quantity;

        /** 下单时单价 */
        @Field(type = FieldType.Scaled_Float, scalingFactor = 100)
        private BigDecimal price;
    }
}
