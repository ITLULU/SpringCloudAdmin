package com.opensabre.admin.web.controller.hotel;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.opensabre.admin.common.entity.Result;
import com.opensabre.admin.dao.entity.po.Hotel;
import com.opensabre.admin.dao.mapper.HotelMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * 酒店Controller - 提供酒店列表、详情等接口
 */
@RestController
@RequestMapping("/api/hotel")
@Tag(name = "酒店管理", description = "酒店列表、详情")
public class HotelController {

    @Autowired
    private HotelMapper hotelMapper;

    /**
     * 酒店列表（分页）
     */
    @Operation(summary = "酒店列表", description = "分页查询营业中的酒店")
    @GetMapping("/list")
    public Result<Object> list(@RequestParam(defaultValue = "1") Integer pageNum,
                               @RequestParam(defaultValue = "10") Integer pageSize,
                               @RequestParam(required = false) String keyword) {
        Page<Hotel> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<Hotel> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Hotel::getStatus, 1);
        if (keyword != null && !keyword.isBlank()) {
            wrapper.and(w -> w.like(Hotel::getName, keyword)
                    .or().like(Hotel::getBrand, keyword)
                    .or().like(Hotel::getAddress, keyword));
        }
        wrapper.orderByDesc(Hotel::getCreatedTime);
        Page<Hotel> result = hotelMapper.selectPage(page, wrapper);
        return Result.success(result);
    }

    /**
     * 酒店详情
     */
    @Operation(summary = "酒店详情", description = "根据ID查询酒店详细信息")
    @GetMapping("/{id}")
    public Result<Object> detail(@PathVariable String id) {
        Hotel hotel = hotelMapper.selectById(id);
        if (hotel == null || hotel.getStatus() != 1) {
            return Result.fail("酒店不存在或已停业");
        }
        return Result.success(hotel);
    }
}
