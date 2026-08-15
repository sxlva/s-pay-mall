# 项目开发与维护技术契约

> **版本**: v1.0 | **生效日期**: 2026-07-02 | **适用范围**: 全体开发者 + AI 协作
>
> 本文档是 s-pay-mall 项目的**最高优先级技术规范**，所有代码修改必须遵守。AI 协作时，本文档优先级高于 CLAUDE.md 中的建议性描述。

---

## 一、命名规范清单

### 1.1 后端命名规范（强制）

| 类型 | 规范 | 正确示例 | 禁止示例 | 所在模块 |
|------|------|---------|---------|---------|
| API Facade 接口 | `I{Aggregate}Facade` | `IAuthFacade` | `IAuthService` | `s-pay-mall-api` |
| 请求 DTO | `{Action}RequestDTO` | `UserLoginRequestDTO` | `UserLoginRequest` | `s-pay-mall-api/dto/` |
| 响应 DTO | `{Entity}RespDTO` | `CartItemRespDTO`, `OrderListRespDTO` | `CartItemResponse` | `s-pay-mall-api/dto/` |
| 视图对象 VO | `{Entity}VO` | `UserVO`, `ProductVO` | `UserResponseVO` | `s-pay-mall-api/vo/` |
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
| Application Service | `{Purpose}ApplicationService` | `OrderApplicationService` | — | `s-pay-mall-trigger/application/` |
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
- **JSON 输出字段**: `dto/` 包直接输出的字段使用 `camelCase`（无 `@JsonProperty`）；`vo/` 包声明但非 Controller 直接返回的字段使用 `snake_case`（有 `@JsonProperty`）

| 层级 | Java 字段 | @JsonProperty | JSON 实际输出 |
|------|----------|---------------|-------------|
| `api/dto/` (Controller 返回) | `orderId` | **无** | `"orderId"` (camelCase) |
| `api/vo/` (Facade 声明) | `order_no` | **有** | `"order_no"` (snake_case) |
| PO (MyBatis 映射) | `orderNo` | 不适用 | 数据库 `order_no` |

### 1.4 前端命名规范（强制）

| 类型 | 规范 | 示例 |
|------|------|------|
| 页面组件 | PascalCase + `Page` 后缀 | `OrderListPage.vue`, `ProductDetailPage.vue` |
| API 函数 | camelCase, 动词开头 | `getOrder()`, `createOrder()`, `loadProducts()` |
| TypeScript 接口 | PascalCase, 与后端 VO 名称一致 | `interface OrderVO`, `interface ProductVO` |
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

### 1.6 Controller 路径规范（强制）

