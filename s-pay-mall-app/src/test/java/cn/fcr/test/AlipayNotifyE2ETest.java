package cn.fcr.test;

import cn.fcr.infrastructure.dao.mall.IProductDao;
import cn.fcr.infrastructure.dao.mall.po.Product;
import cn.fcr.infrastructure.dao.order.IOrderDao;
import cn.fcr.infrastructure.dao.order.IOrderItemDao;
import cn.fcr.infrastructure.dao.order.IOrderMainDao;
import cn.fcr.infrastructure.dao.order.po.OrderItem;
import cn.fcr.infrastructure.dao.order.po.OrderMain;
import cn.fcr.infrastructure.dao.order.po.PayOrder;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.client.consumer.DefaultMQPushConsumer;
import org.apache.rocketmq.client.consumer.listener.ConsumeConcurrentlyStatus;
import org.apache.rocketmq.client.consumer.listener.MessageListenerConcurrently;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.redisson.api.RAtomicLong;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.Signature;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 支付宝支付回调 E2E Mock 测试
 *
 * <p>通过 MockMvc 将模拟支付宝异步通知的 POST 请求发送到
 * {@code /pay-api/v1/alipay/alipay_notify_url}，完整经过
 * Controller → ApplicationService → TransactionService →
 * OrderStateMachineServiceImpl → DAO → MySQL / Redis 现有链路。</p>
 *
 * <p>验签说明：项目生产验签为 RSA2（SHA256withRSA）纯参数校验，
 * 无时间戳/nonce 防重。本测试在测试环境自生成 RSA 密钥对，
 * 通过 {@link DynamicPropertySource} 将 {@code alipay.alipay_public_key}
 * 覆盖为本测试公钥，并用对应私钥按 {@code AlipayGatewayImpl.getSignCheckContentV1}
 * 的完全相同规则构造待签字符串后签名——
 * {@code AlipayGatewayImpl.verifySignature} 生产代码原样执行，未绕过验签。</p>
 *
 * <p>依赖真实环境：MySQL(127.0.0.1:23306, db=s-pay-mall)、
 * Redis(127.0.0.1:26379, db=1)、RocketMQ(127.0.0.1:9876)。
 * 测试数据通过唯一 orderNo / 独立测试商品隔离，@After 物理清理；
 * RocketMQ 已发布的消息无法回滚，属已知限制。</p>
 *
 * @author 傅崇睿
 */
@Slf4j
@RunWith(SpringRunner.class)
@SpringBootTest
@AutoConfigureMockMvc
public class AlipayNotifyE2ETest {

    /** 支付宝异步通知地址（与 AliPayController 路径一致，dev profile api-version=v1） */
    private static final String NOTIFY_URL = "/pay-api/v1/alipay/alipay_notify_url";

    /** RocketMQ order_paid 主题 */
    private static final String TOPIC_ORDER_PAID = "order_paid";

    /** Redis 库存 Key 前缀（与 StockGatewayImpl 一致） */
    private static final String STOCK_KEY_PREFIX = "mall:product:stock:";

    /**
     * 测试订单所属用户：复用现有 mall_user(id=1)。
     * order_main.user_id 存在外键 fk_order_user → mall_user(id)，
     * 测试不新建用户、不修改用户数据，订单行在 @After 物理删除。
     */
    private static final Long TEST_USER_ID = 1L;

    /** 测试用 RSA 密钥对（静态初始化一次，供签名与公钥覆盖使用） */
    private static final KeyPair TEST_KEY_PAIR = generateRsaKeyPair();

    @Autowired
    private MockMvc mockMvc;

    @Resource
    private IOrderMainDao orderMainDao;

    @Resource
    private IOrderItemDao orderItemDao;

    @Resource
    private IOrderDao orderDao;

    @Resource
    private IProductDao productDao;

    @Resource
    private RedissonClient redissonClient;

    /** JdbcTemplate：仅用于 @After 物理清理 pay_order（IOrderDao 无 delete 方法） */
    @Resource
    private org.springframework.jdbc.core.JdbcTemplate jdbcTemplate;

