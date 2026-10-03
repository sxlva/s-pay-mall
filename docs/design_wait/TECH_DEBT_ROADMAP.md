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
| P0-2 | Application 层模块归属 | `OrderApplicationService` 位于 trigger 模块的 `trigger.application` 包，应与 trigger 独立 | 新建 `s-pay-mall-application` 模块，迁移 `OrderApplicationService` + `OrderTransactionService` | 待处理 |
| P0-3 | Infrastructure 层 @Transactional 违规 | `WeixinLoginGatewayImpl.createWechatUserAndBind()` 标注了 `@Transactional` | 创建 `AuthApplicationService`，将事务上移至 Application 层 | 待处理 |
| P0-4 | 幂等性设计缺失 | 所有状态变更 API 入口均无 `requestId` 幂等保护；MQ Listener 未进行 SETNX 消费幂等检查 | 状态变更 DTO 添加 `requestId` 字段；Application 层实现事务外锁操作；MQ Listener 加 `tryAcquire()` | 待处理 |
| P0-5 | Domain 层跨领域反向依赖 | `domain/order/service/PayOrderService.java` import 了 `domain.mall.gateway.IPayGateway` | 方案A: IPayGateway → `domain/shared/`；方案B: 领域事件解耦；方案C: PayOrderService → `domain/mall/` | 已处理（2026-10-03，M2-5：IPayGateway 随 mall 订单簇收编进 `domain/order/gateway`，PayOrderService 与 IPayGateway 同域，反向依赖自然消除，无需三选一） |
| P0-6 | Controller 中业务路由逻辑 | `MallAuthController` 含 `if (openId != null)` 注册策略判断；`AliPayController` 含 `"TRADE_SUCCESS".equals()` 支付状态判断 | 业务判断下沉到 Domain Service | 待处理 |
| P0-9 | 并发回调重复扣库存 | `OrderStateMachineServiceImpl.paySuccess()` 先无锁读状态（`canPay()`），且 `OrderRepositoryImpl.updateOrderStatusByOrderNo` 的 UPDATE 无 status 条件（139-145 行）：并发 notify 均读到 INIT 时全部穿过守卫，各自执行 `syncDBStockForPaySuccess` 重复扣 MySQL 库存。**2026-10-03 实测复现**（10 线程同一瞬间重放同一合法 notify，证据：`s-pay-mall-app/src/test/java/cn/fcr/test/AlipayNotifyE2ETest.java#testPayNotify_concurrentDuplicateNotify`）：10 个事务全部通过 `canPay()`，库存 100→80（扣 10×2 件）；order_main/pay_order 双写同值无业务损害，但**库存扣减不在幂等保护内**。串行重放幂等正常（同测试类场景 2），仅并发触发 | ①`updateOrderStatusByOrderNo` UPDATE 加 `AND status = 'CREATED'` 条件，以影响行数作守卫（0 行即重复回调，直接返回不再扣库存）②`syncDBStockForPaySuccess` 仅在守卫通过时执行 ③修复后移除该测试的 `@Ignore` 作为回归测试。建议纳入 JV-003 M2（与超时关单分流同批触碰状态机） | 待处理 |

### 🟡 P1 (重要级)

| ID | 问题 | 描述 | 修复路径 | 状态 |
|----|------|------|---------|------|
| P1-1 | Controller 大面积缺 @Valid | ~~11 个 Controller 仅 2 个方法使用了 `@Valid`~~（2026-10-02 核实：实际仅 1 处 `MallAdminController#saveProduct`，原记载与代码不符） | 逐个 Controller 方法加 `@Valid` + DTO 字段加校验注解 | 已处理（2026-10-02：8 个 `@RequestBody` DTO 端点补齐 `@Valid`；trigger 模块补 `spring-boot-starter-validation` 依赖——此前全项目无 JSR-303 实现，注解静默失效；`GlobalExceptionHandler` 新增 `MethodArgumentNotValidException → 0002` 映射；`CategorySaveRequestDTO.status`/`UserSaveRequestDTO.status` 的 `@NotNull` 放宽为可选以匹配前端表单；全部端点经真实请求实测） |
| P1-2 | Redis+DB 跨资源事务一致性 | `cancelOrder()` 在 `@Transactional` 内包含 `stockGateway.restoreStock()`，DB 回滚时 Redis 无法回滚 | 参照 `createOrder` 模式，将库存恢复移到事务外 | 待处理 |
| P1-3 | OrderPaidRocketListener 含业务编排 | `sendPaymentNotification()` 在 Listener 中直接实现（查订单→查微信→发模板消息） | 提取到 Application Service | 待处理 |
| P1-4 | MQ 消息发送缺超时参数 | `RocketMqOrderEventPublisher.convertAndSend` 无超时（`OrderEventGatewayImpl` 已于 2026-10-01 删除） | 添加 3000ms 超时参数 | 待处理 |
| P1-5 | Domain 层 POM 非必要技术依赖 | POM 含 `spring-context`, `spring-tx`, `alipay-sdk-java`, `jjwt`, `fastjson` → 存在误用风险 | 逐个确认实际引用，移除或替换为标准 API | 待处理 |