| Controller | 类级别路径 | HTTP 方法 | 方法路径 | 完整 URL |
|-----------|-----------|----------|---------|---------|
| AliPayController | `/pay-api/v1/alipay/` | POST | `create_pay_order` | `POST /pay-api/v1/alipay/create_pay_order` |
| AliPayController | `/pay-api/v1/alipay/` | POST | `alipay_notify_url` | `POST /pay-api/v1/alipay/alipay_notify_url` |
| LoginController | `/pay-api/v1/login/` | GET | `weixin_qrcode_ticket` | `GET /pay-api/v1/login/weixin_qrcode_ticket` |
| LoginController | `/pay-api/v1/login/` | GET | `check_login` | `GET /pay-api/v1/login/check_login` |
| WeixinPortalController | `/pay-api/v1/weixin/portal/` | POST | `receive` | `POST /pay-api/v1/weixin/portal/receive` |
| MallOrderController | `/mall-api/v1` | POST | `/cart` | `POST /mall-api/v1/cart` |
| MallOrderController | `/mall-api/v1` | GET | `/cart` | `GET /mall-api/v1/cart` |
| MallOrderController | `/mall-api/v1` | PUT | `/cart/quantity` | `PUT /mall-api/v1/cart/quantity` |
| MallOrderController | `/mall-api/v1` | DELETE | `/cart/delete` | `DELETE /mall-api/v1/cart/delete` |
| MallOrderController | `/mall-api/v1` | POST | `/orders` | `POST /mall-api/v1/orders` |
| MallOrderController | `/mall-api/v1` | GET | `/orders` | `GET /mall-api/v1/orders` |
| MallOrderController | `/mall-api/v1` | GET | `/orders/{orderNo}/continue-pay` | `GET /mall-api/v1/orders/{orderNo}/continue-pay` |
| MallOrderController | `/mall-api/v1` | GET | `/orders/{orderNo}/check-stock` | `GET /mall-api/v1/orders/{orderNo}/check-stock` |
| MallProductController | `/mall-api/v1` | GET | `/products` | `GET /mall-api/v1/products` |
| MallProductController | `/mall-api/v1` | GET | `/categories` | `GET /mall-api/v1/categories` |
| MallAuthController | `/mall-api/v1/auth` | POST | `/register` | `POST /mall-api/v1/auth/register` |
| MallAuthController | `/mall-api/v1/auth` | POST | `/login` | `POST /mall-api/v1/auth/login` |
| ProfileController | `/mall-api/v1` | GET | `/profile` | `GET /mall-api/v1/profile` |
| MallAdminController | `/mall-api/v1/admin` | GET/POST/PUT/DELETE | `/users`, `/categories`, `/products`, `/orders`, `/statistics/*` | (管理后台全套 CRUD) |
| AdminApiController | `/pay-api/v1/admin` | GET/POST/PUT/DELETE | （与 MallAdminController 重复路径结构） | (旧版管理后台 API) |

> ⚠️ **已知问题**: `MallAdminController` 和 `AdminApiController` 存在路径和功能重复，参见 [TECH_DEBT_ROADMAP.md](docs/design_wait/TECH_DEBT_ROADMAP.md)。

### 1.7 版本号约定

| 配置项 | 值 | 说明 |
|--------|---|------|
| API 版本 | `v1` | 硬编码为 `${app.config.api-version}`，当前值为 `v1` |
| 版本策略 | URL 路径前缀 | `/mall-api/v1/`, `/pay-api/v1/` |

---

## 二、DDD 架构强制约束

### 2.1 分层依赖规则（强制）

```
Trigger → Application → Domain ← Infrastructure
```

| 层 | 允许依赖 | 禁止依赖 |
|----|---------|---------|
| **Domain (领域层)** | 标准 Java 类库、`jakarta.validation` 注解 | Spring Framework, Redis, RocketMQ, MyBatis |
| **Application (应用层)** | Domain 层 | Infrastructure 层 |
| **Infrastructure (基础设施层)** | Domain 层（实现接口） | Application 层, Trigger 层 |
| **Trigger (触发层)** | Application 层 | Domain 层（直接调用） |

### 2.2 各层职责约束（强制）

#### Domain 层

| 允许 | 禁止 |
|------|------|
| 定义 Entity、Value Object | 使用 `@Service` / `@Component` / `@Autowired` |
| 实现核心业务逻辑 | 使用 `@Transactional` |
| 定义 Gateway 接口、Repository 接口 | 导入 `org.springframework.**` |
| 定义 Domain Service 接口和实现 | 导入 Redis / RocketMQ / MyBatis 包 |
| 使用 `jakarta.validation` 注解 | 直接操作数据库/缓存/MQ |

#### Infrastructure 层

| 允许 | 禁止 |
|------|------|
| 实现 Domain 层接口 | 编写核心业务逻辑 |
| 数据库操作 (DAO, Mapper) | 包含业务规则判断 |
| Redis / RocketMQ 操作 | 修改业务状态 |
| 外部 HTTP 调用 | 跨领域模块直接耦合 |

#### Infrastructure 模块对称性（强制）

```
domain/auth/    → infrastructure/auth/    (认证仓储实现、网关实现)
domain/mall/    → infrastructure/mall/    (商城仓储实现、网关实现)
domain/order/   → infrastructure/order/   (订单仓储实现、网关实现)
(跨领域)        → infrastructure/shared/  (Redis/MQ 基础封装)
```

#### Trigger 层

