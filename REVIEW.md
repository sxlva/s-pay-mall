# s-pay-mall 代码审查规则

> **版本**: v3.0 | **生效日期**: 2026-08-23
>
> **本文件职责**: Code Review / Architecture Review 的检查规范。回答"如何判断代码是否违反规则"，**不重新定义规则**。规则本身引用各自 SSOT。
>
> **规则来源**:
> - DDD 架构 → [DDD_ARCHITECTURE_SPEC.md](DDD_ARCHITECTURE_SPEC.md)
> - API 契约 → [API_CONTRACT.md](API_CONTRACT.md)
> - 命名/跨切面 → [DEVELOPMENT_GUIDE.md](DEVELOPMENT_GUIDE.md)
> - AI 行为 → [AI_AGENT_RULES.md](AI_AGENT_RULES.md)
> - 安全缺陷 → [SECURITY_ISSUES.md](SECURITY_ISSUES.md)

---

## 零、契约核心检查（最高优先级）

> 任何涉及接口路径、请求/响应字段、契约类（Req/Res）/TS interface 命名的新增或修改，必须先核对 [API_CONTRACT.md](API_CONTRACT.md)，该文件是唯一命名真相源。

**审查要点**:
- ✅ 新增接口：API_CONTRACT.md 中是否已存在对应条目？不存在 → 判 P0，要求先补契约再合并
- ✅ 字段命名：代码中的字段名是否与契约文件逐字一致（含大小写）？不一致 → 判 P0
- ❌ 禁止在 `api/vo` 包新增使用 `@JsonProperty` 进行 snake_case 转换的类；已存在的视为技术债，禁止扩展
- ❌ 禁止前端 TS interface 声明契约文件中不存在的字段（"臆造字段"，如 `stockStatus`）
- ✅ 代码与契约不一致时，默认以契约为准改代码；仅当契约本身有明显错误时才反向修改契约，且须注明原因

---

## 一、DDD 架构合规性审查

> 规则定义见 [DDD_ARCHITECTURE_SPEC.md](DDD_ARCHITECTURE_SPEC.md)。本节提供检查方法。

### 1.1 分层依赖检查

**检查方法**:

```
依赖方向必须严格单向: Trigger → Application → Domain ← Infrastructure
```

- ✅ 允许: Trigger 依赖 Application
- ✅ 允许: Application 依赖 Domain
- ✅ 允许: Infrastructure 依赖 Domain (实现接口)
- ❌ 禁止: Domain 依赖任何其他层
- ❌ 禁止: Infrastructure 依赖 Application 或 Trigger
- ❌ 禁止: Application 依赖 Infrastructure

> **现状说明**（见 [DDD_SPEC §1.1](DDD_ARCHITECTURE_SPEC.md)）: `s-pay-mall-application` 模块已于 2026-10-03 创建（TECH_DEBT P0-2），`OrderApplicationService`/`OrderTransactionService` 位于 `cn.fcr.application` 包，按 Application 层规则审查，不得放宽标准。

**Domain 层严禁出现的导入**（审查工具清单）:

```java
import org.springframework.*;              // Spring 框架
import org.apache.rocketmq.*;              // RocketMQ
import org.redisson.*;                     // Redisson
import org.springframework.data.redis.*;   // Redis
import org.apache.ibatis.*;                // MyBatis
```

### 1.2 各层职责边界检查

> 规则定义见 [DDD_SPEC §2](DDD_ARCHITECTURE_SPEC.md)。以下为审查时的常见违规模式。

**Trigger 层违规模式**:
- ❌ Controller 包含 `if (业务条件)` 业务判断
- ❌ Controller 直接调用 Domain 服务（绕过 Application 层）

**Application 层违规模式**:
- ❌ 包含核心业务规则（如 `if (items.size() > 100)` 数量判断应在 Domain）
- ❌ 直接操作数据库

