# s-pay-mall 系统学习总纲（STUDY GUIDE）

> **定位**：本文档是教学文档，不是设计文档。`docs/design/` 下的 `module-*.md`、`ROADMAP.md` 继续作为**设计事实**的参考；本文档回答"按什么顺序学、学到什么程度算会、去哪段源码验证"。
> **版本**：v1.4（2026-10-19：附录 D-2 已登记 SECURITY_ISSUES **S-06** 并修复——portal `post()` 补验签，重放攻击实测被拒，E2E 17/17 回归通过；D-1 重扫重签发列为已知限制未处理。v1.3：附录 A 六项差异完成原设计文档修正；附录 D 两项风险经 C-7 实验实测确认；C-7 销项。v1.2：删除简历 bullet"零资损"表述；场景 3 补充通知异常边界；P3 区分"状态正确"与"通知可恢复"。v1.1：前置知识三级化、日程六要素化+顺延规则、四场景消息可靠性分析、附录 A 六字段化、新增附录 D）
> **依据**：全部关键源码已逐文件核对（含行号）；Git 历史中 Guava 的引入与移除已考证。
> **适用对象**：具备 Java/Spring 基础、遗忘微信登录与支付宝支付协议、需要从零学习 RocketMQ 的本科生。
> **最终验收标准（总纲级）**：不看任何文档，能画出登录/下单/支付/履约/关单五条时序图，能沿真实源码逐行讲解，能分析异常场景，能回答面试官连续追问。

**证据标记图例（全文统一）**：

| 标记 | 含义 |
|------|------|
| 【代码推导】 | 从源码路径/配置直接推导得出，本轮审计**未实际运行**该路径 |
| 【文档实测】 | 2026-10-07 首次通过的实测结果（E2E 五类 17 用例）；**已于 2026-10-19 在当前工作区全量复跑通过（C-1 销项）** |
| 【待验证】 | 需要实验、外部官方文档或你的决策确认，**不得**当作已证实结论引用 |
| 无标记的通用描述 | 源码结构、类名、行号等直接观察事实 |

---

## 一、项目全景：这个系统在解决什么问题

s-pay-mall 是一个**前后端分离的电商商城系统**，用户侧支持微信扫码登录、浏览商品、加购、下单、支付宝当面付支付；管理侧支持商品/分类/订单/用户管理。

用一句话概括它的技术主线：**一条"钱"的链路**。

```
微信扫码登录（你是谁）
  → 浏览/加购（你要买什么）
  → 下单（Redis 预扣库存 + MySQL 落单，一个事务）
  → 支付宝支付（唤起收银台）
  → 支付宝异步回调（验签 + 金额校验 + 状态机推进，一个事务）
  → RocketMQ 异步处理（当前实际职责：微信模板消息通知）
  → 30 分钟不付钱？延时消息触发超时关单（关单前先问支付宝"他到底付没付"）
  → 消息丢了/重试耗尽了？定时 Job 补偿 + DLQ 告警 + 人工介入
```

为什么这个系统值得学（而不是"又一个 CRUD 项目"）：它的复杂度不在功能数量，而在**分布式环境下的正确性**——重复回调、重复消费、支付与关单并发、DB 与 Redis 跨资源一致性。这些正是 Java 后端校招面试的高频追问区。

### 模块如何协作（DDD 分层视角）

```
Trigger 层（Controller/Listener/Job）   只做协议适配：HTTP 解析、MQ 消息接收、定时触发
   ↓ 调用
Application 层（cn.fcr.application）     用例编排 + 事务边界（OrderApplicationService 等）
   ↓ 调用
Domain 层（cn.fcr.domain）              业务规则：状态机、幂等语义、库存检查、登录判定
   ↓ 依赖接口（端口）
Infrastructure 层（cn.fcr.infrastructure） 技术实现：MyBatis、Redis、Retrofit 调微信/支付宝、RocketMQ
```

**依赖方向铁律**：上层能调下层，Domain 层只依赖接口不依赖实现。面试被问"DDD 在哪体现"，就带面试官看 `IStockGateway`（Domain 定义接口）与 `StockGatewayImpl`（Infrastructure 实现）这一对。

---

## 二、核心业务链路

完整链路见 [README §二](README.md) 的 mermaid 全景图。学习时必须能独立画出以下五条：

| # | 链路 | 起点 → 终点 | 核心难点 |
|---|------|------------|----------|
| 1 | 微信扫码登录 | 前端取 ticket → 微信 SCAN 回调 → Redis 存 JWT → 前端轮询取走 | 异步事件如何通知浏览器（轮询）、一次性凭证 |
| 2 | 下单 | 购物车 → Redis 预扣库存 → 事务落单 → 发延时关单消息 | 幂等（防重复下单）、库存不超卖、失败回滚 |
| 3 | 支付回调 | 支付宝 POST → 验签 → 金额校验 → 状态机推进（事务内）→ 事务外发 MQ | 验签防伪造、重复回调幂等、事件不重复发布 |
| 4 | MQ 异步处理 | order_paid 消息 → 消费幂等守门 → 状态推进（空转）→ 微信通知 | at-least-once 下的重复投递抑制（见 §八，不是"恰好一次"） |
| 5 | 超时关单 | 30min 延时消息 → 支付宝二次确认 → 状态机关单（事务内）→ 事务外恢复 Redis 库存 | 关单与支付并发（钱已收不能关）、跨资源一致性 |

