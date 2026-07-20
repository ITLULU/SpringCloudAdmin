package com.opensabre.admin.dao.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.opensabre.admin.dao.entity.po.HotelTrip;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDate;
import java.util.List;

/**
 * 用户行程 Mapper
 */
@Mapper
public interface HotelTripMapper extends BaseMapper<HotelTrip> {

    /**
     * 查询用户在某酒店是否有时间重叠的有效行程
     */
    List<HotelTrip> selectOverlappingTrips(@Param("userId") String userId,
                                           @Param("hotelId") String hotelId,
                                           @Param("checkInDate") LocalDate checkInDate,
                                           @Param("checkOutDate") LocalDate checkOutDate);

    /**
     * 查询用户的有效行程列表
     */
    List<HotelTrip> selectByUserId(@Param("userId") String userId);

    /**
     * 查询用户在某酒店当前是否有有效入住（今天日期在入住范围内）
     */
    HotelTrip selectActiveTrip(@Param("userId") String userId, @Param("hotelId") String hotelId);
}
