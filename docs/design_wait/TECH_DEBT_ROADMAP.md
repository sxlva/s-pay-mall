# 技术债追踪清单 (Tech Debt Roadmap)

> **合并自**: `ARCHITECTURE_DEBT.md` + `SYSTEM_ARCHITECTURE_ISSUES.md` + `SYSTEM_ISSUES_SUMMARY.md` + `audit-20260702.md` + `FRONTEND_API_LAYER_ISSUES.md` + `interface-contract-inconsistency.md`
> **合并日期**: 2026-07-02
> **状态**: 待处理

---

## 一、后端架构技术债

### 🔴 P0 (阻塞级)

| ID | 问题 | 描述 | 修复路径 | 状态 |
|----|------|------|---------|------|
| P0-1 | 新旧订单系统并存 | `order` 包(旧,单商品)与 `mall` 包(新,多商品)两套 `OrderEntity`/`OrderStatusVO`/`OrderState` 并存，`AbstractOrderService` 仍被生产环境调用 | ①将 `handleTimeoutCloseOrder` 迁至 `OrderStateMachineServiceImpl` ②统一支付成功处理到 `MallOrderServiceImpl.paySuccess()` ③删除旧 `OrderEntity`/`OrderStatusVO`/`ShopCartEntity` | **已关闭（2026-10-03 legacy sunset 全部完成）**：①M2-1 超时关单分流；②回调统一走状态机；③`domain/order/legacy` + `infrastructure/order/legacy` + 绑定测试已整体删除，legacy 数据清零（pay_order 无主订单行=0），守卫规则 5 防回潮 |
| P0-2 | Application 层模块归属 | ~~`OrderApplicationService` 位于 trigger 模块的 `trigger.application` 包~~（2026-10-03 已迁移，详见附录 A.12） | 新建 `s-pay-mall-application` 模块，迁移 `OrderApplicationService` + `OrderTransactionService` | 已处理（2026-10-03） |
| P0-3 | Infrastructure 层 @Transactional 违规 | ~~`WeixinLoginGatewayImpl.createWechatUserAndBind()` 标注了 `@Transactional`~~（2026-10-04 已上移，详见附录 A.13） | 创建 `AuthApplicationService`，将事务上移至 Application 层 | 已处理（2026-10-04） |
| P0-4 | 幂等性设计缺失 | 所有状态变更 API 入口均无 `requestId` 幂等保护；MQ Listener 未进行 SETNX 消费幂等检查 | 状态变更 DTO 添加 `requestId` 字段；Application 层实现事务外锁操作；MQ Listener 加 `tryAcquire()` | **已关闭（2026-10-04）**：下单 requestId 幂等 + order_paid 消费幂等，三场景 E2E 锁定，详见附录 A.15 |
| P0-5 | Domain 层跨领域反向依赖 | `domain/order/service/PayOrderService.java` import 了 `domain.mall.gateway.IPayGateway` | 方案A: IPayGateway → `domain/shared/`；方案B: 领域事件解耦；方案C: PayOrderService → `domain/mall/` | 已处理（2026-10-03，M2-5：IPayGateway 随 mall 订单簇收编进 `domain/order/gateway`，PayOrderService 与 IPayGateway 同域，反向依赖自然消除，无需三选一） |
| P0-6 | Controller 中业务路由逻辑 | ~~`MallAuthController` 含 `if (openId != null)` 注册策略判断；`AliPayController` 含 `"TRADE_SUCCESS".equals()` 支付状态判断~~（2026-10-04 已下沉，详见附录 A.14） | 业务判断下沉到 Domain Service | 已处理（2026-10-04） |
| P0-9 | 并发回调重复扣库存 | `OrderStateMachineServiceImpl.paySuccess()` 先无锁读状态（`canPay()`），且 `OrderRepositoryImpl.updateOrderStatusByOrderNo` 的 UPDATE 无 status 条件（139-145 行）：并发 notify 均读到 INIT 时全部穿过守卫，各自执行 `syncDBStockForPaySuccess` 重复扣 MySQL 库存。**2026-10-03 实测复现**（10 线程同一瞬间重放同一合法 notify，证据：`s-pay-mall-start/src/test/java/cn/fcr/test/AlipayNotifyE2ETest.java#testPayNotify_concurrentDuplicateNotify`）：10 个事务全部通过 `canPay()`，库存 100→80（扣 10×2 件）；order_main/pay_order 双写同值无业务损害，但**库存扣减不在幂等保护内**。串行重放幂等正常（同测试类场景 2），仅并发触发 | ①`updateOrderStatusByOrderNo` UPDATE 加源状态条件，以影响行数作守卫（0 行即重复回调，直接返回不再扣库存）②`syncDBStockForPaySuccess` 仅在守卫通过时执行 ③修复后移除该测试的 `@Ignore` 作为回归测试。建议纳入 JV-003 M2（与超时关单分流同批触碰状态机） | **已关闭（2026-10-03）**：①UPDATE 增加 `expectStatus` 源状态条件（INIT 存储形式 CREATED，经 `OrderState.toDbStatus()` 映射），签名变为 `(orderNo, expectStatus, targetStatus)`；②paySuccess 影响行数 0 即返回 false，跳过 pay_order 更新与库存扣减；③deliver/complete/cancel 三变迁同步加守卫；④@Ignore 已移除，10 线程并发回归测试通过，全量测试 Skipped=0 |

### 🟡 P1 (重要级)

