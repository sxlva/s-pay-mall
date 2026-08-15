# 未来功能规划表

> **创建日期**: 2026-07-02
> **说明**: 本文档记录待实现的功能需求、技术方案选型和风险点，供后续开发排期参考。

---

## 一、功能规划概览

| 优先级 | 功能 | 模块 | 预估工时 | 依赖 |
|-------|------|------|---------|------|
| P0 | 统一异常处理 | 全栈 | 3d | — |
| P0 | 枚举类规范重建 | 后端 Domain + Types | 2d | — |
| P0 | 参数校验全覆盖 | 后端 Trigger + 前端 | 3d | P0 统一异常处理 |
| P1 | 图片存储方案 (公开/隐私) | 后端 + Nginx | 5d | — |
| P1 | Nginx 反向代理图片请求 | 运维/部署 | 2d | P1 图片存储方案 |
| P1 | 数据迁移风险点方案 | 后端 DBA | 3d | — |
| P2 | 快递鸟 API 物流追踪 | 后端 + 前端 | 5d | — |

---

## 二、P0 级功能详述

### 2.1 统一异常处理

**现状**: 项目已有 `GlobalExceptionHandler` (`@RestControllerAdvice`) 和 `BusinessException` / `AppException`，但使用不统一——部分 Controller 方法直接抛出 `IllegalArgumentException`，部分返回裸字符串。

**改造目标**:

| 改造项 | 说明 |
|--------|------|
| 统一错误码枚举 | 创建 `ErrorCode` 枚举，覆盖：业务错误、参数校验错误、系统错误、第三方调用错误 |
| 统一异常类 | `BusinessException(code, message)` — 所有业务异常统一使用 |
| GlobalExceptionHandler 增强 | 捕获 `MethodArgumentNotValidException` → 返回字段级校验错误 |
| 响应格式统一 | 所有异常返回 `Response(code, info, null)` |
| 前端错误拦截 | Axios 响应拦截器统一 `error.message` 展示 |

**错误码规范**:

| 前缀 | 含义 | 示例 |
|------|------|------|
| `A0xxx` | 认证/授权 | `A0001` - Token 过期 |
| `B0xxx` | 业务错误 | `B0001` - 库存不足 |
| `C0xxx` | 参数校验 | `C0001` - 必填参数缺失 |
| `S0xxx` | 系统错误 | `S0001` - 数据库异常 |
| `T0xxx` | 第三方错误 | `T0001` - 支付宝调用失败 |

**涉及文件**:
- 新建 `s-pay-mall-types/.../ErrorCode.java` 枚举
- 改造 `GlobalExceptionHandler.java`
- 改造 `BusinessException.java` / `AppException.java`
- 前端 `src/utils/request.ts` 拦截器

---

### 2.2 枚举类规范重建

**现状**: 存在两套状态枚举体系，命名和值定义不一致:

| 旧系统 (order 包) | 新系统 (mall 包) | 问题 |
|-------------------|-----------------|------|
| `OrderStatusVO` | `OrderState` | CREATE vs INIT, DEAL_DONE vs SHIPPED/DONE |
| `PayStatus` (types 模块) | 硬编码字符串 "WAIT_PAY", "PAY_SUCCESS" | 枚举值不统一，部分使用字符串字面量 |

**改造目标**:

| 改造项 | 说明 |
|--------|------|
| 统一订单状态枚举 | 合并 `OrderStatusVO` + `OrderState` → `OrderStatus` (单一枚举，放 `s-pay-mall-types`) |
| 统一支付状态枚举 | 统一 `PayStatus` 枚举值，替换代码中硬编码字符串 |
| 枚举值定义规范 | `code(int) + desc(String)` 结构，所有枚举继承 `IEnum` 接口 |
| 枚举序列化规范 | Controller 返回 `{code, desc}` JSON 对象，MyBatis 自动映射 |
| 前端枚举同步 | 前端 `enums/` 目录维护同构枚举对象 |

**枚举值定义示例**:

```java
public enum OrderStatus implements IEnum<Integer> {
    CREATED (0, "已创建"),
    PAY_WAIT(1, "待支付"),
    PAID    (2, "已支付"),
    SHIPPED (3, "已发货"),
    DONE    (4, "已完成"),
    CLOSED  (5, "已关闭");
}
```

