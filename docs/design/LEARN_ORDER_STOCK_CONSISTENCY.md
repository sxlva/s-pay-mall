# LEARN_ORDER_STOCK_CONSISTENCY.md — 库存与订单一致性教材

> **课程定位**：STUDY_GUIDE Day 6 配套教材，建议约 2 小时，含动手实验。
> **学习前提**：完成教材三（RocketMQ）。本教材的"超时关单三道防线"是教材三 §3.3 预告内容的完整展开；关单消息的消费入口不再重复讲解。
> **证据标记**：【代码推导】= 仅阅读源码得出，未在当前工作区运行验证；【文档实测】= 2026-10-07 首次通过、**2026-10-19 当前工作区全量复跑通过（17/17，C-1 销项）**；【待验证】= 附编号。
> **一句话总纲**：本项目的库存一致性靠"**Redis 预扣防超卖 + MySQL 条件更新当最终裁判 + 事务外恢复库存**"三件套维持；订单状态机全部用"条件更新影响行数"做并发守卫。**不存在任何分布式事务**，所有一致性都是设计出来的最终一致。

---

## 1. 问题背景

### 1.1 库存问题为什么难

电商库存的难点不在"扣一个数字"，而在三个客观事实叠加：

1. **并发**：秒杀场景下同一时刻成千上万个请求抢同一件商品；
2. **多副本**：本项目库存同时存在于 Redis（`mall:product:stock:{id}`）和 MySQL（`product.stock` 字段），两份数据天然会不一致；
3. **支付不确定性**：用户下单后不一定会付款，扣了的库存要能退；更麻烦的是"关单的瞬间用户恰好付了钱"。

不使用任何防护的后果就是**超卖**：卖出比库存更多的商品。超卖在业务上是资损级的（要么采购救火、要么退款道歉），所以库存问题永远是面试和答辩的必考题。

### 1.2 本项目为什么不直接在 MySQL 扣库存

最简单的方案是下单时直接 `UPDATE product SET stock = stock - N WHERE ...`（条件更新防超卖，见 §2.2），为什么本项目要引入 Redis 预扣、支付成功时才落 MySQL？

**原理层面的回答**：下单是一个高频、轻量的操作，MySQL 行锁竞争会成为瓶颈；Redis 单线程原子操作扛并发能力强几个数量级。**用 Redis 扛"流量裁决"（谁能下单），用 MySQL 做"最终账本"（实际库存是多少）**。

**局限**：引入了两份数据的同步问题——这正是本教材 §4-§5 要逐场景讲清的代价。没有免费的方案，只有诚实的取舍。

### 1.3 本项目库存/状态的四条数据线在脑中的位置

学习本模块时，请始终想着这四条线，任何业务动作都是"四条线一起变化"：

| # | 数据 | 位置 | 角色 |
|---|------|------|------|
| ① | 可售库存 | Redis `mall:product:stock:{productId}` | 流量裁决（防超卖前线） |
| ② | 账面库存 | MySQL `product.stock` | 最终账本 |
| ③ | 订单状态 | MySQL `order_main.status`（INIT 落库为 `CREATED`） | 订单生命周期 |
| ④ | 支付单状态 | MySQL `pay_order.status`（WAIT_PAY/PAYING/PAID/CLOSED/…） | 支付生命周期（WAIT_PAY 行是补偿 Job 的扫描口径） |

§3 的"四线变化图"会把每个业务事件对四条线的动作全部列出，这是本教材最重要的复习图。

---

## 2. 前置知识（约 40 分钟）

### 2.1 Redis 原子操作与"先查后扣"的竞态

`RAtomicLong.addAndGet(-n)` 是原子操作——并发执行时 Redis 保证每个请求按顺序扣减，不会丢更新。但"先 `get()` 检查够不够、再扣"是**两个非原子的操作**，中间可以被其他请求插入。所以：

- 预检查（`hasEnoughStock`，`MallOrderServiceImpl.java` L211-215）只是**性能优化**（提前拒绝，省一次无效的负数补偿）；
- 真正的防超卖闸门是**扣完检查余数**：为负就加回去并拒绝（`StockGatewayImpl.java` L45-49）。

答题时不要把 `get >= quantity` 说成防超卖手段——它只是预检。

### 2.2 数据库条件更新（乐观锁扣减）

`s-pay-mall-infrastructure/src/main/java/cn/fcr/infrastructure/dao/mall/IProductDao.java` L31：

