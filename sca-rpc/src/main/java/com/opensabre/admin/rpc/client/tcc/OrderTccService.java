package com.opensabre.admin.rpc.client.tcc;

import com.opensabre.admin.rpc.client.dto.OrderCreateRequest;
import io.seata.rm.tcc.api.BusinessActionContext;
import io.seata.rm.tcc.api.BusinessActionContextParameter;
import io.seata.rm.tcc.api.LocalTCC;
import io.seata.rm.tcc.api.TwoPhaseBusinessAction;

/**
 * TCC 订单服务接口 - Seata 两阶段提交
 * <p>
 * Try: 创建待确认订单（status=2）
 * Commit: 确认订单（status 2→1）
 * Rollback: 取消订单（status 2→0）
 */
@LocalTCC
public interface OrderTccService {

    /**
     * TCC Try: 创建待确认订单
     *
     * @return orderId
     */
    @TwoPhaseBusinessAction(name = "orderTccAction", commitMethod = "commit", rollbackMethod = "rollback", useTCCFence = true)
    String tryCreate(@BusinessActionContextParameter(paramName = "request") OrderCreateRequest request);

    /**
     * TCC Commit: 确认订单
     */
    boolean commit(BusinessActionContext context);

    /**
     * TCC Rollback: 取消订单
     */
    boolean rollback(BusinessActionContext context);
}
