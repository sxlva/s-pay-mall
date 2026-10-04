package cn.fcr.test;

import cn.fcr.infrastructure.dao.mall.IProductDao;
import cn.fcr.infrastructure.dao.mall.po.Product;
import cn.fcr.infrastructure.dao.order.IOrderDao;
import cn.fcr.infrastructure.dao.order.IOrderItemDao;
import cn.fcr.infrastructure.dao.order.IOrderMainDao;
import cn.fcr.infrastructure.dao.order.po.OrderItem;
import cn.fcr.infrastructure.dao.order.po.OrderMain;
import cn.fcr.infrastructure.dao.order.po.PayOrder;
import cn.fcr.application.OrderApplicationService;
import cn.fcr.infrastructure.order.gateway.AlipayQueryGatewayImpl;
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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;

/**
 * 超时关单分流 E2E 测试（M2-1 行为修复验收）
 *
 * <p>直接调用 OrderApplicationService.handleTimeoutCloseOrder（
 * OrderTimeoutCloseRocketListener 的唯一入口），验证 P0-1 新旧订单分流：
 * order_main 存在的订单走状态机 cancel（关 pay_order + 恢复 Redis 预扣库存），
 * 否则走旧域逻辑。</p>
 *
 * <p>依赖真实环境：MySQL(127.0.0.1:23306, db=s-pay-mall)、
 * Redis(127.0.0.1:26379, db=1)。测试数据通过唯一 orderNo / 独立测试商品隔离，
 * @After 物理清理。</p>
 *
 * @author 傅崇睿
 */
@Slf4j
@RunWith(SpringRunner.class)
@SpringBootTest
public class TimeoutCloseOrderE2EIT {

    /** Redis 库存 Key 前缀（与 StockGatewayImpl 一致） */
    private static final String STOCK_KEY_PREFIX = "mall:product:stock:";

    /**
     * 测试订单所属用户：复用现有 mall_user(id=1)。
     * order_main.user_id 存在外键 fk_order_user → mall_user(id)。
     */
    private static final Long TEST_USER_ID = 1L;

    @Autowired
    private OrderApplicationService orderApplicationService;

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

    /**
     * 支付宝交易查询网关 Mock（B2）：超时关单前二次确认依赖它；
     * 默认返回 false（未支付，走关单），场景 3 显式置 true 验证"已支付转履约"。
     * Mock 同时避免 E2E 测试对外网支付宝的真实请求。
     */
    @MockBean
    private AlipayQueryGatewayImpl alipayQueryGateway;

    /** 本测试类创建的订单号与商品ID，用于 @After 物理清理 */
    private final List<String> testOrderNos = new ArrayList<>();
    private final List<Long> testProductIds = new ArrayList<>();

