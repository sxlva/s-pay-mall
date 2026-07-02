# 架构重构清单

> **最后更新**: 2026-06-21
> **分支**: `refactor/application-layer-order-old-domain`
> **合并自**: DDD-AUDIT-REPORT.md + ARCHITECTURE_DEBT.md

本文档是 s-pay-mall DDD 架构重构的唯一追踪清单。已完成的记录保留上下文供回溯；未完成的列出修复路径和依赖关系，供后续有时间时继续。

---

## 一、已完成项（保留上下文）

### ✅ 阶段1-3：基础设施层模块化

| 任务 | 说明 |
|------|------|
| config/ 按领域拆分 | `auth/` / `mall/` / `order/` / `shared/` |
| dao/ 按领域拆分 | `auth/` / `mall/` / `order/` |
| Notice.java 死代码删除 | 移除未使用的通知类 |
| StockGatewayImpl 核实 | 原子操作 + 竞态补偿，无需迁移 |
| RocketMqOrderEventPublisher 核实 | 事件发布基础设施，位置正确 |

### ✅ P0-7：NoPayNotifyOrderJob 解耦 Alipay SDK

**问题**：Job 直接依赖 Alipay SDK 进行 `"10000".equals(code)` 状态判断。
**修复**：封装至 `IAlipayQueryGateway` → `AlipayQueryGatewayImpl`。
**文件**：`domain/order/gateway/IAlipayQueryGateway.java`、`infrastructure/order/gateway/AlipayQueryGatewayImpl.java`

### ✅ P0-8：库存预检查下沉 Domain 层

**问题**：`StockGatewayImpl.deductStock()` 里做了库存充足性判断（Infrastructure 层包含业务逻辑）。
**修复**：预检查上移至 `MallOrderServiceImpl.checkAndDeductStock()`，Gateway 仅保留原子 `decr` + 竞态补偿。
**文件**：`domain/mall/service/impl/MallOrderServiceImpl.java`、`infrastructure/mall/gateway/StockGatewayImpl.java`

### ✅ 7-1a：Mall Domain Application 层重建

**问题**：`MallOrderController` 直接注入 `IMallCartService` / `IMallOrderService`。
**修复**：在 `trigger/application/` 下新建 `OrderApplicationService`，包装购物车 + 订单的 8 个 Mall Domain 方法。
**文件**：`trigger/application/OrderApplicationService.java`、`trigger/http/mall/MallOrderController.java`

### ✅ 7-1b：旧 Order Domain 迁移

**问题**：`AliPayController` 等 5 个 Trigger 层文件直接注入 `IOrderService` / `PayOrderService` / `IMallOrderService`。
**修复**：`OrderApplicationService` 新增 8 个旧 Order Domain 包装方法 + `getOrderByNo`，5 个文件全部改为注入 `OrderApplicationService`。`createPayOrder` 签名根据调用方实际数据类型定为 `(String userId, String productId)`。
**文件**：`AliPayController.java`、`AliPayReturnController.java`、`NoPayNotifyOrderJob.java`、`TimeoutCloseOrderJob.java`、`OrderTimeoutCloseRocketListener.java`

### ✅ 7-1c：管理后台链路收口

**问题**：`MallAdminController` 和 `AdminApiController` 仍直接注入 `IMallOrderService`。
**修复**：注入改为 `OrderApplicationService`，新增 `deleteOrder()` 方法统一事务边界。工具方法 `toBigDecimal()` / `toMap()` 抽取到 `BaseController`。

### ✅ 7-1d：createOrder / changeOrderPaySuccess 事务与 MQ 发送解耦

**问题**：
- `createOrder()` 中 `sendDelayCloseMessage()` 在 `@Transactional` 内部，MQ 发送失败会导致事务回滚
- `changeOrderPaySuccess()` 中事件发布在基础设施层 (`OrderRepository`)，应上移至 Application 层编排

**修复**：
- 新建 `OrderTransactionService`（package-private，仅 `OrderApplicationService` 内部调用），`createOrderInTransaction()` 和 `changeOrderPaySuccessInTransaction()` 标注 `@Transactional`
- `OrderApplicationService.createOrder()` / `changeOrderPaySuccess()` 移除 `@Transactional`，委托 `OrderTransactionService` 完成事务后执行 MQ/事件发布
- MQ 发送/事件发布失败用 try-catch 保护，仅 warn 日志，不阻断主流程
- 库存回滚 catch 块加 try-catch 保护，避免回滚异常覆盖原始异常

