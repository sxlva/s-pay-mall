# 课程答辩准备手册

> **用途**：一对一答辩现场使用，可直接背诵。所有代码引用均来自项目实际源码。

---

## 一、老师可能问的基础问题（30秒口答版）

### 1. Redis 的 setex 命令是什么意思？项目中哪里用了？

setex 就是 SET + EXpire，设置一个 key 的值同时指定过期时间。比如我们项目里缓存微信 access_token：`stringRedisTemplate.opsForValue().set(key, accessToken, 110, TimeUnit.MINUTES)`，表示把 token 写入 Redis 并在 110 分钟后自动删除。为什么是 110 分钟？因为微信官方是 120 分钟，提前 10 分钟续期避免边界过期。

### 2. RAtomicLong.addAndGet() 为什么是原子的？

因为 Redis 是单线程执行命令的，所有命令排队串行处理。所以 `addAndGet(-quantity)` 这条 INCRBY 指令在执行时不会被其他客户端打断，无论多少并发请求过来，都是排着队一个个执行，不存在"读到半截数据"的问题。这和我们写 Java 多线程用 `AtomicInteger` 的 CAS 原理类似，只不过这个"单线程"是由 Redis 服务端保证的。

### 3. RSA2 验签的步骤是什么？

三步：第一步，拿到支付宝公钥（从支付宝开放平台下载，配置在我们的 `AliPayConfigProperties` 里）；第二步，把支付宝回调过来的参数排除 sign 和 sign_type 字段后，按 key 字母序排序拼接成待验签字符串；第三步，用支付宝 SDK 的 `rsa256CheckContent()` 方法，拿公钥验证签名是否匹配。验签不通过直接返回 "false"，通过才更新订单状态。

### 4. @RocketMQMessageListener 注解的作用？

这个注解把一个普通类变成一个 RocketMQ 消息消费者。比如我们项目的 `OrderPaidRocketListener`，注解里配置 `topic = "order_paid"` 和 `consumerGroup = "s-pay-mall-order-paid-consumer"`，它实现了 `RocketMQListener<PaySuccessMessage>` 接口。当 `order_paid` 这个 topic 有新消息时，RocketMQ 会自动调用它的 `onMessage()` 方法，我们在这个方法里完成后续履约。

### 5. MyBatis-Plus 和 MyBatis 的区别？

MyBatis-Plus 是 MyBatis 的增强工具，在 MyBatis 基础上提供了内置的 CRUD 方法（不需要写 XML 就能做单表增删改查）、分页插件、乐观锁支持等。我们项目用 MyBatis-Plus 主要体现在 DAO 层：`IProductDao` 等接口继承 `BaseMapper<T>`，就可以直接用 `insert()`、`updateById()` 等方法。

### 6. JWT Token 由哪三部分组成？项目中用于什么？

三部分：Header（声明算法，比如 HS256）、Payload（放用户信息，比如 userId、username、role），Signature（用密钥对前两部分签名，防止篡改）。我们项目在 `TokenProviderAdapter.createToken()` 里生成 JWT，用户扫码登录成功后返回给前端，后续每次请求带在 Header 里，`JwtAuthenticationFilter` 会拦截验证。

### 7. BCrypt 和 MD5 有什么区别？

BCrypt 自带随机盐值，每次加密同一个密码结果都不一样，有效防彩虹表攻击。MD5 是简单哈希，同一个密码永远生成同一个结果，可以暴力破解或者查彩虹表。而且 BCrypt 可以设置计算强度参数，故意让加密变慢，增加暴力破解的成本。我们项目虽然没有直接使用 BCrypt 注册（用户走微信扫码），但在安全设计文档里标注了这一点。

### 8. 什么是幂等性？项目中哪里用到了？

