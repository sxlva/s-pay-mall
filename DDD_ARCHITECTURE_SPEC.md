# s-pay-mall DDD 架构规范

> **版本**: v1.0 | **生效日期**: 2026-08-23 | **适用范围**: 全体开发者 + AI Agent
>
> **本文件是 DDD 架构规则的唯一真相源（SSOT）**。只负责 DDD 分层、依赖、职责、对称性、事务边界。不包含 Git/API/MQ/AI 行为/Review 等内容（见各自 SSOT）。

---

## 1. 分层依赖规则（强制单向）

```
Trigger → Application → Domain ← Infrastructure
```

| 层 | 允许依赖 | 禁止依赖 |
|----|---------|---------|
| **Domain（领域层）** | 标准 Java 类库、`jakarta.validation` | Spring / Redis / RocketMQ / MyBatis |
| **Application（应用层）** | Domain 层 | Infrastructure 层 |
| **Infrastructure（基础设施层）** | Domain 层（实现接口） | Application 层、Trigger 层 |
| **Trigger（触发层）** | Application 层 | Domain 层（直接调用） |

### 1.1 模块物理归属规则

DDD 分层不仅要求逻辑依赖单向，还要求**物理模块独立**。各层应独立为 Maven 模块：

| 层 | Maven 模块 | 当前状态 |
|----|-----------|---------|
| Trigger | `s-pay-mall-trigger` | ✅ 存在 |
| Application | `s-pay-mall-application` | ✅ 存在（2026-10-03 创建，详见 ROADMAP §3.1 P0-2 / §3.2 A.12） |
| Domain | `s-pay-mall-domain` | ✅ 存在 |
| Infrastructure | `s-pay-mall-infrastructure` | ✅ 存在 |

**规则**: Application 层**必须**独立为 `s-pay-mall-application` 模块，与 Trigger 层物理分离。Trigger 层通过依赖 `s-pay-mall-application` 模块调用 Application 服务，禁止将 Application 服务放在 Trigger 模块内。

> **执行记录（2026-10-03，原 DOCUMENT_CODE_MISMATCH 已消除）**: `OrderApplicationService` + `OrderTransactionService` 已按 ROADMAP §3.1 P0-2 迁入 `s-pay-mall-application` 模块的 `cn.fcr.application` 包，两服务**一起移出**，未产生循环依赖（执行前分析结论已沉淀至 ROADMAP §3.2 A.12）。原启动模块 `s-pay-mall-app` 同步更名 `s-pay-mall-start`，定位明确为装配/启动模块、不含业务代码。

### 1.2 模块依赖事实（pom.xml 验证）

```
s-pay-mall-start        → trigger + application + domain + infrastructure
s-pay-mall-trigger      → application + domain + infrastructure
s-pay-mall-application  → domain
s-pay-mall-infrastructure → domain
s-pay-mall-domain       → types + api（无技术框架依赖，见 §4）
```

当前**无循环依赖**。

---

## 2. 各层职责

### 2.1 Trigger 层（触发层）

| 允许 | 禁止 |
|------|------|
| 接收 HTTP 请求（`@RestController`、`@GetMapping` 等） | 编写业务逻辑 |
| `@Valid` 参数校验 | 直接调用 Domain 层服务（必须通过 Application 层） |
| DTO 与 Command/Query 转换 | 包含 `if (业务条件)` 等业务判断 |
| MQ Listener 消息接收 | 实现业务规则 |
| 调用 Application 层服务 | — |

### 2.2 Application 层（应用层）

| 允许 | 禁止 |
|------|------|
| 编排多个领域服务 | 包含核心业务规则 |
| `@Transactional` 事务控制 | 直接操作数据库（必须通过 Repository） |
| DTO 组装转换 | 复杂业务计算逻辑 |

### 2.3 Domain 层（领域层）— 核心

| 允许 | 禁止 |
|------|------|
| 定义 Entity、Value Object | 导入任何技术框架（Spring、Redis、MQ 等） |
| 实现核心业务逻辑 | 使用 `@Service`、`@Component`、`@Autowired` |
| 定义 Gateway 接口、Repository 接口 | 使用 `@Transactional` |
| 定义 Domain Service 接口和实现 | 直接操作数据库/缓存/消息队列 |
| 使用 `jakarta.validation` 注解 | 导入 `org.springframework.**` |

