# 项目开发与维护技术契约

> **版本**: v2.0 | **生效日期**: 2026-08-23 | **适用范围**: 全体开发者 + AI 协作
>
> **本文档职责**: 命名规范 + 跨切面编码规范（MQ / Redis / 异常 / Git）。
>
> **已收敛到独立 SSOT 的章节**:
> - DDD 架构约束 → [DDD_ARCHITECTURE_SPEC.md](DDD_ARCHITECTURE_SPEC.md)
> - API 端点 / DTO / VO / 类型映射 → [API_CONTRACT.md](API_CONTRACT.md)
> - AI Agent 行为 / 阅读顺序 / 冲突处理 → [AI_AGENT_RULES.md](AI_AGENT_RULES.md)
> - 代码审查 checklist / 报告格式 → [REVIEW.md](REVIEW.md)

---

## 一、命名规范清单

### 1.1 后端命名规范（强制）

| 类型 | 规范 | 正确示例 | 禁止示例 | 所在模块 |
|------|------|---------|---------|---------|
| 出线请求类 | `{端前缀}{业务名}Req` | `UserCartAddReq`, `AdminUserSaveReq` | `UserCartAddRequestDTO` | `s-pay-mall-api/dto/{admin,user,common}/req/` |
| 出线响应类 | `{端前缀}{业务名}Res` | `UserOrderRes`, `AdminOrderRes` | `OrderListRespDTO`, `AdminOrderVO` | `s-pay-mall-api/dto/{admin,user,common}/res/` |
| 共用出线类 | 无前缀（登录、注册、双端共用） | `LoginReq`, `LoginRes`, `ProductRes` | `CommonProductRes` | `s-pay-mall-api/dto/common/` |
> 决策依据与旧→新映射见 [API_NAMING_DECISION.md](docs/API_NAMING_DECISION.md)；`Response<T>` 统一包装类保留原名。
| 领域实体 | `{Name}Entity` | `OrderEntity`, `UserEntity` | `Order` | `s-pay-mall-domain` |
| 值对象 | `{Name}VO` | `OrderCreateVO`, `CartItemVO` | — | `s-pay-mall-domain` |
| 领域服务接口 | `I{Domain}Service` | `IOrderService`, `IMallOrderService` | — | `s-pay-mall-domain` |
| 领域服务实现 | `{Domain}ServiceImpl` | `MallOrderServiceImpl` | — | `s-pay-mall-domain` |
| 仓储接口 | `I{Entity}Repository` | `IOrderRepository` | `OrderDao` | `s-pay-mall-domain` |
| 仓储实现 | `{Entity}RepositoryImpl` | `OrderRepositoryImpl` | — | `s-pay-mall-infrastructure` |
| 网关接口 | `I{Purpose}Gateway` | `IStockGateway`, `IPayGateway` | — | `s-pay-mall-domain` |
| 网关实现 | `{Purpose}GatewayImpl` | `StockGatewayImpl` | — | `s-pay-mall-infrastructure` |
| DAO 接口 | `I{Table}Dao` | `IProductDao`, `IOrderMainDao` | — | `s-pay-mall-infrastructure/dao/` |
| PO 持久对象 | `{Table}` (无后缀) | `Product`, `OrderMain`, `CartItem` | `ProductPO` | `s-pay-mall-infrastructure/dao/{module}/po/` |
| Controller | `{Purpose}Controller` | `MallOrderController` | — | `s-pay-mall-trigger/http/` |
| Application Service | `{Purpose}ApplicationService` | `OrderApplicationService` | — | `s-pay-mall-application`（`cn.fcr.application`） |
| MQ Listener | `{Purpose}RocketListener` | `OrderPaidRocketListener` | — | `s-pay-mall-trigger/listener/` |
| Job 定时任务 | `{Purpose}Job` | `NoPayNotifyOrderJob` | — | `s-pay-mall-trigger/job/` |
| 状态机服务 | `{Domain}StateMachineServiceImpl` | `OrderStateMachineServiceImpl` | — | `s-pay-mall-domain` |
| 消息 DTO | `{Domain}MsgDTO` | `StockChangeMsgDTO` | — | `s-pay-mall-domain` |

### 1.2 数据库命名规范（强制）

