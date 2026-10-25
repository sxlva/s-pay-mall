# LEARN_ROCKETMQ.md — RocketMQ 专项教材

> **课程定位**：STUDY_GUIDE Day 5 配套教材（用户重点学习模块），建议约 2 小时，含动手实验。
> **学习前提**：完成 Day 1–4（总纲 + 微信登录 + 支付宝支付教材），尤其是支付宝支付教材中"事务边界竖线"一节——本教材的大量结论建立在"状态推进在回调事务内同步完成"这一事实上。
> **证据标记**：【代码推导】= 仅阅读源码得出，未在当前工作区运行验证；【文档实测】= 2026-10-07 首次通过、**2026-10-19 当前工作区全量复跑通过（17/17，C-1 销项）**；【待验证】= 附编号，见 §5.6 与 STUDY_GUIDE 附录 C。
> **一句话总纲**：本项目用 RocketMQ 解决"支付成功后异步通知微信用户"和"超时自动关单"两个问题；**支付状态推进与库存持久化不在 MQ 上**，这是全教材最重要的口径，面试高危。

---

## 1. 问题背景

### 1.1 为什么支付回调里不能把所有活同步干完

支付宝异步通知你的服务器时，它等待的是一行 `success` 文本。如果你在回调处理里同步做这些事：

- 调用微信 API 发模板消息（一次外部 HTTP 调用，正常几百毫秒，异常时可能数秒）；
- 做任何未来可能扩展的重活（发短信、积分、推荐计算……）。

问题在于：

1. **支付宝有通知超时限制**，你的处理越慢，支付宝越可能判定通知失败而重发，放大流量；
2. **外部依赖的故障会传导到支付回调主链路**——微信 API 挂了，如果同步调用，支付状态推进明明已经成功，HTTP 响应却可能失败；
3. **回调线程被占住**，吞吐下降。

**本项目怎么做**：回调事务只做 DB 状态推进（订单 PAID、支付单 PAID、库存持久化），事务提交后把"支付成功"这件事作为一个**消息**丢给 RocketMQ，立刻返回 `success` 给支付宝。微信通知由独立的消费者异步做。【代码推导】

**局限**：异步意味着"支付成功"和"微信通知送达"之间出现一个时间差和可靠性缺口（消息可能丢），这个缺口的完整边界在 §5 分析。

### 1.2 为什么不能简单地 `new Thread()` / 线程池

面试常考。自己开线程做异步，对比 MQ 缺了四样东西：

| 能力 | `new Thread` | RocketMQ |
|------|-------------|----------|
| 进程崩溃后任务存活 | 否，线程随 JVM 一起死 | 是，消息在 broker 持久化 |
| 失败后自动重试 | 否，要自己写重试逻辑 | 是，broker 级重试（maxReconsumeTimes） |
| 下游故障时的缓冲 | 否，线程池满了只能拒绝或堆积在内存 | 是，消息积压在 broker，下游恢复后继续消费 |
| 多实例部署时的负载均衡 | 否，任务在哪台机器上就在哪台执行 | 是，同组消费者均分消息 |

**本项目怎么做**：没有用任何线程池方案，全部走 RocketMQ。【代码推导】

### 1.3 为什么是 RocketMQ 而不是 Guava EventBus——真实的演进史

你的记忆"以前是 Guava 实现的"是对的，但演进不止一步。以 Git 历史为准（以下提交哈希可在仓库中核实）：

1. **Guava EventBus 阶段**（进程内事件总线）：提交 `07ea1b7`（"feat: 支付回掉"）引入了 `GuavaConfig.java`（+9 行），与支付回调功能同期落地。EventBus 是**单 JVM 内**的发布/订阅，本质仍是同步方法调用（只是解耦了调用关系），进程一重启事件就丢了。
2. **RabbitMQ 阶段**：提交 `03314db`（"feat: 添加Redis服务支持，优化微信登录状态管理与消息发送"）**删除了 GuavaConfig.java**（−37 行），新增 `RabbitMQConfig.java`（+66 行）。这一步解决了跨进程持久化问题。
3. **RocketMQ 阶段**：提交 `10b3129`（"fix(pay): 支付状态判定修复（B1/B2）"）起 pom.xml 中出现 rocketmq 依赖；当前 `RocketMqOrderEventPublisher` 至少自 `1797b27` 起存在。

**当前状态**：全仓库已无任何 `com.google.common` 的 Java import（grep 为空），但 `pom.xml` L117-118 与 `s-pay-mall-start/pom.xml` L76-77 仍声明 guava 依赖——**这是一个死依赖**，面试如果被问"你项目里用了 Guava 什么"，诚实答案是"历史上用 EventBus 做进程内事件，已移除，依赖声明残留未清理"。【代码推导】

**教学要点**：不要把这个演进讲成"Guava 不好所以换了"。正确叙事是：**业务可靠性要求升级驱动工具升级**——单机事件总线 → 需要跨进程持久化与重试 → 选择 MQ。选型跟着需求走，这是面试官想听到的思维方式。

### 1.4 MQ 在本项目实际承担什么（最重要的一节）

在往下读之前，先建立 Topic 全景。下表是本教材的"地图"，请先记住它，§3 会展开：

| Topic | 生产者 | 消费者（消费组） | 消息体 | 用途 |
|-------|--------|-----------------|--------|------|
| `order_paid` | `RocketMqOrderEventPublisher`（支付回调事务提交后；超时关单"晚到支付"救援路径） | `OrderPaidRocketListener`（`s-pay-mall-order-paid-consumer`） | `PaySuccessMessage{tradeNo, orderNo}` | 支付成功事件 |
| `order-timeout-topic` | `OrderPaymentGatewayImpl.sendDelayCloseMessage`（下单事务提交后） | `OrderTimeoutCloseRocketListener`（`s-pay-mall-timeout-group`） | `String orderNo`，delayLevel=9（代码注释称 30 分钟） | 超时关单 |
| `%DLQ%s-pay-mall-order-paid-consumer` | broker 自动转入 | `DlqAlertListeners.OrderPaidDlqAlertListener` | 原消息体字符串 | 死信告警 |
| `%DLQ%s-pay-mall-timeout-group` | broker 自动转入 | `DlqAlertListeners.TimeoutCloseDlqAlertListener` | 原消息体字符串 | 死信告警 |
| `product-stock-change-topic` | **无生产者**（TD-9 考证，有意保留） | —（消费组存在但收不到消息） | — | 预留 |

