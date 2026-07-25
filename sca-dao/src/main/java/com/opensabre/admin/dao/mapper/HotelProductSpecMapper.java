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

    /**
     * TCC Try: 冻结库存（stock减少，frozen_stock增加）
     */
    int freezeStock(@Param("specId") String specId, @Param("quantity") int quantity);

    /**
     * TCC Confirm: 确认扣减（frozen_stock减少）
     */
    int confirmFrozen(@Param("specId") String specId, @Param("quantity") int quantity);

    /**
     * TCC Cancel: 释放冻结（stock恢复，frozen_stock减少）
     */
    int cancelFrozen(@Param("specId") String specId, @Param("quantity") int quantity);
}