```sql
UPDATE product SET stock = stock - #{quantity}, update_time = NOW()
WHERE id = #{id} AND stock >= #{quantity}
```

`stock >= #{quantity}` 这个 WHERE 条件就是并发守卫：两个事务并发扣同一件商品，只有一个能拿到行锁并扣成功；即使都通过检查，第二个执行时 stock 已变小，条件不满足、影响行数为 0，扣减无声地失败。**影响行数即裁判**——这是本项目状态机和库存共用的核心模式。

### 2.3 事务与"不可回滚的外部副作用"

DB 事务能保证 MySQL 操作的原子性，但 **Redis 操作不参与 MySQL 事务**。如果在事务内先恢复 Redis 库存、后做 DB 状态流转，DB 回滚时 Redis 的恢复不会回滚——库存凭空多出来，超卖。本项目的纪律（P1-2）：

> **取消/关单时，DB 状态流转在事务内，Redis 库存恢复在事务提交后执行。**

源码证据：`OrderStateMachineServiceImpl.java` L179 注释 + `OrderApplicationService.java` L283-290（主动取消）、L483-486（超时关单）。

### 2.4 状态机与守卫方法

`OrderEntity.java` L53-56：

```java
public boolean canPay()      { return this.state == OrderState.INIT; }
public boolean canCancel()   { return this.state == OrderState.INIT; }
public boolean canDeliver()  { return this.state == OrderState.PAID; }
public boolean canComplete() { return this.state == OrderState.SHIPPED; }
```

合法变迁：`INIT → PAID → SHIPPED → DONE`，`INIT → CANCELED`。守卫方法是**内存实体的第一道检查**（快速失败），但单靠它不够——查出来的状态可能被并发事务抢先改变，所以每次写库都用 §2.2 的条件更新兜底（`OrderStateMachineServiceImpl` L51-56 等四处）。

**状态口径细节（面试加分项）**：`OrderState` 落库时 INIT 存 `CREATED`（`OrderState.java` L16、L73-75），读取方 `fromDbStatus` 还兼容历史 `COMPLETED`/`CANCELLED` 旧值（L88-94）。这是"存量数据口径固化"的典型处理——答题时能说出"为什么不用一个统一字符串"，说明你看过真实项目的演进痕迹。

---

## 3. 完整业务流程

### 3.1 四线变化图（全教材核心图，要求默画）

```
事件          ①Redis可售      ②MySQL账面      ③order_main    ④pay_order
─────────────────────────────────────────────────────────────────────────
下单成功       −q（预扣）       不变             CREATED(INIT)   WAIT_PAY
唤起支付宝     不变            不变             不变            内存变PAYING，
                                                （落库仍WAIT_PAY，注释见
                                                IOrderPaymentGateway L8-9）
支付成功回调    不变（已预扣）    −q（条件更新）    CREATED→PAID    WAIT_PAY→PAID
主动取消/超时   +q（事务外恢复）  不变             CREATED→CANCELED WAIT_PAY/PAYING→CLOSED
关单
晚到支付       不变            不变             已是CANCELED    已是CLOSED
（关单后回调    ── 记ERROR日志，人工退款或补履约，见 OrderApplicationService L370-377 ──
到达）
```

**读图要点**：

- MySQL 账面库存在"下单→支付成功"之间是**不扣**的：取消时只恢复 Redis（`restoreStockForCancel` 注释，`OrderStateMachineServiceImpl` L196-199 明确"此时尚未同步扣减 MySQL 库存，故无需恢复 MySQL"）。这就是 §1.2"Redis 裁决、MySQL 账本"的分工；
- 唤起支付宝只改**内存实体**为 PAYING，落库仍是 WAIT_PAY——这是刻意的（`OrderTransactionService.java` L71-72 注释）：超时关单与补单扫描都以 `pay_order` 的 WAIT_PAY 行为准，少一个状态就少一类漏扫场景；
- 晚到支付不是技术故障分支，是**业务死角分支**：钱已收、单已死，只能人工介入。系统设计的目标是把它压缩到极小概率（B2 修复：延时关单从 1 分钟放宽到 30 分钟，注释见 `OrderPaymentGatewayImpl` L38-40）。

### 3.2 订单创建链路（含幂等）

