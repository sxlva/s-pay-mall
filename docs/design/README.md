# s-pay-mall 商城系统 - 架构设计与知识汇总

> 本文档为系统架构设计的完整知识集，遵循 DDD（领域驱动设计）分层原则，覆盖核心业务链路、技术亮点、演进对比及面试考点。

---

## 一、系统整体架构

### 1.1 技术栈

| 层级 | 技术选型 | 说明 |
|------|----------|------|
| 视图层 | Vue 3 + Element Plus + Vite | 前端框架 |
| 网关层 | Spring Cloud Gateway | API 网关 |
| 认证层 | JWT Token | 无状态认证 |
| 后端框架 | Spring Boot 2.7.x | 后端框架 |
| 持久层 | MyBatis-Plus + MySQL 8.0 | 数据访问 |
| 缓存层 | Redis (Lettuce) + Redisson | 缓存与分布式锁 |
| 消息队列 | Apache RocketMQ 5.x | 异步消息 |
| 微信生态 | 微信公众号登录 + 微信支付 | 社交登录与支付 |
| 支付宝生态 | 支付宝当面付 | 支付渠道 |
| 构建工具 | Maven 多模块 | 依赖管理 |

### 1.2 DDD 分层架构

```mermaid
flowchart TB
    subgraph 触发层 [Trigger Layer]
        T1[Controller<br/>REST 接口]
        T2[Listener<br/>RocketMQ 消费者]
        T3[Job<br/>定时任务]
    end

    subgraph 应用层 [Application Layer]
        A1[AppService<br/>应用编排]
        A2[DTO/Command/Query<br/>数据传输]
        A3[Config<br/>基础设施装配]
    end

    subgraph 领域层 [Domain Layer - 核心]
        D1[Entity/ValueObject<br/>领域对象]
        D2[DomainService<br/>领域服务]
        D3[Gateway接口<br/>外部依赖抽象]
        D4[Repository接口<br/>持久化抽象]
        D5[Event<br/>领域事件]
    end

    subgraph 基础设施层 [Infrastructure Layer]
        I1[GatewayImpl<br/>外部服务实现]
        I2[RepositoryImpl<br/>DAO 封装]
        I3[DAO/MyBatis<br/>数据访问]
        I4[Redis/Redisson<br/>缓存与分布式锁]
        I5[RocketMQ<br/>消息中间件]
    end

    T1 --> A1
    T2 --> A1
    T3 --> A1
    A1 --> D2
    A1 --> D1
    D2 --> D3
    D2 --> D4
    D3 -.实现.-> I1
    D4 -.实现.-> I2
    I2 --> I3
    I1 --> I4
    I1 --> I5
```

**依赖规约（强约束）：**
- 依赖方向严格单向：`Trigger → Application → Domain ← Infrastructure`
- 领域层不依赖任何具体技术框架（Spring、Redis、MQ 注解均不得出现在领域层）
- 基础设施层通过实现 Domain 层的接口完成"反向依赖注入"

### 1.3 模块结构设计

```mermaid
flowchart LR
    A[s-pay-mall商城系统]
    
    subgraph 用户模块
        B1[用户注册]
        B2[用户登录]
        B3[个人中心]
        B4[微信扫码登录]
        B5[第三方登录绑定]
    end
    
    subgraph 商品模块
        C1[商品分类]
        C2[商品浏览]
        C3[商品搜索]
        C4[商品详情]
    end
    
    subgraph 购物车模块
        D1[添加购物车]
        D2[修改数量]
        D3[删除商品]
        D4[清空购物车]
        D5[购物车列表]
    end
    
    subgraph 订单模块
        E1[创建订单]
        E2[订单列表]
        E3[订单详情]
        E4[取消订单]
        E5[超时关单]
    end
    
    subgraph 支付模块
        F1[微信支付]
        F2[支付宝支付]
        F3[支付回调]
        F4[支付成功处理]
    end
    
    subgraph 管理员模块
        G1[用户管理]
        G2[商品管理]
        G3[订单管理]
        G4[分类管理]
    end
    
    subgraph 权限模块
        H1[角色管理]
        H2[权限管理]
        H3[用户角色分配]
    end
    
    A --> 用户模块
    A --> 商品模块
    A --> 购物车模块
    A --> 订单模块
    A --> 支付模块
    A --> 管理员模块
    A --> 权限模块
```

