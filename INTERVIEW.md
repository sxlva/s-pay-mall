# 字节跳动后端面试准备指南

本文件是 s-pay-mall 项目的面试准备指南，帮助你在代码开发完成后进行高并发场景的自我审查和面试准备。

## 使用方式

当你完成一个功能模块后，可以使用以下命令触发面试模拟：

```
你现在是字节跳动的后端面试官，请根据我刚刚提交的代码，指出其中在【高并发场景】下可能存在的隐患，并问我两个相关的技术问题。
```

---

## 一、高并发场景核心问题

### 1.1 面试审查要点

面试官将重点关注以下方面：

#### 并发安全问题
- 库存扣减的原子性保证
- 分布式锁的实现方式
- 乐观锁 vs 悲观锁的选择
- 数据一致性保证

#### 性能瓶颈
- 数据库热点问题
- 缓存策略设计
- 请求削峰填谷
- 异步处理策略

#### 可靠性设计
- 幂等性保证
- 超时重试机制
- 降级熔断策略
- 灾备方案

#### 资源管理
- 线程池配置
- 连接池管理
- 内存使用控制
- GC 优化

---

## 二、典型面试问题与回答

### 2.1 库存扣减

**Q: Redis 原子操作如何保证不超卖？如果 Redis 宕机怎么办？**

```
Redis 原子操作保证不超卖：
1. 使用 RAtomicLong.addAndGet() 进行原子扣减
2. 先 get() 预检库存是否充足
3. 扣减后检查结果是否 < 0，若是则回滚
4. Redis 宕机时，订单创建会失败，需要有重试机制或降级方案

代码示例：
Long result = stockDecr(key, quantity);
if (result < 0) {
    // 回滚
    stockIncr(key, quantity);
    throw new BusinessException("库存不足");
}
```

### 2.2 订单创建

**Q: 如何保证订单号的唯一性？分布式 ID 方案有哪些？**

```
分布式 ID 方案对比：

1. 雪花算法 (Snowflake)
   - 64位 ID = 1位符号 + 41位时间戳 + 10位机器ID + 12位序列号
   - 优点：趋势递增、不依赖数据库
   - 缺点：依赖时钟（时钟回拨会重复）

2. Redis 自增
   - 利用 Redis INCR 保证唯一性
   - 优点：简单、性能高
   - 缺点：依赖 Redis

3. UUID
   - 全局唯一但无序
   - 缺点：不适合作为数据库主键（索引效率低）

4. 数据库自增
   - 单库可用，分库分表需要使用分布式 ID
   - 优点：简单
   - 缺点：无法跨库

推荐：雪花算法 + Redis 兜底
```

### 2.3 支付回调

**Q: 如何处理支付回调的乱序问题？如何保证幂等性？**

```
1. 使用订单号作为幂等键
2. 在数据库中添加唯一索引
3. 状态机校验：只有特定状态才能变更
4. 使用 Redis SETNX 记录已处理的回调
5. 对于乱序问题：使用消息队列保证顺序处理

代码示例：
if (!idempotentGateway.checkAndLock(requestId)) {
    return "success";  // 已处理，返回成功避免重复回调
}
try {
    processPaymentCallback(callback);
} finally {
    idempotentGateway.unlock(requestId);
}
// ⚠️ 注意：此示例不含事务。若方法有 @Transactional 注解，需将锁操作提到事务外层，
//    或使用 finally 块确保锁释放（finally 在事务提交前执行）。
```

### 2.4 缓存设计

**Q: 缓存穿透、缓存击穿、缓存雪崩分别是什么？如何解决？**

```
1. 缓存穿透
   - 定义：查询不存在的数据，每次都打到数据库
   - 解决：空值缓存、布隆过滤器

2. 缓存击穿
   - 定义：热点 key 过期瞬间，大量请求同时打到数据库
   - 解决：互斥锁、永不过期 + 异步更新

3. 缓存雪崩
   - 定义：大量 key 同时过期，导致大量请求打到数据库
   - 解决：过期时间随机化、多级缓存、熔断降级

代码示例：
// 空值缓存解决穿透
String value = redis.get(key);
if (value == null) {
    value = db.query(key);
    if (value != null) {
        redis.setex(key, 5 * 60, value);  // 缓存非空值
    } else {
        // 真正缓存空值，防止缓存穿透
        redis.setex(key, 60, "");  // 空字符串缓存，短时间过期
        return null;
    }
}
return value;
}
```