    /** 事件验证消费者：独立 consumer group 订阅 order_paid，收集收到的 orderNo */
    private DefaultMQPushConsumer eventVerifierConsumer;

    /** 事件验证消费者收到的 orderNo 集合 */
    private final Set<String> receivedOrderNos = new CopyOnWriteArraySet<>();

    /** 本测试类创建的订单号与商品ID，用于 @After 物理清理 */
    private final List<String> testOrderNos = new ArrayList<>();
    private final List<Long> testProductIds = new ArrayList<>();

    /**
     * 将测试公钥注入 alipay.alipay_public_key，
     * 使生产验签代码能用测试私钥签出的签名通过校验。
     * 仅作用于本测试 Spring 上下文，不修改任何生产配置。
     *
     * @param registry 动态属性注册器
     */
    @DynamicPropertySource
    static void overrideAlipayPublicKey(DynamicPropertyRegistry registry) {
        registry.add("alipay.alipay_public_key",
                () -> Base64.getEncoder().encodeToString(TEST_KEY_PAIR.getPublic().getEncoded()));
    }

    /**
     * 每个测试方法前启动 order_paid 事件验证消费者
     *
     * @throws Exception 消费者启动失败
     */
    @Before
    public void startEventVerifier() throws Exception {
        eventVerifierConsumer = new DefaultMQPushConsumer("test-order-paid-verifier-" + UUID.randomUUID());
        eventVerifierConsumer.setNamesrvAddr("127.0.0.1:9876");
        // 每个测试方法都使用全新随机 consumer group，从头消费已保留消息，
        // 避免"消息先于消费者注册发布"导致的竞态丢消息
        eventVerifierConsumer.setConsumeFromWhere(org.apache.rocketmq.common.consumer.ConsumeFromWhere.CONSUME_FROM_FIRST_OFFSET);
        eventVerifierConsumer.subscribe(TOPIC_ORDER_PAID, "*");
        eventVerifierConsumer.registerMessageListener((MessageListenerConcurrently) (msgs, context) -> {
            for (org.apache.rocketmq.common.message.MessageExt msg : msgs) {
                String body = new String(msg.getBody(), StandardCharsets.UTF_8);
                log.info("【测试事件验证】收到 order_paid 消息: {}", body);
                for (String orderNo : new HashSet<>(testOrderNos)) {
                    if (body.contains(orderNo)) {
                        receivedOrderNos.add(orderNo);
                    }
                }
            }
            return ConsumeConcurrentlyStatus.CONSUME_SUCCESS;
        });
        eventVerifierConsumer.start();
        receivedOrderNos.clear();
        awaitRouteReady(10000);
    }

    /**
     * 等待事件验证消费者完成 topic 路由拉取与订阅就绪，
     * 防止消息在消费者订阅完成前发布而丢失。
     *
     * @param timeoutMs 超时毫秒
     * @throws Exception 路由查询失败
     */
    private void awaitRouteReady(long timeoutMs) throws Exception {
        long deadline = System.currentTimeMillis() + timeoutMs;
        while (System.currentTimeMillis() < deadline) {
            try {
                if (!eventVerifierConsumer.fetchSubscribeMessageQueues(TOPIC_ORDER_PAID).isEmpty()) {
                    return;
                }
            } catch (Exception e) {
                log.warn("等待 order_paid 路由就绪中: {}", e.getMessage());
            }
            Thread.sleep(300);
        }
        throw new IllegalStateException("order_paid 消费者路由在超时时间内未就绪");
    }