**关键口径（附录 A-4，面试高危）**：`order_paid` 的消费者执行 `paySuccess` 时，订单状态**早已被回调事务同步推进为 PAID**，`OrderEntity.canPay()`（`OrderEntity.java` L53，仅 `state == INIT` 为真）返回 false，`OrderStateMachineServiceImpl.paySuccess`（L45-48）直接返回 false——**消费侧的 paySuccess 是恒空转**。MQ 消费侧真正干活的只有 `sendPaySuccessNotification`（微信模板消息编排）。

所以：**本项目的 MQ 实际职责 = 支付成功后的微信通知编排 + 超时关单触发**。它不是履约驱动架构。谁在面试中把这条讲成"支付成功后发消息异步履约"，就违背了代码事实。

---

## 2. 前置知识（约 50 分钟）

> 按"先讲原理"的要求，这一节只讲概念，不绑定项目代码。每小节末尾给"本项目对应什么"。

### 2.1 Broker、Topic、消费组

- **Broker**：MQ 服务器，消息在这持久化。生产者和消费者都连它。
- **Topic**：消息的逻辑分类，生产者发到哪个 topic，消费者订阅哪个 topic。本项目有 `order_paid`、`order-timeout-topic` 两个业务 topic（见 §1.4 矩阵）。
- **消费组（consumer group）**：一组消费者的名字。**同组**消费者瓜分消息（集群模式，默认）：一条消息只被组内一个实例消费；**不同组**各自消费一份（广播）。本项目两个消费组 `s-pay-mall-order-paid-consumer`、`s-pay-mall-timeout-group`，注解见 `OrderPaidRocketListener.java` L21-22、`OrderTimeoutCloseRocketListener.java` L18-19。

### 2.2 at-least-once 语义

RocketMQ 默认提供的是**至少一次（at-least-once）**：消息不会丢（只要 broker 没崩且配置正常），但**可能重复投递**。典型重复来源：

1. broker 把消息投给消费者后，**没等到确认**（消费者宕机、网络中断、处理超时）→ broker 认为没消费成功，重投；
2. 消费者处理成功但**返回确认前宕机** → broker 同样重投。

**推论：消费端必须自己做幂等**。这是全教材第二重要的原理句。

### 2.3 确认（ACK）机制

`RocketMQListener` 的语义（rocketmq-spring）：`onMessage` 方法**正常返回即视为 ACK**；**抛出异常即视为消费失败**，触发重试。对应到代码：

- `OrderPaidRocketListener.onMessage` 成功 return → ACK（L52、L57-59 路径）；
- 捕获异常后 L65 `throw new RuntimeException(...)` → 不 ACK → broker 重投。

### 2.4 重试与死信（DLQ）

消费失败的重试节奏由 broker 控制（默认按延迟级别递增，具体间隔以 broker 配置为准，不必背）。**重试次数上限**由消费端注解 `maxReconsumeTimes` 指定，本项目两个业务消费组都是 **5**（`OrderPaidRocketListener` L22、`OrderTimeoutCloseRocketListener` L19）。

超过 5 次仍失败，broker 把消息转入 **`%DLQ%<消费组名>`** 死信主题。死信不会再被自动投递——**它是一道"人工介入"的闸门**。本项目对两个 DLQ 各挂了一个告警监听器（§4.7）：收到死信打 ERROR 日志并完整落消息体，作为人工重放素材。

### 2.5 延时消息

RocketMQ 支持发送时指定 `delayLevel`，消息到期后才对消费组可见。本项目用它实现"下单 30 分钟后检查未支付则关单"（`OrderPaymentGatewayImpl.java` L42，`delayLevel=9`）。

delayLevel 与真实延迟时长的映射是 **broker 配置项**，RocketMQ 官方默认级别表为：1=1s、2=5s、3=10s、4=30s、5=1m、6=2m、7=5m、8=10m、**9=30m**、10=1h……代码注释（L38-40）写明"level 9 对应 30 分钟"，且注释记录了 B2 修复前是 level 5（1 分钟，用户付款时订单可能已被关闭）。**注意**：这是官方默认表 + 代码注释的陈述，本地 broker 若改过配置则不一定；实际投递时延未在当前工作区实测【待验证 C-4】。

### 2.6 四个必须分清的可靠性边界

后面 §5 反复用到，先建立词汇表。一条消息从产生到生效要经过四道门：

1. **数据库事务提交**（本地可靠性）；
2. **消息发送到 broker**（投递可靠性）；
3. **消费者处理成功**（消费可靠性）；
4. **消费者返回 ACK**（确认可靠性）。

任何一道门出问题，现象和补救手段都不同。"发送成功 ≠ 消费成功"、"消费成功 ≠ 不会重投"，这两句话请记牢。

---

## 3. 完整业务流程

### 3.1 支付成功消息的一生（时序图）

