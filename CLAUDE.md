# s-pay-mall 商城系统 - Claude Code 项目说明

## 项目概述

s-pay-mall 是一个基于 **领域驱动设计(DDD)** 架构的电商商城系统,采用前后端分离架构,支持微信扫码登录、支付宝支付、微信支付等核心电商功能。

## 技术栈

### 后端技术栈
- **框架**: Spring Boot 2.7.12
- **Java版本**: Java 17
- **数据库**: MySQL 8.0 + MyBatis-Plus
- **缓存**: Redis (Lettuce) + Redisson (分布式锁)
- **消息队列**: Apache RocketMQ 5.x
- **认证**: JWT Token
- **支付**: 支付宝当面付、微信支付
- **构建工具**: Maven 多模块

### 前端技术栈
- **框架**: Vue 3 + TypeScript
- **UI组件**: Element Plus 2.3.2
- **构建工具**: Vite
- **状态管理**: Pinia
- **HTTP客户端**: Axios

## DDD 分层架构

本项目严格遵循 DDD 分层架构原则,依赖方向必须单向:

```
Trigger(触发层) → Application(应用层) → Domain(领域层) ← Infrastructure(基础设施层)
```

### 模块结构

```
s-pay-mall/
├── s-pay-mall-api/              # API层 - 接口契约定义
│   ├── dto/                     # 数据传输对象
│   ├── vo/                      # 视图对象
│   └── I*Facade                 # 外部接口定义
├── s-pay-mall-app/              # 应用启动模块 - Spring Boot 启动入口
│   ├── config/                  # 配置类 (Spring、Security、线程池等)
│   └── Application.java         # 启动类
├── s-pay-mall-domain/           # 领域层 - 核心业务逻辑
│   ├── auth/                    # 认证领域
│   ├── mall/                    # 商城领域
│   └── order/                   # 订单领域
├── s-pay-mall-infrastructure/   # 基础设施层 - 技术实现
│   ├── auth/                    # 认证领域实现 (对应 domain/auth)
│   ├── mall/                    # 商城领域实现 (对应 domain/mall)
│   ├── order/                   # 订单领域实现 (对应 domain/order)
│   └── shared/                  # 跨领域共用技术实现 (Redis/MQ基础配置)
├── s-pay-mall-trigger/          # 触发层 - 接口暴露
│   ├── controller/              # REST接口
│   ├── listener/                # MQ监听器
│   └── job/                     # 定时任务
├── s-pay-mall-types/            # 通用类型定义 (仅全项目通用 Enum、错误码)
└── s-pay-mall-front/            # 前端项目
```

### s-pay-mall-types 模块边界

`s-pay-mall-types` (通用类型层) 的职责边界:
- **允许存放**:
  - 全项目通用的 Enum (如 `OrderState`, `PayStatus`)
  - 全局错误码定义 (如 `ErrorCodes`)
  - 公共基础 VO (如 `PageVO`, `BaseVO`)
- **严禁存放**:
  - 任何业务领域逻辑
  - 业务相关的 DTO/VO (必须存放在对应模块的 `s-pay-mall-api` 目录下)
  - 与特定领域耦合的类型定义

**与 s-pay-mall-api 的区分**:
| 模块 | 存放内容 | 作用域 |
|------|---------|--------|
| `s-pay-mall-types` | 通用 Enum、错误码、基础 VO | 全项目共享 |
| `s-pay-mall-api` | 业务 DTO、VO、Facade 接口 | 按领域模块划分 |

### 各层职责

#### 1. API层 (s-pay-mall-api)
- **职责**: 定义外部接口契约,包含 DTO、VO、Facade 接口
- **命名规范**: 接口以 `I` 开头,以 `Facade` 结尾,如 `IAuthFacade`
- **约束**: 仅定义接口契约,不包含实现逻辑

#### 2. 应用层 (s-pay-mall-app)
- **职责**: 应用启动、配置管理、基础设施装配
- **包含**: Spring Boot 启动类、配置文件、安全配置、线程池配置等
- **约束**: 不包含业务逻辑

#### 3. 领域层 (s-pay-mall-domain) - **核心层**
- **职责**: 核心业务逻辑的唯一所在地
- **包含**:
  - Entity (实体): 具有唯一标识和生命周期
  - Value Object (值对象): 不可变的值
  - Domain Service (领域服务): 跨实体的业务逻辑
  - Gateway Interface (网关接口): 外部依赖的抽象
  - Repository Interface (仓储接口): 持久化抽象
- **约束**:
  - **严禁依赖任何具体技术框架** (Spring、Redis、MQ 注解不得出现)
  - 所有业务逻辑必须在此层实现
  - 通过接口定义外部依赖,由基础设施层实现

#### 4. 基础设施层 (s-pay-mall-infrastructure)
- **职责**: 技术实现,实现领域层定义的接口
- **包含**:
  - Repository 实现 (DAO 封装)
  - Gateway 实现 (外部服务集成)
  - Redis 操作
  - MQ 消息发送