### 2.5 消息队列

**Q: 如何保证消息不丢失？如何处理消息重复消费？**

```
保证消息不丢失：
1. 生产者确认：publisher confirm 机制
2. Broker 持久化：同步刷盘
3. 消费者确认：手动 ACK

处理重复消费：
1. 消费幂等性：使用 messageId 作为幂等键
2. 业务去重：数据库唯一索引
3. 状态机校验：只有特定状态才能变更

代码示例：
@RocketMQMessageListener(topic = "order-topic")
public class OrderListener implements RocketMQListener<Message> {
    @Override
    public void onMessage(Message message) {
        if (!idempotentGateway.checkAndLock(message.getMessageId())) {
            return;  // 已处理，跳过
        }
        try {
            processMessage(message);
        } finally {
            idempotentGateway.unlock(message.getMessageId());
        }
    }
}
```

### 2.6 分布式锁

**Q: Redisson 分布式锁的实现原理是什么？如何防止死锁？**

```
实现原理：
1. 基于 Redis SETNX + Lua 脚本实现
2. 使用 Hash 结构存储锁信息（线程ID + 过期时间）
3. WATCH + MULTI + EXEC 实现乐观锁

防止死锁：
1. 设置合理的过期时间
2. 看门狗机制（Watchdog）自动续期
3. 唯一 value（线程ID）防止误删他人锁

代码示例：
RLock lock = redissonClient.getLock("order:lock:" + orderNo);
try {
    // 尝试加锁，等待10秒，锁自动30秒过期
    if (lock.tryLock(10, 30, TimeUnit.SECONDS)) {
        // 业务逻辑
    }
} finally {
    if (lock.isHeldByCurrentThread()) {
        lock.unlock();
    }
}
```

---

## 三、Java 集合底层知识

### 3.1 HashMap 底层实现

**Q: HashMap 在 JDK 1.8 有什么优化？**

```
1.7 头插法 → 1.8 尾插法
原因：避免死循环（resize 时多线程并发）

1.8 优化：
- 数组 + 链表 + 红黑树（桶 >= 8 且数组 >= 64 转为红黑树）
- 优化了 hash 算法（扰动函数）
- 扩容时减少 rehash 操作

线程安全：
- HashMap 线程不安全
- ConcurrentHashMap 使用 CAS + synchronized
```

### 3.2 ConcurrentHashMap

**Q: ConcurrentHashMap 如何保证线程安全？**

```
JDK 1.7：Segment 分段锁
- 每个 Segment 独立加锁
- 并发度 = Segment 数量

JDK 1.8：
- CAS + synchronized
- synchronized 只锁当前桶的头节点
- 并发度 = 数组长度

put 逻辑：
1. 计算 hash
2. if (数组为空) CAS 初始化
3. if (桶为空) CAS 写入
4. if (桶正在扩容) 帮助扩容
5. synchronized 锁住头节点，遍历链表/红黑树
```

### 3.3 ArrayList vs LinkedList

**Q: ArrayList 和 LinkedList 如何选择？**

```
ArrayList：
- 底层：动态数组
- 优点：随机访问 O(1)、CPU 缓存友好
- 缺点：插入删除 O(n)、扩容成本高
- 使用场景：读多写少

LinkedList：
- 底层：双向链表
- 优点：插入删除 O(1)（已知位置）
- 缺点：随机访问 O(n)、内存碎片
- 使用场景：写多读少、队列实现
```

---

## 四、JVM 底层知识

### 4.1 垃圾回收算法

**Q: JVM 垃圾回收算法有哪些？**

```
1. 标记清除 (Mark-Sweep)
   - 缺点：产生内存碎片

2. 复制算法 (Copying)
   - 优点：无碎片、效率高
   - 缺点：浪费一半内存
   - 使用：年轻代 (Eden:Survivor = 8:1)

3. 标记整理 (Mark-Compact)
   - 优点：无碎片
   - 缺点：效率较低
   - 使用：老年代

4. 分代收集 (Generational)
   - 年轻代：复制算法
   - 老年代：标记整理/标记清除
```

### 4.2 垃圾收集器

**Q: CMS 和 G1 的区别？**

