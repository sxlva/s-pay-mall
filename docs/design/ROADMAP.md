# 演进路线与未决事项 (Roadmap)

> **合并自**: `design_wait/FUTURE_FEATURES.md` + `design_wait/UPGRADE_POINTS.md` + `design_wait/TECH_DEBT_ROADMAP.md`
> **合并日期**: 2026-10-05
> **文档定位**: 本文档是"系统还能怎么演进"的唯一入口——未来功能规划、已设计暂不实现的升级点、未处理技术债、技术债关闭记录（答辩证据链）。历史过程性细节（迁移参考、批次记录）已压缩，需要时可从 git 历史回溯。

---

## 一、未来功能规划

### 1.1 功能概览

| 优先级 | 功能 | 模块 | 预估工时 | 依赖 | 状态 |
|-------|------|------|---------|------|------|
| ~~P0~~ | ~~统一异常处理~~ | 全栈 | 3d | — | ✅ 已关闭（2026-10-05，主体工作已完成，见 1.4） |
| ~~P0~~ | ~~枚举类规范重建~~ | 后端 Domain + Types | 2d | — | ✅ 已关闭（2026-10-05，主体工作已完成，见 1.4） |
| ~~P0~~ | ~~参数校验全覆盖~~ | 后端 Trigger + 前端 | 3d | — | ✅ 已关闭（2026-10-05，主体工作已完成，见 1.4） |
| P1 | 图片存储方案（OSS/MinIO 演进） | 后端 + 运维 | 5d | — | **本地存储方案已实现**：商品图片已落地为本地文件存储（见 [module-product-image](module-product-image.md)），OSS 迁移为演进项 |
| P1 | Nginx 反向代理图片请求 | 运维/部署 | 2d | 图片存储方案 | ⏸ 暂缓：本地存储下由 Spring 静态资源映射直接服务（`uploads/products/`），演示环境无 Nginx 依赖；上生产时按需补做 |
| P1 | 数据迁移风险点方案（SOP） | 后端 DBA | 3d | — | 📋 方案已备，按需执行（见 1.3） |
| P2 | 快递鸟 API 物流追踪 | 后端 + 前端 | 5d | — | 📋 方案已备，需快递鸟商户账号（见 1.5） |

### 1.2 P1 图片存储演进方案（OSS/MinIO）

当前商品图片采用本地文件存储（`uploads/products/`），满足毕设演示需求。生产化演进方向：

| 组件 | 选型 | 说明 |
|------|------|------|
| 对象存储 | 阿里云 OSS / MinIO(自建) | 公开 bucket 存放商品图，私有 bucket 存放用户隐私图 |
| 上传流程 | 后端签名直传 | 前端获取 STS Token → 直传 OSS，避免占用 JVM 带宽 |
| 隐私图片访问 | 后端签发临时签名 URL | 用户头像等隐私图返回带签名的临时 URL（有效期 30min） |
| 图片处理 | OSS 图片处理 (resize/水印) | URL 参数 `?x-oss-process=image/resize,m_fixed,w_200` |
| Nginx 代理 | `location /static/` → OSS | 图片资源不经过 Java 应用，强缓存 `expires 30d` |

**涉及文件**（落地时）：新建 `IOssGateway`（Domain）/ `OssGatewayImpl`（Infrastructure）/ `FileController`（Trigger）+ 前端上传组件。`LocalProductImageGatewayImpl` 到 OSS 实现是接口替换，业务层零改动。

### 1.3 P1 数据迁移 SOP

