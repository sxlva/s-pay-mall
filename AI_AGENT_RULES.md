# s-pay-mall AI Agent 统一行为规范

> **版本**: v1.0 | **生效日期**: 2026-08-23 | **适用范围**: Trae / Claude Code / DeepSeek / 所有 AI Coding Agent
>
> **本文件是所有 AI Agent 的统一入口**。只定义 Agent 的行为流程、决策规则和 SSOT 索引，**不复制任何领域规则**。领域规则由各自 SSOT 维护，本文件仅引用。

---

## 1. 文档地位

本文件是 AI Agent 行为规范的**唯一真相源（SSOT）**。

- Agent 的阅读顺序、优先级判定、冲突处理流程 → **仅在本文件定义**
- DDD 架构规则 → 见 [DDD_ARCHITECTURE_SPEC.md](DDD_ARCHITECTURE_SPEC.md)
- API 契约 → 见 [API_CONTRACT.md](API_CONTRACT.md)
- 命名/跨切面规范 → 见 [DEVELOPMENT_GUIDE.md](DEVELOPMENT_GUIDE.md)
- 审查方法 → 见 [REVIEW.md](REVIEW.md)
- 安全缺陷 → 见 [SECURITY_ISSUES.md](SECURITY_ISSUES.md)
- 技术债 → 见 [docs/design_wait/TECH_DEBT_ROADMAP.md](docs/design_wait/TECH_DEBT_ROADMAP.md)

---

## 2. 强制阅读顺序

接到任何任务后，必须按此顺序阅读，未完成不得产出代码：

| 序号 | 文档 | 必读条件 |
|------|------|---------|
| 1 | **本文件**（AI_AGENT_RULES.md） | 始终必读 |
| 2 | [AGENTS.md](AGENTS.md) — 项目概述 | 始终必读 |
| 3 | [API_CONTRACT.md](API_CONTRACT.md) — 接口契约 | 涉及接口/字段时必读 |
| 4 | [DDD_ARCHITECTURE_SPEC.md](DDD_ARCHITECTURE_SPEC.md) — DDD 架构 | 涉及架构/新增类时必读 |
| 5 | [DEVELOPMENT_GUIDE.md](DEVELOPMENT_GUIDE.md) — 命名+跨切面规范 | 涉及编码时必读 |
| 6 | [SECURITY_ISSUES.md](SECURITY_ISSUES.md) — 安全缺陷 | 涉及鉴权/支付时必读 |
| 7 | [docs/design_wait/TECH_DEBT_ROADMAP.md](docs/design_wait/TECH_DEBT_ROADMAP.md) — 技术债 | 涉及重构时必读 |
| 8 | [docs/design/README.md](docs/design/README.md) — 架构知识集 | 涉及业务链路时必读 |

---

## 3. 规则优先级与冲突处理

### 3.1 优先级（从高到低）

```
1. 项目实际代码 / 当前事实          ← 最高，代码是最终事实
2. API_CONTRACT.md                  ← API SSOT
3. DDD_ARCHITECTURE_SPEC.md         ← DDD SSOT
4. DEVELOPMENT_GUIDE.md             ← 命名+跨切面规范 SSOT
5. AI_AGENT_RULES.md（本文件）       ← AI 行为 SSOT
6. SECURITY_ISSUES.md               ← 安全 SSOT
7. TECH_DEBT_ROADMAP.md             ← 技术债 SSOT
8. REVIEW.md                        ← 审查方法 SSOT
9. AGENTS.md / docs/design/*        ← 上下文，非规则
```

### 3.2 冲突处理规则

| 情况 | 处理方式 |
|------|---------|
| 文档与代码不符 | 标注 `DOCUMENT_CODE_MISMATCH` 并报告，**不擅自改代码掩盖问题**。若该不一致已登记在 TECH_DEBT，引用其 ID |
| 同级 SSOT 之间冲突 | **不得自行裁决**。报告冲突，在用户确认前不进行可能改变架构语义的修改 |
| SSOT 与非 SSOT 冲突 | 以 SSOT 为准 |
| 非 SSOT 文档间冲突 | 以更接近代码事实的为准 |
| 规则含义不确定 | 报告不确定点，向用户求证，不猜测 |