```
支付宝 ──异步通知──▶ 回调处理
                        │
                        ▼
              ┌─ 事务开始 ─┐
              │ 1. 晚到支付检查（已关闭则记 ERROR 返回）
              │ 2. 条件更新订单 INIT→PAID（影响行数=0 则跳过）
              │ 3. pay_order → PAID
              │ 4. MySQL 库存持久化（syncDBStockForPaySuccess）
              └─ 事务提交 ─┘          ◀── 此刻"支付成功"已是 DB 事实
                        │
                        ▼  事务外
              5. RocketMqOrderEventPublisher.publishPaySuccess
                 syncSend("order_paid", {tradeNo, orderNo}, 超时3000ms)
                 【失败仅记日志，不回滚事务】
                        │
                        ▼
              返回 "success" 给支付宝（终止其重试）
                        │
                        │        broker 持久化消息
                        ▼
              OrderPaidRocketListener.onMessage
                 6. SETNX 幂等键 tryAcquire（key=stock:event:order_paid_notify:{orderNo}）
                    已存在 → 记日志 return（ACK，跳过）
                 7. paySuccess(orderNo) → canPay=false → 空转返回
                 8. sendPaySuccessNotification → 查订单 → 查 openid → 微信模板消息
                 9. return（ACK）── 成功路径不释放幂等键，24h 自动过期
```

**阅读要点**：

- 步骤 2-4 与步骤 5 之间的事务竖线是支付宝支付教材的核心图，本教材不再重复论证，但 §5 的场景分析全靠它；
- 步骤 7 的"空转"不是 bug，是当前架构的必然结果（状态推进已同步完成），见 §1.4 口径；
- 步骤 8 的失败**不会**让 ACK 失败——`sendPaySuccessNotification` 内部 catch-all（见 §5.3）。

**生产触发点**（三处，都汇到同一方法 `OrderApplicationService.changeOrderPaySuccess`，L369-392）：

1. 支付宝异步通知正常路径：`changeOrderPaySuccess` L357 调用点（ notify handler 内）；
2. **晚到支付救援**：`handleTimeoutCloseOrder`（L470-480）查支付宝发现"其实已支付"时，转 `changeOrderPaySuccess`（L480），同样会发消息；
3. **主动查单补偿**：`NoPayNotifyOrderJob`（`NoPayNotifyOrderJob.java` L93）对"支付宝说已付但没收到通知"的订单调 `changeOrderPaySuccess`。

### 3.2 延时关单消息的一生

```
用户下单                    事务内：创建订单 + Redis 预扣库存
   │                                │
   │ 事务提交后（L154 注释明确：避免在事务内部发送导致事务 hold）
   ▼
OrderPaymentGatewayImpl.sendDelayCloseMessage(orderNo)
   syncSend("order-timeout-topic", orderNo, 3000ms, delayLevel=9)
   【失败仅记日志】
   │
   ▼  （约 30 分钟后到期，以 broker 配置为准【待验证 C-4】）
OrderTimeoutCloseRocketListener.onMessage(orderNo)
   → handleTimeoutCloseOrder(orderNo)
       ① 查订单，不存在/状态不符 → return false（记"未执行"）
       ② 向支付宝发起交易查询
          - 支付宝说"未支付/交易不存在" → 关单 + 库存恢复
          - 支付宝说"已支付"       → 转 changeOrderPaySuccess（晚到支付救援，见 §3.1 触发点2）
       异常 → throw，broker 重试，5 次耗尽进 DLQ
```

**为什么关单前必须查支付宝**（面试高频）：延时消息到期的瞬间用户可能正在付款。"到期"只说明"时间过了"，不说明"用户没付钱"。直接关单会造成"钱已收、订单死"。当前实现用支付宝二次确认兜底，这是设计取舍（§7.3 会对比"直接关单"方案）。

### 3.3 关单的三道防线（预告）

延时消息只是第一道。`NoPayNotifyOrderJob`（`s-pay-mall-trigger/.../job/NoPayNotifyOrderJob.java`）提供两道兜底：

- **40 分钟兜底关单**：查 `pay_order` 中 WAIT_PAY 且创建超过 40 分钟的订单（`IOrderDao.java` L73-74，SQL：`status='WAIT_PAY' and create_time < DATE_SUB(NOW(), INTERVAL 40 MINUTE)`），走 `handleTimeoutCloseOrder` 同一路径（L110）；
- **5 分钟主动查单**：WAIT_PAY 超过 5 分钟（`IOrderDao.java` L63-64）的订单向支付宝查交易状态，已付则补 `changeOrderPaySuccess`（L93）。

完整论证（含为什么 30/40 分钟窗口、防重入设计）是教材四（`LEARN_ORDER_STOCK_CONSISTENCY.md`，Day 6）的内容，本节只需知道：**延时消息丢失/消费失败不至于漏关单，因为还有 Job 轮询兜底**。这也是"不能把 MQ 可靠性单独评估"的原因——系统可靠性是组合拳。

---

## 4. 源码精读（按执行顺序）

> 建议对照源码阅读。每处给出"这段代码在做什么 → 为什么这样写 → 局限是什么"。

### 4.1 生产者：`RocketMqOrderEventPublisher`

`s-pay-mall-infrastructure/src/main/java/cn/fcr/infrastructure/order/event/RocketMqOrderEventPublisher.java`

```java
// L25-41（节选）
public void publishPaySuccess(String tradeNo, String orderNo) {
    try {
        // L27-28 注释：消息契约统一为 {tradeNo, orderNo}，userId 由消费方反查
        PaySuccessMessage message = ...builder().tradeNo(tradeNo).orderNo(orderNo).build();
        // P1-4：显式 3000ms 发送超时
        rocketMQTemplate.syncSend(TOPIC, MessageBuilder.withPayload(message).build(), 3000L);
    } catch (Exception e) {
        // L38：发送失败仅记日志
        log.error("发送支付成功消息失败，但不影响订单状态更新: ...");
    }
}
```

- **为什么 3000ms 超时**：`convertAndSend` 无超时重载（L33 注释），`syncSend` 显式超时避免发送线程无限挂起——发送 hang 住会拖慢回调响应，支付宝会重发通知放大流量。
- **为什么 catch-all 吞掉**：发送失败不回滚（消息在事务外，事务已提交，无从回滚）。代价就是 §5 场景 2 的可靠性缺口。
- **局限**：消息体只有 `tradeNo` + `orderNo`，消费侧需要的信息（userId、商品名、金额）全部要反查——这是"瘦消息"设计，省带宽增查询，同时保证了消息体不含敏感数据。

