package cn.fcr.infrastructure.adapter.service;

import cn.fcr.domain.mall.adapter.port.IOrderPaymentPort;
import cn.fcr.domain.mall.model.entity.OrderEntity;
import cn.fcr.domain.mall.model.entity.OrderItemEntity;
import cn.fcr.domain.mall.model.entity.OrderState;
import cn.fcr.domain.mall.model.valobj.CartItemVO;
import cn.fcr.domain.mall.model.valobj.OrderCreateVO;
import cn.fcr.domain.mall.model.valobj.OrderVO;
import cn.fcr.domain.mall.service.IMallCartService;
import cn.fcr.domain.mall.service.IMallOrderService;
import cn.fcr.domain.order.model.entity.PayOrderEntity;
import cn.fcr.domain.order.model.valobj.PayStatus;
import cn.fcr.infrastructure.dao.IOrderDao;
import cn.fcr.infrastructure.dao.IOrderItemDao;
import cn.fcr.infrastructure.dao.IOrderMainDao;
import cn.fcr.infrastructure.dao.IProductDao;
import cn.fcr.infrastructure.dao.po.OrderItem;
import cn.fcr.infrastructure.dao.po.OrderMain;
import cn.fcr.infrastructure.dao.po.PayOrder;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
public class MallOrderServiceImpl implements IMallOrderService {

    @Resource
    private IMallCartService mallCartService;

    @Resource
    private IOrderMainDao orderMainDao;

    @Resource
    private IOrderItemDao orderItemDao;

    @Resource
    private IProductDao productDao;

    @Resource
    private IOrderDao orderDao;

    @Resource
    private IOrderPaymentPort orderPaymentPort;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public OrderCreateVO createOrder(Long userId, String address) {
        List<CartItemVO> cart = mallCartService.listCart(userId);
        if (cart.isEmpty()) {
            throw new IllegalArgumentException("购物车为空");
        }

        OrderEntity orderEntity = buildOrderEntity(userId, address, cart);
        // 移除 orderEntity.pay() 调用，保持初始状态为 INIT（对应数据库的 CREATED）
        persistOrder(orderEntity);

        mallCartService.clearCart(userId);

        orderPaymentPort.sendDelayCloseMessage(orderEntity.getOrderNo());

        PayOrderEntity payOrderEntity = buildPayOrderEntity(orderEntity);
        String payUrl = orderPaymentPort.generatePayUrl(payOrderEntity);

        orderPaymentPort.updatePayOrderInfo(payOrderEntity);

        return OrderCreateVO.builder()
                .orderNo(orderEntity.getOrderNo())
                .totalAmount(orderEntity.getTotalAmount())
                .status(orderEntity.getState().getCode())
                .payUrl(payUrl)
                .build();
    }

    private OrderEntity buildOrderEntity(Long userId, String address, List<CartItemVO> cart) {
        String orderNo = "ORD" + UUID.randomUUID().toString()
                .replace("-", "").substring(0, 29).toUpperCase();

        OrderEntity entity = OrderEntity.builder()
                .orderNo(orderNo)
                .userId(userId)
                .address(address)
                .state(OrderState.INIT)
                .createTime(LocalDateTime.now())
                .updateTime(LocalDateTime.now())
                .build();

        BigDecimal total = BigDecimal.ZERO;
        for (CartItemVO item : cart) {
            OrderItemEntity orderItem = OrderItemEntity.builder()
                    .productId(item.getProductId())
                    .productName(item.getProductName())
                    .price(item.getProductPrice())
                    .quantity(item.getQuantity())
                    .createTime(LocalDateTime.now())
                    .build();
            entity.addItem(orderItem);
            total = total.add(item.getItemAmount());
        }
        entity.setTotalAmount(total);

        return entity;
    }

