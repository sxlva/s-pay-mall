package cn.fcr.test;

import cn.fcr.application.OrderApplicationService;
import cn.fcr.domain.mall.product.gateway.IIdempotentGateway;
import cn.fcr.domain.order.adapter.event.PaySuccessMessageEvent;
import cn.fcr.domain.order.model.valobj.OrderCreateVO;
import cn.fcr.infrastructure.dao.auth.IUserBindingDao;
import cn.fcr.infrastructure.dao.auth.po.UserBinding;
import cn.fcr.infrastructure.dao.mall.ICartItemDao;
import cn.fcr.infrastructure.dao.mall.IProductDao;
import cn.fcr.infrastructure.dao.mall.po.CartItem;
import cn.fcr.infrastructure.dao.mall.po.Product;
import cn.fcr.infrastructure.dao.order.IOrderDao;
import cn.fcr.infrastructure.dao.order.IOrderItemDao;
import cn.fcr.infrastructure.dao.order.IOrderMainDao;
import cn.fcr.infrastructure.dao.order.po.OrderItem;
import cn.fcr.infrastructure.dao.order.po.OrderMain;
import cn.fcr.infrastructure.dao.order.po.PayOrder;
import cn.fcr.infrastructure.auth.login.gateway.WeixinGatewayImpl;
import cn.fcr.trigger.listener.OrderPaidRocketListener;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.extern.slf4j.Slf4j;
import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.junit4.SpringRunner;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * 下单与支付通知幂等 E2E 测试（P0-4 验收）
 *
 * <p>场景 1：同 requestId 串行重发 → 返回首次创建的订单，只生成一笔订单；
 * 场景 2：无 requestId 并发双击（10 线程同刻下单）→ 用户级短锁只放行一笔；
 * 场景 3：order_paid 消息重复消费 → 微信通知只发送一次。</p>
 *
 * <p>依赖真实环境：MySQL(127.0.0.1:23306, db=s-pay-mall)、
 * Redis(127.0.0.1:26379, db=1)。测试数据通过唯一 orderNo / 商品 / requestId 隔离，
 * @After 物理清理（幂等 Key 随 24h 过期，测试值全局唯一无需清理）。</p>
 *
 * @author 傅崇睿
 */
@Slf4j
@RunWith(SpringRunner.class)
@SpringBootTest
public class OrderCreateIdempotencyE2ETest {

    /** Redis 库存 Key 前缀（与 StockGatewayImpl 一致） */
    private static final String STOCK_KEY_PREFIX = "mall:product:stock:";

    /**
     * 测试订单所属用户：复用现有 mall_user(id=1)。
     * order_main.user_id 存在外键 fk_order_user → mall_user(id)。
     */
    private static final Long TEST_USER_ID = 1L;

    @Autowired
    private OrderApplicationService orderApplicationService;

    @Autowired
    private OrderPaidRocketListener orderPaidRocketListener;

    /** 唯一真正调用微信服务器的网关，Mock 为空实现；场景 3 用 verify 断言通知次数 */
    @MockBean
    private WeixinGatewayImpl weixinGatewayImpl;

    @Resource
    private IOrderMainDao orderMainDao;

    @Resource
    private IOrderItemDao orderItemDao;

    @Resource
    private IOrderDao orderDao;

    @Resource
    private IProductDao productDao;

    @Resource
    private ICartItemDao cartItemDao;

    @Resource
    private IUserBindingDao userBindingDao;

    @Resource
    private RedissonClient redissonClient;

    /** JdbcTemplate：仅用于 @After 物理清理 pay_order（IOrderDao 无 delete 方法） */
    @Resource
    private org.springframework.jdbc.core.JdbcTemplate jdbcTemplate;

    /** 本测试类创建的订单号与商品ID，用于 @After 物理清理 */
    private final List<String> testOrderNos = new ArrayList<>();
    private final List<Long> testProductIds = new ArrayList<>();
    private final List<Long> testCartItemIds = new ArrayList<>();
    private final List<Long> testBindingIds = new ArrayList<>();