    /**
     * 每个测试方法后清理：物理删除测试订单/商品数据、删除 Redis 库存 Key
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
            // IOrderDao 为纯注解 Mapper（非 BaseMapper），无 delete 方法，测试清理用 JdbcTemplate 物理删除
            jdbcTemplate.update("delete from pay_order where order_id = ?", orderNo);
        }
        testOrderNos.clear();
        for (Long productId : testProductIds) {
            redissonClient.getAtomicLong(STOCK_KEY_PREFIX + productId).delete();
            productDao.deleteById(productId);
        }
        testProductIds.clear();
    }

    /**
     * 场景 1：新订单超时关单走状态机（M2-1 核心验收）
     *
     * <p>准备 order_main(CREATED) + order_item + pay_order(WAIT_PAY)，
     * Redis 预扣库存（S0-q）。调用 handleTimeoutCloseOrder：</p>
     *
     * <p>硬标准：返回 true；order_main=CANCELED；pay_order=CLOSED；
     * Redis 库存恢复到 S0（预扣被回滚）；MySQL 库存不变（未支付不动 DB）。
     * 第二次调用返回 false（状态机守卫幂等）。</p>
     *
     * @throws Exception 断言失败
     */
    @Test
    public void testTimeoutClose_newOrderViaStateMachine() throws Exception {
        // ========== 准备真实新订单 ==========
        int quantity = 2;
        int stockS0 = 100;
        Long productId = createTestProduct(stockS0);
        String orderNo = uniqueOrderNo("TC1");
        createNewChainOrder(orderNo, productId, "M2E2E测试商品", quantity,
                new BigDecimal("19.90"), "WAIT_PAY", "CREATED");

        // 模拟下单时 Redis 预扣（createOrderInTransaction → checkAndDeductStock）
        redissonClient.getAtomicLong(STOCK_KEY_PREFIX + productId).set(stockS0 - quantity);
        assertEquals(stockS0 - quantity,
                redissonClient.getAtomicLong(STOCK_KEY_PREFIX + productId).get());
        assertEquals(Integer.valueOf(stockS0), productDao.selectById(productId).getStock());

        // ========== 第一次超时关单：应分流到状态机 cancel ==========
        boolean closed = orderApplicationService.handleTimeoutCloseOrder(orderNo);
        assertTrue("新订单超时关单应返回 true", closed);

        // ========== 断言 1：order_main = CANCELED ==========
        OrderMain orderMain = queryOrderMain(orderNo);
        assertNotNull("order_main 应存在", orderMain);
        assertEquals("order_main 状态应为 CANCELED", "CANCELED", orderMain.getStatus());

        // ========== 断言 2：pay_order = CLOSED（旧逻辑只会动旧 order 表，不会关 pay_order） ==========
        PayOrder payOrder = orderDao.queryByOrderNo(orderNo);
        assertNotNull("pay_order 应存在", payOrder);
        assertEquals("pay_order 状态应为 CLOSED", "CLOSED", payOrder.getStatus());

        // ========== 断言 3：Redis 预扣库存被恢复 ==========
        assertEquals("Redis 库存应恢复到 S0", stockS0,
                redissonClient.getAtomicLong(STOCK_KEY_PREFIX + productId).get());

        // ========== 断言 4：MySQL 库存不变（未支付订单不动 DB 库存） ==========
        assertEquals("MySQL 库存应保持 S0", Integer.valueOf(stockS0),
                productDao.selectById(productId).getStock());

        // ========== 断言 5：重复超时消息幂等（状态机守卫拒绝，返回 false） ==========
        boolean secondClose = orderApplicationService.handleTimeoutCloseOrder(orderNo);
        assertFalse("已关闭订单再次超时关单应返回 false", secondClose);
        assertEquals("重复关单后 order_main 仍应为 CANCELED", "CANCELED",
                queryOrderMain(orderNo).getStatus());
        assertEquals("重复关单后 pay_order 仍应为 CLOSED", "CLOSED",
                orderDao.queryByOrderNo(orderNo).getStatus());
        assertEquals("重复关单后 Redis 库存不得重复恢复", stockS0,
                redissonClient.getAtomicLong(STOCK_KEY_PREFIX + productId).get());
    }