### 🔵 P2 (优化级)

| ID | 问题 | 描述 | 修复路径 | 状态 |
|----|------|------|---------|------|
| P2-1 | createPayOrder 缺事务保护 | `OrderApplicationService.createPayOrder()` 无 `@Transactional` | 加注事务或委托给 `OrderTransactionService` | 待处理 |
| P2-2 | 缺死信队列配置 | 3 个 RocketMQ Listener 均未配置 DLQ | 为 `order_paid`, `order-timeout-topic`, `product-stock-change-topic` 配置 DLQ | 待处理 |
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

### C.1 实际依赖方向（pom.xml 证实）

```
app → trigger → domain → types
              → api
              → infrastructure → domain
```

- `s-pay-mall-application` 模块**不存在**于磁盘上
- `OrderApplicationService` 位于 `trigger` 模块的 `cn.fcr.trigger.application` 包内
- 当前**无循环依赖**

### C.2 提取独立模块注意事项

1. 需 `OrderApplicationService` + `OrderTransactionService` **一起移出**，否则产生 `trigger ↔ application` 循环
2. `s-pay-mall-trigger/pom.xml` → 添加依赖 `s-pay-mall-application`
3. `s-pay-mall-app/pom.xml` → 添加依赖 `s-pay-mall-application`
4. 4 个子模块（trigger/domain/infrastructure/types）均配置了 `maven-archetype-plugin`，可能干扰正常打包

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
| P0-2 (独立 Application 模块) | 功能可用，仅架构不干净；当前无循环依赖 | 有时间做结构性重构时 |
| P0-3 (WeixinLoginGatewayImpl @Transactional) | 跟随 P0-2 一并解决，减少迁移次数 | P0-2 完成后立即跟进 |
| P0-4 (幂等性设计) | 需前端配合改造请求参数；当前状态机提供了部分防护 | 前端排期支持 |
| P0-5 (Domain 跨模块依赖) | 需仔细分析影响范围，选方案三选一 | 有时间分析时 |

---

## 十一、附录 F：文档与代码不一致清单（2026-08-23 审计新增）

> 以下为规则体系重构审计中发现的文档与代码不一致项。规则文档已修正为以代码为准，此处登记供回溯参考。

| ID | 问题 | 文档声称 | 代码实际 | 代码位置 | 处置 |
|----|------|---------|---------|---------|------|
| DOC-1 | DomainServiceConfig @Bean 数量过时 | "12 个 @Bean"（DEVELOPMENT_GUIDE §2.4 + docs/design/README §三 原文） | **17 个 @Bean** | [DomainServiceConfig.java](../../s-pay-mall-infrastructure/src/main/java/cn/fcr/infrastructure/config/shared/DomainServiceConfig.java) | 新 SSOT 不写死数量，改为引用代码 |
| DOC-2 | Redis 幂等 Key 格式不一致 | `mall:stock:msg:processed:{messageId}`（DEVELOPMENT_GUIDE §五 原文） | `stock:event:{businessType}:{businessNo}` | [IdempotentGatewayImpl.java](../../s-pay-mall-infrastructure/src/main/java/cn/fcr/infrastructure/mall/gateway/IdempotentGatewayImpl.java) | 新 SSOT 以代码为准，标注差异 |
| DOC-3 | ~~AdminApiController 路径描述不精确~~ | ~~"与 MallAdminController 重复路径结构"（DEVELOPMENT_GUIDE §1.6 原文）~~ | ~~前缀不同（`/pay-api/.../admin` vs `/mall-api/.../admin`），功能 CRUD 重复~~ | ~~[MallAdminController.java](../../s-pay-mall-trigger/src/main/java/cn/fcr/trigger/http/mall/MallAdminController.java) / [AdminApiController.java](../../s-pay-mall-trigger/src/main/java/cn/fcr/trigger/http/mall/AdminApiController.java)~~ | 已处理（2026-10-01，JV-003 第二批：删除 `AdminApiController` + SecurityConfig 死规则 + 契约 §4.2 移除） |
| DOC-4 | REVIEW.md 悬空引用 | 引用 `CLAUDE_project_guide_v2.md`（3 处） | 该文件不存在 | [REVIEW.md](../../REVIEW.md) 原 §1.1/§7.4.4/§三 | REVIEW.md v3.0 已修复，改为引用 DDD_ARCHITECTURE_SPEC.md |
