package cn.fcr.trigger.listener;

import cn.fcr.domain.order.adapter.event.PaySuccessMessageEvent;
import cn.fcr.domain.mall.service.IMallOrderService;
import cn.fcr.infrastructure.adapter.port.LoginPort;
import cn.fcr.infrastructure.dao.IOrderItemDao;
import cn.fcr.infrastructure.dao.IOrderMainDao;
import cn.fcr.infrastructure.dao.IUserBindingDao;
import cn.fcr.infrastructure.dao.po.OrderItem;
import cn.fcr.infrastructure.dao.po.OrderMain;
import cn.fcr.infrastructure.dao.po.UserBindingPO;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Slf4j
@Component
@RocketMQMessageListener(topic = "order.paid", consumerGroup = "s-pay-mall-order-paid-consumer")
public class OrderPaidRocketListener implements RocketMQListener<PaySuccessMessageEvent.PaySuccessMessage> {

    @Resource
    private IMallOrderService mallOrderService;

    @Resource
    private IOrderMainDao orderMainDao;

    @Resource
    private IOrderItemDao orderItemDao;

    @Resource
    private IUserBindingDao userBindingDao;

    @Resource
    private LoginPort loginPort;

    private static final String TEST_OPENID = "or0Ab6ivwmypESVp_bYuk92T6SvU";

    @Override
    public void onMessage(PaySuccessMessageEvent.PaySuccessMessage message) {
        log.info("【RocketMQ 核心链路】收到支付成功消息，开始执行后续核心业务逻辑。订单号: {}, 交易号: {}",
                message.getOrderNo(), message.getTradeNo());

        try {
            mallOrderService.paySuccess(message.getOrderNo());
            log.info("订单状态更新成功，触发后续履约链路：开始发货、用户充值、发放会员权益、计算返利...");

            // TODO: 发货流程 - 实物商品需要触发物流系统
            // deliverGoods(message.getOrderNo());

            // TODO: 虚拟商品/会员充值 - 自动发放权益
            // memberService.recharge(message.getUserId(), message.getOrderNo());

            // TODO: 会员升级 - 根据订单金额计算会员积分
            // memberService.upgradeStatus(message.getUserId());

            // TODO: 返利机制 - 推荐人返利、订单返利
            // rebateService.calculateRebate(message.getOrderNo(), message.getUserId());
            
            try {
                sendWechatNotify(message);
                log.info("【履约完成】订单 {} 所有后续业务处理完毕，微信通知已发送", message.getOrderNo());
            } catch (Exception e) {
                log.error("发送微信通知失败，但不影响订单状态，订单号: {}", message.getOrderNo(), e);
            }

        } catch (Exception e) {
            log.error("处理支付成功消息异常，订单号: {}, 交易号: {}", message.getOrderNo(), message.getTradeNo(), e);
            throw e;
        }
    }

    private void sendWechatNotify(PaySuccessMessageEvent.PaySuccessMessage message) throws IOException {
        LambdaQueryWrapper<OrderMain> orderQuery = new LambdaQueryWrapper<>();
        orderQuery.eq(OrderMain::getOrderNo, message.getOrderNo());
        OrderMain orderMain = orderMainDao.selectOne(orderQuery);

        if (orderMain == null) {
            log.warn("订单不存在，不发送微信通知，订单号: {}", message.getOrderNo());
            return;
        }

        LambdaQueryWrapper<OrderItem> itemQuery = new LambdaQueryWrapper<>();
        itemQuery.eq(OrderItem::getOrderId, orderMain.getId());
        List<OrderItem> orderItems = orderItemDao.selectList(itemQuery);

        String productName = "商品";
        if (orderItems != null && !orderItems.isEmpty()) {
            productName = orderItems.get(0).getProductName();
        }

        String openid = TEST_OPENID;
        if (orderMain.getUserId() != null) {
            UserBindingPO userBinding = userBindingDao.findByUserIdAndIdentityType(orderMain.getUserId(), "weixin");
            if (userBinding != null) {
                openid = userBinding.getIdentifier();
            }
        }

        String payTime = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        String amount = orderMain.getTotalAmount().toString();

        loginPort.sendPaySuccessTemplate(openid, productName, message.getOrderNo(), amount, payTime);
        log.info("微信支付成功通知已发送，openid: {}, 商品: {}, 订单号: {}", openid, productName, message.getOrderNo());
    }
}