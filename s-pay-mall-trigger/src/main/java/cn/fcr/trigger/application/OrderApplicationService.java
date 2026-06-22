package cn.fcr.trigger.application;

import cn.fcr.domain.mall.gateway.IOrderPaymentGateway;
import cn.fcr.domain.mall.model.valobj.CartItemVO;
import cn.fcr.domain.mall.model.valobj.OrderCreateVO;
import cn.fcr.domain.mall.model.valobj.OrderVO;
import cn.fcr.domain.mall.service.IMallCartService;
import cn.fcr.domain.mall.service.IMallOrderService;
import cn.fcr.domain.order.adapter.event.IOrderEventPublisher;
import cn.fcr.domain.order.model.entity.ShopCartEntity;
import cn.fcr.domain.order.service.IOrderService;
import cn.fcr.domain.order.service.PayOrderService;
import cn.fcr.domain.shared.model.entity.PayOrderEntity;
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
    /** 旧订单领域服务 */
    private final IOrderService orderService;
    /** 支付单领域服务 */
    private final PayOrderService payOrderService;
    /** 订单事件发布器 */
    private final IOrderEventPublisher orderEventPublisher;
    /** 订单事务服务（内部使用） */
    private final OrderTransactionService orderTransactionService;

    public OrderApplicationService(IMallCartService mallCartService,
                                   IMallOrderService mallOrderService,
                                   IOrderPaymentGateway orderPaymentGateway,
                                   IOrderService orderService,
                                   PayOrderService payOrderService,
                                   IOrderEventPublisher orderEventPublisher,
                                   OrderTransactionService orderTransactionService) {
        this.mallCartService = mallCartService;
        this.mallOrderService = mallOrderService;
        this.orderPaymentGateway = orderPaymentGateway;
        this.orderService = orderService;
        this.payOrderService = payOrderService;
        this.orderEventPublisher = orderEventPublisher;
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

    // ==================== 旧 Order Domain ====================

    /**
     * 创建支付订单（旧域）
     *
     * @param userId    用户ID
     * @param productId 商品ID
     * @return 支付订单实体
     * @throws Exception 创建失败
     */
    public PayOrderEntity createPayOrder(String userId, String productId) throws Exception {
        ShopCartEntity shopCartEntity = ShopCartEntity.builder()
                .userId(userId)
                .productId(productId)
                .build();
        return orderService.createOrder(shopCartEntity);
    }

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
     * 订单支付成功处理（旧域）
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
     * @return 订单ID列表
     */
    public List<String> queryNoPayNotifyOrder() {
        return orderService.queryNoPayNotifyOrder();
    }

    /**
     * 处理超时关单
     *
     * @param orderNo 订单号
     * @return true表示关闭成功
     */
    @Transactional(rollbackFor = Exception.class)
    public boolean handleTimeoutCloseOrder(String orderNo) {
        return orderService.handleTimeoutCloseOrder(orderNo);
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