### 1.4 业务流程全景图

```mermaid
flowchart LR
    用户 --> 微信扫码
    微信扫码 --> A1[模块: 登录]
    A1 --> A2[模块: 商品]
    A2 --> A3[模块: 购物车]
    A3 --> A4[模块: 订单创建]
    A4 --> A5[模块: 库存预扣]
    A5 --> A6[模块: 支付]
    A6 --> A7[模块: 异步履约]
    A7 --> A8[模块: 库存同步]
    
    A7 -.延时消息.-> A9[模块: 超时关单]
```

---

## 二、数据库设计

### 2.1 ER 关系图

```mermaid
erDiagram
    mall_user ||--o{ user_binding : has
    mall_user ||--o{ cart_item : has
    mall_user ||--o{ order_main : creates
    mall_user }o--|| user_role : assigned_to
    user_role }o--|| role : includes
    role }o--|| role_permission : has
    role_permission }o--|| permission : includes
    
    category ||--o{ product : contains
    product ||--o{ cart_item : in
    product ||--o{ order_item : in
    
    order_main ||--|| pay_order : relates_to
    order_main ||--o{ order_item : contains
```

### 2.2 核心表结构

| 表名 | 说明 | 关键字段 |
|------|------|----------|
| mall_user | 用户表 | id, username, password, status |
| user_binding | 第三方绑定表 | user_id, identity_type, identifier |
| role | 角色表 | id, role_code, role_name |
| permission | 权限表 | id, perm_code, perm_name |
| user_role | 用户角色关联表 | user_id, role_id |
| role_permission | 角色权限关联表 | role_id, permission_id |
| category | 商品分类表 | id, name, status |
| product | 商品表 | id, category_id, name, price, stock |
| cart_item | 购物车表 | user_id, product_id, quantity |
| order_main | 订单主表 | order_no, user_id, total_amount, status |
| order_item | 订单明细表 | order_id, product_id, price, quantity |
| pay_order | 支付订单表 | order_id, status, pay_url, pay_time |

---

## 三、核心业务模块

### 3.1 登录鉴权模块

#### 3.1.1 微信扫码登录流程

```mermaid
sequenceDiagram
    autonumber
    participant User as 用户浏览器
    participant F as 前端 Vue
    participant C as LoginController
    participant S as WeixinLoginService
    participant A as WeixinApiService
    participant Cache as Guava Cache
    participant WX as 微信API服务器

    User->>F: 访问 /login 页面
    F->>C: GET /api/v1/login/weixin_qrcode_ticket
    C->>S: createQrCodeTicket()
    S->>A: getAccessToken(appId, appSecret)
    A->>Cache: get("wx:token:appid")
    
    alt 缓存命中
        Cache-->>A: accessToken
    else 缓存未命中
        A->>WX: HTTP GET /cgi-bin/token
        WX-->>A: { access_token, expires_in }
        A->>Cache: put("wx:token:appid", token, 7000s)
    end
    
    A->>WX: HTTP POST /cgi-bin/qrcode/create
    WX-->>A: { ticket }
    A-->>S: ticket
    S-->>C: ticket
    C-->>F: Response<String>(ticket)
    F->>User: 渲染二维码
```

#### 3.1.2 扫码回调与轮询机制