**Domain 层违规模式**:
- ❌ 使用 `@Service`、`@Autowired`、`@Component`、`@Transactional`
- ❌ 导入 `org.springframework.**`

**Infrastructure 层违规模式**:
- ❌ 包含业务规则判断（如 `if (order.getStatus() == CREATED)` 修改业务状态）
- ❌ 使用 `@Transactional`（已知违规见 [DDD_SPEC §2.4](DDD_ARCHITECTURE_SPEC.md)，已登记 TECH_DEBT P0-3）

### 1.3 Infrastructure 模块对称性检查

> 规则定义见 [DDD_SPEC §4](DDD_ARCHITECTURE_SPEC.md)。

- ✅ `infrastructure/{module}/` 包下只能包含该 Domain 模块所需的实现
- ✅ 物理隔离: 不同模块代码不能直接相互调用
- ❌ 禁止跨模块类引用（如 `MallRepositoryImpl` 直接依赖 `OrderRepositoryImpl`）
- ❌ 禁止全局大杂烩包

---

## 二、代码质量审查

### 2.1 命名规范检查

> 规则定义见 [DEVELOPMENT_GUIDE.md §1](DEVELOPMENT_GUIDE.md)。

- ✅ 后端命名是否符合 §1.1 规范表
- ✅ 前端命名是否符合 §1.4 规范表
- ❌ 禁止 `UserLoginRequest`（应为 `LoginReq`，见 API_NAMING_DECISION）
- ❌ 禁止 `Map<String, Object>` 返回
- ❌ 禁止新增出线类使用 `@JsonProperty` snake_case（admin/res 历史类除外，见「零」）

### 2.2 注释完整性检查

> 规则定义见 [DEVELOPMENT_GUIDE.md §六](DEVELOPMENT_GUIDE.md)。

- ✅ 类注释: 说明类的职责和作用
- ✅ 接口注释: 说明接口的用途
- ✅ 方法注释: 说明功能、参数、返回值
- ✅ 复杂逻辑注释: 解释业务规则和实现思路
- ❌ 禁止删除既有注释（代码变更时同步更新而非删除）

### 2.3 异常处理检查

> 规则定义见 [DEVELOPMENT_GUIDE.md §四](DEVELOPMENT_GUIDE.md)。

- ✅ 业务异常使用 `BusinessException`，含错误码和描述
- ✅ 异常捕获后必须有处理逻辑（记录日志/转换/重新抛出）
- ❌ 禁止空 catch 块
- ❌ 禁止透传原始异常 message（信息泄漏）

### 2.4 事务管理检查

> 规则定义见 [DDD_ARCHITECTURE_SPEC.md §6](DDD_ARCHITECTURE_SPEC.md)。

- ✅ `@Transactional` 仅在 Application 层（`*ApplicationService` / `*TransactionService`）
- ❌ Domain 层不得有 `@Transactional`
- ❌ Infrastructure 层不得有 `@Transactional`（已知违规见 DDD_SPEC §2.4）
- ✅ Redis 幂等锁在事务外（`trySet`/`unlock` 不纳入 `@Transactional`）

---

## 三、设计模式选型审查

> 决策原则见 [AI_AGENT_RULES.md §12](AI_AGENT_RULES.md)。默认不引入，仅在满足"信号"且不触发"否决条件"时考虑。

| 场景信号 | 触发考虑的模式 | 否决条件（满足任一则不用） |
|---|---|---|
| 同一 Gateway/Service 存在 2 个以上互斥算法且步骤结构相似 | 模板方法 | 只有 1 种实现；或未来 6 个月内无第二种实现的明确计划 |
| 领域服务核心流程固定，某几步需按运行时条件切换 | 策略模式 | 分支数 ≤ 2 且逻辑简单，`if-else` 可读性更高 |
| Repository/Gateway 需按配置切换具体后端 | 工厂模式 | Spring 按 `@Profile`/`@ConditionalOnProperty` 注入即可满足 |
| 前端组件存在 3 个以上相似但细节不同的展示变体 | 组合/插槽模式（Vue slots） | 变体 ≤ 2 且未来无扩展计划，直接用 `v-if` 分支 |

