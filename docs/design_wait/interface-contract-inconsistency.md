# 前后端接口契约不一致问题记录

## 问题类别

前后端接口契约不一致（Single Source of Truth 违反）

## 问题描述

前端 `StockCheckResult` 接口类型定义与后端实际返回的 `StockCheckRespDTO` 存在字段差异。前端私自添加了后端未定义的 `stockStatus` 字段，违反了 DDD 架构一致性原则。

## 具体差异

| 字段 | 后端定义 (`StockCheckRespDTO`) | 前端定义 (`StockCheckResult`) | 一致性 |
|------|-------------------------------|------------------------------|--------|
| `success` | `Boolean` | `boolean` | ✅ 一致 |
| `message` | `String` | `string?` | ✅ 基本一致 |
| `stockStatus` | ❌ 不存在 | ✅ 存在（前端独有） | ❌ **不一致** |

## 涉及文件

### 后端
- [StockCheckRespDTO.java](../../s-pay-mall-api/src/main/java/cn/fcr/api/dto/StockCheckRespDTO.java) — 后端响应 DTO 定义
- [MallOrderController.java](../../s-pay-mall-trigger/src/main/java/cn/fcr/trigger/http/mall/MallOrderController.java#L186-L199) — 库存检查接口实现

### 前端
- [order.ts](../../s-pay-mall-front/src/api/order.ts#L10-L19) — 前端类型定义（包含多余字段）
- [OrderListPage.vue](../../s-pay-mall-front/src/views/OrderListPage.vue#L171-L176) — 前端使用位置（仅使用 `success` 和 `message`）

## 违反规范

1. **Single Source of Truth 原则**：前端私自添加了后端未定义的字段，导致接口契约不一致
2. **DDD 架构一致性**：前后端接口契约应保持严格一致，前端不应自行扩展后端未支持的字段

## 当前影响

- 前端定义的 `stockStatus` 字段实际未被使用
- 若未来有人依赖该字段做逻辑判断，会导致运行时数据缺失（字段始终为 `undefined`）
- 增加维护成本，开发者需额外确认哪些字段是有效可用的

## 建议处理方案

### 方案1：删除前端多余字段（推荐）

删除前端 `stockStatus` 字段，保持与后端一致。符合当前业务需求，无需修改后端。

```typescript
// 修改前
export interface StockCheckResult {
  success: boolean;
  message?: string;
  stockStatus?: Record<number, { available: number; required: number }>; // 删除此行
}

// 修改后
export interface StockCheckResult {
  success: boolean;
  message?: string;
}
```

### 方案2：后端扩展接口（如需详细库存信息）

若业务需要返回详细的库存状态信息，应遵循以下流程：

1. 在后端 `StockCheckRespDTO` 添加 `stockStatus` 字段
2. 更新业务逻辑填充数据（`OrderApplicationService.checkStock()`）
3. 前端同步使用该字段

## 预防措施

1. **接口契约优先**：修改接口时，先在后端定义 DTO，再同步更新前端类型
2. **代码审查**：PR 审查时检查前后端字段一致性
3. **自动化测试**：增加接口响应字段校验测试

---

*记录日期：2026-07-02*
*发现方式：代码审查 + 手动比对*