| ID | 问题 | 描述 | 修复路径 | 状态 |
|----|------|------|---------|------|
| P1-1 | Controller 大面积缺 @Valid | ~~11 个 Controller 仅 2 个方法使用了 `@Valid`~~（2026-10-02 核实：实际仅 1 处 `MallAdminController#saveProduct`，原记载与代码不符） | 逐个 Controller 方法加 `@Valid` + DTO 字段加校验注解 | 已处理（2026-10-02：8 个 `@RequestBody` DTO 端点补齐 `@Valid`；trigger 模块补 `spring-boot-starter-validation` 依赖——此前全项目无 JSR-303 实现，注解静默失效；`GlobalExceptionHandler` 新增 `MethodArgumentNotValidException → 0002` 映射；`CategorySaveRequestDTO.status`/`UserSaveRequestDTO.status` 的 `@NotNull` 放宽为可选以匹配前端表单；全部端点经真实请求实测） |
| P1-2 | Redis+DB 跨资源事务一致性 | ~~`cancelOrder()` 在 `@Transactional` 内包含 `stockGateway.restoreStock()`，DB 回滚时 Redis 无法回滚~~ | 参照 `createOrder` 模式，将库存恢复移到事务外 | 已处理（2026-10-04）：状态机 `cancel()` 改为仅 DB 状态流转，新增 `restoreStockForCancel(orderNo)` 由 `OrderApplicationService` 在事务提交后调用（主动取消与超时关单两个入口同模式）；`IMallOrderService.cancelOrder` 返回订单号以支撑事务外恢复；恢复失败仅告警不回滚（取消为终态，不构成超卖） |
| P1-3 | OrderPaidRocketListener 含业务编排 | ~~`sendPaymentNotification()` 在 Listener 中直接实现（查订单→查微信→发模板消息）~~ | 提取到 Application Service | 已处理（2026-10-04）：编排下沉为 `OrderApplicationService.sendPaySuccessNotification()`，Listener 仅保留幂等守门与消费语义，不再注入领域网关 |
| P1-4 | MQ 消息发送缺超时参数 | ~~`RocketMqOrderEventPublisher.convertAndSend` 无超时（`OrderEventGatewayImpl` 已于 2026-10-01 删除）~~ | 添加 3000ms 超时参数 | 已处理（2026-10-04）：`convertAndSend` 增加 3000ms 超时，与 `sendDelayCloseMessage` 约定一致 |
| P1-5 | Domain 层 POM 非必要技术依赖 | ~~POM 含 `spring-context`, `spring-tx`, `alipay-sdk-java`, `jjwt`, `fastjson` → 存在误用风险~~ | 逐个确认实际引用，移除或替换为标准 API | 已处理（2026-10-04）：domain 内 5 项依赖 + `guava`/`commons-codec`（均无引用）一并移除；`alipay-sdk-java` 改由 infrastructure 显式声明（原为 domain 传递依赖隐性引入，编译已证实）；全仓库无引用的 `java-jwt` 从 domain/start/根 pom 清理；`mvn test-compile` 全绿 |

### 🔵 P2 (优化级)

| ID | 问题 | 描述 | 修复路径 | 状态 |
|----|------|------|---------|------|
| P2-1 | createPayOrder 缺事务保护 | ~~`OrderApplicationService.createPayOrder()` 无 `@Transactional`~~（方法已删除） | 加注事务或委托给 `OrderTransactionService` | 已关闭（2026-10-03）：`createPayOrder` 为无调用方死端点，随 legacy 下线步骤 B 一并删除，条目失效 |
| P2-2 | 缺死信队列配置 | 3 个 RocketMQ Listener 均未配置 DLQ | 为 `order_paid`, `order-timeout-topic`, `product-stock-change-topic` 配置 DLQ | 部分处理（2026-10-04）：三个 Listener 已显式配置 `maxReconsumeTimes=5`（重试耗尽自动进 `%DLQ%`）；DLQ 告警与重放流程属运维项，方案见 [UPGRADE_POINTS.md](UPGRADE_POINTS.md) U-5，暂不实现 |
| P2-3 | WeixinGatewayImpl 缺超时配置 | `Retrofit2Config.java` 未显式配置 OkHttpClient 超时 | 设置 `connectTimeout=5s`, `readTimeout=10s` | 待处理 |
| P2-4 | ~~`pay-success-topic` 无消费者~~ | ~~`OrderEventGatewayImpl.sendPaySuccessMessage()` 发送消息但无消费者订阅~~ | ~~接入消费者或删除未使用的发送逻辑~~ | 已处理（2026-10-01，JV-003：删除 `IOrderEventGateway`/`OrderEventGatewayImpl`，topic 随之废弃） |
| P2-5 | ~~支付成功消息通道重复~~ | ~~`order_paid` 和 `pay-success-topic` 两个 Topic 职责不清~~ | ~~明确职责或合并~~ | 已处理（2026-10-01，JV-003：保留 `order_paid`，删除 `pay-success-topic` 通道） |
| P2-6 | `WeixinBindService` 方法未使用 | `tryAcquireRegisterLock()` / `releaseRegisterLock()` 定义但未调用 | 接入注册流程或移除 | 待处理 |
| P2-7 | 认证令牌双抽象命名冲突 | 账号密码链路用 `IAuthTokenGateway`/`AuthTokenGatewayImpl`（mall.gateway），微信链路用 `ITokenProvider`/`TokenProviderAdapter`（auth.gateway），两者均纯委托 `JwtTokenProvider.createToken`，同一概念两套接口、包位置与职责交叉 | 收敛为单一 `IAuthTokenGateway`（含 `createToken`/`encodePassword`/`matchesPassword`），移至 `domain.auth.gateway`；删除 `ITokenProvider`/`TokenProviderAdapter` | 已处理（2026-10-03：接口迁移 auth 域、`WeixinLoginService`/`DomainServiceConfig`/`MallUserServiceImpl` 改注入；编译通过、单测 6/6 通过、新增 `WeixinScanLoginMockE2ETest` 2/2 通过、真实应用注册→登录→profile E2E 验证通过；详见附录 A.11） |

---

## 二、前端架构技术债

### 🔴 P0 (阻塞级)

| ID | 问题 | 描述 | 修复路径 | 状态 |
|----|------|------|---------|------|
| FP0-1 | API 调用三层重叠 | `src/api/` + `src/repositories/` + `src/services/` 三层并存，职责重叠 | 合并为统一 `api/` 层 → 删除 `repositories/` 和 `services/` | 待处理 |
| FP0-2 | admin.ts 过于臃肿 | 294 行包含 5 个独立业务模块（用户/分类/商品/订单/统计） | 拆分为 `api/admin/user.ts`, `category.ts`, `product.ts`, `order.ts`, `statistics.ts` | 待处理 |
| FP0-3 | cartRepository 技术栈不一致 | 使用 bare `fetch`，其他层使用 Axios | 迁移为统一 Axios 实例 | 待处理 |
| FP0-4 | 前后端字段不一致 | `CheckoutPage.vue` 创建订单后 `orderNo` 硬编码为空串，后端返回的 `orderId` 未被使用 | 修正字段映射：`OrderCreateRespDTO.orderId` → 前端 `OrderCreateResult.orderNo` | 待处理 |

### 🟡 P1 (重要级)

| ID | 问题 | 描述 | 修复路径 | 状态 |
|----|------|------|---------|------|
| FP1-1 | 类型与 API 函数混放 | `admin.ts` 中 7 个类型接口与 API 函数混合定义 | 类型提取到 `types/domain/admin.ts` | 待处理 |
| FP1-2 | 统一 API 工具未使用 | `src/utils/api.ts` 提供了通用 request 工具但未被 `api/` 层使用 | 整合或删除 | 待处理 |
| FP1-3 | 前端多余字段 | `StockCheckResult` 含后端未定义的 `stockStatus` 字段 | 删除多余字段 | 待处理 |
| FP1-4 | 调试代码未清除 | `src/api/order.ts:26` 含 `console.log` | 删除调试语句 | 待处理 |
| FP1-5 | 命名不一致 | 管理端类型 `OrderAdminVO` vs 商城端 `Order` 后缀不统一 | 统一：管理端 `AdminVO` 后缀，商城端 `VO` 后缀 | 待处理 |