**通用否决规则**: 若无法回答"如果不用这个模式，未来大概率会具体怎么改坏"，一律判定为不需要引入。

---

## 四、安全性审查

> 安全缺陷清单见 [SECURITY_ISSUES.md](SECURITY_ISSUES.md)。涉及鉴权/支付的审查必须先读该文件。

### 4.1 参数校验检查

- ✅ Trigger 层: 使用 `@Valid` 校验 DTO
- ✅ 必填字段: `@NotNull`、`@NotBlank`
- ✅ 格式校验: `@Pattern`、`@Size` 等

### 4.2 敏感信息保护

- ✅ 密码: BCrypt 加密存储
- ✅ 支付密钥: 环境变量注入
- ✅ 日志: 不得记录密码、支付信息
- ❌ 禁止硬编码密钥、密码

### 4.3 SQL 注入防护

- ✅ MyBatis: 使用 `#{}` 参数绑定
- ❌ 禁止 `${}` 拼接 SQL（除非必要且已转义）

### 4.4 MQ Listener 审查

> 消费规范见 [DEVELOPMENT_GUIDE.md §二](DEVELOPMENT_GUIDE.md)。

- ✅ 消费幂等性: 使用 `messageId` 作为幂等键 + SETNX
- ✅ 异常重试: 抛出 `RuntimeException` 触发重试，最多 3 次
- ✅ 死信队列: 每个 Topic 配置 DLQ
- ✅ 超时参数: `syncSend`/`convertAndSend` 设置超时（建议 3000ms）

---

## 五、性能审查

- ✅ 避免 N+1 查询: 使用 JOIN 或批量查询
- ✅ 分页查询: 大数据量必须分页
- ✅ 索引使用: 查询条件字段必须有索引
- ❌ 禁止 `SELECT *`: 明确指定需要的字段
- ✅ 热点数据缓存 + 合理 TTL
- ✅ 缓存穿透防护: 空值也缓存
- ✅ 库存扣减: Redis 原子操作或分布式锁
- ✅ 幂等性设计: 防止重复提交
- ✅ 乐观锁: 更新时检查版本号

---

## 六、幂等性与防御性编程审查

### 6.1 接口重入检测

> 幂等规范见 [DEVELOPMENT_GUIDE.md §二.3](DEVELOPMENT_GUIDE.md)。事务边界见 [DDD_SPEC §6](DDD_ARCHITECTURE_SPEC.md)。

- ✅ 状态变更 API 必须包含 `requestId` 幂等键（`@NotNull String`）
- ✅ Application 层在调用 Domain 服务前进行幂等性校验
- ✅ Redis SETNX 或数据库唯一索引实现幂等性
- ✅ 幂等锁在事务外执行（见 DDD_SPEC §6.2 正确模式）

**幂等键场景**:

| 场景 | 推荐幂等键 |
|------|-----------|
| 创建订单 | `orderNo` |
| 支付操作 | `requestId` + `orderNo` |
| 库存扣减 | `requestId` |
| 消息消费 | `messageId` |

### 6.2 外部调用防御性检查

- ✅ HTTP 调用: 设置连接超时和读取超时
- ✅ 数据库操作: 设置查询超时
- ✅ 降级处理: 外部服务不可用时要有兜底方案

**超时参考**:

| 操作类型 | 建议超时 |
|----------|---------|
| HTTP 请求 | 1-5 秒 |
| 数据库查询 | 1-3 秒 |
| Redis 操作 | 500 毫秒 |
| MQ 发送 | 1 秒 |

### 6.3 集合操作空值检查

