# DDD 架构合规性审计报告

> **审计日期**: 2026-06-20
> **审计范围**: s-pay-mall 全模块（domain / infrastructure / trigger / app / api / types）
> **审计标准**: REVIEW.md (代码审查规则) + DDD 分层架构规范
> **问题级别**: P0 (严重) / P1 (一般) / P2 (建议)

---

## 一、总体评估

| 维度 | 状态 | 说明 |
|------|------|------|
| **Domain 层纯净度** | ✅ **通过** | 无 Spring 注解、无技术框架依赖 |
| **依赖流向 (Domain → Infrastructure)** | ✅ **通过** | Domain 层不依赖 Infrastructure 或 Trigger |
| **Infrastructure → App/Trigger** | ✅ **通过** | Infrastructure 层不反向依赖 App/Trigger |
| **App → Infrastructure** | ❌ **违规** | App 模块直接引用了 Infrastructure 的 DAO 和 Config (P1) |
| **Trigger → 直接调用 Domain** | ❌ **严重违规** | P0 级大规模问题，多个 Controller 绕过 Application 层 |
| **模块对称性 (Infra ↔ Domain)** | ⚠️ **部分违规** | 缺少 shared/ 目录、OrderRepositoryImpl 错位 |
| **层次职责边界** | ❌ **触发层和基础设施层有违规** | Controller 含业务逻辑，Infrastructure 含 @Transactional |
| **幂等性设计** | ❌ **严重缺失** | 所有 DTO 缺少幂等键，外部 API 缺乏幂等保护 |

---

## 二、P0 级别问题（必须立即修复）

### P0-1: Trigger 层 Controller 大面积直接调用 Domain 层服务

**违反规范**: REVIEW.md §1.3 — Trigger 层禁止直接调用 Domain 层服务，必须通过 Application 层

**问题描述**: 几乎所有 Controller 都直接注入并调用了 Domain Service 接口，绕过了 Application 层。这意味着事务控制、幂等性检查、DTO 转换等跨领域关注点完全缺失。

| 问题文件 | 直接注入的 Domain 服务 | 影响方法数 |
|---------|----------------------|-----------|
| `s-pay-mall-trigger/.../mall/MallOrderController.java:38-41` | `IMallCartService`, `IMallOrderService` | 7 个 (addCart, listCart, updateQuantity, deleteCartItem, createOrder, listOrders, continuePay, checkStock) |
| `s-pay-mall-trigger/.../mall/MallAdminController.java:43-55` | `IMallUserService`, `IMallProductService`, `IMallOrderService`, `IMallStatisticsService` | 15+ 个 |
| `s-pay-mall-trigger/.../mall/AdminApiController.java:35-48` | `IMallUserService`, `IMallProductService`, `IMallOrderService`, `IMallStatisticsService` | 15+ 个 |
| `s-pay-mall-trigger/.../mall/MallAuthController.java:31-38` | `IMallUserService`, `WeixinBindService`, `ILoginService` | 5 个 |
| `s-pay-mall-trigger/.../mall/ProfileController.java` | `IMallUserService` | 多个 |
| `s-pay-mall-trigger/.../mall/MallProductController.java` | `IMallProductService` | 多个 |
| `s-pay-mall-trigger/.../http/AliPayController.java:33-36` | `IOrderService`, `PayOrderService` | 2 个 |
| `s-pay-mall-trigger/.../http/AliPayReturnController.java:21` | `IMallOrderService` | 1 个 |
| `s-pay-mall-trigger/.../http/LoginController.java:30` | `ILoginService` | 2 个 |
| `s-pay-mall-trigger/.../http/WeixinPortalController.java` | `ILoginService`, `WeixinBindService` | 多个 |
| `s-pay-mall-trigger/.../job/NoPayNotifyOrderJob.java:26` | `IOrderService` | 1 个 |
| `s-pay-mall-trigger/.../job/TimeoutCloseOrderJob.java:18` | `IOrderService` | 1 个 |
| `s-pay-mall-trigger/.../job/StockPreheatRunner.java:27-30` | `IProductRepository`, `IStockGateway` | 1 个 |
| `s-pay-mall-trigger/.../listener/OrderTimeoutCloseRocketListener.java:21` | `IOrderService` | 1 个 |

**修复建议**:
1. 创建完整的 Application Service 层，每个领域业务对应一个 Application Service
2. 所有 Controller 改为注入 Application Service，而非 Domain Service
3. Job 和 Listener 同样通过 Application Service 调用领域逻辑

