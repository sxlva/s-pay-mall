# s-pay-mall 系统架构问题总结

## 审计时间
2026-07-02

## 审计背景
通过代码审查发现项目中存在新旧两套订单领域模型并存的问题，导致代码冗余、维护困难和潜在的逻辑不一致。

---

## 一、核心架构问题：新旧订单系统并存

项目中存在两套订单领域模型，分别位于 `order` 包和 `mall` 包，导致代码冗余和维护困难。

| 维度 | order 包（旧系统） | mall 包（新系统） |
|------|-------------------|------------------|
| 业务场景 | 单商品直接购买 | 多商品购物车 |
| 状态枚举 | `OrderStatusVO` | `OrderState` |
| 订单实体 | `OrderEntity`（单商品） | `OrderEntity`（多商品+items） |
| 仓储接口 | `IOrderRepository` | `IMallOrderQueryGateway` |
| 状态标记 | `@deprecated` | 当前主流程 |

---

## 二、具体问题清单

### 2.1 废弃代码仍在生产环境运行

**问题描述**：`AbstractOrderService` 已标记 `@deprecated`，但三个关键方法仍被生产环境调用。

| 方法 | 调用位置 | 用途 |
|------|---------|------|
| `handleTimeoutCloseOrder()` | `OrderTimeoutCloseRocketListener.onMessage()` | 处理超时关单 MQ 消息 |
| `changeOrderPaySuccess()` | `OrderTransactionService.changeOrderPaySuccessInTransaction()` | 支付成功状态更新 |
| `queryNoPayNotifyOrder()` | `OrderApplicationService.queryNoPayNotifyOrder()` | 查询未收到回调的订单（补偿） |

