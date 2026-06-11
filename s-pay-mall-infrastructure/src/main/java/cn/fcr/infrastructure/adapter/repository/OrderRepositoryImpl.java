package cn.fcr.infrastructure.adapter.repository;

import cn.fcr.domain.mall.gateway.IMallOrderQueryGateway;
import cn.fcr.domain.mall.model.entity.OrderEntity;
import cn.fcr.domain.mall.model.entity.OrderItemEntity;
import cn.fcr.domain.mall.model.entity.OrderState;
import cn.fcr.domain.mall.model.valobj.OrderVO;
import cn.fcr.infrastructure.dao.IOrderDao;
import cn.fcr.infrastructure.dao.IOrderItemDao;
import cn.fcr.infrastructure.dao.IOrderMainDao;
import cn.fcr.infrastructure.dao.po.OrderItem;
import cn.fcr.infrastructure.dao.po.OrderMain;
import cn.fcr.infrastructure.dao.po.PayOrder;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Repository
public class OrderRepositoryImpl implements IMallOrderQueryGateway {

    @Resource
    private IOrderMainDao orderMainDao;

    @Resource
    private IOrderItemDao orderItemDao;

    @Resource
    private IOrderDao orderDao;

    @Override
    public void saveOrder(OrderEntity orderEntity) {
        OrderMain orderMain = new OrderMain();
        orderMain.setOrderNo(orderEntity.getOrderNo());
        orderMain.setUserId(orderEntity.getUserId());
        orderMain.setTotalAmount(orderEntity.getTotalAmount());
        orderMain.setStatus(orderEntity.getState() != null ? orderEntity.getState().toDbStatus() : "CREATED");
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

        if (!items.isEmpty()) {
            PayOrder payOrder = PayOrder.builder()
                    .userId(String.valueOf(orderEntity.getUserId()))
                    .productId(String.valueOf(items.get(0).getProductId()))
                    .productName(items.get(0).getProductName())
                    .orderId(orderEntity.getOrderNo())
                    .orderTime(new Date())
                    .totalAmount(orderEntity.getTotalAmount())
                    .status("WAIT_PAY")
                    .createTime(new Date())
                    .updateTime(new Date())
                    .build();
            orderDao.insert(payOrder);
        }
    }

    @Override
    public List<OrderVO> findOrders(Long userId, String status, String start, String end) {
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

    @Override
    public OrderEntity findById(Long id) {
        OrderMain order = orderMainDao.selectById(id);
        if (order == null) return null;
        return toOrderEntity(order);
    }

    @Override
    public OrderEntity findByOrderNo(String orderNo) {
        LambdaQueryWrapper<OrderMain> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(OrderMain::getOrderNo, orderNo);
        OrderMain order = orderMainDao.selectOne(queryWrapper);
        if (order == null) return null;
        return toOrderEntity(order);
    }

    @Override
    public int deleteById(Long id) {
        return orderMainDao.deleteById(id);
    }

    @Override
    public int updateOrderStatus(Long orderId, String status) {
        LambdaUpdateWrapper<OrderMain> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(OrderMain::getId, orderId);
        updateWrapper.set(OrderMain::getStatus, status);
        updateWrapper.set(OrderMain::getUpdateTime, LocalDateTime.now());
        return orderMainDao.update(null, updateWrapper);
    }

    @Override
    public int updateOrderStatusByOrderNo(String orderNo, String status) {
        LambdaUpdateWrapper<OrderMain> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(OrderMain::getOrderNo, orderNo);
        updateWrapper.set(OrderMain::getStatus, status);
        updateWrapper.set(OrderMain::getUpdateTime, LocalDateTime.now());
        return orderMainDao.update(null, updateWrapper);
    }

    @Override
    public BigDecimal sumDailySales(String date) {
        return orderMainDao.sumDailySales(date);
    }

    @Override
    public Integer countDailyOrders(String date) {
        return orderMainDao.countDailyOrders(date);
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

        OrderState state = OrderState.fromDbStatus(order.getStatus());
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

    private OrderEntity toOrderEntity(OrderMain order) {
        LambdaQueryWrapper<OrderItem> itemQuery = new LambdaQueryWrapper<>();
        itemQuery.eq(OrderItem::getOrderId, order.getId());
        List<OrderItem> items = orderItemDao.selectList(itemQuery);

        List<OrderItemEntity> itemEntities = items.stream()
                .map(item -> OrderItemEntity.builder()
                        .id(item.getId())
                        .productId(item.getProductId())
                        .productName(item.getProductName())
                        .price(item.getPrice())
                        .quantity(item.getQuantity())
                        .createTime(item.getCreateTime())
                        .build())
                .collect(Collectors.toList());

        return OrderEntity.builder()
                .orderNo(order.getOrderNo())
                .userId(order.getUserId())
                .totalAmount(order.getTotalAmount())
                .address(order.getAddress())
                .state(OrderState.fromDbStatus(order.getStatus()))
                .items(itemEntities)
                .createTime(order.getCreateTime())
                .updateTime(order.getUpdateTime())
                .build();
    }
}