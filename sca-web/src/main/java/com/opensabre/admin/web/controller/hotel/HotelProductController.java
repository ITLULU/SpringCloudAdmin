package com.opensabre.admin.web.controller.hotel;

import com.opensabre.admin.common.entity.Result;
import com.opensabre.admin.dao.entity.po.HotelProduct;
import com.opensabre.admin.dao.entity.po.HotelProductSpec;
import com.opensabre.admin.dao.mapper.HotelProductMapper;
import com.opensabre.admin.dao.mapper.HotelProductSpecMapper;
import com.opensabre.admin.web.controller.hotel.response.ProductDetailResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 酒店商品Controller - 提供商品列表、详情（含规格）等接口
 */
@RestController
@RequestMapping("/api/hotel/product")
@Tag(name = "酒店商品", description = "商品列表、详情、规格")
public class HotelProductController {

    @Autowired
    private HotelProductMapper hotelProductMapper;

    @Autowired
    private HotelProductSpecMapper hotelProductSpecMapper;

    /**
     * 某酒店的商品列表
     */
    @Operation(summary = "商品列表", description = "查询某酒店的上架商品")
    @GetMapping("/list")
    public Result<Object> list(@RequestParam String hotelId) {
        List<HotelProduct> products = hotelProductMapper.selectByHotelId(hotelId);
        return Result.success(products);
    }

    /**
     * 商品详情（含规格列表）
     */
    @Operation(summary = "商品详情", description = "查询商品详情及其所有规格")
    @GetMapping("/{id}")
    public Result<Object> detail(@PathVariable String id) {
        HotelProduct product = hotelProductMapper.selectById(id);
        if (product == null || product.getStatus() != 1) {
            return Result.fail("商品不存在或已下架");
        }
        List<HotelProductSpec> specs = hotelProductSpecMapper.selectByProductId(id);
        return Result.success(new ProductDetailResponse(product, specs));
    }
}
