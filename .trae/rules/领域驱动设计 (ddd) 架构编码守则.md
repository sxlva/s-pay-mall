# DDD 架构编码守则（IDE 工作区规则）

> 本文件由 Trae IDE 自动加载。完整 DDD 架构规范的唯一真相源为 [DDD_ARCHITECTURE_SPEC.md](../../DDD_ARCHITECTURE_SPEC.md)。
>
> 本文件仅作快速提醒，不重复维护规则全文。审查时以 DDD_ARCHITECTURE_SPEC.md 为准。

## 1. 分层依赖（强制单向）

```
Trigger → Application → Domain ← Infrastructure
```

严禁逆向依赖。详见 [DDD_ARCHITECTURE_SPEC.md §1](../../DDD_ARCHITECTURE_SPEC.md)。

## 2. 各层职责（禁止跨界）

- **Trigger/User Interface 层**: 仅接收请求、参数校验、DTO 与 Command/Query 转换。**禁止编写业务逻辑或 Service**。
- **Application 层**: 编排领域服务和中转，不包含核心业务规则。
- **Domain 层**: 核心业务逻辑唯一所在地。所有 Domain Service 必须且只能定义在此层。
- **Infrastructure 层**: 仅负责数据库、缓存、消息队列等技术实现。**禁止编写核心业务逻辑**。

详见 [DDD_ARCHITECTURE_SPEC.md §2](../../DDD_ARCHITECTURE_SPEC.md)。

## 3. 修改规范

- **存量代码尊重原则**: 修改已有功能时，禁止重构或删除与当前需求无关的正常运行逻辑。
- **修改完备性**: 所有修改必须完整，禁止伪代码占位符。必须通盘修改所有受影响的依赖类。
- **静态检查**: 代码变更后必须确保项目能够通过编译。

AI Agent 行为规范见 [AI_AGENT_RULES.md](../../AI_AGENT_RULES.md)。