**教学口径警示（已核实源码）**：链路 3 中订单状态推进（INIT→PAID、pay_order→PAID、MySQL 库存同步）是在**支付回调的事务内同步完成**的；链路 4 的 MQ 消费侧再调 `paySuccess` 时，因状态机守卫（订单已 PAID，`canPay()` 为 false）恒为空转——MQ 在这条链路当前承担的异步工作**只有微信模板消息通知**。不要把"履约"说成完全由 MQ 驱动，被追问会穿帮。详见 §八"MQ 职责边界"与 [附录 A 差异 A-4](#附录-a文档与代码差异清单六字段逐项)。

---

## 三、模块依赖关系

| 模块 | 职责 | 输入 | 输出 | 主要调用方 |
|------|------|------|------|-----------|
| auth（登录/绑定） | 账密登录、微信扫码登录、微信↔账密绑定、JWT 签发 | 用户名密码 / 微信 ticket+openid | JWT、userId | 前端所有需登录接口 |
| mall/user | 用户实体、角色、资料 | userId | UserEntity、绑定状态 | auth、订单（查 openid 发通知） |
| mall/product + stock | 商品、库存预扣/恢复、库存预热 | productId、quantity | 剩余库存 | 订单（下单/取消/支付同步） |
| mall/cart | 购物车增删改查 | userId、productId | CartItemVO 列表 | 订单创建时读取 |
| order（状态机） | 订单状态流转规则、支付单状态 | orderNo、事件 | 流转是否成功（boolean） | Application 层全部订单用例 |
| order/gateway（支付） | 支付宝 SDK 封装：生成支付链接、验签、交易查询 | PayOrderEntity / 回调参数 | payUrl / 验签结果 / 交易状态 | PayOrderService、状态机、Job |
| order/event | RocketMQ 事件发布 | tradeNo、orderNo | order_paid 消息 | 支付成功后（事务外） |
| trigger/listener | MQ 消费：支付成功、超时关单、DLQ 告警 | MQ 消息 | 业务调用 + ACK/重投 | RocketMQ broker |
| trigger/job | 定时补偿：补单、兜底关单、库存预热、图片清理 | 调度触发 | 补偿调用 | Spring Scheduler |

依赖要点：`MallUserServiceImpl`（mall/user）通过 `IOrderQueryGateway`（order 定义的接口）查订单数做删除守卫——这是 Domain 层跨领域**正向依赖接口**的合法姿势，面试可讲。

---

## 四、学习前置知识（三级分档）

> 分档依据：你自述"微信/支付宝已遗忘、MQ 想学"，以及代码实际依赖。**不根据专业推断已掌握**。三个"从原理学起"的域，教材第一章就是原理，不用课外找资料。

### 第 1 级：应当已经具备（缺了当天补 30 分钟，不阻塞进度）

| 知识域 | 掌握标准 | 用在哪 |
|--------|----------|--------|
| Spring / Spring Boot | IoC 装配、Bean 注入、配置文件 `${}` 占位符 | 全系统分层装配、配置注入 |
| HTTP / REST | 方法/状态码/请求头；知道"服务器回调服务器"（webhook）这种形态存在 | 支付宝异步通知、微信事件推送 |
| Linux / Docker | 看日志（grep/tail）、docker-compose 起服务 | 所有动手实验 |
| MySQL 基础 | 表/唯一索引/行锁概念、会写会跑简单 SQL | 订单表、唯一索引兜底幂等 |

### 第 2 级：需要重新复习（教材内嵌"复习卡"，但以你主动过一遍为准）

| 知识域 | 掌握标准 | 用在哪 | 复习触发点 |
|--------|----------|--------|-----------|
| Spring 声明式事务 | 能回答：事务边界画在哪一层、`@Transactional` 自调用为什么失效（动态代理）、事务内为什么不能发 HTTP/发 MQ | 下单/支付/关单/登录四个事务边界 | Day 4 支付宝教材前，必须过 |
| JWT | 三段结构、HS256 签名验签流程、exp 语义 | 登录态与 Security 过滤器 | Day 3 微信教材安全链前 |
| Redis 数据结构与 TTL | String 的 SET/SETNX/GETDEL 语义、TTL 作用 | 登录态、幂等键、access_token 缓存 | Day 2 微信教材前 |
| Redisson `RAtomicLong` | `addAndGet` 是原子操作、`trySet` 即 SETNX | 库存扣减、幂等网关 | Day 6 一致性教材前 |
| 数据库乐观锁/条件更新 | `UPDATE ... WHERE status='预期值'` 影响行数 0 意味着什么 | 状态机并发守卫、幂等 L2 | Day 4 支付宝教材前，与事务一起过 |
| Java 并发常识 | 原子操作 vs 锁、竞态窗口的概念 | L1/L2/L3 三层检查的存在理由 | Day 6 |

### 第 3 级：需要从原理开始学习（各配独立课时，教材承担原理讲解）

| 知识域 | 为什么从原理学 | 原理课时安排 |
|--------|----------------|--------------|
| **微信开放平台协议** | 你已遗忘，且凭证体系（access_token/ticket/openid/UnionID/JWT）是五个不同层级的概念，不先分清会全程混淆 | Day 2 上午：教材"前置知识"节，约 40 分钟，配凭证五列表 |
| **支付宝开放平台协议** | 你已遗忘；"异步通知 + 验签"不是直觉上的请求-响应模型，不理解它就看不懂回调代码里每个判断的顺序 | Day 4 上午：教材"前置知识"节，约 40 分钟（当面付/异步通知/RSA2），再进源码 |
| **RocketMQ** | 你的重点新知；at-least-once、重试、死信、延时级别四个概念是理解全部消费端代码的前提 | Day 5 上午：教材"前置知识"节，约 50 分钟，配 Topic 矩阵填空练习 |

---

## 五、学习顺序及理由

```
第 1 步  跑通系统 + 全局图景      （Day 1）   —— 先有地图，后面所有教材都知道自己在哪
第 2 步  微信扫码登录教材         （Day 2~3） —— 相对独立、链路短，适合建立"时序图→源码"的学习方法
第 3 步  支付宝支付教材           （Day 4）   —— 建立在事务/状态机复习之上，面试权重最高
第 4 步  RocketMQ 专项教材        （Day 5）   —— 你的重点；必须等支付链路学完，否则不知道消息里装的是什么
第 5 步  库存与订单一致性教材     （Day 6）   —— 超时关单/补偿 Job 属支付后半场，与库存横切面一起收口
第 6 步  升级路线与答辩面试       （Day 7）   —— 建立在"知道当前缺什么"之上，否则升级点讲不出代价
```

**为什么是这个顺序**：

- **先跑通再读书**。所有教材的"动手实验"都假设你能把系统拉起来、能看日志。第一天不解决，后面每天浪费 20 分钟。
- **微信登录先行**：外部依赖少（一个 Redis + 一个测试号）、没有事务嵌套，是练"画时序图→对照源码→默画"方法论的最佳素材。方法对了，后面加速。
- **支付在 MQ 之前**：MQ 消息的内容（tradeNo/orderNo）、发布时机（事务外）、消费语义（幂等）全部取决于支付链路的设计。反过来学就会把 MQ 学成背概念。
- **一致性收口**：库存/状态机的条件更新在支付和关单里各出现一次，单独成章做对比，比散落在两个教材里更能讲清"为什么"。
- **升级点最后**：只有能准确说出"现在哪一步会丢消息、谁会兜底"，"为什么还要 outbox"才有说服力——这正是面试官区分"背过"和"做过"的地方。

---

## 六、学习任务清单（每项有明确完成标准）

> 总规则：每项任务完成后**合上文档**自测；自测不通过，回炉该任务的教材，不要带着缺口前进。

### 阶段 0：环境跑通（Day 1）

| # | 任务 | 完成标准 |
|---|------|----------|
| 0.1 | 按 AGENTS.md 命令起 MySQL/Redis/RocketMQ 与后端、前端 | 前端页面可打开，商品列表有数据 |
| 0.2 | 完成一次账密注册→登录→加购→下单 | 数据库 `order_main`/`pay_order` 各出现一行，Redis `mall:product:stock:{id}` 减少 |
| 0.3 | 用支付宝沙箱完成一次真实支付（或阅读 `AlipayNotifyE2EIT` 理解回调如何模拟） | 订单状态变 PAID；能在日志里找到"支付回调，验签通过"一行 |
| 0.4 | 画出五条核心链路图（见 §二） | 不看文档，15 分钟内画完，参与者命名与源码类名一致 |

### 阶段 1：微信扫码登录（Day 2~3，对应 LEARN_WECHAT_LOGIN.md）

| # | 任务 | 完成标准 |
|---|------|----------|
| 1.1 | 说清 OpenID / UnionID / access_token / ticket / JWT 五个凭证各是什么、谁签发、谁消费、有效期 | 能填出"凭证五列表"（类似 module-auth §3.2 的表），且与代码值一致（Redis token TTL = 5min，access_token 缓存 110min） |
| 1.2 | 画出"取 ticket → SCAN 回调 → 轮询取 token"完整时序图 | 图中每一步能标出类名+方法名（WeixinGatewayImpl.createQrCodeTicket 等） |
| 1.3 | 沿源码讲解自动注册流程 | 能讲清 `registerWeChatUserByScan` 为什么先建 temp 用户再改名，以及 4 个写操作为何必须在同一事务（AuthApplicationService.handleWechatScanLogin） |
| 1.4 | 分析三个异常场景 | 凭证过期（轮询超时）、重复轮询（getLoginToken 读后删）、绑定冲突（bindWeChat 的幂等与冲突守卫）各会怎样，答出代码依据 |
| 1.5 | 闭卷：回答 module-auth §七 考点表全部 8 问 | 不看文档口述，每问给出源码证据 |

> 注：扫码登录的**安全设计风险**（ticket 30 天有效、portal 回调未验签等）已列入附录 D，并于 2026-10-19 经 C-7 实验实测确认成立。其中回调未验签（D-2）已登记 **S-06 并修复**（重放攻击实测被拒）；重扫重签发（D-1）列为已知限制。答辩可如实陈述"发现 → 实验确认 → 修复 → 回归验证"完整闭环。

### 阶段 2：支付宝支付（Day 4，对应 LEARN_ALIPAY_PAYMENT.md）

| # | 任务 | 完成标准 |
|---|------|----------|
| 2.1 | 说清"异步通知"协议 | 为什么支付宝不直接同步返回结果；不回 success 会触发支付宝侧重试（**重试次数与间隔是支付宝平台协议参数，以官方文档为准**，本文不引用未核实数值） |
| 2.2 | 画出回调处理链路（验签→金额校验→事务内状态机→事务外发事件） | 能指出每一步的源码行（OrderApplicationService.handleAlipayCallback L315-359） |
| 2.3 | 讲清幂等四层防御 | 验签+金额校验→状态机条件更新→事件仅流转后发布→消费端 SETNX；每层能回答"去掉它会怎样" |
| 2.4 | 分析重复回调与晚到支付 | 重复回调：状态机影响行数 0 跳过；晚到支付（已 CANCELED）：error 日志 + 人工介入，不回滚不重试 |
| 2.5 | 闭卷：回答 module-order-pay §七 考点表全部 9 问 | 同上 |

### 阶段 3：RocketMQ 专项（Day 5，对应 LEARN_ROCKETMQ.md）

| # | 任务 | 完成标准 |
|---|------|----------|
| 3.1 | 说清为什么需要 MQ（而不是 new Thread） | 从进程崩溃、削峰、解耦三个角度各给一个本项目的具体场景 |
| 3.2 | 画出 Topic 矩阵并逐个讲生产/消费方 | order_paid / order-timeout-topic / %DLQ% 三个 topic，生产者、消费者、消费组、maxReconsumeTimes 全对 |
| 3.3 | 沿源码讲解"发送在事务外" | 能指出 changeOrderPaySuccess 中 transactionService 与 publish 的先后，并回答"发送失败会怎样"（§八场景 2，含前提 P1-P3） |
| 3.4 | 分析重复消费与消费失败路径 | 消费幂等 SETNX 的三崩溃窗口（§八 W1/W2/W3）；重试 5 次进 DLQ → 告警监听 → 人工重放 |
| 3.5 | 闭卷：口述 §八四场景速查表 + 三个崩溃窗口 | 每格能给出代码依据；broker 重试间隔等未实测参数如实标注待验证 |
| 3.6 | 延时关单三道防线 | 延时消息（30min，**项目配置** delayLevel=9）→ 关单前支付宝二次确认 → Job 40min 兜底；能回答每道防线防什么、防不住什么 |

### 阶段 4：库存与订单一致性（Day 6，对应 LEARN_ORDER_STOCK_CONSISTENCY.md）

| # | 任务 | 完成标准 |
|---|------|----------|
| 4.1 | 三层检查防超卖 | L1 预检查/L2 原子扣减/L3 负库存回滚各在哪层哪行，为什么 L1 不能替代 L3（竞态窗口） |
| 4.2 | Redis 与 DB 的一致性模型 | Redis 是事实源、DB 乐观锁（`decreaseStock` 条件更新）兜底；支付成功时同步扣 DB（syncDBStockForPaySuccess） |
| 4.3 | 取消订单的库存恢复 | 为什么必须在事务提交后（P1-2：DB 回滚 Redis 无法回滚）；失败仅告警 |
| 4.4 | 下单幂等三层防御 | requestId SETNX + order_no 唯一索引 + 状态机守卫；能演示重复提交（OrderCreateIdempotencyE2EIT 场景，【文档实测】，已于 2026-10-19 复跑通过） |
| 4.5 | 补偿 Job 的扫描边界 | 能讲清 `queryNoPayNotifyOrder` 只扫 `pay_order = WAIT_PAY` 的超时单——这是"事件发送失败无补偿"缺口的精确边界（§八场景 2） |

### 阶段 5：升级路线与答辩面试（Day 7，对应 INTERVIEW_AND_DEFENSE.md）

| # | 任务 | 完成标准 |
|---|------|----------|
| 5.1 | 对 U-1~U-5 每个升级点做七问论证 | 按"现状/问题/改造/收益/成本/验证/面试表达"各写 3 句话，全部指向真实代码位置 |
| 5.2 | 简历项目描述成稿 | 3~4 条 bullet，每条可被源码/E2E 测试证据支撑（质量标准：不夸大可靠性、性能与个人贡献） |
| 5.3 | 答辩 8 分钟讲稿 | 覆盖：选题背景、架构选型（DDD/MQ/Redis 各一句为什么）、核心难点（幂等/一致性）、演示路径、升级规划 |
| 5.4 | 模拟追问 20 问 | 从 ROADMAP 答辩索引表 + 教材考点表抽 20 题，闭卷作答 |

---

## 七、源码导航（按学习顺序）

> 行号以 2026-10-11 的工作区为准；阅读时以方法名为主、行号为辅。路径前缀：`trigger` = s-pay-mall-trigger，`application` = s-pay-mall-application，`domain` = s-pay-mall-domain，`infra` = s-pay-mall-infrastructure，`start` = s-pay-mall-start。

**阶段 1 · 微信登录**

| 顺序 | 类 | 重点方法（行号） |
|------|----|------------------|
| 1 | trigger/http/LoginController.java | `weixinQrCodeTicket` L42，`checkLogin` L70（轮询入口） |
| 2 | infra/auth/login/gateway/WeixinGatewayImpl.java | `createQrCodeTicket` L49（`expire_seconds=2592000` L56，见附录 A-2/D），`getAccessToken` L135（Redis 110min 缓存 L148） |
| 3 | trigger/http/WeixinPortalController.java | `post` L93（SCAN 事件分流绑定/登录 L110-127；**接收 signature 参数但未验签**，见附录 D） |
| 4 | application/AuthApplicationService.java | `handleWechatScanLogin` L40（事务边界） |
| 5 | domain/auth/login/service/WeixinLoginService.java | `handleWechatScanLogin` L50（自动注册分支 L57-62；L51 日志打印 openid，见附录 D-2） |
| 6 | infra/auth/login/gateway/WeixinLoginGatewayImpl.java | `saveLoginToken` L41（5min）、`getLoginToken` L46（读后删 L51）、`findUserIdByOpenid` L34 |
| 7 | domain/mall/user/service/impl/MallUserServiceImpl.java | `registerWeChatUserByScan` L85、`bindWeChat` L235、`setPassword` L261 |
| 8 | domain/auth/login/service/WeixinBindService.java | 绑定票据状态管理（委托 IWeChatTokenRepository） |
| 9 | trigger/http/mall/MallAuthController.java | `/bind/qrcode`、`/bind/status`、`/bind/confirm`、`/password` 端点 |
| 10 | start/config/security/ 下 SecurityConfig + JwtAuthenticationFilter | 白名单 L52-74；过滤器解析失败清空上下文 L75 |
| 测试 | start/src/test/java/cn/fcr/test/WeixinScanLoginMockE2EIT.java、AuthBindMockE2EIT.java | 【文档实测】2026-10-07 通过；当前工作区已于 2026-10-19 复跑通过 |

**阶段 2 · 支付宝支付**

| 顺序 | 类 | 重点方法（行号） |
|------|----|------------------|
| 1 | application/OrderApplicationService.java | `createOrder` L169（幂等守门+事务编排） |
| 2 | application/OrderTransactionService.java | `createOrderInTransaction` L61（库存预扣+落单+支付单，catch 中回滚库存 L82-89） |
| 3 | domain/order/service/PayOrderService.java | `generatePayUrl` L36、`verifyCallbackSign` L64 |
| 4 | trigger/http/AliPayController.java | `payNotify` L43（仅协议适配） |
| 5 | application/OrderApplicationService.java | `handleAlipayCallback` L315（trade_status→验签→TD-5 金额校验） |
| 6 | application/OrderTransactionService.java | `changeOrderPaySuccessInTransaction` L102（事务内状态推进） |
| 7 | domain/order/service/impl/OrderStateMachineServiceImpl.java | `paySuccess` L36（条件更新守卫 L51-56）、`cancel` L150 |
| 8 | application/OrderApplicationService.java | `changeOrderPaySuccess` L369（CANCELED 守卫 L374、事务外发事件 L386-391） |
| 9 | infra/order/gateway/AlipayQueryGatewayImpl.java | `queryTradeSuccess` L27（code=10000 + TRADE_SUCCESS 双重校验） |
| 测试 | AlipayNotifyE2EIT.java | 【文档实测】；已于 2026-10-19 复跑通过 |

**阶段 3 · RocketMQ**

| 顺序 | 类 | 重点方法（行号） |
|------|----|------------------|
| 1 | infra/order/event/RocketMqOrderEventPublisher.java | `publishPaySuccess` L25（syncSend 3000ms L34；catch-all 仅日志 L37-40 ←场景 2 缺口） |
| 2 | infra/order/gateway/OrderPaymentGatewayImpl.java | `sendDelayCloseMessage` L35（delayLevel=9 即 30min L42，**项目配置**；失败仅日志但有 Job 兜底） |
| 3 | trigger/listener/OrderPaidRocketListener.java | `onMessage` L42（SETNX 幂等 L48、异常 release+重投 L64-65、maxReconsumeTimes=5 L22；**成功路径不释放**，见 §八窗口分析） |
| 4 | trigger/listener/OrderTimeoutCloseRocketListener.java | `onMessage` L34 |
| 5 | application/OrderApplicationService.java | `handleTimeoutCloseOrder` L470（支付宝二次确认 L478）、`sendPaySuccessNotification` L402 |
| 6 | trigger/listener/DlqAlertListeners.java | 两个 DLQ 告警监听 L21-50 |
| 7 | trigger/job/NoPayNotifyOrderJob.java | `exec` L57（Redisson 锁 L62）、`compensateNoPayNotify` L85（5min 补单，**无逐条隔离**，A-6）、`compensateStaleUnpaidOrders` L103（40min 兜底关单） |
| 8 | infra/mall/product/gateway/IdempotentGatewayImpl.java | `tryAcquire` L45（SETNX）、`markDone` L89、`release` L63 |
| 9 | domain/order/adapter/event/PaySuccessMessageEvent.java | 消息契约（topic 常量、字段 tradeNo/orderNo） |
| 测试 | TimeoutCloseOrderE2EIT.java、OrderCreateIdempotencyE2EIT.java | 【文档实测】；已于 2026-10-19 复跑通过 |

**阶段 4 · 库存与一致性**

| 顺序 | 类 | 重点方法（行号） |
|------|----|------------------|
| 1 | domain/order/service/impl/MallOrderServiceImpl.java | `checkAndDeductStock` L44（L1 预检查+边扣边回滚）、`paySuccess` L116、`restoreDeductedStock` L75 |
| 2 | infra/mall/product/gateway/StockGatewayImpl.java | `deductStock` L35（L2 原子扣减+L3 负库存回滚 L45-49）、`syncDBStockDeduct` L127（DB 乐观锁）、`restoreStock` L56 |
| 3 | domain/.../OrderStateMachineServiceImpl.java | `syncDBStockForPaySuccess` L75、`restoreStockForCancel` L186 |
| 4 | trigger/job/StockPreheatRunner.java | `run` L42（启动预热 @Order(2)） |
| 5 | **infra/.../dao/order 层 `IPayOrderGateway.queryNoPayNotifyOrder`** | 扫描条件 = `pay_order = WAIT_PAY` 且创建超 5min——**只扫待支付单**，理解场景 2 缺口的钥匙（§八） |
| 6 | domain/order/model/entity/OrderState.java | 状态枚举与 DB 状态映射（TD-4 口径） |
| 配置 | start/resources/application*.yml | RocketMQ namesrv、Redis、`security.jwt.secret: ${JWT_SECRET}`（L115-116） |

**贯穿：工程与安全**
- trigger/http/mall/GlobalExceptionHandler.java：`onJwtException` L124（→0003）、`onException` L143（不透传 message）
- infra/auth/token/JwtTokenProvider.java：L22-23（`@Value` 注入，**无硬编码默认密钥**）
- start/resources/application-dev.yml L115-116 / application-prod.yml L115-116：`security.jwt.secret: ${JWT_SECRET}`
- domain 层零 Spring 注解验证：任意 domain 类文件头无 `@Service/@Autowired`（P0-3/H1 守卫）

---

## 八、关键技术风险清单（需要特别验证的问题）

> 标记说明：✅ 代码已实现；⚠️ 代码已实现但存在已知缺口；❌ 设计中有、代码未实现（升级点，不得当作已实现讲）。本节分析性结论为【代码推导】，凡涉及 broker/支付宝行为的参数标注【待验证】。

### 幂等

| 风险 | 现状 | 证据 |
|------|------|------|
| 重复下单 | ✅ requestId SETNX（24h，PROCESSING/结果分态）+ `order_no` 唯一索引兜底 | OrderApplicationService L169-217 |
| 支付宝重复回调 | ✅ 状态机条件更新（影响行数 0 跳过）+ 事件仅流转后发布（TD-6） | OrderStateMachineServiceImpl L51-56 |
| MQ 重复投递 | ✅ 消费端 SETNX：**24h 窗口内的重复投递抑制**（非严格 exactly-once，见下方崩溃窗口分析） | OrderPaidRocketListener L47-66 |
| ⚠️ 消费幂等键与业务非原子 | U-3 已登记：SETNX 与业务提交非同一事务，宕机中间态理论上可能漏处理/重复（窗口 W1/W3） | ROADMAP U-3 |

### 一致性

| 风险 | 现状 | 证据 |
|------|------|------|
| DB 与 Redis 库存不一致 | ✅ Redis 为事实源；支付成功事务内同步扣 DB（乐观锁）；DB 失败仅告警 | OrderStateMachineServiceImpl L75-97 |
| 取消订单库存恢复 | ✅ 事务提交后恢复 Redis（P1-2），失败仅 error 日志人工补偿 | OrderApplicationService L497-503 |
| ⚠️ 订单已 PAID 但 order_paid 事件发送失败 | ❌ 无自动补偿：publish 失败仅日志（双重吞异常），且补单 Job 只扫 WAIT_PAY 单、扫不到已 PAID 单 → 微信通知丢失靠人工。**"不构成资损"仅在下方 P1-P3 前提下成立** | RocketMqOrderEventPublisher L37-40；NoPayNotifyOrderJob L85-96 |
| ⚠️ 延时关单消息发送失败 | ⚠️ 发送失败仅日志；40min Job 兜底关单覆盖（TD-7）——**此 topic 与 order_paid 不同，有补偿** | OrderPaymentGatewayImpl L44-46、NoPayNotifyOrderJob L103 |
| 事务提交后、发消息前进程崩溃 | ❌ 无 outbox，同上一行缺口 | U-2 未实现 |

### 并发

| 风险 | 现状 | 证据 |
|------|------|------|
| 库存超卖 | ✅ 三层检查（L1 预检/L2 原子扣减/L3 负库存回滚） | module-stock §2.2 |
| 支付与关单并发（钱已收订单被关） | ✅ 三道防线：30min 窗口 + 关单前支付宝二次确认 + 晚到支付显式异常路径 | handleTimeoutCloseOrder L470-488、changeOrderPaySuccess L374-377 |
| 多实例定时 Job 重复执行 | ✅ Redisson tryLock（60s 租约） | NoPayNotifyOrderJob L59-73 |
| ⚠️ Job 补单循环无单条隔离 | ⚠️ `compensateNoPayNotify` 单条异常会中断整轮（仅兜底关单有逐条 try/catch） | NoPayNotifyOrderJob L90-95 |

### MQ 职责边界（已核实，禁止推广）

> **order_paid 当前唯一的业务副作用是"微信模板消息通知"**：可重入、无资金语义、丢失仅影响体验【代码推导】。
> **order-timeout-topic 的职责是"超时关单触发器"**：发送失败有 Job 40min 兜底（TD-7）。
> **禁止推广**：以下可靠性结论全部绑定当前职责划分。若未来把发货、结算、积分等**有资金或资源语义**的履约下沉到 MQ 消费端，"事件发送失败仅损失通知"的结论立即失效，缺口升级为资损级，必须先落实 U-2 outbox 或等价机制。

### 消息可靠性四场景速查表（你的重点，逐项核对结论）

| 场景 | 机制（代码依据） | 当前结果 | 兜底与缺口 | 证据级别 |
|------|------------------|----------|------------|----------|
| **1. 事务提交失败** | 异常穿透 → `GlobalExceptionHandler` 0001 JSON → 支付宝收到非 success 触发其侧重试；且 Job 补单（WAIT_PAY>5min 主动查单）最终驱动履约 | 订单保持 INIT，经重投或补单最终履约 | 双层兜底，无缺口。支付宝的重试次数/间隔是**平台协议参数，以官方文档为准**，本文不引用未核实数值（module-order-pay 转述"24h/7 次"→【待验证 C-6】） | 代码推导；重放行为【文档实测】（AlipayNotifyE2EIT，已于 2026-10-19 复跑通过） |
| **2. 提交成功、发送失败**（含发送前崩溃） | publish 双重吞异常（OrderApplicationService L386-391；Publisher L37-40）；补单 Job 扫描条件为 WAIT_PAY，扫不到已 PAID 单 | 订单 PAID 正确；消息不存在 → **微信通知丢失** | 无自动补偿，人工看日志兜底。"不构成资损"**仅在 P1-P3 前提成立时有效**（见下） | 代码推导 |
| **3. 发送成功、消费失败** | catch → release 幂等键 → 抛异常 → broker 重试（maxReconsumeTimes=5，**项目配置**）→ DLQ → 告警监听 ERROR 日志 | 重试期间 paySuccess 幂等空转，无资金副作用；耗尽进死信 + 告警 | 人工控制台重放；自动化重放 ❌ 未实现（U-5 二期）。⚠️ 若消费端**宕机**（未走 catch），幂等键残留 PROCESSING → 重放被误跳（窗口 W1）。**补充边界**：释放幂等键并触发 broker 重试的路径，仅适用于异常实际传播到 Listener 的情况——`sendPaySuccessNotification` 内部吞掉的通知发送异常（OrderApplicationService L425-427）不会传播到 Listener：此时 Listener 正常返回、消息被确认，微信通知失败不会自动触发重试，且无独立补偿机制【代码推导】 | 代码推导；broker 重试间隔/DLQ 转移时机【待验证 C-5】 |
| **4. 消费成功、确认失败**（未 ACK/消费端宕机） | 成功路径幂等键不释放（24h TTL，OrderPaidRocketListener L47-53 注释明确）；重投 → SETNX 拦截 → 正常 return → ACK | **24h 窗口内**重复投递被抑制 | 窗口 W3（>24h 迟到重投，理论场景）→ 通知重复发送，无资金后果（当前职责边界下） | 代码推导；broker 重投窗口【待验证 C-5】 |

**前提 P1-P3（"场景 2 不构成资损"的适用边界）**：

- **P1（职责边界）**：order_paid 当前唯一副作用是微信通知，无资金/库存语义；资金语义的状态推进在回调事务内同步完成【代码推导】。
- **P2（重复容忍）**：通知重复发送不产生资金后果，仅体验问题【代码推导】。
- **P3（状态正确性）**：即使事件丢失，订单本身仍保持正确的 PAID 状态（状态推进在回调事务内已完成）；但通知无法通过该条**未成功发送**的消息自动恢复，也不存在 DLQ 重放路径（消息从未到达 broker）。人工补发能力与具体操作流程尚需核实，不应宣称已有完善的补发机制【代码推导】。
- 任一前提被未来改动破坏（履约下沉消费端），本结论即刻失效——面试中必须主动声明此前提，这正是区分"背答案"与"懂边界"的地方。

**幂等键与消息确认之间的三个崩溃窗口（"恰好一次"的严谨替代表述）**：

SETNX 幂等键提供的是 **24h 窗口内的重复投递抑制**，不是严格 exactly-once。以下三个窗口期间进程崩溃，行为不同：

- **W1（SETNX 成功 → 业务完成前宕机）**：幂等键残留 `PROCESSING`（release 只在 catch 路径，无 finally/看门狗补偿）→ 24h 内的重投/人工重放被 SETNX 跳过 → **该条消息漏处理**（当前后果=通知丢失）。U-3（消费记录表与业务同事务）针对此窗口。
- **W2（业务完成 → ACK 前宕机）**：broker 重投 → SETNX 拦截 → 正常 return → ACK。期望行为，24h 内有效。
- **W3（幂等键 24h 过期后的迟到重投）**：SETNX 成功 → 业务重复执行 → 通知重复发送。窗口外无保护；当前职责边界下后果限于体验。

---

## 九、面试与答辩目标

完成全部阶段后，你应当能**独立**（不看文档）完成以下四类任务：

**A. 画图**：五条核心时序图（登录/下单/回调/消费/关单）+ DDD 分层依赖图 + Topic 矩阵图。

**B. 源码讲解**：任指一个方法（如 `changeOrderPaySuccess`），能说出它的调用方、事务边界、内部每一步调用的下一层方法、关键判断的代码行。

**C. 场景分析**（面试官/答辩老师的追问形式）：
1. "支付宝重复通知多次，你的系统会发生什么？" → 四层防御逐层应答（重试次数是平台协议参数，以官方文档为准，不背数字）
2. "订单关单的那一秒用户付了钱怎么办？" → 三道防线
3. "消息发出时丢了怎么办？" → 现状缺口 + 前提 P1-P3 + U-1/U-2 演进（❌ 不得声称已实现）
4. "消费成功但 ACK 失败呢？" → 24h 窗口抑制 + W1/W2/W3 三窗口，明确说"不是严格 exactly-once"
5. "为什么用 Redis 扣库存而不是数据库？" → 性能 + 原子性 + 最终一致性模型
6. "微信登录为什么要轮询而不是长连接？" → 实现成本/容错 vs SSE 升级路径
7. "Guava 和 MQ 什么关系？" → 没有关系：Guava Cache 是初始版本的冗余 Bean（无消费方），已随 P1-5 移除；登录态缓存由 Redis 承担；MQ 解决的是异步解耦与延时调度（Git 史实，见附录 B）

**D. 升级论证**：任选一个升级点（outbox / 消费记录表 / OSS / 快递鸟），按七问表做 1 分钟陈述 + 3 分钟追问应对。

**简历可写（均有源码/E2E 支撑，表述已按审计口径收紧）**：
- 基于支付宝异步回调设计多层校验与幂等防护，包括回调验签、金额校验、订单状态条件更新及 RocketMQ 消费端重复投递抑制，降低重复回调与重复消费风险（各机制的适用边界见 §八，不宣称端到端"零资损"）
- 基于 RocketMQ 延时消息实现 30min 超时关单，配套支付宝二次确认 + 分布式锁定时任务双兜底
- Redis RAtomicLong 三层检查防超卖，DB 乐观锁最终一致兜底
- 微信扫码登录：ticket 一次性消费 + JWT + Redis 登录态，双登录方式经 user_binding 绑定同一账户

---

## 十、7 天学习日程（每天六要素，约 2h/天）

### 总规则（顺延机制）

1. **验收四维**：每天必须通过 ①原理理解 ②源码定位 ③动手验证 ④闭卷表达（口述或画图）四个维度验收。**"读完文档"不计完成**。
2. **顺延规则**：任一维度未过 → 当天标记"未通过"，该天剩余任务顺延，后续日程整体平移，总周期弹性 7~9 天。宁可拆任务，不压缩验证环节。
3. **卡点处理**：同一主题顺延超过 2 天仍不通过 → 停下回 §四检查前置知识缺口（通常是第 2 级某项没真正过），补齐后再推进，不硬推。
4. **证据诚实**：动手实验结果与闭卷回答中，凡引用未实测的行为（broker 重试、支付宝重试等），必须口头标注"待验证"，养成与附录 C 一致的诚实口径。

### Day 1 — 跑通系统，建立全局地图

- **目标**：能亲手走完"注册→加购→下单→支付"并说出系统分层。
- **阅读**：AGENTS.md；STUDY_GUIDE §一/§二；README §一/§二（10 分钟）。
- **核心问题**：这个系统的"钱"从哪一步到哪一步？DDD 四层各不许做什么？
- **动手验证**：完成一次账密下单（支付可用沙箱或跳过）；查 `order_main`/`pay_order` 各一行、`mall:product:stock:{id}` 减少；日志中找到下单与库存扣减两行。
- **闭卷任务**：五条链路图（可对照画一遍后合书重画）。
- **完成标准**：合书 15 分钟画出五条链路，参与者用真实类名（如 `OrderPaidRocketListener`，不是"消费者"这种泛称）；答不出"Domain 层为什么不能有 `@Transactional`"则 Day 1 不通过。

### Day 2 — 微信协议原理 + 登录主流程

- **目标**：从原理分清五个凭证，沿源码走通"取 ticket → SCAN 回调 → 轮询取 JWT"。
- **阅读**：微信教材"前置知识"+"完整流程"节（约 40min 原理）；module-auth §二/§三；源码 `WeixinGatewayImpl`（L49、L135）、`WeixinLoginGatewayImpl`（L34-54）、`WeixinPortalController`（L93-127）、`WeixinLoginService`（L50-84）。
- **核心问题**：微信服务器为什么不能直接通知浏览器？ticket→JWT 为什么存 Redis 而不是 JVM 内缓存（Guava 史实）？`getLoginToken` 为什么读后删？
- **动手验证**：调 `/pay-api/v1/login/weixin_qrcode_ticket` 拿到真实 ticket；Redis 里看 `wechat:access_token:{appid}`（TTL ≈ 6600s）；微信扫码后观察 SCAN 回调日志与 ticket→JWT 的 key（TTL 300s）。
- **闭卷任务**：画登录时序图（类名+方法名级），标注三个凭证的 TTL。
- **完成标准**：不看资料填出凭证五列表（谁签发/谁消费/有效期/存哪）；能解释"重复轮询同一 ticket 第二次返回什么"（Redis key 已删 → 0003）。

### Day 3 — 微信登录异常、绑定与安全链

- **目标**：掌握三类异常处理与账号绑定守卫，闭合微信模块；能讲清附录 D 两项已实测确认的安全风险（C-7，2026-10-19）及其修复方案。
- **阅读**：`MallUserServiceImpl`（`registerWeChatUserByScan` L85、`bindWeChat` L235、`setPassword` L261）；`MallAuthController` 绑定端点；`SecurityConfig`（L50-77）+ `JwtAuthenticationFilter`（L54-81）+ `GlobalExceptionHandler`（`onJwtException` L124）；附录 D。
- **核心问题**：`bind/confirm` 的 userId 为什么必须从 JWT 取而不是请求参数（IDOR）？重复绑定与冲突绑定分别走哪个分支？非法 token 访问受保护端点，谁返回 401？
- **动手验证**：同一 ticket 调两次 `check_login`（第二次应 0003）；尝试绑定他人已绑定的 openId（应 0001）；无 token 调 `/mall-api/v1/orders`（应 HTTP 401 + code 0003）。
- **闭卷任务**：module-auth §七 8 问口述（每问给源码证据）+ 附录 D 两个已实测风险点各用 2 句话讲清"问题是什么、处置是什么"（注明：D-2 已登记 S-06 并修复、D-1 重扫为已知限制）。
- **完成标准**：8 问中 6 问以上能给出类名/方法级证据；三场景（过期/重复/冲突）各能用代码行号讲清。

### Day 4 — 支付宝协议原理 + 回调主链路

- **目标**：理解异步通知协议；沿源码走通"验签→金额校验→事务内状态机→事务外发事件"。
- **阅读**：支付宝教材"前置知识"（异步通知/RSA2/重试策略，约 40min，重试参数以官方文档为准）；module-order-pay §二~§四；源码 `AliPayController`（L43-56）、`OrderApplicationService.handleAlipayCallback`（L315-359）与 `changeOrderPaySuccess`（L369-392）、`OrderTransactionService`（L102-112）、`OrderStateMachineServiceImpl.paySuccess`（L36-66）。
- **核心问题**：为什么必须回 `"success"`（不回会触发支付宝侧重试，具体参数待验证 C-6）？四层幂等各自防什么重复？"事件仅在实际流转后发布"（TD-6）解决的是什么？**事务边界到底画在哪条线的前后？**
- **动手验证**：跑 `AlipayNotifyE2EIT`（或沙箱回调），日志中确认顺序：验签通过 → 金额一致 → 状态机流转 → 发事件；同一回调重放，断言事件计数恰为 1【文档实测，2026-10-19 已复跑通过】。
- **闭卷任务**：画回调链路图，用一条竖线标出事务边界，竖线左侧/右侧各写了什么。
- **完成标准**：能不看代码指出"金额校验在事务外、状态推进在事务内、发消息在事务外"三层各在哪一行；答错事务边界位置则当天不通过。

### Day 5 — RocketMQ 原理 + 消息全生命周期

- **目标**：从 at-least-once 原理出发，掌握生产、消费、重试、死信全链路。
- **阅读**：MQ 教材"前置知识"（broker/topic/consumer group/at-least-once/重试/DLQ/延时级别，约 50min）；module-order-pay §五/§六；源码 `RocketMqOrderEventPublisher`（L25-41）、`OrderPaymentGatewayImpl.sendDelayCloseMessage`（L35-47）、`OrderPaidRocketListener`（L42-67）、`OrderTimeoutCloseRocketListener`、`DlqAlertListeners`（L21-50）、`IdempotentGatewayImpl`（L45-61）。
- **核心问题**：发送成功为什么不等于消费成功？消费者为什么要自己幂等而不是依赖 broker？重试 5 次之后谁发现这条消息？
- **动手验证**：停掉 MySQL（或 Redis），通过 RocketMQ 控制台向 `order_paid` 发一条测试消息，观察消费重试 → 进入 `%DLQ%s-pay-mall-order-paid-consumer` → `DlqAlertListeners` 打 ERROR 日志（broker 重试间隔以本地 docker 配置为准，**物理间隔待验证 C-5**）。
- **闭卷任务**：画 Topic 矩阵（topic/生产者/消费者/消费组/maxReconsumeTimes/死信去向），口述一条消息从出生到 DLQ 的一生 + 四场景速查表逐格讲解 + W1/W2/W3 三窗口。
- **完成标准**：能回答 §八四场景表每格的"结果/兜底/缺口"；能解释"消费幂等键为什么成功不释放、异常要释放"；broker 相关未实测参数能主动说"待验证"。

### Day 6 — 超时关单、补偿 Job 与一致性收口

- **目标**：掌握关单三道防线与"事务内/外"原则在库存侧的应用；吃透补偿 Job 的扫描边界。
- **阅读**：module-stock 全章；`OrderApplicationService.handleTimeoutCloseOrder`（L470-488）、`NoPayNotifyOrderJob`（L57-116）、`MallOrderServiceImpl.checkAndDeductStock`（L44-65）、`StockGatewayImpl`（L35-53、L127-146）、`OrderStateMachineServiceImpl.cancel`（L150-183）与 `restoreStockForCancel`（L186-221）；`IPayOrderGateway.queryNoPayNotifyOrder` 的 WAIT_PAY 扫描条件。
- **核心问题**：关单前为什么要先问支付宝（`queryTradeSuccess` 的双重校验 L41-52）？库存恢复为什么必须在事务提交后（P1-2）？**为什么"事务成功但事件丢失"的订单永远进不了补单 Job 的扫描范围？**
- **动手验证**：`redis-cli` 把某商品 `mall:product:stock:{id}` 改为 1 后并发两次下单，观察 L3 负库存回滚日志；读 `TimeoutCloseOrderE2EIT` 并指出测试如何制造"延时消息丢失→Job 兜底"场景。
- **闭卷任务**：画"下单→支付→关单"中库存与状态的变化图（Redis 库存、DB 库存、order_main、pay_order 四条线）。
- **完成标准**：四线变化图全对；能讲出三道防线各防什么、防不住什么（残余窗口）；能不看代码回答场景 2 缺口的精确边界（WAIT_PAY 扫描条件）。

### Day 7 — 升级点、简历与答辩

- **目标**：把"知道缺什么"转化为"能论证为什么值得改"。
- **阅读**：ROADMAP §二（U-1~U-5）；STUDY_GUIDE 附录 A/B/C/D；通读 2 个 E2EIT（建议 `AlipayNotifyE2EIT` + `OrderCreateIdempotencyE2EIT`）。
- **核心问题**：每个升级点的代价是什么？哪个缺口是"通知级"、哪个在你改动后会变成"资损级"（P1 前提被破坏的场景）？
- **动手验证**：环境就绪则跑 `mvn verify -Dtest='*E2EIT'`；不就绪则记录"待验证"并在答辩前补跑（附录 C-1）。
- **闭卷任务**：20 问模拟（从考点表+答辩索引抽）+ 8 分钟答辩讲稿录音自听一遍。
- **完成标准**：简历 3~4 条 bullet 成稿且每条有源码/E2E 证据；U-1~U-5 任选两个做七问表陈述各 1 分钟。

**拆分说明**：原文档曾把"支付宝全章"压进一天、"MQ 全章"压进一天，超出 2h 预算；现按"回调主链路（Day 4）/ 延时关单+Job（Day 6）"与"MQ 生产消费（Day 5）/ 延时消息与补偿（Day 6）"拆分归位。总天数不变，顺延规则兜底超支。

---

## 附录 A：文档与代码差异清单（六字段逐项）

> 按 AI_AGENT_RULES §3.2 标注 DOCUMENT_CODE_MISMATCH。差异已在各教材生成前登记，教材口径以代码为准。
> **2026-10-19 全部六项已按本清单完成原设计文档修正**（A-1/A-2 module-auth、A-3 SECURITY_ISSUES S-01、A-4/A-5 module-order-pay + docs/design/README、A-6 module-order-pay §8.2），清单保留作为审计轨迹。

### 重点链路复核结论（支付回调 → 事件 → 消费 → 通知 → Job 的实际关系）

沿源码逐步确认（本轮再次核对，结论与审计一致）：

```
AliPayController.payNotify (L43-56)
  → OrderApplicationService.handleAlipayCallback (L315)
      ① trade_status 判断（PayTradeStatus.isSuccess，L317-321）
      ② 验签 payOrderService.verifyCallbackSign → IPayGateway.verifySignature (PayOrderService L64)
      ③ 金额一致性校验 total_amount vs 本地订单 (L341-355，仅当订单存在时)
      ④ changeOrderPaySuccess(orderNo, tradeNo) (L357)
  → changeOrderPaySuccess (L369)
      ⑤ 订单已 CANCELED → 仅 error 日志，不履约不发事件，但 handleAlipayCallback 仍 return true → 回支付宝 "success"（L374-377 + L358）
      ⑥ 【事务内】orderTransactionService.changeOrderPaySuccessInTransaction (L380；@Transactional 在 OrderTransactionService L102)
            → MallOrderServiceImpl.paySuccess (L116) → 状态机 paySuccess (L36-66)：
              canPay 守卫 → 条件 UPDATE order_main INIT→PAID（影响行数 0 即返回 false）→ pay_order→PAID → 同步扣 MySQL 库存
      ⑦ 仅 transitioned=true 时 → orderEventPublisher.publishPaySuccess (L386-391，【事务外】，失败仅 warn)
  → RocketMqOrderEventPublisher.publishPaySuccess (L25-41)：syncSend 3000ms；catch-all 仅日志 (L37-40)
  → OrderPaidRocketListener.onMessage (L42-67)：SETNX 幂等 → orderApplicationService.paySuccess (L56)
      → 状态机 paySuccess：订单已 PAID，canPay()=false (L45-48) → 返回 false，无任何写操作（空转确认）
      → sendPaySuccessNotification (L59)：查 openid → 微信模板消息；内部异常全部吞掉 (L425-427)

NoPayNotifyOrderJob（每 30s，Redisson 锁）：
  → 补单：pay_order = WAIT_PAY 且创建 > 5min → 支付宝 queryTradeSuccess → changeOrderPaySuccess (重驱 ④-⑦ 全链路)
  → 兜底关单：WAIT_PAY 且创建 > 40min → handleTimeoutCloseOrder (二次确认 → 关单或转履约)
```

三个关键结论：

1. **状态推进的权威路径是回调事务本身**，MQ 消费侧是幂等空转 + 通知编排【代码推导】。
2. **Job 补单覆盖的是"支付宝回调根本没到达"**（pay_order 仍是 WAIT_PAY）；**不覆盖"事务已提交但事件发布失败"**（此时 pay_order 已是 PAID，不在 `queryNoPayNotifyOrder` 扫描条件内）【代码推导】。这是 A-4 的实质，也是场景 2 缺口的精确边界。
3. **晚到支付（CANCELED 后回调到达）走人工路径**：不回滚、不重试、不发事件，只 error 日志，但对支付宝回 success 终止其重试【代码推导】。

### 逐项六字段

**A-1｜module-auth.md §3.1 轮询检测的对象**
- 原文档描述："前端通过短轮询检测 Redis 中是否出现 openid"。
- 实际代码行为：`WeixinLoginGatewayImpl.saveLoginToken(ticket, token)`（L41-44）存的是 JWT，TTL 5min；`getLoginToken`（L46-54）读后删。openid 只在 SCAN 回调（`WeixinPortalController` L122）与绑定状态（`WeixinBindService`）中出现。
- 证据：`s-pay-mall-infrastructure/.../auth/login/gateway/WeixinLoginGatewayImpl.java` L41-54；`domain/auth/login/service/WeixinLoginService.java` L70。
- 影响：学习——会把"登录态凭证"误学成"openid 传递"，进而无法理解一次性消费设计；简历——若写"轮询检测 openid"会被读过源码的面试官追问穿帮；答辩——低风险。
- 是否需要修正文档：**是**。module-auth §3.1 改为"检测 Redis 中是否出现 JWT"。

**A-2｜module-auth.md §2.1 ticket 有效期**
- 原文档描述："Ticket 由微信 API 生成，有效期 1800s（30 分钟），扫码后 Ticket 立即失效"。
- 实际代码行为：`WeixinGatewayImpl.createQrCodeTicket` L56 请求 `expire_seconds=2592000`（30 天，临时二维码上限）；scene_id 固定 100601。扫码后失效是微信行为，这部分文档表述成立。
- 证据：`WeixinGatewayImpl.java` L55-63。
- 影响：学习——低（真正兜底的是 Redis token 5min + 一次性消费）；简历/答辩——若被问"ticket 多久过期"答 30 分钟会与代码不符。**安全副产物**：ticket 30 天有效 + 固定 scene_id 意味着二维码图片在 30 天内可被保存重扫，登录安全性完全依赖 Redis 层的 5min 一次性 token。已连同 portal 回调验签问题一并列入附录 D（**2026-10-19 经 C-7 实验实测确认**；验签问题已登记 S-06 并修复，重扫为已知限制）。
- 是否需要修正文档：**是**，数值改为 2592000s，并引用附录 D。

**A-3｜SECURITY_ISSUES.md S-01 JWT 密钥**
- 原文档描述："默认密钥硬编码（JwtTokenProvider.java:22）+ prod 密钥配置缺失"，状态"部分缓解"。
- 实际代码行为：`JwtTokenProvider` L22-23 为 `@Value("${security.jwt.secret}")`，**无默认值**；`application-dev.yml` L115-116 与 `application-prod.yml` L115-116 均为 `${JWT_SECRET}`。环境变量缺失时启动直接失败，不存在静默使用弱密钥的路径。
- 证据：上述三处；工作区未提交的 `s-pay-mall-start/.env.example`（JWT_SECRET 占位，注入链路见 C-2）。
- 影响：学习——按 S-01 原文会以为系统仍有硬编码后门，口径错误；简历——"密钥经环境变量注入、无硬编码默认"是加分点；答辩——被问安全设计应讲新口径。
- 是否需要修正文档：**是**。S-01 状态更新为"已修复（密钥注入化）"，残余仅保留"仓库 public + 历史密钥在 git 历史中"。

**A-4｜module-order-pay.md 与 README §二 "异步履约"**
- 原文档描述：README 全景图节点"异步履约：订单状态推进 + 微信模板消息通知"；module-order-pay §一"通过 RocketMQ 解耦后置履约（支付成功通知、库存同步、微信模板消息）"——原文连"库存同步"也列在 MQ 侧。
- 实际代码行为：状态推进（order_main/pay_order/DB 库存）在回调事务内同步完成（`OrderTransactionService.changeOrderPaySuccessInTransaction` L102-112 → 状态机 L36-97）；MQ 消费侧 `paySuccess` 因 `canPay()` 守卫空转，实际执行的异步工作只有 `sendPaySuccessNotification`。**库存同步根本不经 MQ**。
- 证据：`OrderStateMachineServiceImpl.java` L36-97；`OrderPaidRocketListener.java` L56-59；`OrderApplicationService.sendPaySuccessNotification` L402-428。
- 影响：学习——全系统最容易讲错的一条线；简历——"MQ 驱动履约"属夸大，但"MQ 承载支付成功通知的异步处理，配合消费幂等与 DLQ 告警"完全真实可写；答辩——高危。
- 是否需要修正文档：**是**，两处都改，时序图节点标注"状态推进已在回调事务内完成；此处为幂等确认 + 通知编排"。

**A-5｜ROADMAP U-1 与 module-order-pay 时序图的口径冲突**
- 原文档描述：ROADMAP U-1 备注"现状支付履约在回调事务内同步完成"（与代码一致）；module-order-pay §三时序图画"异步履约 → 订单状态推进 + 微信模板消息"（与代码不一致）。
- 实际代码行为：同 A-4。
- 证据：同 A-4。
- 影响：同一仓库两份文档互相矛盾，按 AI_AGENT_RULES §3.2"以更接近代码事实的为准"——ROADMAP 对、module-order-pay 图错。
- 是否需要修正文档：**是**，修 module-order-pay（见 A-4）。

**A-6｜module-order-pay §8.3 Job 单条失败隔离**
- 原文档描述："逐个走 handleTimeoutCloseOrder…单条失败隔离（记日志继续后续）"。
- 实际代码行为：逐条 try/catch 只存在于 `compensateStaleUnpaidOrders`（L108-115）；`compensateNoPayNotify`（L90-95）无逐条保护，单条异常上抛至 `exec` 总 catch（L77-79）中断整轮（兜底关单分支也被跳过）。
- 证据：`trigger/job/NoPayNotifyOrderJob.java` L85-116。
- 影响：学习/面试——被问"Job 健壮性"时如实答；低风险小缺陷，也可作为"我发现并登记的改进点"讲。
- 是否需要修正文档：**是**，§8.2 补一句"补单分支无逐条隔离，单条异常中断整轮，已登记"。

---

## 附录 B：Guava 史实（Git 考证结论）

| 事实 | 证据 |
|------|------|
| 初始提交（223b6ba，课程模板项目导入）存在 `GuavaConfig`：`CacheBuilder` 构建 `Cache<String,String>`，`expireAfterWrite(3s)`；该快照全仓检索**无任何消费方**，属冗余 Bean | 历史路径 s-pay-mall-ddd-fcr-app/.../GuavaConfig.java |
| 移除提交（7fea58a，P1-5）：随 Domain 层依赖清理删除 guava 依赖 | `git log -S "guava"` |
| 登录态临时凭证的实际承担者：JVM 内本地缓存（无消费方）→ **Redis**（`ticket→JWT`，5min，一次性消费，`WeixinLoginGatewayImpl`） | 现行代码 |
| RocketMQ 与 Guava **无替代关系**：MQ 用于 order_paid 异步处理与延时关单；Guava 从未承担 MQ 职责 | ROADMAP、module-order-pay、Git 史实 |
| **结论**：简历/答辩禁止出现"Guava 换成 MQ"的表述。正确表述："登录临时凭证由 JVM 本地缓存演进为 Redis 分布式存储（跨实例共享、TTL 可控、一次性消费）；支付成功后的异步处理由同步调用演进为 RocketMQ 解耦 + 延时消息 + DLQ 告警。" |

---

## 附录 C：待验证清单（不阻塞学习，答辩前必须清零或形成口径）

| # | 事项 | 验证方式 | 关联 |
|---|------|----------|------|
| C-1 | E2E 全量回归在当前工作区（含未提交的 .env 改造）下是否通过 | **已于 2026-10-19 复跑通过**：`mvn -P e2e verify`，5 个 E2EIT 类 17/17 用例全过，BUILD SUCCESS。前置修复：环境库 product 表补 `image_url` 列（代码先行、schema 漂移所致，见附录 A 增补） | §七全部【文档实测】标记已在当前工作区复核，视为实测 |
| C-2 | `${JWT_SECRET}` 注入链路（.env → DotenvEnvironmentPostProcessor → yml）在 dev 启动时无缺失 | 清空 JWT_SECRET 启动应失败（无默认值=无静默弱密钥） | A-3 |
| C-3 | 微信测试号沙箱：真实 SCAN 回调链路（ticket→绑定→登录）可走通 | 参考 AuthBindMockE2EIT 的 mock 方式；真实环境依赖外网 | 阶段 1 |
| C-4 | RocketMQ namesrv 在 docker 环境的延时消息（level 9）实际延迟精度 | TimeoutCloseOrderE2EIT 覆盖逻辑；物理延迟以 broker 为准 | Day 6 |
| C-5 | broker 重试间隔与 DLQ 转移时机（本地 docker 环境实测） | Day 5 动手任务产物：停依赖 → 投递测试消息 → 观察重试与 DLQ | §八场景 3/4 |
| C-6 | 支付宝异步通知重试参数官方文档核实 | 查阅支付宝官方文档；此前"24h/7 次"为 module-order-pay 转述，**未经本轮核实**；注意区分：项目中与重试相关的只是"回 success/false"的行为（代码事实），次数与间隔纯为支付宝平台协议 | §八场景 1、§九 |
| C-7 | 扫码登录安全验证（附录 D 两项）：a) 无签名 POST 伪造 SCAN 是否可签发 token；b) 重扫旧二维码的实际行为（是否重新签发 JWT） | **已于 2026-10-19 本地实测，两项均成立**（详见附录 D）：伪造 signature=timestamp=nonce=fake 的 POST 即完成"自动注册用户 → 签发 JWT → 公开轮询端点取走 token"全链路；同一 ticket 重扫会重新签发全新 JWT（iat 不同）。实验产物已清理。**后续：D-2 已登记 S-06 并修复（重放攻击实测被拒），D-1 重扫列为已知限制** | 附录 D |