```mermaid
sequenceDiagram
    autonumber
    participant WX as 微信客户端
    participant WXAPI as 微信服务器
    participant Portal as WeixinPortalController
    participant Service as WeixinLoginService
    participant Cache as Guava Cache
    participant F as 前端
    participant C as LoginController

    WX->>WXAPI: 扫描二维码
    WXAPI->>Portal: POST /api/v1/weixin/portal/receive (XML)
    Portal->>Service: saveLoginState(ticket, openid)
    Service->>Cache: put("wx:login:ticket:"+ticket, openid, 180s)
    Portal-->>WXAPI: "success"

    loop 每 3 秒轮询
        F->>C: GET /api/v1/login/check_login?ticket=xxx
        C->>Service: checkLogin(ticket)
        Service->>Cache: getIfPresent("wx:login:ticket:"+ticket)
        alt 已扫码
            Cache-->>Service: openid
            Service-->>C: openid
            C-->>F: Response<String>(openid)
            F->>F: 停止轮询，跳转主页
        else 未扫码
            Cache-->>Service: null
            Service-->>C: null
            C-->>F: Response<String>(未登录)
        end
    end
```

### 3.2 订单支付模块

#### 3.2.1 创建支付订单

```mermaid
sequenceDiagram
    autonumber
    participant User as 用户
    participant C as AliPayController
    participant S as OrderService
    participant R as OrderRepository
    participant Redis as Redisson (库存)
    participant DB as MySQL
    participant Ali as 支付宝SDK

    User->>C: POST /api/v1/alipay/create_pay_order
    C->>S: createOrder(shopCartEntity)
    
    S->>R: queryUnPayOrder(userId, productId)
    R->>DB: SELECT * FROM pay_order WHERE status='pay_wait'
    
    alt 存在未支付订单
        DB-->>R: 已有订单
        R-->>S: OrderEntity
        S-->>C: 返回原 payUrl
    else 无未支付订单
        R->>Redis: deductStock(productId, 1)
        alt 库存不足
            Redis-->>R: STOCK_INSUFFICIENT
            R-->>S: 异常向上抛
            S-->>C: 业务异常
        else 库存充足
            Redis-->>R: remainingStock
            R->>DB: INSERT INTO pay_order (status=CREATE)
            R->>Ali: pageExecute(request)
            Ali-->>R: form HTML
            R->>DB: UPDATE pay_order SET pay_url=?, status='pay_wait'
            R-->>S: payOrderEntity
        end
    end
    
    S-->>C: payOrderEntity
    C-->>User: Response<String>(payUrl)
```

#### 3.2.2 支付回调与异步履约

```mermaid
sequenceDiagram
    autonumber
    participant Ali as 支付宝服务器
    participant C as AliPayController
    participant V as RSA2 验签组件
    participant S as OrderService
    participant R as OrderRepository
    participant MQ as RocketMQ
    participant L as OrderPaidListener
    participant WeChat as 微信网关

    Ali->>C: POST /api/v1/alipay/alipay_notify_url
    C->>V: rsa256CheckContent(params, alipayPublicKey)
    
    alt 验签失败
        V-->>C: false
        C-->>Ali: HTTP 400
    else 验签成功
        C->>C: 检查 trade_status == 'TRADE_SUCCESS'
        C->>S: changeOrderPaySuccess(orderId)
        S->>R: changeOrderPaySuccess(orderId)
        R->>DB: UPDATE pay_order SET status='pay_success'
        R->>MQ: convertAndSend("order_paid", PaySuccessMessage)
        R-->>S: void
        S-->>C: void
        C-->>Ali: "success"
        
        MQ->>L: onMessage(PaySuccessMessage)
        L->>S: mallOrderService.paySuccess(orderNo)
        L->>WeChat: sendPaymentSuccessNotification(openid, ...)
    end
```

### 3.3 库存服务模块

#### 3.3.1 库存预扣减流程

