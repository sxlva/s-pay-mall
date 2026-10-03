package cn.fcr.application;

import cn.fcr.domain.mall.cart.model.valobj.CartItemVO;
import cn.fcr.domain.mall.cart.service.IMallCartService;
import cn.fcr.domain.order.adapter.event.IOrderEventPublisher;
import cn.fcr.domain.order.gateway.IOrderPaymentGateway;
import cn.fcr.domain.order.gateway.IPayOrderGateway;
import cn.fcr.domain.order.model.valobj.OrderCreateVO;
import cn.fcr.domain.order.model.valobj.OrderVO;
import cn.fcr.domain.order.service.IMallOrderService;
import cn.fcr.domain.order.service.IOrderStateMachineService;
import cn.fcr.domain.order.service.PayOrderService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

/**
 * 订单应用层服务
 *
 * @author 傅崇睿
 */
@Slf4j
@Service
public class OrderApplicationService {

    /** 商城购物车领域服务 */
    private final IMallCartService mallCartService;
    /** 商城订单领域服务 */
    private final IMallOrderService mallOrderService;
    /** 订单支付网关 */
    private final IOrderPaymentGateway orderPaymentGateway;
    /** 支付单领域服务 */
    private final PayOrderService payOrderService;
    /** 订单事件发布器 */
    private final IOrderEventPublisher orderEventPublisher;
    /** 订单状态机服务（超时关单分流） */
    private final IOrderStateMachineService orderStateMachineService;
    /** 支付单网关（补偿查询等待支付的订单） */
    private final IPayOrderGateway payOrderGateway;
    /** 订单事务服务（内部使用） */
    private final OrderTransactionService orderTransactionService;

    public OrderApplicationService(IMallCartService mallCartService,
                                   IMallOrderService mallOrderService,
                                   IOrderPaymentGateway orderPaymentGateway,
                                   PayOrderService payOrderService,
                                   IOrderEventPublisher orderEventPublisher,
                                   IOrderStateMachineService orderStateMachineService,
                                   IPayOrderGateway payOrderGateway,
                                   OrderTransactionService orderTransactionService) {
        this.mallCartService = mallCartService;
        this.mallOrderService = mallOrderService;
        this.orderPaymentGateway = orderPaymentGateway;
        this.payOrderService = payOrderService;
        this.orderEventPublisher = orderEventPublisher;
        this.orderStateMachineService = orderStateMachineService;
        this.payOrderGateway = payOrderGateway;
        this.orderTransactionService = orderTransactionService;
    }

    // ==================== 购物车 ====================

    /**
     * 添加商品到购物车
     *
     * @param userId    用户ID
     * @param productId 商品ID
     * @param quantity  数量
     * @return 影响行数
     */
    @Transactional(rollbackFor = Exception.class)
    public int addCart(Long userId, Long productId, Integer quantity) {
        return mallCartService.addCart(userId, productId, quantity);
    }

    /**
     * 查询购物车列表
     *
     * @param userId 用户ID
     * @return 购物车商品列表
     */
    public List<CartItemVO> listCart(Long userId) {
        return mallCartService.listCart(userId);
    }

    /**
     * 更新购物车商品数量
     *
     * @param userId    用户ID
     * @param productId 商品ID
     * @param quantity  新数量
     * @return 影响行数
     */
    @Transactional(rollbackFor = Exception.class)
    public int updateCartQuantity(Long userId, Long productId, Integer quantity) {
        return mallCartService.updateQuantity(userId, productId, quantity);
    }

    /**
     * 删除购物车商品
     *
     * @param userId     用户ID
     * @param cartItemId 购物车条目ID
     * @return 影响行数
     */
    @Transactional(rollbackFor = Exception.class)
    public int deleteCartItem(Long userId, Long cartItemId) {
        return mallCartService.deleteCartItem(userId, cartItemId);
    }

    // ==================== 订单 ====================

    /**
     * 创建订单
     *
     * <p>事务边界在 OrderTransactionService 中控制，MQ 消息发送在事务外执行，
     * 避免 sendDelayCloseMessage 在事务内部导致事务 hold 问题。</p>
     *
     * @param userId  用户ID
     * @param address 收货地址
     * @return 订单创建结果，含orderNo和payUrl
     */
    public OrderCreateVO createOrder(Long userId, String address) {
        // 事务内完成所有 DB 操作
        OrderCreateVO result = orderTransactionService.createOrderInTransaction(userId, address);

        // 事务提交后发送延时关闭消息（失败不影响主流程）
        try {
            orderPaymentGateway.sendDelayCloseMessage(result.getOrderNo());
        } catch (Exception e) {
            log.warn("发送延时关闭消息失败，orderNo: {}, error: {}", result.getOrderNo(), e.getMessage());
        }

        return result;
    }

