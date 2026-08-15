# 安全缺陷清单（SECURITY_ISSUES）

> **创建日期**: 2026-08-15 | **来源**: 代码安全审计（Spring Security + JWT 鉴权链路审查）
> **状态说明**: 本清单仅作记录，尚未开始修复。

| 编号 | 缺陷 | 位置(文件:行号) | 严重级别 | 攻击链说明 | 修复方案 | 状态 |
|------|------|----------------|---------|-----------|---------|------|
| S-01 | JWT密钥硬编码且prod环境未覆盖 + 用户端接口permitAll，二者组合构成认证绕过链 | `application-prod.yml`(密钥配置缺失)、`JwtTokenProvider.java:22`(默认密钥硬编码)、`BaseController.java:21`、`SecurityConfig.java:48-72`(permitAll清单) | 严重 | 攻击者用已知默认密钥 `REPLACED_DEFAULT_JWT_SECRET` 自签 `uid=任意值` 的HS256 token，因用户端接口（orders/cart/profile等）在Security层全部放行，可直接以任意用户身份调用订单/购物车/资料接口 | prod通过 `SECURITY_JWT_SECRET` 环境变量注入≥32字节强随机密钥；dev密钥移出仓库改用本地 `.env`（加入 `.gitignore`）；同时收紧 SecurityConfig，用户端只白名单 `/auth/login`、`/auth/register`、商品浏览等公开接口，其余要求 `authenticated()` | 待修复 |
| S-02 | JWT解析失败静默放行 | `JwtAuthenticationFilter.java`(doFilterInternal) | 中 | catch异常后仅 `log.warn`，不返回401、不中断请求，坏token/无token请求穿透到Controller层，仅靠Controller内 `currentUserId()` 二次解析兜底，存在漏校验风险 | 过滤器内解析失败时直接返回401并终止请求，不再依赖Controller兜底 | 待修复 |
| S-03 | 异常处理丢失认证语义 + 信息泄漏 | `GlobalExceptionHandler.java:94`(onException兜底) | 中 | 未登录、token过期、签名错误、NPE全部归为同一 `UN_ERROR` 错误码，前端无法区分并驱动跳登录页；且原始异常消息（如JWT过期时间、NPE栈顶）直接透传给前端 | 为 `JwtException` 及其子类、`IllegalArgumentException("未登录")` 分别定义独立错误码，禁止透传原始异常 message，改为固定文案 | 待修复 |
| S-04 | 代码注释与实际鉴权实现不符 | `MallAdminController.java:36`、`AdminApiController.java:29` | 低 | 注释声称"权限由Gateway/Interceptor层控制"或"Spring Security ADMIN角色校验"，实际均由 SecurityConfig 的 URL 规则控制，误导后续维护者 | 更新注释以反映实际由 SecurityConfig URL 规则控制权限 | 待修复 |
| S-05 | `@EnableMethodSecurity` 已启用但项目内零处使用 `@PreAuthorize`/`@Secured` | `SecurityConfig.java:26` | 低 | 启而未用，若未来有接口直接标注方法级注解会立即生效产生意外鉴权，且实际权限全部落在 URL 规则中，职责分散 | 若不打算使用方法级注解，移除该注解；若打算用，逐步在 admin 接口补充 | 待修复 |

---

## 备注

> ⚠️ 仓库当前为 **public**，dev 环境密钥（`REPLACED_COURSE_JWT_SECRET`）已提交到 git 历史，即使后续 rotate 密钥，仍建议在正式提交/答辩前评估是否需要清理 commit 历史，或至少在 README 中说明该密钥仅用于课程演示环境。
