package com.opensabre.admin.stock.controller;

import com.opensabre.admin.common.entity.Result;
import com.opensabre.admin.dao.entity.po.HotelProduct;
import com.opensabre.admin.dao.entity.po.HotelProductSpec;
import com.opensabre.admin.dao.mapper.HotelProductMapper;
import com.opensabre.admin.dao.mapper.HotelProductSpecMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 商品服务 Controller - 提供商品查询接口（供 Feign 内部调用）
 */
@Slf4j
@RestController
@RequestMapping("/inner/product")
@Tag(name = "商品服务(内部)", description = "商品查询，供微服务间 Feign 调用")
public class ProductController {

    @Autowired
    private HotelProductMapper hotelProductMapper;

    @Autowired
    private HotelProductSpecMapper hotelProductSpecMapper;

    /**
     * 某酒店的上架商品列表
     */
    @Operation(summary = "商品列表", description = "查询某酒店的上架商品")
    @GetMapping("/list")
    public Result<List<HotelProduct>> listByHotelId(@RequestParam String hotelId) {
        List<HotelProduct> products = hotelProductMapper.selectByHotelId(hotelId);
        return Result.success(products);
    }

    /**
     * 商品详情（含规格列表）
     */
    @Operation(summary = "商品详情", description = "查询商品详情及其所有规格")
    @GetMapping("/{id}")
    public Result<Map<String, Object>> detail(@PathVariable String id) {
        HotelProduct product = hotelProductMapper.selectById(id);
        if (product == null || product.getStatus() != 1) {
            return Result.fail("商品不存在或已下架");
        }
        List<HotelProductSpec> specs = hotelProductSpecMapper.selectByProductId(id);
        Map<String, Object> data = new HashMap<>();
        data.put("product", product);
        data.put("specs", specs);
        return Result.success(data);
    }

    /**
     * 查询商品基本信息（供下单校验用）
     */
    @Operation(summary = "商品信息", description = "根据ID查询商品基本信息")
    @GetMapping("/info/{id}")
    public Result<HotelProduct> productInfo(@PathVariable String id) {
        HotelProduct product = hotelProductMapper.selectById(id);
        if (product == null) {
            return Result.fail("商品不存在");
        }
        return Result.success(product);
    }

    /**
     * 查询规格信息（供下单获取价格用）
     */
    @Operation(summary = "规格信息", description = "根据规格ID查询规格详情")
    @GetMapping("/spec/{specId}")
    public Result<HotelProductSpec> specInfo(@PathVariable String specId) {
        HotelProductSpec spec = hotelProductSpecMapper.selectById(specId);
        if (spec == null) {
            return Result.fail("规格不存在");
        }
        return Result.success(spec);
    }
}