幂等性就是同一个操作执行一次和执行多次，结果一样。我们项目有三处典型的幂等设计：第一，支付回调——验签通过后更新订单状态时附带 `WHERE status = 'PAY_WAIT'` 条件，已经改为 PAY_SUCCESS 的订单不会再被更新；第二，库存扣减——`StockGatewayImpl.deductStock()` 的 L3 竞态检查，库存为负即回滚；第三，MQ 消费——`IdempotentGatewayImpl.tryAcquire()` 用 Redisson 的 `trySet()` 实现 SETNX，消息 ID 处理过了就不再处理。

### 9. DDD 分层中领域层为什么不能有 @Service 注解？

因为领域层应该是纯 POJO，不依赖任何框架。如果加了 `@Service`，领域层就耦合了 Spring，单元测试必须启动 Spring 容器，违背了 DDD 的"领域独立可测"原则。我们项目的做法是把 12 个 Domain Service 全部写成普通 Java 类，然后在 `DomainServiceConfig` 里用 `@Bean` 方法手动 new 出来注入 Spring。这样既能在生产环境用 Spring DI，又能在测试时脱离框架直接 new + Mock 接口。

### 10. 支付宝回调为什么必须返回 "success"？

因为支付宝的异步通知机制规定：如果你不返回 "success"，支付宝认为通知失败，会在 24 小时内按间隔（4分钟、10分钟、10分钟、1小时……共 7 次）重复发送。这会导致同一笔支付通知被多次处理。所以我们必须快速验签并返回 "success"，然后用幂等机制保证重复通知不会造成数据错误。

---

## 二、代码级问题准备

### 1. WeixinGatewayImpl.getAccessToken() — AccessToken Redis 缓存

```java
// 文件: s-pay-mall-infrastructure/.../auth/gateway/WeixinGatewayImpl.java 第148-165行

private String getAccessToken() throws IOException {
    String key = Constants.REDIS_WECHAT_ACCESS_TOKEN_PREFIX + appid;
    String accessToken = stringRedisTemplate.opsForValue().get(key);
    if (accessToken == null) {
        Call<WeixinTokenResponseDTO> call = weixinApiService.getToken(
                "client_credential", appid, appSecret);
        WeixinTokenResponseDTO weixinTokenRes = call.execute().body();
        if (weixinTokenRes == null) {
            throw new RuntimeException("获取微信 access token 失败");
        }
        accessToken = weixinTokenRes.getAccess_token();
        stringRedisTemplate.opsForValue().set(key, accessToken, 110, TimeUnit.MINUTES);
        log.info("从微信 API 获取并缓存 access token");
    }
    return accessToken;
}
```

**如果面试官问"这段代码做什么"：**

这是 Cache-Aside 缓存模式的实现——先从 Redis 查 access_token，查不到才调微信 API 获取，然后回写 Redis 并设置 110 分钟过期。Key 的格式是 `wechat:access_token:{appid}`，常量定义在 `Constants.REDIS_WECHAT_ACCESS_TOKEN_PREFIX`。

### 2. StockGatewayImpl.deductStock() — 库存原子扣减

```java
// 文件: s-pay-mall-infrastructure/.../mall/gateway/StockGatewayImpl.java 第49-67行

@Override
public long deductStock(Long productId, Integer quantity) {
    String stockKey = STOCK_KEY_PREFIX + productId;
    RAtomicLong stockCounter = redissonClient.getAtomicLong(stockKey);

    // 【DDD】预检查已上移至 Domain 层（MallOrderServiceImpl），
    // 本方法仅保留原子操作和竞态补偿，不再做前置读判断。
    long remainingStock = stockCounter.addAndGet(-quantity);

    // 竞态补偿：多个并发请求同时扣减时，若结果库存为负则回滚
    if (remainingStock < 0) {
        stockCounter.addAndGet(quantity);
        throw new AppException("STOCK_INSUFFICIENT", "商品库存不足，无法下单");
    }

    log.info("【库存扣减成功】productId={}, 扣减后库存={}", productId, remainingStock);
    return remainingStock;
}
```

