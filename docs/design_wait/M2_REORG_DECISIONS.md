# M2 领域重构决策记录

> **日期**: 2026-10-03 | **分支**: 261001-fcr-refactor | **备份分支**: backup/m2-baseline-20261003
> **核心原则**: auth 管身份、mall 管商城、order 管订单

---

## 一、开工前决策点（已全部拍板）

| # | 决策 | 结论 |
|---|------|------|
| 1 | User 聚合归属 | **留 `mall/user`**。User 是商城会员聚合（管理端 CRUD 对象）；auth 只做登录编排 + 令牌 + 微信登录，凭据校验通过 `IAuthTokenGateway` 解耦。**auth 不搬 User** |
| 2 | 订单读模型归属 | `OrderVO`/`OrderCreateVO` 随订单进 `order/model/valobj`；`ProductVO`/`CategoryVO` 等留 mall 对应子包 |
| 3 | `StockChangeMsgDTO` 归属 | 归 `mall/product`（库存属商品子域）；MQ 消息体放 domain 是否合理另行评估，本次只归位不动语义 |
| 4 | legacy 过渡期限 | legacy 包在 P0-1 数据清零后删除；本次只做归位不删除；`OrderServiceTest` 随 legacy 保留 |
| 5 | 死端点清理 | 不夹带。M2 全部完成后再单独一批（与前端 TS 清理一起） |

## 二、测试基线（2026-10-03 实测）

| 模块 | 结果 | 说明 |
|------|------|------|
| domain | 6/6 通过 | DomainArchitectureGuardTest 2 + OrderEntityClearItemsTest 4 |
| app | 8 过 / 1 错 / 1 忽略 | AlipayNotifyE2ETest 5（其中并发幂等场景 @Ignore，待 P0-9）+ WeixinScanLoginMockE2ETest 2 + ApiTest 1；**OrderServiceTest 1 错为存量问题**（旧域 `PayOrderEntity.initPayUrl` 状态守卫抛异常，与 M2 无关） |

> ⚠️ 与计划预估"8 过 1 忽略"的差异：实际多 1 个存量 error（OrderServiceTest）。该错误在 M2 前后均存在，不作为 M2 的通过门槛，但记入此处备查。
> 注：app 模块 `pom.xml` 硬编码 `skipTests=true`，全量测试需临时改为 `false`（测完恢复）。

## 三、阶段与边界

| 阶段 | 内容 | 性质 |
|------|------|------|
| M2-0 | 基线准备（本文件 + 备份分支 + 基线确认） | 准备 |
| M2-1 | 超时关单分流：按 order_main 是否存在分流，新订单→状态机 cancel，旧订单→旧逻辑 | **行为修复（唯一改变系统行为的一步）** |
| M2-2 | shared 并入 order/model；删旧域重复 initPayUrl；PayOrderService 管状态流转 | 移动+一行修复 |
| M2-3 | auth 拆 login/token/permission 三包 | 纯移动 |
| M2-4 | mall 拆 product/cart/user/statistics 四包（infra 镜像联动） | 纯移动 |
| M2-5 | order 收编 mall 订单代码；旧链残余进 order/legacy | 移动+边界收敛 |
| M2-6 | DomainArchitectureGuardTest 更新、三份规范文档同步、全量测试+E2E 回归 | 验证 |

**不在本次范围**: MapStruct 统一、前端 TS 类型政策、command 孤儿包语义重审、支付宝/微信回调逻辑、Go 网关。

**横切面规则**: git mv 保历史；每步 `mvn clean install -DskipTests` + 放开 skipTests 全量测试必须绿；infra 包结构镜像跟随 domain；trigger 层不动行为/不动 JSON 契约（import 变更除外）；DomainArchitectureGuardTest 随结构同步更新。

## 四、M2-1 实施记录（2026-10-03）

**改动**：`OrderApplicationService.handleTimeoutCloseOrder` 分流——`mallOrderService.getOrderByNo(orderNo) != null`（order_main 存在）→ `orderStateMachineService.cancel(orderNo)`；否则走旧 `IOrderService.handleTimeoutCloseOrder`。注入 `IOrderStateMachineService`，监听器不变。

