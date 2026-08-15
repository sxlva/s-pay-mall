# s-pay-mall 代码审查规则（v2）

> 本文件为 Claude Code 审查代码时的最高优先级指导方针,所有审查必须严格遵循以下规则。
> **本版本新增「零、契约核心检查」，优先级高于第一章 DDD 检查** —— 字段/接口命名问题必须在架构问题之前先拦截，因为它是本项目当前最大的错误来源（详见接口契约审计记录）。

---

## 零、契约核心检查（最高优先级，先于一切其他审查项）

**规则**: 任何涉及接口路径、请求/响应字段、DTO/VO/TS interface 命名的新增或修改，必须先核对 `docs/API_CONTRACT.md`，该文件是本项目唯一命名真相源。

**审查要点**:
- ✅ 新增接口：`API_CONTRACT.md` 中是否已存在对应条目？不存在 → 判 P0，要求先补充契约条目再合并代码
- ✅ 字段命名：代码中的字段名是否与契约文件逐字一致（包括大小写）？不一致 → 判 P0
- ❌ **禁止**在 `api/vo` 包新增使用 `@JsonProperty` 进行 snake_case 转换的类；已存在的视为技术债，禁止在其基础上扩展字段
- ❌ **禁止**前端 TS interface 声明后端契约文件中不存在的字段（即"臆造字段"，如审计中发现的 `stockStatus`）
- ✅ 若发现代码与契约文件不一致，默认以契约文件为准去改代码；仅当契约文件本身有明显错误时才反向修改契约文件，且必须在审查意见中注明原因

**检查方法**:
```
1. 定位本次改动涉及的接口/字段
2. 在 API_CONTRACT.md 的"二、端点契约表"或"三、对象字段契约表"中搜索对应条目
3. 逐字段比对：字段名、类型、是否可为空
4. 不一致 → 停止审查其他项，先报 P0
```

**示例**:
```java
// ❌ 错误: VO 类使用 @JsonProperty 转 snake_case（违反契约文件"全局命名策略"）
public class ProductVO {
    @JsonProperty("category_id")
    private Long categoryId;
}

// ✅ 正确: 与 API_CONTRACT.md 保持一致的 camelCase
public class ProductVO {
    private Long categoryId;
}
```

```typescript
// ❌ 错误: 前端声明契约文件中不存在的字段
export interface StockCheckResult {
  success: boolean;
  message: string;
  stockStatus: string;  // API_CONTRACT.md 中无此字段，禁止臆造
}

// ✅ 正确: 与契约文件逐字段对应
export interface StockCheckResult {
  success: boolean;
  message: string;
}
```

---

## 一、DDD 架构合规性审查

### 1.1 分层依赖检查

**规则**: 依赖方向必须严格单向: `Trigger → Application → Domain ← Infrastructure`

**审查要点**:
- ✅ **允许**: Trigger 依赖 Application
- ✅ **允许**: Application 依赖 Domain
- ✅ **允许**: Infrastructure 依赖 Domain (实现 Domain 接口)
- ❌ **禁止**: Domain 依赖任何其他层
- ❌ **禁止**: Infrastructure 依赖 Application 或 Trigger
- ❌ **禁止**: Application 依赖 Infrastructure

> **现状说明**：`s-pay-mall-application` 模块当前不存在于磁盘，`OrderApplicationService` 暂存于 `trigger/application` 包内（见 `CLAUDE_project_guide_v2.md` "现状与磁盘实际结构的差异"）。审查该包代码时按 Application 层规则审查，但**不得**以"反正还在 trigger 包里"为由放宽标准。

**检查方法**:
```java
// Domain 层严禁出现的导入
import org.springframework.*;           // Spring 框架
import org.apache.rocketmq.*;         // RocketMQ
import org.redisson.*;                // Redisson
import org.springframework.data.redis.*; // Redis
```

### 1.2 Domain 层依赖例外清单

**规则**: Domain 层原则上不依赖任何技术框架,但以下标准 Java 类库和校验注解是允许的:

**允许的标准 Java 类库**:
| 包路径 | 说明 | 典型用途 |
|--------|------|----------|
| `java.util.Optional` | 空值安全处理 | 返回值空值处理 |
| `java.util.Objects` | 对象工具类 | `requireNonNull`, `equals` |
| `java.util.Collection` | 集合接口 | 集合操作 |
| `java.util.List` / `java.util.Map` | 集合实现 | 数据结构 |
| `java.lang.String` / `java.lang.Long` | 基础类型 | 数据类型 |
| `java.time.*` | 日期时间 API | 时间处理 |
| `java.math.BigDecimal` | 高精度数值 | 金额计算 |

**允许的校验注解**:
| 注解 | 包路径 | 用途 | 使用位置 |
|------|--------|------|----------|
| `@NotNull` | `jakarta.validation.constraints` | 非空校验 | Domain 层 Entity 字段 |
| `@NotBlank` | `jakarta.validation.constraints` | 字符串非空校验 | Domain 层 Entity 字段 |
| `@NotEmpty` | `jakarta.validation.constraints` | 集合非空校验 | Domain 层 Entity 字段 |
| `@Size` | `jakarta.validation.constraints` | 长度/大小校验 | Domain 层 Entity 字段 |
| `@Min` / `@Max` | `jakarta.validation.constraints` | 数值范围校验 | Domain 层 Entity 字段 |
| `@Pattern` | `jakarta.validation.constraints` | 正则校验 | Domain 层 Entity 字段 |

**@Valid 与 @Validated 使用限制**:
| 注解 | 允许使用位置 | 说明 |
|------|-------------|------|
| `@Valid` | **Trigger 层** (Controller) | 触发参数校验 |
| `@Validated` | **Trigger 层** (Controller) | 触发参数校验，支持分组 |
| `@Valid` 嵌套 | **禁止在 Domain 层使用** | Domain Entity 不要使用 @Valid 进行嵌套校验 |

**禁止的技术框架**:
| 框架 | 说明 |
|------|------|
| Spring Framework | `@Service`, `@Autowired`, `@Component` 等 |
| Spring Boot | `@SpringBootApplication`, `@Configuration` 等 |
| Redis | `RedisTemplate`, `RedissonClient` 等 |
| RocketMQ | `@RocketMQMessageListener`, `MQProducer` 等 |
| MyBatis | `@Mapper`, `SqlSession` 等 |