| 类型 | 规范 | 示例 |
|------|------|------|
| 表名 | `lower_snake_case` | `order_main`, `order_item`, `mall_user`, `user_binding` |
| 字段名 | `lower_snake_case` | `order_no`, `product_id`, `create_time` |
| 主键 | `id` (自增) | `id BIGINT AUTO_INCREMENT` |
| 业务唯一键 | 独立字段 | `order_no VARCHAR`, `product_id BIGINT` |

### 1.3 Entity ↔ PO ↔ DB 字段映射规范（强制）

- **Java PO 字段**: Java 属性使用 `camelCase`，通过 MyBatis-Plus `@TableField` 映射到数据库 `snake_case` 列
- **Domain Entity 字段**: 与 DB 列语义对应，命名使用 `camelCase`
- **JSON 输出字段**: `api/dto/{user,common}/` 下出线类使用 `camelCase`（无 `@JsonProperty`）；`api/dto/admin/res/` 中 `AdminUserRes`、`AdminOrderRes` 为历史遗留 `snake_case`（有 `@JsonProperty`），禁止在其上扩展字段

| 层级 | Java 字段 | @JsonProperty | JSON 实际输出 |
|------|----------|---------------|-------------|
| `api/dto/{user,common}/res/`（如 UserOrderRes） | `orderId` | **无** | `"orderId"` (camelCase) |
| `api/dto/admin/res/` (AdminUserRes/AdminOrderRes) | `order_no` | **有**（历史遗留，禁止扩展） | `"order_no"` (snake_case) |
| PO (MyBatis 映射) | `orderNo` | 不适用 | 数据库 `order_no` |

> **决议**: 全局统一为 camelCase。admin/res 两个历史类的 `@JsonProperty` snake_case 视为技术债，禁止在其基础上扩展字段；新增出线类一律 camelCase，不加 `@JsonProperty`。

### 1.4 前端命名规范（强制）

| 类型 | 规范 | 示例 |
|------|------|------|
| 页面组件 | PascalCase + `Page` 后缀 | `OrderListPage.vue`, `ProductDetailPage.vue` |
| API 函数 | camelCase, 动词开头 | `getOrder()`, `createOrder()`, `loadProducts()` |
| TypeScript 接口 | PascalCase, 以 API_CONTRACT §五 映射表为准 | `interface Order`, `interface ProductRes` |
| 请求参数类型 | `{Name}Request` 或 `{Name}Params` | `OrderCreateRequest`, `OrderListParams` |
| Pinia Store | `use{Name}Store` | `useOrderStore`, `useUserStore` |
| 组合式 Hook | `use{Name}` | `useOrder()`, `useProduct()` |
| Props | camelCase | `orderNo`, `productId` |
| Emits | `on{Event}` | `onSubmit`, `onDelete` |

### 1.5 前端目录结构规范（强制）

```
src/
├── api/                          # API 调用层（唯一 HTTP 客户端封装）
│   ├── {module}/                 # 按业务模块划分，对齐后端 Controller
│   │   ├── index.ts              # API 函数定义
│   │   └── types.ts              # 模块专属类型（与后端 VO/DTO 一一对应）
│   └── request.ts                # 统一 Axios 实例 + 拦截器
├── types/                        # 跨模块共享类型
│   └── domain/
│       ├── order.ts
│       ├── product.ts
│       ├── cart.ts
│       ├── payment.ts
│       ├── adminUser.ts
│       └── user.ts
├── stores/                       # Pinia 状态管理
│   ├── user.ts
│   ├── product.ts
│   ├── cart.ts
│   ├── order.ts
│   └── payment.ts
├── hooks/                        # 组合式函数（Store 的薄封装层）
│   ├── useCart.ts
│   ├── useOrder.ts
│   ├── usePayment.ts
│   └── useProduct.ts
├── views/                        # 页面组件（扁平结构或按模块分目录）
│   ├── admin/                    # 管理后台页面
│   ├── LoginPage.vue
│   ├── ProductListPage.vue
│   ├── CartPage.vue
│   ├── CheckoutPage.vue
│   └── OrderListPage.vue
└── utils/
    ├── request.ts                # Axios 实例（baseURL, 拦截器）
    └── product.ts                # 业务工具函数
```

> **Controller 路径**: 端点完整定义见 [API_CONTRACT.md](API_CONTRACT.md) §二-四，本文档不再重复维护。

---

## 二、消息队列规范

### 2.1 Topic 命名规范（强制）

| Topic | 模式 | 示例 |
|-------|------|------|
| 业务事件 | `{domain}_{event}` | `order_paid` |
| 延时消息 | `{domain}-timeout-topic` | `order-timeout-topic` |
| 库存变更 | `product-stock-change-topic` | — |