---

## 附录 D：扫码登录安全设计风险（2026-10-19 实测确认；D-2 已登记 S-06 并修复）

> 状态：两项风险已由 C-7 本地实验**实测成立**（实验记录：伪造 POST 全链路攻击成功，测试用户/Redis key 已清理）。其中 **D-2 已登记 SECURITY_ISSUES S-06 并修复**：`post()` 入口补充 `SignatureUtil.check` 验签，修复后重放同一攻击实测被拒（验签失败 WARN、无 JWT、无用户创建、check_login 0003），E2E 17/17 回归通过。**D-1 重扫重签发未处理**，列为已知限制（QR 登录通病，缓解方向 ticket 缩短并绑定会话），答辩如实说明即可。

### D-1 ticket 生命周期与重扫能力

- **重复扫码**：ticket 有效期 30 天（A-2，代码 L56 请求值），且二维码图片本身只是 ticket 的编码。扫码登录后 Redis 中 ticket→JWT 被消费删除，但**同一二维码图片再次扫描会再次触发 SCAN 事件 → 重新签发 JWT 并重建 Redis key**（`WeixinPortalController` L110-127 → `WeixinLoginService.handleWechatScanLogin` L50-84，每次扫码独立签发）【**2026-10-19 实测确认 C-7b**：同一 ticket 第二次伪造 SCAN 后 Redis 重新出现全新 JWT，iat 与首次不同】。
- **凭证重新签发**：每次扫码 mint 一个全新 JWT，旧 JWT 不失效、无黑名单——同一账户可并存多个有效会话【代码推导】。
- **授权上下文**：本流程中"扫码"即视为授权，无显式"确认登录"步骤；且取 ticket 端点公开（`/pay-api/v1/login/**` permitAll，SecurityConfig L67）、ticket 不绑定发起方会话——**任何能诱导目标用户扫码的一方，都能拿到目标账户在本系统的登录态**（扫码诱导/QR 登录 CSRF 模式，属 QR 登录类系统的通病，需确认是否在预期威胁模型内）【代码推导】。
- 现有缓解：Redis token 5min TTL + 一次性消费（限制了每次扫码后的取用窗口，但不限制 30 天内的重扫）。