- **约束**: **严禁在此层编写核心业务逻辑**

#### 5. 触发层 (s-pay-mall-trigger)
- **职责**: 接收外部请求,调用应用层服务
- **包含**: Controller、Listener、Job
- **约束**: 仅负责参数校验、DTO 转换,不包含业务逻辑

## 核心业务模块

### 用户模块
- 用户注册/登录
- 微信扫码登录
- 第三方账号绑定
- 个人中心

### 商品模块
- 商品分类管理
- 商品浏览/搜索
- 商品详情

### 购物车模块
- 添加购物车
- 修改数量
- 删除商品

### 订单模块
- 创建订单
- 订单列表/详情
- 订单状态流转
- 超时关单

### 支付模块
- 支付宝支付
- 微信支付
- 支付回调处理
- 异步履约

### 管理模块
- 用户管理
- 商品管理
- 订单管理
- 分类管理

## 编程规范

### 命名规范

#### 后端命名
- **API接口**: `I{Aggregate}Facade`, 如 `IAuthFacade`, `IOrderFacade`
- **DTO**: `{Action}RequestDTO`, 如 `UserLoginRequestDTO`
- **VO**: `{Entity}VO`, 如 `UserVO`, `OrderVO`
- **领域服务**: `I{Domain}Service`, 如 `IOrderService`
- **仓储接口**: `I{Entity}Repository`, 如 `IOrderRepository`
- **网关接口**: `I{Purpose}Gateway`, 如 `IStockGateway`
- **实体**: `{Name}Entity`, 如 `OrderEntity`

#### 前端命名
- **组件**: PascalCase, 如 `OrderListPage.vue`
- **API文件**: camelCase, 如 `order.ts`
- **类型定义**: PascalCase, 如 `interface OrderVO`

### 依赖注入规范
- **构造器注入优先**: 推荐使用构造器注入,明确依赖关系
- **避免字段注入**: 不使用 `@Autowired` 字段注入

### 异常处理
- **业务异常**: 抛出自定义业务异常,由全局异常处理器统一处理
- **技术异常**: 捕获并转换为业务异常或记录日志

### 事务管理
- **应用层控制**: 事务边界在应用层 Service 方法上声明
- **领域层无事务注解**: 领域层不使用 `@Transactional`

## 技术亮点

### 1. 库存预扣减 (Redisson RAtomicLong)
- 使用 Redis 原子操作进行库存预扣减
- 双重检查防止超卖
- 异步同步到数据库

### 2. 分布式幂等性
- 接口层: 唯一业务单号 + 数据库唯一索引
- 缓存层: Redisson `trySet` SETNX
- 业务层: 状态机校验
- 数据层: 乐观锁

### 3. RocketMQ 异步解耦
- 支付成功异步履约
- 库存变更消息
- 延时消息实现订单超时关闭

### 4. 微信扫码登录
- Access Token 缓存 (Guava Cache)
- 轮询机制检查登录状态
- OpenID 作为用户标识

## 环境配置

### 必需的环境变量
```bash
# 数据库配置
DB_HOST=localhost
DB_PORT=3306
DB_NAME=s_pay_mall
DB_USERNAME=root
DB_PASSWORD=your_password

# Redis配置
REDIS_HOST=localhost
REDIS_PORT=6379
REDIS_PASSWORD=your_redis_password

# 微信配置
WECHAT_APP_ID=your_app_id
WECHAT_APP_SECRET=your_app_secret

# 支付宝配置
ALIPAY_APP_ID=your_app_id
ALIPAY_PRIVATE_KEY=your_private_key
ALIPAY_PUBLIC_KEY=alipay_public_key
```

### 启动方式
```bash
# 后端启动
cd s-pay-mall-app
mvn spring-boot:run

# 前端启动
cd s-pay-mall-front
npm install
npm run dev
```

## 常见问题

### 1. 编译错误
- 确保 Java 版本为 17
- 检查 Maven 依赖是否完整
- 确认环境变量配置正确

### 2. Redis 连接失败
- 检查 Redis 服务是否启动
- 确认密码配置正确
- 检查网络连接

### 3. 支付回调失败
- 确认支付平台配置的回调地址正确
- 检查签名验证配置
- 查看日志确认回调是否到达

## 相关文档

- [架构设计文档](docs/design/README.md)
- [DDD架构编码守则](.trae/rules/领域驱动设计 (ddd) 架构编码守则.md)
- [项目记忆](~/.trae-cn/memory/projects/-Users-xiaolv-Develop-projects-backend-java-s-pay-mall/project_memory.md)

## 开发者信息

- **开发者**: fcr
- **邮箱**: fuchongrui006@gmail.com
- **组织**: fuchongrui
- **GitHub**: https://github.com/sxlva

---

**重要提示**: 在修改代码时,请务必遵循 DDD 架构规范和编码守则。所有业务逻辑必须在领域层实现,严禁在触发层或基础设施层编写业务逻辑。
