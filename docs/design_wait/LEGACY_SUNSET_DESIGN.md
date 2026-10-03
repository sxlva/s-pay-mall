# 旧链（legacy）下线设计（Legacy Sunset）

> 状态：**已完工（2026-10-03）**——步骤 A/B/C 全部执行完毕，3 个提交（ece2a9c / 3b59b38 / 收尾提交），P0-1 已关闭，legacy 数据清零，守卫规则 5 上线
> 执行偏差记录：① `OrderServiceTest` 原计划步骤 C 删除，因步骤 B 摘 Bean 后报 NoSuchBeanDefinitionException，提前至 B 删除；② 设计文档中 `queryNoPayNotifyOrder` 原拟放 `IOrderQueryGateway`，实施时改放 `IPayOrderGateway`（该接口契约即 pay_order 读写，内聚更高），`IOrderQueryGateway` 保持"mall 跨域读模型"单一职责
> 关联：TECH_DEBT_ROADMAP P0-1（已关闭）、M2_REORG_DECISIONS 决策点 4/5
> 核心原则：**先补新链能力、再断旧链调用点、最后删包**；每步独立编译 + 全量测试 + 提交

---

## 一、背景与现状

M2 完成后旧链代码已归位 `domain/order/legacy` + `infrastructure/order/legacy`，但仍被 4 条生产路径引用，属于"过渡态"。legacy 的数据形态 = `pay_order` 表中 `order_main` 不存在的行。

**数据现状（2026-10-03 实查 MySQL 127.0.0.1:23306）**：

| 检查项 | 结果 |
|--------|------|
| `pay_order` 总行数 | 3 |
| legacy 行数（order_main 不存在） | **3（全部为 legacy）** |
| 3 行状态 | 全部 CREATE（未支付），2026-10-02~03 的测试单 |
| `order_main` 行数 | 0 |
| `pay_order` 非 CREATE 行 | 0 |

结论：**无在途业务、无已支付旧单**，3 行未支付测试单可直接清除。数据清零是删包的前置条件。

**调用点实查清单**（本次逐处核实，含行号）：

| # | 位置 | 说明 |
|---|------|------|
| 1 | `OrderApplicationService.handleTimeoutCloseOrder` | 超时关单分流：order_main 不存在 → `orderService.handleTimeoutCloseOrder`（旧域） |
| 2 | `OrderTransactionService.changeOrderPaySuccessInTransaction:106-109` | 支付回调分流：order_main 不存在 → `orderService.changeOrderPaySuccess`（仅改 pay_order 状态） |
| 3 | `AliPayController.createPayOrder`（`/alipay/create_pay_order`） | 死端点：唯一造旧链订单的入口；**前端已确认无调用**，可直接删 |
| 4 | `NoPayNotifyOrderJob.exec` → `queryNoPayNotifyOrder` | 回调补偿 Job：当前查的是 `pay_order.status='WAIT_PAY' and create_time < NOW()-5min`，新链同样写 pay_order，**Job 本身必须保留**，只需把查询挪到新链 |
| 5 | `DomainServiceConfig.orderService` Bean | 手动装配 `OrderService(IOrderRepository, IProductGateway, IPaymentGateway)` |
| 6 | `OrderQueryGatewayImpl.countOrdersByUserId` → legacy `IOrderRepository.countByUserId` | **注意：该方法实际查的是 order_main 表**（infra OrderRepository:166-170），语义已是新链口径，只是实现挂在 legacy 仓储上 |

**依赖 legacy 的测试**：`OrderServiceTest`（app，直接 import legacy）、`CreateOrderAggregateTest`（domain）、`AlipayNotifyE2ETest` 场景 3、`TimeoutCloseOrderE2ETest` 场景 3。

---

## 二、目标结构（完成后）

```
domain/order
├── model       Order/OrderItem/OrderState + PayOrderEntity/PayStatus（不变）
├── service     MallOrderService、OrderStateMachineService、PayOrderService（不变）
├── gateway     现有网关口 + queryNoPayNotifyOrder 归入此处（新增）
├── adapter     event / repository（不变）
└── legacy      ❌ 整包删除
```

`infrastructure/order/legacy/` 整包删除；`infrastructure/order/repository/OrderRepository.java`（legacy 仓储实现）删除。

## 三、阶段划分（3 步，每步可编译、可测试、单独提交）

### 步骤 A：新链能力补齐（行为不变，半天）

**A1. 补偿查询挪入新链网关**

- `IOrderQueryGateway`（domain/order/gateway）新增：
  ```java
  /** 查询等待支付超过5分钟但未收到回调的订单号，用于 NoPayNotifyOrderJob 主动补单 */
  List<String> queryNoPayNotifyOrder();
  ```
- `OrderQueryGatewayImpl` 实现：直接注入 `IOrderDao` 调现有 `queryNoPayNotifyOrder()` SQL（SQL 不动，查的 pay_order 表新链也在用）。
- `OrderQueryGatewayImpl.countOrdersByUserId` 改为注入 `IOrderMainDao` 查 order_main（把现在挂在 legacy OrderRepository:166-170 的实现原样搬过来，**语义不变**）。
- `OrderApplicationService.queryNoPayNotifyOrder` 改为委托 `IOrderQueryGateway`（删掉对 `IOrderService` 的调用，但此步骤**保留** legacy 注入，步骤 B 再摘）。