**文件**：`trigger/application/OrderTransactionService.java`、`trigger/application/OrderApplicationService.java`

### ✅ 事件发布职责剥离

**问题**：`OrderRepository.changeOrderPaySuccess()` 在基础设施层直接调用 `orderEventPublisher.publishPaySuccess()`。
**修复**：从 `OrderRepository` 移除 `IOrderEventPublisher` 依赖，在 `OrderApplicationService.changeOrderPaySuccess()` 中编排 DB 更新 + 事件发布。
**文件**：`infrastructure/order/repository/OrderRepository.java`

### ✅ Bug Fix：库存扣减泄漏 (MallOrderServiceImpl.checkAndDeductStock)

**问题**：原实现两轮遍历（预检全部 → 逐项扣减），若第二轮中途失败，已扣库存无法回滚——因为调用方 `deductedItems` 变量在 try 块内赋值，异常时保持空列表，`restoreDeductedStock` 收到空列表。
**修复**：合并为单轮——逐项检查后立即扣减并记录到 `deductedItems`，任何一项失败时方法内部主动调用 `restoreDeductedStock(已扣列表)` 回滚后抛出异常，不依赖外部 catch 块。
**文件**：`domain/mall/service/impl/MallOrderServiceImpl.java:38-61`

### ✅ Bug Fix：JWT Token 日志泄露 (LoginController)

**问题**：`LoginController.checkLogin()` 将 `openidToken`（JWT 认证令牌）以 INFO 级别写入日志。
**修复**：日志行移除 `openidToken` 参数，仅保留 `ticket` 输出。Token 仍正常返回前端。
**文件**：`trigger/http/LoginController.java:68`

### ✅ Bug Fix：category_id 字段映射修复

**问题**：`IProductRepository` 中 categoryId 字段映射错误。
**文件**：`domain/mall/adapter/repository/IProductRepository.java`

---

## 二、未完成项（供后续重构参考）

### 🔴 P0-2：Application 层模块归属问题

**问题**：`OrderApplicationService`（19 个方法）位于 `s-pay-mall-trigger` 模块的 `cn.fcr.trigger.application` 包下，与 Controller/Listener/Job 共享 Maven 模块。理想状态下 Application 层应独立于 Trigger。

**当前状态**：功能可用，仅架构不干净。`@Transactional` 和业务编排正确。

**曾尝试的迁移及失败原因**：
- 曾迁至 `s-pay-mall-app` 模块 → `app → trigger → app` 循环依赖
- fat JAR 打包后丢失 trigger 层代码

**修复路径**：
1. 新建独立模块 `s-pay-mall-application`，仅包含 Application Service 类
2. `s-pay-mall-trigger/pom.xml` → 添加依赖 `s-pay-mall-application`
3. `s-pay-mall-app/pom.xml` → 添加依赖 `s-pay-mall-application`
4. 迁移 `OrderApplicationService` + `OrderTransactionService` 至新模块，包名改为 `cn.fcr.application`
5. 更新所有 import 引用
6. `mvn clean package` + fat JAR 内容检查

**受影响文件**：`MallOrderController`、`MallAdminController`、`AdminApiController`、`OrderPaidRocketListener`、`AliPayController`、`AliPayReturnController`、`NoPayNotifyOrderJob`、`TimeoutCloseOrderJob`、`OrderTimeoutCloseRocketListener`

**注意**：完成后可顺手解决 P0-3（见下方）。

---

### 🔴 P0-3：WeixinLoginGatewayImpl @Transactional（基础设施层违规）

**问题**：`infrastructure/auth/gateway/WeixinLoginGatewayImpl.java:69` 的 `createWechatUserAndBind()` 标注了 `@Transactional`。基础设施层不应控制事务边界。

**当前状态**：代码功能正确（insert + updateById 在同一事务中），但违反分层规范。

**修复路径**：
1. 创建 `AuthApplicationService`（可参照 `OrderApplicationService` 模式，放在 `trigger/application/` 下，等 P0-2 完成后再迁入独立模块）
2. 移除 `WeixinLoginGatewayImpl` 上的 `@Transactional`
3. 在 `AuthApplicationService` 中编排事务
4. 将用户创建的命名逻辑（`"temp_" + UUID` → `"wx_user_" + id`）抽离到 Domain 层