### 🔵 P2 (优化级)

| ID | 问题 | 描述 | 修复路径 | 状态 |
|----|------|------|---------|------|
| FP2-1 | 组件接口契约覆盖率低 | 20+ 页面/布局组件仅 3 个定义了 Props/Emits/Slots | 为关键组件添加类型化的 Props/Emits 定义 | 待处理 |
| FP2-2 | localStorage 残留 | `checkout_products` 写入后从不清理 | 添加清理逻辑 | 待处理 |

---

## 三、JSON 命名策略不一致问题

| 层级 | 当前状态 | JSON 输出 | 问题 |
|------|---------|----------|------|
| `api/dto/` (Controller 实际返回) | 无 `@JsonProperty` | camelCase | — |
| `api/vo/` (Controller 返回 VO) | 全部有 `@JsonProperty` | snake_case | 与 dto 输出策略不一致 |

**影响**: `vo/` 包声明了 snake_case 但 Controller 实际返回的是 dto 包 camelCase，前端同时消费两种命名风格。

**建议**: 统一为 camelCase（前端 TypeScript 惯例），逐步废弃 `@JsonProperty` 蛇形命名。

---

## 四、建议修复顺序

```
第〇阶段（可立即执行）：
├── FP1-4: 删除调试 console.log
├── FP1-3: 删除 StockCheckResult 多余字段
└── P2-3: WeixinGatewayImpl 加超时配置

第一阶段（核心问题）：
├── P0-1: 新旧订单系统统一
├── P0-6: Controller 业务逻辑下沉
├── P0-9: 并发回调幂等（条件更新守卫，修库存重复扣减）
├── FP0-4: 前后端字段不一致修复
└── P1-4: MQ 超时参数

第二阶段（架构重构）：
├── P0-2: Application 独立模块
├── P0-3: Infrastructure @Transactional 上移
├── FP0-1: 前端 API 三层合并
└── FP0-2: admin.ts 拆分

第三阶段（质量提升）：
├── P0-4: 幂等性补齐
├── P1-1: @Valid 全覆盖
├── P1-5: Domain POM 依赖清理
└── FP1-1: 类型与 API 分离

第四阶段（完善）：
├── P2-2: 死信队列
├── P2-4: pay-success-topic 消费者
├── P0-5: 跨领域依赖解耦
└── FP2-1: 组件接口契约
```

---

## 五、相关文档索引

| 文档 | 说明 |
|------|------|
| [DEVELOPMENT_GUIDE.md](../../DEVELOPMENT_GUIDE.md) | 技术契约 — 命名规范、架构约束 |
| [REVIEW.md](../../REVIEW.md) | 代码审查规则 |
| [FUTURE_FEATURES.md](FUTURE_FEATURES.md) | 未来功能规划 |

---

> **维护约定**: 后续所有技术债修复以本文档为唯一追踪来源。原 6 个独立文件不再更新，保留备查。


## 六、附录 A：已修复项（历史记录，供回溯参考）

以下为 2026-07-02 前已完成的重构项，保留上下文供后续修复参考。

### A.1 基础设施层模块化（阶段 1-3）

| 任务 | 说明 |
|------|------|
| config/ 按领域拆分 | `auth/` / `mall/` / `order/` / `shared/` |
| dao/ 按领域拆分 | `auth/` / `mall/` / `order/` |
| Notice.java 死代码删除 | 移除未使用的通知类 |
| StockGatewayImpl 核实 | 原子操作 + 竞态补偿，无需迁移 |
| RocketMqOrderEventPublisher 核实 | 事件发布基础设施，位置正确 |

### A.2 P0-7：NoPayNotifyOrderJob 解耦 Alipay SDK

**问题**：Job 直接依赖 Alipay SDK 进行 `"10000".equals(code)` 状态判断。
**修复**：封装至 `IAlipayQueryGateway` → `AlipayQueryGatewayImpl`。
**文件**：`domain/order/gateway/IAlipayQueryGateway.java`、`infrastructure/order/gateway/AlipayQueryGatewayImpl.java`

### A.3 P0-8：库存预检查下沉 Domain 层

**问题**：`StockGatewayImpl.deductStock()` 里做了库存充足性判断（Infrastructure 层包含业务逻辑）。
**修复**：预检查上移至 `MallOrderServiceImpl.checkAndDeductStock()`，Gateway 仅保留原子 `decr` + 竞态补偿。
**文件**：`domain/mall/service/impl/MallOrderServiceImpl.java`、`infrastructure/mall/gateway/StockGatewayImpl.java`

### A.4 7-1a：Mall Domain Application 层重建

**问题**：`MallOrderController` 直接注入 `IMallCartService` / `IMallOrderService`。
**修复**：在 `trigger/application/` 下新建 `OrderApplicationService`，包装购物车 + 订单的 8 个 Mall Domain 方法。
**文件**：`trigger/application/OrderApplicationService.java`、`trigger/http/mall/MallOrderController.java`

### A.5 7-1b：旧 Order Domain 迁移

**问题**：`AliPayController` 等 5 个 Trigger 层文件直接注入 `IOrderService` / `PayOrderService` / `IMallOrderService`。
**修复**：`OrderApplicationService` 新增 8 个旧 Order Domain 包装方法 + `getOrderByNo`，5 个文件全部改为注入 `OrderApplicationService`。
**文件**：`AliPayController.java`、`AliPayReturnController.java`、`NoPayNotifyOrderJob.java`、`TimeoutCloseOrderJob.java`、`OrderTimeoutCloseRocketListener.java`

### A.6 7-1c：管理后台链路收口

**问题**：`MallAdminController` 和 `AdminApiController` 仍直接注入 `IMallOrderService`。
**修复**：注入改为 `OrderApplicationService`，新增 `deleteOrder()` 方法统一事务边界。

### A.7 7-1d：createOrder / changeOrderPaySuccess 事务与 MQ 发送解耦

**问题**：
- `createOrder()` 中 `sendDelayCloseMessage()` 在 `@Transactional` 内部，MQ 发送失败会导致事务回滚
- `changeOrderPaySuccess()` 中事件发布在基础设施层 (`OrderRepository`)，应上移至 Application 层编排