- ✅ `Collection.isEmpty()` / `Optional.ofNullable()` / `CollectionUtils.isEmpty()`
- ✅ 方法返回集合时返回空集合而非 null

---

## 七、前端代码审查

> 类型映射见 [API_CONTRACT.md §五](API_CONTRACT.md)。命名规范见 [DEVELOPMENT_GUIDE.md §1.4-1.5](DEVELOPMENT_GUIDE.md)。

### 7.1 TypeScript 类型检查

- ✅ 接口定义从 API_CONTRACT.md 派生，不从后端代码反推
- ✅ 使用 `import type`
- ❌ 禁止 `any` 类型
- ❌ 禁止声明契约文件中不存在的字段

### 7.2 API 调用规范

- ✅ API 封装在 `api/` 目录统一管理
- ❌ 禁止 `repositories/` 与 `api/` 重复封装同一端点（现状清理项）
- ❌ 禁止组件直接调用 `axios`
- ✅ 错误处理: 统一拦截器

### 7.3 组件设计规范

- ✅ 单一职责
- ✅ Props 类型定义
- ✅ 事件命名: `on` 前缀（`onClick`、`onSubmit`）

### 7.4 前端 DDD 架构映射

- ✅ `src/views/` 和 `src/api/` 按业务模块划分目录，对齐后端业务模块
- ✅ TypeScript 接口与 API_CONTRACT.md 逐字段对应
- ✅ API 层负责解构 `Response<T>`，组件层直接使用 Res 对象
- ❌ 禁止组件直接消费 `Response<T>` 包装
- ❌ 禁止因字段不一致在 API 层做"映射层强行对齐"——应回到 API_CONTRACT.md 修正命名并同步改双端
- ✅ 前后端统一 camelCase（历史遗留 snake_case 视为技术债，禁止扩展）

---

## 八、测试审查

- ✅ Domain 层核心业务逻辑必须有单元测试
- ✅ 测试覆盖率: 核心逻辑 > 80%
- ✅ 测试命名: `test{方法名}_{场景}`
- ✅ Mock 外部依赖
- ✅ API 测试主要业务流程
- ✅ 测试前准备数据、测试后清理数据

---

## 九、文档审查

- ✅ 接口说明与 [API_CONTRACT.md](API_CONTRACT.md) 对应条目一致
- ✅ 请求参数、返回值、错误码有注释
- ✅ 类注释说明职责、方法注释说明功能
- ✅ API_CONTRACT.md 与本次改动同步更新

---

## 十、Git 提交审查

> 规范见 [DEVELOPMENT_GUIDE.md §五](DEVELOPMENT_GUIDE.md)。

- ✅ 单次提交只做一件事
- ✅ 涉及字段改名时，API_CONTRACT.md 更新与代码改动在同一/紧邻提交
- ❌ 禁止提交无关代码
- ❌ 禁止提交敏感信息

---

## 十一、审查清单

### 契约核对（优先检查）
- [ ] 涉及的接口/字段是否已在 API_CONTRACT.md 中有对应条目？
- [ ] 字段命名是否与契约文件逐字一致？
- [ ] 是否存在 `@JsonProperty` snake_case 转换（应判 P0）？
- [ ] 前端是否存在契约文件中不存在的臆造字段？

### 架构合规性（对照 [DDD_ARCHITECTURE_SPEC.md](DDD_ARCHITECTURE_SPEC.md)）
- [ ] 依赖方向是否正确 (Trigger → Application → Domain ← Infrastructure)
- [ ] Domain 层是否依赖了技术框架
- [ ] 业务逻辑是否在 Domain 层实现
- [ ] Trigger 层是否包含业务逻辑
- [ ] Infrastructure 层是否包含业务逻辑
- [ ] Infrastructure 层是否按模块对称拆分
- [ ] 是否存在跨模块类引用