```mermaid
sequenceDiagram
    autonumber
    participant U as 用户
    participant C as OrderController
    participant S as OrderService
    participant StockSvc as StockService
    participant GW as IStockGateway
    participant R as Redisson (RAtomicLong)
    participant MQ as RocketMQ
    participant Listener as StockChangeListener

    U->>C: 下单请求
    C->>S: createOrder()
    S->>StockSvc: deductStock(productId, qty)
    StockSvc->>GW: deductStock(productId, qty)
    GW->>R: get(stockKey) [预检]
    
    alt 库存不足
        R-->>GW: currentStock < qty
        GW-->>StockSvc: throw STOCK_INSUFFICIENT
    else 预检通过
        GW->>R: addAndGet(-qty) [原子扣减]
        R-->>GW: remainingStock
        alt remainingStock < 0
            GW->>R: addAndGet(+qty) [回滚]
            GW-->>StockSvc: throw STOCK_INSUFFICIENT
        else 扣减成功
            GW-->>StockSvc: remainingStock
            S->>MQ: 发送 product-stock-change-topic
        end
    end
```

#### 3.3.2 库存同步流程

```mermaid
sequenceDiagram
    autonumber
    participant Producer as 库存变更 Producer
    participant MQ as RocketMQ
    participant Consumer as ProductStockChangeListener
    participant IdemGW as IIdempotentGateway
    participant R as Redisson (SETNX)
    participant StockGW as IStockGateway
    participant DB as MySQL

    Producer->>MQ: send(StockChangeMessageDTO)
    MQ->>Consumer: onMessage(message)
    Consumer->>IdemGW: tryAcquire("STOCK_CHANGE", messageId)
    IdemGW->>R: trySet(key, "PROCESSING", 24h)
    
    alt SETNX 失败（重复消费）
        R-->>IdemGW: false
        IdemGW-->>Consumer: skip
        Consumer-->>MQ: ACK
    else SETNX 成功（首次处理）
        R-->>IdemGW: true
        IdemGW-->>Consumer: continue
        Consumer->>StockGW: setStock(productId, newStock)
        StockGW->>R: set(stockKey, newStock)
        Consumer->>DB: UPDATE product SET stock=? WHERE id=?
        Consumer-->>MQ: ACK
    end
```

---

## 四、技术亮点综述

### 4.1 DDD 战略设计

| 设计要素 | 落地形式 | 价值体现 |
|----------|----------|----------|
| 限界上下文 | user / mall / order / auth 四 BC 隔离 | 业务边界清晰，团队可独立演进 |
| 聚合根 | `CreateOrderAggregate`、`OrderAggregate` | 强一致性的最小边界 |
| 端口与适配器 | `IWeChatGateway` / `IStockGateway` 接口 | 屏蔽外部依赖，领域可单测 |
| 领域事件 | `PaySuccessMessageEvent` | 跨上下文协作的解耦语言 |
| 仓储模式 | `IOrderRepository` + `OrderRepositoryImpl` | 屏蔽 MyBatis 持久化细节 |

### 4.2 RocketMQ 异步解耦

- **消息定位**：从进程内 `EventBus` 演进为分布式消息中间件
- **核心 Topic 划分**：
  - `order_paid`：支付成功事件
  - `product-stock-change-topic`：库存变更事件
  - `order-timeout-topic`：订单超时延时关单
- **核心收益**：主链路响应时长从 ~800ms 降至 ~120ms；后置业务（发货、通知、积分）故障不影响支付主流程

### 4.3 Redisson 分布式锁与原子计数器

- **库存预扣减**：`RAtomicLong.addAndGet()` 保证 Redis 端扣减的原子性
- **幂等闭环**：`RBucket.trySet()` 实现分布式 SETNX，配合 24h TTL 防死锁
- **双检查防超卖**：先 `get()` 预判 + 扣减后二次校验，处理并发穿透场景

### 4.4 幂等性闭环设计

