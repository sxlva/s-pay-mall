# s-pay-mall 项目状态摘要（2025-06-20）

## Domain 层 Spring 注解已全部移除

所有 `@Service`、`@Component`、`@Resource` 已从 Domain 层删除。Domain 类现在是纯 POJO，所有 Bean 注册移至 Infrastructure 层的手动装配配置。

## Bean 注册策略

### DomainServiceConfig（统一注册入口）

**路径**: `s-pay-mall-infrastructure/src/main/java/cn/fcr/infrastructure/config/DomainServiceConfig.java`

注册全部 12 个 Domain Service Bean：

| Bean | 依赖 |
|---|---|
| `WeixinLoginService` | `IWeChatGateway`, `IWechatLoginGateway`, `ITokenProvider` |
| `WeixinBindService` | `IWeChatTokenRepository` |
| `MallCartServiceImpl` | `ICartRepository`, `IProductRepository`, `IDistributedLockService` |
| `MallProductServiceImpl` | `IProductRepository` |
| `MallUserServiceImpl` | `IUserRepository`, `IAuthTokenGateway`, `IOrderQueryGateway`, `IUserBindingGateway` |
| `MallStatisticsServiceImpl` | `IStatisticsRepository` |
| `OrderService` | `IOrderRepository`, `IProductGateway`, `IPaymentGateway` |
| `OrderStateMachineServiceImpl` | `IMallOrderQueryGateway`, `IPayOrderGateway`, `IStockGateway` |
| `MallOrderServiceImpl` | `IMallCartService`, `IMallOrderQueryGateway`, `IOrderPaymentGateway`, `IOrderStateMachineService`, `IStockGateway` |
| `DeductHandler` | `IIdempotentGateway`, `IStockGateway` |
| `AdminUpdateHandler` | `IIdempotentGateway`, `IStockGateway` |
| `RestoreHandler` | `IIdempotentGateway`, `IStockGateway` |

**不得在 s-pay-mall-app 层创建 Config 类来注册 Domain Bean**。Infrastructure 层依赖 Domain 层并实现其接口，有权实例化 Domain Service，App 层不应知道 Domain 具体实现类。

### Infrastructure 层实现类仍可使用 `@Component`/`@Repository`

- Gateway 实现 → `@Component`
- Repository 实现 → `@Repository`
- 这**不违反架构规范**，因为 Infrastructure 层实现 Domain 接口，使用 Spring 注解是合理的。

## 关键文件位置变更

| 旧文件 | 新文件 |
|---|---|
| `StrategyConfig.java`（已删除） | `DomainServiceConfig.java`（同包 `cn.fcr.infrastructure.config`） |

### 跨领域 Gateway 位置

`s-pay-mall-infrastructure/src/main/java/cn/fcr/infrastructure/shared/gateway/` 以下是跨领域桥接实现：
- `OrderQueryGatewayImpl.java` — 实现 `cn.fcr.domain.mall.gateway.IOrderQueryGateway`，桥接 mall 域与 order 域

## POM 依赖注意事项

`s-pay-mall-app/pom.xml` **必须同时包含** `s-pay-mall-trigger` 和 `s-pay-mall-domain`：

```xml
<dependency>
    <groupId>cn.fcr</groupId>
    <artifactId>s-pay-mall-trigger</artifactId>
</dependency>
<dependency>
    <groupId>cn.fcr</groupId>
    <artifactId>s-pay-mall-domain</artifactId>
</dependency>
```

缺失 trigger 会导致所有 Controller 404；缺失 domain 会导致依赖注入失败。

## 启动注意：Stale JAR 问题

`mvn spring-boot:run` 从 `s-pay-mall-app` 目录直接运行时，依赖的模块（如 domain）会从本地 Maven repo 加载 JAR，而不是使用 target/ 下的最新编译 class。

**如果修改了 Domain 层的类**（任何 `s-pay-mall-domain` 下的文件），必须在修改后执行：

```bash
mvn install -DskipTests -Dmaven.test.skip=true
```

或者从父项目使用 reactor 构建：

```bash
cd ..
mvn spring-boot:run -f s-pay-mall-app/pom.xml
```

## 已知遗留问题

### 1. 测试文件兼容性

`s-pay-mall-app/src/test/java/cn/fcr/test/OrderServiceTest.java` 已修复：
- `PayOrderEntity` import 路径：`cn.fcr.domain.shared.model.entity.PayOrderEntity`
- `ShopCartEntity` 使用了 Builder 模式（无 setter）

### 2. 启动所需环境变量

`.env` 文件中需要包含：

```bash
WEIXIN_TEMPLATE_ID=<value>
WEIXIN_TEMPLATE_ID_PAY_SUCCESS=<value>  # 容易遗漏
```

### 3. WeixinGatewayImpl 中的 `@Value`

```java
@Value("${weixin.config.template_id_pay_success}")
private String templateIdPaySuccess;
```

对应环境变量名为 `WEIXIN_TEMPLATE_ID_PAY_SUCCESS`。

## 端口

- 应用端口：`8092`（`application-dev.yml` 中配置）
- MySQL 端口：`23306`（如使用 Docker 映射）
- Redis 端口：`26379`
- RocketMQ 端口：`9876`