**Domain 层严禁出现的导入**:

```java
import org.springframework.*;              // Spring 框架
import org.apache.rocketmq.*;              // RocketMQ
import org.redisson.*;                     // Redisson
import org.springframework.data.redis.*;   // Redis
import org.apache.ibatis.*;                // MyBatis
```

### 2.4 Infrastructure 层（基础设施层）

| 允许 | 禁止 |
|------|------|
| 实现 Domain 层接口 | 编写核心业务逻辑 |
| 数据库操作（DAO、MyBatis Mapper） | 包含业务规则判断 |
| Redis / RocketMQ 操作 | 修改业务状态 |
| 外部 HTTP 调用 | 跨领域模块直接耦合 |

> **已修复（2026-10-04，详见 ROADMAP §3.1 P0-3 / §3.2 A.13）**: ~~`WeixinLoginGatewayImpl#createWechatUserAndBind` 标注了 `@Transactional`~~。事务边界已上移至 `AuthApplicationService.handleWechatScanLogin`，Infrastructure 层事务注解已摘除，全仓 `@Transactional` 仅存于 Application 层。

---

## 2.5 领域边界规则（2026-10-03 M2 重构落地）

**核心原则：auth 管身份、mall 管商城、order 管订单。**

### 2.5.1 目标包结构

```
domain
├── auth            身份：登录编排、令牌、第三方登录、权限
│   ├── login       ILoginService、WeixinLoginService、WeixinBindService、微信网关/仓储
│   ├── token       IAuthTokenGateway（JWT 令牌）
│   └── permission  Role 等角色/权限模型
├── mall            商城：商品、购物车、会员、统计（不再出现任何订单类）
│   ├── product     Product/Category 聚合、库存网关、MQ 库存变更 handler、商品异常
│   ├── cart        Cart/CartItem 聚合、购物车分布式锁
│   ├── user        User 聚合、UserLoginVO/UserProfile、用户绑定网关
│   └── statistics  统计读模型
└── order           订单：下单、支付单、状态机（唯一订单出口，不再出现商品/购物车类）
    ├── model       Order/OrderItem/OrderState、PayOrderEntity/PayStatus、OrderVO 等读模型
    ├── service     IMallOrderService、IOrderStateMachineService、PayOrderService
    ├── gateway     IMallOrderQuery/IOrderPayment/IOrderQuery/IPay/IPayOrder/IAlipayQuery 网关
    ├── adapter     IOrderEventPublisher（支付成功事件）
```

### 2.5.2 边界规则（由 DomainArchitectureGuardTest 守卫规则 3/4/5 自动化守护）

| 规则 | 内容 |
|------|------|
| B1 | mall 不再出现任何订单类：mall → order 只允许 import `domain.order.gateway` 包下的接口，禁止 import 订单实体/服务/读模型 |
| B2 | order 不再出现商品/购物车/会员类：order → mall 只允许 import `mall..gateway..` 库存网关接口与 `mall.cart.model.valobj.CartItemVO`（下单入参的共享读模型），禁止 import 实体/服务 |
| B3 | 跨域只允许通过 gateway 接口交互，禁止互相 import 实体（B1/B2 的白名单即全部合法出口） |

> **例外说明**: `CartItemVO` 是购物车值对象读模型（下单入参），按共享内核对待；若未来引入 OrderCreateCommand 可消除该例外。

---

## 3. Domain 层依赖例外清单

### 3.1 允许的标准 Java 类库

| 包路径 | 典型用途 |
|--------|---------|
| `java.util.Optional` | 空值安全处理 |
| `java.util.Objects` | `requireNonNull`、`equals` |
| `java.util.Collection` / `List` / `Map` | 集合操作 |
| `java.lang.String` / `Long` / `Integer` | 基础类型 |
| `java.time.*` | 日期时间 API |
| `java.math.BigDecimal` | 高精度数值（金额计算） |

### 3.2 允许的校验注解（`jakarta.validation.constraints`）