**修复**：
- 新建 `OrderTransactionService`（package-private，仅 `OrderApplicationService` 内部调用）
- `OrderApplicationService.createOrder()` / `changeOrderPaySuccess()` 移除 `@Transactional`，委托 `OrderTransactionService`
- MQ 发送/事件发布失败用 try-catch 保护，不阻断主流程

### A.8 事件发布职责剥离

**问题**：`OrderRepository.changeOrderPaySuccess()` 在基础设施层直接调用 `orderEventPublisher.publishPaySuccess()`。
**修复**：从 `OrderRepository` 移除 `IOrderEventPublisher` 依赖，在 `OrderApplicationService.changeOrderPaySuccess()` 中编排。

### A.9 Bug Fix：库存扣减泄漏

**问题**：`MallOrderServiceImpl.checkAndDeductStock()` 两轮遍历（预检全部 → 逐项扣减），若第二轮中途失败，已扣库存无法回滚。
**修复**：合并为单轮——逐项检查后立即扣减并记录到 `deductedItems`，任何一项失败时内部调用 `restoreDeductedStock(已扣列表)` 回滚后抛出异常。

### A.10 Bug Fix：JWT Token 日志泄露

**问题**：`LoginController.checkLogin()` 将 `openidToken`（JWT 认证令牌）以 INFO 级别写入日志。
**修复**：日志行移除 `openidToken` 参数，仅保留 `ticket` 输出。

### A.11 P2-7：认证令牌双抽象收敛为单一 IAuthTokenGateway

**问题**：两条登录链路各建一套 Token 抽象——账号密码链路 `mall.gateway.IAuthTokenGateway`/`AuthTokenGatewayImpl`，微信扫码链路 `auth.gateway.ITokenProvider`/`TokenProviderAdapter`。两个实现的 `createToken` 均纯委托 `JwtTokenProvider`，`ITokenProvider` 全项目仅 1 个实现、1 个注入点、1 个调用点，属重复抽象；且认证职责放在 `mall` 域包名与职责错位。

**修复**：
- `IAuthTokenGateway` 从 `cn.fcr.domain.mall.gateway` 移至 `cn.fcr.domain.auth.gateway`，三个方法（`createToken`/`encodePassword`/`matchesPassword`）原样保留
- `AuthTokenGatewayImpl` 同步移至 `cn.fcr.infrastructure.auth.gateway`（git mv 保留历史）
- 删除 `ITokenProvider.java`、`TokenProviderAdapter.java`
- `WeixinLoginService`、`DomainServiceConfig`（两个 Bean）、`MallUserServiceImpl` 改为依赖新位置的 `IAuthTokenGateway`，仅 import/字段类型变化，业务逻辑零改动
- `JwtTokenProvider`（JWT 算法/secret/有效期）、登录业务逻辑、Controller、数据库均未触碰
- 设计文档同步：`docs/design/module-auth.md`（时序图）、`docs/design/README.md`（架构图）

**验证证据**：
- 全仓 grep 无 `ITokenProvider`/`TokenProviderAdapter`/`mall.gateway.IAuthTokenGateway` 残留
- `mvn compile` / `mvn test`（含 `DomainArchitectureGuardTest` 架构守护）通过，6/6
- 新增 `WeixinScanLoginMockE2ETest`（`@MockBean` 替换唯一调用微信服务器的 `WeixinGatewayImpl`，其余真实装配）：首次扫码自动注册签发合法 JWT、同 openid 复用账号两场景 2/2 通过
- 真实应用 E2E：注册 → 登录 → 携带 JWT 访问 profile（200）→ 无 token 访问 profile（403），签名解析正常

**经验**：领域自定义函数式接口（`PasswordMatcher`）作参数类型是干净的解耦点——`UserEntity.validatePassword` 以方法引用绑定，接口合并不影响实体及其测试。

### A.12 P0-2：Application 层独立模块 + 启动模块更名 start

**问题**：`s-pay-mall-application` 模块不存在，`OrderApplicationService`/`OrderTransactionService` 寄居于 trigger 模块的 `cn.fcr.trigger.application` 包，违反 DDD 规范 §1.1 模块物理归属规则；同时 `s-pay-mall-app` 命名与 Application 层概念易混淆。

**修复**：
- 新建 `s-pay-mall-application` 模块（pom 仅依赖 `s-pay-mall-domain` + `spring-context` + `spring-tx` + `slf4j-api` + `lombok`，未复制其他子模块的 `maven-archetype-plugin`）
- 两个服务**一起**迁移至 `cn.fcr.application` 包（代码零改动，仅 package/import 变化），避免附录 C.2 所述 `trigger ↔ application` 循环依赖
- `s-pay-mall-app` 更名 `s-pay-mall-start`（git mv 保留历史；artifactId/finalName/`spring.application.name` 同步），定位明确为装配/启动模块
- 父 pom `<modules>` + `dependencyManagement`、`trigger`/`start` pom 依赖按附录 C.2 第 2/3 条更新
- 7 个 trigger 调用方 + 1 个 E2E 测试的 import 由 `cn.fcr.trigger.application` 改为 `cn.fcr.application`
- trigger pom 移除显式 `spring-tx` 依赖（trigger 内已无 `@Transactional` 使用，且 `spring-boot-starter-jdbc` 传递引入）
- 附带修复（legacy 下线步骤 B 的 trigger 侧遗漏）：`7218299` 删除 legacy 包时 `trigger/application` 仍引用已删除的 `IOrderService`/`ShopCartEntity`，且 import 停留在 M2 重组前的旧包路径，**全量编译实际已处于失败状态**（IDEA 增量编译掩盖了该问题）。按 LEGACY_SUNSET_DESIGN 步骤 B/M2-1/A1 补齐：`createPayOrder` 死方法删除（B3，无调用方）、超时关单改走状态机 `cancel` 分流 + order_main 不存在记 warn 返回 false（M2-1+B1）、回调旧分支改 warn 日志直接返回（B2）、补偿查询改委托 `IPayOrderGateway.queryNoPayNotifyOrder()`（A1）、两服务构造函数摘除 legacy 注入（B4）

**验证证据**：
- 全仓 grep 无 `cn.fcr.trigger.application` / `s-pay-mall-app` 残留（除历史记录外）
- `mvn compile` 全模块通过；`mvn package -DskipTests` 打包通过
- 模块依赖方向与 DDD 规范 §1.2 一致，无循环依赖

**经验**：先全仓 grep 调用方再动手——本次 7 个调用方全部在 trigger 内部、两服务 import 面只有 domain + spring-tx + lombok，是纯搬运；`git mv` 目录更名让启动模块的未提交改动（P0-9 进行中）无损跟随。

### A.13 P0-3：Infrastructure 层 @Transactional 上移至 AuthApplicationService