**涉及文件**：
- [AbstractOrderService.java](file:///Users/xiaolv/Develop/projects/backend/java/s-pay-mall/s-pay-mall-domain/src/main/java/cn/fcr/domain/order/service/AbstractOrderService.java#L22)
- [OrderTimeoutCloseRocketListener.java](file:///Users/xiaolv/Develop/projects/backend/java/s-pay-mall/s-pay-mall-trigger/src/main/java/cn/fcr/trigger/listener/OrderTimeoutCloseRocketListener.java#L38)
- [OrderTransactionService.java](file:///Users/xiaolv/Develop/projects/backend/java/s-pay-mall/s-pay-mall-trigger/src/main/java/cn/fcr/trigger/application/OrderTransactionService.java#L93)

### 2.2 领域模型重复

**问题描述**：两个包各有一套 `OrderEntity`，字段定义、状态机逻辑完全不同。

**旧系统 OrderEntity** (`order` 包)：
- 单商品模式：`productId`, `productName`, `payUrl`
- 状态值对象：`OrderStatusVO`
- 时间类型：`Date`

**新系统 OrderEntity** (`mall` 包)：
- 多商品模式：`List<OrderItemEntity> items`, `address`
- 状态枚举：`OrderState`
- 时间类型：`LocalDateTime`

**涉及文件**：
- [order/OrderEntity.java](file:///Users/xiaolv/Develop/projects/backend/java/s-pay-mall/s-pay-mall-domain/src/main/java/cn/fcr/domain/order/model/entity/OrderEntity.java)
- [mall/OrderEntity.java](file:///Users/xiaolv/Develop/projects/backend/java/s-pay-mall/s-pay-mall-domain/src/main/java/cn/fcr/domain/mall/model/entity/OrderEntity.java)

### 2.3 状态枚举不一致

**问题描述**：旧系统用 `OrderStatusVO`，新系统用 `OrderState`，状态码和含义不完全对应。

| 旧系统 (OrderStatusVO) | 新系统 (OrderState) | 对应关系 |
|------------------------|---------------------|---------|
| CREATE | INIT | 对应 |
| PAY_WAIT | INIT | 部分重叠 |
| PAY_SUCCESS | PAID | 对应 |
| DEAL_DONE | SHIPPED/DONE | 部分重叠 |
| CLOSE | CANCELED | 对应 |

**涉及文件**：
- [OrderStatusVO.java](file:///Users/xiaolv/Develop/projects/backend/java/s-pay-mall/s-pay-mall-domain/src/main/java/cn/fcr/domain/order/model/valobj/OrderStatusVO.java)
- [OrderState.java](file:///Users/xiaolv/Develop/projects/backend/java/s-pay-mall/s-pay-mall-domain/src/main/java/cn/fcr/domain/mall/model/entity/OrderState.java)

### 2.4 购物车模型差异

**问题描述**：旧系统 `ShopCartEntity` 只支持单商品，新系统 `CartItemVO` 支持多商品。

**涉及文件**：
- [ShopCartEntity.java](file:///Users/xiaolv/Develop/projects/backend/java/s-pay-mall/s-pay-mall-domain/src/main/java/cn/fcr/domain/order/model/entity/ShopCartEntity.java)
- [CartItemVO.java](file:///Users/xiaolv/Develop/projects/backend/java/s-pay-mall/s-pay-mall-domain/src/main/java/cn/fcr/domain/mall/model/valobj/CartItemVO.java)

### 2.5 超时关单逻辑分散

**问题描述**：`handleTimeoutCloseOrder` 在旧系统实现，新系统的状态机服务缺少此功能。

**涉及文件**：
- [OrderTimeoutCloseRocketListener.java](file:///Users/xiaolv/Develop/projects/backend/java/s-pay-mall/s-pay-mall-trigger/src/main/java/cn/fcr/trigger/listener/OrderTimeoutCloseRocketListener.java)

### 2.6 支付成功处理重复

**问题描述**：旧系统 `changeOrderPaySuccess` 和新系统 `paySuccess` 并存，存在重复逻辑。

**涉及文件**：
- [OrderTransactionService.java](file:///Users/xiaolv/Develop/projects/backend/java/s-pay-mall/s-pay-mall-trigger/src/main/java/cn/fcr/trigger/application/OrderTransactionService.java#L93)
- [MallOrderServiceImpl.java](file:///Users/xiaolv/Develop/projects/backend/java/s-pay-mall/s-pay-mall-domain/src/main/java/cn/fcr/domain/mall/service/impl/MallOrderServiceImpl.java#L116)

### 2.7 仓储接口冗余

**问题描述**：`IOrderRepository` 和 `IMallOrderQueryGateway` 功能重叠。

**涉及文件**：
- [IOrderRepository.java](file:///Users/xiaolv/Develop/projects/backend/java/s-pay-mall/s-pay-mall-domain/src/main/java/cn/fcr/domain/order/adapter/repository/IOrderRepository.java)
- [IMallOrderQueryGateway.java](file:///Users/xiaolv/Develop/projects/backend/java/s-pay-mall/s-pay-mall-domain/src/main/java/cn/fcr/domain/mall/gateway/IMallOrderQueryGateway.java)

---

## 三、依赖关系图（旧系统仍被调用的链路）

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

---

## 四、建议修复优先级

| 优先级 | 修复项 | 说明 |
|--------|-------|------|
| **高** | 将 `handleTimeoutCloseOrder` 迁移到新状态机服务 | 生产环境核心功能 |
| **高** | 统一支付成功处理到 `MallOrderServiceImpl.paySuccess()` | 生产环境核心功能 |
| **中** | 删除旧系统的 `OrderEntity`、`OrderStatusVO`、`ShopCartEntity` | 消除模型重复 |
| **中** | 合并仓储接口，清理 `IOrderRepository` | 消除接口冗余 |
| **低** | 删除废弃的 `AbstractOrderService` 和 `OrderService` | 清理废弃服务 |

---

## 五、关键风险点

1. **补偿查询依赖**：旧系统的 `queryNoPayNotifyOrder()` 用于补偿查询，需确认新系统是否有替代方案
2. **跨域共享接口**：`IOrderEventPublisher` 是跨域共享接口，需要保留或迁移到合适位置
3. **数据库表映射**：两套系统可能对应不同的数据库表结构，迁移时需确认数据兼容性

---

## 六、order 包文件处理建议

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

## 七、备注

本文档基于 2026-07-02 代码审查结果，后续修复需根据实际执行情况更新。