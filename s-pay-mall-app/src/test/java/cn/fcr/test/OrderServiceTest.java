package cn.fcr.test;

import cn.fcr.domain.shared.model.entity.PayOrderEntity;
import cn.fcr.domain.order.model.entity.ShopCartEntity;
import cn.fcr.domain.order.service.IOrderService;
import com.alibaba.fastjson.JSON;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import javax.annotation.Resource;

/**
 * 订单服务测试
 *
 * @author 傅崇睿
 */
@Slf4j
@RunWith(SpringRunner.class)
@SpringBootTest
public class OrderServiceTest {

    /** 旧订单领域服务 */
    @Resource
    private IOrderService orderService;

    /**
     * 测试创建订单（旧域）
     *
     * @throws Exception 创建失败
     */
    @Test
    public void test_createOrder() throws Exception {
        ShopCartEntity shopCartEntity = ShopCartEntity.builder()
                .userId("fuchongrui")
                .productId("10001")
                .build();
        PayOrderEntity payOrderEntity = orderService.createOrder(shopCartEntity);
        log.info("请求参数:{}", JSON.toJSONString(shopCartEntity));
        log.info("测试结果:{}", JSON.toJSONString(payOrderEntity));
    }

}