| 层级 | 手段 | 适用场景 |
|------|------|----------|
| 接口层 | 唯一业务单号 + 数据库唯一索引 | 重复下单防御 |
| 缓存层 | Redisson `trySet` SETNX | MQ 消息重复消费 |
| 业务层 | 状态机校验（如 `CREATED → PAID`） | 支付回调乱序 |
| 数据层 | 乐观锁（`UPDATE ... WHERE stock >= ?`） | DB 库存最终一致 |

### 4.5 安全与高可用

- 支付回调 **RSA2 验签**（支付宝公钥验签）
- 异步通知必须回 `"success"`，否则 24h 内多次重试
- Redis 集群部署 + 密码独立配置（`${REDIS_PASSWORD}` 环境变量注入）
- 数据库连接池 + 线程池统一管理

---

## 五、升级对比：EventBus → RocketMQ

### 5.1 升级前架构（Guava EventBus）

```mermaid
flowchart TD
    A[支付成功回调] --> B[更新订单状态]
    B --> C[EventBus.post 事件]
    C --> D[发货服务]
    C --> E[通知服务]
    C --> F[积分服务]
    C --> G[会员服务]
```

**特点：**
- **同步执行**：事件在主线程中同步执行
- **单机局限**：只能在本 JVM 内传播
- **无重试机制**：失败即丢失
- **无消息持久化**：内存存储，服务重启丢失

### 5.2 升级后架构（Apache RocketMQ）

```mermaid
flowchart TD
    A[支付成功回调] --> B[更新订单状态]
    B --> C[RocketMQ 发送消息]
    C --> D[order_paid Topic]
    
    D --> D1[OrderPaidListener]
    D --> D2[库存变更Listener]
    D --> D3[通知Listener]
    
    D1 --> E1[发货服务]
    D1 --> E2[积分服务]
    D1 --> E3[会员权益服务]
```

**特点：**
- **异步解耦**：消息异步处理，不阻塞主流程
- **分布式支持**：支持多消费者、多生产者集群部署
- **消息持久化**：消息存储在磁盘，支持消息重放
- **重试机制**：失败自动重试，可配置重试次数
- **延时消息**：支持订单超时关闭等延时场景

### 5.3 核心对比表

| 对比项 | Guava EventBus | Apache RocketMQ |
|--------|---------------|----------------|
| **架构模式** | 单机同步 | 分布式异步 |
| **消息持久化** | 无 | 有（磁盘存储） |
| **重试机制** | 无 | 支持（可配置次数） |
| **延时消息** | 不支持 | 支持（延时投递） |
| **消息追踪** | 困难 | 支持（消息轨迹） |
| **集群消费** | 不支持 | 支持 |
| **顺序消息** | 不保证 | 支持（按分区有序） |
| **事务消息** | 不支持 | 支持（半消息） |
| **适用场景** | 单机、简单场景 | 分布式、微服务架构 |

### 5.4 延时消息实现订单超时关闭

```mermaid
sequenceDiagram
    participant CreateOrder as 创建订单
    participant RocketMQ as RocketMQ
    participant DelayListener as 延时监听器
    participant OrderService as 订单服务
    participant DB as 数据库

    CreateOrder->>RocketMQ: 发送延时消息（15分钟）
    Note over RocketMQ: 消息在延时 Topic 等待
    RocketMQ->>+DelayListener: 延时15分钟后投递
    DelayListener->>OrderService: handleTimeoutCloseOrder()
    OrderService->>+DB: 检查订单状态
    alt 未支付
        DB-->>OrderService: 状态未变
        OrderService->>DB: 更新为 CLOSED
        OrderService->>RocketMQ: 发送库存恢复消息
    else 已支付
        DB-->>OrderService: 状态已变
        Note over DelayListener: 跳过，不处理
    end
```

---

## 六、面试知识点汇总

### 6.1 微信登录相关

**Q1：微信扫码登录的原理是什么？**

A：用户访问登录页时，后端向微信服务器请求 `access_token` 和二维码 `ticket`，前端用 ticket 拼装二维码图片。用户扫码后，微信服务器回调我们配置的 URL，携带用户的 `openid`。前端通过轮询检查扫码状态，扫码成功后用 openid 作为登录凭证。

