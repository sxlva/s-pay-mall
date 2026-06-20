package cn.fcr.trigger.job;

import cn.fcr.domain.order.gateway.IAlipayQueryGateway;
import cn.fcr.domain.order.service.IOrderService;
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
 *
 * @note 当前已禁用：因 IOrderDao.queryNoPayNotifyOrder 方法未绑定，导致每3秒抛出异常
 *       需在 IOrderDao 中正确配置该方法后再启用
 */
@Slf4j
//@Component()
public class NoPayNotifyOrderJob {

    @Resource
    private IOrderService orderService;

    @Resource
    private IAlipayQueryGateway alipayQueryGateway;

    //@Scheduled(cron = "0/3 * * * * ?")
    public void exec() {
        try {
            log.info("任务；检测未接收到或未正确处理的支付回调通知");
            List<String> orderIds = orderService.queryNoPayNotifyOrder();
            if (null == orderIds || orderIds.isEmpty()) return;

            for (String orderId : orderIds) {
                if (alipayQueryGateway.queryTradeSuccess(orderId)) {
                    orderService.changeOrderPaySuccess(orderId);
                }
            }
        } catch (Exception e) {
            log.error("检测未接收到或未正确处理的支付回调通知失败", e);
        }
    }

}