**示例**:
```java
// ✅ 正确: Domain 层使用标准 Java 类库和校验注解
import java.util.Optional;
import java.util.Objects;
import jakarta.validation.constraints.NotNull;

public class OrderEntity {
    @NotNull
    private String orderNo;
    
    public Optional<OrderItem> findItem(Long productId) {
        Objects.requireNonNull(productId);
        return items.stream()
            .filter(item -> item.getProductId().equals(productId))
            .findFirst();
    }
}

// ❌ 错误: Domain 层依赖技术框架
import org.springframework.stereotype.Service;  // 禁止

@Service  // 禁止
public class OrderService {
    @Autowired  // 禁止
    private RedisTemplate redisTemplate;  // 禁止
}
```

### 1.3 层级职责边界检查

#### Trigger 层 (触发层)
**允许**:
- 接收 HTTP 请求 (`@RestController`, `@GetMapping` 等)
- 参数校验 (`@Valid`, `@NotNull` 等)
- DTO 与 Command/Query 的转换
- 调用 Application 层服务

**禁止**:
- ❌ 编写业务逻辑
- ❌ 直接调用 Domain 层服务 (必须通过 Application 层)
- ❌ 包含 `if (业务条件) { ... }` 等业务判断
- ❌ 实现业务规则

**示例**:
```java
// ✅ 正确: Trigger 层仅做参数转换和服务调用
@PostMapping("/login")
public Response<UserLoginVO> login(@RequestBody UserLoginRequestDTO request) {
    return Response.success(authService.login(request));
}

// ❌ 错误: Trigger 层包含业务逻辑
@PostMapping("/login")
public Response<UserLoginVO> login(@RequestBody UserLoginRequestDTO request) {
    if (request.getUsername().length() < 3) {  // 业务逻辑
        throw new BusinessException("用户名长度不足");
    }
    return Response.success(authService.login(request));
}
```

#### Application 层 (应用层)
**允许**:
- 编排多个领域服务
- DTO 组装和转换
- 事务控制 (`@Transactional`)
- 调用 Domain 层服务

**禁止**:
- ❌ 包含核心业务规则
- ❌ 直接操作数据库 (必须通过 Repository)
- ❌ 包含复杂的业务计算逻辑

**示例**:
```java
// ✅ 正确: Application 层编排领域服务
@Service
public class OrderApplicationService {
    public OrderVO createOrder(OrderCreateCommand command) {
        // 1. 调用领域服务
        OrderEntity order = orderService.createOrder(command);
        // 2. 转换为 VO
        return OrderAssembler.toVO(order);
    }
}

// ❌ 错误: Application 层包含业务逻辑
@Service
public class OrderApplicationService {
    public OrderVO createOrder(OrderCreateCommand command) {
        // 业务逻辑应该在 Domain 层
        if (command.getItems().size() > 100) {
            throw new BusinessException("订单商品数量超限");
        }
        // ...
    }
}
```

#### Domain 层 (领域层) - 核心层
**允许**:
- 定义 Entity、Value Object
- 实现核心业务逻辑
- 定义 Domain Service
- 定义 Gateway 接口 (外部依赖抽象)
- 定义 Repository 接口 (持久化抽象)

**禁止**:
- ❌ 导入任何技术框架 (Spring、Redis、MQ 等)
- ❌ 使用 `@Service`、`@Component` 等注解
- ❌ 直接操作数据库、缓存、消息队列

**示例**:
```java
// ✅ 正确: Domain 层定义接口和业务逻辑
public interface IStockGateway {
    boolean deductStock(Long productId, Integer quantity);
}

public class OrderEntity {
    public void pay() {
        if (this.status != OrderState.CREATED) {
            throw new BusinessException("订单状态不正确");
        }
        this.status = OrderState.PAID;
    }
}

// ❌ 错误: Domain 层依赖技术框架
import org.springframework.stereotype.Service;  // 禁止

@Service  // 禁止
public class OrderService {
    @Autowired  // 禁止
    private RedisTemplate redisTemplate;  // 禁止
}
```

#### Infrastructure 层 (基础设施层)
**允许**:
- 实现 Domain 层定义的接口
- 数据库操作 (DAO、MyBatis Mapper)
- 缓存操作 (Redis)
- 消息队列操作 (RocketMQ)
- 外部服务调用 (HTTP Client)

**禁止**:
- ❌ 编写核心业务逻辑
- ❌ 包含业务规则判断
- ❌ 修改业务状态

**示例**:
```java
// ✅ 正确: Infrastructure 层实现 Domain 接口
@Repository
public class OrderRepositoryImpl implements IOrderRepository {
    @Autowired
    private OrderDao orderDao;
    
    public OrderEntity queryOrder(String orderNo) {
        return orderDao.selectByOrderNo(orderNo);
    }
}

// ❌ 错误: Infrastructure 层包含业务逻辑
@Repository
public class OrderRepositoryImpl implements IOrderRepository {
    public OrderEntity queryOrder(String orderNo) {
        OrderEntity order = orderDao.selectByOrderNo(orderNo);
        // 业务逻辑应该在 Domain 层
        if (order.getStatus() == OrderState.CREATED) {
            order.setStatus(OrderState.CANCELLED);  // 禁止修改业务状态
        }
        return order;
    }
}
```

### 1.4 基础设施与领域模块对称性

**规则**: Infrastructure 层必须严格按照 Domain 层的模块结构进行对称拆分，确保单一职责与物理隔离。

**重要性**: 在字节跳动等大厂，后端代码量极大，如果没有 Infrastructure 的模块化拆分，RepositoryImpl 会迅速膨胀成一个几千行的"巨石文件"，在 Code Review 时会被直接打回。

**映射规则**:

| Domain 层模块 | Infrastructure 层模块 | 包含内容 |
|---------------|---------------------|----------|
| `domain/auth/` | `infrastructure/auth/` | 认证相关的仓储实现、网关实现 |
| `domain/mall/` | `infrastructure/mall/` | 商城相关的仓储实现、网关实现 |
| `domain/order/` | `infrastructure/order/` | 订单相关的仓储实现、网关实现 |
| (跨领域) | `infrastructure/shared/` | Redis 基础封装、MQ 基础配置、通用工具 |

**目录结构示例**:

```
s-pay-mall-infrastructure/
├── src/main/java/cn/fcr/infrastructure/
│   ├── auth/                      # 对应 Domain/auth
│   │   ├── repository/            # 认证仓储实现
│   │   └── gateway/               # 认证网关实现
│   ├── mall/                      # 对应 Domain/mall
│   │   ├── repository/            # 商城仓储实现
│   │   │   ├── ProductRepository.java
│   │   │   ├── CartRepository.java
│   │   │   └── StatisticsRepository.java
│   │   └── gateway/               # 商城网关实现
│   │       ├── StockGatewayImpl.java
│   │       └── IdempotentGatewayImpl.java
│   ├── order/                     # 对应 Domain/order
│   │   ├── repository/            # 订单仓储实现
│   │   │   └── OrderRepositoryImpl.java
│   │   └── gateway/               # 订单网关实现
│   │       └── PaymentGatewayImpl.java
│   └── shared/                    # 仅允许跨领域共用技术实现
│       ├── redis/                  # Redis 基础封装
│       ├── mq/                     # MQ 基础配置
│       └── config/                 # 基础设施通用配置
```

**审查要点**:

- ✅ **包路径映射**: `s-pay-mall-infrastructure/src/main/java/cn/fcr/infrastructure/{module}/` 必须存在，且该包下只能包含该 Domain 模块所需的实现
- ✅ **单一职责**: 每个 Infrastructure 模块只负责实现对应 Domain 模块的接口
- ✅ **物理隔离**: 不同模块的代码不能直接相互调用，必须通过 Domain 层解耦

**禁止项**:
- ❌ 禁止在 `infrastructure/mall/` 下出现 `order` 相关的实现
- ❌ 禁止跨模块的类引用 (如 `MallRepositoryImpl` 直接依赖 `OrderRepositoryImpl`)
- ❌ 禁止出现全局大杂烩包 (`infrastructure/common/` 下塞满所有不相关的类)

**示例**:

```java
// ✅ 正确: mall 模块的仓储实现
// infrastructure/mall/repository/ProductRepository.java
@Repository
public class ProductRepository implements IProductRepository {
    // 只处理商品相关的仓储逻辑
}

// ✅ 正确: order 模块的仓储实现
// infrastructure/order/repository/OrderRepositoryImpl.java
@Repository
public class OrderRepositoryImpl implements IOrderRepository {
    // 只处理订单相关的仓储逻辑
}

// ❌ 错误: 跨模块耦合
// infrastructure/mall/repository/ProductRepository.java
@Repository
public class ProductRepository implements IProductRepository {
    @Autowired
    private OrderRepository orderRepository;  // 禁止：mall 不应依赖 order
    
    public void something() {
        orderRepository.findById();  // 跨模块调用，违反对称性原则
    }
}
```

---

## 二、代码质量审查规则

### 2.1 命名规范检查

#### 后端命名
| 类型 | 规范 | 示例 | 检查项 |
|------|------|------|--------|
| API 接口 | `I{Aggregate}Facade` | `IAuthFacade`, `IOrderFacade` | ❌ 禁止 `IAuthService` |
| DTO | `{Action}RequestDTO` | `UserLoginRequestDTO` | ❌ 禁止 `UserLoginRequest` |
| VO | `{Entity}VO` | `UserVO`, `OrderVO` | ❌ 禁止返回 `Map<String, Object>`；❌ 禁止 `@JsonProperty` snake_case（见「零」） |
| 领域服务 | `I{Domain}Service` | `IOrderService` | ✅ 接口在 Domain 层 |
| 仓储接口 | `I{Entity}Repository` | `IOrderRepository` | ✅ 接口在 Domain 层 |
| 网关接口 | `I{Purpose}Gateway` | `IStockGateway` | ✅ 接口在 Domain 层 |
| 实体 | `{Name}Entity` | `OrderEntity` | ✅ 在 Domain 层 |

#### 前端命名
| 类型 | 规范 | 示例 |
|------|------|------|
| 组件文件 | PascalCase | `OrderListPage.vue` |
| API 文件 | camelCase | `order.ts` |
| 类型定义 | PascalCase，字段名与契约文件逐字一致 | `interface OrderVO` |
| 方法名 | camelCase | `handleCreateOrder` |

### 2.2 注释完整性检查

**规则**: 所有类、接口、公共方法必须有完整的 JavaDoc 注释

**检查项**:
- ✅ 类注释: 说明类的职责和作用
- ✅ 接口注释: 说明接口的用途
- ✅ 方法注释: 说明方法的功能、参数、返回值
- ✅ 复杂逻辑注释: 解释业务规则和实现思路

**示例**:
```java
/**
 * 订单领域服务
 * 负责订单的创建、支付、取消等核心业务逻辑
 */
public interface IOrderService {
    /**
     * 创建订单
     * 
     * @param command 创建订单命令
     * @return 订单实体
     * @throws BusinessException 当库存不足时抛出
     */
    OrderEntity createOrder(OrderCreateCommand command);
}
```

### 2.3 异常处理检查

**规则**: 异常必须明确、有意义,避免捕获后不处理

**检查项**:
- ✅ 业务异常: 使用自定义 `BusinessException`
- ✅ 异常信息: 明确说明错误原因
- ✅ 异常捕获: 必须有处理逻辑 (记录日志、转换、重新抛出)
- ❌ 禁止空 catch 块
- ❌ 禁止捕获 `Exception` 后不做任何处理

**示例**:
```java
// ✅ 正确: 明确的业务异常
public void deductStock(Long productId, Integer quantity) {
    if (quantity <= 0) {
        throw new BusinessException("扣减数量必须大于0");
    }
    // ...
}

// ❌ 错误: 捕获后不处理
try {
    orderService.createOrder(command);
} catch (Exception e) {
    // 空处理
}
```

### 2.4 事务管理检查

**规则**: 事务边界必须在 Application 层,Domain 层不得有事务注解

**检查项**:
- ✅ Application 层方法上有 `@Transactional`
- ❌ Domain 层不得有 `@Transactional`
- ✅ 事务传播行为明确 (`REQUIRED`, `REQUIRES_NEW` 等)

**示例**:
```java
// ✅ 正确: Application 层控制事务
@Service
public class OrderApplicationService {
    @Transactional(rollbackFor = Exception.class)
    public OrderVO createOrder(OrderCreateCommand command) {
        // ...
    }
}

// ❌ 错误: Domain 层有事务注解
public class OrderService implements IOrderService {
    @Transactional  // 禁止
    public OrderEntity createOrder(OrderCreateCommand command) {
        // ...
    }
}
```

---

## 三、设计模式选型审查规则（新增）

**规则**: 设计模式的引入必须满足"信号"且不触发"否决条件"，默认不用。目的是防止过度工程化（ROI 原则），同时避免该抽象的地方用 if-else 堆砌。