    /**
     * 每个测试方法后清理：物理删除测试订单/商品数据、删除 Redis 库存 Key、关闭事件消费者。
     * 说明：RocketMQ 已发布的 order_paid 消息无法回滚，消息体仅含测试 orderNo，无业务副作用。
     */
    @After
    public void cleanup() throws Exception {
        for (String orderNo : testOrderNos) {
            OrderMain orderMain = queryOrderMain(orderNo);
            if (orderMain != null) {
                LambdaQueryWrapper<OrderItem> itemWrapper = new LambdaQueryWrapper<>();
                itemWrapper.eq(OrderItem::getOrderId, orderMain.getId());
                orderItemDao.delete(itemWrapper);
                orderMainDao.deleteById(orderMain.getId());
            }
            // IOrderDao 为纯注解 Mapper（非 BaseMapper），无 delete 方法，测试清理用 JdbcTemplate 物理删除
            jdbcTemplate.update("delete from pay_order where order_id = ?", orderNo);
        }
        testOrderNos.clear();
        for (Long productId : testProductIds) {
            redissonClient.getAtomicLong(STOCK_KEY_PREFIX + productId).delete();
            productDao.deleteById(productId);
        }
        testProductIds.clear();
        if (eventVerifierConsumer != null) {
            eventVerifierConsumer.shutdown();
        }
    }

    /**
     * 场景 1：新订单支付成功回调
     *
     * <p>准备 order_main(CREATED) + order_item + pay_order(WAIT_PAY)，
     * Redis 预扣库存。发送一次合法 notify，验证 order_main=PAID、
     * pay_order=PAID、MySQL 库存 S0-2、Redis 库存不变、order_paid 已发布。</p>
     *
     * @throws Exception 请求或断言失败
     */
    @Test
    public void testPayNotify_newOrderPaySuccess() throws Exception {
        // ========== 准备真实新订单 ==========
        int quantity = 2;
        int stockS0 = 100;
        Long productId = createTestProduct(stockS0);
        String orderNo = uniqueOrderNo("S1");
        createNewChainOrder(orderNo, productId, "M1E2E测试商品", quantity,
                new BigDecimal("19.90"), "WAIT_PAY", "CREATED");

        // 模拟下单时 Redis 预扣（OrderTransactionService.createOrderInTransaction → checkAndDeductStock）
        redissonClient.getAtomicLong(STOCK_KEY_PREFIX + productId).set(stockS0 - quantity);
        long redisStockBefore = redissonClient.getAtomicLong(STOCK_KEY_PREFIX + productId).get();
        assertEquals(stockS0 - quantity, redisStockBefore);
        assertEquals(Integer.valueOf(stockS0), productDao.selectById(productId).getStock());

        // ========== 构造合法支付宝 notify 参数（真实 RSA2 签名） ==========
        Map<String, String> notifyParams = buildSignedNotifyParams(orderNo, "19.90");

        // ========== 发送一次 POST /pay-api/v1/alipay/alipay_notify_url ==========
        ResultActions result = sendNotify(notifyParams);
        result.andExpect(status().isOk())
                .andExpect(content().string("success"));

        // ========== 断言 1：order_main = PAID ==========
        OrderMain orderMain = queryOrderMain(orderNo);
        assertNotNull("order_main 应存在", orderMain);
        assertEquals("order_main 状态应为 PAID", "PAID", orderMain.getStatus());

        // ========== 断言 2：pay_order = PAID 且已记录支付时间（项目实际状态值，非 PAY_SUCCESS） ==========
        PayOrder payOrder = orderDao.queryByOrderNo(orderNo);
        assertNotNull("pay_order 应存在", payOrder);
        assertEquals("pay_order 状态应为 PAID", "PAID", payOrder.getStatus());
        assertNotNull("pay_order.pay_time 应已写入", payOrder.getPayTime());

        // ========== 断言 3：MySQL 库存 = S0 - 2 ==========
        assertEquals("MySQL 库存应扣减一次", Integer.valueOf(stockS0 - quantity),
                productDao.selectById(productId).getStock());

        // ========== 断言 4：Redis 库存与预扣值一致（支付成功链路不重复扣 Redis） ==========
        long redisStockAfter = redissonClient.getAtomicLong(STOCK_KEY_PREFIX + productId).get();
        assertEquals("Redis 库存应保持预扣值", stockS0 - quantity, redisStockAfter);

        // ========== 断言 5：order_paid 事件已发布并被测试消费者收到 ==========
        assertTrue("应在超时时间内收到 order_paid 事件, orderNo=" + orderNo,
                awaitEvent(orderNo, 15000));
    }

