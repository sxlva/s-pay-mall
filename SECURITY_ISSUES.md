# 安全缺陷清单（SECURITY_ISSUES）

> **创建日期**: 2026-08-15 | **来源**: 代码安全审计（Spring Security + JWT 鉴权链路审查）
> **状态说明**: 本清单仅作记录，尚未开始修复。

| 编号 | 缺陷 | 位置(文件:行号) | 严重级别 | 攻击链说明 | 修复方案 | 状态 |
|------|------|----------------|---------|-----------|---------|------|
| S-01 | JWT密钥硬编码且prod环境未覆盖 + 用户端接口permitAll，二者组合构成认证绕过链 | `application-prod.yml`(密钥配置缺失)、`JwtTokenProvider.java:22`(默认密钥硬编码)、`BaseController.java:21`、`SecurityConfig.java:48-72`(permitAll清单) | 严重 | 攻击者用已知默认密钥 `REPLACED_DEFAULT_JWT_SECRET` 自签 `uid=任意值` 的HS256 token，因用户端接口（orders/cart/profile等）在Security层全部放行，可直接以任意用户身份调用订单/购物车/资料接口 | prod通过 `SECURITY_JWT_SECRET` 环境变量注入≥32字节强随机密钥；dev密钥移出仓库改用本地 `.env`（加入 `.gitignore`）；同时收紧 SecurityConfig，用户端只白名单 `/auth/login`、`/auth/register`、商品浏览等公开接口，其余要求 `authenticated()` | 部分缓解（2026-10-02：permitAll 已收窄至公开浏览/登录回调，用户端数据接口与 create_pay_order 改要求认证且 userId 取自 JWT；默认密钥仍可自签 token 的风险仍在，见 S-02） |
| S-02 | JWT解析失败静默放行 | `JwtAuthenticationFilter.java`(doFilterInternal) | 中 | catch异常后仅 `log.warn`，不返回401、不中断请求，坏token/无token请求穿透到Controller层，仅靠Controller内 `currentUserId()` 二次解析兜底，存在漏校验风险 | 过滤器内解析失败时直接返回401并终止请求，不再依赖Controller兜底 | 已修复（2026-10-07）：过滤器解析失败清空 SecurityContext 并继续走过滤器链，受保护端点由 SecurityConfig 注册的 AuthenticationEntryPoint 统一返回 401 + `{"code":"0003"}` JSON（公开端点不受过期 token 影响，避免破坏浏览）；Controller 内二次解析为兜底不变。E2E 新增场景 4（匿名/非法 token 访问受保护端点均 401 + 0003）。前端配套（2026-10-07）：axios 响应拦截器识别 HTTP 401 / 业务 code 0003，自动清理本地登录态（token/userId/username/role）并跳转登录页 |
| S-03 | 异常处理丢失认证语义 + 信息泄漏 | `GlobalExceptionHandler.java:94`(onException兜底) | 中 | 未登录、token过期、签名错误、NPE全部归为同一 `UN_ERROR` 错误码，前端无法区分并驱动跳登录页；且原始异常消息（如JWT过期时间、NPE栈顶）直接透传给前端 | 为 `JwtException` 及其子类、`IllegalArgumentException("未登录")` 分别定义独立错误码，禁止透传原始异常 message，改为固定文案 | 已修复（2026-10-07）：① 新增 `JwtException` 处理器归 0003 未登录 + 固定文案；② 兜底 `onException` 不再透传 `e.getMessage()`，统一固定文案，原始异常仅落服务端日志；③ 顺带修正：`bindWeChat`/`setPassword` 面向用户的守卫由 `IllegalArgumentException` 改抛 `AppException(0001, 原文案)`（与 handler 既定设计一致，对外响应不变）。E2E 全量回归通过 |
| S-04 | 代码注释与实际鉴权实现不符 | `MallAdminController.java:36` | 低 | 注释声称"权限由Gateway/Interceptor层控制"，实际由 SecurityConfig 的 URL 规则（`/mall-api/v1/admin/**` hasRole ADMIN）控制，误导后续维护者 | 更新注释以反映实际由 SecurityConfig URL 规则控制权限 | 待修复（AdminApiController 部分已随该类删除于 2026-10-01 消除） |
| S-05 | `@EnableMethodSecurity` 已启用但项目内零处使用 `@PreAuthorize`/`@Secured` | `SecurityConfig.java:26` | 低 | 启而未用，若未来有接口直接标注方法级注解会立即生效产生意外鉴权，且实际权限全部落在 URL 规则中，职责分散 | 若不打算使用方法级注解，移除该注解；若打算用，逐步在 admin 接口补充 | 保留不动（2026-10-07 决议）：零注解时该注解不产生任何行为，无实际风险；`permission`/`role_permission` 表与之一并保留作细粒度 RBAC 扩展点（见 ROADMAP TD-11），毕设范围不落地方法级鉴权 |

---

## 备注

> ⚠️ 仓库当前为 **public**，dev 环境密钥（`REPLACED_COURSE_JWT_SECRET`）已提交到 git 历史，即使后续 rotate 密钥，仍建议在正式提交/答辩前评估是否需要清理 commit 历史，或至少在 README 中说明该密钥仅用于课程演示环境。