| 场景信号 | 触发考虑的模式 | 否决条件（满足任一则不用） |
|---|---|---|
| 同一 Gateway/Service 存在 2 个以上互斥算法或渠道实现，且步骤结构相似、仅个别步骤不同（如支付宝/微信支付回调验签流程） | 模板方法模式 | 只有 1 种实现；或未来 6 个月内无第二种实现的明确计划 |
| 领域服务核心流程固定，某几步需按运行时条件切换行为（如库存扣减：预扣减 vs 直接扣减） | 策略模式 | 分支数 ≤ 2 且逻辑简单，`if-else` 可读性更高 |
| Repository/Gateway 需要按配置切换具体后端实现 | 工厂模式 | Domain 层已通过接口解耦，Spring 按 `@Profile`/`@ConditionalOnProperty` 注入即可满足，无需额外工厂类 |
| 前端组件存在 3 个以上相似但细节不同的展示变体（如订单卡片按状态显示不同操作按钮） | 组合/插槽模式（Vue slots） | 变体 ≤ 2 且未来无扩展计划，直接用 `v-if` 分支 |

**通用否决规则**: 审查时若无法回答"如果不用这个模式，未来大概率会具体怎么改坏"，一律判定为不需要引入，按 `CLAUDE_project_guide_v2.md` 中"输出前自查③ROI原则"处理。

**示例**:
```java
// ✅ 正确: 满足信号（支付宝/微信验签流程高度相似）且无法简单否决，使用模板方法
public abstract class AbstractPayCallbackHandler {
    public final void handle(String rawBody) {
        verifySign(rawBody);
        PayResult result = parseResult(rawBody);
        updateOrderStatus(result);
    }
    protected abstract void verifySign(String rawBody);
    protected abstract PayResult parseResult(String rawBody);
    private void updateOrderStatus(PayResult result) { /* 共用逻辑 */ }
}

// ❌ 错误: 仅有 1 种库存扣减方式，仍引入策略模式接口 + 工厂 + 3 个实现类，属于过度工程化
public interface IStockDeductStrategy { void deduct(Long productId, Integer qty); }
public class StockDeductStrategyFactory { /* 只有一个实现，纯属多余的抽象层 */ }
```

---

## 四、安全性审查规则

### 4.1 参数校验检查

**规则**: 所有外部输入必须校验,防止非法参数

**检查项**:
- ✅ Trigger 层: 使用 `@Valid` 校验 DTO
- ✅ 必填字段: 使用 `@NotNull`, `@NotBlank`
- ✅ 格式校验: 使用 `@Pattern`, `@Email`, `@Size` 等
- ✅ 业务校验: 在 Domain 层进行

**示例**:
```java
// ✅ 正确: Trigger 层参数校验
@PostMapping("/login")
public Response<UserLoginVO> login(@Valid @RequestBody UserLoginRequestDTO request) {
    // ...
}

public class UserLoginRequestDTO {
    @NotBlank(message = "用户名不能为空")
    private String username;
    
    @NotBlank(message = "密码不能为空")
    @Size(min = 6, max = 20, message = "密码长度必须在6-20位之间")
    private String password;
}
```

### 4.2 敏感信息保护

**规则**: 敏感信息不得明文存储、不得记录到日志

**检查项**:
- ✅ 密码: 必须加密存储 (BCrypt)
- ✅ 支付密钥: 使用环境变量注入
- ✅ 日志: 不得记录密码、支付信息等敏感数据
- ❌ 禁止硬编码密钥、密码

**示例**:
```java
// ✅ 正确: 环境变量注入
@Value("${alipay.private-key}")
private String privateKey;

// ❌ 错误: 硬编码
private String privateKey = "MIIEvgIBADANBgkqhkiG9w0BAQEFAASC...";
```

### 4.3 SQL 注入防护

**规则**: 使用参数化查询,禁止字符串拼接 SQL

**检查项**:
- ✅ MyBatis: 使用 `#{}` 参数绑定
- ❌ 禁止使用 `${}` 拼接 SQL (除非必要且已转义)

**示例**:
```xml
<!-- ✅ 正确: 参数绑定 -->
<select id="selectByOrderNo" resultType="OrderEntity">
    SELECT * FROM order_main WHERE order_no = #{orderNo}
</select>

<!-- ❌ 错误: SQL 拼接 -->
<select id="selectByOrderNo" resultType="OrderEntity">
    SELECT * FROM order_main WHERE order_no = '${orderNo}'
</select>
```

### 4.4 MQ Listener 审查规则

**规则**: Trigger 层的 MQ Listener (消息消费者) 必须遵循以下规范，确保消息可靠处理。

#### 4.4.1 消费幂等性

**检查项**:
- ✅ 消费者必须实现消息幂等性 (防止重复消费)
- ✅ 使用消息唯一标识 (`messageId`) 作为幂等键
- ✅ 数据库唯一索引或 Redis SETNX 保证幂等性

**示例**:
```java
// ✅ 正确: 消费前检查幂等性
@RocketMQMessageListener(
    topic = "order-topic",
    consumerGroup = "order-consumer-group",
    selectorExpression = "*"
)
public class OrderListener implements RocketMQListener<Message> {
    @Autowired
    private IIdempotentGateway idempotentGateway;

    @Override
    public void onMessage(Message message) {
        String messageId = message.getMessageId();
        // 幂等性检查
        if (!idempotentGateway.checkAndLock(messageId)) {
            log.info("消息已处理，跳过: {}", messageId);
            return;
        }
        try {
            processMessage(message);
        } finally {
            idempotentGateway.unlock(messageId);
        }
    }
}

// ❌ 错误: 缺少幂等性检查
public class OrderListener implements RocketMQListener<Message> {
    @Override
    public void onMessage(Message message) {
        processMessage(message);  // 重复消息会导致重复处理
    }
}
```

#### 4.4.2 异常处理与重试

**检查项**:
- ✅ 消费异常必须抛出 `RuntimeException` 触发重试
- ✅ 设置合理的重试次数 (建议 3 次)
- ✅ 超过重试次数后进入死信队列 (DLQ)
- ✅ 记录失败消息便于排查

**示例**:
```java
// ✅ 正确: 异常重试 + 死信队列
@RocketMQMessageListener(
    topic = "order-topic",
    consumerGroup = "order-consumer-group"
)
public class OrderListener implements RocketMQListener<Message> {
    private static final int MAX_RETRY = 3;

    @Override
    public void onMessage(Message message) {
        try {
            processMessage(message);
        } catch (Exception e) {
            log.error("消息处理失败, messageId: {}, retryCount: {}",
                message.getMessageId(), getRetryCount(message), e);
            // 抛出异常触发重试
            throw new RuntimeException("消息处理失败", e);
        }
    }
}
```