    /**
     * 查询订单列表
     *
     * @param userId    用户ID
     * @param status    订单状态（可选）
     * @param startTime 开始时间（可选）
     * @param endTime   结束时间（可选）
     * @return 订单列表
     */
    public List<OrderVO> listOrders(Long userId, String status, String startTime, String endTime) {
        return mallOrderService.listOrders(userId, status, startTime, endTime);
    }

    /**
     * 继续支付未完成订单
     *
     * @param orderNo 订单号
     * @return 订单支付信息
     */
    @Transactional(rollbackFor = Exception.class)
    public OrderCreateVO continuePay(String orderNo) {
        return mallOrderService.continuePay(orderNo);
    }

    /**
     * 检查订单库存
     *
     * @param orderNo 订单号
     * @return true表示库存充足
     */
    public boolean checkStock(String orderNo) {
        return mallOrderService.checkOrderStock(orderNo);
    }

    /**
     * 订单支付成功处理
     *
     * @param orderNo 订单号
     */
    @Transactional(rollbackFor = Exception.class)
    public void paySuccess(String orderNo) {
        mallOrderService.paySuccess(orderNo);
    }

    /**
     * 发货
     *
     * @param orderId 订单ID
     * @return 影响行数
     */
    @Transactional(rollbackFor = Exception.class)
    public int deliverOrder(Long orderId) {
        return mallOrderService.deliverOrder(orderId);
    }

    /**
     * 取消订单
     *
     * @param orderId 订单ID
     * @return 影响行数
     */
    @Transactional(rollbackFor = Exception.class)
    public int cancelOrder(Long orderId) {
        return mallOrderService.cancelOrder(orderId);
    }

    /**
     * 删除订单
     *
     * @param orderId 订单ID
     * @return 影响行数
     */
    @Transactional(rollbackFor = Exception.class)
    public int deleteOrder(Long orderId) {
        return mallOrderService.deleteOrder(orderId);
    }

    // ==================== 支付回调与补偿 ====================

    /**
     * 验证支付回调签名
     *
     * @param params          回调参数
     * @param alipayPublicKey 支付宝公钥
     * @return true表示验签通过
     */
    public boolean verifyPayCallbackSign(Map<String, String> params, String alipayPublicKey) {
        return payOrderService.verifyCallbackSign(params, alipayPublicKey);
    }

    /**
     * 订单支付成功处理
     *
     * <p>事务边界在 OrderTransactionService 中控制，事件发布在事务外执行。</p>
     *
     * @param orderId 订单ID
     */
    public void changeOrderPaySuccess(String orderId) {
        // 事务内完成 DB 状态更新
        orderTransactionService.changeOrderPaySuccessInTransaction(orderId);

        // 事务提交后发布支付成功事件（失败不影响主流程）
        try {
            orderEventPublisher.publishPaySuccess(orderId, orderId);
        } catch (Exception e) {
            log.warn("发布支付成功事件失败，orderId: {}, error: {}", orderId, e.getMessage());
        }
    }

    /**
     * 查询未收到回调通知的订单
     *
     * <p>委托支付单网关查询 pay_order 中等待支付超过5分钟的订单号，
     * 供 NoPayNotifyOrderJob 主动补偿。</p>
     *
     * @return 订单ID列表
     */
    public List<String> queryNoPayNotifyOrder() {
        return payOrderGateway.queryNoPayNotifyOrder();
    }

    /**
     * 处理超时关单
     *
     * <p>【M2-1 超时关单分流】order_main 存在的订单走状态机 cancel
     * （关 pay_order + 恢复 Redis 预扣库存，状态机守卫保证幂等）；
     * order_main 不存在的订单记 warn 日志并返回 false（legacy 已下线，
     * 不影响 MQ 消费重试语义）。</p>
     *
     * @param orderNo 订单号
     * @return true表示关闭成功
     */
    @Transactional(rollbackFor = Exception.class)
    public boolean handleTimeoutCloseOrder(String orderNo) {
        OrderVO order = mallOrderService.getOrderByNo(orderNo);
        if (order == null) {
            log.warn("超时关单：order_main 不存在，忽略处理: orderNo={}", orderNo);
            return false;
        }
        return orderStateMachineService.cancel(orderNo);
    }

    // ==================== 跨域共享（Mall Domain 查询） ====================

    /**
     * 根据订单号查询订单
     *
     * @param orderNo 订单号
     * @return 订单VO
     */
    public OrderVO getOrderByNo(String orderNo) {
        return mallOrderService.getOrderByNo(orderNo);
    }
}
