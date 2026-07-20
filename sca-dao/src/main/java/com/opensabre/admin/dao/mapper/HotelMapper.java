package com.opensabre.admin.dao.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.opensabre.admin.dao.entity.po.Hotel;
import org.apache.ibatis.annotations.Mapper;

/**
 * 酒店 Mapper
 */
@Mapper
public interface HotelMapper extends BaseMapper<Hotel> {
}