```
POST /order/create (userId, address, requestId)
  │
  ▼
OrderApplicationService.createOrder  L169
  │ ① 幂等守门：requestId 非空 → SETNX(24h)；为空 → 降级用户级短锁(10s)
  │    命中且已完成 → 返回首次订单；命中且 PROCESSING → 抛"请勿重复下单"
  ▼
OrderTransactionService.createOrderInTransaction  L62（@Transactional）
  │ ② listCart → checkAndDeductStock（逐项：预检→原子扣→失败回滚已扣部分）
  │ ③ buildAndSaveOrder（order_main CREATED + 订单项）
  │ ④ toPayOrder → generatePayUrl（内存变 PAYING，落库 WAIT_PAY）
  │ ⑤ clearCart
  │ 异常 → catch 内 restoreDeductedStock 补偿 Redis 预扣 → 重抛
  ▼
事务提交后（L200-205）：
  │ ⑥ sendDelayCloseMessage（30min 延时关单消息，失败仅告警）
  │ ⑦ markDone（幂等键写入 orderNo，供同 requestId 重发时返回）
```

**幂等语义细节**（L156-162 注释）：requestId 为空时降级为 `uid:{userId}` 10 秒锁——只防双击/并发重发，**不承诺跨请求幂等**。面试被问"前端没传 requestId 会怎样"，这是标准答案。

### 3.3 超时关单的三道防线

```
防线1（准点）  下单30min后：order-timeout-topic 延时消息到期
              → OrderTimeoutCloseRocketListener → handleTimeoutCloseOrder
防线2（兜底）  NoPayNotifyOrderJob 每30秒扫描：
              ├─ WAIT_PAY 超 5min → 主动查支付宝 → 已付则补 changeOrderPaySuccess（补单）
              └─ WAIT_PAY 超 40min → 走 handleTimeoutCloseOrder 同一路径（兜住防线1的漏网）
防线0（用户）  主动取消 cancelOrder → 同一状态机
```

`handleTimeoutCloseOrder`（`OrderApplicationService.java` L470-488）内部三步：

1. 订单不存在 → warn + false（不影响 MQ 重试语义，M2-1 分流注释）；
2. **向支付宝二次确认**（L478）：已支付 → 转 `changeOrderPaySuccess`（晚到支付救援），**不关单**；
3. 未支付 → `cancelOrderInTransaction`（状态机条件更新 INIT→CANCELED + 关闭 pay_order），提交后 `restoreStockAfterCancel` 恢复 Redis（L485）。

**为什么三道防线不会互相踩踏**：它们最终都汇聚到状态机的条件更新（`UPDATE ... WHERE status='CREATED'`），同一订单无论被触发几次，**恰好一次生效**——影响行数 0 的触发方安静退出。这是"用幂等设计代替分布式锁协调多触发源"的范例。

**Job 自身的并发安全**（TD-8）：`NoPayNotifyOrderJob` cron 每 30 秒，多实例部署时用 Redisson 分布式锁 `lock:job:no-pay-notify` 防重，`tryLock(0)` 不等待——拿不到锁的实例直接跳过本轮（L59-64），租约 60 秒兜底防宕机死锁（L42-43）。

**窗口数字的关系**（要能解释）：30（延时关单）< 40（Job 兜底扫描）——兜底必须比正常触发晚，否则两道防线并发执行会失去"兜底"语义；5 分钟主动查单则是因为支付宝通知理论上秒级到达，5 分钟没等到基本可判定"通知丢了"，越早补对用户越好。

---

## 4. 源码精读（按执行顺序）

### 4.1 创建订单事务：`OrderTransactionService.createOrderInTransaction`

`s-pay-mall-application/src/main/java/cn/fcr/application/OrderTransactionService.java` L61-90

- L66 `checkAndDeductStock`：**先扣 Redis 后落库**——顺序很重要，若先落库后扣库存，扣失败时订单就成了无主单；
- L82-89 异常补偿：catch 里先 `restoreDeductedStock`（逐项把已扣的加回来）再重抛；补偿本身失败只记 ERROR（L85-87），因为事务回滚会撤掉订单，此时 Redis 多扣的库存需要人工发现——这是已知的、注释里写明的小死角；
- **局限**：`checkAndDeductStock` 逐项扣、逐项回滚，购物车商品多时多次 Redis 往返（n 件商品 = n 次预检 + n 次扣减）。性能不是当前瓶颈，但要知道成本。

### 4.2 Redis 预扣：`StockGatewayImpl.deductStock` 与 `MallOrderServiceImpl.checkAndDeductStock`