#### 4.4.3 死信队列 (DLQ) 处理

**检查项**:
- ✅ 必须配置死信队列接收失败消息
- ✅ 定期检查和处理死信队列
- ✅ 死信消息必须有告警通知

**死信队列配置**:
```yaml
rocketmq:
  consumer:
    # 死信队列主题
    deadLetterTopic: order-topic-dlq
    # 最大重试次数
    maxRetryTimes: 3
```

#### 4.4.4 消费顺序性

**检查项**:
- ✅ 有顺序要求的场景必须使用顺序消息
- ✅ 使用 `MessageQueueSelector` 保证消息路由到同一队列
- ⚠️ 顺序消息吞吐量较低，按需使用

---

## 五、性能优化审查规则

### 5.1 数据库查询优化

**检查项**:
- ✅ 避免 N+1 查询: 使用 JOIN 或批量查询
- ✅ 分页查询: 大数据量必须分页
- ✅ 索引使用: 查询条件字段必须有索引
- ❌ 禁止 `SELECT *`: 明确指定需要的字段

### 5.2 缓存使用检查

**检查项**:
- ✅ 热点数据缓存: 如商品信息、分类信息
- ✅ 缓存过期时间: 设置合理的 TTL
- ✅ 缓存穿透防护: 空值也缓存
- ✅ 缓存更新策略: 主动更新或过期更新

### 5.3 并发控制检查

**检查项**:
- ✅ 库存扣减: 使用 Redis 原子操作或分布式锁
- ✅ 幂等性设计: 防止重复提交
- ✅ 乐观锁: 更新时检查版本号

---

## 六、幂等性与防御性编程审查规则

### 6.1 接口重入检测 (幂等性检查)

**规则**: 所有触发状态变更的外部接口必须实现幂等性保护

**⚠️ 事务边界限制**:

> **【警告】** 幂等性锁的 Redis 操作 (`trySet`/`unlock`) **严禁纳入** `@Transactional` 事务作用域内。MySQL 事务回滚不会自动回滚 Redis 操作，可能导致 Redis 锁状态与数据库状态不一致。

**推荐策略**:

| 策略 | 说明 | 适用场景 |
|------|------|---------|
| finally 块释放锁 | 在 `@Transactional` 方法内使用 `finally` 块释放锁，Spring AOP 在方法退出后提交事务，锁释放先于事务提交 | 低并发业务 |
| 事务外执行锁操作 | 将幂等锁的获取和释放放在 `@Transactional` 方法外部（推荐） | 高并发业务 |
| 分布式事务 (TCC/Saga) | 使用分布式事务框架协调 Redis 和 MySQL | 强一致性要求 |

**幂等锁正确示例**:

```java
// ✅ 正确: 锁操作在事务外 (推荐)
@Service
public class OrderApplicationService {
    @Autowired
    private IIdempotentGateway idempotentGateway;
    @Autowired
    private OrderDomainService orderDomainService;  // 领域服务（无事务注解）

    // 外层方法：无事务注解，负责幂等锁
    public OrderVO createOrder(OrderCreateRequestDTO request) {
        // 1. 幂等性检查 (事务外)
        if (!idempotentGateway.checkAndLock(request.getRequestId())) {
            throw new BusinessException("请求重复，请稍后重试");
        }
        try {
            // 2. 调用带事务的内层方法
            return createOrderWithTransaction(request);
        } finally {
            // 3. 释放锁 (事务提交后执行)
            idempotentGateway.unlock(request.getRequestId());
        }
    }

    // 内层方法：带事务注解
    @Transactional(rollbackFor = Exception.class)
    public OrderVO createOrderWithTransaction(OrderCreateRequestDTO request) {
        // 执行业务逻辑 (事务内)
        OrderEntity order = orderDomainService.createOrder(request);
        return OrderAssembler.toVO(order);
    }
}
```

**检查项**:
- ✅ RequestDTO 必须包含幂等键 (`requestId` 或 `orderNo`，命名以 `API_CONTRACT.md` 为准)
- ✅ Application 层在调用 Domain 服务前必须进行幂等性校验
- ✅ 使用 Redis SETNX 或数据库唯一索引实现幂等性
- ✅ 幂等键必须具有足够的随机性 (UUID 或雪花算法)

**幂等键要求**:
| 场景 | 推荐幂等键 | 说明 |
|------|-----------|------|
| 创建订单 | `orderNo` | 业务唯一单号 |
| 支付操作 | `requestId` + `orderNo` | 请求ID + 订单号 |
| 库存扣减 | `requestId` | 外部请求唯一标识 |
| 消息消费 | `messageId` | MQ 消息唯一标识 |

**示例**:
```java
// ✅ 正确: RequestDTO 包含幂等键
public class OrderCreateRequestDTO {
    @NotNull(message = "请求ID不能为空")
    private String requestId;  // 幂等键

    private List<OrderItemDTO> items;
    // ...
}

// ✅ 正确: Application 层进行幂等性校验 (使用 finally 确保锁释放)
@Service
public class OrderApplicationService {
    @Autowired
    private IIdempotentGateway idempotentGateway;

    @Transactional(rollbackFor = Exception.class)
    public OrderVO createOrder(OrderCreateRequestDTO request) {
        // 1. 幂等性检查 (事务外)
        if (!idempotentGateway.checkAndLock(request.getRequestId())) {
            throw new BusinessException("请求重复，请稍后重试");
        }
        try {
            // 2. 执行业务逻辑 (事务内)
            OrderEntity order = orderService.createOrder(command);
            return OrderAssembler.toVO(order);
        } finally {
            // 3. 释放锁 (finally 确保无论成功还是失败都释放锁)
            idempotentGateway.unlock(request.getRequestId());
        }
    }
}

// ❌ 错误: 缺少幂等性检查
@Service
public class OrderApplicationService {
    @Transactional(rollbackFor = Exception.class)
    public OrderVO createOrder(OrderCreateRequestDTO request) {
        // 直接执行，没有幂等性保护
        OrderEntity order = orderService.createOrder(command);
        return OrderAssembler.toVO(order);
    }
}
```

### 6.2 外部调用防御性检查

**规则**: 所有外部系统调用 (Gateway) 必须添加超时时间和降级处理

**检查项**:
- ✅ HTTP 调用: 设置连接超时和读取超时 (`connectTimeout`, `readTimeout`)
- ✅ RPC 调用: 设置合理的超时时间
- ✅ 数据库操作: 设置查询超时 (`statementTimeout`)
- ✅ 降级处理: 外部服务不可用时要有兜底方案
- ✅ 熔断机制: 高频失败时自动熔断