**问题**：`WeixinLoginGatewayImpl.createWechatUserAndBind`（infrastructure）标注 `@Transactional`，承担"首次扫码自动注册"（插 mall_user → 更新用户名 → 插 user_binding → 插 user_role）的事务边界，违反 DDD 规范 §2.4 / §6.1。

**修复**：
- `s-pay-mall-application` 新建 `AuthApplicationService`，`handleWechatScanLogin(ticket, openid)` 标注 `@Transactional` 并整体委托 `ILoginService`——WeixinLoginService **零改动**，"是否新用户"的业务规则仍留在 Domain 层
- `WeixinLoginGatewayImpl` 摘除 `@Transactional` 及 import，方法体 4 步写库语义不变
- `WeixinPortalController` SCAN 登录分支注入改为 `AuthApplicationService`（生产入口与事务边界对齐）；`LoginController` 取 ticket/轮询为无事务读，保持注入 `ILoginService` 不动
- `WeixinScanLoginMockE2ETest` 入口同步切换，并新增场景 3：模板通知异常 → 断言 `user_binding` 不存在（4 个写操作整体回滚）
- 未出现 `AuthApplicationService → WeixinLoginService → AuthApplicationService` 回环；Domain 无任何 `cn.fcr.application` 引用

**验证证据**：
- 全仓 `@Transactional` 仅存于 `s-pay-mall-application` 三个类（`OrderApplicationService`/`OrderTransactionService`/`AuthApplicationService`），Infrastructure / Domain 层零事务注解
- `WeixinScanLoginMockE2ETest` 3/3 通过（含新增回滚场景）；domain 全量测试 9/9 通过（含 `DomainArchitectureGuardTest` 架构守护）；全量编译打包通过
- 依赖方向保持 `Trigger → Application → Domain → Gateway` 单向

**经验**：事务边界必须挂在调用链最外层的 Spring 代理入口；Domain 纯 POJO 无法持有事务，"判断规则留 Domain、用例编排+事务留 Application"是两边都不重写的最小方案。

### A.14 P0-6：Controller 业务路由逻辑下沉（注册策略 + 支付状态判断）

**问题**：
- `MallAuthController#register` 用 `if (openId != null)` 在 Trigger 层路由注册策略；且 Domain 侧 `register()`/`registerWithWeChat()` 约 80% 步骤复制（查重/建户/赋角色硬编码 `2L`）
- `AliPayController#payNotify` 用 `"TRADE_SUCCESS".equals()` 裸字符串判断支付状态；Domain 侧 `PayOrderEntity.verifyCallbackSign(Map, orderNo, amount)` 重复同一状态判断且**全仓零调用**（死代码，方法名与实际行为"对单号/对金额/看状态"不符）
- 跨模块双写：`WeixinLoginGatewayImpl.createWechatUserAndBind`（infrastructure）直写 mall_user/user_binding/user_role 三个 DAO，与 `MallUserServiceImpl.registerWithWeChat` 是同一套建户规则的两份实现

**修复**：
- 新增 `domain/order/model/vo/PayTradeStatus` 枚举（WAIT_BUYER_PAY/TRADE_CLOSED/TRADE_SUCCESS/TRADE_FINISHED），`isSuccess()` 唯一承载"算成功"规则
- `OrderApplicationService.handleAlipayCallback(params, alipayPublicKey)`：状态判断 → 验签 → `changeOrderPaySuccess` 一步编排，Controller 只解析参数并回 success/false；删除 `verifyPayCallbackSign` 中转方法与实体死方法
- `IMallUserService#register(username, password, openId)` 统一注册入口（openId 空白→账密注册，否则微信注册并绑定），原两个 public 方法降为实现内私有方法；公共步骤收敛为私有 `createUser`（查重+建户+赋角色），角色 ID 提为常量 `MEMBER_ROLE_ID`
- 新增 `IMallUserService#registerWeChatUserByScan(openId)`：扫码自动注册（临时名落库 → `IUserRepository.updateUsername` 固化 `wx_user_{userId}` → 绑定 → 签发 token）收敛进用户领域服务；`WeixinLoginService` 构造注入 `IMallUserService`（跨域协作走 domain 服务接口，"是否新用户"规则仍在 auth 域）；`IWechatLoginGateway`/`WeixinLoginGatewayImpl` 删除 `createWechatUserAndBind`，移除 mall_user/user_role DAO 依赖，只留查询+缓存
- `IUserRepository` 新增 `updateUsername`（既有 `updateUser` 只更新 status/password，语义不匹配）
- `DomainServiceConfig` 同步 `weixinLoginService` Bean 装配（mallUserService → weixinLoginService 单向，无循环依赖）

**验证证据**：
- 全仓 grep 确认旧入口（`verifyPayCallbackSign`/`registerWithWeChat` public 签名/`createWechatUserAndBind`）零残留，仅文档/历史附录提及
- 8 模块 `mvn compile` BUILD SUCCESS
- `WeixinScanLoginMockE2ETest` 走 `authApplicationService.handleWechatScanLogin` 生产入口，断言（wx_user_{uid}/MEMBER/绑定落库/异常回滚）与本修复后的领域路径一致，注释同步更新

**经验**：Trigger 层只做协议适配（解析/回执），任何 `if` 背后只要有业务语义就该问"这条规则有没有第二个入口"——本次两处重复都是被第二个入口（扫码自动注册、实体校验方法）暴露出来的；枚举 + 领域服务单一入口是这类下沉的标准形态。

### A.15 P0-4：状态变更入口幂等保护（下单 requestId 幂等 + order_paid 消费幂等）

**问题**：
- `POST /orders` 无任何幂等保护：网络重试/用户双击/前端重复提交会产生重复订单，并各自预扣 Redis 库存
- `order_paid` MQ 监听器（`OrderPaidRocketListener`）无消费幂等：RocketMQ at-least-once 重投会导致 `paySuccess` 状态机重复变迁、MySQL 库存重复扣减、微信支付成功通知重复推送

**修复**：
- `IIdempotentGateway` 扩展为通用业务幂等网关：新增 `tryAcquire(type, no, ttlSeconds)` 重载、`markDone(type, no, resultValue)`（记录业务结果值）、`getValue(type, no)`；幂等键前缀 `stock:event:` 保留不动（旧键兼容，避免 24h 窗口内库存双扣）
- `UserOrderCreateReq` 新增可选字段 `requestId`；`OrderApplicationService.createOrder(userId, address, requestId)`：
  - requestId 非空 → 幂等键=requestId（24h）。获取锁失败时读结果值：已完成（orderNo）则查单组装 `OrderCreateVO` 返回首次订单（payUrl 为空，前端走 continue-pay）；`PROCESSING` 中则抛"订单正在处理中"提示
  - requestId 为空 → 降级为用户级短锁 `uid:{userId}`（10s），仅防双击/并发重发
  - 成功后 `markDone` 写 orderNo；业务异常 `release` 允许用户重试（幂等锁在事务外，不 hold 事务）
