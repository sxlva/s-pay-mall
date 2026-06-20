package cn.fcr.infrastructure.config;

import cn.fcr.domain.mall.gateway.IPayGateway;
import cn.fcr.domain.order.service.PayOrderService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class PayOrderConfig {

    @Bean
    public PayOrderService payOrderService(IPayGateway payGateway) {
        return new PayOrderService(payGateway);
    }
}