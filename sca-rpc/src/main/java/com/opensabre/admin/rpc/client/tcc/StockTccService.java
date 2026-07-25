package com.opensabre.admin.rpc.client.tcc;

import com.opensabre.admin.rpc.client.dto.StockFreezeRequest;
import io.seata.rm.tcc.api.BusinessActionContext;
import io.seata.rm.tcc.api.BusinessActionContextParameter;
import io.seata.rm.tcc.api.LocalTCC;
import io.seata.rm.tcc.api.TwoPhaseBusinessAction;

/**
 * TCC 库存服务接口 - Seata 两阶段提交
 * <p>
 * Try: 冻结库存（stock -= qty, frozen_stock += qty）
 * Commit: 确认扣减（frozen_stock -= qty）
 * Rollback: 释放冻结（stock += qty, frozen_stock -= qty）
 */
@LocalTCC
public interface StockTccService {

    /**
     * TCC Try: 冻结库存
     */
    @TwoPhaseBusinessAction(name = "stockTccAction", commitMethod = "commit", rollbackMethod = "rollback", useTCCFence = true)
    boolean tryFreeze(@BusinessActionContextParameter(paramName = "request") StockFreezeRequest request);

    /**
     * TCC Commit: 确认扣减冻结的库存
     */
    boolean commit(BusinessActionContext context);

    /**
     * TCC Rollback: 释放冻结的库存
     */
    boolean rollback(BusinessActionContext context);
}