### 4.2 延时消息生产：`OrderPaymentGatewayImpl.sendDelayCloseMessage`

`s-pay-mall-infrastructure/src/main/java/cn/fcr/infrastructure/order/gateway/OrderPaymentGatewayImpl.java` L35-47

- L42：`syncSend("order-timeout-topic", message, 3000, 9)`——第 4 个参数 9 就是 delayLevel；
- L38-40 注释是**宝贵的设计记录**：修复前是 level 5（1 分钟），与业务支付超时窗口不一致，导致"用户付款时订单可能已被关闭，形成钱已收、订单死的死角"。面试可以直接引用这个 B2 案例，证明你理解"延时级别必须匹配业务窗口"；
- 调用点在 `OrderApplicationService` L202，且 L154 注释明确**发送在事务提交后**（避免事务 hold）——"事务内不落 MQ"是本项目两条消息生产的统一纪律。

### 4.3 支付成功消费者：`OrderPaidRocketListener`

`s-pay-mall-trigger/src/main/java/cn/fcr/trigger/listener/OrderPaidRocketListener.java`

- **L21-22 注解**：topic、`consumerGroup = "s-pay-mall-order-paid-consumer"`、`maxReconsumeTimes = 5`——三个可靠性参数都在这一行；
- **L48-53 幂等守门**：`idempotentGateway.tryAcquire(BUSINESS_TYPE_ORDER_PAID_NOTIFY, orderNo)`，拿不到键 → 记日志 return（**不抛异常**）。注意这个细节：重复消息是"正常 return"而不是抛异常，**故意避免无意义重投**——如果抛异常，broker 会再投 5 次，全是浪费；
- **L55-59 业务委托**：`paySuccess`（空转，§1.4）→ `sendPaySuccessNotification`；
- **L61-66 异常路径**：释放幂等键 → 重抛。释放键是为了让重试能重新拿到键、重新执行；重抛是让 broker 知道要重试。两个动作缺一不可，顺序也不能反；
- **局限**：成功路径**不释放键**（注释 L37-38：键 24h 自动过期）。这把双刃剑：24h 内重投一律跳过（幂等性强）；24h 后若 broker 仍重投（极端积压场景），会重新执行——届时 paySuccess 依旧空转，但微信通知可能重复发送一次。这是已知的、可接受的边界（通知重复不构成资损）。

### 4.4 消费侧业务：`OrderApplicationService` 三个方法

`s-pay-mall-application/src/main/java/cn/fcr/application/OrderApplicationService.java`

- **L258-261 `paySuccess`**：`@Transactional` 包一层 `mallOrderService.paySuccess`。链路：`OrderStateMachineServiceImpl` L45 `canPay()` 守卫 → L51-52 条件更新（`INIT→PAID`，影响行数 0 即并发/重复，拒绝）→ L59 pay_order → L62 `syncDBStockForPaySuccess`。在当前架构下 L45 就 return false——但**注意**：这段状态机代码同时被"晚到支付救援/Job 补单"路径使用，那些路径里订单可能仍是 INIT，paySuccess 是**真的会跑完整状态推进**的。所以说"消费侧恒空转"要精确：恒空转的前提是"回调事务已先行推进状态"；消费侧代码本身是完整的状态机，不是摆设。【代码推导】
- **L369-392 `changeOrderPaySuccess`**：§3.1 的 1-5 步全在这里。重点再看 L379-384：只有 `changeOrderPaySuccessInTransaction` 返回 true（**本次实际发生状态流转**）才发消息——TD-6 防重复发事件。晚到支付分支（L370-377）记 ERROR 后 return，**不发事件**。
- **L402-428 `sendPaySuccessNotification`**：查订单 → 查 openid（未绑定则跳过）→ 微信模板消息。L425-427 **catch-all 吞异常**：通知失败仅记日志，不影响 ACK。这个设计的后果是 §5.3 的边界：通知失败不触发任何重试。

### 4.5 超时关单消费者：`OrderTimeoutCloseRocketListener`

`s-pay-mall-trigger/src/main/java/cn/fcr/trigger/listener/OrderTimeoutCloseRocketListener.java` L34-50

- **没有消费侧幂等键**。防重复靠 `handleTimeoutCloseOrder` 内部的状态检查（订单已关闭/已支付时返回 false，L42-44 记"未执行"）。为什么可以没有键：关单本身是幂等操作（关一次和关十次结果相同），且内部走条件更新兜底；
- 异常直接重抛（L48），走 broker 重试 → 5 次耗尽 → `%DLQ%s-pay-mall-timeout-group` → 告警监听器。

### 4.6 幂等原语：`IdempotentGatewayImpl`

`s-pay-mall-infrastructure/src/main/java/cn/fcr/infrastructure/mall/product/gateway/IdempotentGatewayImpl.java`

- L27：键前缀 `stock:event:`；L32：过期 24 小时；L37：值固定 `PROCESSING`；
- L50 `bucket.trySet(...)` 就是 **SETNX**（SET if Not eXists）：键不存在才设置成功并带 TTL。返回 true=首次，false=已存在；
- 消费幂等键完整形态：`stock:event:order_paid_notify:{orderNo}`；
- **作用边界（精确表述）**：SETNX 做的是"**24 小时窗口内抑制重复投递导致的重复执行**"。它不是 exactly-once：窗口外重投拦不住；`tryAcquire` 成功到 `release`/过期之间的崩溃窗口（§5.2 W3）也拦不住"处理失败但键未释放"的残留（本项目靠 TTL 兜底）。答题时用"窗口内抑制重复执行 + 24h TTL 兜底"，不要说"保证恰好一次"。

### 4.7 死信告警：`DlqAlertListeners`

