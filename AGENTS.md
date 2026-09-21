# s-pay-mall 商城系统 - Codex 项目说明

## 项目概述

s-pay-mall 是一个基于 **领域驱动设计(DDD)** 架构的电商商城系统，采用前后端分离架构，支持微信扫码登录、支付宝支付、微信支付等核心电商功能。

## 技术栈

### 后端
- Spring Boot 2.7.12 / Java 17 / MySQL 8.0 + MyBatis-Plus
- Redis (Lettuce) + Redisson (分布式锁)
- Apache RocketMQ 5.x / JWT Token
- 支付宝当面付、微信支付 / Maven 多模块

### 前端
- Vue 3 + TypeScript / Element Plus 2.3.2
- Vite / Pinia / Axios

## 模块结构

```
s-pay-mall/
├── s-pay-mall-api/              # API层 - DTO、VO、IFacade 接口契约
├── s-pay-mall-app/              # 应用启动 - Spring Boot 入口 + 配置
├── s-pay-mall-domain/           # 领域层 - 核心业务逻辑（auth/mall/order）
├── s-pay-mall-infrastructure/   # 基础设施层 - 技术实现（auth/mall/order/shared）
├── s-pay-mall-trigger/          # 触发层 - Controller/Listener/Job
├── s-pay-mall-types/            # 通用类型层 - 全局Enum、错误码
└── s-pay-mall-front/            # 前端 Vue 3 项目
```

**DDD 依赖方向**: `Trigger → Application → Domain ← Infrastructure`

## 核心业务模块

| 模块 | 功能 | 关键领域 |
|------|------|---------|
| 用户 | 注册/登录、微信扫码、绑定 | `auth`, `mall/user` |
| 商品 | 分类、浏览、搜索 | `mall/product` |
| 购物车 | 增删改查、清空 | `mall/cart` |
| 订单 | 创建、列表、状态流转、超时关单 | `order`, `mall/order` |
| 支付 | 支付宝支付、回调、异步履约 | `mall/pay` |
| 管理 | 用户/商品/分类/订单 CRUD + 统计 | `admin` |

## 环境配置

```bash
DB_HOST=localhost        DB_PORT=3306        DB_NAME=s_pay_mall
REDIS_HOST=localhost     REDIS_PORT=6379
WECHAT_APP_ID=xxx       WECHAT_APP_SECRET=xxx
ALIPAY_APP_ID=xxx       ALIPAY_PRIVATE_KEY=xxx
```

## 常用命令

```bash
# 后端
cd s-pay-mall-app && mvn spring-boot:run

# 前端
cd s-pay-mall-front && npm install && npm run dev
```

## 规则体系（SSOT 索引）

> AI Agent 必须首先阅读 [AI_AGENT_RULES.md](AI_AGENT_RULES.md) 获取阅读顺序和冲突处理规则。

| 规则类别 | 唯一来源（SSOT） | 用途 |
|---------|----------------|------|
| AI Agent 行为 | [AI_AGENT_RULES.md](AI_AGENT_RULES.md) | 阅读顺序、优先级、冲突处理、修改边界 |
| DDD 架构 | [DDD_ARCHITECTURE_SPEC.md](DDD_ARCHITECTURE_SPEC.md) | 分层、依赖、职责、对称性、事务边界 |
| API 契约 | [API_CONTRACT.md](API_CONTRACT.md) | 端点、DTO/VO、字段命名、类型映射、错误码 |
| 命名/跨切面规范 | [DEVELOPMENT_GUIDE.md](DEVELOPMENT_GUIDE.md) | 命名、MQ、Redis、异常、Git、注释 |
| 代码审查 | [REVIEW.md](REVIEW.md) | checklist、检查方法、报告格式 |
| 安全缺陷 | [SECURITY_ISSUES.md](SECURITY_ISSUES.md) | S-01~S-05 安全问题清单 |
| 技术债 | [docs/design_wait/TECH_DEBT_ROADMAP.md](docs/design_wait/TECH_DEBT_ROADMAP.md) | 已知问题与修复计划 |
| 功能规划 | [docs/design_wait/FUTURE_FEATURES.md](docs/design_wait/FUTURE_FEATURES.md) | 待实现功能清单 |
| 架构知识集 | [docs/design/README.md](docs/design/README.md) | 业务链路图、模块文档导航 |
| 路由入口 | [CLAUDE.md](CLAUDE.md) | Claude Code 自动加载，路由到 AI_AGENT_RULES.md |

## 注释规范

> 详见 [DEVELOPMENT_GUIDE.md §六](DEVELOPMENT_GUIDE.md)。

- 文件头: `/** <职责> \n * @author 傅崇睿 */`
- public 方法: Javadoc/JSDoc，含参数和返回值说明
- 实体字段: `/** 含义说明 */`
- 语言: 中文，技术术语保留英文

## 开发者信息

- **开发者**: 傅崇睿（fuchongrui006@gmail.com）
- **组织**: fuchongrui
- **GitHub**: https://github.com/sxlva