    /**
     * 每个测试方法后清理：物理删除测试订单/商品/购物车/绑定数据、删除 Redis 库存 Key
     */
    @After
    public void cleanup() {
        for (String orderNo : testOrderNos) {
            OrderMain orderMain = queryOrderMain(orderNo);
            if (orderMain != null) {
                LambdaQueryWrapper<OrderItem> itemWrapper = new LambdaQueryWrapper<>();
                itemWrapper.eq(OrderItem::getOrderId, orderMain.getId());
                orderItemDao.delete(itemWrapper);
                orderMainDao.deleteById(orderMain.getId());
            }
            jdbcTemplate.update("delete from pay_order where order_id = ?", orderNo);
        }
        testOrderNos.clear();
        // 先删购物车条目（cart_item.product_id 有外键 fk_cart_product），再删商品
        for (Long cartItemId : testCartItemIds) {
            cartItemDao.deleteById(cartItemId);
        }
        testCartItemIds.clear();
        for (Long productId : testProductIds) {
            redissonClient.getAtomicLong(STOCK_KEY_PREFIX + productId).delete();
            productDao.deleteById(productId);
        }
        testProductIds.clear();
        for (Long bindingId : testBindingIds) {
            userBindingDao.deleteById(bindingId);
        }
        testBindingIds.clear();
    }

    /**
     * 场景 1：同 requestId 串行重发 → 返回首次订单，全库仅一笔
     *
     * <p>模拟前端网络重试：同一 requestId 连续两次调用 createOrder。
     * 硬标准：两次返回同一 orderNo；order_main/pay_order 各仅一行；
     * MySQL 库存只预扣一次。</p>
     *
     * @throws Exception 断言失败
     */
    @Test
    public void testCreateOrder_sameRequestId_replayReturnsSameOrder() throws Exception {
        int stockS0 = 100;
        Long productId = createTestProduct(stockS0);
        addCart(TEST_USER_ID, productId, 2);
        // 模拟真实下单前置：Redis 库存 Key 已预热（下单只预扣 Redis，MySQL 扣减发生在支付成功）
        redissonClient.getAtomicLong(STOCK_KEY_PREFIX + productId).set(stockS0);
        String requestId = "P04-REQ-" + UUID.randomUUID().toString().substring(0, 8);

        OrderCreateVO first = orderApplicationService.createOrder(TEST_USER_ID, "幂等测试地址", requestId);
        assertNotNull("首次下单应成功", first);
        assertNotNull("首次下单应生成订单号", first.getOrderNo());
        testOrderNos.add(first.getOrderNo());

        // 同一 requestId 重发（网络重试/双击后的二次提交）
        OrderCreateVO replay = orderApplicationService.createOrder(TEST_USER_ID, "幂等测试地址", requestId);
        assertEquals("同 requestId 重发必须返回首次创建的订单号",
                first.getOrderNo(), replay.getOrderNo());
        assertEquals("重发返回的金额应与首次一致", first.getTotalAmount(), replay.getTotalAmount());

        // 全库仅一笔订单
        assertNotNull("订单必须真实存在", queryOrderMain(first.getOrderNo()));
        assertEquals("pay_order 必须仅有一行", 1,
                jdbcTemplate.queryForObject("select count(*) from pay_order where order_id = ?",
                        Integer.class, first.getOrderNo()).intValue());
        assertEquals("下单只预扣 Redis 库存一次", stockS0 - 2,
                redissonClient.getAtomicLong(STOCK_KEY_PREFIX + productId).get());
        assertEquals("MySQL 库存在支付成功前不得扣减", Integer.valueOf(stockS0),
                productDao.selectById(productId).getStock());
    }

