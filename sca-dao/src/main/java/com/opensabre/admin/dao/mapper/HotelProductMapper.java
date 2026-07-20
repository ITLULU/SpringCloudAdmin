package com.opensabre.admin.dao.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.opensabre.admin.dao.entity.po.HotelProduct;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 酒店商品 Mapper
 */
@Mapper
public interface HotelProductMapper extends BaseMapper<HotelProduct> {

    /**
     * 查询某酒店的上架商品列表
     */
    List<HotelProduct> selectByHotelId(@Param("hotelId") String hotelId);
}
