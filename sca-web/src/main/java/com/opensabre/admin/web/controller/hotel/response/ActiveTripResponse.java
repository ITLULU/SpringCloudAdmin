package com.opensabre.admin.web.controller.hotel.response;

import com.opensabre.admin.dao.entity.po.HotelTrip;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 入住状态检查响应
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "入住状态检查响应")
public class ActiveTripResponse {

    @Schema(description = "是否已入住")
    private Boolean checkedIn;

    @Schema(description = "当前有效行程（未入住时为null）")
    private HotelTrip trip;
}