`s-pay-mall-trigger/src/main/java/cn/fcr/trigger/listener/DlqAlertListeners.java`

- 两个静态内部类分别监听两个 DLQ topic（L28-29、L42-43），收到死信打 ERROR 日志、完整消息体落日志（L33、L47）；
- L13-14 注释说明人工重放路径：**RocketMQ 控制台"消息查询 → 重新发送"**；
- L16-17 注释说明 `product-stock-change-topic` 无生产者（TD-9 考证），其 DLQ 永不写入，**故不配置告警监听器**——这个"为什么没有第三个监听器"的解释本身就是面试细节题。

---

## 5. 异常与边界（本教材核心）

### 5.1 四种故障场景速查表（与 STUDY_GUIDE §八同口径）

| 场景 | 发生位置 | 当前实现的结果 | 证据 |
|------|---------|---------------|------|
| **S1 数据库事务提交失败** | `changeOrderPaySuccessInTransaction` 回滚 | 订单仍是 INIT/WAIT_PAY，消息**不会发送**（L380 在事务方法之后）。支付宝重试通知/Job 补单/延时关单会再次触发。系统自洽 | 【代码推导】 |
| **S2 DB 提交成功但消息发送失败** | 事务提交后、`syncSend` 抛异常 | `publishPaySuccess` catch-all 仅记日志（L37-40）。**消息丢了，且无任何自动补偿**（无 outbox、无本地消息表）。在当前职责边界下：订单状态、库存均已正确，**只损失微信通知**（前提 P1-P3 见下） | 【代码推导】 |
| **S3 消息发送成功但消费失败** | 消费者处理抛异常 | L64 释放幂等键 + L65 重抛 → broker 重试，最多 5 次；5 次耗尽进 DLQ，告警监听器打 ERROR 留痕，**人工控制台重放**。幂等键释放保证重试能真正重执行 | 【代码推导】 |
| **S4 消费成功但确认失败** | `onMessage` 已执行完、ACK 前宕机 | broker 视为失败而重投 → 到消费者时幂等键**仍存在**（成功路径不释放）→ L50-52 跳过。**at-least-once + 消费幂等 = 24h 窗口内效果等价 exactly-once**，窗口外（极端积压超 24h）可能重复执行，后果仅为微信通知可能重发 | 【代码推导】 |

**S2 的"不构成资损"成立前提**（缺一不可，答题必须带）：

- **P1**：支付状态推进以回调事务为准且在事务内同步完成——消息不是状态推进的载体；
- **P2**：消费侧 paySuccess 不承载库存扣减等资金/库存副作用（库存持久化已在回调事务内由 `syncDBStockForPaySuccess` 完成）；
- **P3**：当前 MQ 职责仅限通知编排。若未来把库存扣减、履约等关键业务移到消费者执行，S2 就是资损级缺口，必须配 outbox/事务消息（U-2 已论证）。

### 5.2 三个崩溃窗口（W1/W2/W3）

进程在任意时刻崩溃，落在哪个窗口决定后果：

```
事务提交 ●━━━━━ W1 ━━━━━━● 发送成功 ●━━━━━ W2 ━━━━━━● 消费完成 ●━━━━ W3 ━━━━● ACK
         │                │                        │                      │
     S1 自洽          S2 消息丢失              重投→SETNX跳过          S4 重投→SETNX跳过
     重试通知兜底      仅损通知(前提P1-P3)      (窗口内幂等)            (窗口内幂等)
```

- **W1**：事务已提交、消息未发出。等价 S2；
- **W2**：消息已发、消费者未处理完。重投 → 幂等键不存在（首次）→ 正常消费；若首次消费已部分执行后崩溃，则落到 W3 讨论；
- **W3**：消费完成、ACK 前崩溃。重投 → 幂等键存在 → 跳过。**注意一个细节**：W3 里"消费完成"指 `paySuccess` 空转 + `sendPaySuccessNotification` 已发出。跳过意味着不再补发通知——若崩溃点在"通知已发"之前（通知没发出去但幂等键未释放的路径），重投被跳过后**通知永久丢失**（成功路径不释放键、异常路径才释放；通知异常被吞见 §5.3）。这是窗口内的一个已知死角，量级是"个别用户收不到支付通知"，非资损。

### 5.3 通知异常被吞的边界

`sendPaySuccessNotification` 的 catch-all（L425-427）意味着：微信 API 故障、openid 查询失败等**任何**通知环节异常，都只是一行 ERROR 日志：

- 不触发 broker 重试（没抛出去）；
- 无独立补偿任务；
- 恢复手段：人工核实后补发【待验证：是否存在任何补发入口，当前未发现】。

这与教材二场景 G 的口径一致。答题模板："当前把微信通知定位为 best-effort 渠道，失败留痕、人工兜底；若要求必达，需要引入通知记录表 + 重试任务（可列入升级点）。"

### 5.4 SETNX 幂等键的作用边界（再强调）

- 键值恒为 `PROCESSING`，没有"成功/失败"状态区分，`markDone` 方法（L89-101）在当前消费链路**未被调用**——键的生命周期就是"24h 后自动消失"；
- 24h 窗口的选择依据：支付宝通知重试与 broker 重投都不会跨这个量级（支付宝重试通常 24h 内结束【文档实测/平台协议，具体参数不背，C-6】）；窗口过长会拖长"失败后想人工重放却被幂等键拦住"的时间，过短则失去抑制作用；
- 消费侧幂等键前缀 `stock:event:` 是历史沿用命名（本类位于 mall/product 包下，实际被订单消费链路使用）——命名与归属不一致是历史债，面试问到"键为什么叫 stock 开头"可以如实回答。

### 5.5 延时消息的两道边界