| 迁移场景 | 风险点 | 缓解措施 |
|---------|--------|---------|
| 订单状态枚举统一 | 存量订单状态码不一致 | ①幂等迁移脚本 ②新旧枚举映射表 ③分批迁移 + 校验 |
| 数据库表结构变更 | 服务中断、数据丢失 | ①蓝绿部署 ②先加可空字段 → 数据填充 → 设非空 ③回滚预案 |
| Redis Key 重命名 | 缓存失效、库存数据丢失 | ①RENAME 批量操作 ②双写过渡期 ③DB 库存同步兜底 |
| 本地/OSS 图片迁移 | 图片 URL 大面积失效 | ①旧 URL 301 重定向 ②数据库 URL 字段脚本批量替换 ③CDN 预热 |
| 消息队列 Topic 变更 | 消息丢失、重复消费 | ①新老 Topic 并行过渡 ②消费双写 ③幂等保证 |

**变更前** → 备份数据库/Redis dump、确认回滚方案、灰度验证；**变更中** → 执行幂等脚本、监控错误率/QPS、一致性校验（count/sum/checksum）；**变更后** → 30min 观察期、数据抽样验证、清理过渡代码。

### 1.4 P0 批次关闭记录（2026-10-05）

| 项 | 关闭结论 |
|----|---------|
| 统一异常处理 | `GlobalExceptionHandler` 已覆盖 `AppException`/领域删除保护/`DataIntegrityViolationException`/`MethodArgumentNotValidException → 0002` 与通用兜底，响应统一 `Response(code, info, null)`；前端 axios 拦截器统一错误提示。五段式 `ErrorCode` 体系（A0xxx~T0xxx）对当前规模属过度设计，不做 |
| 枚举类规范重建 | 两套订单状态枚举已合并为单个 `OrderState`（code + 中文描述 + DB 状态映射三元结构，双向转换），优于原方案；`PayStatus` 已是 code+desc 枚举；主代码全部使用枚举（最后一处字面量 2026-10-05 消除）。`IEnum` 接口、MyBatis 自动映射、`{code, desc}` JSON 化属过度设计，不做 |
| 参数校验全覆盖 | `@Valid` 全覆盖 + `spring-boot-starter-validation` 依赖 + `MethodArgumentNotValidException → 0002` 全局映射已于 2026-10-02 完成并逐端点实测；下单 requestId 幂等 + order_paid 消费幂等已固化（E2E）。自定义校验注解、分组校验为可选增强 |

### 1.5 P2 快递鸟 API 物流追踪