---

### P0-2: Application Service 错位放置在 Trigger 模块

**违反规范**: DDD 分层架构 — Application 层应独立于 Trigger 层

| 问题文件 |
|---------|
| `s-pay-mall-trigger/src/main/java/cn/fcr/trigger/application/OrderApplicationService.java` |

**问题描述**: `OrderApplicationService`（唯一的 Application Service）被放置在 `s-pay-mall-trigger` 模块的 `cn.fcr.trigger.application` 包下。根据 DDD 架构，Application Service 应属于独立的 Application 层（通常在 `s-pay-mall-app` 模块）。

**修复建议**: 将 `OrderApplicationService` 迁移到 `s-pay-mall-app` 模块（或新建 `s-pay-mall-application` 模块）。

---

### P0-3: Infrastructure 层包含 @Transactional 事务控制

**违反规范**: REVIEW.md §1.3（基础设施层）& §2.4（事务管理）— @Transactional 只应在 Application 层

| 问题文件 | 行号 | 问题说明 |
|---------|------|---------|
| `s-pay-mall-infrastructure/.../auth/gateway/WeixinLoginGatewayImpl.java` | L69 | `@Transactional(rollbackFor = Exception.class)` 出现在基础设施层的网关实现上。事务边界应在 Application 层控制 |

**问题描述**: `WeixinLoginGatewayImpl.createWechatUserAndBind()` 标注了 `@Transactional`。基础设施层不应控制事务，事务是应用层的编排职责。该方法同时包含了业务命名规则（生成 temp_/wx_user_ 前缀用户名），混合了数据访问和业务逻辑。

**修复建议**:
1. 移除 `WeixinLoginGatewayImpl.createWechatUserAndBind()` 上的 `@Transactional`
2. 在 Application Service 层添加事务控制
3. 将用户创建的业务命名逻辑抽离到 Domain 层

---

### P0-4: 幂等性设计严重缺失 — 所有 API 入口均无幂等保护

**违反规范**: REVIEW.md §5.1 — 所有触发状态变更的外部接口必须实现幂等性保护

**问题汇总**:

| 检查项 | 结果 |
|-------|------|
| RequestDTO 是否包含幂等键 (`requestId` 或 `orderNo`) | ❌ **所有 DTO 均缺失** |
| Application 层幂等性校验 | ❌ **不存在**（因无 Application Layer） |
| MQ Listener 幂等性检查 | ❌ **缺失**（OrderPaidRocketListener, OrderTimeoutCloseRocketListener） |

**问题描述**:
1. `OrderCreateRequestDTO.java` — 缺少 `requestId` 等幂等键，创建订单无幂等保护
2. `CreatePayRequestDTO.java` — 缺少 `requestId`，支付请求无幂等保护
3. `CartAddRequestDTO.java` — 缺少 `requestId`，添加购物车无幂等保护
4. `OrderPaidRocketListener` — 未使用 `IIdempotentGateway` 进行消费幂等检查
5. `OrderTimeoutCloseRocketListener` — 未进行幂等性检查
6. `OrderApplicationService` — 虽然有 `@Transactional`，但未在事务边界外执行幂等锁操作

> **注意**: 基础设施层虽然已经实现了 `IdempotentGatewayImpl`（基于 Redisson trySet），但它仅被 `DeductHandler` 等库存变更处理器内部使用，未暴露给外部 API 入口。

**修复建议**:
1. 在所有状态变更 DTO 中添加 `@NotNull private String requestId`（UUID 或雪花算法生成）
2. 创建 Application Service 实现"事务边界外执行锁操作"模式
3. MQ Listener 中添加幂等性检查

---

### P0-5: Domain 层存在跨领域反向依赖 (order → mall)

**违反规范**: DDD 聚合边界 — 领域模块之间应通过事件或共享内核解耦，不应直接依赖

| 问题文件 | 违反内容 |
|---------|---------|
| `s-pay-mall-domain/.../order/service/PayOrderService.java` | import `cn.fcr.domain.mall.gateway.IPayGateway` |

**问题描述**: `domain/order` 模块的 `PayOrderService` 依赖了 `domain/mall` 模块的 `IPayGateway`。这导致 `order` 领域无法独立于 `mall` 领域，破坏聚合边界。此问题直接导致基础设施层的跨模块耦合（`infrastructure/order/gateway/PaymentGatewayImpl.java` 同时依赖 `domain.order` 和 `domain.mall`）。

