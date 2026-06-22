package cn.fcr.infrastructure.config.order;

import cn.fcr.domain.mall.gateway.IPayGateway;
import cn.fcr.domain.order.service.PayOrderService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 支付订单服务手动装配配置
 *
 * @author 傅崇睿
 */
@Configuration
public class PayOrderConfig {

    /**
     * 注册PayOrderService Bean
     *
     * @param payGateway 支付网关
     * @return PayOrderService实例
     */
    @Bean
    public PayOrderService payOrderService(IPayGateway payGateway) {
        return new PayOrderService(payGateway);
    }
}