| 事项 | 说明 |
|------|------|
| API 文档 | [快递鸟即时查询 API](https://www.kdniao.com/api-track) |
| 认证方式 | `EBusinessID` + `DataSign` (MD5 签名) + `RequestType` |
| 核心接口 | 即时查询 `?RequestType=1002` — 传物流单号 + 快递公司编码，返回轨迹 |

**技术设计**：`domain/mall/gateway/ILogisticsGateway.queryTrace(trackingNo, expressCode)` → `infrastructure/mall/gateway/KdniaoGatewayImpl`；`order_main` 增加 `tracking_no`、`express_code` 两个字段；前端订单详情页新增物流时间轴组件（Element Plus Timeline）。

---

## 二、系统升级点设计集（已设计、暂不实现）

> 每一项均已形成完整设计方案，因毕设范围、部署规模（单实例演示）所限**暂不实现**。答辩被追问"消息丢了怎么办 / 系统还能怎么改进"时可引用对应设计说明演进路径。

**已实现的可靠性现状**：事务内状态推进 → 事务提交后发送 `order_paid` → 消费端 Redis SETNX 幂等 → RocketMQ 延时关单（delayLevel=9，30 分钟窗口）→ 关单前二次确认（查支付宝交易状态）→ 消费失败重试（`maxReconsumeTimes=5`）→ 死信队列（`%DLQ%` + consumerGroup）→ 晚到支付显式异常路径（error 日志 + 人工核实标记）。

### U-1 对账补偿 Job（消息丢失的最后一道保险）

**问题**：MQ 可能"发出时就丢"——进程在事务提交后、消息发送前崩溃，`order_paid` 永远不会出现，重试和 DLQ 都救不回来。

**设计**：
- **支付补偿**：定时扫描 `pay_order = PAID` 但商城主单未推进、且超过 5~10 分钟的订单，直接调用 `paySuccess` 补偿（状态机重复推进仅 warn，补偿与 MQ 消费并发安全）；
- **超时关单兜底**：扫 2 倍延时（约 60 分钟）仍未关闭的待支付订单，调用 `handleTimeoutCloseOrder`；
- **多实例防重**：Job 加 Redisson `tryLock`，抢不到锁直接跳过本轮（现有 `NoPayNotifyOrderJob` 为裸 `@Scheduled`，多实例下会重复执行，落地时一并修复）；
- **落点**：不新建组件，扩展 `NoPayNotifyOrderJob` 增加两个检查分支。

**暂不实现**：单实例演示 MQ 丢失概率极低；成本约 1~2 天。

### U-2 发送端 Outbox（本地消息表，治"发出时丢"的根因）

**问题**："事务提交后发送"方案中，提交与发送之间仍有微小崩溃窗口。

**设计**：
- 支付回调事务内**同事务写入 outbox 表**：`id`、`biz_no`（唯一索引）、`topic`、`payload`、`status`、`retry_count`、`next_retry_time`；
- 独立 Job 扫描待发送记录，同步发送并检查 `SendResult`；失败累加次数、按退避更新重试时间，超限告警；
- 重扫重发是**期望行为**（at-least-once），消费端幂等保证无副作用；
- 成功记录仅标记状态不物理删除，保留审计能力。

**备选取舍**：RocketMQ 事务消息（需实现回查接口）对本项目规模而言 outbox 更易理解与排查，故不选。

**暂不实现**：当前发送窗口已极小，U-1 可兜住残余；全链路约 2~3 天。

### U-3 消费幂等升级：消费记录表替代 Redis 单键

**问题**：现状 `order_paid_notify:{orderNo}` 一个 Redis 键同时表示"处理中"和"已完成"，存在两个竞态：①消费超时但业务已部分完成时重投被跳过 → 漏处理；②Redis 键设置与业务提交非原子，宕机中间态 → 可能重复执行。库存 handler 的 `stock:event:{changeType}:{businessNo}` 同问题。

**设计**：
- **推荐**：新增消费记录表 `(consumer_group, biz_no)` 唯一索引，插入记录与业务变更放**同一个 DB 事务**——宕机回滚则记录回滚、重投自然重执，提交成功则唯一索引天然拒重；
- **备选**：Redis 拆双状态（`PROCESSING` 短 TTL + `DONE` 长 TTL），库存"检查+扣减"用 Lua 保证原子；一致性弱于 DB 方案；
- **外部副作用（微信模板消息）无法进事务**，只能接受"至少一次"：重复发一条通知可接受（当前选择），或二期加通知记录表去重。

**暂不实现**：当前 Redis SETNX + 异常释放已通过三场景 E2E，单机竞态窗口极小；DB 方案约 2~3 天。

### U-4 Listener 异常分类处理

**问题**：不可恢复错误（反序列化失败、订单不存在）重试 16 次毫无意义，白白占用重试资源。

**设计**：可恢复异常（DB/Redis 抖动）继续抛出走重试；不可恢复异常记录 error 日志后直接 ACK（不落失败记录表，监控依赖日志告警）。现状部分实践已符合（paySuccess 对不存在订单 warn 后返回），本项将规则显式化并补入开发规范。

**暂不实现**：现状行为已接近目标，差异仅在代码结构，毕设层面收益不明显。

### U-5 DLQ 监控告警与重放流程

**问题**：消息耗尽重试进入 `%DLQ%{consumerGroup}` 后无人处理等于静默丢消息。

**设计**：
- 对 `%DLQ%` 主题配置告警：消息量 > 0 即报警，值班介入；
- 重放工具只投递给目标 consumer group，重放前确认幂等键不会误判（24h TTL 已过期的需人工核实）；
- RocketMQ 控制台自带消息查询与重新发送，手工重放先以操作手册形式固化，自动化重放工具列为二期；
- 重放仍失败的转人工工单（order_paid 类人工核实后手动触发 paySuccess）。

**暂不实现**：U-1~U-3 落地后进入 DLQ 的消息量趋近于零；告警与重放属低频运维流程，演示环境可用控制台手工操作。

### 演进顺序建议

U-1 对账 Job（含 NoPayNotifyOrderJob 分布式锁）→ U-2 outbox → U-3 消费记录表 → U-4 异常分类 → U-5 DLQ 告警与重放。

前一项落地会显著降低后一项的触发概率，整体呈"**兜底 → 治根因 → 加固 → 运维**"的递进结构。

### 答辩引用索引

| 追问场景 | 引用章节 |
|---------|---------|
| "消息发出时就丢了怎么办？" | U-1（兜底）+ U-2（根因） |
| "重复消费会不会重复扣库存/发货？" | U-3 + 已实现下单/消费幂等（见 三.2 A.15） |
| "死信队列谁处理？" | U-5 + 已实现 maxReconsumeTimes 显式配置 |
| "定时任务多实例会不会重复执行？" | U-1 多实例防重段 |
| "为什么不用 RocketMQ 事务消息？" | U-2 备选方案取舍段 |

---

## 三、技术债关闭记录与未处理项

> **状态：后端 P0/P1 已清零（2026-10-04）；前端 FP 全项已关闭（2026-10-05）。** 以下为关闭记录（单行结论，完整过程见 git 提交历史）与仅剩的未处理项。

### 3.1 后端技术债（全部已关闭）

| ID | 问题 | 关闭结论（一句话） | 日期 |
|----|------|------------------|------|
| P0-1 | 新旧订单系统并存 | `order` legacy 包整体下线（超时关单分流状态机、回调统一走状态机、legacy 包删除、pay_order 无主行清零、守卫规则防回潮） | 2026-10-03 |
| P0-2 | Application 层模块归属 | 新建 `s-pay-mall-application` 模块，`OrderApplicationService`/`OrderTransactionService` 迁至 `cn.fcr.application`；`s-pay-mall-app` 更名 `s-pay-mall-start` | 2026-10-03 |
| P0-3 | Infrastructure 层 @Transactional 违规 | 事务上移至新建 `AuthApplicationService`，Infrastructure/Domain 层零事务注解 | 2026-10-04 |
| P0-4 | 幂等性设计缺失 | 下单 requestId 幂等（锁在事务外，PROCESSING/完成分态）+ order_paid 消费幂等（失败不抛异常避免无谓重投），三场景 E2E 锁定 | 2026-10-04 |
| P0-5 | Domain 层跨领域反向依赖 | M2-5：`IPayGateway` 随 mall 订单簇收编进 `domain/order/gateway`，反向依赖自然消除，无需三选一 | 2026-10-03 |
| P0-6 | Controller 中业务路由逻辑 | 注册策略/支付状态判断下沉 Domain（`PayTradeStatus` 枚举 + 领域服务单一入口），Trigger 层只做协议适配 | 2026-10-04 |
| P0-9 | 并发回调重复扣库存 | 状态 UPDATE 加 `expectStatus` 源状态条件（条件更新守卫），影响 0 行即重复回调直接返回，10 线程并发回归测试通过 | 2026-10-03 |
| P1-1 | Controller 大面积缺 @Valid | 8 个 `@RequestBody` 端点补齐 `@Valid` + 补 JSR-303 实现依赖（此前注解静默失效）+ 全局异常映射，逐端点实测 | 2026-10-02 |
| P1-2 | Redis+DB 跨资源事务一致性 | 取消订单库存恢复移到事务提交后（与下单同模式），恢复失败仅告警不回滚 | 2026-10-04 |
| P1-3 | OrderPaidRocketListener 含业务编排 | 编排下沉 `OrderApplicationService.sendPaySuccessNotification()`，Listener 仅保留幂等守门 | 2026-10-04 |
| P1-4 | MQ 消息发送缺超时参数 | `convertAndSend` 增加 3000ms 超时 | 2026-10-04 |
| P1-5 | Domain 层 POM 非必要技术依赖 | 移除 spring-context/spring-tx/alipay-sdk/jjwt/fastjson/guava 等，alipay-sdk 改由 infrastructure 显式声明 | 2026-10-04 |
| P2-1 | createPayOrder 缺事务保护 | 死端点随 legacy 下线删除，条目失效 | 2026-10-03 |
| P2-2 | 缺死信队列配置 | 三个 Listener 显式 `maxReconsumeTimes=5`（耗尽自动进 `%DLQ%`）；告警与重放见 U-5 | 2026-10-04 |
| P2-3 | WeixinGatewayImpl 缺超时配置 | Retrofit2Config 显式 connect 5s / read 10s | 2026-10-04 |
| P2-4 | pay-success-topic 无消费者 | 删除 `IOrderEventGateway`/`OrderEventGatewayImpl`，topic 废弃 | 2026-10-01 |
| P2-5 | 支付成功消息通道重复 | 保留 `order_paid`，删除 `pay-success-topic` 通道 | 2026-10-01 |
| P2-6 | WeixinBindService 死代码 | 随注册锁清理移除 | 2026-10-04 |
| P2-7 | 认证令牌双抽象命名冲突 | 收敛为单一 `IAuthTokenGateway` 移至 `domain.auth.gateway`，删除 `ITokenProvider`/`TokenProviderAdapter` | 2026-10-03 |

### 3.2 历史修复附录（7 月批次，单行记录）

| 项 | 结论 |
|----|------|
| A.1 基础设施层模块化 | config/dao 按领域拆分，死代码删除 |
| A.2 P0-7 NoPayNotifyOrderJob 解耦 Alipay SDK | 封装为 `IAlipayQueryGateway`/`AlipayQueryGatewayImpl` |
| A.3 P0-8 库存预检查下沉 Domain 层 | 预检查上移 `MallOrderServiceImpl`，Gateway 仅保留原子 decr + 竞态补偿 |
| A.4-A.8 应用层重建与事务/MQ 解耦 | 新建 `OrderApplicationService`/`OrderTransactionService` 编排，MQ 发送移出事务 try-catch 保护，事件发布从 Repository 上移至 Application |
| A.9 库存扣减泄漏 | 两轮遍历改单轮（逐项检查即扣减，失败回滚已扣列表） |
| A.10 JWT Token 日志泄露 | 日志移除 openidToken 参数 |
| A.11 P2-7 令牌收敛 | 见 3.1 表 |
| A.12 P0-2 Application 模块 | 见 3.1 表；**经验**：先全仓 grep 调用方再动手 |
| A.13 P0-3 事务上移 | 见 3.1 表；**经验**："判断规则留 Domain、用例编排+事务留 Application"是最小方案 |
| A.14 P0-6 业务下沉 | 见 3.1 表；**经验**：Trigger 的每个 `if` 都要问"这条规则有没有第二个入口" |
| A.15 P0-4 幂等保护 | 见 3.1 表；**经验**：幂等锁必须在事务外获取；"处理中"与"已完成"要用不同值区分 |

### 3.3 前端技术债（全部已关闭，2026-10-05 分支 261005-frontend-debt）

| ID | 问题 | 关闭结论（一句话） |
|----|------|------------------|
| FP0-1 | API 调用三层重叠 | `repositories/` + `services/` 全部并入统一 `api/` 层，两层目录删除，数据出口唯一 |
| FP0-2 | admin.ts 过于臃肿 | 294 行拆分为 `api/admin/{user,category,product,order,statistics}.ts` |
| FP0-3 | cartRepository 技术栈不一致 | 裸 fetch 全部迁统一 Axios 实例，token/解包/错误提示走拦截器 |
| FP0-4 | 前后端字段不一致 | `orderId→orderNo` 统一映射，修复支付轮询空 orderNo 永不命中问题 |
| FP1-1 | 类型与 API 函数混放 | 7 个管理端类型抽入 `types/domain/admin.ts` |
| FP1-2 | 统一 API 工具未使用 | 唯一调用方迁移后删除 `utils/api.ts` |
| FP1-3 | 前端多余字段 | `StockCheckResult.stockStatus` 删除 |
| FP1-4 | 调试代码未清除 | api/order.ts 与 OrderListPage 调试 log 清除 |
| FP1-5 | 命名不一致 | 管理端统一 `AdminVO` 后缀 |
| FP2-1 | 组件接口契约覆盖率低 | 以删除替代加类型（脚手架残留零引用组件直接删除） |
| FP2-2 | localStorage 残留 | `checkout_products` 无读取方，删除残留写入 |

### 3.4 未处理技术债（登记在案）

| ID | 问题 | 现状与建议 |
|----|------|-----------|
| TD-1 | JSON 命名策略不一致 | `api/dto/` 无 `@JsonProperty`（camelCase）与 `api/vo/` 全量 `@JsonProperty`（snake_case）并存，前端同时消费两种风格。建议统一为 camelCase（前端 TypeScript 惯例），逐步废弃蛇形命名——**低风险低收益，业务迭代时顺手做** |
| TD-2 | 死信队列告警与重放 | `maxReconsumeTimes=5` 已配置，但 DLQ 无人值守（消息进入 `%DLQ%` 后需人工处理）。方案完整设计见 U-5，生产化时优先落地 |
| TD-3 | 关单写 `pay_time=now()` 语义污染 | `changeOrderClose` 关单时写支付时间，未支付订单有支付时间属字段语义污染，读取端不受影响。支付链路审计时一并处理 |
| TD-4 | `OrderState.DONE` 存储口径不一致 | `toDbStatus()` 输出 COMPLETED 而状态机写 DONE，读取端 `fromDbStatus` 双兼容已兜住。订单状态口径统一时处理 |
| TD-5 | 支付回调未校验金额一致 | 验签已保证参数真实性，属支付安全增强（需 BigDecimal 比较）。支付安全加固批次处理 |
| TD-6 | 重复回调重复发布 `order_paid` 事件 | 下游消费幂等（orderNo 幂等键）已兜底，仅产生冗余消息。事件链路优化时处理 |
| TD-7 | `sendDelayCloseMessage` 内层 catch 吞异常 | 失败靠 NoPayNotifyOrderJob 补偿兜底。可靠性设计专题时处理 |
| TD-8 | NoPayNotifyOrderJob 无分布式锁 | 多实例部署会重复执行。U-1 落地时一并加 Redisson `tryLock` |

### 3.5 暂缓/有意跳过项（结论沉淀）

- **五段式 `ErrorCode` 体系**（A0xxx~T0xxx）：对当前项目规模属过度设计，沿用 `Constants.ResponseCode`（0000/0001/0002/0003/0403）。
- **`IEnum` + MyBatis 自动映射 + `{code, desc}` JSON 化**：`OrderState` 三元结构（code+desc+DB 映射）优于原方案；前端以 TypeScript 联合类型表达状态。
- **RocketMQ 事务消息**：outbox 本地消息表对本项目更易理解与排查（U-2 取舍段）。
- **消费失败记录表**：避免"表没人看"的新问题，监控依赖日志告警（U-4）。

---

## 相关文档

| 文档 | 说明 |
|------|------|
| [README](README.md) | 系统总览与模块导航 |
| [module-product-image](module-product-image.md) | 商品图片功能设计与实现（P1 图片存储的本地落地版） |
| [DEVELOPMENT_GUIDE.md](../../DEVELOPMENT_GUIDE.md) | 技术契约 — 命名规范、架构约束 |
| [REVIEW.md](../../REVIEW.md) | 代码审查规则 |