### D-2 候选更严重发现：portal 回调未验签

- `WeixinPortalController.post`（L93-135）接收 `signature/timestamp/nonce` 参数但**未调用 `SignatureUtil.check`**——GET `validate()`（L55-76）有验签，POST 消息接收没有。即：任何能访问该公开端点的人可**伪造 SCAN 事件 XML**，携带任意 `openid`（@RequestParam）→ `handleWechatScanLogin(ticket, openid)` 按该 openid 查/建用户并签发 JWT → token 存入攻击者自选 ticket key → 攻击者经公开轮询端点 `check_login` 取走 token【**2026-10-19 实测确认 C-7a**：以 `signature=fake` POST 构造 XML，系统为攻击者自选 openid 自动注册用户（userId=77）并签发 JWT，`check_login` 公开轮询返回 code 0000 + 完整 token，攻击链全链路走通；对照组 GET 验签正确拒绝】。
- openid 获取途径：服务日志**明文 info 级打印**（`WeixinLoginService` L51"处理微信扫码登录: ticket={}, openid={}"、`WeixinPortalController` L118 等）【代码事实】。
- 前置条件与严重级别：需网络可达 portal（openid 可由攻击者自选，无需事先获取）。C-7a 实验已成立，属**认证绕过级**（中~严重：可批量伪造注册、可对已知 openid 的账户直接签发登录态；若结合绑定票据可演变为账户接管）。**已登记 S-06 并修复（2026-10-19）**：post() 入口复用 `SignatureUtil.check` 验签（`SignatureUtil` 增补 `sign()` 供测试构造合法签名），修复后重放攻击实测被拒，E2E 17/17 回归通过。**残余加固项**：日志 openid 脱敏、ticket 缩短并绑定会话（同 D-1）。

---

> **下一阶段的交付约定**：教材按 LEARN_WECHAT_LOGIN.md → LEARN_ALIPAY_PAYMENT.md → LEARN_ROCKETMQ.md → LEARN_ORDER_STOCK_CONSISTENCY.md → INTERVIEW_AND_DEFENSE.md 顺序逐份生成，每份采用统一模板（问题背景/前置知识/完整流程/源码精读/异常边界/动手实验/设计取舍/面试答辩/闭卷验收），每份交付后等待检查再进入下一份。微信登录教材将从微信开放协议的外部原理讲起。
