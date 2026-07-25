package com.opensabre.admin.dao.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.opensabre.admin.dao.entity.po.TccStockRecord;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * TCC 库存冻结记录 Mapper
 */
@Mapper
public interface TccStockRecordMapper extends BaseMapper<TccStockRecord> {

    /**
     * 根据 xid 查询冻结记录
     */
    List<TccStockRecord> selectByXid(@Param("xid") String xid);

    /**
     * 根据 xid 批量更新状态
     */
    int updateStatusByXid(@Param("xid") String xid, @Param("status") int status);

    /**
     * 删除已完成的记录（状态为1或2，且创建时间超过指定小时数）
     */
    int deleteFinishedRecords(@Param("hours") int hours);
}