**如果面试官问"这段代码做什么"：**

这是库存原子扣减的核心代码。直接执行 `addAndGet(-quantity)` 原子递减（L2），如果结果为负说明并发超卖了，立即 `addAndGet(quantity)` 回滚并抛异常（L3）。预检查（L1）不在这里，而是在上一层的 `MallOrderServiceImpl.checkAndDeductStock()` 中通过 `getStock()` 先判断一次。这个设计体现了 DDD 的分层原则：Infrastructure 层只做原子操作，业务判断归 Domain 层。

### 3. AliPayController.payNotify() — 支付回调验签入口

```java
// 文件: s-pay-mall-trigger/.../http/AliPayController.java 第65-98行

@RequestMapping(value = "alipay_notify_url", method = RequestMethod.POST)
public String payNotify(HttpServletRequest request) {
    // 1. 检查 trade_status
    if (!"TRADE_SUCCESS".equals(request.getParameter("trade_status"))
            && !"TRADE_FINISHED".equals(request.getParameter("trade_status"))) {
        return "false";
    }

    Map<String, String> params = extractParams(request);

    // 2. RSA2 验签
    boolean signVerified = orderApplicationService
            .verifyPayCallbackSign(params, alipayPublicKey);
    if (!signVerified) {
        log.error("支付回调，签名验证失败");
        return "false";
    }

    // 3. 验签通过，处理支付成功
    String tradeNo = params.get("out_trade_no");
    orderApplicationService.changeOrderPaySuccess(tradeNo);

    return "success";
}
```

**如果面试官问"这段代码做什么"：**

这是支付宝异步回调的入口，路径是 `POST /pay-api/v1/alipay/alipay_notify_url`。逻辑分三步：先校验交易状态是否为 TRADE_SUCCESS 或 TRADE_FINISHED，然后调用 `verifyPayCallbackSign()` 做 RSA2 验签——这个调用链最终走到 `AlipayGatewayImpl.rsa256CheckContent()`。验签通过后调用 `changeOrderPaySuccess()`，内部在事务中更新订单状态，事务提交后再通过 RocketMQ 发布 `order_paid` 消息。

### 4. IdempotentGatewayImpl.tryAcquire() — SETNX 幂等检查

```java
// 文件: s-pay-mall-infrastructure/.../mall/gateway/IdempotentGatewayImpl.java 第48-65行

@Override
public boolean tryAcquire(String businessType, String businessNo) {
    String idempotentKey = buildKey(businessType, businessNo);
    // buildKey 生成: "stock:event:{businessType}:{businessNo}"
    RBucket<String> bucket = redissonClient.getBucket(idempotentKey);

    // 使用 trySet 实现 SETNX（仅在不存在时设置）
    boolean acquired = bucket.trySet("PROCESSING", 24, TimeUnit.HOURS);

    if (acquired) {
        log.info("【幂等性检查】获取锁成功，businessType={}, businessNo={}", 
                businessType, businessNo);
    } else {
        log.info("【幂等性检查】获取锁失败（正在处理或已处理），跳过");
    }
    return acquired;
}
```

**如果面试官问"这段代码做什么"：**

这是用 Redisson 的 `trySet()` 实现 Redis SETNX 语义。Key 格式是 `stock:event:{业务类型}:{业务单号}`，TTL 24 小时。如果 SETNX 成功（返回 true），说明是首次处理，继续执行业务；如果失败（返回 false），说明这个消息已经被处理过，直接跳过。还有一个对应的 `release()` 方法，在业务执行异常时删除 Key，允许 MQ 重试。

---

## 三、系统设计亮点（答辩主动陈述用）

### 亮点一：Redis 缓存 + RocketMQ 异步的组合解耦

