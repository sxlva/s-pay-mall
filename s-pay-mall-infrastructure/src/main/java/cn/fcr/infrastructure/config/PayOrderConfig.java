package cn.fcr.infrastructure.config;

import cn.fcr.domain.order.service.PayOrderService;
import com.alipay.api.AlipayClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class PayOrderConfig {

    @Bean
    public PayOrderService payOrderService(AlipayClient alipayClient,
                                           AliPayConfigProperties properties) {
        return new PayOrderService(
                alipayClient,
                properties.getNotify_url(),
                properties.getReturn_url()
        );
    }
}