    /**
     * 场景 2：重复回调幂等（M1 最核心验收）
     *
     * <p>对完全相同的合法 notify 参数连续发送 3 次（原样重放，与支付宝重发机制一致）。
     * 硬标准：order_main 仍 PAID、pay_order 仍 PAID、MySQL 库存只减少一次。</p>
     *
     * @throws Exception 请求或断言失败
     */
    @Test
    public void testPayNotify_duplicateNotifyIdempotent() throws Exception {
        // ========== 准备真实新订单 ==========
        int quantity = 2;
        int stockS0 = 100;
        Long productId = createTestProduct(stockS0);
        String orderNo = uniqueOrderNo("S2");
        createNewChainOrder(orderNo, productId, "M1E2E测试商品", quantity,
                new BigDecimal("19.90"), "WAIT_PAY", "CREATED");

        redissonClient.getAtomicLong(STOCK_KEY_PREFIX + productId).set(stockS0 - quantity);
        long redisStockBefore = redissonClient.getAtomicLong(STOCK_KEY_PREFIX + productId).get();

        // ========== 构造一次合法签名，原样重放 3 次（sign 不变，必过验签） ==========
        Map<String, String> notifyParams = buildSignedNotifyParams(orderNo, "19.90");

        sendNotify(notifyParams).andExpect(status().isOk()).andExpect(content().string("success"));
        sendNotify(notifyParams).andExpect(status().isOk()).andExpect(content().string("success"));
        sendNotify(notifyParams).andExpect(status().isOk()).andExpect(content().string("success"));

        // ========== 硬标准：只看数据库与库存终态 ==========
        OrderMain orderMain = queryOrderMain(orderNo);
        assertNotNull(orderMain);
        assertEquals("重复回调后 order_main 仍应为 PAID", "PAID", orderMain.getStatus());

        PayOrder payOrder = orderDao.queryByOrderNo(orderNo);
        assertNotNull(payOrder);
        assertEquals("重复回调后 pay_order 仍应为 PAID", "PAID", payOrder.getStatus());

        assertEquals("MySQL 库存只能减少一次", Integer.valueOf(stockS0 - quantity),
                productDao.selectById(productId).getStock());

        long redisStockAfter = redissonClient.getAtomicLong(STOCK_KEY_PREFIX + productId).get();
        assertEquals("Redis 库存不得因重复回调变化", redisStockBefore, redisStockAfter);

        // 每次重放都会再发一条 order_paid（已知现象，监听器幂等拦截），至少应收到一条
        assertTrue("应收到 order_paid 事件, orderNo=" + orderNo, awaitEvent(orderNo, 15000));
    }

