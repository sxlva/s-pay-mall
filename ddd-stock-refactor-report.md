# 库存逻辑 DDD 重构报告

## 1. 结构说明

### DDD 分层架构

| 层级 | 模块 | 文件 | 职责说明 |
|------|------|------|----------|
| **Trigger 层** | s-pay-mall-trigger | [ProductStockChangeRocketListener.java](file:///Users/xiaolv/Develop/projects/backend/java/s-pay-mall/s-pay-mall-trigger/src/main/java/cn/fcr/trigger/listener/ProductStockChangeRocketListener.java) | MQ 消息监听与策略路由，不包含业务逻辑 |
| **Domain 层** | s-pay-mall-domain | [StockChangeHandler.java](file:///Users/xiaolv/Develop/projects/backend/java/s-pay-mall/s-pay-mall-domain/src/main/java/cn/fcr/domain/mall/service/StockChangeHandler.java) | 策略接口定义 |
| **Domain 层** | s-pay-mall-domain | [DeductHandler.java](file:///Users/xiaolv/Develop/projects/backend/java/s-pay-mall/s-pay-mall-domain/src/main/java/cn/fcr/domain/mall/service/handler/DeductHandler.java) | 扣减库存策略实现 |
| **Domain 层** | s-pay-mall-domain | [RestoreHandler.java](file:///Users/xiaolv/Develop/projects/backend/java/s-pay-mall/s-pay-mall-domain/src/main/java/cn/fcr/domain/mall/service/handler/RestoreHandler.java) | 恢复库存策略实现 |
| **Domain 层** | s-pay-mall-domain | [AdminUpdateHandler.java](file:///Users/xiaolv/Develop/projects/backend/java/s-pay-mall/s-pay-mall-domain/src/main/java/cn/fcr/domain/mall/service/handler/AdminUpdateHandler.java) | 管理员更新策略实现 |
| **Domain 层** | s-pay-mall-domain | [IIdempotentGateway.java](file:///Users/xiaolv/Develop/projects/backend/java/s-pay-mall/s-pay-mall-domain/src/main/java/cn/fcr/domain/mall/gateway/IIdempotentGateway.java) | 幂等性网关接口（Domain 定义） |
| **Domain 层** | s-pay-mall-domain | [IStockGateway.java](file:///Users/xiaolv/Develop/projects/backend/java/s-pay-mall/s-pay-mall-domain/src/main/java/cn/fcr/domain/mall/gateway/IStockGateway.java) | 库存网关接口（Domain 定义） |
| **Domain 层** | s-pay-mall-domain | [StockChangeMsg.java](file:///Users/xiaolv/Develop/projects/backend/java/s-pay-mall/s-pay-mall-domain/src/main/java/cn/fcr/domain/mall/model/dto/StockChangeMsg.java) | 内部消息 DTO |
| **Domain 层** | s-pay-mall-domain | [StockChangeMessageDTO.java](file:///Users/xiaolv/Develop/projects/backend/java/s-pay-mall/s-pay-mall-domain/src/main/java/cn/fcr/domain/mall/model/dto/StockChangeMessageDTO.java) | MQ 消息 DTO |
| **Infrastructure 层** | s-pay-mall-infrastructure | [IdempotentGatewayImpl.java](file:///Users/xiaolv/Develop/projects/backend/java/s-pay-mall/s-pay-mall-infrastructure/src/main/java/cn/fcr/infrastructure/gateway/IdempotentGatewayImpl.java) | 幂等性检查 Redis 实现 |
| **Infrastructure 层** | s-pay-mall-infrastructure | [StockGatewayImpl.java](file:///Users/xiaolv/Develop/projects/backend/java/s-pay-mall/s-pay-mall-infrastructure/src/main/java/cn/fcr/infrastructure/gateway/StockGatewayImpl.java) | 库存操作 Redis/DB 实现 |

### 依赖方向

```
Trigger层 (ProductStockChangeRocketListener)
    ↓
Domain层 (StockChangeHandler, IIdempotentGateway, IStockGateway)
    ↓
Infrastructure层 (IdempotentGatewayImpl, StockGatewayImpl)
```

**严格遵守 DDD 依赖规则**：依赖方向单向，上层依赖下层，下层不依赖上层。

---

## 2. 策略模式映射表

| 变更类型 (Tag) | Handler 实现类 | 业务场景 | 库存操作类型 |
|---------------|---------------|----------|-------------|
| `PAY_DEDUCT` | [DeductHandler](file:///Users/xiaolv/Develop/projects/backend/java/s-pay-mall/s-pay-mall-domain/src/main/java/cn/fcr/domain/mall/service/handler/DeductHandler.java) | 支付成功扣减库存 | 原子递减 |
| `ORDER_RESTORE` | [RestoreHandler](file:///Users/xiaolv/Develop/projects/backend/java/s-pay-mall/s-pay-mall-domain/src/main/java/cn/fcr/domain/mall/service/handler/RestoreHandler.java) | 订单取消恢复库存 | 原子递增 |
| `ADMIN_UPDATE` | [AdminUpdateHandler](file:///Users/xiaolv/Develop/projects/backend/java/s-pay-mall/s-pay-mall-domain/src/main/java/cn/fcr/domain/mall/service/handler/AdminUpdateHandler.java) | 管理员修改库存 | 全量设置 |

### 策略路由机制

```java
// ProductStockChangeRocketListener.findHandler()
private StockChangeHandler findHandler(String changeType) {
    for (StockChangeHandler handler : stockChangeHandlers) {
        if (handler.supports(changeType)) {
            return handler;
        }
    }
    return null;
}
```

**设计优势**：
- 新增变更类型只需添加新 Handler，无需修改监听器（开闭原则）
- Spring 自动注入所有 `StockChangeHandler` 实现到 List
- 通过 `supports()` 方法实现策略的动态匹配

---

## 3. 幂等性实现路径

### 幂等 Key 规范

| 业务类型 | 业务单号来源 | 幂等 Key 格式 |
|----------|-------------|--------------|
| `deduct` | orderId | `stock:event:deduct:{orderId}` |
| `restore` | orderId | `stock:event:restore:{orderId}` |
| `admin_update` | updateRecordId | `stock:event:admin_update:{updateRecordId}` |

### 执行语义

```
┌─────────────────────────────────────────────────────────────┐
│                    收到 MQ 消息                              │
└─────────────────────────────────────────────────────────────┘
                            ↓
┌─────────────────────────────────────────────────────────────┐
│  tryAcquire(businessType, businessNo)                       │
│  Redis.setIfAbsent(key, "PROCESSING", 24h)                 │
└─────────────────────────────────────────────────────────────┘
                            ↓
              ┌──────────────┴──────────────┐
              ↓                              ↓
    ┌──────────────────┐          ┌──────────────────┐
    │ 成功 (acquired)  │          │ 失败 (!acquired) │
    │   分支 A         │          │   分支 B         │
    └──────────────────┘          └──────────────────┘
              ↓                              ↓
    ┌──────────────────┐          ┌──────────────────┐
    │ 执行业务逻辑      │          │ 跳过执行         │
    │ (扣减/恢复/更新) │          │ 返回当前库存     │
    └──────────────────┘          └──────────────────┘
              ↓
    ┌──────────────────┐
    │ 成功：不删除 Key │
    │ (24h 自动过期)   │
    └──────────────────┘
              ↓
    ┌──────────────────┐
    │ 异常：release()  │
    │ 删除 Key         │
    │ 允许重试消费     │
    └──────────────────┘
```

### 关键代码实现

**幂等锁获取** ([IdempotentGatewayImpl.java](file:///Users/xiaolv/Develop/projects/backend/java/s-pay-mall/s-pay-mall-infrastructure/src/main/java/cn/fcr/infrastructure/gateway/IdempotentGatewayImpl.java)):

```java
@Override
public boolean tryAcquire(String businessType, String businessNo) {
    String idempotentKey = buildKey(businessType, businessNo);
    RBucket<String> bucket = redissonClient.getBucket(idempotentKey);
    
    // 使用 trySet 实现 SETNX
    boolean acquired = bucket.trySet("PROCESSING", 24, TimeUnit.HOURS);
    return acquired;
}
```

**异常处理示例** ([DeductHandler.java](file:///Users/xiaolv/Develop/projects/backend/java/s-pay-mall/s-pay-mall-domain/src/main/java/cn/fcr/domain/mall/service/handler/DeductHandler.java)):

```java
try {
    long remainingStock = stockGateway.deductStock(productId, quantity);
    return remainingStock;  // 成功不删除 Key
} catch (Exception e) {
    idempotentGateway.release(BUSINESS_TYPE_DEDUCT, businessNo);  // 异常释放锁
    throw new RuntimeException("扣减库存失败", e);
}
```

---

## 4. 代码依赖分析

### 依赖关系矩阵

| 模块 | 依赖的模块 | 是否符合 DDD |
|------|-----------|-------------|
| ProductStockChangeRocketListener | domain (StockChangeHandler, StockChangeMsg) | ✓ 符合 |
| DeductHandler | domain (IIdempotentGateway, IStockGateway) | ✓ 符合 |
| RestoreHandler | domain (IIdempotentGateway, IStockGateway) | ✓ 符合 |
| AdminUpdateHandler | domain (IIdempotentGateway, IStockGateway) | ✓ 符合 |
| IdempotentGatewayImpl | domain (IIdempotentGateway) | ✓ 符合 |
| StockGatewayImpl | domain (IStockGateway) | ✓ 符合 |

### 循环依赖检查

**检查结果**：无循环依赖

```
Trigger → Domain → Infrastructure
     ↑              │
     └──────────────┘ （无反向依赖）
```

### 基础设施层污染检查

**检查结果**：基础设施层未污染领域层

| 检查项 | 状态 | 说明 |
|--------|------|------|
| Infrastructure 层是否调用 Domain 层业务逻辑 | ✗ 否 | 仅实现接口，不调用业务服务 |
| Domain 层是否包含 Redis/DB 实现代码 | ✗ 否 | 仅定义接口契约 |
| Handler 是否包含技术实现细节 | ✗ 否 | 仅调用 Gateway 接口 |

### 设计优点

1. **策略模式解耦**：业务逻辑与消息路由完全分离
2. **接口隔离**：Domain 层定义接口，Infrastructure 层实现
3. **幂等闭环**：完整的幂等性检查和异常恢复机制
4. **可扩展性**：新增库存变更类型只需添加新 Handler
5. **单一职责**：每个 Handler 只处理一种业务场景

---

## 5. 代码规范检查

### 命名规范

| 类型 | 规范 | 示例 |
|------|------|------|
| 接口 | 动词+名词+Gateway | `IIdempotentGateway` |
| 实现类 | 接口名+Impl | `IdempotentGatewayImpl` |
| Handler | 业务场景+Handler | `DeductHandler` |
| 常量 | 大写下划线 | `BUSINESS_TYPE_DEDUCT` |

### 日志规范

所有日志必须包含：
- `productId`: 商品标识
- `businessNo`: 业务单号（幂等 Key 组成部分）
- `messageId`: 消息唯一标识（追踪用）

---

## 6. 总结

### 重构成果

| 指标 | 重构前 | 重构后 |
|------|--------|--------|
| Handler 数量 | 1（耦合所有逻辑） | 3（单一职责） |
| 幂等 Key | `messageId` | `businessType:businessNo` |
| 异常处理 | 无恢复机制 | release() 删除 Key 允许重试 |
| 可扩展性 | 修改原有代码 | 新增 Handler 即可 |
| DDD 分层 | 混合 | 清晰分层 |

### 后续优化建议

1. **消息格式校验**：在监听器层添加消息字段校验
2. **监控指标**：添加幂等锁获取成功率、重试次数等监控
3. **告警机制**：当幂等锁获取失败次数超过阈值时告警
4. **死信队列**：处理无法匹配 Handler 的消息
5. **单元测试**：为每个 Handler 添加幂等性测试用例