**超时时间参考**:
| 操作类型 | 建议超时时间 | 说明 |
|----------|-------------|------|
| HTTP 请求 | 1-5 秒 | 根据网络状况调整 |
| 数据库查询 | 1-3 秒 | 复杂查询可适当延长 |
| Redis 操作 | 500 毫秒 | 缓存操作应快速响应 |
| MQ 发送 | 1 秒 | 消息发送不应阻塞主流程 |

**示例**:
```java
// ✅ 正确: 外部调用添加超时和降级处理
@Component
public class WeixinGatewayImpl implements IWeixinGateway {
    private final OkHttpClient httpClient = new OkHttpClient.Builder()
        .connectTimeout(5, TimeUnit.SECONDS)  // 连接超时
        .readTimeout(10, TimeUnit.SECONDS)    // 读取超时
        .build();
    
    public String getAccessToken(String appId, String appSecret) {
        try {
            Request request = new Request.Builder()
                .url(buildTokenUrl(appId, appSecret))
                .get()
                .build();
            
            try (Response response = httpClient.newCall(request).execute()) {
                if (!response.isSuccessful()) {
                    throw new BusinessException("微信API调用失败");
                }
                return parseAccessToken(response.body().string());
            }
        } catch (SocketTimeoutException e) {
            log.warn("微信API调用超时: {}", e.getMessage());
            throw new BusinessException("微信服务暂时不可用，请稍后重试");
        } catch (IOException e) {
            log.error("微信API调用异常: {}", e.getMessage(), e);
            throw new BusinessException("微信服务暂时不可用，请稍后重试");
        }
    }
}

// ❌ 错误: 缺少超时设置
@Component
public class WeixinGatewayImpl implements IWeixinGateway {
    private final OkHttpClient httpClient = new OkHttpClient();  // 默认无超时
    
    public String getAccessToken(String appId, String appSecret) {
        // 无超时控制，可能导致线程阻塞
        Request request = new Request.Builder()
            .url(buildTokenUrl(appId, appSecret))
            .get()
            .build();
        
        Response response = httpClient.newCall(request).execute();  // 可能无限等待
        return parseAccessToken(response.body().string());
    }
}
```

### 6.3 集合操作空值检查

**规则**: 所有集合操作必须进行判空处理，防止 `NullPointerException`

**检查项**:
- ✅ `Collection.isEmpty()`: 判断集合是否为空
- ✅ `Optional.ofNullable()`: 安全处理可能为 null 的对象
- ✅ `CollectionUtils.isEmpty()`: Apache Commons 工具类
- ✅ 空集合返回: 方法返回集合时，返回空集合而非 null

**示例**:
```java
// ✅ 正确: 集合操作进行判空处理
import java.util.Optional;
import org.apache.commons.collections4.CollectionUtils;

public class OrderService implements IOrderService {
    public List<OrderItem> getItems(Long orderId) {
        // 使用 Optional 安全处理
        return Optional.ofNullable(orderRepository.findItems(orderId))
            .orElse(Collections.emptyList());
    }
    
    public void addItems(Long orderId, List<OrderItem> items) {
        // 使用 CollectionUtils 判空
        if (CollectionUtils.isEmpty(items)) {
            throw new BusinessException("订单商品列表不能为空");
        }
        // ...
    }
}

// ❌ 错误: 缺少空值检查
public class OrderService implements IOrderService {
    public List<OrderItem> getItems(Long orderId) {
        List<OrderItem> items = orderRepository.findItems(orderId);
        return items;  // items 可能为 null
    }
    
    public void addItems(Long orderId, List<OrderItem> items) {
        for (OrderItem item : items) {  // items 可能为 null，抛出 NPE
            // ...
        }
    }
}
```

---

## 七、前端代码审查规则

### 7.1 TypeScript 类型检查

**规则**: 必须使用强类型,避免 `any`

**检查项**:
- ✅ 接口定义: 从 `API_CONTRACT.md` 派生 TypeScript 接口，不从后端代码反推
- ✅ 类型导入: 使用 `import type`
- ❌ 禁止使用 `any` 类型

### 7.2 API 调用规范

**规则**: API 调用必须统一封装,错误统一处理

**检查项**:
- ✅ API 封装: 在 `api/` 目录下统一管理，禁止 `repositories/` 与 `api/` 重复封装同一端点（见现状说明，本次重构需清理重叠层）
- ✅ 错误处理: 统一拦截器处理错误
- ✅ 参数验证: 调用前验证必要参数

**示例**:
```typescript
// ✅ 正确: API 封装
// api/order.ts
export function createOrder(data: OrderCreateRequest): Promise<OrderVO> {
  return request.post('/api/v1/orders', data)
}

// 使用
const order = await createOrder({ productId: 123, quantity: 1 })

// ❌ 错误: 直接调用
axios.post('/api/v1/orders', data)
```

### 7.3 组件设计规范

**检查项**:
- ✅ 单一职责: 一个组件只负责一个功能
- ✅ Props 验证: 定义 Props 类型
- ✅ 事件命名: 使用 `on` 前缀,如 `onClick`, `onSubmit`

### 7.4 前端 DDD 架构映射

**规则**: 前端逻辑也应尽可能按照业务模块划分，并对齐后端的 Facade 定义。

**重要性**: Vue3 + TS 项目最容易烂尾的地方就是 `any` 的泛滥和 API 的混乱。加上"组件不直接消费 Response"这一条，能帮你省下大量改动数据结构的麻烦。

#### 7.4.1 模块化目录结构

**规则**: `src/views/` 和 `src/api/` 下必须按照业务模块划分目录。

**目录结构示例**:

```
s-pay-mall-front/
├── src/
│   ├── api/                          # API 层（唯一封装层，repositories/ 与 services/ 中的重复封装本次一并清理）
│   │   ├── auth/                    # 对应后端 IAuthFacade
│   │   │   ├── types.ts             # 认证相关类型定义（从 API_CONTRACT.md 派生）
│   │   │   └── index.ts             # 认证 API 封装
│   │   ├── mall/                    # 对应后端 IMallProductFacade
│   │   │   ├── types.ts
│   │   │   └── index.ts
│   │   ├── order/                   # 对应后端 IMallOrderFacade
│   │   │   ├── types.ts
│   │   │   └── index.ts
│   │   └── admin/                   # 对应后端 IAdminFacade
│   │       ├── types.ts
│   │       └── index.ts
│   ├── views/
│   │   ├── auth/
│   │   ├── mall/
│   │   ├── order/
│   │   └── admin/
```

