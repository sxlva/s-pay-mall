# 架构遗留项记录 (Architecture Debt)

本文档由 DDD 架构重构过程中识别，记录已知的架构遗留问题。每项包含背景原因、当前临时方案、以及彻底解决需要的步骤。

---

## 1. Application 层模块归属问题 (P0-2)

### 背景原因

在 DDD 分层架构中，Application 层应独立于 Trigger（接口触发）层和 Infrastructure（基础设施）层。理想状态下，`OrderApplicationService` 应位于独立的 Application 层模块中。

### 当前临时方案

`OrderApplicationService`（11 个方法）目前位于 `s-pay-mall-trigger` 模块的 `cn.fcr.trigger.application` 包下，与 Controller、Listener、Job 等 Trigger 层组件共享同一个 Maven 模块。

### 曾尝试的迁移及失败原因

曾尝试将 Application Service 迁移到 `s-pay-mall-app` 模块（`cn.fcr.app.application` 包），但因为 `s-pay-mall-app` 是 Spring Boot 启动模块：

- App 模块依赖 Trigger 模块（`App → Trigger`，用于 `@SpringBootApplication` 组件扫描和 fat JAR 打包）
- Trigger 模块如需注入 App 模块的类，需 `Trigger → App`
- 这形成 `Trigger → App → Trigger` 循环依赖，Maven 无法构建
- 即使移除 `App → Trigger`（仅保留运行时类路径扫描），fat JAR 将丢失全部 Trigger 层的 Controller/Listener/Job 类

### 彻底解决步骤

1. 新建独立模块 `s-pay-mall-application`，仅包含 Application Service 类及所需依赖
2. 修改 `s-pay-mall-trigger/pom.xml`：添加对 `s-pay-mall-application` 的依赖
3. 修改 `s-pay-mall-app/pom.xml`：添加对 `s-pay-mall-application` 的依赖
4. 将 `OrderApplicationService` 从 `s-pay-mall-trigger` 迁移到新模块，包名改为 `cn.fcr.application`
5. 更新所有 import 引用（MallOrderController、MallAdminController、AdminApiController、OrderPaidRocketListener 等）
6. 验证：`mvn clean package` + fat JAR 内容检查

依赖关系变为：
```
app ──→ application ←── trigger
 │                          │
 └──→ domain ←──┘
         ↑
   infrastructure
```

---

## 2. 旧 Order Domain 与新 Mall Domain 并存 ✅ 已完成

### 背景原因

项目存在两套并行的订单系统：

| | 旧 Order Domain | 新 Mall Domain |
|---|---|---|
| 包路径 | `cn.fcr.domain.order` | `cn.fcr.domain.mall` |
| 服务接口 | `IOrderService` | `IMallOrderService` |
| 仓储 | `IOrderRepository` | `IMallOrderQueryGateway` |
| 订单模型 | 单商品订单（`ShopCartEntity`） | 多商品购物车订单（`CartItemVO`） |
| 使用方 | AliPayController、AliPayReturnController、NoPayNotifyOrderJob、TimeoutCloseOrderJob、OrderTimeoutCloseRocketListener | MallOrderController |

### 完成内容（2026-06-21）

已在 `OrderApplicationService` 中新增 8 个旧 Order Domain 包装方法，并将以下 5 个 Trigger 层文件全部改为注入 `OrderApplicationService`：

| 文件 | 原注入 | 改后 |
|------|--------|------|
| `AliPayController.java` | `IOrderService` + `PayOrderService` | `OrderApplicationService` |
| `AliPayReturnController.java` | `IMallOrderService` | `OrderApplicationService` |
| `NoPayNotifyOrderJob.java` | `IOrderService` | `OrderApplicationService` |
| `TimeoutCloseOrderJob.java` | `IOrderService` | `OrderApplicationService` |
| `OrderTimeoutCloseRocketListener.java` | `IOrderService` | `OrderApplicationService` |

`OrderApplicationService.createPayOrder` 签名根据调用方实际数据类型调整为 `createPayOrder(String userId, String productId)`，全程保持 String 类型，不引入额外类型转换风险。

两个域的 `OrderEntity` 类各自独立（`cn.fcr.domain.order.model.entity.OrderEntity` 和 `cn.fcr.domain.mall.model.entity.OrderEntity`），互不干扰。

---

## 3. 事件发布职责剥离 ✅ 已完成

### 背景原因

`OrderRepository.changeOrderPaySuccess()`（位于 `s-pay-mall-infrastructure/.../order/repository/OrderRepository.java`）原先在更新数据库状态之后直接调用了 `orderEventPublisher.publishPaySuccess()`。这是基础设施层在主动编排领域事件发布，违反了分层职责——事件发布的"何时发"应由 Application 层决策。

### 完成内容（2026-06-21）

1. 从 `OrderRepository.changeOrderPaySuccess()` 中移除了 `orderEventPublisher.publishPaySuccess()` 调用，仅保留 DB 状态更新
2. 移除了 `OrderRepository` 中对 `IOrderEventPublisher` 的依赖注入（字段 + import）
3. 在 `OrderApplicationService.changeOrderPaySuccess()` 中编排 DB 更新 + 事件发布：

   ```java
   @Transactional(rollbackFor = Exception.class)
   public void changeOrderPaySuccess(String orderId) {
       orderService.changeOrderPaySuccess(orderId);   // DB 更新
       orderEventPublisher.publishPaySuccess(orderId, orderId);  // 事件发布
   }
   ```

4. `AliPayController.payNotify()` 和 `NoPayNotifyOrderJob.exec()` 已通过旧 Order Domain 迁移改为调用 `OrderApplicationService.changeOrderPaySuccess()`

事件发布的原子性（DB 事务提交成功但消息发布失败）不在修复范围，作为已知的可接受技术限制。

---

---

## 4. MallAdminController / AdminApiController 仍直接注入 IMallOrderService (P2)

### 背景原因

全局搜索发现 `MallAdminController.java` 和 `AdminApiController.java` 仍直接注入 `IMallOrderService`，未经过 `OrderApplicationService` 层。

### 当前临时方案

这两个 Controller 属于 Mall 管理后台链路（商品管理、用户管理、统计管理），与本次完成的订单/支付链路（Mall Domain 购物车 + 旧 Order Domain 支付宝单品）相互独立。涉及的 `IMallOrderService` 方法（管理端订单查询/统计等）与已收口的 `getOrderByNo` / `paySuccess` 不同。

### 建议处理方式

作为未来可选的 7-1c 任务：
1. 在 `OrderApplicationService` 中补充管理后台所需的 `IMallOrderService` 包装方法
2. 将 `MallAdminController` 和 `AdminApiController` 的注入改为 `OrderApplicationService`

---

## 5. 观察：Trigger 层完整 Domain 收口扫描

### 观察记录

`ProductStockChangeRocketListener` 注入 `StockChangeHandler`（mall 域库存变更处理器），本次未纳入排查范围。当前全局搜索仅覆盖 `IOrderService` / `PayOrderService` / `IMallOrderService` 三个接口，后续做 Trigger 层完整收尾时需要重新全局扫描所有 `@Resource` 注入点，不局限于这三个接口。

---

> **本文档由 2026-06-21 DDD 架构重构过程中识别并记录。**
> 当前版本已通过完整的 `mvn clean package -DskipTests` + fat JAR 内容验证。
> 供后续有时间时参考升级。