- `OrderPaidRocketListener.onMessage` 开头 `tryAcquire(BUSINESS_TYPE_ORDER_PAID_NOTIFY, orderNo)`：获取失败即重复消息，log 后直接 return 不抛异常（ACK 不重投）；处理异常则 `release` 并重抛（交给 MQ 重投）
- 顺带修复：`ffd29bf` 迁移时误删 `WeixinLoginGatewayImpl.getLoginToken` 的"取后即删"，扫码登录 token 5 分钟内可重放，已恢复一次性消费语义

**验证证据**：
- 新增 `OrderCreateIdempotencyE2ETest` 三场景全绿（13/13）：①同 requestId 串行重发 → 返回同一 orderNo、pay_order 仅一行、Redis 库存仅预扣一次；②无 requestId 10 线程并发 → 恰好 1 成功 9 拒绝（IllegalStateException）；③order_paid 重复消费 → `sendPaymentSuccessNotification` 仅调用 1 次
- 全量 `mvn clean install` EXIT=0，domain 守卫 9/9 绿

**经验**：幂等锁必须在事务外获取（否则锁持有期间挂起事务连接）；"处理中"与"已完成"要用不同值区分（`PROCESSING_VALUE` vs 业务结果值），否则重发请求分不清"等一等"还是"返回结果"；MQ 消费幂等失败不抛异常是标准做法——抛异常只会触发无谓重投。

---

## 七、附录 B：新旧订单系统依赖关系与迁移参考

### B.1 当前调用链路（旧系统仍被调用）

```
超时关单 MQ → OrderApplicationService → IOrderService → AbstractOrderService
                                                    ↓
                                              OrderService

支付回调   → OrderTransactionService   → IOrderService → OrderService
                                                    ↓
                                              repository.changeOrderPaySuccess()

补偿查询   → OrderApplicationService   → IOrderService → OrderService
                                                    ↓
                                              repository.queryNoPayNotifyOrder()
```

### B.2 order 包文件处理建议（P0-1 详细参考）

| 文件 | 处理建议 | 原因 |
|------|---------|------|
| `IOrderEventPublisher` | **保留** | 跨域共享，支付回调时使用 |
| `PayOrderService` | **保留** | 支付链接生成和验签逻辑可复用 |
| `AbstractOrderService` | **迁移后删除** | 超时关单逻辑需迁移 |
| `IOrderService` | **迁移后删除** | 接口定义需合并 |
| `IOrderRepository` | **迁移后删除** | 仓储接口需合并 |
| `OrderEntity` (order包) | **删除** | 已被 mall 包替代 |
| `OrderStatusVO` | **删除** | 已被 OrderState 替代 |
| `ShopCartEntity` | **删除** | 已被 CartItemVO 替代 |
| `CreateOrderAggregate` | **删除** | 已被 OrderEntity.createFromCart() 替代 |
| `OrderService` | **删除** | 已被 MallOrderServiceImpl 替代 |

---

## 八、附录 C：模块依赖与打包分析（P0-2 详细参考）

### C.1 实际依赖方向（pom.xml 证实，2026-10-03 P0-2 修复后）

```
s-pay-mall-start        → trigger / application / domain / infrastructure
s-pay-mall-trigger      → application / domain / infrastructure（+ api / types）
s-pay-mall-application  → domain
s-pay-mall-infrastructure → domain
s-pay-mall-domain       → types / api
```

- `s-pay-mall-application` 模块已创建，`OrderApplicationService`/`OrderTransactionService` 位于 `cn.fcr.application` 包内
- `s-pay-mall-app` 已更名 `s-pay-mall-start`（装配/启动模块，不含业务代码）
- 当前**无循环依赖**

### C.2 提取独立模块注意事项（已于 2026-10-03 按此执行，详见附录 A.12）

1. 需 `OrderApplicationService` + `OrderTransactionService` **一起移出**，否则产生 `trigger ↔ application` 循环 —— 已照做（两服务同迁 `cn.fcr.application`）
2. `s-pay-mall-trigger/pom.xml` → 添加依赖 `s-pay-mall-application` —— 已添加
3. `s-pay-mall-start/pom.xml`（原 `s-pay-mall-app`）→ 添加依赖 `s-pay-mall-application` —— 已添加
4. 4 个子模块（trigger/domain/infrastructure/types）均配置了 `maven-archetype-plugin`，可能干扰正常打包 —— 新模块未复制该插件

---

## 九、附录 D：前后端类型比对参考（FP0-4 / FP1-3 详细参考）

| 来源文件 | 后端DTO | 前端类型 | 字段一致性 |
|---------|---------|---------|-----------|
| `api/dto/OrderCreateRespDTO` | `orderId` (String) | `OrderCreateResult.orderNo` (string) | **不一致** — `orderId` vs `orderNo`，前端 `CheckoutPage.vue` 硬编码空串 |
| `api/dto/CartItemRespDTO` | `price` (String) | `CartItem.productPrice` (string) | 运行时常一致（映射层中转） |
| `api/dto/OrderListRespDTO` | `orderNo` (String) | `Order.orderNo` (string) | ✅ **一致** |
| `api/vo/ProductVO` | `category_id` (有@JsonProperty) | `ProductVO.category_id` | ✅ **一致**（前端统一用下划线） |
| `api/dto/StockCheckRespDTO` | `success`(Boolean)+`message`(String) | `StockCheckResult.success`+`message`+`stockStatus` | **不一致** — 前端多余 `stockStatus` 字段 |

### D.1 前端额外字段说明

| 前端文件 | 路径 | 说明 |
|---------|------|------|
| `CartItemRaw`, `CartState` | `types/domain/cart.ts` | 前端自有中间状态类型，非后端DTO直接映射 |
| `OrderItem`, `OrderState`, `orderCount/items/updateTime` | `types/domain/order.ts` | 部分为前端自有，部分为后端 `OrderListRespDTO` 映射 |
| `PayOrder`, `PayStatus`, `PollingState`, `PaymentState` | `types/domain/payment.ts` | 全部为前端自有状态管理类型 |

---

## 十、附录 E：暂缓/有意跳过项及原因

