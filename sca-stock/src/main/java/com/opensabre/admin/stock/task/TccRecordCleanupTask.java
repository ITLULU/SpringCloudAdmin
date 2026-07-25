package com.opensabre.admin.stock.task;

import com.opensabre.admin.dao.mapper.TccStockRecordMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * TCC 记录定时清理任务
 * <p>
 * 清理已完成（已确认/已取消）超过 24 小时的 tcc_stock_record 记录，
 * 防止表无限增长。
 */
@Slf4j
@Component
public class TccRecordCleanupTask {

    @Autowired
    private TccStockRecordMapper tccStockRecordMapper;

    /**
     * 每天凌晨 3 点执行清理
     * 删除状态为已确认(1)或已取消(2)且创建时间超过24小时的记录
     */
    @Scheduled(cron = "0 0 3 * * ?")
    public void cleanupFinishedRecords() {
        log.info("[TCC清理] 开始清理已完成的TCC记录...");
        try {
            int deleted = tccStockRecordMapper.deleteFinishedRecords(24);
            log.info("[TCC清理] 清理完成，删除记录数: {}", deleted);
        } catch (Exception e) {
            log.error("[TCC清理] 清理失败: {}", e.getMessage(), e);
        }
    }
}