- `MallOrderServiceImpl.java` L44-65：逐项循环——`hasEnoughStock` 预检（L49）→ `deductStock`（L55）→ 任一步失败 `restoreDeductedStock` 回滚已扣部分（L51/L59）。**注意 L39-40 的 DDD 注释**：预检查已上移到 Domain 层，Gateway 只剩原子操作和竞态补偿——职责分层的证据，面试可讲；
- `StockGatewayImpl.java` L35-53：核心只有两行——`addAndGet(-quantity)`（L42，原子扣减）与负数补偿（L45-49：为负则加回并抛 `STOCK_INSUFFICIENT`）。**这两行就是防超卖的全部机制**；
- L44-48 的"竞态补偿"注释解释了为什么"先查后扣"够用：并发下两个请求都扣到负数，各自加回各自的量，结果可能是两个都被拒绝（宁可少卖，不可超卖）。

### 4.3 支付成功的状态推进：`OrderStateMachineServiceImpl.paySuccess`

`s-pay-mall-domain/src/main/java/cn/fcr/domain/order/service/impl/OrderStateMachineServiceImpl.java` L38-66（教材三 §4.4 已给出完整链路，此处聚焦库存）：

```
L45  canPay() 守卫（内存，INIT 才放行）
L51  UPDATE order_main SET status='PAID' WHERE order_no=? AND status='CREATED'  ← 并发裁判
L59  pay_order → PAID
L62  syncDBStockForPaySuccess(order)：逐项 syncDBStockDeduct
L75-97  遍历订单项 → stockGateway.syncDBStockDeduct(productId, quantity)
```

`syncDBStockDeduct`（`StockGatewayImpl.java` L127-146）→ `IProductDao.decreaseStock`（L31 条件更新 SQL）。**L91-95 是一个必须诚实掌握的边界**：DB 扣减失败（影响行数 0）**不抛异常、不回滚事务**——订单照样 PAID，注释解释"Redis 已预扣成功，DB 扣减失败可能是并发场景下的正常情况"，并留了"可以考虑加入补偿机制或告警通知"的尾巴。后果：此时 Redis 可售库存已扣、MySQL 账面未扣，两份数据出现一段漂移窗口【代码推导】。当前缓解手段是 Redis 才是销售闸门（账面漂移不造成超卖），升级点见 §7。

### 4.4 取消/关单：`cancel` + 事务外恢复

- 状态机 `cancel`（`OrderStateMachineServiceImpl.java` L150-183）：`canCancel()`（INIT）→ 条件更新 `INIT→CANCELED`（L166-171，影响行数 0 = 被并发流转，拒绝并 return false）→ pay_order 为 WAIT_PAY/PAYING 时关闭（L174-177）→ **L179 注释明确库存恢复不在事务内**；
- 恢复（L186-220）：只回 Redis（`stockGateway.restoreStock`），不碰 MySQL——因为走到取消时 MySQL 从未扣过（四线变化图 §3.1）；
- 调用方（`OrderApplicationService`）：主动取消 L283-290、超时关单 L483-486，都在事务提交后调 `restoreStockAfterCancel`（L497-503），其 catch-all 注释（L493）是标准答案："订单已取消为终态，库存未恢复只损失可售库存，不构成超卖"。

### 4.5 补偿 Job：`NoPayNotifyOrderJob`

`s-pay-mall-trigger/src/main/java/cn/fcr/trigger/job/NoPayNotifyOrderJob.java`

- L57 cron `0/30 * * * * ?`（每 30 秒）；
- L59-64 Redisson 锁防多实例重入；L62 `tryLock(0, 60s)` 语义要能说清：等待 0 秒、租约 60 秒；
- L85-96 补单：5 分钟窗口 SQL（`IOrderDao.java` L63-64）→ `queryTradeSuccess` 二次确认 → `changeOrderPaySuccess(orderId, null)`（tradeNo 传 null，L92 注释：补单路径不取回支付宝交易号）；
- L103-115 兜底关单：40 分钟 SQL（L73-74）→ `handleTimeoutCloseOrder`；单条失败 catch 后继续（L112-114），保证一个坏订单不拖垮整轮。

### 4.6 状态口径与历史兼容

`OrderState.java` L6-9 的类注释值得整段读：领域层用 `code`，DB 存混合口径（INIT→CREATED），读取双向兼容（含 `COMPLETED`/`CANCELLED` 旧值）。**这是"数据口径一旦落库就不能单方面改"的真实案例**——E2E 验收固化了 CREATED，单方面改会打挂存量数据。答辩遇到"为什么这么麻烦"时使用这个答案。