    /**
     * 场景 2-补：并发重放幂等（并发极端场景验证）
     *
     * <p>M1 静态核实已登记的残留风险：paySuccess 先无锁读状态（canPay），
     * 且 {@code updateOrderStatusByOrderNo} 的 UPDATE 无 status 条件
     * （OrderRepositoryImpl.java:139-145），两个请求同时读到 INIT 时
     * 可能双双通过守卫并各自执行一次库存扣减。</p>
     *
     * <p><b>2026-10-03 实测结论：风险真实存在，比技术债预估更严重。</b>
     * 10 线程同一瞬间重放同一合法 notify，10 个事务全部通过 canPay 守卫，
     * 各自执行一次 syncDBStockForPaySuccess：MySQL 库存从 100 被扣到 80
     * （10×2 件），order_main/pay_order 仍写同值 PAID。
     * 状态写双写同值无业务损害（与技术债结论一致），
     * 但<b>库存扣减不在幂等保护内，并发回调会重复扣库存</b>。</p>
     *
     * <p>因 M1 冻结生产代码（禁止修改 OrderStateMachineServiceImpl/
     * OrderTransactionService），本测试暂标记 @Ignore；
     * 待 JV-003 M2 或技术债修复（如 UPDATE 加 status 条件 +
     * 扣减前置校验影响行数）后移除 @Ignore 启用。</p>
     *
     * @throws Exception 请求或断言失败
     */
    @Test
    @org.junit.Ignore("并发重放暴露库存重复扣减缺陷（实测 100→80），待 M2/技术债修复后启用")
    public void testPayNotify_concurrentDuplicateNotify() throws Exception {
        // ========== 准备真实新订单 ==========
        int quantity = 2;
        int stockS0 = 100;
        Long productId = createTestProduct(stockS0);
        String orderNo = uniqueOrderNo("SC");
        createNewChainOrder(orderNo, productId, "M1E2E测试商品", quantity,
                new BigDecimal("19.90"), "WAIT_PAY", "CREATED");

        redissonClient.getAtomicLong(STOCK_KEY_PREFIX + productId).set(stockS0 - quantity);
        long redisStockBefore = redissonClient.getAtomicLong(STOCK_KEY_PREFIX + productId).get();

        // ========== 同一份合法签名，10 个线程同一瞬间并发重放 ==========
        Map<String, String> notifyParams = buildSignedNotifyParams(orderNo, "19.90");
        int threadCount = 10;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch ready = new CountDownLatch(threadCount);
        CountDownLatch fire = new CountDownLatch(1);
        List<Future<String>> futures = new ArrayList<>();
        for (int i = 0; i < threadCount; i++) {
            futures.add(executor.submit(() -> {
                ready.countDown();
                fire.await(); // 所有线程就绪后统一开火，最大化时间重叠
                return mockMvc.perform(buildNotifyRequest(notifyParams))
                        .andReturn().getResponse().getContentAsString();
            }));
        }
        assertTrue("并发线程未在超时时间内就绪", ready.await(10, TimeUnit.SECONDS));
        fire.countDown();
        List<String> responses = new ArrayList<>();
        for (Future<String> future : futures) {
            responses.add(future.get(30, TimeUnit.SECONDS));
        }
        executor.shutdown();

        // 响应仅作参考，不作为验收标准（支付宝对重复通知的响应本就是 success/false 都可能）
        log.info("并发重放 10 次响应: {}", responses);

        // ========== 硬标准：数据库终态，库存必须只减少一次 ==========
        OrderMain orderMain = queryOrderMain(orderNo);
        assertNotNull(orderMain);
        assertEquals("并发回调后 order_main 仍应为 PAID", "PAID", orderMain.getStatus());

        PayOrder payOrder = orderDao.queryByOrderNo(orderNo);
        assertNotNull(payOrder);
        assertEquals("并发回调后 pay_order 仍应为 PAID", "PAID", payOrder.getStatus());

        assertEquals("并发重放下 MySQL 库存只能减少一次（暴露无状态条件更新的并发窗口）",
                Integer.valueOf(stockS0 - quantity), productDao.selectById(productId).getStock());

        long redisStockAfter = redissonClient.getAtomicLong(STOCK_KEY_PREFIX + productId).get();
        assertEquals("并发重放下 Redis 库存不得变化", redisStockBefore, redisStockAfter);

        assertTrue("应收到 order_paid 事件, orderNo=" + orderNo, awaitEvent(orderNo, 15000));
    }