| 允许 | 禁止 |
|------|------|
| 接收 HTTP 请求 | 编写业务逻辑 |
| `@Valid` 参数校验 | 直接调用 Domain 层服务 |
| DTO 转换 | 包含 `if (业务条件)` 等业务判断 |
| MQ Listener 消息接收 | 实现业务规则 |

#### Application 层

| 允许 | 禁止 |
|------|------|
| 编排多个领域服务 | 包含核心业务规则 |
| `@Transactional` 事务控制 | 直接操作数据库 |
| DTO 组装转换 | 复杂业务计算逻辑 |

### 2.3 Domain 层依赖例外清单

**允许的标准 Java 类库**:

| 包路径 | 典型用途 |
|--------|---------|
| `java.util.*` | Optional, Objects, Collection, List, Map |
| `java.lang.*` | String, Long, Integer |
| `java.time.*` | LocalDateTime, LocalDate |
| `java.math.BigDecimal` | 金额计算 |

**允许的校验注解** (`jakarta.validation.constraints`):

| 注解 | 使用位置 |
|------|---------|
| `@NotNull`, `@NotBlank`, `@NotEmpty` | Entity 字段 |
| `@Size`, `@Min`, `@Max`, `@Pattern` | Entity 字段 |

### 2.4 Domain Service Bean 注册策略

- Domain Service 实现类为**纯 POJO**，不加任何 Spring 注解
- 通过 `DomainServiceConfig` 中的 12 个 `@Bean` 方法手动注册到 Spring 容器
- 所有依赖通过**构造器注入**传入

```java
// ✅ 正确: Domain Service 纯 POJO
public class MallOrderServiceImpl implements IMallOrderService {
    private final IStockGateway stockGateway;
    public MallOrderServiceImpl(IStockGateway stockGateway) {
        this.stockGateway = stockGateway;
    }
}

// ✅ 正确: Infrastructure 层 Config 注册
@Configuration
public class DomainServiceConfig {
    @Bean
    public IMallOrderService mallOrderService(IStockGateway stockGateway) {
        return new MallOrderServiceImpl(stockGateway);
    }
}
```

### 2.5 事务边界约束（强制）

| 规则 | 说明 |
|------|------|
| 事务注解仅存在于 Application 层 | `@Transactional` 只放在 `*ApplicationService` 方法上 |
| Domain 层严禁 `@Transactional` | 业务逻辑不应与事务边界耦合 |
| MQ 发送在事务外 | `sendDelayCloseMessage()` 等 MQ 操作必须在事务提交后执行 |
| Redis 幂等锁在事务外 | `trySet` / `unlock` 严禁纳入 `@Transactional` 作用域 |

---

## 三、API 设计规范

### 3.1 通用响应包装（强制）

所有 API 返回统一使用 `Response<T>`:

```json
{
  "code": "00000",
  "info": "success",
  "data": { ... }
}
```

### 3.2 请求 DTO 设计规范（强制）

| 规范 | 说明 |
|------|------|
| 状态变更 API 必须包含 `requestId` | 幂等键，类型 `@NotNull String` |
| 字段使用 `@NotNull` / `@NotBlank` | 必填字段标注 |
| 使用 `@Valid` 触发校验 | Controller 方法参数加 `@Valid` |

### 3.3 前后端类型映射规范（强制）

| 后端 DTO/VO | 前端 TypeScript 接口 | 存放位置 |
|------------|---------------------|---------|
| `OrderCreateRespDTO` | `OrderCreateResult` | `api/order/types.ts` |
| `OrderListRespDTO` | `OrderVO` | `types/domain/order.ts` |
| `CartItemRespDTO` | `CartItemVO` | `types/domain/cart.ts` |
| `ProductVO` | `ProductVO` | `types/domain/product.ts` |
| `UserVO` | `UserVO` | `types/domain/user.ts` |
| `StockCheckRespDTO` | `StockCheckResult` | `api/order/types.ts` |

### 3.4 数据流转规范（强制）

```
后端 Controller → Response<DTO> → 前端 api/ 层解构 → 组件接收干净的 VO
```