> **现状清理项**：审计发现 `src/repositories/`（如 `cartRepository` 用 bare fetch）、`src/services/adminUserService.ts` 与 `api/` 层功能重叠。本次重构目标是**只保留 `api/` 一层**，`repositories/`、`services/` 中的逻辑迁移合并后删除，禁止保留"两套封装并存"的状态。

#### 7.4.2 类型安全

**规则**: `API_CONTRACT.md` 中定义的每个字段必须有对应的 TypeScript Interface 字段，严禁在组件中直接使用 `any` 或 JSON 对象字面量，严禁声明契约文件中不存在的字段。

**审查要点**:
- ✅ 必须定义 TypeScript 接口，与 `API_CONTRACT.md` 逐字段对应
- ✅ 使用 `type` 或 `interface` 关键字
- ❌ 禁止使用 `any` 类型
- ❌ 禁止直接使用 JSON 对象字面量
- ❌ 禁止声明契约文件中不存在的字段（即"臆造字段"）

**类型定义示例**:

```typescript
// ✅ 正确: 与 API_CONTRACT.md「三、对象字段契约表」逐字段对应
// api/order/types.ts
export interface OrderVO {
  orderNo: string;
  totalAmount: number;
  status: number;
  items: OrderItemVO[];
  createTime: string;
}

export interface OrderCreateRequest {
  requestId: string;  // 幂等键，见契约文件
  productId: number;
  quantity: number;
}
```

```typescript
// ❌ 错误: 使用 any 或 JSON 对象字面量
const order: any = await fetch('/api/v1/orders/123')
```

#### 7.4.3 数据流转规范

**规则**: API 层必须处理后端返回的响应包装（拆解 `Response<T>`），组件层仅直接使用 `T` 数据。

**审查要点**:
- ✅ API 层负责解构响应，提取 `data` 字段
- ✅ 组件层直接使用干净的 VO 对象
- ❌ 组件层禁止直接消费 `Response<T>` 包装对象
- ❌ 禁止在组件中多次解构响应
- ❌ **禁止**因后端字段与前端类型不一致而在 API 层做"映射层强行对齐"（如原 `cartRepository.ts` 将 `price` 映射为 `productPrice`）——发现字段不一致时，应回到 `API_CONTRACT.md` 修正命名并同步改双端，而不是加一层映射掩盖问题

**数据流转示例**:

```typescript
// api/order/index.ts
import type { OrderVO, OrderCreateRequest } from './types'
import request from '@/utils/request'

export async function getOrder(orderNo: string): Promise<OrderVO> {
  const res = await request.get<Response<OrderVO>>(`/orders/${orderNo}`)
  return res.data
}

export async function createOrder(data: OrderCreateRequest): Promise<OrderVO> {
  const res = await request.post<Response<OrderVO>>('/orders', data)
  return res.data
}
```

```vue
<!-- ✅ 正确: 组件层直接使用干净的 VO -->
<template>
  <div v-for="order in orders" :key="order.orderNo">
    <span>{{ order.orderNo }}</span>
    <span>{{ order.totalAmount }}</span>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { getOrderList } from '@/api/order'
import type { OrderVO } from '@/api/order/types'

const orders = ref<OrderVO[]>([])

async function loadOrders() {
  orders.value = await getOrderList()
}
</script>
```

#### 7.4.4 前端类型与后端字段映射来源

**规则**: 前端 TypeScript 类型必须与 `API_CONTRACT.md` 的「三、对象字段契约表」一一对应，**不再以后端 VO 代码作为参照源**（因为后端 VO 代码本身可能是待整改的历史遗留状态，见「零、契约核心检查」）。

**字段命名规范**:
- ✅ 前后端统一 camelCase（见 `CLAUDE_project_guide_v2.md`"全局序列化与命名策略"决议）
- ❌ 禁止出现 snake_case 字段（历史遗留 `category_id` 等，本次重构一并清理，双端同步改名）
- ❌ 禁止混用 snake_case 和 camelCase

---

## 八、测试审查规则

### 8.1 单元测试检查

**检查项**:
- ✅ Domain 层: 必须有单元测试
- ✅ 测试覆盖率: 核心业务逻辑覆盖率 > 80%
- ✅ 测试命名: `test{方法名}_{场景}`
- ✅ Mock 使用: 外部依赖使用 Mock

### 8.2 集成测试检查

**检查项**:
- ✅ API 测试: 测试主要业务流程
- ✅ 数据准备: 测试前准备测试数据
- ✅ 数据清理: 测试后清理测试数据

---

## 九、文档审查规则

### 9.1 API 文档检查

**检查项**:
- ✅ 接口说明: 每个接口必须有说明，且与 `API_CONTRACT.md` 对应条目一致
- ✅ 参数说明: 请求参数必须有注释
- ✅ 返回值说明: 响应字段必须有注释
- ✅ 错误码说明: 列出可能的错误码

### 9.2 代码注释检查

**检查项**:
- ✅ 类注释: 说明类的职责
- ✅ 方法注释: 说明方法的功能、参数、返回值
- ✅ 复杂逻辑注释: 解释实现思路
- ✅ TODO 注释: 标记待完成的功能

---

## 十、Git 提交规范

### 10.1 提交信息格式

**格式**: `<type>(<scope>): <subject>`

**类型**:
- `feat`: 新功能
- `fix`: 修复 Bug
- `refactor`: 重构
- `docs`: 文档更新（含 `API_CONTRACT.md` 变更）
- `test`: 测试相关
- `chore`: 构建/工具相关

**示例**:
```
feat(order): 实现订单超时自动关闭功能
fix(payment): 修复支付宝回调验签失败问题
refactor(user): 重构用户登录逻辑,提取领域服务
docs(contract): 统一 ProductVO.categoryId 命名，更新 API_CONTRACT.md
```

### 10.2 代码变更规范

**检查项**:
- ✅ 单次提交: 一个提交只做一件事
- ✅ 提交粒度: 适中,不要过大或过小
- ✅ 若涉及字段改名，`API_CONTRACT.md` 的更新必须与代码改动在同一提交或紧邻提交中完成，不得滞后
- ❌ 禁止提交无关代码
- ❌ 禁止提交敏感信息 (密码、密钥等)

---

## 十一、审查清单

每次代码审查必须检查以下项目:

### 契约核对（新增，优先检查）
- [ ] 涉及的接口/字段是否已在 `API_CONTRACT.md` 中有对应条目？
- [ ] 字段命名是否与契约文件逐字一致？
- [ ] 是否存在 `@JsonProperty` snake_case 转换（应判 P0）？
- [ ] 前端是否存在契约文件中不存在的臆造字段？