1. **发送失败**（`sendDelayCloseMessage` catch-all）：消息丢失 → 该订单失去 30 分钟准点关单触发。但 40 分钟 Job 兜底（§3.3）会捞回，最多晚 10 分钟关单【代码推导】；
2. **到期瞬间用户正在付款**：靠 `handleTimeoutCloseOrder` 内支付宝二次确认解决（§3.2）。【文档实测】`TimeoutCloseOrderE2EIT`（`s-pay-mall-start/src/test/...`）覆盖了"关单前查询支付宝，已支付则转履约"的救援路径——2026-10-19 已随全量 E2E 在当前工作区复跑通过（C-1 销项）。

### 5.6 本教材待验证事项

| 编号 | 事项 | 验证方法 |
|------|------|---------|
| C-4 | delayLevel=9 实际投递时延是否 30 分钟 | 发送延时消息记录时间差（实验 E4） |
| C-5 | ~~消费失败后重试 5 次进 DLQ、告警日志出现~~ **已于 2026-10-12 实测通过（见 §6-E3 实测记录）** | 实验 E3 |
| C-6 | 支付宝通知重试时长/次数的协议参数 | 查官方文档，不背数字、不写死 |
| C-1 | ~~10-07 测试报告的全量结论在当前工作区复跑~~ **已于 2026-10-19 复跑通过（17/17，BUILD SUCCESS）** | 跑通 E2E 测试套件 |

---

## 6. 动手实验

> 环境依赖：MySQL（映射端口 23306）、Redis（26379）、RocketMQ（9876），均为 `docker-compose` 映射。以下实验不影响业务代码。

### E1 环境确认（10 分钟）

```bash
docker compose ps   # 确认 mysql/redis/rocketmq 存活
redis-cli -p 26379 ping   # 期望 PONG
```

完成标准：三端都通，能在 RocketMQ 控制台（容器映射端口，以 compose 文件为准）看到 Topic 列表。

### E2 正常消费观察（20 分钟）

1. 启动后端；
2. RocketMQ 控制台 → 消息查询/发送测试消息 → 向 `order_paid` 投递一条 `{"tradeNo":"TEST001","orderNo":"TESTORDER001"}`；
3. 观察后端日志：应依次出现 `OrderPaidRocketListener` L44 "收到支付成功消息" → 幂等获取成功 → `paySuccess` 的"订单状态不允许更新"warn（空转，因为 TESTORDER001 不存在或状态不符）→ 通知环节"订单不存在"warn；
4. 用 `redis-cli -p 26379 GET "stock:event:order_paid_notify:TESTORDER001"` 应看到 `PROCESSING`，约 24h 后消失。

完成标准：能指出每一步对应源码行号，并能解释为什么 paySuccess 是空转。

> **实测记录（2026-10-12，当前工作区）**：全部通过。向 `order_paid` 投递 `{"tradeNo":"TEST002","orderNo":"TESTORDER002"}` 后 4 秒内日志依次出现：L44 收到消息 → `IdempotentGatewayImpl` 获取锁成功（key=`stock:event:order_paid_notify:TESTORDER002`）→ 状态机"订单不存在"warn（paySuccess 空转的实测形态）→ 通知环节"订单不存在"warn。Redis 1 号库（注意：`application-dev.yml` L63 `database: 1`，不是 0 号库）中该键值为 `PROCESSING`、TTL 86318s≈24h。另启动日志确认 5 个监听容器全部注册（含 TD-9 的 `product-stock-change-topic` 消费组）。

### E3 重试与死信（核心实验，30 分钟）【待验证 C-5 的验证过程】

1. 停掉 MySQL：`docker compose stop mysql`（让 DB 操作必然失败）；
2. 向 `order_paid` 投递测试消息；
3. 观察日志：处理异常 → 释放幂等键 → 重抛 → broker 重投（重试间隔递增，以 broker 配置为准），循环至 5 次；
4. 第 5 次失败后：控制台出现 `%DLQ%s-pay-mall-order-paid-consumer` 主题，消息在其中；
5. 后端日志出现 `【DLQ告警】order_paid 消息进入死信队列（重试5次耗尽）…`，消息体完整落日志；
6. 恢复 MySQL，控制台对该死信"重新发送"→ 观察正常消费。

完成标准：能口述 S3 完整链条（异常→释放键→重试→DLQ→告警→人工重放），并解释为什么第一次重试前必须释放幂等键。

> **实测记录（2026-10-12，当前工作区）**：S3 链条全部验证通过，C-5 销项。停 MySQL 后投递 `TESTORDER003`：
>
> | 时间 | 事件 |
> |------|------|
> | 22:37:01 | 首次消费：SETNX 成功 → `paySuccess` 查库抛 `CJCommunicationsException: Connection refused`（Hikari 取连接约 30s 超时） |
> | 22:37:31 | catch 路径：`【幂等性释放】释放锁成功` → 重抛；Redis 键确认已删除（异常路径释放幂等键的实证） |
> | 22:37:41 | broker 第 1 次重投（+10s） |
> | 22:38:41 / 22:39:41 / 22:41:41 / 22:46:41 | 第 2~5 次重投，间隔 +30s/+1m/+2m/+5m（broker 默认 messageDelayLevel 级别 1~5，**实测值**；这是 broker 配置不是项目配置） |
> | 22:46:42 | 第 6 次投递（=1+5 次重试）失败，`DefaultRocketMQListenerContainer` 打 `reconsumeTimes:5` WARN，消息转入 `%DLQ%s-pay-mall-order-paid-consumer`（mqadmin topicStatus 确认 Max Offset=1） |
> | 22:46:42 | DLQ 告警监听器消费偏移推进（消费成功） |
> | 22:49:56 | 向 DLQ 主题直接补投一条验证告警输出：`【DLQ告警】order_paid 消息进入死信队列（重试5次耗尽），需人工核实并重放: body=...`（ERROR 级、消息体完整、同时落 log_error.log） |
> | 22:51:22 | 恢复 MySQL 后向 `order_paid` 重放 `TESTORDER003`：SETNX 重新获取成功（证明失败期间键确实被释放）→ 完整消费 → ACK；Redis 键重新建立，TTL≈24h |
>
> **已知观察（诚实记录）**：上述 22:46:42 首次死信的告警日志行未落盘（broker 偏移确认消费成功，但应用日志缺失）；另 22:31:16 一条首投消息同样"已消费无日志"。直接补投复测证明监听器日志路径本身正常，判断为日志框架瞬时丢失（logback 配置了 `neverBlock` 异步文件输出器，理论上有丢弃窗口），不影响业务行为结论—— broker 偏移是消费事实的最终依据。如果你在复跑时也遇到"消费了但没日志"，先查 broker 偏移再怀疑业务代码。