本项目的亮点之一是把同步的支付回调链路拆成了异步。支付宝回调到达后，我们只做两件事：验签和返回 "success"，整个过程不超过 100ms。然后通过 RocketMQ 的 `order_paid` topic，把后续的发货、微信通知、积分计算全部异步化。这样做的好处是：即使后面的微信通知发送失败，也不会影响支付宝回调的返回，支付宝不会触发重试。而且新增业务只需要添加一个 Consumer 订阅同一个 topic 就行了，完全不用改支付代码。

### 亮点二：双检查防超卖设计（Redis 原子操作）

本项目的亮点之一是库存超卖防护用了三层检查。第一层在 `MallOrderServiceImpl` 里做预检查，读一下 Redis 当前值，不够就直接拒绝，避免无谓的原子操作。第二层是 `StockGatewayImpl` 里的 `addAndGet(-quantity)` 原子递减。第三层是递减后检查结果是否为负——如果有两个请求同时通过预检看到库存=1，第一个扣到 0，第二个就会扣到 -1，触发回滚。这种设计本质上是 CAS 思想在 Redis 上的实践，不需要分布式锁，性能高了不止一个数量级。

### 亮点三：支付回调的幂等闭环

本项目的亮点之一是支付回调做了完整的幂等防御。支付宝的异步通知在 24 小时内会重试 7 次，我们必须保证每次通知处理结果一致。我们的做法是：在更新订单状态时附带 `WHERE status = 'PAY_WAIT'`，已经是 PAY_SUCCESS 的不会被再次更新；在 MQ 消费端用 SETNX 幂等键 `stock:event:{type}:{messageId}` 防重复消费，24 小时后自动过期。这样即使支付宝重试 7 次，或者 MQ 重复投递，都不会造成数据错误。

### 亮点四：DDD 分层边界控制（领域层纯 POJO）

本项目的亮点之一是严格遵循了 DDD 的分层依赖原则。Domain 层的 12 个 Service 实现类全部是纯 POJO，没有任何 Spring 注解。它们的依赖注入是通过 Infrastructure 层的 `DomainServiceConfig` 手动 `@Bean` 注册完成的。这样做的好处是：领域层可以脱离 Spring 独立单测，你只需要 new 一个 Service 然后 Mock 它的接口依赖就行了。我验证过，你可以全局搜索 Domain 层的 Java 文件，不会找到任何一个 `@Service`、`@Component` 或 `@Resource` 注解。

### 亮点五：订单超时关闭的延时消息方案

本项目的亮点之一是用 RocketMQ 延时消息实现了订单超时自动关闭。创建订单成功后，`OrderPaymentGatewayImpl.sendDelayCloseMessage()` 会向 `order-timeout-topic` 发送一条延时消息，消费者 `OrderTimeoutCloseRocketListener` 收到消息后调用 `OrderApplicationService.handleTimeoutCloseOrder()`。这里有一个二次校验：如果在这段时间内用户已经支付了，状态机会拦截取消操作，订单不会被误关。这个方案比定时任务轮询的优点是：不需要扫全表，消息精确投递，延时窗口灵活可控。

---

## 四、"是不是你自己做的"验证问题


### 问题 1：库存扣减为什么要做两次检查而不是一次？

答：因为一次检查不够。如果只做一次预检查——比如先 `get()` 看到库存=1，再 `addAndGet(-1)` 扣减——在并发场景下，线程 A 和线程 B 可能同时通过预检查（都看到库存=1），然后先后执行扣减。A 先扣到 0，B 再扣到 -1，就超卖了。所以我设计的方案是：L2 直接原子递减（不做前置读判断），L3 递减后检查结果是否为负。如果为负说明并发冲突了，立即回滚。L1 预检查放在 Domain 层是为了快速失败，避免不必要的原子操作和回滚。三层合在一起才能既保性能又防超卖。你可以看代码：`StockGatewayImpl` 第 56-63 行只有 L2+L3，第 53 行注释写明了"预检查已上移至 Domain 层（MallOrderServiceImpl）"。