    /**
     * 场景 2：无 requestId 并发双击（10 线程同刻下单）→ 用户级短锁只放行一笔
     *
     * <p>模拟无幂等键场景的用户双击/并发重发：10 线程同一瞬间对同一用户下单。
     * 硬标准：恰好 1 个线程成功、其余 9 个被幂等锁拒绝（IllegalStateException）；
     * Redis 库存只预扣一次（MySQL 扣减发生在支付成功，此处不验证）。</p>
     *
     * @throws Exception 断言失败
     */
    @Test
    public void testCreateOrder_concurrentDoubleClick_userLockAllowsOnlyOne() throws Exception {
        int stockS0 = 100;
        Long productId = createTestProduct(stockS0);
        addCart(TEST_USER_ID, productId, 2);
        // 模拟真实下单前置：Redis 库存 Key 已预热（下单只预扣 Redis，MySQL 扣减发生在支付成功）
        redissonClient.getAtomicLong(STOCK_KEY_PREFIX + productId).set(stockS0);

        int threadCount = 10;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch ready = new CountDownLatch(threadCount);
        CountDownLatch fire = new CountDownLatch(1);
        List<Future<String>> futures = new ArrayList<>();
        for (int i = 0; i < threadCount; i++) {
            futures.add(executor.submit(() -> {
                ready.countDown();
                fire.await();
                try {
                    OrderCreateVO vo = orderApplicationService.createOrder(TEST_USER_ID, "并发双击测试地址", null);
                    return "OK:" + vo.getOrderNo();
                } catch (IllegalStateException e) {
                    return "REJECTED:" + e.getMessage();
                }
            }));
        }
        assertTrue("并发线程未在超时时间内就绪", ready.await(10, TimeUnit.SECONDS));
        fire.countDown();

        AtomicInteger okCount = new AtomicInteger();
        AtomicInteger rejectedCount = new AtomicInteger();
        String successOrderNo = null;
        for (Future<String> future : futures) {
            String result = future.get(30, TimeUnit.SECONDS);
            if (result.startsWith("OK:")) {
                okCount.incrementAndGet();
                successOrderNo = result.substring(3);
            } else {
                rejectedCount.incrementAndGet();
            }
        }
        executor.shutdown();

        assertEquals("并发双击恰好只能成功一笔", 1, okCount.get());
        assertEquals("其余 9 个请求必须被幂等锁拒绝", threadCount - 1, rejectedCount.get());

        assertNotNull("成功的订单必须存在", queryOrderMain(successOrderNo));
        testOrderNos.add(successOrderNo);
        assertEquals("并发下 Redis 库存只能预扣一次", stockS0 - 2,
                redissonClient.getAtomicLong(STOCK_KEY_PREFIX + productId).get());
        assertEquals("MySQL 库存在支付成功前不得扣减", Integer.valueOf(stockS0),
                productDao.selectById(productId).getStock());
    }

    /**
     * 场景 3：order_paid 消息重复消费 → 微信通知只发送一次
     *
     * <p>模拟 RocketMQ at-least-once 重投：同一 orderNo 的支付成功消息
     * 连续消费两次。硬标准：{@code sendPaymentSuccessNotification} 仅被调用一次；
     * 第二次消费被幂等守门直接跳过。</p>
     *
     * @throws Exception 断言失败
     */
    @Test
    public void testOrderPaidListener_duplicateConsume_notifyOnlyOnce() throws Exception {
        int stockS0 = 100;
        int quantity = 2;
        Long productId = createTestProduct(stockS0);
        String orderNo = uniqueOrderNo("P04");
        createNewChainOrder(orderNo, productId, quantity);
        testOrderNos.add(orderNo);
        // 用户绑定微信，通知链路可达（真实 send 已被 Mock 为空实现）
        bindWechat(TEST_USER_ID);

        PaySuccessMessageEvent.PaySuccessMessage message = new PaySuccessMessageEvent.PaySuccessMessage();
        message.setUserId(String.valueOf(TEST_USER_ID));
        message.setOrderNo(orderNo);
        message.setTradeNo("TRADE-P04-" + UUID.randomUUID().toString().substring(0, 8));

        // 第一次消费：正常处理并发送通知
        orderPaidRocketListener.onMessage(message);
        // 第二次消费：模拟 MQ 重投，必须被消费幂等跳过
        orderPaidRocketListener.onMessage(message);

        verify(weixinGatewayImpl, times(1))
                .sendPaymentSuccessNotification(anyString(), anyString(), anyString(), anyString(), anyString());
    }

