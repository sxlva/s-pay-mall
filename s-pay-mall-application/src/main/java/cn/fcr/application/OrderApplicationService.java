package cn.fcr.application;

import cn.fcr.domain.mall.cart.model.valobj.CartItemVO;
import cn.fcr.domain.mall.cart.service.IMallCartService;
import cn.fcr.domain.mall.product.gateway.IIdempotentGateway;
import cn.fcr.domain.mall.user.gateway.IUserBindingGateway;
import cn.fcr.domain.order.adapter.event.IOrderEventPublisher;
import cn.fcr.domain.order.gateway.IOrderPaymentGateway;
import cn.fcr.domain.order.gateway.IPayOrderGateway;
import cn.fcr.domain.order.gateway.IMallOrderQueryGateway;
import cn.fcr.domain.order.model.entity.OrderEntity;
import cn.fcr.domain.order.model.entity.OrderState;
import cn.fcr.domain.order.model.valobj.OrderCreateVO;
import cn.fcr.domain.order.model.valobj.OrderVO;
import cn.fcr.domain.order.model.vo.PayTradeStatus;
import cn.fcr.domain.order.service.IMallOrderService;
import cn.fcr.domain.order.service.IOrderStateMachineService;
import cn.fcr.domain.order.service.PayOrderService;
import cn.fcr.domain.order.gateway.IAlipayQueryGateway;
import cn.fcr.domain.auth.login.gateway.IWeChatGateway;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
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
    /** 订单查询网关（P1-3：支付成功通知编排） */
    private final IMallOrderQueryGateway mallOrderQueryGateway;
    /** 用户绑定网关（P1-3：查询微信 openid） */
    private final IUserBindingGateway userBindingGateway;
    /** 微信网关（P1-3：发送模板消息） */
    private final IWeChatGateway weChatGateway;
    /** 支付宝交易查询网关（B2：超时关单前的二次确认） */
    private final IAlipayQueryGateway alipayQueryGateway;

    /** 支付时间格式化器 */
    private static final DateTimeFormatter PAY_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public OrderApplicationService(IMallCartService mallCartService,
                                   IMallOrderService mallOrderService,
                                   IOrderPaymentGateway orderPaymentGateway,
                                   PayOrderService payOrderService,
                                   IOrderEventPublisher orderEventPublisher,
                                   IOrderStateMachineService orderStateMachineService,
                                   IPayOrderGateway payOrderGateway,
                                   OrderTransactionService orderTransactionService,
                                   IIdempotentGateway idempotentGateway,
                                   IMallOrderQueryGateway mallOrderQueryGateway,
                                   IUserBindingGateway userBindingGateway,
                                   IWeChatGateway weChatGateway,
                                   IAlipayQueryGateway alipayQueryGateway) {
        this.mallCartService = mallCartService;
        this.mallOrderService = mallOrderService;
        this.orderPaymentGateway = orderPaymentGateway;
        this.payOrderService = payOrderService;
        this.orderEventPublisher = orderEventPublisher;
        this.orderStateMachineService = orderStateMachineService;
        this.payOrderGateway = payOrderGateway;
        this.orderTransactionService = orderTransactionService;
        this.idempotentGateway = idempotentGateway;
        this.mallOrderQueryGateway = mallOrderQueryGateway;
        this.userBindingGateway = userBindingGateway;
        this.weChatGateway = weChatGateway;
        this.alipayQueryGateway = alipayQueryGateway;
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
     * <p>P1-2：事务内只做 DB 状态流转（{@link OrderTransactionService#cancelOrderInTransaction}），
     * 库存恢复在事务提交后执行，避免 DB 回滚时 Redis 库存无法回滚导致超卖。</p>
     *
     * @param orderId 订单ID
     * @return 影响行数
     */
    public int cancelOrder(Long orderId) {
        String orderNo = orderTransactionService.cancelOrderInTransaction(orderId);
        if (orderNo == null) {
            return 0;
        }
        restoreStockAfterCancel(orderNo);
        return 1;
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

        String orderNo = params.get("out_trade_no");
        // 支付宝交易号（trade_no）与商户订单号（out_trade_no）是两个字段：
        // 前者是支付宝侧流水号，随支付成功事件透传给 order_paid 消费者；后者用于本地履约
        String alipayTradeNo = params.get("trade_no");
        log.info("支付回调，验签通过，交易名称: {}, 商户订单号: {}, 支付宝交易号: {}, 交易金额: {}",
                params.get("subject"), orderNo, alipayTradeNo, params.get("total_amount"));

        changeOrderPaySuccess(orderNo, alipayTradeNo);
        return true;
    }

    /**
     * 订单支付成功处理
     *
     * <p>事务边界在 OrderTransactionService 中控制，事件发布在事务外执行。</p>
     *
     * @param orderNo 商户订单号（order_main.order_no）
     * @param tradeNo 支付宝交易号（回调 trade_no；补单等无交易号的场景可为 null）
     */
    public void changeOrderPaySuccess(String orderNo, String tradeNo) {
        // 【B2 晚到支付异常路径】订单已关闭仍收到支付成功通知：钱已收但订单不可履约，
        // 明确记录待人工介入（退款或补履约），不触发履约、不发布支付成功事件；
        // 仍由 Controller 回支付宝 success 以终止其通知重试（重试只会重复进入本路径）
        OrderVO order = mallOrderService.getOrderByNo(orderNo);
        if (order != null && OrderState.CANCELED.getCode().equals(order.getStatus())) {
            log.error("晚到支付：订单已关闭仍收到支付成功通知，需人工核实退款或补履约, orderNo={}, tradeNo={}", orderNo, tradeNo);
            return;
        }

        // 事务内完成 DB 状态更新
        orderTransactionService.changeOrderPaySuccessInTransaction(orderNo);

        // 事务提交后发布支付成功事件（失败不影响主流程）
        try {
            orderEventPublisher.publishPaySuccess(tradeNo, orderNo);
        } catch (Exception e) {
            log.warn("发布支付成功事件失败，orderNo: {}, error: {}", orderNo, e.getMessage());
        }
    }

    /**
     * 发送支付成功微信模板消息通知（P1-3：业务编排从 Listener 下沉到应用层）
     *
     * <p>按序编排：查订单 → 查微信 openid → 发送模板消息。
     * 订单不存在或未绑定微信时静默跳过；发送失败仅记日志，不影响消费 ACK。</p>
     *
     * @param orderNo 订单号
     */
    public void sendPaySuccessNotification(String orderNo) {
        try {
            OrderEntity order = mallOrderQueryGateway.findByOrderNo(orderNo);
            if (order == null) {
                log.warn("发送支付通知失败：订单不存在，orderNo={}", orderNo);
                return;
            }

            String openid = userBindingGateway.getWeChatOpenIdByUserId(order.getUserId());
            if (openid == null) {
                log.info("用户未绑定微信，跳过支付通知推送，userId={}", order.getUserId());
                return;
            }

            String productName = order.getItems() != null && !order.getItems().isEmpty()
                    ? order.getItems().get(0).getProductName()
                    : "商品";
            String amount = order.getTotalAmount() != null ? order.getTotalAmount().toString() : "0";
            String payTime = LocalDateTime.now().format(PAY_TIME_FORMATTER);

            weChatGateway.sendPaymentSuccessNotification(openid, productName, orderNo, amount, payTime);
            log.info("支付成功微信通知发送成功，orderNo={}, openid={}", orderNo, openid);

        } catch (Exception e) {
            log.error("发送支付成功微信通知异常，orderNo={}", orderNo, e);
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
     * （仅 DB 状态流转，状态机守卫保证幂等）；
     * order_main 不存在的订单记 warn 日志并返回 false（legacy 已下线，
     * 不影响 MQ 消费重试语义）。</p>
     *
     * <p>P1-2：库存恢复在事务提交后执行，与主动取消订单一致。</p>
     *
     * <p>【B2 关单前二次确认】关单前先向支付宝查询交易状态，已支付则转履约路径不关闭。</p>
     *
     * @param orderNo 订单号
     * @return true表示关闭成功或已转履约
     */
    public boolean handleTimeoutCloseOrder(String orderNo) {
        OrderVO order = mallOrderService.getOrderByNo(orderNo);
        if (order == null) {
            log.warn("超时关单：order_main 不存在，忽略处理: orderNo={}", orderNo);
            return false;
        }
        // 【B2 关单前二次确认】关单瞬间用户可能已完成支付（回调/补单尚未到达），
        // 先向支付宝确认交易状态；已支付则转履约路径，杜绝"订单已关闭但钱已收"的死路
        if (alipayQueryGateway.queryTradeSuccess(orderNo)) {
            log.warn("超时关单：支付宝侧交易已成功，转履约路径不关闭订单: orderNo={}", orderNo);
            changeOrderPaySuccess(orderNo, null);
            return true;
        }
        boolean canceled = orderTransactionService.cancelOrderInTransaction(orderNo);
        if (canceled) {
            restoreStockAfterCancel(orderNo);
        }
        return canceled;
    }

    /**
     * 取消事务提交后恢复 Redis 预扣库存（P1-2）
     *
     * <p>失败仅记 error 日志不回滚：订单已取消为终态，库存未恢复只损失可售库存，不构成超卖。</p>
     *
     * @param orderNo 订单号
     */
    private void restoreStockAfterCancel(String orderNo) {
        try {
            orderStateMachineService.restoreStockForCancel(orderNo);
        } catch (Exception e) {
            log.error("取消订单后恢复库存失败，需人工补偿, orderNo={}", orderNo, e);
        }
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