```
CMS (Concurrent Mark Sweep)：
- 老年代收集器
- 步骤：初始标记 → 并发标记 → 重新标记 → 并发清除
- 优点：并发收集、低停顿
- 缺点：产生内存碎片、CPU 敏感、浮动垃圾

G1 (Garbage First)：
- 面向服务端的垃圾收集器
- 将堆划分为多个 Region
- 优先回收价值最大的 Region
- 步骤：初始标记 → 并发标记 → 最终标记 → 筛选回收
- 优点：可预测停顿、无内存碎片
- 使用：JDK 9+ 默认
```

### 4.3 类加载机制

**Q: 双亲委派模型是什么？为什么要这样做？**

```
双亲委派模型：
1. Bootstrap ClassLoader → Extension ClassLoader → Application ClassLoader
2. 类加载时，先委托父类加载器加载
3. 父类无法加载时，才自己加载

为什么要这样做：
1. 防止类被重复加载
2. 防止核心 API 被篡改（如自定义 String 类）
3. 保证类的唯一性

破坏双亲委派：
- JDBC 驱动加载（Thread.currentThread().setContextClassLoader）
- OSGi、热部署
```

---

## 五、MySQL 底层知识

### 5.1 索引失效场景

**Q: 什么情况会导致索引失效？**

```
1. 使用函数/运算
   SELECT * FROM t WHERE YEAR(create_time) = 2024

2. 类型转换
   SELECT * FROM t WHERE id = '123'  (id 是 int)

3.  LIKE 前缀通配符
   SELECT * FROM t WHERE name LIKE '%abc'

4. OR 连接不同字段
   SELECT * FROM t WHERE id = 1 OR name = 'abc'
   (应该改为 UNION 或覆盖索引)

5. NOT IN / NOT EXISTS
   SELECT * FROM t WHERE id NOT IN (1, 2, 3)

6. 联合索引违背最左前缀原则
   ALTER TABLE t ADD INDEX idx(a, b, c);
   SELECT * FROM t WHERE b = 1;  -- 索引失效
```

### 5.2 事务隔离级别

**Q: MySQL 默认隔离级别是什么？如何解决幻读？**

```
默认隔离级别：REPEATABLE READ

隔离级别：
- READ UNCOMMITTED：脏读、不可重复读、幻读
- READ COMMITTED：解决脏读
- REPEATABLE READ (MySQL)：解决脏读、不可重复读
- SERIALIZABLE：解决所有问题，但性能最差

解决幻读：
- MVCC (Multi-Version Concurrency Control) + 快照读
- Next-Key Lock (当前读) 锁定记录 + 间隙锁

Next-Key Lock：
- 锁定记录本身
- 锁定记录之间的间隙
- 防止插入新记录
```

### 5.3 MySQL 优化

**Q: SQL 性能优化有哪些手段？**

```
1. 慢查询分析
   - 开启慢查询日志
   - 使用 EXPLAIN 分析执行计划

2. 索引优化
   - 合理创建索引（高频查询字段）
   - 避免索引失效
   - 使用覆盖索引减少回表

3. SQL 优化
   - 避免 SELECT *
   - 批量操作代替循环单条
   - 分页优化 (延迟关联)

4. 表结构优化
   - 字段类型选择（合适的大小）
   - 适当冗余减少 JOIN
   - 冷热数据分离

5. 架构优化
   - 主从复制读写分离
   - 分库分表
   - 缓存预热
```

---

## 六、项目相关面试问题

### 6.1 DDD 架构相关

**Q: 为什么选择 DDD 架构？相比传统三层架构有什么优势？**

```
1. 传统三层架构问题：
   - 业务逻辑散落在 Service 层
   - 贫血模型：对象只有 getter/setter
   - 代码膨胀后难以维护

2. DDD 优势：
   - 业务边界清晰（限界上下文）
   - 领域模型充血：业务逻辑内聚
   - 便于领域知识的沉淀和传递
   - 支持复杂业务场景

3. 适用场景：
   - 复杂业务逻辑
   - 团队协作
   - 长期维护的项目
```

### 6.2 支付系统相关

**Q: 如何设计一个可靠的支付系统？**

```
1. 幂等性设计
   - 使用订单号作为幂等键
   - 唯一索引 + 状态机校验

2. 回调安全
   - 签名验证
   - 回调幂等性
   - 异步处理避免阻塞

3. 资金安全
   - 对账系统
   - 差异预警
   - 人工审核机制

4. 容灾设计
   - 回调重试机制
   - 死信队列
   - 人工补偿
```

---

**重要提示**: 本文件是面试准备指南，可单独使用。进行代码审查时，请使用 REVIEW.md 作为审查规则。