| 项目 | 跳过原因 | 重新启动条件 |
|------|---------|-------------|
| P0-4 (幂等性设计) | 需前端配合改造请求参数；当前状态机提供了部分防护 | 前端排期支持 |
| P0-5 (Domain 跨模块依赖) | 需仔细分析影响范围，选方案三选一 | 有时间分析时 |
| C1：`changeOrderClose` 关单时写 `pay_time=now()` | 数据字段语义污染（未支付订单有支付时间），不影响功能正确性 | 支付链路审计时一并处理 |
| C2：`OrderState.DONE` 存储口径不一致（`toDbStatus()`=COMPLETED vs 状态机写 `getCode()`=DONE） | 读取端 `fromDbStatus` 双兼容已兜住 | 订单状态口径统一时处理 |
| C3：支付回调未校验 `total_amount` 与订单应付金额一致 | 验签已保证参数真实性，属支付安全增强而非 correctness blocker；需 BigDecimal 比较 | 支付安全加固批次 |
| C4：重复回调重复发布 `order_paid` 事件 | 下游消费幂等（orderNo 幂等键）已兜底，仅产生冗余消息 | 事件链路优化时 |
| C5：`sendDelayCloseMessage` 内层 catch 吞异常 | 失败仅靠 `NoPayNotifyOrderJob` 补偿；外层 catch 已成死代码；涉及可靠性设计取舍 | 可靠性设计专题 |
| ~~6 个零调用 public API（`ILoginService.saveLoginState` + `IOrderDao` 的 `queryTimeoutCloseOrderList`/`queryUnPayOrder`/`queryOrderByOrderNo`/`closeOrderWithOptimisticLock`/`updateStatus`）~~ | ~~无调用方，但删除前需按项目规则再全仓核对一遍引用~~ | 已处理（2026-10-04，⑦ P2 批次）：删除前全仓（含测试与 XML）零引用复核通过，接口/实现/私有 helper 一并移除 |
| ~~`AlipayGatewayImpl`【验签调试】日志打印 sign 前 50 字符与待签串前缀~~ | ~~生产噪音~~ | 已处理（2026-10-04，⑦ P2 批次）：4 条调试日志降为 debug 级，排障可临时调高，生产默认不输出 |
| ~~实际 DB 名 `s-pay-mall` 与 AGENTS.md `DB_NAME=s_pay_mall` 不一致~~ | ~~文档小差异~~ | 已处理（2026-10-04，⑦ P2 批次）：AGENTS.md 修正为 `s-pay-mall` |

---

## 十一、附录 F：文档与代码不一致清单（2026-08-23 审计新增）

> 以下为规则体系重构审计中发现的文档与代码不一致项。规则文档已修正为以代码为准，此处登记供回溯参考。

| ID | 问题 | 文档声称 | 代码实际 | 代码位置 | 处置 |
|----|------|---------|---------|---------|------|
| DOC-1 | DomainServiceConfig @Bean 数量过时 | "12 个 @Bean"（DEVELOPMENT_GUIDE §2.4 + docs/design/README §三 原文） | **17 个 @Bean** | [DomainServiceConfig.java](../../s-pay-mall-infrastructure/src/main/java/cn/fcr/infrastructure/config/shared/DomainServiceConfig.java) | 新 SSOT 不写死数量，改为引用代码 |
| DOC-2 | Redis 幂等 Key 格式不一致 | `mall:stock:msg:processed:{messageId}`（DEVELOPMENT_GUIDE §五 原文） | `stock:event:{businessType}:{businessNo}` | [IdempotentGatewayImpl.java](../../s-pay-mall-infrastructure/src/main/java/cn/fcr/infrastructure/mall/gateway/IdempotentGatewayImpl.java) | 新 SSOT 以代码为准，标注差异 |
| DOC-3 | ~~AdminApiController 路径描述不精确~~ | ~~"与 MallAdminController 重复路径结构"（DEVELOPMENT_GUIDE §1.6 原文）~~ | ~~前缀不同（`/pay-api/.../admin` vs `/mall-api/.../admin`），功能 CRUD 重复~~ | ~~[MallAdminController.java](../../s-pay-mall-trigger/src/main/java/cn/fcr/trigger/http/mall/MallAdminController.java) / [AdminApiController.java](../../s-pay-mall-trigger/src/main/java/cn/fcr/trigger/http/mall/AdminApiController.java)~~ | 已处理（2026-10-01，JV-003 第二批：删除 `AdminApiController` + SecurityConfig 死规则 + 契约 §4.2 移除） |
| DOC-4 | REVIEW.md 悬空引用 | 引用 `CLAUDE_project_guide_v2.md`（3 处） | 该文件不存在 | [REVIEW.md](../../REVIEW.md) 原 §1.1/§7.4.4/§三 | REVIEW.md v3.0 已修复，改为引用 DDD_ARCHITECTURE_SPEC.md |

---

## 十二、附录 G：支付状态修复与测试机制分层（2026-10-04，①②③④ 批次）

> 批次纪律：一次只处理一个问题，先调查/修改/验证再进入下一个。本附录按批次记录。

### G.1 ① B1：支付宝交易查询误判支付成功

**问题**：`AlipayQueryGatewayImpl.queryTradeSuccess` 只判断 `code == "10000"`——该业务码仅表示**查询请求本身成功**，不代表交易支付成功。`NoPayNotifyOrderJob`（每 30s）会把 `WAIT_BUYER_PAY`（待付款）的订单误判为支付成功并触发履约。属支付状态机入口错误。

**修复**：判定口径改为 `code=10000 && tradeStatus=TRADE_SUCCESS` 双重校验；异常/空响应分支语义不变。顺带将 `@Resource` 字段注入改为构造器注入（与同包 `AlipayGatewayImpl` 风格一致）。

**验证证据**：新增 `AlipayQueryGatewayImplTest` 7 用例全绿（TRADE_SUCCESS→true / WAIT_BUYER_PAY→false / TRADE_CLOSED→false / code≠10000→false / tradeStatus 缺失→false / 空响应→false / 客户端异常→false）；全仓 `mvn test-compile` 通过；唯一调用方 `NoPayNotifyOrderJob` 语义直接改善，无需改动。

**经验**：第三方网关的"调用成功"与"业务成功"必须分开判断；SDK 业务码的第一语义是通信/请求层面。

### G.2 ② Domain 测试发现机制修复

**问题**：`OrderEntityTest`（29 用例）与 `UserEntityTest`（10 用例）使用 JUnit 4，domain pom 同时有 junit-jupiter + Surefire 3.0.0-M5 → provider 走 JUnit Platform → **缺 `junit-vintage-engine` 时 JUnit 4 类被静默跳过**，无任何告警。`mvn test` 显示 "Tests run: 9" 但名义测试远多于此。

**修复**：两个测试类迁移到 JUnit 5（删除自写 `fail`/`assertThrows`/`assertDoesNotThrow` 辅助方法）；domain pom 移除 `junit:junit`（消除 footgun）。