**Q2：为什么要缓存 Access Token？**

A：微信 API 对 `access_token` 有调用频率限制（2000次/分钟），且有效期为 2 小时。频繁请求会导致 API 被限流。缓存可以减少不必要的请求，同时在缓存未命中时才去获取新的 token。

**Q3：OpenID 和 UnionID 的区别？**

A：`OpenID` 是用户在每个公众号/应用下的唯一标识，不同应用间不同。`UnionID` 是用户在微信开放平台下的唯一标识，同一用户在不同应用间相同。需要绑定微信开放平台账号才能获取 UnionID。

### 6.2 支付相关

**Q4：支付接口如何保证幂等性？**

A：
1. 创建订单前查询是否存在未支付订单（user_id + product_id）
2. 使用订单号作为幂等键
3. 数据库唯一索引防止重复插入
4. 支付回调验签 + 状态检查

**Q5：为什么支付回调要验签？**

A：支付回调是外部系统（微信/支付宝）调用我们的接口。如果没有验签，攻击者可以伪造支付成功回调，导致用户未付款但订单变为已支付。验签使用平台公钥验证签名，确保回调确实来自官方服务器。

**Q6：支付成功后的异步处理为什么用 MQ？**

A：
1. **解耦**：支付回调需要快速响应（否则对方会重试），后续的发货、通知等操作通过消息异步处理
2. **可靠性**：MQ 支持消息持久化和重试，确保业务被正确处理
3. **性能**：支付接口快速返回，提升用户体验
4. **扩展性**：新增业务只需订阅消息，无需修改支付代码

### 6.3 RocketMQ 相关

**Q7：RocketMQ 的延时消息是如何实现的？**

A：RocketMQ 支持延时消息，通过将消息发送到延时队列实现。延时消息分为 18 个级别（1s、5s、10s...2h），消息在延时时间到达后才投递给消费者。对于更精确的延时需求，可以使用 RocketMQ 的定时消息特性。

**Q8：如何保证消息消费的幂等性？**

A：
1. **业务层面**：在消息处理逻辑中检查业务状态（如订单是否已处理）
2. **存储层面**：使用数据库唯一键或 Redis SETNX 记录已处理的消息 ID
3. **消息层面**：RocketMQ 支持消息去重，可以开启消息去重功能

**Q9：RocketMQ 的消息消费模式？**

A：
1. **集群消费**：一条消息只会被消费组中的一个消费者处理
2. **广播消费**：一条消息会被消费组中所有消费者处理
3. **顺序消费**：按消息发送顺序消费（需配合分区顺序）

### 6.4 DDD 相关

**Q10：DDD 的分层架构是怎样的？**

A：
- **Trigger Layer（触发层）**：接收外部请求，如 Controller、Listener
- **Application Layer（应用层）**：编排领域服务，处理 DTO 转换
- **Domain Layer（领域层）**：核心业务逻辑、实体、值对象、领域服务
- **Infrastructure Layer（基础设施层）**：技术实现，如 DAO、网关实现、MQ

**Q11：仓储模式和 DAO 的区别？**

A：
- **DAO（Data Access Object）**：面向数据库表，操作数据库
- **Repository（仓储）**：面向领域对象，封装持久化逻辑
- Repository 内部可能调用多个 DAO，但对外只暴露领域对象

**Q12：为什么使用网关模式？**

A：网关模式将外部服务调用抽象为接口，便于：
1. 替换外部服务实现（如切换微信 SDK）
2. 统一处理外部调用异常
3. 模拟外部服务进行单元测试（Mock）

### 6.5 数据库相关

**Q13：订单表为什么要使用订单号作为业务主键？**

A：
1. **可读性**：订单号可包含时间、业务类型等信息，便于排查问题
2. **幂等性**：外部系统回调时携带订单号，作为业务标识
3. **分布式**：自增 ID 在分库分表场景下有局限性