    /* ==================== 测试数据构造工具 ==================== */

    /**
     * 创建独立测试商品（真实写入 product 表）
     *
     * @param stock 初始库存
     * @return 商品ID
     */
    private Long createTestProduct(int stock) {
        Product product = new Product();
        product.setCategoryId(1L);
        product.setName("P04幂等测试商品-" + UUID.randomUUID().toString().substring(0, 8));
        product.setDescription("P0-4 幂等 E2E 测试专用商品");
        product.setPrice(new BigDecimal("9.95"));
        product.setStock(stock);
        product.setStatus(1);
        product.setCreateTime(LocalDateTime.now());
        product.setUpdateTime(LocalDateTime.now());
        productDao.insert(product);
        testProductIds.add(product.getId());
        return product.getId();
    }

    /**
     * 添加购物车条目（真实写入 cart_item 表）
     */
    private void addCart(Long userId, Long productId, int quantity) {
        CartItem item = new CartItem();
        item.setUserId(userId);
        item.setProductId(productId);
        item.setQuantity(quantity);
        item.setCreateTime(LocalDateTime.now());
        item.setUpdateTime(LocalDateTime.now());
        cartItemDao.insert(item);
        testCartItemIds.add(item.getId());
    }

    /**
     * 创建新链订单（order_main + order_item + pay_order，真实写入 MySQL）
     */
    private void createNewChainOrder(String orderNo, Long productId, int quantity) {
        OrderMain orderMain = new OrderMain();
        orderMain.setOrderNo(orderNo);
        orderMain.setUserId(TEST_USER_ID);
        orderMain.setTotalAmount(new BigDecimal("19.90"));
        orderMain.setStatus("CREATED");
        orderMain.setAddress("P04测试地址");
        orderMain.setCreateTime(LocalDateTime.now());
        orderMain.setUpdateTime(LocalDateTime.now());
        orderMainDao.insert(orderMain);

        OrderItem item = new OrderItem();
        item.setOrderId(orderMain.getId());
        item.setProductId(productId);
        item.setProductName("P04幂等测试商品");
        item.setPrice(new BigDecimal("9.95"));
        item.setQuantity(quantity);
        item.setCreateTime(LocalDateTime.now());
        orderItemDao.insert(item);

        PayOrder payOrder = PayOrder.builder()
                .userId(String.valueOf(TEST_USER_ID))
                .productId(String.valueOf(productId))
                .productName("P04幂等测试商品")
                .orderId(orderNo)
                .orderTime(new Date())
                .totalAmount(new BigDecimal("19.90"))
                .status("WAIT_PAY")
                .createTime(new Date())
                .updateTime(new Date())
                .build();
        orderDao.insert(payOrder);

        // 模拟下单时的 Redis 预扣
        redissonClient.getAtomicLong(STOCK_KEY_PREFIX + productId).set(100 - quantity);
    }

    /**
     * 为测试用户绑定微信（真实写入 user_binding 表，使支付通知链路可达）
     */
    private void bindWechat(Long userId) {
        UserBinding binding = new UserBinding();
        binding.setUserId(userId);
        binding.setIdentityType("WECHAT_MP");
        binding.setIdentifier("mock_openid_p04_" + UUID.randomUUID().toString().substring(0, 8));
        binding.setCredential("");
        binding.setCreateTime(LocalDateTime.now());
        userBindingDao.insert(binding);
        testBindingIds.add(binding.getId());
    }

    /**
     * 按订单号查询 order_main
     */
    private OrderMain queryOrderMain(String orderNo) {
        LambdaQueryWrapper<OrderMain> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(OrderMain::getOrderNo, orderNo);
        return orderMainDao.selectOne(wrapper);
    }

    /**
     * 生成唯一订单号
     */
    private String uniqueOrderNo(String prefix) {
        return prefix + UUID.randomUUID().toString().replace("-", "").substring(0, 20);
    }
}