### 3.3 SSOT 判定方法

> 同一条规则只能存在一个权威来源。其他文档只能引用它。

| 规则类别 | 唯一来源 | 判定理由 |
|---------|---------|---------|
| DDD 分层/依赖/职责/对称性/事务 | DDD_ARCHITECTURE_SPEC.md | 架构规则的唯一定义点 |
| API/端点/DTO/VO/字段命名/类型映射/错误码 | API_CONTRACT.md | 自声明"唯一权威来源" |
| 命名规范/MQ/Redis/异常/Git | DEVELOPMENT_GUIDE.md | 编码跨切面规范 |
| AI Agent 行为/阅读顺序/冲突处理 | AI_AGENT_RULES.md（本文件） | Agent 行为流程 |
| 审查 checklist/检查方法/报告格式 | REVIEW.md | 审查方法论 |
| 安全缺陷清单 | SECURITY_ISSUES.md | 安全问题专项 |
| 技术债追踪 | TECH_DEBT_ROADMAP.md | 已知问题与修复计划 |
| 功能规划 | FUTURE_FEATURES.md | 待实现功能 |
| 项目概述/技术栈/模块结构 | AGENTS.md | 项目上下文 |

---

## 4. 修改代码前必须做什么

### 4.1 前置检查

```
1. 确认任务类型，按 §2 阅读对应 SSOT
2. 涉及接口/字段 → 核对 API_CONTRACT.md 是否存在对应条目
   - 不存在 → 禁止生成代码，先补契约
3. 涉及架构/新增类 → 核对 DDD_ARCHITECTURE_SPEC.md 分层依赖
4. 涉及命名 → 核对 DEVELOPMENT_GUIDE.md §1 命名规范
5. 涉及鉴权/支付 → 核对 SECURITY_ISSUES.md 是否有未修复缺陷
6. 涉及重构 → 核对 TECH_DEBT_ROADMAP.md 是否已登记
```

### 4.2 确认代码事实

- 修改前**必须读取目标文件**，理解现有代码
- 不得基于文档描述假设代码结构，以代码实际为准
- 发现文档与代码不符时，按 §3.2 处理

---

## 5. 修改代码时禁止做什么（硬边界）

| # | 禁止项 | 规则来源 |
|---|--------|---------|
| H1 | 禁止在 Domain 层添加 Spring 注解（`@Service`/`@Autowired`/`@Component`/`@Transactional`） | [DDD_SPEC §各层职责](DDD_ARCHITECTURE_SPEC.md) |
| H2 | 禁止在 Infrastructure 层编写业务逻辑 | [DDD_SPEC §各层职责](DDD_ARCHITECTURE_SPEC.md) |
| H3 | 禁止 Trigger 层绕过 Application 层直接调用 Domain 服务 | [DDD_SPEC §分层依赖](DDD_ARCHITECTURE_SPEC.md) |
| H4 | 禁止前端使用 `any` 类型 | [API_CONTRACT §七](API_CONTRACT.md) |
| H5 | 禁止前端组件直接消费 `Response<T>` 包装 | [API_CONTRACT §七](API_CONTRACT.md) |
| H6 | 禁止前端声明 API_CONTRACT.md 中不存在的字段 | [API_CONTRACT §七](API_CONTRACT.md) |
| H7 | 禁止前端 `api/vo` 包新增 `@JsonProperty` snake_case | [API_CONTRACT §七](API_CONTRACT.md) |
| H8 | 禁止跨领域模块直接耦合 | [DDD_SPEC §对称性](DDD_ARCHITECTURE_SPEC.md) |
| H9 | 禁止删除既有注释；代码变更时同步更新注释而非删除 | 本文件 |
| H10 | 禁止在未读取 SSOT 的情况下生成涉及接口/架构的代码 | 本文件 |