    private PayOrderEntity buildPayOrderEntity(OrderEntity orderEntity) {
        StringBuilder productNameBuilder = new StringBuilder();
        StringBuilder productIds = new StringBuilder();
        List<OrderItemEntity> items = orderEntity.getItems();

        for (int i = 0; i < items.size(); i++) {
            OrderItemEntity item = items.get(i);
            if (i > 0) {
                productNameBuilder.append("、");
                productIds.append(",");
            }
            productNameBuilder.append(item.getProductName());
            productIds.append(item.getProductId());
        }

        return PayOrderEntity.builder()
                .orderNo(orderEntity.getOrderNo())
                .userId(String.valueOf(orderEntity.getUserId()))
                .productId(productIds.toString())
                .productName(productNameBuilder.toString())
                .totalAmount(orderEntity.getTotalAmount())
                .status(PayStatus.WAIT_PAY)
                .createTime(LocalDateTime.now())
                .updateTime(LocalDateTime.now())
                .build();
    }

    private void persistOrder(OrderEntity orderEntity) {
        OrderMain orderMain = new OrderMain();
        orderMain.setOrderNo(orderEntity.getOrderNo());
        orderMain.setUserId(orderEntity.getUserId());
        orderMain.setTotalAmount(orderEntity.getTotalAmount());
        orderMain.setStatus(mapDomainStateToDbStatus(orderEntity.getState()));
        orderMain.setAddress(orderEntity.getAddress());
        orderMain.setCreateTime(orderEntity.getCreateTime());
        orderMain.setUpdateTime(orderEntity.getUpdateTime());
        orderMainDao.insert(orderMain);

        Long orderId = orderMain.getId();

        List<OrderItemEntity> items = orderEntity.getItems();
        for (OrderItemEntity item : items) {
            OrderItem orderItem = new OrderItem();
            orderItem.setOrderId(orderId);
            orderItem.setProductId(item.getProductId());
            orderItem.setProductName(item.getProductName());
            orderItem.setPrice(item.getPrice());
            orderItem.setQuantity(item.getQuantity());
            orderItem.setCreateTime(LocalDateTime.now());
            orderItemDao.insert(orderItem);
        }

        PayOrder payOrder = PayOrder.builder()
                .userId(String.valueOf(orderEntity.getUserId()))
                .productId(String.valueOf(items.get(0).getProductId()))
                .productName(items.get(0).getProductName())
                .orderId(orderEntity.getOrderNo())
                .orderTime(new Date())
                .totalAmount(orderEntity.getTotalAmount())
                .status(PayStatus.WAIT_PAY.getCode())
                .createTime(new Date())
                .updateTime(new Date())
                .build();
        orderDao.insert(payOrder);
        log.info("已创建支付订单记录，orderNo={}, status=WAIT_PAY", orderEntity.getOrderNo());
    }

    @Override
    public List<OrderVO> listOrders(Long userId, String status, String start, String end) {
        LambdaQueryWrapper<OrderMain> queryWrapper = new LambdaQueryWrapper<>();

        if (userId != null) {
            queryWrapper.eq(OrderMain::getUserId, userId);
        }
        if (status != null && !status.isBlank()) {
            queryWrapper.eq(OrderMain::getStatus, status);
        }
        if (start != null && !start.isBlank()) {
            queryWrapper.ge(OrderMain::getCreateTime, start);
        }
        if (end != null && !end.isBlank()) {
            queryWrapper.le(OrderMain::getCreateTime, end);
        }
        queryWrapper.orderByDesc(OrderMain::getId);

        return orderMainDao.selectList(queryWrapper).stream()
                .map(this::toOrderVO)
                .collect(Collectors.toList());
    }

    private OrderVO toOrderVO(OrderMain order) {
        OrderVO.OrderVOBuilder builder = OrderVO.builder()
                .id(order.getId())
                .orderNo(order.getOrderNo())
                .userId(order.getUserId())
                .totalAmount(order.getTotalAmount())
                .address(order.getAddress())
                .status(order.getStatus())
                .createTime(order.getCreateTime())
                .updateTime(order.getUpdateTime());

        OrderState state = mapDbStatusToDomainState(order.getStatus());
        if (state != null) {
            builder.statusDesc(state.getDescription());
        }

        LambdaQueryWrapper<OrderItem> itemQuery = new LambdaQueryWrapper<>();
        itemQuery.eq(OrderItem::getOrderId, order.getId());
        List<OrderItem> items = orderItemDao.selectList(itemQuery);

        List<OrderVO.OrderItemVO> itemVOs = items.stream()
                .map(item -> OrderVO.OrderItemVO.builder()
                        .id(item.getId())
                        .productId(item.getProductId())
                        .productName(item.getProductName())
                        .price(item.getPrice())
                        .quantity(item.getQuantity())
                        .build())
                .collect(Collectors.toList());
        builder.items(itemVOs);

        return builder.build();
    }

