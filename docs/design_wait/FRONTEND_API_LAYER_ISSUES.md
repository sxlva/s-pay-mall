# 前端 API 层问题记录（2026-07-02）

> **问题来源**：用户质疑 `admin.ts` 和 `order.ts` 文件是否过于混乱
> **涉及文件**：`src/api/admin.ts`、`src/api/order.ts`

---

## 一、文件结构问题

### 1.1 admin.ts 过于臃肿（294行）

**问题**：一个文件包含 **5 个完全独立的业务模块**，违反单一职责原则：

| 业务模块 | API 函数数 | 行范围 |
|---------|-----------|--------|
| 用户管理 | 5 | L11-L52 |
| 分类管理 | 3 | L54-L96 |
| 商品管理 | 3 | L98-L149 |
| 订单管理 | 5 | L151-L253 |
| 统计数据 | 2 | L255-L294 |

**影响**：
- 修改一个模块可能影响其他模块的稳定性
- 文件过大导致代码检索效率降低
- 多人协作时冲突概率增加

### 1.2 类型定义与 API 函数混放

**问题**：`admin.ts` 中定义了 **7 个类型接口**，与 API 函数混合在一起：

| 类型名称 | 用途 |
|---------|------|
| `CategoryVO` | 分类值对象（后台管理） |
| `ProductAdminVO` | 商品值对象（后台管理） |
| `OrderAdminVO` | 订单值对象（后台管理） |
| `OrderItemVO` | 订单商品项（后台管理） |
| `OrderQueryParams` | 订单查询参数（后台管理） |
| `SalesTrendVO` | 销售额走势数据 |
| `CategoryRatioVO` | 分类销售占比数据 |

`order.ts` 中定义了 `StockCheckResult`。

**影响**：
- 类型定义分散，不便于集中管理和复用
- 与后端 API 契约层职责不清
- 类型变更影响范围难以追踪

---

## 二、架构层次问题（根因）

> **引用来源**：[audit-20260702.md](file:///Users/xiaolv/Develop/projects/backend/java/s-pay-mall/docs/design_wait/audit-20260702.md) 第七节

### 2.1 API 调用层次重叠（三层并存）

**问题**：前端存在三个职责重叠的 API 封装层，导致代码重复、维护困难：

```
src/api/          → admin.ts + order.ts（Axios 封装）
src/repositories/ → 3 个 repository（cartRepository 用 bare fetch，其余用 Axios）
src/services/     → adminUserService.ts（重复封装 admin.ts）
```

**重叠详情**：

| 层级 | 文件 | 职责 | 问题 |
|------|------|------|------|
| `api/` | `admin.ts` | 管理端 CRUD API | 过于臃肿（294行） |
| `api/` | `order.ts` | 订单相关 API | 与 `orderRepository` 重叠 |
| `repositories/` | `orderRepository.ts` | 重复封装 + 补充订单端点 | 与 `api/order.ts` 重复 |
| `repositories/` | `cartRepository.ts` | 购物车端点（bare fetch） | 技术栈不一致 |
| `repositories/` | `productRepository.ts` | 商品端点 | 补充 `api/` 层未覆盖的端点 |
| `services/` | `adminUserService.ts` | 重复封装用户管理 API | 与 `api/admin.ts` 重复 |

**影响**：
- 新增端点时不确定该放在哪一层
- 同一 API 可能被多次封装，维护时需要修改多个文件
- `cartRepository` 使用 bare `fetch`，与其他层的 Axios 技术栈不一致
- 代码重复导致体积增大，测试覆盖困难

### 2.2 统一 API 工具未被使用

**问题**：`src/utils/api.ts` 提供了通用 request 工具，但 `api/` 层完全未使用。

**影响**：
- 工具层与业务层脱节
- 无法统一处理请求拦截、错误处理、日志等通用逻辑

---

## 三、代码质量问题

### 3.1 存在调试代码

**问题**：`order.ts` 第 26 行保留了调试日志：

```typescript
console.log('【订单查询参数】', params);
```

**影响**：
- 生产代码不应包含调试日志
- 可能泄露敏感信息到控制台
- 增加不必要的日志输出

### 3.2 命名不一致

**问题**：同一业务概念在不同文件中命名不统一：

| 业务概念 | admin.ts | order.ts | 问题 |
|---------|----------|----------|------|
| 订单类型 | `OrderAdminVO` | `Order` | 后缀不一致 |
| 订单项 | `OrderItemVO` | 未定义 | `order.ts` 缺少对应类型 |

**影响**：
- 开发者难以快速理解类型含义
- 跨模块传递数据时类型不兼容
- 增加代码理解成本

---

## 四、重构建议

### 4.1 合并 API 调用层次（核心重构）

**目标**：将 `api/`、`repositories/`、`services/` 三层合并为统一的 `api/` 层，消除职责重叠。

**重构后结构**：

```
src/api/
├── admin/                    # 后台管理 API（拆分后）
│   ├── index.ts              # 统一导出
│   ├── user.ts
│   ├── category.ts
│   ├── product.ts
│   ├── order.ts
│   └── statistics.ts
├── mall/                     # 商城端 API（合并 repositories/ 层）
│   ├── index.ts
│   ├── order.ts              # 合并 api/order.ts + repositories/orderRepository.ts
│   ├── cart.ts               # 迁移 cartRepository.ts，改用 Axios
│   └── product.ts            # 迁移 productRepository.ts
└── common.ts                 # 通用 API 工具（迁移 utils/api.ts）
```

**迁移策略**：
1. 将 `repositories/` 和 `services/` 层的 API 函数迁移到 `api/` 层对应的子模块
2. `cartRepository.ts` 的 bare `fetch` 改为使用统一的 Axios 实例
3. 删除原 `repositories/` 和 `services/` 目录
4. 更新所有组件/Hook 中的 import 引用

### 4.2 提取类型到 types/ 目录

```
src/types/
├── admin/                    # 后台管理类型
│   ├── index.ts
│   ├── user.ts
│   ├── category.ts
│   ├── product.ts
│   ├── order.ts
│   └── statistics.ts
├── mall/                     # 商城端类型
│   ├── index.ts
│   ├── order.ts
│   ├── cart.ts
│   └── product.ts
└── shared.ts                 # 跨模块共用类型
```

### 4.3 删除调试代码

移除 `order.ts` 中的 `console.log` 调试语句。

### 4.4 统一命名规范

- 后台管理类型统一使用 `Admin` 后缀，如 `OrderAdminVO`
- 商城端类型统一使用 `Mall` 前缀或无后缀，如 `OrderMallVO` 或 `Order`
- 跨模块共用类型（如 `OrderItemVO`）提取到 `types/shared.ts`

---

## 五、优先级建议

| 优先级 | 问题 | 建议处理时机 |
|-------|------|-------------|
| 🔴 P0 | API 三层合并（api/repositories/services） | 近期，根因问题 |
| 🔴 P0 | admin.ts 拆分 | 三层合并过程中一并处理 |
| 🟡 P1 | 类型定义提取到 types/ 目录 | 三层合并后立即执行 |
| 🟡 P1 | 删除调试代码 | 随时 |
| 🔵 P2 | 命名统一 | 与其他重构一并处理 |