**注意**：不严格依赖 P0-2——可以照搬 `OrderApplicationService` 放在 trigger 模块。但文档建议等独立模块完成后一并迁移。

---

### 🔴 P0-4：幂等性设计缺失

**问题**：所有状态变更 API 入口均无幂等保护。虽然基础设施层已实现 `IdempotentGatewayImpl`（Redisson `trySet`），但仅被 `DeductHandler` 内部使用，未暴露给外部 API。

| 检查项 | 状态 |
|-------|------|
| `OrderCreateRequestDTO` | 缺 `requestId` |
| `CreatePayRequestDTO` | 缺 `requestId` |
| `CartAddRequestDTO` | 缺 `requestId` |
| `OrderPaidRocketListener` | 未进行消费幂等检查 |
| `OrderTimeoutCloseRocketListener` | 未进行幂等检查 |

**当前缓解措施**：
- `OrderPaidRocketListener` 的 `paySuccess()` 通过状态机 `order.canPay()` 守卫防止重复支付——重复投递不会重复扣款，只是空查询一次
- `OrderTimeoutCloseRocketListener` 通过 `order.canCancel()` 守卫防止重复关单
- 但两者仍会重复执行数据库查询，浪费资源

**修复路径**：
1. 在所有状态变更 DTO 中添加 `@NotNull private String requestId`
2. Application 层实现"事务边界外执行锁操作"模式（REVIEW.md §5.1）
3. MQ Listener 在 `onMessage()` 开头调用 `idempotentGateway.tryAcquire()`
4. **需前端配合**改造请求参数——这是暂缓的主要原因

---

### 🔴 P0-5：Domain 层跨领域反向依赖 (order → mall)

**问题**：`domain/order/service/PayOrderService.java` import 了 `cn.fcr.domain.mall.gateway.IPayGateway`。`order` 领域不应依赖 `mall` 领域。

**修复路径**（三选一）：
- **方案A**：将 `IPayGateway` 提升到 `domain/shared/gateway/` 共享内核
- **方案B**：通过领域事件解耦（order 发事件 → mall 订阅处理支付）
- **方案C**：将 `PayOrderService` 整体迁移至 `domain/mall/`——如果它其实不属于 order 领域

**风险**：如果选方案C，需确认旧 Order Domain 的其他组件是否依赖 `PayOrderService`。

---

### 🟡 P1-2：Controller 大面积缺少 @Valid 参数校验

**问题**：11 个 Controller 中仅 `MallAdminController.saveProduct()` 和 `AdminApiController.saveProduct()` 使用了 `@Valid`。

| Controller | @Valid | 缺失的校验 |
|-----------|--------|-----------|
| AliPayController | ❌ | `CreatePayRequestDTO` 字段 |
| WeixinPortalController | ❌ | 微信回调参数 |
| LoginController | ❌ | `ticket` 必填 |
| ProfileController | ❌ | 用户信息参数 |
| MallProductController | ❌ | 商品查询参数 |
| MallOrderController | ❌ | 订单创建/查询参数 |
| MallAuthController | ❌ | 认证参数 |
| AliPayReturnController | ❌ | 回调参数 |

**修复路径**：逐个 Controller 方法加 `@Valid` + DTO 字段加 `@NotNull`/`@NotBlank` 等。

---

### 🟡 P1-3：Controller 中包含业务路由逻辑

| 文件 | 问题代码 |
|------|---------|
| `MallAuthController.java:48-59` | `if (request.getOpenId() != null)` — 根据 openId 决定注册策略 |
| `MallAuthController.java:128-138` | `"BINDING_PENDING".equals(status)` — 绑定状态判断 |
| `AliPayController.java:83-86` | `"TRADE_SUCCESS".equals(...)` — 支付状态判断 |

**修复路径**：业务判断下沉到 Domain Service，Controller 只做参数校验和 DTO 转换。

---

### 🟡 P1-5 (剩余)：Redis + DB 跨资源事务一致性

**已完成**：`createOrder` 和 `changeOrderPaySuccess` 的 MQ 发送已解耦到事务外。