    /**
     * 场景 3：遗留旧订单回调（仅 pay_order，无 order_main）
     *
     * <p>走旧链 orderService.changeOrderPaySuccess，仅更新 pay_order 状态为
     * PAY_SUCCESS（OrderStatusVO.PAY_SUCCESS，项目旧链实际状态值），
     * 不进入新订单状态机、不执行新订单库存扣减。</p>
     *
     * @throws Exception 请求或断言失败
     */
    @Test
    public void testPayNotify_legacyOrderWithoutOrderMain() throws Exception {
        // ========== 准备遗留旧订单：只有 pay_order，没有 order_main ==========
        int stockS0 = 100;
        Long productId = createTestProduct(stockS0);
        String orderNo = uniqueOrderNo("S3");
        createLegacyPayOrderOnly(orderNo, productId, "M1E2E测试商品", new BigDecimal("19.90"));

        assertNull("准备阶段 order_main 必须不存在", queryOrderMain(orderNo));
        PayOrder before = orderDao.queryByOrderNo(orderNo);
        assertNotNull(before);
        assertEquals("WAIT_PAY", before.getStatus());

        // ========== 发送合法 notify ==========
        Map<String, String> notifyParams = buildSignedNotifyParams(orderNo, "19.90");
        sendNotify(notifyParams).andExpect(status().isOk()).andExpect(content().string("success"));

        // ========== 断言 1：pay_order 更新为旧链的 PAY_SUCCESS ==========
        PayOrder payOrder = orderDao.queryByOrderNo(orderNo);
        assertNotNull(payOrder);
        assertEquals("旧链 pay_order 状态应为 PAY_SUCCESS", "PAY_SUCCESS", payOrder.getStatus());
        assertNotNull("pay_order.pay_time 应已写入", payOrder.getPayTime());

        // ========== 断言 2：order_main 仍不存在（旧链不写 order_main） ==========
        assertNull("order_main 必须仍不存在", queryOrderMain(orderNo));

        // ========== 断言 3：不执行新订单库存扣减 ==========
        assertEquals("旧链回调不得扣减 MySQL 库存", Integer.valueOf(stockS0),
                productDao.selectById(productId).getStock());
        assertFalse("旧链回调不得写入 Redis 库存 Key",
                redissonClient.getAtomicLong(STOCK_KEY_PREFIX + productId).isExists());

        // 旧链同样会发布 order_paid（已知行为），监听器查询订单不存在仅 warn，不写库
        assertTrue("应收到 order_paid 事件, orderNo=" + orderNo, awaitEvent(orderNo, 15000));
        assertNull("监听器消费后 order_main 仍必须不存在", queryOrderMain(orderNo));
        assertEquals("监听器消费后 MySQL 库存仍不变", Integer.valueOf(stockS0),
                productDao.selectById(productId).getStock());
    }

    /**
     * 验签真实性负向验证：篡改签名参数后必须验签失败
     *
     * <p>用错误的私钥签名（模拟伪造回调），生产验签代码必须返回 false，
     * 且订单状态不得变更。证明验签未被绕过。</p>
     *
     * @throws Exception 请求或断言失败
     */
    @Test
    public void testPayNotify_forgedSignRejected() throws Exception {
        int stockS0 = 100;
        Long productId = createTestProduct(stockS0);
        String orderNo = uniqueOrderNo("SF");
        createNewChainOrder(orderNo, productId, "M1E2E测试商品", 2,
                new BigDecimal("19.90"), "WAIT_PAY", "CREATED");
        redissonClient.getAtomicLong(STOCK_KEY_PREFIX + productId).set(stockS0 - 2);

        // 用另一把私钥签名 → 与上下文配置的测试公钥不匹配 → 必须验签失败
        Map<String, String> params = buildNotifyParams(orderNo, "19.90");
        KeyPair attackerKeyPair = generateRsaKeyPair();
        params.put("sign", rsa256Sign(buildSignCheckContent(params), attackerKeyPair));
        params.put("sign_type", "RSA2");

        sendNotify(params).andExpect(status().isOk()).andExpect(content().string("false"));

        // 验签失败 → 不得进入任何业务链路
        assertEquals("验签失败 order_main 状态不得变更", "CREATED", queryOrderMain(orderNo).getStatus());
        assertEquals("验签失败 pay_order 状态不得变更", "WAIT_PAY",
                orderDao.queryByOrderNo(orderNo).getStatus());
        assertEquals("验签失败 MySQL 库存不得变更", Integer.valueOf(stockS0),
                productDao.selectById(productId).getStock());
        assertFalse("验签失败不得发布 order_paid 事件", awaitEvent(orderNo, 3000));
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
        product.setName("M1E2E测试商品-" + UUID.randomUUID().toString().substring(0, 8));
        product.setDescription("JV-003 M1 回调 E2E 测试专用商品");
        product.setPrice(new BigDecimal("9.95"));
        product.setStock(stock);
        product.setCategory("测试");
        product.setStatus(1);
        product.setCreateTime(LocalDateTime.now());
        product.setUpdateTime(LocalDateTime.now());
        productDao.insert(product);
        testProductIds.add(product.getId());
        return product.getId();
    }

