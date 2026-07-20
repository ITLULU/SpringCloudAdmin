package com.opensabre.admin.dao.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.opensabre.admin.dao.entity.po.HotelOrderItem;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 订单明细 Mapper
 */
@Mapper
public interface HotelOrderItemMapper extends BaseMapper<HotelOrderItem> {

    /**
     * 查询某订单的所有明细
     */
    List<HotelOrderItem> selectByOrderId(@Param("orderId") String orderId);
}
