package com.opensabre.admin.web.controller.hotel.response;

import com.opensabre.admin.dao.entity.po.HotelProduct;
import com.opensabre.admin.dao.entity.po.HotelProductSpec;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 商品详情响应（含规格列表）
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "商品详情响应")
public class ProductDetailResponse {

    @Schema(description = "商品信息")
    private HotelProduct product;

    @Schema(description = "规格列表")
    private List<HotelProductSpec> specs;
}