| 注解 | 用途 | 使用位置 |
|------|------|----------|
| `@NotNull` | 非空校验 | Domain Entity 字段 |
| `@NotBlank` | 字符串非空校验 | Domain Entity 字段 |
| `@NotEmpty` | 集合非空校验 | Domain Entity 字段 |
| `@Size` | 长度/大小校验 | Domain Entity 字段 |
| `@Min` / `@Max` | 数值范围校验 | Domain Entity 字段 |
| `@Pattern` | 正则校验 | Domain Entity 字段 |

### 3.3 `@Valid` / `@Validated` 使用限制

| 注解 | 允许使用位置 | 说明 |
|------|-------------|------|
| `@Valid` | Trigger 层（Controller） | 触发参数校验 |
| `@Validated` | Trigger 层（Controller） | 触发参数校验，支持分组 |
| `@Valid` 嵌套 | **禁止在 Domain 层使用** | Domain Entity 不要使用 `@Valid` 嵌套校验 |

### 3.4 禁止的技术框架

| 框架 | 禁止的注解/类 |
|------|-------------|
| Spring Framework | `@Service`、`@Autowired`、`@Component`、`@Transactional` |
| Spring Boot | `@SpringBootApplication`、`@Configuration` |
| Redis | `RedisTemplate`、`RedissonClient` |
| RocketMQ | `@RocketMQMessageListener`、`MQProducer` |
| MyBatis | `@Mapper`、`SqlSession` |

> **已修复（2026-10-04，对应 ROADMAP §3.1 P1-5）**: ~~`s-pay-mall-domain/pom.xml` 包含 `spring-context`、`spring-tx`、`alipay-sdk-java`、`jjwt`、`fastjson` 技术依赖~~，已移除（`alipay-sdk-java` 改由 infrastructure 显式声明）。Domain 层仅剩纯领域依赖。

---

## 4. Infrastructure 模块对称性

Infrastructure 层必须按 Domain 层模块结构对称拆分，确保单一职责与物理隔离。

### 4.1 映射规则

| Domain 层模块 | Infrastructure 层模块 | 包含内容 |
|---------------|---------------------|----------|
| `domain/auth/` | `infrastructure/auth/` | 认证仓储实现、网关实现（`login/`、`token/` 镜像） |
| `domain/mall/` | `infrastructure/mall/` | 商城仓储实现、网关实现（`product/`、`cart/`、`user/`、`statistics/` 镜像） |
| `domain/order/` | `infrastructure/order/` | 订单仓储实现、网关实现 |
| （跨领域） | `infrastructure/shared/` | Redis/MQ 基础封装、DomainServiceConfig |

> **镜像规则（2026-10-03 M2）**: Infrastructure 包结构镜像跟随 Domain 子包拆分；`JwtTokenProvider` 归 `infrastructure/auth/token/`，订单网关实现归 `infrastructure/order/gateway/`。旧链（`legacy/` 镜像）已于 2026-10-03 legacy sunset 整体删除。

### 4.2 审查要点

- `infrastructure/{module}/` 包下只能包含该 Domain 模块所需的实现
- 禁止跨模块类引用（如 `MallRepositoryImpl` 直接依赖 `OrderRepositoryImpl`）
- 禁止全局大杂烩包（`infrastructure/common/` 下塞满不相关类）

---

## 5. Domain Service Bean 注册策略

### 5.1 规则

- Domain Service 实现类为**纯 POJO**，不加任何 Spring 注解
- 通过 `infrastructure/config/shared/DomainServiceConfig` 中的 `@Bean` 方法手动注册到 Spring 容器
- 所有依赖通过**构造器注入**传入

> **注意**: `@Bean` 方法数量以代码实际为准（见 [DomainServiceConfig.java](s-pay-mall-infrastructure/src/main/java/cn/fcr/infrastructure/config/shared/DomainServiceConfig.java)）。**不在此写死数量**，避免文档与代码脱节。

### 5.2 示例