    /**
     * 创建新链订单：order_main + order_item + pay_order（真实写入 MySQL）
     *
     * @param orderNo     订单号
     * @param productId   商品ID
     * @param productName 商品名称
     * @param quantity    购买数量
     * @param totalAmount 订单金额
     * @param payStatus   pay_order 初始状态
     * @param mainStatus  order_main 初始状态
     */
    private void createNewChainOrder(String orderNo, Long productId, String productName,
                                     int quantity, BigDecimal totalAmount,
                                     String payStatus, String mainStatus) {
        OrderMain orderMain = new OrderMain();
        orderMain.setOrderNo(orderNo);
        orderMain.setUserId(TEST_USER_ID);
        orderMain.setTotalAmount(totalAmount);
        orderMain.setStatus(mainStatus);
        orderMain.setAddress("M1E2E测试地址");
        orderMain.setCreateTime(LocalDateTime.now());
        orderMain.setUpdateTime(LocalDateTime.now());
        orderMainDao.insert(orderMain);

        OrderItem orderItem = new OrderItem();
        orderItem.setOrderId(orderMain.getId());
        orderItem.setProductId(productId);
        orderItem.setProductName(productName);
        orderItem.setPrice(new BigDecimal("9.95"));
        orderItem.setQuantity(quantity);
        orderItem.setCreateTime(LocalDateTime.now());
        orderItemDao.insert(orderItem);

        PayOrder payOrder = PayOrder.builder()
                .userId(String.valueOf(TEST_USER_ID))
                .productId(String.valueOf(productId))
                .productName(productName)
                .orderId(orderNo)
                .orderTime(new Date())
                .totalAmount(totalAmount)
                .status(payStatus)
                .createTime(new Date())
                .updateTime(new Date())
                .build();
        orderDao.insert(payOrder);
        testOrderNos.add(orderNo);
    }

    /**
     * 创建遗留旧订单：仅 pay_order 行（真实写入 MySQL）
     *
     * @param orderNo     订单号
     * @param productId   商品ID
     * @param productName 商品名称
     * @param totalAmount 订单金额
     */
    private void createLegacyPayOrderOnly(String orderNo, Long productId, String productName,
                                          BigDecimal totalAmount) {
        PayOrder payOrder = PayOrder.builder()
                .userId(String.valueOf(TEST_USER_ID))
                .productId(String.valueOf(productId))
                .productName(productName)
                .orderId(orderNo)
                .orderTime(new Date())
                .totalAmount(totalAmount)
                .status("WAIT_PAY")
                .createTime(new Date())
                .updateTime(new Date())
                .build();
        orderDao.insert(payOrder);
        testOrderNos.add(orderNo);
    }

    /* ==================== 支付宝 notify 构造工具 ==================== */

    /**
     * 构造带真实 RSA2 签名的支付宝 notify 参数
     *
     * @param orderNo     商户订单号（out_trade_no）
     * @param totalAmount 交易金额
     * @return 完整 notify 参数（含 sign / sign_type）
     */
    private Map<String, String> buildSignedNotifyParams(String orderNo, String totalAmount) {
        Map<String, String> params = buildNotifyParams(orderNo, totalAmount);
        params.put("sign", rsa256Sign(buildSignCheckContent(params), TEST_KEY_PAIR));
        params.put("sign_type", "RSA2");
        return params;
    }

    /**
     * 构造支付宝 notify 业务参数（未签名）
     *
     * @param orderNo     商户订单号
     * @param totalAmount 交易金额
     * @return 业务参数 Map
     */
    private Map<String, String> buildNotifyParams(String orderNo, String totalAmount) {
        Map<String, String> params = new HashMap<>();
        params.put("out_trade_no", orderNo);
        params.put("trade_no", "2026100122001408000500123456");
        params.put("total_amount", totalAmount);
        params.put("trade_status", "TRADE_SUCCESS");
        params.put("subject", "商城订单支付");
        params.put("notify_id", UUID.randomUUID().toString().replace("-", ""));
        params.put("notify_time", "2026-10-01 18:00:00");
        params.put("notify_type", "trade_status_sync");
        params.put("gmt_payment", "2026-10-01 18:00:00");
        params.put("charset", "utf-8");
        return params;
    }

