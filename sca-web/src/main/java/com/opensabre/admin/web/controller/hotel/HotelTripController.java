package com.opensabre.admin.web.controller.hotel;

import com.opensabre.admin.common.entity.Result;
import com.opensabre.admin.common.util.UserContextHolder;
import com.opensabre.admin.dao.entity.po.Hotel;
import com.opensabre.admin.dao.entity.po.HotelTrip;
import com.opensabre.admin.dao.entity.po.SysUser;
import com.opensabre.admin.dao.mapper.HotelMapper;
import com.opensabre.admin.dao.mapper.HotelTripMapper;
import com.opensabre.admin.dao.mapper.SysUserMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
    public Result<Object> create(@RequestBody Map<String, String> request) {
        String username = UserContextHolder.getInstance().getUsername();
        SysUser user = sysUserMapper.selectByUsername(username);
        if (user == null) {
            return Result.fail("用户不存在");
        }

        String hotelId = request.get("hotelId");
        String checkInStr = request.get("checkInDate");
        String checkOutStr = request.get("checkOutDate");

        if (hotelId == null || checkInStr == null || checkOutStr == null) {
            return Result.fail("参数不完整");
        }

        LocalDate checkInDate = LocalDate.parse(checkInStr);
        LocalDate checkOutDate = LocalDate.parse(checkOutStr);

        // 校验日期
        if (checkOutDate.isBefore(checkInDate) || checkOutDate.isEqual(checkInDate)) {
            return Result.fail("离店日期必须晚于入住日期");
        }

        // 校验酒店是否存在
        Hotel hotel = hotelMapper.selectById(hotelId);
        if (hotel == null || hotel.getStatus() != 1) {
            return Result.fail("酒店不存在或已停业");
        }

        // 校验时间段是否重叠
        List<HotelTrip> overlapping = hotelTripMapper.selectOverlappingTrips(
                user.getId(), hotelId, checkInDate, checkOutDate);
        if (!overlapping.isEmpty()) {
            return Result.fail("该时间段已有入住行程，不可重叠");
        }

        // 创建行程
        HotelTrip trip = new HotelTrip();
        trip.setUserId(user.getId());
        trip.setHotelId(hotelId);
        trip.setCheckInDate(checkInDate);
        trip.setCheckOutDate(checkOutDate);
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
        String username = UserContextHolder.getInstance().getUsername();
        SysUser user = sysUserMapper.selectByUsername(username);
        if (user == null) {
            return Result.fail("用户不存在");
        }

        List<HotelTrip> trips = hotelTripMapper.selectByUserId(user.getId());
        // 附加酒店信息
        List<Map<String, Object>> result = trips.stream().map(trip -> {
            Map<String, Object> map = new HashMap<>();
            map.put("trip", trip);
            Hotel hotel = hotelMapper.selectById(trip.getHotelId());
            map.put("hotel", hotel);
            return map;
        }).toList();

        return Result.success(result);
    }

    /**
     * 取消行程
     */
    @Operation(summary = "取消行程", description = "取消指定行程")
    @PutMapping("/cancel/{id}")
    public Result<Object> cancel(@PathVariable String id) {
        String username = UserContextHolder.getInstance().getUsername();
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
        String username = UserContextHolder.getInstance().getUsername();
        SysUser user = sysUserMapper.selectByUsername(username);
        if (user == null) {
            return Result.fail("用户不存在");
        }

        HotelTrip activeTrip = hotelTripMapper.selectActiveTrip(user.getId(), hotelId);
        Map<String, Object> data = new HashMap<>();
        data.put("checkedIn", activeTrip != null);
        data.put("trip", activeTrip);
        return Result.success(data);
    }
}
