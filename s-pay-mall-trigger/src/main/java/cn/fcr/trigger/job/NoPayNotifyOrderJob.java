package cn.fcr.trigger.job;

import cn.fcr.domain.order.gateway.IAlipayQueryGateway;
import cn.fcr.trigger.application.OrderApplicationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.List;

/**
 * @description 检测未接收到或未正确处理的支付回调通知
 *
 * <p>支付宝交易查询逻辑已封装至 {@link IAlipayQueryGateway}，
 * 本 Job 不再直接依赖 Alipay SDK。</p>
 */
@Slf4j
@Component()
public class NoPayNotifyOrderJob {

    @Resource
    private OrderApplicationService orderApplicationService;

    @Resource
    private IAlipayQueryGateway alipayQueryGateway;

    @Scheduled(cron = "0/30 * * * * ?")
    public void exec() {
        try {
            log.info("任务；检测未接收到或未正确处理的支付回调通知");
            List<String> orderIds = orderApplicationService.queryNoPayNotifyOrder();
            if (null == orderIds || orderIds.isEmpty()) return;

            for (String orderId : orderIds) {
                if (alipayQueryGateway.queryTradeSuccess(orderId)) {
                    orderApplicationService.changeOrderPaySuccess(orderId);
                }
            }
        } catch (Exception e) {
            log.error("检测未接收到或未正确处理的支付回调通知失败", e);
        }
    }

}