**修复建议**: 考虑通过领域事件解耦，或将 `IPayGateway` 的定义提升到 `domain/shared` 共享内核中。

---

### P0-6: Infrastructure 层缺失 shared/ 目录

**违反规范**: REVIEW.md §1.4（基础设施模块对称性）

| Domain 层 | Infrastructure 层 | 状态 |
|-----------|------------------|------|
| `domain/shared/` | ❌ **不存在** `infrastructure/shared/` | 缺失 |
| (跨领域) `infrastructure/config/` | ⚠️ 存在但缺少 shared 命名 | 建议统一 |

**问题描述**: Domain 层有 `domain/shared/model/entity/PayOrderEntity.java` 和 `domain/shared/model/vo/PayStatus.java` 等跨领域共享模型，但 Infrastructure 层没有对应的 `shared/` 目录。跨领域的基础设施实现（如 Redis 基础封装、MQ 基础配置）被分散在 `config/` 和 `dao/` 目录中。

**修复建议**: 添加 `infrastructure/shared/` 目录，将跨领域共用的技术实现统一归类至此。

---

### P0-7: Trigger 层 Job 包含业务逻辑（Alipay 直连 + 状态判断）

**违反规范**: REVIEW.md §1.3 — Trigger 层禁止编写业务逻辑

| 问题文件 | 行号 | 问题说明 |
|---------|------|---------|
| `s-pay-mall-trigger/.../job/NoPayNotifyOrderJob.java` | L37-49 | 直接使用 `AlipayClient` 查询支付宝交易状态，并判断 `"10000".equals(code)` 作为业务成功标志 |

**问题描述**: `NoPayNotifyOrderJob` 在 Trigger 层直接使用了第三方支付 SDK (AlipayClient) 进行交易状态查询，并对返回码进行业务判断。Alipay 交互和状态判断应封装在 Infrastructure 层的 Gateway 实现中。

---

### P0-8: Infrastructure 层包含业务逻辑（库存预检查）

**违反规范**: REVIEW.md §1.3 — Infrastructure 层禁止编写核心业务逻辑

| 问题文件 | 行号 | 问题说明 |
|---------|------|---------|
| `s-pay-mall-infrastructure/.../mall/gateway/StockGatewayImpl.java` | L57-60 | `if (currentStock < quantity)` — 库存充足性判断是业务规则 |

**问题描述**: `StockGatewayImpl.deductStock()` 中的库存预检查和二次检查虽然操作了 Redis 原子变量，但库存充足性 (`currentStock < quantity`) 的判断是业务不变量的校验，应在 Domain 层进行。

**修复建议**: 在 Domain 层 `OrderEntity` 或 `StockService` 中处理库存充足性校验，`StockGatewayImpl` 仅负责执行原子增减操作。

---

## 三、P1 级别问题（建议修复）

### P1-1: App 模块直接引用 Infrastructure

| 问题文件 | 行号 | 引用目标 |
|---------|------|---------|
| `s-pay-mall-app/.../Application.java` | L3-5 | `IMallUserDao`, `IUserRoleDao`, `MallUser` |
| `s-pay-mall-app/.../config/Retrofit2Config.java` | L3 | `IWeixinApiService` |
| `s-pay-mall-app/.../config/security/JwtAuthenticationFilter.java` | L3 | `JwtTokenProvider` |
| `s-pay-mall-app/.../Application.java` | L25 | `@MapperScan("cn.fcr.infrastructure.dao")` |

**修复建议**: 考虑通过 `@Import` 或自动配置机制间接引用，或接受 App 模块作为引导加载器的特殊定位。

---

### P1-2: MQ Listener 缺少消费幂等性

| 问题文件 | 问题说明 |
|---------|---------|
| `s-pay-mall-trigger/.../listener/OrderPaidRocketListener.java` | 未对 `PaySuccessMessage` 进行幂等检查 |
| `s-pay-mall-trigger/.../listener/OrderTimeoutCloseRocketListener.java` | 未对超时关单消息进行幂等检查 |

**修复建议**: 在 `onMessage()` 方法开头使用 `IIdempotentGateway.tryAcquire()` 进行幂等性检查。

---

### P1-3: Controller 中包含业务路由逻辑