    @Override
    public int deleteOrder(Long id) {
        return orderMainDao.deleteById(id);
    }

    @Override
    public int cancelOrder(Long orderId) {
        LambdaUpdateWrapper<OrderMain> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(OrderMain::getId, orderId);
        updateWrapper.eq(OrderMain::getStatus, "CREATED"); // 只有待支付状态才能取消
        updateWrapper.set(OrderMain::getStatus, mapDomainStateToDbStatus(OrderState.CANCELED));
        updateWrapper.set(OrderMain::getUpdateTime, LocalDateTime.now());
        return orderMainDao.update(null, updateWrapper);
    }

    @Override
    public int deliverOrder(Long orderId) {
        LambdaUpdateWrapper<OrderMain> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(OrderMain::getId, orderId);
        updateWrapper.eq(OrderMain::getStatus, OrderState.PAID.getCode()); // 只有已支付状态才能发货
        updateWrapper.set(OrderMain::getStatus, mapDomainStateToDbStatus(OrderState.SHIPPED));
        updateWrapper.set(OrderMain::getUpdateTime, LocalDateTime.now());
        return orderMainDao.update(null, updateWrapper);
    }

    @Override
    public void paySuccess(String orderNo) {
        LambdaUpdateWrapper<OrderMain> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(OrderMain::getOrderNo, orderNo);
        updateWrapper.eq(OrderMain::getStatus, "CREATED"); // 幂等性检查：只有待支付状态才允许变更
        updateWrapper.set(OrderMain::getStatus, OrderState.PAID.getCode());
        updateWrapper.set(OrderMain::getUpdateTime, LocalDateTime.now());
        
        int updated = orderMainDao.update(null, updateWrapper);
        if (updated > 0) {
            log.info("订单支付成功，orderNo={}", orderNo);
        } else {
            log.warn("订单状态不允许更新或订单不存在，orderNo={}，可能是重复回调", orderNo);
        }
    }

    /**
     * 将领域层 OrderState 映射到数据库 order_main.status 字段
     * 确保领域状态与数据库 DDL 定义保持一致
     */
    private String mapDomainStateToDbStatus(OrderState state) {
        if (state == null) {
            return "CREATED"; // 默认初始状态
        }
        switch (state) {
            case INIT:
                return "CREATED"; // 领域状态 INIT 映射到数据库的 CREATED
            case PAID:
                return "PAID";
            case SHIPPED:
                return "SHIPPED";
            case DONE:
                return "COMPLETED"; // 领域状态 DONE 映射到数据库的 COMPLETED
            case CANCELED:
                return "CANCELLED"; // 领域状态 CANCELED 映射到数据库的 CANCELLED
            default:
                return state.getCode();
        }
    }

    /**
     * 将数据库 order_main.status 字段映射到领域层 OrderState
     * 确保数据库状态与领域状态正确转换
     */
    private OrderState mapDbStatusToDomainState(String dbStatus) {
        if (dbStatus == null) {
            return null;
        }
        switch (dbStatus) {
            case "CREATED":
                return OrderState.INIT; // 数据库 CREATED 映射到领域状态 INIT
            case "PAID":
                return OrderState.PAID;
            case "SHIPPED":
                return OrderState.SHIPPED;
            case "COMPLETED":
                return OrderState.DONE; // 数据库 COMPLETED 映射到领域状态 DONE
            case "CANCELLED":
                return OrderState.CANCELED; // 数据库 CANCELLED 映射到领域状态 CANCELED
            default:
                return OrderState.fromCode(dbStatus);
        }
    }
}