**未完成**：`cancelOrder()`（`OrderApplicationService.java:129`）仍在 `@Transactional` 内包含 `stockGateway.restoreStock()`。DB 事务回滚时 Redis 库存恢复无法回滚，导致 DB 和 Redis 库存不一致。

**修复路径**：参照 `createOrder` 模式——将库存恢复移到事务外，或用 Saga/最终一致性方案。

---

### 🟡 P1-6：OrderPaidRocketListener 包含业务编排逻辑

**问题**：`OrderPaidRocketListener.sendPaymentNotification()`（L59-85）包含完整的支付通知业务流程（查询订单 → 查微信绑定 → 发模板消息），应迁移到 Application Service。

**修复路径**：将 `sendPaymentNotification()` 提取为 `OrderApplicationService` 或 `NotificationApplicationService` 的方法。

---

### 🟡 P1-7：MQ 消息发送缺少超时参数

| 文件 | 问题 |
|------|------|
| `RocketMqOrderEventPublisher.java:27` | `convertAndSend` 无超时 |
| `OrderEventGatewayImpl.java:41` | `syncSend` 无超时 |

**修复路径**：添加 3000ms 超时参数。

---

### 🟡 P1-8：Domain 层 POM 包含非必要技术依赖

| 依赖 | 风险 |
|------|------|
| `spring-context` | 误用 `@Service`/`@Component` 风险 |
| `spring-tx` | 误用 `@Transactional` 风险 |
| `alipay-sdk-java` | 应仅在 Infrastructure 层 |
| `jjwt` / `java-jwt` | 应仅在 Infrastructure 层 |
| `fastjson` | 应仅在 Infrastructure 层 |

**修复路径**：逐个确认是否被 Domain 层代码实际引用——若未引用则从 POM 移除；若引用则重构使用标准 Java API。

---

### 🔵 P2-1：createPayOrder 缺少 @Transactional

**问题**：`OrderApplicationService.createPayOrder()` 创建支付订单无事务保护。若 `orderService.createOrder()` 涉及多步 DB 操作，中途失败会导致部分数据残留。

**修复路径**：方法上加 `@Transactional(rollbackFor = Exception.class)`，或将逻辑委托给 `OrderTransactionService`。

---

### 🔵 缺死信队列配置

**问题**：3 个 RocketMQ Listener 均未配置 DLQ。消息重试耗尽后直接丢弃，无告警。

**修复路径**：为 `order_paid`、`order-timeout-topic`、库存变更 topic 配置死信队列。

---

### 🔵 WeixinGatewayImpl 外部调用缺超时配置

**问题**：`Retrofit2Config.java` 未显式配置 OkHttpClient 超时参数。

**修复路径**：设置 `connectTimeout=5s`、`readTimeout=10s`。

---

## 三、暂缓/有意跳过项（附原因）

| 项目 | 跳过原因 | 重新启动条件 |
|------|---------|-------------|
| P0-2 (独立 Application 模块) | 功能可用，仅架构不干净；当前无循环依赖 | 有时间做结构性重构时 |
| P0-3 (WeixinLoginGatewayImpl @Transactional) | 跟随 P0-2 一并解决，减少迁移次数 | P0-2 完成后立即跟进 |
| P0-4 (幂等性设计) | 需前端配合改造请求参数；当前状态机提供了部分防护 | 前端排期支持 |
| P0-5 (Domain 跨模块依赖) | 需仔细分析影响范围，选方案三选一 | 有时间分析时 |

---

## 四、当前分支 Git 记录

```
85cc149 docs: 更新架构债务清单
b5ad008 refactor(app): 合并 OrderApplicationService，11 个方法，MallOrderController 收口
a5a91a1 refactor(domain): replace java.util.logging with @Slf4j
ce92651 fix: 修正 createOrder 中购物车清空时机
5421228 refactor: 库存预检查下沉 Domain 层
293bd33 refactor: dao 目录按领域拆分
35703fc refactor: config 目录按领域拆分
8c903aa refactor: 封装 NoPayNotifyOrderJob 的 Alipay 查询逻辑至 Gateway
```

---

> **供后续重构参考**：优先解决 P0-2（新建独立模块），然后把 P0-3 顺手解决。P0-4 需要跨团队协调前端。P1 级别问题可以逐个消化。