### E4 延时消息时延（可选，需等待约 30 分钟）【待验证 C-4】

向 `order-timeout-topic` 发送一条带 delayLevel=9 的测试消息（控制台或代码），记录发送与投递时间差。若实测不是 30 分钟，说明本地 broker 级别表被改过，修正 §2.5 的表述。

### E5 延时关单链路（20 分钟，依赖 E1）【文档实测路径，当前未复跑】

走 `TimeoutCloseOrderE2EIT` 的思路：造一个 INIT 状态订单 → 直接调 `handleTimeoutCloseOrder` → 验证"支付宝说未付则关单+恢复库存；说已付则转 changeOrderPaySuccess"。完整步骤留给教材四。

---

## 7. 设计取舍

### 7.1 事务后发送 + catch（当前方案） vs outbox / RocketMQ 事务消息

**当前方案**：DB 事务提交后 `syncSend`，失败仅记日志。S2 缺口：消息可能丢失，无自动补偿。

**备选 A：本地消息表（outbox）**。在同一事务里往 `message_outbox` 表插一条记录；后台任务扫表发消息、成功后标记。优点：DB 提交与"待发送"记录原子一致，S2 缺口关闭；缺点：多一张表、一个轮询任务、至少一次投递语义不变（消费幂等仍要做）。

**备选 B：RocketMQ 事务消息**。半消息 + 本地事务回调 + 回查。优点：broker 侧保证发送与本地事务一致；缺点：概念复杂、回查接口要实现、排查链路变长——对毕设/中小项目过度设计。

**本项目的选择与理由（U-2 已定口径）**：当前职责下（通知编排），S2 的代价只是"个别用户收不到微信通知"，人工可补，因此选了最易理解、最易排查的方案；outbox 被列为升级点而非本期实现。**局限必须诚实**：这个选择依赖 P1-P3 前提，职责一旦扩大到资金/库存业务就必须升级。这是答辩安全线——老师问"消息丢了怎么办"，答"当前职责下损失可容忍、有告警留痕、升级路径是 outbox"，远比假装"不会丢"得分高。

### 7.2 Redis SETNX vs 消费记录表

| 维度 | SETNX + TTL（当前） | 消费记录表（MySQL） |
|------|--------------------|---------------------|
| 实现成本 | 现成 Redisson，5 行代码 | 建表 + 写入 + 查询 |
| 24h 后语义 | 键消失，窗口外重投拦不住 | 记录持久可查，可无限期防重 |
| 崩溃恢复 | TTL 兜底 | 事务内同事务提交，最稳 |
| 本项目选型理由 | 消费侧副作用只是通知，窗口外重复=多推一条通知，可接受 | 留给资金/库存类消费者（未来） |

### 7.3 延时消息 vs 纯定时轮询

- **纯 Job 轮询**：只保留 5/40 分钟 SQL 扫描。优点：去掉 MQ 依赖；缺点：扫描间隔决定关单精度（1 分钟扫一次就最多晚 1 分钟，但 DB 压力 ×N），且"到期"语义不清晰；
- **纯延时消息**：去掉 Job。优点：准点；缺点：消息丢了/消费失败 5 次耗尽 = 订单永不关闭（DLQ 只告警不自动处理）；
- **当前组合（30min 延时 + 40min Job 兜底）**：准点性与可靠性兼得，成本是两套机制并存、要维护窗口关系（30 < 40，兜底才有意义）。这是教科书式的"消息 + 轮询互补"设计，值得在面试中主动讲。

### 7.4 演进复盘：Guava → RabbitMQ → RocketMQ

回到 §1.3 的历史。三步各自的驱动力：

1. Guava EventBus → RabbitMQ：需要**跨进程持久化 + 重试**（单机事件总线重启即丢）；
2. RabbitMQ → RocketMQ：与 B1/B2 支付修复同期（`10b3129`），社区 Java 生态中 RocketMQ 的延迟消息、重试/DLQ 模型对"超时关单"场景更顺手，且阿里系 SDK 与支付宝场景亲和。⚠️ 诚实声明：第二步换型的**确切动机在仓库内没有文档记录**，以上是合理推断【待验证】。面试建议表述："历史选型随可靠性需求和场景匹配演进，当前以 RocketMQ 为准"，不要编造确定理由。

---

## 8. 面试与答辩

### Q1（基础）你这个项目为什么用 MQ？

> 两处真实用途：一是支付成功后异步发微信模板通知，把外部 HTTP 调用移出支付宝回调主链路；二是下单发 30 分钟延时消息做超时关单。没有用 MQ 驱动履约——状态推进在回调事务内同步完成。源码：生产者 `RocketMqOrderEventPublisher` L34，消费者 `OrderPaidRocketListener` L21。

### Q2 消息发送成功是不是就代表消费成功？

> 不是，这是投递和消费两个可靠性边界。发送成功只到 broker；消费可能失败（S3）、ACK 可能丢失（S4）。所以消费端必须有幂等：本项目用 Redis SETNX，键 `stock:event:order_paid_notify:{orderNo}`，24h TTL，见 `IdempotentGatewayImpl` L50 和 `OrderPaidRocketListener` L48-53。

### Q3 重复消费怎么防止？