**A2. 验证**：`mvn clean install` + app 全量测试。Job 行为不变（同一 SQL），无需专门 E2E。

**风险**：`MallUserServiceImpl` 删除用户前的"存在订单"校验走 countOrdersByUserId——搬实现后口径仍是 order_main，**无行为变化**。

### 步骤 B：断旧链调用点（行为收敛，半天）

| 改动 | 位置 | 收敛后行为 |
|------|------|-----------|
| B1 删超时关单旧分支 | `OrderApplicationService.handleTimeoutCloseOrder` | order_main 不存在 → 记 warn 日志并返回 false（不抛异常，不影响 MQ 消费重试语义） |
| B2 删回调旧分支 | `OrderTransactionService.changeOrderPaySuccessInTransaction` | order_main 不存在 → 记 warn 日志、直接返回（支付宝回调仍会回 success，因为验签已通过；异常订单靠日志告警发现） |
| B3 删死端点 | `AliPayController.createPayOrder` + `OrderApplicationService.createPayOrder` + api 模块 `CreatePayReq` | 前端无调用（已核实），404 即预期；`docs/dev-ops` 与 API_CONTRACT 同步 |
| B4 摘 legacy 注入 | `OrderApplicationService`、`OrderTransactionService` 构造函数、`DomainServiceConfig`（删 orderService Bean 及 4 个 legacy import） | Spring 上下文不再装配旧链 |
| B5 测试同步 | 删 `TimeoutCloseOrderE2ETest` 场景 3、`AlipayNotifyE2ETest` 场景 3 | 旧链行为锁定测试随旧链一起退役；如答辩需要"分流曾存在"的证据，留在 git 历史即可 |

**B2 行为说明（需要用户知晓）**：旧链下线后，若出现异常"pay_order 有行但 order_main 无行"的数据（不应再产生），回调将只记日志不改状态。这是**有意收敛**——旧链状态语义（只改 pay_order）本就与新链不一致，静默兜底反而掩盖数据问题。

**验证**：编译 + 全量测试 + 手动 smoke：正常下单→回调→超时关单各跑一笔（新链场景 1/2 已覆盖）。

### 步骤 C：删包 + 文档关闭（半天）

1. `git rm -r`：
   - `s-pay-mall-domain/src/main/java/cn/fcr/domain/order/legacy/`
   - `s-pay-mall-infrastructure/src/main/java/cn/fcr/infrastructure/order/legacy/`
   - `s-pay-mall-infrastructure/src/main/java/cn/fcr/infrastructure/order/repository/OrderRepository.java`
   - `s-pay-mall-domain/src/test/java/cn/fcr/domain/order/legacy/`（CreateOrderAggregateTest）
   - `s-pay-mall-app/src/test/java/cn/fcr/test/OrderServiceTest.java`
2. **守卫测试（可选展示点）**：`DomainArchitectureGuardTest` 可加规则 5——`..domain.order..` 不得出现 `legacy` 子包，防止旧链借尸还魂。
3. 文档同步：
   - `TECH_DEBT_ROADMAP.md`：P0-1 标记**已关闭**（附关闭日期与数据清零记录）
   - `DDD_ARCHITECTURE_SPEC.md`：order 包结构删除 legacy 行
   - `docs/design/README.md` 架构图同步
   - `API_CONTRACT.md`：删除 create_pay_order 条目与 CreatePayReq 定义
   - `M2_REORG_DECISIONS.md`：legacy"过渡态"备注更新为"已下线"
4. 全量验证：`mvn clean install` + app 全量测试，domain 守卫测试随构建运行。

## 四、数据清零 SQL（执行前需 dawnFu 确认）

```sql
-- 先核对（应返回 3）
SELECT COUNT(*) FROM pay_order p
LEFT JOIN order_main o ON p.order_id = o.order_no
WHERE o.order_no IS NULL;

-- 确认后删除（3 行均为 CREATE 未支付测试单，无资损风险）
DELETE p FROM pay_order p
LEFT JOIN order_main o ON p.order_id = o.order_no
WHERE o.order_no IS NULL;
```

注意：`pay_order` 表本身**保留**（新链支付单仍在使用该表），只清无主行。

## 五、风险与缓解

| 风险 | 缓解 |
|------|------|
| OrderQueryGatewayImpl 搬迁实现时口径变化 | countByUserId 搬的是"order_main 计数"现成实现，语义不变；A 步单独提交，若 MallUserService 删除用户校验异常可快速定位 |
| 删端点后文档/前端不一致 | 前端已核实无调用；API_CONTRACT、时序图（docs/design/module-order-pay.md）、JV-003 验收清单一并更新 |
| 回调对无 order_main 订单只记日志，用户钱付了单没更新 | 数据清零后该形态不再产生；且 NoPayNotifyOrderJob 仍在跑（查 pay_order WAIT_PAY 单），一旦支付宝查到交易成功会走新链 paySuccess——但 paySuccess 前提也是 order_main 存在。剩余敞口 = 理论上不应出现的数据异常，用 warn 日志 + 人工巡检兜底 |
| 答辩前时间盒 | 步骤 A/B 可合为一天内完成；C 是纯删除。若时间不足，只做 A+B（旧链已无任何路径可达，包留着不影响正确性） |

## 六、明确不做

- `bind/*` 死端点清理：与本次无关，仍按原计划与前端 TS 清理合并一批
- `pay_order` 表结构变更：新链仍在用，不动
- P0-9 并发幂等修复：独立排期