---

## 5. 异常与边界

> 每个场景按"现象 → 为什么安全/为什么不安全 → 源码位置"组织。

### 场景 1：100 个并发请求抢 1 件商品

Redis `addAndGet` 原子扣减，99 个请求把库存扣到负数 → 各自补偿加回并抛 STOCK_INSUFFICIENT（`StockGatewayImpl.java` L45-49）。**结果：不超卖，可能误拒**（两个同时看到 −1 都加回，实际还有货但都被拒）。误拒只损失转化率，可接受。【代码推导】

### 场景 2：下单后、支付前，用户重复点击下单按钮

- 有 requestId：SETNX 幂等键 24h，第二次直接返回首次订单（`OrderApplicationService.java` L176-191）；
- 无 requestId：`uid:{userId}` 10 秒锁，第二次抛"订单正在处理中"（L172-173、L193）。

### 场景 3：取消订单的同一毫秒，支付回调到达

两个事务并发：一个执行 `UPDATE ... SET CANCELED WHERE status='CREATED'`，另一个执行 `UPDATE ... SET PAID WHERE status='CREATED'`。MySQL 行锁串行化后，**必有一个影响行数为 0**，其业务流程整体退出（cancel 返回 false 不恢复库存 / paySuccess 返回 false 不推进）。**不会出现"订单 PAID 且库存被恢复"的组合**。这就是"条件更新当最终裁判"的含义——不需要分布式锁，不需要分布式事务。【代码推导】

### 场景 4：关单消息重复投递（延时消息重试/Job 与延时消息并发）

`OrderTimeoutCloseRocketListener` **没有消费幂等键**，重投完全依赖状态机：已 CANCELED 的订单再次走到 `cancel`，条件更新影响行数 0 → return false → 记"未执行"（`OrderApplicationService.java` L42-44）。**为什么这里可以没有 SETNX**：关单是幂等操作（关一次和关十次结果一样），且条件更新本身就是原子裁判。对比 `order_paid` 消费者需要 SETNX 的原因：那边连着"非幂等的副作用"（发微信通知），见教材三 §4.3。【代码推导】

### 场景 5：DB 库存扣减失败（syncDBStockDeduct 返回 false）

如 §4.3 所述：订单 PAID、Redis 已扣、MySQL 未扣。账面漂移方向是"账面多于实际可售"，且 Redis 闸门保证不超卖，所以不资损；但 drift 会累积（每次 DB 扣减失败都叠加）。当前只有 WARN 日志（`OrderStateMachineServiceImpl.java` L92），无自动补偿——登记为已知边界，补偿方案见 §7 升级点。【代码推导】

### 场景 6：事务提交成功，但恢复 Redis 库存时 Redis 挂了

`restoreStockAfterCancel` catch-all（`OrderApplicationService.java` L500-502）：记 ERROR 后结束。订单已是 CANCELED 终态，MySQL 无错；损失是该订单预扣的库存暂时回不来——**可售库存变少，方向安全（少卖不超卖）**，事后人工补 `INCR` 即可（或重启流程自然恢复）。注释 L493 把方向讲得很清楚。【代码推导】

### 场景 7：延时消息丢失（发送失败/消费 5 次耗尽）

防线 1 失效，防线 2（40 分钟 Job 扫描 WAIT_PAY 超 40 分钟）捞回，最多晚 10 分钟关单 + 一轮 30 秒扫描间隔。若 Job 也长期失败（如 DB 挂了），订单保持 CREATED/WAIT_PAY 不关闭——**只损失可售库存占用（用户占用着库存不付钱），不超卖**，DB 恢复后 Job 自愈。三道防线没有覆盖"Job 与延时消息同时失效超过 40 分钟"之外的极端窗口？其实覆盖了：任何时刻只要有一个恢复，就会扫描全量漏网订单。【代码推导】

### 场景 8：晚到支付（关单后支付宝回调到达）

订单已 CANCELED → `changeOrderPaySuccess` 的晚到支付分支（L370-377）：记 ERROR 日志"需人工核实退款或补履约"，**不发事件、不推进状态**，Controller 仍回支付宝 success 终止其重试。这是设计取舍：自动退款风险大（金额校验、幂等、对账），交给人工是中小项目的合理选择。【代码推导】

