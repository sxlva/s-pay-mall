package cn.fcr.infrastructure.adapter.port;

import cn.fcr.domain.order.adapter.port.IOrderEventPort;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

@Slf4j
@Component
public class OrderEventPortImpl implements IOrderEventPort {

    @Resource
    private RocketMQTemplate rocketMQTemplate;

    @Override
    public void sendDelayCloseMessage(String orderNo) {
        log.info("发送订单延时关闭消息: orderNo={}", orderNo);
        rocketMQTemplate.syncSend("order-close-topic", orderNo, 3000);
    }

    @Override
    public void sendPaySuccessMessage(String orderNo) {
        log.info("发送支付成功消息: orderNo={}", orderNo);
        rocketMQTemplate.syncSend("pay-success-topic", orderNo);
    }
}