### 2.2 MQ 消费规范（强制）

| 规范 | 说明 |
|------|------|
| 消费幂等性 | 必须使用 SETNX 幂等键 (`messageId`) 防止重复消费 |
| 异常重试 | 抛出 `RuntimeException` 触发重试，最多 3 次 |
| 死信队列 | 每个 Topic 必须配置 DLQ |
| 超时参数 | `syncSend` 和 `convertAndSend` 必须设置超时（建议 3000ms） |

### 2.3 幂等 Key 规范

| 场景 | Key 格式 | TTL |
|------|---------|-----|
| 库存变更 | `mall:stock:msg:processed:{messageId}` | 24h |
| 通用幂等 | `{businessType}:event:{businessNo}` | 24h |

> **注意**: 幂等 key 的实际格式以代码实现为准（见 `IdempotentGatewayImpl`），文档与代码的差异已登记 TECH_DEBT。

---

## 三、Redis Key 规范

### 3.1 Key 命名规范（强制）

| 场景 | Key 格式 | 数据类型 | TTL |
|------|---------|---------|-----|
| 微信 Access Token | `wechat:access_token:{appid}` | String | 110min |
| 商品库存 | `mall:product:stock:{productId}` | RAtomicLong | 永久 |
| 扫码登录 Token | `{ticket}` (直接作为 key) | String | 5min |
| 库存变更幂等 | `mall:stock:msg:processed:{messageId}` | String | 24h |
| 购物车更新锁 | `lock:mall:cart:update:{productId}` | String | — |

### 3.2 Key 命名约定

- 使用 `:` 作为层级分隔符
- 前缀标识业务领域: `wechat:`, `mall:`, `stock:`, `lock:`
- 变量部分用 `{variable}` 表示

> **注意**: 上表以代码实际值为准（验证来源: `StockGatewayImpl`、`IdempotentGatewayImpl`、`RedisDistributedLock`）。若发现文档与代码不符，以代码为准并登记 TECH_DEBT。

---

## 四、异常处理规范（强制）

| 规范 | 说明 |
|------|------|
| 业务异常 | 使用 `BusinessException`，包含错误码和描述 |
| 基础设施异常 | 使用 `AppException`，如 `"STOCK_INSUFFICIENT"` |
| 全局异常处理 | `@RestControllerAdvice` 统一处理，返回 `Response` |
| 禁止空 catch | 捕获异常后必须有处理逻辑 |
| 日志级别 | 业务异常 WARN，系统异常 ERROR |
| 禁止透传原始异常 message | 避免信息泄漏（如 JWT 过期时间、NPE 栈顶） |

> 错误码定义见 [API_CONTRACT.md](API_CONTRACT.md) §六。统一异常处理的改造规划见 [FUTURE_FEATURES.md](docs/design_wait/FUTURE_FEATURES.md) §2.1。

---

## 五、Git 提交规范

| 类型 | 说明 |
|------|------|
| `feat(module):` | 新功能 |
| `fix(module):` | Bug 修复 |
| `refactor(module):` | 重构 |
| `docs(module):` | 文档（含 API_CONTRACT.md 变更） |
| `test(module):` | 测试 |
| `chore:` | 构建/工具相关 |

示例: `feat(order): 实现订单超时自动关闭功能`

### 5.1 变更规范

- 单次提交只做一件事
- 涉及字段改名时，[API_CONTRACT.md](API_CONTRACT.md) 的更新必须与代码改动在同一提交或紧邻提交中完成
- 禁止提交敏感信息（密码、密钥等）

---

## 六、注释规范

- 文件头: `/** <职责> \n * @author 傅崇睿 */`
- public 方法: Javadoc/JSDoc，含参数和返回值说明
- 实体字段: `/** 含义说明 */`
- 语言: 中文，技术术语保留英文
- **禁止删除既有注释**: 代码变更时同步更新注释而非删除

---

## 七、版本历史

| 版本 | 日期 | 变更 |
|------|------|------|
| v2.0 | 2026-08-23 | 收敛：DDD 约束→DDD_ARCHITECTURE_SPEC.md，API 端点→API_CONTRACT.md，AI 守则→AI_AGENT_RULES.md。保留命名/MQ/Redis/异常/Git/注释规范。Redis key 表以代码为准修正 |
| v1.0 | 2026-07-02 | 初始版本 |