### 待验证事项

| 编号 | 事项 | 验证方法 |
|------|------|---------|
| C-1 | ~~10-07 全量 E2E 结论在当前工作区复跑（含 `TimeoutCloseOrderE2EIT` 的关单/二次关单/救援三场景）~~ **已于 2026-10-19 复跑通过（17/17）** | 跑通测试套件 |
| C-8 | ~~场景 1 的并发防超卖实测（并发下单后 Redis 库存不为负、MySQL 无超卖订单）~~ **已于 2026-10-19 实测通过（`docs/dev-ops/stock-pressure-test.sh`，20 并发抢 5 件：成功 5、拒绝 15，Redis 最终 0 无负值，DB 订单数与成功数一致）** | 实验 E2 |

---

## 6. 动手实验

### E1 负数补偿观察（15 分钟）

1. 挑一个商品：`redis-cli -p 26379 SET mall:product:stock:{productId} 1`；
2. 用两个终端几乎同时对该商品下单（或用 `curl` 并发两发）；
3. 观察日志：一个成功，一个出现 `STOCK_INSUFFICIENT`；`GET mall:product:stock:{productId}` 应回到 1（被补偿）。

完成标准：能指出补偿发生在哪一行（`StockGatewayImpl.java` L47），并解释"为什么预检都通过还会扣成负数"。

### E2 并发防超卖压测（30 分钟）【2026-10-19 已实测通过，C-8 销项】

1. 库存设为 10；
2. 用并发工具（jmeter/并发 curl 脚本）发 50 个下单请求；
3. 验证：成功订单数 ≤ 10；Redis 最终值 ≥ 0 且 = 10 − 成功订单数；无负数残留。

完成标准：向面试官复述"预检 + 原子扣减 + 负数补偿"三层各防什么。

> **实测记录（2026-10-19，脚本 `docs/dev-ops/stock-pressure-test.sh`，RUN_ID 隔离口径）**：20 并发用户抢 5 件库存（商品 1）→ 成功 5、拒绝 15（13 个 0001 预检拒绝 = `MallOrderServiceImpl` L49-52 `hasEnoughStock` 路径；2 个 STOCK_INSUFFICIENT = `StockGatewayImpl` L45-49 负数补偿路径）→ Redis 最终库存 0、无持久负值 → DB 订单件数 5 == 成功数，断言输出 `✅ 防超卖验证通过`。注意：幂等键 requestId 每次请求必须唯一，否则 SETNX 会拦截重复 requestId；Redis 幂等/库存键在 1 号库（`application-dev.yml` L63 `database: 1`）。

### E3 取消恢复链路（20 分钟）

1. 下一单，记录 `mall:product:stock:{productId}`；
2. 调取消接口，观察日志顺序：状态机取消（事务内）→ "取消订单状态更新完成" → "Redis库存已恢复"；
3. 验证 Redis 值回增、order_main=CANCELED、pay_order=CLOSED。

完成标准：能解释为什么恢复日志一定出现在事务提交之后，以及 Redis 挂了会怎样（场景 6）。

### E4 超时关单三场景走读（30 分钟）【文档实测，2026-10-19 已复跑通过】

对照 `s-pay-mall-start/src/test/java/cn/fcr/test/TimeoutCloseOrderE2EIT.java` 逐场景读源码（不跑也可）：

- 场景一（L129-154）：INIT 订单 → 模拟支付宝"未支付" → `handleTimeoutCloseOrder` → 关单 + 库存恢复；
- 场景二（L176）：对**已关闭**订单再次调用 → 返回 false，库存**不二次恢复**（条件更新守卫）；
- 场景三（L224-246）：延时消息到期瞬间付款 → 支付宝二次确认"已支付" → 转 `changeOrderPaySuccess` 不关闭。

完成标准：不看测试代码，能口述三场景的断言各验证了哪条防线。

### E5 补偿 Job 观察（15 分钟）

1. 手工造一条 WAIT_PAY 且 `create_time` 超过 40 分钟的 pay_order（直接 SQL 改 create_time）；
2. 观察 30 秒内 `NoPayNotifyOrderJob` 日志："兜底关单补偿，发现 1 个超时未关单订单"；
3. 验证订单被关闭、库存恢复。

完成标准：能说出 Job 的 SQL 口径（WAIT_PAY + create_time 窗口）和分布式锁的作用。

---

## 7. 设计取舍

