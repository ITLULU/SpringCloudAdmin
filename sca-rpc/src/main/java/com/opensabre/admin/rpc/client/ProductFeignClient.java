package com.opensabre.admin.rpc.client;

import com.opensabre.admin.common.entity.Result;
import com.opensabre.admin.dao.entity.po.HotelProduct;
import com.opensabre.admin.dao.entity.po.HotelProductSpec;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;
import java.util.Map;

/**
 * 商品服务 Feign 客户端 - 调用 sca-stock 微服务的商品查询接口
 */
@FeignClient(name = "sca-stock", path = "/inner/product")
public interface ProductFeignClient {

    /**
     * 某酒店的上架商品列表
     */
    @GetMapping("/list")
    Result<List<HotelProduct>> listByHotelId(@RequestParam("hotelId") String hotelId);

    /**
     * 商品详情（含规格列表）
     */
    @GetMapping("/{id}")
    Result<Map<String, Object>> detail(@PathVariable("id") String id);

    /**
     * 商品基本信息（供下单校验用）
     */
    @GetMapping("/info/{id}")
    Result<HotelProduct> productInfo(@PathVariable("id") String id);

    /**
     * 规格信息（供下单获取价格用）
     */
    @GetMapping("/spec/{specId}")
    Result<HotelProductSpec> specInfo(@PathVariable("specId") String specId);
}