```java
// ✅ Domain Service 纯 POJO（无 Spring 注解，仅 @Slf4j）
public class MallOrderServiceImpl implements IMallOrderService {
    private final IStockGateway stockGateway;
    public MallOrderServiceImpl(IStockGateway stockGateway) {
        this.stockGateway = stockGateway;
    }
}

// ✅ Infrastructure 层 Config 注册
@Configuration
public class DomainServiceConfig {
    @Bean
    public IMallOrderService mallOrderService(IStockGateway stockGateway) {
        return new MallOrderServiceImpl(stockGateway);
    }
}
```

---

## 6. 事务边界约束

### 6.1 规则

| 规则 | 说明 |
|------|------|
| `@Transactional` 仅存在于 Application 层 | 只放在 `*ApplicationService` / `*TransactionService` 方法上 |
| Domain 层严禁 `@Transactional` | 业务逻辑不应与事务边界耦合 |
| Infrastructure 层严禁 `@Transactional` | 见 §2.4 已知违规 |
| MQ 发送在事务外 | `sendDelayCloseMessage()` 等 MQ 操作必须在事务提交后执行 |
| Redis 幂等锁在事务外 | `trySet` / `unlock` 严禁纳入 `@Transactional` 作用域（MySQL 回滚不会回滚 Redis） |

### 6.2 幂等锁正确模式（事务外锁 + 事务内业务）

```java
@Service
public class OrderApplicationService {
    // 外层方法：无事务注解，负责幂等锁
    public OrderVO createOrder(UserOrderCreateReq request) {
        // 1. 幂等锁（事务外）
        if (!idempotentGateway.checkAndLock(request.getRequestId())) {
            throw new BusinessException("请求重复");
        }
        try {
            // 2. 委托带事务的内层方法
            return createOrderWithTransaction(request);
        } finally {
            // 3. 释放锁（事务提交后）
            idempotentGateway.unlock(request.getRequestId());
        }
    }

    @Transactional(rollbackFor = Exception.class)
    public OrderVO createOrderWithTransaction(UserOrderCreateReq request) {
        OrderEntity order = orderService.createOrder(request);
        return OrderAssembler.toVO(order);
    }
}
```

---

## 7. 已知违规与已知债索引

本节汇总与 DDD 架构相关的已知问题，详细修复方案见 [ROADMAP.md](docs/design/ROADMAP.md) §3。

| 债务 ID（ROADMAP §3） | 问题 | 本规范条款 | 状态 |
|-------------|------|----------|------|
| P0-2 | ~~`s-pay-mall-application` 模块不存在，Application Service 在 trigger 包~~（2026-10-03 已修复，详见 ROADMAP §3.2 A.12） | §1.1 模块物理归属规则 | 已处理（2026-10-03） |
| P0-3 | ~~`WeixinLoginGatewayImpl` 有 `@Transactional`~~（2026-10-04 已修复，详见 ROADMAP §3.2 A.13） | §2.4 / §6.1 | 已处理（2026-10-04） |
| P0-5 | Domain 层跨领域反向依赖（PayOrderService import mall.gateway） | §2.3 / §4.2 | 已处理（2026-10-03 M2-5：IPayGateway 收编进 order，PayOrderService 同域，依赖自然消除） |
| P1-5 | Domain pom.xml 含技术依赖 | §3.4 | 已处理（2026-10-04） |

> 修改相关代码时，必须先阅读 ROADMAP.md 对应条目，不得擅自修改业务代码绕过问题。

---

## 8. 版本历史

| 版本 | 日期 | 变更 |
|------|------|------|
| v1.2 | 2026-10-04 | P0-3 修复：Infrastructure 层事务注解摘除，事务边界上移至 `AuthApplicationService`；§2.4 已知违规消除，§7 P0-3 标记已处理 |
| v1.1 | 2026-10-03 | P0-2 修复：`s-pay-mall-application` 模块创建，两服务迁入 `cn.fcr.application`；`s-pay-mall-app` 更名 `s-pay-mall-start`。§1.1 DOCUMENT_CODE_MISMATCH 消除，§1.2 依赖事实更新，§7 P0-2 标记已处理 |
| v1.0 | 2026-08-23 | 初始版本：从 .trae/rules + DEVELOPMENT_GUIDE §2 + REVIEW §1 收敛 DDD 架构规则。@Bean 数量不再写死，引用代码。标注已知违规指向 TECH_DEBT（现合并为 ROADMAP §3） |