| 问题文件 | 行号 | 问题说明 |
|---------|------|---------|
| `MallAuthController.java` | L48-59 | `if (request.getOpenId() != null && !request.getOpenId().isBlank())` — 根据 openId 决定注册策略 |
| `MallAuthController.java` | L128-138 | `"BINDING_PENDING".equals(status)` — 绑定状态判断逻辑 |
| `AliPayController.java` | L83-86 | `"TRADE_SUCCESS".equals(request.getParameter("trade_status"))` — 支付状态判断逻辑 |
| `MallOrderController.java` | L66-68 | `if (quantity < 1)` — 基础业务参数校验 |
| `AliPayReturnController.java` | L35-44 | `if (orderNo != null && !orderNo.isEmpty())` — 订单查询 + 状态日志 |

**修复建议**: 将业务判断逻辑迁移到 Domain 层，Controller 只负责参数校验和 DTO 转换。

---

### P1-4: Infrastructure 订单相关实现错位（OrderRepositoryImpl 在 mall/gateway/ 下）

**问题描述**: `OrderRepositoryImpl`（实现 `IMallOrderQueryGateway`）被放置在 `infrastructure/mall/gateway/` 下。虽然 `IMallOrderQueryGateway` 接口定义在 `domain/mall/gateway/`，但其实现本质是订单数据的持久化操作，放在 `mall/` 模块下会导致概念混淆。

**修复建议**: 考虑将 `IMallOrderQueryGateway` 接口提升到 `domain/shared/gateway/` 或在 `infrastructure/order/repository/` 下创建明确的订单查询实现，并在 `infrastructure/mall/gateway/OrderRepositoryImpl` 中委托调用。

---

### P1-5: OrderStateMachineServiceImpl 中 Redis 操作在 @Transactional 内部

**问题位置**: `OrderStateMachineServiceImpl.cancelOrder()` + `OrderApplicationService.cancelOrder()`

`OrderApplicationService.cancelOrder()` 标注了 `@Transactional`，但其调用链中包含了 `stockGateway.restoreStock()`（Redis 原子操作）。当 DB 事务回滚时，Redis 库存恢复无法自动回滚，导致 Redis 与 DB 库存不一致。

### P1-6: Trigger 层 Listener 包含业务编排逻辑

| 问题文件 | 问题说明 |
|---------|---------|
| `s-pay-mall-trigger/.../listener/OrderPaidRocketListener.java` L59-85 | `sendPaymentNotification()` 方法包含完整的业务流程：查询订单 → 查 openid → 格式化数据 → 发送微信通知。此编排应属于 Application Service |

**修复建议**: 将 `sendPaymentNotification()` 的业务编排迁移到 Application Service。

---

### P1-7: MQ 消息发送部分缺少超时参数

| 问题文件 | 行号 | 问题说明 |
|---------|------|---------|
| `s-pay-mall-infrastructure/.../order/event/RocketMqOrderEventPublisher.java` | L27 | `convertAndSend("order_paid", ...)` 无超时参数 |
| `s-pay-mall-infrastructure/.../order/gateway/OrderEventGatewayImpl.java` | L41 | `syncSend("pay-success-topic", orderNo)` 无超时参数 |

相比 `OrderEventGatewayImpl.java:35` 和 `OrderPaymentGatewayImpl.java:52` 正确设置了 `syncSend(topic, msg, 3000)`，以上两处缺少超时会阻塞主线程。

**修复建议**: 为 `syncSend` 和 `convertAndSend` 添加超时参数（如 3000ms）。

### P1-7: Domain 层 POM 包含非必要技术依赖

| 依赖 | 当前使用 | 风险 |
|------|---------|------|
| `spring-context` | 未在源码中直接使用 | 存在误用风险 |
| `spring-tx` | 未在源码中直接使用 | 存在误用 @Transactional 风险 |
| `alipay-sdk-java` | 未在源码中直接使用 | 技术框架依赖 |
| `jjwt` / `java-jwt` | 未在源码中直接使用 | 技术框架依赖 |
| `fastjson` | 未在源码中直接使用 | 技术框架依赖 |

**修复建议**: 移除 Domain 层 POM 中未使用的技术框架依赖，保持 Domain 层纯净。

---

## 四、P2 级别问题（建议优化）

### P2-1: Infrastructure 层 DAO 扁平化结构