**Q14：如何设计商品秒杀场景的库存扣减？**

A：
1. **数据库层面**：使用乐观锁（版本号）或悲观锁（SELECT FOR UPDATE）
2. **Redis 层面**：预扣减库存，异步同步到数据库
3. **MQ 层面**：使用 RocketMQ 事务消息确保一致性
4. **限流层面**：使用 Redis/Lua 脚本实现接口限流

---

## 七、系统设计亮点

### 7.1 高可用设计

| 设计点 | 实现方式 |
|--------|----------|
| 支付幂等性 | 订单号唯一索引 + 未支付订单查询 |
| MQ 消息幂等 | Redis SETNX 检查消息 ID |
| 订单超时关闭 | RocketMQ 延时消息 |
| 库存同步 | MQ 异步 + Redis 缓存 |
| 热点数据缓存 | Redis 分布式缓存 |

### 7.2 安全性设计

| 设计点 | 实现方式 |
|--------|----------|
| 密码加密 | BCrypt 单向哈希 |
| 支付验签 | RSA2 签名验证 |
| SQL 注入 | MyBatis-Plus 参数绑定 |
| XSS 攻击 | 请求参数校验 |
| 接口限流 | Redis + Lua 脚本 |

### 7.3 性能优化

| 优化点 | 实现方式 |
|--------|----------|
| Token 缓存 | Guava Cache 缓存 Access Token |
| 热点数据 | Redis 缓存商品、库存信息 |
| 异步处理 | RocketMQ 解耦非核心流程 |
| 连接池 | Druid/HikariCP 数据库连接池 |
| 线程池 | 异步任务线程池配置 |

---

## 八、项目结构说明

```
s-pay-mall/
├── s-pay-mall-trigger/          # 触发层（Controller、Listener）
├── s-pay-mall-app/              # 应用层（Service、DTO、Config）
├── s-pay-mall-domain/           # 领域层（Entity、VO、Gateway接口）
├── s-pay-mall-infrastructure/   # 基础设施层（DAO、Gateway实现、MQ）
├── s-pay-mall-api/              # API定义（接口、DTO）
└── docs/
    └── design/                   # 设计文档
        ├── README.md             # 本文件 - 架构设计与知识汇总
        ├── module-auth.md        # 登录模块详细设计
        ├── module-order-pay.md   # 订单支付模块详细设计
        ├── module-stock.md       # 库存模块详细设计
        ├── pay.md                # 支付流程快速参考
        └── weixinLogin.md        # 微信登录流程快速参考
```

---

## 九、版本与演进

| 版本 | 关键技术栈 | 重大变化 |
|------|------------|----------|
| v1.0 | Guava EventBus + 本地缓存 | 初始版本 |
| v2.0 | 引入 RocketMQ | 异步解耦、消息持久化 |
| v3.0（当前） | 引入 Redisson + 库存预扣减模型 | 防超卖、幂等闭环 |

---

## 十、答辩常见问题

1. **项目用了哪些设计模式？**
   - 答：仓储模式（Repository）、网关模式（Gateway）、工厂模式（Aggregate）、观察者模式（MQ）

2. **为什么要用 DDD 架构？**
   - 答：业务复杂度高，DDD 强调领域驱动设计，将业务逻辑与技术实现分离，提高代码可维护性和可扩展性

3. **RocketMQ 和 Kafka 的区别？**
   - 答：RocketMQ 适合电商场景，有延时消息、事务消息支持；Kafka 适合大数据日志场景，高吞吐量

4. **如何保证分布式事务一致性？**
   - 答：使用 RocketMQ 事务消息（半消息机制），本地事务 + MQ 消息投递的二维提交

5. **项目的技术难点是什么？**
   - 答：微信/支付宝支付回调的验签和幂等性处理；订单超时关闭的延时消息设计；多环境配置管理

---

> 最新更新：2026-06