> 三层：① broker at-least-once 保证不丢但可能重投；② 消费端 SETNX 幂等键，重复消息直接 return（注意是 return 不是抛异常，避免无意义重投 5 次）；③ 真正写库的操作靠状态机条件更新兜底（`OrderStateMachineServiceImpl` L51-52，影响行数 0 即拒绝）。注意 SETNX 是 24h 窗口内抑制，不是 exactly-once。

### Q4 消息丢了怎么办？

> 分两种丢：发送时丢（S2，DB 已提交但 syncSend 失败）——当前无自动补偿，是已知缺口，职责限于通知所以损失可容忍，升级路径是 outbox；投递中丢——broker 持久化 + 消费失败重试 5 次 + 死信进 DLQ 告警（`DlqAlertListeners`），人工控制台重放。我不会声称系统不会丢消息。

### Q5 延时关单为什么要查支付宝？直接关不行吗？

> 延时消息到期只说明时间到了，不说明用户没付款——到期瞬间用户可能正在支付。直接关单会造成"钱已收、订单死"。当前实现在 `handleTimeoutCloseOrder` 里向支付宝二次确认：未付才关单+恢复库存，已付则转支付成功处理（晚到支付救援）。另外延时消息失败还有 40 分钟 Job 兜底（`IOrderDao` L73-74），消息丢了也不至于漏关单。

### Q6（陷阱）支付成功之后是不是发消息异步履约？

> 不是。这是最容易被文档误导的一点：回调事务内同步完成订单 PAID、支付单 PAID、库存持久化（`changeOrderPaySuccess` L369-392 → `OrderStateMachineServiceImpl` L50-62）；MQ 消息在事务提交后发送，消费侧 paySuccess 因 canPay 守卫恒为空转（订单已是 PAID），消费侧实际只编排微信通知。MQ 是通知渠道，不是履约引擎。

### Q7 死信队列你们怎么用？

> 消费组 `maxReconsumeTimes=5`，耗尽后 broker 转入 `%DLQ%<消费组>`。项目为两个业务 DLQ 各配了告警监听器（`DlqAlertListeners`）：ERROR 日志 + 完整消息体落痕，人工在 RocketMQ 控制台"重新发送"重放。已知局限：只告警不自动处理；`product-stock-change-topic` 无生产者故不配告警（TD-9）。

### Q8 为什么用 RocketMQ？以前用过别的吗？

> 项目历史上有过 Guava EventBus（进程内事件总线）和 RabbitMQ，Git 历史可查（EventBus 在"支付回掉"提交引入，后随 Redis 服务提交移除换 RabbitMQ；现行为 RocketMQ）。驱动力是可靠性需求升级：进程内事件重启即丢 → 需要 broker 持久化与重试。当前 pom 里还残留 guava 死依赖，是清理遗漏。

### 答辩 1 分钟陈述模板

> "支付回调我只在事务内做状态推进，事务提交后发一条 RocketMQ 消息用于异步微信通知；消费端用 Redis SETNX 做 24 小时幂等防重投。超时关单用 30 分钟延时消息，关单前必须向支付宝二次确认防止误关已支付订单，另有 40 分钟定时任务兜底防消息丢失。已知边界是'事务提交后发送'存在消息丢失窗口，因当前 MQ 只承担通知职责故损失可容忍，升级路径是本地消息表。"

---

## 9. 闭卷验收

> 全部不看文档完成。任一环节卡住超过 2 分钟，回到指定小节回读后重试。

**A. 画图（15 分钟）**
1. 默画 §1.4 Topic 矩阵（5 个 topic、生产者、消费组、maxReconsumeTimes）；
2. 默画 §3.1 支付成功消息的时序图，标注事务边界竖线与"事务外发送"。

通过标准：Topic 名、消费组名、重试次数与矩阵一致；时序图含全部 9 个步骤。

**B. 口述（15 分钟）**
3. 讲清 S1-S4 四场景，每场景说出"后果 + 兜底手段 + 源码位置"；
4. 讲清 W1/W2/W3 三个崩溃窗口分别落在时序图哪个位置；
5. 回答："为什么重复消息是 return 而不是抛异常？"（答不出回读 §4.3）

通过标准：不使用"恰好一次"这类不准确表述；能主动说出 S2 的前提 P1-P3。

**C. 源码定位（10 分钟，说出文件+方法+大致行号）**
6. 发送超时 3000ms 在哪里设置；7. delayLevel=9 在哪里、注释记录了什么修复；8. SETNX 原语在哪一行；9. 释放幂等键+重抛在哪两行；10. DLQ 告警的两个类名。

**D. 场景分析（20 分钟）**
11. "用户在延时消息到期前 1 秒完成支付，系统会发生什么？"——要求按 §3.2 时序推演到最终状态（订单 PAID、不会误关单、救援路径可能再发一次消息、SETNX 兜底）；
12. "消费侧 Redis 挂了，消息会怎样？"——tryAcquire 抛异常 → 走异常路径…？（注意：`tryAcquire` 在 try 块**外**，Redis 故障时异常向上抛 → broker 重试 → 5 次耗尽 → DLQ 告警。这是真实行为，想清楚再答）【代码推导】。

**E. 快问快答（3 分钟，脱口而出）**
发送成功=消费成功吗？/ at-least-once 下消费端必须做什么？/ maxReconsumeTimes 本项目几？/ 死信之后谁处理？/ 延时消息丢了谁兜底？/ 为什么关单前查支付宝？/ MQ 在你项目驱动履约吗？/ Guava 现在还在用吗？

**全部通过 = Day 5 完成。** 未通过项当天顺延，进入 Day 6（教材四：库存与订单一致性，关单三道防线的完整论证）前必须补齐。

---

*本教材所有【代码推导】结论以 2026-10-12 工作区源码为准；实验类结论复跑后请自行将标记升级为实测，并在 STUDY_GUIDE 附录 C 中销项。*
