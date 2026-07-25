package com.opensabre.admin.stock.service;

import com.opensabre.admin.dao.entity.po.TccStockRecord;
import com.opensabre.admin.dao.mapper.HotelProductSpecMapper;
import com.opensabre.admin.dao.mapper.TccStockRecordMapper;
import com.opensabre.admin.rpc.client.dto.StockFreezeRequest;
import com.opensabre.admin.rpc.client.tcc.StockTccService;
import io.seata.core.context.RootContext;
import io.seata.rm.tcc.api.BusinessActionContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * TCC 库存服务实现 - Seata 两阶段提交
 * <p>
 * Try: 冻结库存（stock -= qty, frozen_stock += qty）
 * Commit: 确认扣减（frozen_stock -= qty）
 * Rollback: 释放冻结（stock += qty, frozen_stock -= qty）
 * <p>
 * 防悬挂、空回滚、幂等由 Seata TCC 框架自动保障。
 */
@Slf4j
@Service
public class StockTccServiceImpl implements StockTccService {

    @Autowired
    private HotelProductSpecMapper hotelProductSpecMapper;

    @Autowired
    private TccStockRecordMapper tccStockRecordMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean tryFreeze(StockFreezeRequest request) {
        String xid = RootContext.getXID();
        log.info("[TCC-Try] 冻结库存开始: xid={}, items={}", xid, request.getItems().size());

        for (StockFreezeRequest.FreezeItem item : request.getItems()) {
            // 冻结库存：stock -= qty, frozen_stock += qty
            int affected = hotelProductSpecMapper.freezeStock(item.getSpecId(), item.getQuantity());
            if (affected == 0) {
                throw new RuntimeException("库存不足: specId=" + item.getSpecId() + ", quantity=" + item.getQuantity());
            }

            // 记录冻结操作（供 Commit/Rollback 使用）
            TccStockRecord record = new TccStockRecord();
            record.setXid(xid);
            record.setSpecId(item.getSpecId());
            record.setQuantity(item.getQuantity());
            record.setStatus(0); // 0=冻结中
            tccStockRecordMapper.insert(record);

            log.info("[TCC-Try] 冻结库存成功: specId={}, quantity={}, xid={}", item.getSpecId(), item.getQuantity(), xid);
        }

        return true;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean commit(BusinessActionContext context) {
        String xid = context.getXid();
        log.info("[TCC-Commit] 确认库存扣减: xid={}", xid);

        List<TccStockRecord> records = tccStockRecordMapper.selectByXid(xid);
        if (records.isEmpty()) {
            log.warn("[TCC-Commit] 未找到冻结记录: xid={}", xid);
            return true;
        }

//         幂等检查：已确认过则直接返回
        if (records.stream().allMatch(r -> r.getStatus() == 1)) {
            log.info("[TCC-Commit] 已确认过，幂等返回: xid={}", xid);
            return true;
        }

        for (TccStockRecord record : records) {
            if (record.getStatus() != 0) continue;

            // 确认扣减：frozen_stock -= qty
            hotelProductSpecMapper.confirmFrozen(record.getSpecId(), record.getQuantity());
            log.info("[TCC-Commit] 确认扣减: specId={}, quantity={}", record.getSpecId(), record.getQuantity());
        }

        // 更新记录状态为已确认
        tccStockRecordMapper.updateStatusByXid(xid, 1);
        log.info("[TCC-Commit] 库存确认完成: xid={}", xid);
        return true;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean rollback(BusinessActionContext context) {
        String xid = context.getXid();
        log.info("[TCC-Rollback] 释放冻结库存: xid={}", xid);

        List<TccStockRecord> records = tccStockRecordMapper.selectByXid(xid);
        if (records.isEmpty()) {
            // 空回滚：Try 可能还没执行
            log.warn("[TCC-Rollback] 空回滚，Try未执行: xid={}", xid);
            TccStockRecord mark = new TccStockRecord();
            mark.setXid(xid);
            mark.setSpecId("empty_rollback");
            mark.setQuantity(0);
            mark.setStatus(2); // 已取消标记
            tccStockRecordMapper.insert(mark);
            return true;
        }

        // 幂等检查：已取消过则直接返回
        if (records.stream().allMatch(r -> r.getStatus() == 2)) {
            log.info("[TCC-Rollback] 已取消过，幂等返回: xid={}", xid);
            return true;
        }

        for (TccStockRecord record : records) {
            if (record.getStatus() != 0) continue;
            if ("empty_rollback".equals(record.getSpecId())) continue;

            // 释放冻结：stock += qty, frozen_stock -= qty
            hotelProductSpecMapper.cancelFrozen(record.getSpecId(), record.getQuantity());
            log.info("[TCC-Rollback] 释放冻结: specId={}, quantity={}", record.getSpecId(), record.getQuantity());
        }

        // 更新记录状态为已取消
        tccStockRecordMapper.updateStatusByXid(xid, 2);
        log.info("[TCC-Rollback] 库存释放完成: xid={}", xid);
        return true;
    }
}
