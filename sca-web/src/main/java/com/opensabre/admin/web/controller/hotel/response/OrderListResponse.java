package com.opensabre.admin.web.controller.hotel.response;

import com.opensabre.admin.dao.entity.po.HotelOrder;
import com.opensabre.admin.dao.entity.po.HotelOrderItem;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 订单列表响应
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "订单列表响应")
public class OrderListResponse {

    @Schema(description = "订单信息")
    private HotelOrder order;

    @Schema(description = "订单明细")
    private List<HotelOrderItem> items;

    @Schema(description = "酒店名称")
    private String hotelName;
}