**修复的功能洞**：旧链 `OrderRepository.findByOrderNo` 以 **pay_order 表**为订单来源，M2-1 之前新订单的超时消息实际走旧链：pay_order 被置 CLOSED 但 **order_main 永远保持 CREATED**，且库存按旧链 `queryOrderItems` 固定 quantity=1 恢复（数量错误、无条件恢复 MySQL）。修复后新订单由状态机统一流转：order_main=CANCELED、pay_order=CLOSED、按 order_item 真实数量恢复 Redis 预扣库存、不动 MySQL。

**验证**：新增 `TimeoutCloseOrderE2ETest`（真实 MySQL/Redis，3 场景全过）：
1. 新订单 INIT → 关单 true，order_main=CANCELED，pay_order=CLOSED，Redis 库存 S0-q→S0，MySQL 不变；重复消息 false（幂等）
2. 已支付订单 PAID → 守卫拒绝 false，双表与库存均不变
3. 旧订单（仅 pay_order）→ 旧链行为保持：pay_order=CLOSED、不写 order_main、按旧语义恢复库存；重复关单 false

全量：domain 6/6；app 12（11 过 + 1 存量 error OrderServiceTest + 1 @Ignore 并发幂等），与基线一致（新增 3 过）。

## 五、M2-2 实施记录（2026-10-03）

**改动**：
1. `shared` 包整体并入 order：`PayOrderEntity` → `order/model/entity`，`PayStatus` → `order/model/vo`（git mv 保历史），23 个文件 import 机械替换，`domain.shared` 包删除
2. 删除旧 `OrderEntity.initPayUrl`（死代码，全仓无调用方，与 `PayOrderEntity.initPayUrl` 重复）
3. 职责定死：`PayOrderService` javadoc 明确——支付单状态流转（生成支付链接 WAIT_PAY→PAYING、失败标记 FAILED）只允许经本服务入口，持久化由 gateway 承担

**过渡期说明**：mall 侧 8 个文件暂 import order/model 的 `PayOrderEntity`/`PayStatus`（跨域实体引用的过渡态），M2-5 收编 mall 订单代码后消除。

**验证**：全仓 grep 无 `domain.shared` 残留（infra `config/shared` 为另一包，保留）；`mvn clean install` 全量构建通过；domain 6/6、app 12（11 过 + 1 存量 error + 1 @Ignore）与基线一致。

## 五之二、M2-3 实施记录（2026-10-03）

**改动**：auth 拆三包（纯移动，git mv 保历史）：
- `auth/login`：ILoginService、WeixinLoginService、WeixinBindService、IWeChatGateway、IWechatLoginGateway、IWeChatTokenRepository
- `auth/token`：IAuthTokenGateway（infra 侧 AuthTokenGatewayImpl、JwtTokenProvider 自 config/auth 一并镜像）
- `auth/permission`：Role（唯一权限模型）

infra 镜像同步：`auth/login/gateway(+dto)`、`auth/login/repository`、`auth/token`。app 模块 cn.fcr.config 下 Retrofit2Config、JwtAuthenticationFilter 及微信登录 E2E 测试的 import 同步修正。

**验证**：全仓 grep 零残留；`mvn clean install` 通过；domain 6/6、app 12（11 过 + 1 存量 error + 1 @Ignore）与基线一致，WeixinScanLoginMockE2ETest 2/2 证明登录链路完好。

## 六、风险备案

1. 40+ 文件移动引用漏改 → 分 6 步，编译器兜底，脚本只做机械 import 替换
2. Eclipse 污染 target → 每步 `mvn clean install`；改动期间建议关闭 Eclipse 自动构建
3. 超时关单分流改变行为 → M2-1 单独提交、单独实测（造新订单验证超时关单真正生效）
4. 时间盒 → M2-1/2 优先级最高；3/4/5 为纯结构，时间不够可顺延
