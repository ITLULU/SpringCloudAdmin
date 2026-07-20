package com.opensabre.admin.web.controller.hotel.response;

import com.opensabre.admin.dao.entity.po.Hotel;
import com.opensabre.admin.dao.entity.po.HotelTrip;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 行程详情响应（行程 + 酒店信息）
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "行程详情响应")
public class TripWithHotelResponse {

    @Schema(description = "行程信息")
    private HotelTrip trip;

    @Schema(description = "酒店信息")
    private Hotel hotel;
}
