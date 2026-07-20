package com.opensabre.admin.dao.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.opensabre.admin.dao.entity.po.HotelProductSpec;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 商品规格 Mapper
 */
@Mapper
public interface HotelProductSpecMapper extends BaseMapper<HotelProductSpec> {

    /**
     * 查询某商品的所有规格列表
     */
    List<HotelProductSpec> selectByProductId(@Param("productId") String productId);

    /**
     * 扣减库存（乐观锁，stock >= quantity 才更新）
     */
    int deductStock(@Param("specId") String specId, @Param("quantity") int quantity);

    /**
     * 归还库存
     */
    int restoreStock(@Param("specId") String specId, @Param("quantity") int quantity);
}
