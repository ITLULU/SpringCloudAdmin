package com.opensabre.admin.web.controller.hotel;

import com.opensabre.admin.common.entity.Result;
import com.opensabre.admin.common.util.SecurityUtils;
import com.opensabre.admin.dao.entity.po.Hotel;
import com.opensabre.admin.dao.entity.po.HotelTrip;
import com.opensabre.admin.dao.entity.po.SysUser;
import com.opensabre.admin.dao.mapper.HotelMapper;
import com.opensabre.admin.dao.mapper.HotelTripMapper;
import com.opensabre.admin.dao.mapper.SysUserMapper;
import com.opensabre.admin.web.controller.hotel.request.CreateTripRequest;
import com.opensabre.admin.web.controller.hotel.response.ActiveTripResponse;
import com.opensabre.admin.web.controller.hotel.response.TripWithHotelResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 酒店行程Controller - 创建行程、查看行程、取消行程
 */
@RestController
@RequestMapping("/api/hotel/trip")
@Tag(name = "酒店行程", description = "入住、行程管理")
public class HotelTripController {

    @Autowired
    private HotelTripMapper hotelTripMapper;

    @Autowired
    private HotelMapper hotelMapper;

    @Autowired
    private SysUserMapper sysUserMapper;

    /**
     * 创建行程（0元入住）
     */
    @Operation(summary = "创建行程", description = "0元入住，校验时间段不重叠")
    @PostMapping
    public Result<HotelTrip> create(@Valid @RequestBody CreateTripRequest request) {
        String username = SecurityUtils.getCurrentUsername();
        SysUser user = sysUserMapper.selectByUsername(username);
        if (user == null) {
            return Result.fail("用户不存在");
        }

        // 校验日期
        if (!request.getCheckOutDate().isAfter(request.getCheckInDate())) {
            return Result.fail("离店日期必须晚于入住日期");
        }

        // 校验酒店是否存在
        Hotel hotel = hotelMapper.selectById(request.getHotelId());
        if (hotel == null || hotel.getStatus() != 1) {
            return Result.fail("酒店不存在或已停业");
        }

        // 校验时间段是否重叠
        List<HotelTrip> overlapping = hotelTripMapper.selectOverlappingTrips(
                user.getId(), request.getHotelId(), request.getCheckInDate(), request.getCheckOutDate());
        if (!overlapping.isEmpty()) {
            return Result.fail("该时间段已有入住行程，不可重叠");
        }

        // 创建行程
        HotelTrip trip = new HotelTrip();
        trip.setUserId(user.getId());
        trip.setHotelId(request.getHotelId());
        trip.setCheckInDate(request.getCheckInDate());
        trip.setCheckOutDate(request.getCheckOutDate());
        trip.setStatus(1);
        hotelTripMapper.insert(trip);

        return Result.success(trip);
    }

    /**
     * 我的行程列表
     */
    @Operation(summary = "我的行程", description = "查询当前用户的所有行程")
    @GetMapping("/my")
    public Result<Object> myTrips() {
        String username = SecurityUtils.getCurrentUsername();
        SysUser user = sysUserMapper.selectByUsername(username);
        if (user == null) {
            return Result.fail("用户不存在");
        }

        List<HotelTrip> trips = hotelTripMapper.selectByUserId(user.getId());
        List<TripWithHotelResponse> result = trips.stream()
                .map(trip -> {
                    Hotel hotel = hotelMapper.selectById(trip.getHotelId());
                    return new TripWithHotelResponse(trip, hotel);
                })
                .toList();

        return Result.success(result);
    }

    /**
     * 取消行程
     */
    @Operation(summary = "取消行程", description = "取消指定行程")
    @PutMapping("/cancel/{id}")
    public Result<Object> cancel(@PathVariable String id) {
        String username = SecurityUtils.getCurrentUsername();
        SysUser user = sysUserMapper.selectByUsername(username);
        if (user == null) {
            return Result.fail("用户不存在");
        }

        HotelTrip trip = hotelTripMapper.selectById(id);
        if (trip == null || !trip.getUserId().equals(user.getId())) {
            return Result.fail("行程不存在");
        }
        if (trip.getStatus() == 0) {
            return Result.fail("行程已取消");
        }

        trip.setStatus(0);
        hotelTripMapper.updateById(trip);
        return Result.success();
    }

    /**
     * 检查用户当前是否在某酒店入住中
     */
    @Operation(summary = "检查入住状态", description = "检查当前用户在某酒店是否有有效入住")
    @GetMapping("/active")
    public Result<Object> checkActive(@RequestParam String hotelId) {
        String username = SecurityUtils.getCurrentUsername();
        SysUser user = sysUserMapper.selectByUsername(username);
        if (user == null) {
            return Result.fail("用户不存在");
        }

        HotelTrip activeTrip = hotelTripMapper.selectActiveTrip(user.getId(), hotelId);
        return Result.success(new ActiveTripResponse(activeTrip != null, activeTrip));
    }
}