`infrastructure/dao/` 目录包含了 9 个 DAO 接口和 9 个 PO 类，未被按照领域模块拆分。建议将这些 DAO 按 `auth/dao/`、`mall/dao/`、`order/dao/` 拆分，实现与领域模块的物理对齐。

### P2-2: 部分 Domain Service 方法缺少 JavaDoc

`MallOrderServiceImpl.java`、`MallCartServiceImpl.java` 中的部分公共方法缺少 JavaDoc 注释。

### P2-3: Infrastructure config 目录命名统一

当前 `infrastructure/config/` 承担了部分跨领域技术实现（如 Redis 配置、支付配置），建议统一为 `infrastructure/shared/config/`，与 `domain/shared/` 形成对称。

### P2-4: WeixinGatewayImpl 外部调用缺少显式超时配置 + 缺少业务错误码检查

**问题位置**: `Retrofit2Config.java` L17-21、`WeixinGatewayImpl.java` L79/107/133

虽然使用了 Retrofit2，但未显式配置 OkHttpClient 及超时参数（使用默认 10s）。此外 `WeixinGatewayImpl` 仅检查 HTTP 响应是否为 null 主体，未验证微信 API 返回的业务错误码（`errcode` 字段）。例如 `{"errcode":40001,"errmsg":"invalid credential"}` 会被静默忽略。

**修复建议**: 
1. 在 Retrofit2Config 中显式配置 OkHttpClient，设置 `connectTimeout=5s`、`readTimeout=10s`
2. 在微信 API 调用后检查 `errcode` 字段，非零则抛出异常

---

## 五、合规项（值得肯定的部分）

| 检查维度 | 结果 | 说明 |
|---------|------|------|
| Domain 层纯净度 | ✅ | 67 个 Java 源文件均无 Spring 注解、无技术框架导入 |
| Domain 层仓储/网关接口设计 | ✅ | 接口定义清晰，通过构造器注入实现（纯 POJO） |
| Domain 层实体封装 | ✅ | `OrderEntity.canPay()` 等状态守卫方法正确 |
| Domain 服务装配策略 | ✅ | 通过 `DomainServiceConfig` 集中装配，而不是注解扫描 |
| Infrastructure → Domain 实现 | ✅ | Infrastructure 层的仓储和网关正确实现了 Domain 接口 |
| Infrastructure 三层结构 | ✅ | `auth/`、`mall/`、`order/` 基本子模块存在 |
| 触发层无 Infrastructure 直接引用 | ✅ | Trigger 层不导入 `cn.fcr.infrastructure.*` |
| Infrastructure 无 Trigger/App 反向引用 | ✅ | Infrastructure 层不导入 Trigger 或 App |

---

## 六、问题统计

| 级别 | 数量 | 立即处理 |
|------|------|---------|
| **P0 (严重)** | 8 大类 | ✅ **必须立即修复** |
| **P1 (一般)** | 7 类 | ⚠️ 建议修复 |
| **P2 (建议)** | 4 类 | 可选优化 |

---

## 七、优先修复建议

### 第一阶段（立即 — 阻止架构腐化继续扩散）

1. **所有 Controller 迁移至通过 Application Service 调用 Domain** — 这是最核心的架构问题
2. **将 `OrderApplicationService` 移出 Trigger 模块到合适位置**
3. **移除 `WeixinLoginGatewayImpl` 上的 `@Transactional`**
4. **在 `OrderCreateRequestDTO` 和 `CreatePayRequestDTO` 中添加 `requestId` 幂等键**
5. **在 `OrderPaidRocketListener` 和 `OrderTimeoutCloseRocketListener` 中添加幂等性检查**

### 第二阶段（短期 — 系统性修复）

6. **拆分 Application Service 层**，覆盖所有业务入口
7. **抽离 Controller 中的业务逻辑到 Domain/Application 层**
8. **将 `NoPayNotifyOrderJob` 中的 Alipay 调用迁移到 Gateway**
9. **将 `StockGatewayImpl` 中的库存业务判断抽离到 Domain 层**
10. **添加 `infrastructure/shared/` 目录**

### 第三阶段（中长期 — 架构优化）

11. **消除 Domain 层 order → mall 的跨模块依赖**
12. **按领域模块拆分 `dao/` 目录**
13. **清理 Domain 层 POM 中的非必要依赖**

---

*报告生成日期: 2026-06-20*
*审计工具: Claude Code + 手动代码审查*
