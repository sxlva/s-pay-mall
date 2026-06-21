package cn.fcr.trigger.job;

import cn.fcr.trigger.application.OrderApplicationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.List;

/**
 * @description 超时关单（已废弃，改用 RocketMQ 延时消息）
 */
@Slf4j
//@Component()
public class TimeoutCloseOrderJob {

    @Resource
    private OrderApplicationService orderApplicationService;

    //@Scheduled(cron = "0 0/10 * * * ?")
    public void exec() {
        try {
            log.info("任务；超时30分钟订单关闭");
            List<String> orderIds = orderApplicationService.queryTimeoutCloseOrderList();
            if (null == orderIds || orderIds.isEmpty()) {
                log.info("定时任务，超时30分钟订单关闭，暂无超时未支付订单 orderIds is null");
                return;
            }
            for (String orderId : orderIds) {
                boolean status = orderApplicationService.changeOrderClose(orderId);
                log.info("定时任务，超时30分钟订单关闭 orderId: {} status：{}", orderId, status);
            }
        } catch (Exception e) {
            log.error("定时任务，超时15分钟订单关闭失败", e);
        }
    }

}
