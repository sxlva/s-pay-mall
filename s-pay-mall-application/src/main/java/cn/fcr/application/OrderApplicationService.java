package cn.fcr.application;

import cn.fcr.domain.mall.cart.model.valobj.CartItemVO;
import cn.fcr.domain.mall.cart.service.IMallCartService;
import cn.fcr.domain.mall.product.gateway.IIdempotentGateway;
import cn.fcr.domain.order.adapter.event.IOrderEventPublisher;
import cn.fcr.domain.order.gateway.IOrderPaymentGateway;
import cn.fcr.domain.order.gateway.IPayOrderGateway;
import cn.fcr.domain.order.model.valobj.OrderCreateVO;
import cn.fcr.domain.order.model.valobj.OrderVO;
import cn.fcr.domain.order.model.vo.PayTradeStatus;
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
    /** 幂等网关（P0-4：下单幂等守门） */
    private final IIdempotentGateway idempotentGateway;

    public OrderApplicationService(IMallCartService mallCartService,
                                   IMallOrderService mallOrderService,
                                   IOrderPaymentGateway orderPaymentGateway,
                                   PayOrderService payOrderService,
                                   IOrderEventPublisher orderEventPublisher,
                                   IOrderStateMachineService orderStateMachineService,
                                   IPayOrderGateway payOrderGateway,
                                   OrderTransactionService orderTransactionService,
                                   IIdempotentGateway idempotentGateway) {
        this.mallCartService = mallCartService;
        this.mallOrderService = mallOrderService;
        this.orderPaymentGateway = orderPaymentGateway;
        this.payOrderService = payOrderService;
        this.orderEventPublisher = orderEventPublisher;
        this.orderStateMachineService = orderStateMachineService;
        this.payOrderGateway = payOrderGateway;
        this.orderTransactionService = orderTransactionService;
        this.idempotentGateway = idempotentGateway;
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
     * 创建订单（P0-4 幂等保护）
     *
     * <p>事务边界在 OrderTransactionService 中控制，MQ 消息发送在事务外执行，
     * 避免 sendDelayCloseMessage 在事务内部导致事务 hold 问题。</p>
     *
     * <p>幂等语义：
     * <ul>
     *   <li>requestId 非空：以 requestId 为幂等键（24h），重复请求返回首次创建的订单；
     *       首次请求仍在处理中时抛异常提示稍后查询</li>
     *   <li>requestId 为空：降级为用户级短锁（10s），仅防双击/并发重发，不承诺跨请求幂等</li>
     * </ul>
     * 执行异常时释放幂等键，允许用户重试。</p>
     *
     * @param userId    用户ID
     * @param address   收货地址
     * @param requestId 幂等键（客户端生成的 UUID，可为空）
     * @return 订单创建结果，含orderNo和payUrl
     */
    public OrderCreateVO createOrder(Long userId, String address, String requestId) {
        // ===== P0-4 幂等守门 =====
        boolean hasRequestId = requestId != null && !requestId.isBlank();
        String idemNo = hasRequestId ? requestId : "uid:" + userId;
        long ttlSeconds = hasRequestId ? 24 * 3600 : 10;
        boolean acquired = idempotentGateway.tryAcquire(
                IIdempotentGateway.BUSINESS_TYPE_ORDER_CREATE, idemNo, ttlSeconds);
        if (!acquired) {
            if (hasRequestId) {
                // 同 requestId 重发：返回首次创建的订单（payUrl 走 continue-pay 重新获取）
                String doneOrderNo = idempotentGateway.getValue(
                        IIdempotentGateway.BUSINESS_TYPE_ORDER_CREATE, idemNo);
                if (doneOrderNo != null && !IIdempotentGateway.PROCESSING_VALUE.equals(doneOrderNo)) {
                    OrderVO existed = mallOrderService.getOrderByNo(doneOrderNo);
                    if (existed != null) {
                        log.info("创建订单幂等命中：同 requestId 返回已有订单，orderNo={}", doneOrderNo);
                        return OrderCreateVO.builder()
                                .orderNo(existed.getOrderNo())
                                .totalAmount(existed.getTotalAmount())
                                .status(existed.getStatus())
                                .build();
                    }
                }
            }
            throw new IllegalStateException("订单正在处理中或已提交，请勿重复下单");
        }

        try {
            // 事务内完成所有 DB 操作
            OrderCreateVO result = orderTransactionService.createOrderInTransaction(userId, address);

            // 事务提交后发送延时关闭消息（失败不影响主流程）
            try {
                orderPaymentGateway.sendDelayCloseMessage(result.getOrderNo());
            } catch (Exception e) {
                log.warn("发送延时关闭消息失败，orderNo: {}, error: {}", result.getOrderNo(), e.getMessage());
            }

            // 记录幂等结果：同 requestId 重发时返回本订单
            if (hasRequestId) {
                idempotentGateway.markDone(IIdempotentGateway.BUSINESS_TYPE_ORDER_CREATE, idemNo, result.getOrderNo());
            }
            return result;
        } catch (RuntimeException e) {
            // 创建失败：释放幂等键允许重试
            idempotentGateway.release(IIdempotentGateway.BUSINESS_TYPE_ORDER_CREATE, idemNo);
            throw e;
        }
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
     * 处理支付宝支付异步回调（P0-6：业务判断从 Trigger 层下沉）
     *
     * <p>由应用层按序完成：交易状态判断（{@link PayTradeStatus#isSuccess()}）→
     * 验签 → 订单履约。Controller 仅负责解析参数与回传 success/false。</p>
     *
     * @param params          支付宝回调参数
     * @param alipayPublicKey 支付宝公钥（验签）
     * @return true 表示已受理并触发履约，应回 "success"；false 表示忽略或验签失败，应回 "false"
     */
    public boolean handleAlipayCallback(Map<String, String> params, String alipayPublicKey) {
        String tradeStatusCode = params.get("trade_status");
        PayTradeStatus tradeStatus = PayTradeStatus.fromCode(tradeStatusCode);
        if (tradeStatus == null || !tradeStatus.isSuccess()) {
            log.info("支付回调，非成功状态忽略: trade_status={}", tradeStatusCode);
            return false;
        }

        boolean signVerified = payOrderService.verifyCallbackSign(params, alipayPublicKey);
        if (!signVerified) {
            log.error("支付回调，签名验证失败。公钥长度: {}, 公钥前100字符: {}, 接收参数: {}",
                    alipayPublicKey != null ? alipayPublicKey.length() : 0,
                    alipayPublicKey != null && alipayPublicKey.length() > 100 ? alipayPublicKey.substring(0, 100) : alipayPublicKey,
                    params);
            return false;
        }

        String tradeNo = params.get("out_trade_no");
        log.info("支付回调，验签通过，交易名称: {}, 商户订单号: {}, 交易金额: {}",
                params.get("subject"), tradeNo, params.get("total_amount"));

        changeOrderPaySuccess(tradeNo);
        return true;
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