### 7.1 Redis 预扣 + 支付时持久化 vs 下单直接扣 MySQL

| 维度 | 当前（Redis 预扣→支付时落库） | 备选（下单直接条件更新 MySQL） |
|------|------------------------------|------------------------------|
| 下单吞吐 | Redis 原子操作，高 | 行锁竞争，热点商品受限 |
| 数据一致性 | 两副本，需对账思路 | 单副本天然一致 |
| 取消成本 | 恢复 Redis（快） | 无需恢复（未扣账面） |
| 适用条件 | 并发有压力、允许最终一致 | 并发低、强一致优先 |

收益/成本：吞吐换一致性复杂度。本项目是教学+毕设场景，真实并发不高，选 Redis 预扣的**收益主要是抗峰能力（简历话术：防超卖设计）**，成本就是 §5 那些边界场景——能讲清边界，这个取舍就立得住。

### 7.2 DB 扣减失败不抛异常（场景 5）的取舍

备选：扣减失败则整个支付回调回滚 → 问题：支付宝侧钱已收，回滚后订单还是 CREATED，靠重试/补单再来一次，链路更复杂且期间用户看到"未支付已扣款"。当前选择：宁可账面短暂漂移，不让用户已付的钱在系统里"消失"。**方向安全（不超卖）+ WARN 留痕 + 人工可对账**。升级方向：`decreaseStock` 失败写库存对账表/发告警，定时任务对账补齐（收益：可观测性；成本：一张表 + 一个 Job）。

### 7.3 多触发源无锁化（三道防线）

三道触发源（延时消息、40 分钟 Job、用户主动取消）若用分布式锁互斥，复杂度和死锁风险都上升。当前选择：**全部汇聚到状态机条件更新**，用"影响行数裁判"让后到者自然失效。这是本项目一以贯之的模式（状态机四处条件更新 + 库存条件更新同构），是答辩中最能体现"并发设计功力"的点。

### 7.4 PAYING 不落库

唤起支付宝时内存实体置 PAYING，落库保持 WAIT_PAY（`OrderTransactionService.java` L71-72 注释）。取舍：少一个持久化状态就少一类漏扫（超时关单/补单都以 WAIT_PAY 为准）；代价是"用户打开了收银台"这个信息无法从 DB 查询——当前没有业务需要它。如果未来要做"唤起的支付链接有效期管理"，才需要落库。

---

## 8. 面试与答辩

### Q1 你们怎么防超卖？

> 三层：①下单时 Redis 原子扣减 `addAndGet(-n)`，扣成负数就补偿回滚并拒绝——这是真正闸门；②预检 `get >= n` 只是提前拒绝的优化，不是防线；③支付成功时 MySQL 条件更新 `UPDATE ... WHERE stock >= n` 落账面，影响行数为 0 说明并发下已被扣。宁可误拒不可超卖。（源码：`StockGatewayImpl.java` L42-49、`IProductDao.java` L31）

### Q2 Redis 和 MySQL 库存不一致怎么办？

> 先说不一致的两个来源：取消时只回 Redis（此时账面未扣，刻意设计，注释见 OrderStateMachineServiceImpl L196-199）；DB 扣减失败只记 WARN（L91-95），会累积漂移。再说安全性：所有销售走 Redis 闸门，漂移方向是"账面 ≥ 实际可售"，不会超卖；最后是治理：WARN 留痕可人工对账，升级方向是库存对账表 + 补偿 Job。

### Q3 支付回调和关单同时到达怎么办？

> 不需要加锁。两条链路最终都执行 `UPDATE order_main SET ... WHERE status='CREATED'`，行锁串行化后必有一个影响行数为 0，整体退出。MySQL 条件更新就是最终裁判——这是无分布式事务方案的关键设计。（OrderStateMachineServiceImpl L51-56、L166-171）

### Q4 超时关单怎么设计的？消息丢了怎么办？

> 三道防线：30 分钟延时消息准点触发；Job 每 30 秒扫 WAIT_PAY 超 5 分钟的主动查单补履约、超 40 分钟的兜底关单；用户可主动取消。关单前必须向支付宝二次确认，防止"到期瞬间用户已付款"被误关。所有触发源汇聚到状态机条件更新，天然幂等。延时消息丢失最多晚 40 分钟由 Job 捞回。（源码：OrderApplicationService L470-488、NoPayNotifyOrderJob L57-115）

### Q5 为什么恢复库存放在事务提交后？