**迁移后暴露的断言漂移**：6 处失败均为异常消息文本与生产代码脱节（守卫行为正确）——生产消息早已改为"拒绝支付/取消/发货/完成操作"与"购物车数据流为空，无法组装订单"，测试断言同步对齐。

**验证证据**：`mvn test -pl s-pay-mall-domain -am` 从 Tests run: 9 恢复到 **48/48 全绿（4 个类全部被执行）**。

**经验**：Surefire 3.x + jupiter 环境下，JUnit 4 测试缺失 vintage engine 是**静默**跳过——测试计数（Tests run 总数）是发现此问题的唯一信号，排查时应先核对"类数 × 每类用例数"与报告总数。

### G.3 ③ Surefire / Failsafe 测试分层

**目标结构**：`mvn test` = 快速单测（无 Docker 依赖）；`mvn verify -P e2e` = 需 MySQL(23306)/Redis(26379)/RocketMQ(9876) 的完整 E2E。

**修复**：
- 根 pom：新增 `<skipStartTests>true</skipStartTests>` 默认值 + `e2e` profile（置 false）
- start pom：Surefire 2.6 → 3.2.5 并参数化 `skipTests=${skipStartTests}`；新增 Failsafe 3.2.5（integration-test/verify goals）
- 4 个 E2E 类 `git mv` 改名 `*E2ETest → *E2EIT`（匹配 Failsafe 默认 include，保留 git 历史）；`ApiTest` 不动，自然归入 e2e
- start pom 新增 `junit-vintage-engine`（test）——Failsafe/Surefire 3.2.5 检测到 jupiter 后走 JUnitPlatform provider，start 测试均为 JUnit 4，否则 **0 测试被执行**（G.2 同因）

**踩坑记录（重要）**：最初按惯例把 `<skipTests>true</skipTests>` 定义在根 properties——**它是 Surefire 的 user property，会被所有模块的 Surefire 拾取，导致全部单元测试被跳过**。已改用专用属性名 `skipStartTests`。教训：Surefire/Failsafe 的参数名（skipTests/skip/maven.test.skip）不可占用为项目属性名。

**验证证据**：`mvn clean test`：domain 48 + application（当时 0）+ infrastructure 7 全绿，start "Tests are skipped"；`mvn verify -P e2e -pl s-pay-mall-start`：ApiTest 1 + 4 个 E2EIT 共 13 用例全绿，BUILD SUCCESS。

### G.4 ④ B2：超时关单与晚到支付死路

**问题**：延时关单消息写死 `delayLevel=5`（1 分钟），用户付款时订单可能已被关闭：回调验签通过 → 状态机 `canPay=false` 只记 warn → 应用层不抛异常仍回支付宝 success（不再重投）→ 订单 CANCELED 但钱已收；且 `queryNoPayNotifyOrder` 只捞 `WAIT_PAY` 订单，这笔死单永远无法被补偿。

**业务规则**（先行明确）：订单关闭前允许支付；关闭后收到成功支付通知必须进入明确异常路径，不能静默。

**修复**（三道防线）：
1. **延长关单窗口**：`delayLevel=5`(1min) → `9`(30min)，与 `queryTimeoutCloseOrderList` 的 30 分钟口径一致
2. **关单前二次确认**：`handleTimeoutCloseOrder` 先 `alipayQueryGateway.queryTradeSuccess(orderNo)`，已支付则转 `changeOrderPaySuccess` 履约路径、不关闭订单（注入 `IAlipayQueryGateway`）
3. **晚到支付异常路径**：`changeOrderPaySuccess` 发现订单已 CANCELED → error 日志"需人工核实退款或补履约"，**不履约、不发布支付成功事件**；仍回支付宝 success 以终止其重试（重试只会重复进入本路径）

**验证证据**：新增 `OrderApplicationServiceTest` 5 用例全绿（订单不存在/已支付转履约/未支付关单+恢复库存/守卫拒绝不恢复库存/晚到支付异常路径）——application 模块首个单测；`TimeoutCloseOrderE2EIT` 加 `@MockBean AlipayQueryGatewayImpl`（避免 E2E 请求外网支付宝）并新增场景 3"关单前已支付→转履约（PAID+DB 库存扣减）"全绿；单测 60 + E2E 14 全量通过。

**经验**：支付链路的"时间窗"与"二次确认"是互补关系——延长窗口降低撞车概率，二次确认兜底残余窗口；晚到支付的终态必须显式（日志留痕 + 人工介入标记），静默 ACK 是资金类 bug 的温床。

### G.5 观察记录（未复现，持续观察）

- 2026-10-04 批次③验证期间：两次出现**一次性、不可复现**的失败——① domain 大面积 error（48 中 41 error + 5 failure，重跑即绿）；② E2E 中 `OrderCreateIdempotencyE2EIT` 上下文加载失败（`PasswordEncoder` 缺失，同命令重跑即绿）。两次均非确定性，同命令立即重跑均通过。疑似 Windows 文件锁/资源时序类环境问题。**若再次出现，优先怀疑环境而非代码**，并保留完整输出与 dump 文件（`target/surefire-reports/*.dumpstream`）。

### G.6 ⑦ P2：零调用 API 清理与日志降噪

**问题**：审查确认 6 个 public API 全仓零引用——`ILoginService.saveLoginState`（含 `WeixinLoginService` 实现与私有 `persistLoginState`）；`IOrderDao` 的 `queryUnPayOrder`、`queryTimeoutCloseOrderList`、`queryOrderByOrderNo`、`closeOrderWithOptimisticLock`、`updateStatus`。另 `AlipayGatewayImpl` 回调验签留有 4 条【验签调试】info 级日志，回调高峰期污染日志。

**修复**：
- 删除上述 6 个零调用 API（删除前全仓引用复核；`IUserRepository.updateStatus` 为另一接口，未受影响）
- `AlipayGatewayImpl` 4 条验签调试日志降为 debug 级（保留排查能力，生产默认不输出）
- 文档修正：`AGENTS.md` 环境示例 `DB_NAME=s_pay_mall` → `s-pay-mall`（与实际库名一致）

**验证证据**：`mvn clean test` 单测 60/60 全绿（domain 48 + application 5 + infrastructure 7，start 跳过）；`install -DskipTests` + `mvn verify -P e2e -pl s-pay-mall-start` 14/14 全绿。

**经验**：删 API 前必须全仓引用复核（含 XML、注释外的调用方），且注意"同名不同类"陷阱（`updateStatus` 在 `IUserRepository` 与 `IOrderDao` 各有一份，只删其一）；日志降噪用级别调整而非删除，排查路径不丢。
