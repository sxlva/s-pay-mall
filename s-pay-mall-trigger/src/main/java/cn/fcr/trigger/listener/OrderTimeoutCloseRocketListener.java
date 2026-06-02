package cn.fcr.trigger.listener;

import cn.fcr.domain.mall.service.IMallOrderService;
import cn.fcr.infrastructure.dao.IOrderMainDao;
import cn.fcr.infrastructure.dao.IProductDao;
import cn.fcr.infrastructure.dao.IOrderItemDao;
import cn.fcr.infrastructure.dao.po.OrderItem;
import cn.fcr.infrastructure.dao.po.OrderMain;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.List;

/**
 * RocketMQ 超时关单消息消费者
 * 
 * <p>订阅 order-timeout-topic，接收延时消息进行超时关单处理</p>
 */
@Slf4j
@Component
@RocketMQMessageListener(topic = "order-timeout-topic", consumerGroup = "s-pay-mall-timeout-group")
public class OrderTimeoutCloseRocketListener implements RocketMQListener<String> {

    @Resource
    private IOrderMainDao orderMainDao;
    
    @Resource
    private IOrderItemDao orderItemDao;
    
    @Resource
    private IProductDao productDao;

    @Override
    public void onMessage(String orderNo) {
        log.info("接收到超时关单消息: orderNo={}", orderNo);
        
        try {
            // 查询订单当前状态
            LambdaQueryWrapper<OrderMain> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(OrderMain::getOrderNo, orderNo);
            OrderMain order = orderMainDao.selectOne(queryWrapper);
            
            if (order == null) {
                log.warn("订单不存在，可能已被删除: orderNo={}", orderNo);
                return;
            }
            
            // 状态判定：只有待支付状态才执行关单
            if (!"CREATED".equals(order.getStatus())) {
                log.info("订单状态已变更，无需关单: orderNo={}, status={}", orderNo, order.getStatus());
                return;
            }
            
            // 执行关单：更新订单状态为已关闭
            LambdaUpdateWrapper<OrderMain> updateWrapper = new LambdaUpdateWrapper<>();
            updateWrapper.eq(OrderMain::getId, order.getId());
            updateWrapper.eq(OrderMain::getStatus, "CREATED"); // 乐观锁
            updateWrapper.set(OrderMain::getStatus, "CLOSED");
            updateWrapper.set(OrderMain::getUpdateTime, LocalDateTime.now());
            
            int affectedRows = orderMainDao.update(null, updateWrapper);
            if (affectedRows == 0) {
                log.info("订单状态已被其他线程修改，关单失败: orderNo={}", orderNo);
                return;
            }
            
            // 恢复库存
            LambdaQueryWrapper<OrderItem> itemQueryWrapper = new LambdaQueryWrapper<>();
            itemQueryWrapper.eq(OrderItem::getOrderId, order.getId());
            List<OrderItem> orderItems = orderItemDao.selectList(itemQueryWrapper);
            
            for (OrderItem item : orderItems) {
                // 使用乐观锁恢复库存：SET stock = stock + quantity WHERE id = ?
                productDao.increaseStock(item.getProductId(), item.getQuantity());
                log.info("已恢复库存: productId={}, quantity={}", item.getProductId(), item.getQuantity());
            }
            
            log.info("超时关单成功: orderNo={}", orderNo);
            
        } catch (Exception e) {
            log.error("超时关单处理失败: orderNo={}", orderNo, e);
            // 异常不抛，确保消息被签收，避免重复消费
        }
    }
}