### 5.1 AI 不允许的操作

| 操作 | 原因 |
|------|------|
| 自行创建新的 Maven 模块 | 需人工评估模块依赖关系 |
| 删除 `deprecated` 代码 | 需先确认调用链完整迁移 |
| 合并/删除 API 端点 | 需确认前端同步改造 |
| 修改 Redis Key 格式 | 影响生产数据一致性 |
| 修改数据库表结构 | 需 DBA 评审 + 数据迁移方案 |

---

## 6. 修改完备性要求

- **存量代码尊重原则**: 修改已有功能时，禁止重构或删除与当前需求无关的正常运行逻辑
- **修改完备性**: 所有修改必须完整，禁止生成"// 保持原样"等伪代码占位符。修改了 Service 接口必须同步修改实现类
- **静态检查**: 代码变更后必须确保项目能够通过编译

---

## 7. 前后端联动修改协议

修改涉及前后端接口时，必须同步修改：

| 修改后端 | 同步修改前端 |
|---------|------------|
| DTO 新增/删除字段 | `types/domain/` 或 `api/{module}/types.ts` |
| 新增 Controller 端点 | `api/{module}/index.ts` 新增调用函数 |
| 修改响应字段名 | 前端所有引用该字段的组件 |
| 修改错误码 | 前端错误处理逻辑 |
| 修改 JSON 命名策略 | 全局检索前端字段读取 |

> 字段不一致时：回到 [API_CONTRACT.md](API_CONTRACT.md) 修正命名并同步改双端，**禁止加映射层掩盖问题**。

---

## 8. 测试要求

- Domain 层核心业务逻辑必须有单元测试
- 测试命名: `test{方法名}_{场景}`
- 外部依赖使用 Mock
- 修改后确保现有测试通过

---

## 9. 安全要求

涉及鉴权/支付的代码修改，**必须先读 [SECURITY_ISSUES.md](SECURITY_ISSUES.md)**。

当前未修复的安全缺陷（S-01 ~ S-05）涉及 JWT 密钥、认证绕过、异常信息泄漏。修改相关代码时不得引入新的安全风险。

---

## 10. Git 修改边界

- Git 提交格式见 [DEVELOPMENT_GUIDE.md §Git 规范](DEVELOPMENT_GUIDE.md)
- 单次提交只做一件事
- 涉及字段改名时，API_CONTRACT.md 的更新必须与代码改动在同一提交或紧邻提交中完成
- 禁止提交敏感信息（密码、密钥等）

---

## 11. Code Review 前置要求

提交代码前，对照 [REVIEW.md](REVIEW.md) 的审查清单自查。审查报告格式见 REVIEW.md §报告格式。

---

## 12. 设计模式 ROI 原则

设计模式默认**不引入**。仅在满足"信号"且不触发"否决条件"时考虑。详见 [REVIEW.md §设计模式选型](REVIEW.md)。

> 若无法回答"如果不用这个模式，未来大概率会具体怎么改坏"，一律判定为不需要引入。

---

## 13. Agent 执行流程

```
开始任务
  ↓
读取本文件（入口 + 阅读顺序 + 冲突处理）
  ↓
判断任务类型 → 按 §2 读取对应 SSOT
  ↓
读取 AGENTS.md（项目上下文）
  ↓
修改前检查（§4 前置检查 + §5 硬边界）
  ↓
实施（遵守 DDD/API/命名 SSOT）
  ↓
测试（§8）
  ↓
Review（对照 REVIEW.md checklist）
  ↓
报告
```

---

## 14. 版本历史

| 版本 | 日期 | 变更 |
|------|------|------|
| v1.0 | 2026-08-23 | 初始版本：从 DEVELOPMENT_GUIDE §八 + REVIEW §十一 收敛 AI 行为规则，建立 Agent 统一入口 |
