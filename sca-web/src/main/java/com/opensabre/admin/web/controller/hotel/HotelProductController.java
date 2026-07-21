package com.opensabre.admin.web.controller.hotel;

import com.opensabre.admin.common.entity.Result;
import com.opensabre.admin.dao.entity.po.HotelProduct;
import com.opensabre.admin.rpc.client.ProductFeignClient;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 酒店商品Controller - 提供商品列表、详情（含规格）等接口
 * <p>
 * 商品数据存储在 sca-stock 服务，通过 Feign 调用获取
 */
@RestController
@RequestMapping("/api/hotel/product")
@Tag(name = "酒店商品", description = "商品列表、详情、规格")
public class HotelProductController {

    @Autowired
    private ProductFeignClient productFeignClient;

    /**
     * 某酒店的商品列表
     */
    @Operation(summary = "商品列表", description = "查询某酒店的上架商品")
    @GetMapping("/list")
    public Result<List<HotelProduct>> list(@RequestParam String hotelId) {
        return productFeignClient.listByHotelId(hotelId);
    }

    /**
     * 商品详情（含规格列表）
     */
    @Operation(summary = "商品详情", description = "查询商品详情及其所有规格")
    @GetMapping("/{id}")
    public Result<Map<String, Object>> detail(@PathVariable String id) {
        return productFeignClient.detail(id);
    }
}