### 触发层 (MQ Listener)
- [ ] Controller 是否只做参数校验和 DTO 转换
- [ ] 是否存在业务逻辑混在 Controller 中
- [ ] MQ Listener 是否实现了消费幂等性
- [ ] MQ Listener 异常是否正确处理并触发重试
- [ ] 是否配置了死信队列 (DLQ)

### 设计模式选型
- [ ] 是否存在满足信号但未使用对应模式的场景
- [ ] 是否存在不满足信号却引入了设计模式的过度工程化

### 代码质量
- [ ] 命名是否符合 [DEVELOPMENT_GUIDE.md §1](DEVELOPMENT_GUIDE.md) 规范
- [ ] 注释是否完整（含文件头/public 方法/实体字段）
- [ ] 是否删除了既有注释（禁止）
- [ ] 异常处理是否合理
- [ ] 事务管理是否正确

### 安全性（对照 [SECURITY_ISSUES.md](SECURITY_ISSUES.md)）
- [ ] 参数校验是否完整
- [ ] 敏感信息是否保护
- [ ] SQL 注入防护是否到位

### 性能
- [ ] 数据库查询是否优化
- [ ] 是否存在 `SELECT *` 查询
- [ ] 缓存使用是否合理
- [ ] 并发控制是否到位

### 幂等性与防御性编程
- [ ] 请求类（Req）是否包含幂等键
- [ ] Application 层是否进行了幂等性校验
- [ ] 幂等锁是否在事务外
- [ ] 外部调用是否添加了超时时间
- [ ] 外部调用是否有降级处理
- [ ] 集合操作是否进行了判空处理

### 前端代码
- [ ] Vue 组件和 API 请求是否按业务模块存放
- [ ] 是否存在任何 `any` 类型定义
- [ ] 组件是否直接消费 `Response<T>` 而非解构后的数据
- [ ] TypeScript 接口是否与 API_CONTRACT.md 一一对应
- [ ] 是否存在 `repositories/`、`services/` 与 `api/` 重复封装
- [ ] 路由守卫是否正确配置

### 测试
- [ ] 单元测试是否覆盖核心逻辑
- [ ] 测试用例是否合理

### 文档
- [ ] API 文档是否完整
- [ ] 代码注释是否清晰
- [ ] API_CONTRACT.md 是否与本次改动同步更新

---

## 十二、审查报告格式

所有代码审查必须以表格形式输出:

```
## 代码审查报告

| 问题位置 | 级别 | 违反规范 | 修复建议 |
|---------|------|---------|---------|
| 文件路径:行号 | P0/P1/P2 | 违反的具体规则 | 具体的修复建议 |
```

### 级别定义

| 级别 | 说明 | 处理要求 |
|------|------|---------|
| **P0** | 严重问题（命名/契约不一致、DDD 违规、安全漏洞、数据丢失风险、并发安全问题） | 必须修复才能合并 |
| **P1** | 一般问题（命名不规范、注释缺失、参数校验不完整、过度工程化） | 建议修复 |
| **P2** | 建议优化（性能、重构、文档、可读性） | 可选 |

### 审查命令模板

```
请严格对照 REVIEW.md 进行审查，优先核对 API_CONTRACT.md，并以表格形式输出：【问题位置】|【级别(P0/P1/P2)】|【违反规范】|【修复建议】。
```

---

## 十三、版本历史

| 版本 | 日期 | 变更 |
|------|------|------|
| v3.0 | 2026-08-23 | 重构：DDD 规则→引用 DDD_ARCHITECTURE_SPEC.md，命名→引用 DEVELOPMENT_GUIDE.md，Git→引用 DEVELOPMENT_GUIDE.md，AI 行为→引用 AI_AGENT_RULES.md。修复 CLAUDE_project_guide_v2.md 悬空引用（3 处）。保留审查 checklist/检查方法/报告格式 |
| v2.0 | 2026-07-04 | 新增契约核心检查、设计模式选型 |
| v1.0 | 2026-07-02 | 初始版本 |
