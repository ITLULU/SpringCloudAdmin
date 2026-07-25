package com.opensabre.admin.dao.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.opensabre.admin.dao.entity.po.HotelOrder;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 订单 Mapper
 */
@Mapper
public interface HotelOrderMapper extends BaseMapper<HotelOrder> {

    /**
     * 查询用户某行程下的订单列表
     */
    List<HotelOrder> selectByTripId(@Param("tripId") String tripId);

    /**
     * 查询用户所有订单
     */
    List<HotelOrder> selectByUserId(@Param("userId") String userId);

    /**
     * TCC Confirm: 根据 xid 确认订单（status 2->1）
     */
    int confirmOrderByXid(@Param("xid") String xid);

    /**
     * TCC Cancel: 根据 xid 取消订单（status 2->0）
     */
    int cancelOrderByXid(@Param("xid") String xid);
}