| 规则 | 说明 |
|------|------|
| API 层负责解构 `Response<T>` | `return res.data` |
| 组件层直接使用 VO 对象 | 禁止组件消费 `Response<T>` 包装 |
| 禁止使用 `any` 类型 | 所有返回值必须有明确 TypeScript 类型 |
| 禁止组件内直接调用 `axios` | 必须通过 `api/` 层封装 |

### 3.5 字段一致性规则（强制）

| 规则 | 说明 |
|------|------|
| 后端 DTO 是 Single Source of Truth | 前端类型字段不得超出后端 DTO 定义范围 |
| 前端不得私自添加后端未定义的字段 | 见 audit 记录 `stockStatus` 违规案例 |
| 字段命名保持一致 | 后端 `camelCase` → 前端 `camelCase` |

---

## 四、消息队列规范

### 4.1 Topic 命名规范（强制）

| Topic | 模式 | 示例 |
|-------|------|------|
| 业务事件 | `{domain}_{event}` | `order_paid` |
| 延时消息 | `{domain}-timeout-topic` | `order-timeout-topic` |
| 库存变更 | `product-stock-change-topic` | — |

### 4.2 MQ 消费规范（强制）

| 规范 | 说明 |
|------|------|
| 消费幂等性 | 必须使用 SETNX 幂等键 (`messageId`) 防止重复消费 |
| 异常重试 | 抛出 `RuntimeException` 触发重试，最多 3 次 |
| 死信队列 | 每个 Topic 必须配置 DLQ |
| 超时参数 | `syncSend` 和 `convertAndSend` 必须设置超时（建议 3000ms） |

### 4.3 幂等 Key 规范

| 场景 | Key 格式 | TTL |
|------|---------|-----|
| 库存变更 | `mall:stock:msg:processed:{messageId}` | 24h |
| 通用幂等 | `{businessType}:event:{businessNo}` | 24h |

---

## 五、Redis Key 规范

### 5.1 Key 命名规范（强制）

| 场景 | Key 格式 | 数据类型 | TTL |
|------|---------|---------|-----|
| 微信 Access Token | `wechat:access_token:{appid}` | String | 110min |
| 商品库存 | `mall:product:stock:{productId}` | RAtomicLong | 永久 |
| 扫码登录 Token | `{ticket}` (直接作为 key) | String | 5min |
| 库存变更幂等 | `mall:stock:msg:processed:{messageId}` | String | 24h |

### 5.2 Key 命名约定

- 使用 `:` 作为层级分隔符
- 前缀标识业务领域: `wechat:`, `mall:`, `stock:`
- 变量部分用 `{variable}` 表示

---

## 六、异常处理规范（强制）

| 规范 | 说明 |
|------|------|
| 业务异常 | 使用 `BusinessException`，包含错误码和描述 |
| 基础设施异常 | 使用 `AppException`，如 `"STOCK_INSUFFICIENT"` |
| 全局异常处理 | `@RestControllerAdvice` 统一处理，返回 `Response` |
| 禁止空 catch | 捕获异常后必须有处理逻辑 |
| 日志级别 | 业务异常 WARN，系统异常 ERROR |

---

## 七、Git 提交规范

| 类型 | 说明 |
|------|------|
| `feat(module):` | 新功能 |
| `fix(module):` | Bug 修复 |
| `refactor(module):` | 重构 |
| `docs(module):` | 文档 |
| `test(module):` | 测试 |

示例: `feat(order): 实现订单超时自动关闭功能`

---

## 八、AI 协作守则

### 8.1 强制阅读顺序

每次 AI 协作修改代码前，必须严格按以下顺序阅读，**未完成上述阅读顺序产出的代码，视为不符合本项目规范**：