### 架构合规性
- [ ] 依赖方向是否正确 (Trigger → Application → Domain ← Infrastructure)
- [ ] Domain 层是否依赖了技术框架 (允许标准 Java 类库和校验注解)
- [ ] 业务逻辑是否在 Domain 层实现
- [ ] Trigger 层是否包含业务逻辑
- [ ] Infrastructure 层是否包含业务逻辑

### 触发层 (MQ Listener)
- [ ] Controller 是否只做参数校验和 DTO 转换？
- [ ] 是否存在业务逻辑混在 Controller 中？
- [ ] MQ Listener 是否实现了消费幂等性？
- [ ] MQ Listener 异常是否正确处理并触发重试？
- [ ] 是否配置了死信队列 (DLQ) 处理失败消息？

### 基础设施层模块化 (Infrastructure Partitioning)
- [ ] Infrastructure 层是否按模块（auth, mall, order）拆分，且符合对称性？
- [ ] Infrastructure 层是否避免了跨领域模块的直接耦合？
- [ ] 是否存在跨模块的类引用（如 MallRepositoryImpl 依赖 OrderRepositoryImpl）？
- [ ] `infrastructure/shared/` 是否仅包含跨领域共用技术实现？

### 设计模式选型（新增）
- [ ] 是否存在满足信号但未使用对应模式的场景（如多渠道验签流程仍用大段 if-else）？
- [ ] 是否存在不满足信号却引入了设计模式的过度工程化情况？

### 代码质量
- [ ] 命名是否符合规范
- [ ] 注释是否完整
- [ ] 异常处理是否合理
- [ ] 事务管理是否正确

### 安全性
- [ ] 参数校验是否完整
- [ ] 敏感信息是否保护
- [ ] SQL 注入防护是否到位

### 性能
- [ ] 数据库查询是否优化
- [ ] 是否存在 `SELECT *` 查询 (必须明确指定字段)
- [ ] 缓存使用是否合理
- [ ] 并发控制是否到位

### 幂等性与防御性编程
- [ ] RequestDTO 是否包含幂等键 (`requestId` 或 `orderNo`)
- [ ] Application 层是否进行了幂等性校验
- [ ] 外部调用是否添加了超时时间
- [ ] 外部调用是否有降级处理
- [ ] 集合操作是否进行了判空处理

### 前端代码
- [ ] Vue 组件和 API 请求是否按业务模块（auth, mall, order）存放？
- [ ] 是否存在任何 `any` 类型定义？
- [ ] 组件是否直接消费 `Response<T>` 而不是解构后的数据？
- [ ] TypeScript 接口是否与 `API_CONTRACT.md` 一一对应？
- [ ] 是否存在 `repositories/`、`services/` 与 `api/` 重复封装同一端点？
- [ ] API 层是否统一处理响应包装？
- [ ] 路由守卫 (`beforeEach`) 是否正确配置？
- [ ] 未授权用户是否正确跳转到登录页？

### 测试
- [ ] 单元测试是否覆盖核心逻辑
- [ ] 测试用例是否合理

### 文档
- [ ] API 文档是否完整
- [ ] 代码注释是否清晰
- [ ] `API_CONTRACT.md` 是否与本次改动同步更新

---

## 十二、审查报告格式

**规则**: 所有代码审查必须以表格形式输出审查结果

### 报告格式

```
## 代码审查报告

| 问题位置 | 级别 | 违反规范 | 修复建议 |
|---------|------|---------|---------|
| 文件路径:行号 | P0/P1/P2 | 违反的具体规则 | 具体的修复建议 |
```

### 级别定义

| 级别 | 说明 | 处理要求 |
|------|------|---------|
| **P0** | 严重问题 | 必须修复才能合并 |
| **P1** | 一般问题 | 建议修复，不阻塞合并 |
| **P2** | 建议优化 | 可选优化 |

### 级别判定标准

**P0 (严重问题)**:
- 字段/接口命名与 `API_CONTRACT.md` 不一致（新增，优先于其他 P0 判定）
- 违反 DDD 架构原则（如 Domain 层依赖技术框架）
- 安全漏洞（如 SQL 注入、敏感信息泄露）
- 数据丢失风险（如事务管理错误）
- 性能严重问题（如无限循环、内存泄漏）
- 并发安全问题（如竞态条件导致数据不一致）

**P1 (一般问题)**:
- 代码规范问题（如命名不规范）
- 注释不完整或缺失
- 参数校验不完整
- 异常处理不完善
- 测试覆盖不足
- 不满足信号却引入设计模式（过度工程化）

**P2 (建议优化)**:
- 性能优化建议
- 代码重构建议
- 文档完善建议
- 代码可读性优化

### 报告示例

```
## 代码审查报告

| 问题位置 | 级别 | 违反规范 | 修复建议 |
|---------|------|---------|---------|
| s-pay-mall-api/src/main/java/cn/fcr/api/vo/ProductVO.java:12 | P0 | 使用 @JsonProperty("category_id") 转 snake_case，与 API_CONTRACT.md 命名策略不一致 | 移除 @JsonProperty，字段名改回 categoryId，同步核对契约文件 |
| s-pay-mall-domain/src/main/java/cn/fcr/domain/order/service/OrderService.java:45 | P0 | Domain 层依赖了 Spring 框架 (@Autowired) | 移除 @Autowired 注解，改为构造器注入或通过 Gateway 接口抽象依赖 |
| s-pay-mall-trigger/src/main/java/cn/fcr/trigger/controller/OrderController.java:78 | P1 | RequestDTO 缺少幂等键 requestId | 在 OrderCreateRequestDTO 中添加 requestId 字段并添加 @NotNull 校验，同步更新契约文件 |
| s-pay-mall-front/src/repositories/cartRepository.ts:20 | P1 | 与 api/mall 层重复封装购物车接口 | 迁移逻辑至 api/mall/index.ts，删除本文件 |
```

### 审查命令模板

**使用方式**: 在让 Claude 审查代码时，使用以下命令：

```
请严格对照 review.md 进行审查，优先核对 API_CONTRACT.md，并以表格形式输出：【问题位置】|【级别(P0/P1/P2)】|【违反规范】|【修复建议】。
```

---

**重要提示**: 本文件为最高优先级审查规则,所有代码审查必须严格遵循。字段/接口命名问题优先于架构问题被拦截。如发现违反规则的情况,必须在审查意见中明确指出并要求修复。