> 因为 Redis 不参与 MySQL 事务。若事务内恢复 Redis、随后 DB 回滚，Redis 的回增不会回滚，库存凭空变多导致超卖。P1-2 纪律：事务内只做 DB 状态流转，提交后才恢复 Redis；恢复失败也只损失可售库存（少卖不超卖），记 ERROR 人工补。（OrderApplicationService L497-503 注释原文可背）

### Q6 订单状态机怎么实现的？

> 守卫方法（canPay/canCancel/...）+ 条件更新双保险：实体守卫快速失败，写库用 `UPDATE ... WHERE status=期望值` 把并发裁决交给数据库影响行数。INIT 落库为 CREATED 是历史口径，读取方兼容旧值 COMPLETED/CANCELLED——数据口径落库后不能单方面改。（OrderEntity L53-56、OrderState L73-101）

### Q7（陷阱）下单时库存充足，支付时库存不足会怎样？

> 本设计不会发生：下单时已预扣 Redis，30 分钟内库存为下单者保留，支付阶段只做"Redis 预扣 → MySQL 账面"的持久化，不再做充足性判断。如果问的是 DB 持久化失败（条件更新影响行数 0），见 Q2 的漂移治理。

### Q8 补偿 Job 为什么需要分布式锁？

> cron 每 30 秒一轮，多实例部署时同一订单可能被两个实例同时补偿。Redisson `tryLock(0, 60s)`：不等待（拿不到锁直接跳过本轮，补偿下轮再做）、租约 60 秒防宕机死锁。即使锁失效，状态机条件更新仍是最后防线——锁是性能优化，不是正确性依赖。（NoPayNotifyOrderJob L59-64）

### 答辩 1 分钟陈述模板

> "库存采用 Redis 预扣 + MySQL 最终落账的两层结构：下单在 Redis 原子扣减，扣成负数自动补偿拒绝，保证不超卖；支付成功回调在事务内把订单推到 PAID、并用条件更新把预扣落到 MySQL 账面。取消和超时关单在事务提交后才恢复 Redis 库存，避免 DB 回滚时 Redis 无法回滚。超时关单有延时消息、定时任务、用户取消三个触发源，全部汇聚到状态机的条件更新做并发裁判，同一订单恰好一次生效。已知边界是 DB 扣减失败只告警不补偿、存在账面短暂漂移，方向安全，升级方向是库存对账表。"

---

## 9. 闭卷验收

> 不看文档完成。卡住超 2 分钟回读指定小节。

**A. 画图（20 分钟）**
1. 默画 §3.1 四线变化图（6 个事件 × 4 条线全部填对）；
2. 默画 §3.3 三道防线图，标注窗口数字 5/30/40 及其关系。

通过标准：能解释每个"不变"格子为什么不变（如唤起支付宝时落库不变——PAYING 是内存态）。

**B. 口述（15 分钟）**
3. 场景 3（取消与回调同毫秒）完整推演，说出裁判发生在哪条 SQL；
4. Q1 防超卖三层（脱稿）。

**C. 源码定位（10 分钟）**
5. 负数补偿在哪一行；6. 库存条件更新 SQL 在哪一行；7. "库存恢复不在事务内"的注释在哪一行；8. 40 分钟兜底 SQL 在哪一行；9. Job 分布式锁 key 与租约；10. 晚到支付分支在哪几行。

**D. 场景分析（20 分钟）**
11. "DB 扣减失败导致账面漂移，第二天运营发现账面比实物多 3 件"——复盘可能的成因链（哪几个场景会触发）并给出治理建议；
12. "延时消息 broker 挂了 2 小时"——推演哪些订单受影响、各自归宿。

**E. 快问快答（3 分钟）**
预检能不能防超卖？/ 防超卖的真正闸门在哪行？/ 取消时为什么不动 MySQL 账面？/ 恢复库存为什么在事务外？/ 三道防线的窗口数字？/ 多触发源靠什么不互踩？/ PAYING 为什么不落库？/ 晚到支付谁处理？

**全部通过 = Day 6 完成。** 未通过项顺延。进入 Day 7（教材五：升级路线与答辩）前，请确保你能把教材三、四的"已知边界与升级点"串成一条线——教材五将把它们升级为完整的面试级论证。

---

*本教材所有【代码推导】结论以 2026-10-12 工作区源码为准；实验类结论复跑后请自行升级标记并在 STUDY_GUIDE 附录 C 销项。*
