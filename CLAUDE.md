# Claude Code 项目上下文入口

本文件是 Claude Code 的自动加载入口，仅作路由，不重复业务内容。

## 强制阅读顺序

1. **AGENTS.md** — 项目概述（技术栈、模块结构、核心业务描述）
2. **DEVELOPMENT_GUIDE.md** — 技术契约（命名规范、DDD 约束、API 设计规范、AI 协作硬边界，强制执行）
3. **API_CONTRACT.md** — 接口契约（端点定义、DTO 类型映射，新增/修改接口代码前必须核对）

## 使用规则

- 任何代码生成前，先确认 API_CONTRACT.md 中是否存在对应契约条目；不存在则禁止生成代码。
- 设计模式默认不引入，仅在 DEVELOPMENT_GUIDE.md 决策表触发信号时使用。
- 本文件不维护业务内容，如需更新项目概述，编辑 AGENTS.md。