1. **CLAUDE.md** — 项目入口，路由至其他文档
2. **AGENTS.md** — 项目概述（技术栈、模块结构、核心业务描述）
3. **DEVELOPMENT_GUIDE.md**（本文件）— 技术契约（命名规范、DDD 约束、API 设计规范、AI 协作硬边界，强制执行）
4. **API_CONTRACT.md** — 接口契约（端点定义、DTO 类型映射，新增/修改接口代码前必须核对）
5. **docs/design_wait/TECH_DEBT_ROADMAP.md** — 技术债清单（P1，涉及重构时必读）
6. **docs/design/README.md** — 系统架构知识集（P1，涉及架构调整时必读）
7. **REVIEW.md** — 代码审查规则（P0，提交代码前对照）

### 8.2 AI 不可逾越的硬边界

| 边界 | 说明 |
|------|------|
| **禁止在 Domain 层添加 Spring 注解** | `@Service`, `@Autowired`, `@Component`, `@Transactional` 一律禁止 |
| **禁止在 Infrastructure 层编写业务逻辑** | 基础设施层仅做技术实现，不得包含 `if (业务判断)` |
| **禁止在 Trigger 层绕过 Application 层** | Controller/Listener/Job 只能调用 Application Service |
| **禁止前端使用 `any` 类型** | 所有类型必须有明确的 TypeScript 定义 |
| **禁止前端组件直接消费 `Response<T>`** | 必须在 `api/` 层解构 |
| **禁止跨领域模块直接耦合** | `mall` 不能直接 import `order` 的实现类 |

### 8.3 AI 修改代码的检查清单

每次修改代码后，必须自查以下项目：

```
- [ ] 新增类命名是否符合 DEVELOPING_GUIDE.md §1 规范？
- [ ] 是否违反了 DDD 分层依赖规则（§2.1）？
- [ ] Domain 层是否误用了 Spring 注解（§2.2）？
- [ ] 状态变更 API 是否包含 requestId 幂等键（§3.2）？
- [ ] 前端类型是否与后端 DTO 一一对应（§3.3）？
- [ ] 前端 API 层是否正确解构了 Response<T>（§3.4）？
- [ ] 是否添加了 @Valid 参数校验（§3.2）？
- [ ] MQ Listener 是否实现了消费幂等性（§4.2）？
- [ ] 注释是否符合 CLAUDE.md 注释规范？
```

### 8.4 AI 不允许的操作

| 操作 | 原因 |
|------|------|
| 自行创建新的 Maven 模块 | 需人工评估模块依赖关系 |
| 删除 `deprecated` 代码 | 需先确认调用链完整迁移 |
| 合并/删除 API 端点 | 需确认前端同步改造 |
| 修改 Redis Key 格式 | 影响生产数据一致性 |
| 修改数据库表结构 | 需 DBA 评审 + 数据迁移方案 |

### 8.5 前后端联动修改协议

当修改涉及前后端接口时，必须同步修改：

| 修改后端 | 同步修改前端 |
|---------|------------|
| DTO 新增/删除字段 | `types/domain/` 或 `api/{module}/types.ts` |
| 新增 Controller 端点 | `api/{module}/index.ts` 新增调用函数 |
| 修改响应字段名 | 前端所有引用该字段的组件 |
| 修改错误码 | 前端错误处理逻辑 |
| 修改 JSON 命名策略 | 全局检索前端字段读取 |

---

## 九、本文档与其他文档的关系

| 文档 | 角色 | 优先级 |
|------|------|-------|
| `DEVELOPMENT_GUIDE.md` (本文件) | **技术契约** — 强约束，违反即违规 | **最高** |
| `REVIEW.md` | 代码审查规则 — 审查时对照检查 | 高 |
| `CLAUDE.md` | 项目说明 — 技术栈、模块结构概览 | 中 |
| `docs/design/*.md` | 系统设计文档 — 业务链路知识集 | 中 |
| `docs/design_wait/TECH_DEBT_ROADMAP.md` | 技术债追踪 — 已知问题与修复计划 | 参考 |
| `docs/design_wait/FUTURE_FEATURES.md` | 功能规划 — 待实现功能清单 | 参考 |

---

## 十、版本历史

| 版本 | 日期 | 变更 |
|------|------|------|
| v1.0 | 2026-07-02 | 初始版本：提取现有命名规则、定义架构约束、制定 AI 协作守则 |