    /**
     * 构建待签名字符串，规则与 AlipayGatewayImpl.getSignCheckContentV1 完全一致：
     * 排除 sign/sign_type，跳过空值，按键字典序以 & 拼接 key=value
     *
     * @param params notify 参数
     * @return 待签名字符串
     */
    private String buildSignCheckContent(Map<String, String> params) {
        List<String> keys = new ArrayList<>(params.keySet());
        Collections.sort(keys);
        StringBuilder sb = new StringBuilder();
        for (String key : keys) {
            if ("sign".equals(key) || "sign_type".equals(key)) {
                continue;
            }
            String value = params.get(key);
            if (value == null || value.isEmpty()) {
                continue;
            }
            if (sb.length() > 0) {
                sb.append("&");
            }
            sb.append(key).append("=").append(value);
        }
        return sb.toString();
    }

    /**
     * RSA2（SHA256withRSA）签名
     *
     * @param content 待签名字符串
     * @param keyPair 密钥对
     * @return Base64 签名值
     */
    private String rsa256Sign(String content, KeyPair keyPair) {
        try {
            Signature signature = Signature.getInstance("SHA256withRSA");
            signature.initSign(keyPair.getPrivate());
            signature.update(content.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(signature.sign());
        } catch (Exception e) {
            throw new IllegalStateException("构造测试签名失败", e);
        }
    }

    /**
     * 发送 notify POST 请求（MockMvc 完整经过 DispatcherServlet → Controller）
     *
     * @param params notify 参数
     * @return 请求结果
     * @throws Exception 请求失败
     */
    private ResultActions sendNotify(Map<String, String> params) throws Exception {
        return mockMvc.perform(buildNotifyRequest(params));
    }

    /**
     * 构建 notify POST 请求（MockMvc 完整经过 DispatcherServlet → Controller，
     * 请求参数直接写入 parameterMap，无 URL 编码往返，与签名内容逐字节一致）
     *
     * @param params notify 参数
     * @return 请求构建器
     */
    private org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder buildNotifyRequest(
            Map<String, String> params) {
        org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder builder =
                post(NOTIFY_URL).characterEncoding("UTF-8");
        params.forEach(builder::param);
        return builder;
    }

    /* ==================== 断言辅助 ==================== */

    /**
     * 等待测试消费者收到指定 orderNo 的 order_paid 事件
     *
     * @param orderNo 订单号
     * @param timeoutMs 超时毫秒
     * @return 是否收到
     * @throws InterruptedException 等待中断
     */
    private boolean awaitEvent(String orderNo, long timeoutMs) throws InterruptedException {
        long deadline = System.currentTimeMillis() + timeoutMs;
        while (System.currentTimeMillis() < deadline) {
            if (receivedOrderNos.contains(orderNo)) {
                return true;
            }
            Thread.sleep(200);
        }
        return receivedOrderNos.contains(orderNo);
    }

    /**
     * 按订单号查询 order_main
     *
     * @param orderNo 订单号
     * @return order_main 记录，不存在返回 null
     */
    private OrderMain queryOrderMain(String orderNo) {
        LambdaQueryWrapper<OrderMain> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(OrderMain::getOrderNo, orderNo);
        return orderMainDao.selectOne(wrapper);
    }

    /**
     * 生成全局唯一测试订单号（长度适配 order_id varchar(32)）
     *
     * @param prefix 场景前缀
     * @return 订单号
     */
    private String uniqueOrderNo(String prefix) {
        return "E2E" + prefix + UUID.randomUUID().toString().replace("-", "").substring(0, 24);
    }

    /**
     * 生成 2048 位 RSA 密钥对
     *
     * @return RSA 密钥对
     */
    private static KeyPair generateRsaKeyPair() {
        try {
            KeyPairGenerator kpg = KeyPairGenerator.getInstance("RSA");
            kpg.initialize(2048);
            return kpg.generateKeyPair();
        } catch (Exception e) {
            throw new IllegalStateException("生成测试 RSA 密钥对失败", e);
        }
    }
}