    /**
     * 场景 2：已支付新订单收到迟到超时消息，状态机守卫拒绝
     *
     * <p>order_main=PAID 时 canCancel=false，不得关单、不得恢复库存，
     * 防止超时消息误伤已支付订单。</p>
     *
     * @throws Exception 断言失败
     */
    @Test
    public void testTimeoutClose_paidOrderGuarded() throws Exception {
        int quantity = 2;
        int stockS0 = 100;
        Long productId = createTestProduct(stockS0);
        String orderNo = uniqueOrderNo("TC2");
        createNewChainOrder(orderNo, productId, "M2E2E测试商品", quantity,
                new BigDecimal("19.90"), "PAID", "PAID");

        // 已支付订单：DB 库存已扣、Redis 保持预扣值（支付链路不重复扣 Redis）
        redissonClient.getAtomicLong(STOCK_KEY_PREFIX + productId).set(stockS0 - quantity);
        productDao.updateById(stockOf(productId, stockS0 - quantity));

        boolean closed = orderApplicationService.handleTimeoutCloseOrder(orderNo);
        assertFalse("已支付订单超时关单应被守卫拒绝", closed);

        assertEquals("守卫拒绝后 order_main 仍应为 PAID", "PAID",
                queryOrderMain(orderNo).getStatus());
        assertEquals("守卫拒绝后 pay_order 仍应为 PAID", "PAID",
                orderDao.queryByOrderNo(orderNo).getStatus());
        assertEquals("守卫拒绝后 Redis 库存不得恢复", stockS0 - quantity,
                redissonClient.getAtomicLong(STOCK_KEY_PREFIX + productId).get());
        assertEquals("守卫拒绝后 MySQL 库存不得恢复", Integer.valueOf(stockS0 - quantity),
                productDao.selectById(productId).getStock());
    }

    /**
     * 场景 3（B2）：关单前支付宝侧已交易成功 → 转履约路径，不关闭订单
     *
     * <p>order_main=CREATED、pay_order=WAIT_PAY，但用户已在支付宝完成支付
     * （延时消息到期瞬间付款）。handleTimeoutCloseOrder 应先查询支付宝：
     * 已支付 → 走 paySuccess 履约（order_main=PAID、pay_order=PAID、
     * MySQL 库存同步扣减），绝不关闭订单。</p>
     *
     * @throws Exception 断言失败
     */
    @Test
    public void testTimeoutClose_alipayPaidRescued() throws Exception {
        int quantity = 2;
        int stockS0 = 100;
        Long productId = createTestProduct(stockS0);
        String orderNo = uniqueOrderNo("TC3");
        createNewChainOrder(orderNo, productId, "M2E2E测试商品", quantity,
                new BigDecimal("19.90"), "WAIT_PAY", "CREATED");

        // 模拟下单时 Redis 预扣（支付链路不重复扣 Redis）
        redissonClient.getAtomicLong(STOCK_KEY_PREFIX + productId).set(stockS0 - quantity);

        // 支付宝侧查询确认：交易已成功
        when(alipayQueryGateway.queryTradeSuccess(orderNo)).thenReturn(true);

        // ========== 超时关单入口：应转履约而非关单 ==========
        boolean handled = orderApplicationService.handleTimeoutCloseOrder(orderNo);
        assertTrue("支付宝已支付时关单入口应返回 true（已转履约）", handled);

        // ========== 断言 1：订单被履约而非关闭 ==========
        assertEquals("order_main 应为 PAID", "PAID", queryOrderMain(orderNo).getStatus());
        assertEquals("pay_order 应为 PAID", "PAID", orderDao.queryByOrderNo(orderNo).getStatus());

        // ========== 断言 2：MySQL 库存按支付链路同步扣减（S0 - quantity） ==========
        assertEquals("MySQL 库存应扣减为 S0-quantity", Integer.valueOf(stockS0 - quantity),
                productDao.selectById(productId).getStock());

        // ========== 断言 3：Redis 预扣值不变（支付链路不重复扣 Redis） ==========
        assertEquals("Redis 库存应保持预扣值", stockS0 - quantity,
                redissonClient.getAtomicLong(STOCK_KEY_PREFIX + productId).get());
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
        product.setName("M2E2E测试商品-" + UUID.randomUUID().toString().substring(0, 8));
        product.setDescription("M2-1 超时关单分流 E2E 测试专用商品");
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
     * 构造仅含指定库存的更新用商品对象（避免覆盖其他字段）
     *
     * @param productId 商品ID
     * @param stock     库存
     * @return 仅含 id+stock 的商品对象
     */
    private Product stockOf(Long productId, int stock) {
        Product product = new Product();
        product.setId(productId);
        product.setStock(stock);
        return product;
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
        orderMain.setAddress("M2E2E测试地址");
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
}