**涉及文件**:
- 新建 `s-pay-mall-types/.../enums/OrderStatus.java`
- 新建 `s-pay-mall-types/.../enums/PayStatus.java`
- 删除 `OrderStatusVO.java`, `OrderState.java`
- 改造所有状态判断逻辑 → 使用枚举比较
- 前端新建 `src/enums/order.ts` 等

---

### 2.3 参数校验全覆盖

**现状**: 11 个 Controller 中仅 2 个方法使用了 `@Valid` 校验。所有状态变更 API 缺少 `requestId` 幂等键。

**改造目标**:

| 改造项 | 说明 |
|--------|------|
| DTO 字段校验注解 | 所有 `RequestDTO` 类添加 `@NotNull`/`@NotBlank`/`@Size` 等 |
| Controller @Valid | 所有 `@RequestBody` 参数添加 `@Valid` |
| 自定义校验注解 | 如 `@Mobile`, `@OrderNo` 等业务校验器 |
| 分组校验 | 创建/更新使用不同校验分组 (`Create.class`, `Update.class`) |
| 幂等键补齐 | 所有状态变更 DTO 添加 `@NotNull String requestId` |
| 前端表单校验 | 对应前端表单添加 Element Plus 校验规则 |

**涉及文件**: 所有 Controller + 所有 `*RequestDTO` 文件 + 前端表单组件

---

## 三、P1 级功能详述

### 3.1 图片存储方案 (公开/隐私)

**需求**:
- 商品图片 → 公开访问（CDN / OSS 公读）
- 用户头像/身份证等 → 隐私图片（需鉴权访问）
- 图片上传 → 后端接收 → 转存 OSS

**技术方案**:

| 组件 | 选型 | 说明 |
|------|------|------|
| 对象存储 | 阿里云 OSS / MinIO(自建) | 公开 bucket 存放商品图，私有 bucket 存放用户隐私图 |
| 上传流程 | 后端签名直传 | 前端获取 STS Token → 直传 OSS，避免占用 JVM 带宽 |
| 隐私图片访问 | 后端签发临时签名 URL | 用户头像等隐私图返回带签名的临时 URL（有效期 30min） |
| 图片处理 | OSS 图片处理 (resize/水印) | URL 参数 `?x-oss-process=image/resize,m_fixed,w_200` |

**目录结构**:

```
oss://s-pay-mall/
├── public/                      # 公开 bucket
│   ├── products/{productId}/    # 商品图片
│   └── banners/                 # 轮播/广告图
└── private/                     # 私有 bucket
    ├── avatars/{userId}/       # 用户头像
    └── idcards/{userId}/       # 身份证（如需实名）
```

**涉及文件**:
- 新建 `IOssGateway` (Domain 层接口)
- 新建 `OssGatewayImpl` (Infrastructure 实现)
- 新建 `FileController` (Trigger 层)
- 前端上传组件

---

### 3.2 Nginx 反向代理图片请求

**需求**: 图片资源不经过 Java 应用服务器，由 Nginx 直接代理到 OSS/CDN，释放 JVM 线程资源。

**Nginx 配置方案**:

```nginx
# 静态资源直接代理 OSS
location /static/ {
    # 公开图片：直接反向代理到 OSS 公网域名
    proxy_pass https://s-pay-mall.oss-cn-hangzhou.aliyuncs.com/public/;
    proxy_set_header Host s-pay-mall.oss-cn-hangzhou.aliyuncs.com;
    # 强缓存：商品图片
    expires 30d;
    add_header Cache-Control "public, immutable";
}

# 隐私图片：先经过后端鉴权
location /private/ {
    # 走后端签发临时 URL 后 302 重定向
    proxy_pass http://java-backend:8080/api/v1/files/private/;
    proxy_set_header Host $host;
}
```

**涉及变更**:
- Nginx 配置文件新增 `location` 块
- 前端图片 URL 统一使用 `/static/` 前缀（不再直连 OSS 域名）
- 后端隐私图片接口签发临时签名 URL 后 302 跳转

---

### 3.3 数据迁移风险点方案

**适用场景**: 存量数据较少（早期项目），但未来若涉及以下操作需要完整方案:

| 迁移场景 | 风险点 | 缓解措施 |
|---------|--------|---------|
| 订单状态枚举统一 | 存量订单状态码不一致 | ①编写幂等迁移脚本 ②新旧枚举映射表 ③分批迁移 + 校验 |
| 数据库表结构变更 | 服务中断、数据丢失 | ①蓝绿部署 ②先加字段(可空) → 数据填充 → 设非空 ③回滚预案 |
| Redis Key 重命名 | 缓存失效、库存数据丢失 | ①RENAME 命令批量操作 ②双写过渡期 ③DB 库存同步兜底 |
| OSS 迁移 | 图片 URL 大面积失效 | ①旧 URL 301 重定向 ②数据库 URL 字段脚本批量替换 ③CDN 预热 |
| 消息队列 Topic 变更 | 消息丢失、重复消费 | ①新老 Topic 并行运行过渡 ②消费双写 ③幂等保证 |

**迁移 SOP 模板**:

```
1. 变更前
   - [ ] 备份数据库 / Redis dump
   - [ ] 确认回滚方案
   - [ ] 灰度/预发环境验证

2. 变更中
   - [ ] 执行迁移脚本（幂等设计）
   - [ ] 监控错误率/QPS/延迟
   - [ ] 数据一致性校验 (count/sum/checksum)

3. 变更后
   - [ ] 观察期 30min
   - [ ] 数据抽样验证
   - [ ] 清理过渡代码
```

---

## 四、P2 级功能详述

### 4.1 快递鸟 API 物流追踪

**需求**: 订单发货后，对接快递鸟 API 获取真实物流轨迹，用户可在订单详情查看。

**接入方案**:

| 事项 | 说明 |
|------|------|
| API 文档 | [快递鸟即时查询 API](https://www.kdniao.com/api-track) |
| 认证方式 | `EBusinessID` + `DataSign` (MD5 签名) + `RequestType` |
| 请求格式 | `application/x-www-form-urlencoded`，`RequestData` 为 JSON 串 |
| 核心接口 | 即时查询 `?RequestType=1002` — 传入物流单号 + 快递公司编码，返回轨迹 |

**技术设计**:

```java
// Domain 层接口
public interface ILogisticsGateway {
    /**
     * 查询物流轨迹
     * @param trackingNo 快递单号
     * @param expressCode 快递公司编码 (如 SF, YTO)
     * @return 物流轨迹列表
     */
    LogisticsTraceVO queryTrace(String trackingNo, String expressCode);
}
```

**数据库扩展**:

```sql
ALTER TABLE order_main ADD COLUMN tracking_no VARCHAR(64) COMMENT '快递单号';
ALTER TABLE order_main ADD COLUMN express_code VARCHAR(16) COMMENT '快递公司编码';
```

**前端展示**: 订单详情页新增「物流追踪」时间轴组件（参考 Element Plus Timeline）。

**涉及文件**:
- 新建 `domain/mall/gateway/ILogisticsGateway.java`
- 新建 `infrastructure/mall/gateway/KdniaoGatewayImpl.java`
- 新建 `api/vo/LogisticsTraceVO.java`
- 前端 `OrderDetailPage.vue` 物流时间轴
- 数据库 DDL 脚本

---

## 五、功能优先级排序

```
优先实现（本次迭代）：
├── P0: 统一异常处理 + 枚举规范 + 参数校验
│   （这三个改造互相依赖，建议一起做）

其次实现（下一迭代）：
├── P1: 图片存储 + Nginx 代理
│   （运维依赖：需要 OSS 账号 + Nginx 配置权限）

最后实现（排期待定）：
├── P2: 快递鸟物流追踪
│   （业务依赖：需要快递鸟商户账号）
└── 数据迁移 SOP 可按需执行

技术债清理（并行推进）：
└── 参见 TECH_DEBT_ROADMAP.md
```

---

## 六、相关文档索引

| 文档 | 说明 |
|------|------|
| [DEVELOPMENT_GUIDE.md](../../DEVELOPMENT_GUIDE.md) | 技术契约 — 命名规范、架构约束 |
| [TECH_DEBT_ROADMAP.md](TECH_DEBT_ROADMAP.md) | 技术债追踪 — 已知问题与修复计划 |
| [快递鸟 API 文档](https://www.kdniao.com/api-track) | 物流查询接口文档 |
