# API 层命名与包结构决策

> **日期**: 2026-10-02 | **状态**: 定稿 | **范围**: `s-pay-mall-api` 模块全部出线契约类
>
> 本决策替代 DEVELOPMENT_GUIDE §1.1 中"请求 DTO `{Action}RequestDTO` / 响应 DTO `{Entity}RespDTO` / 视图对象 VO `{Entity}VO`"三行旧规，同步修订 REVIEW.md、API_CONTRACT.md（与代码同一提交）。

---

## 一、背景与动机

1. **DTO/VO 后缀无语义分工**。评审确认：api 层 DTO 与 VO 的实际区别不是"数据载体 vs 视图裁剪"，而是"服务哪一端"（用户端 vs 管理端），后缀不承载任何可判定的规则。
2. **domain 与 api 同名类冲突**。`cn.fcr.api.vo.OrderVO` 与 `cn.fcr.domain.mall.model.valobj.OrderVO` 等 4 对同名类，迫使多个 Controller 内联全限定名消歧，是明确的命名债。
3. **出线对象粒度混乱**。同一语义对象在用户端叫 `OrderListRespDTO`、在管理端叫 `OrderVO`，命名规范应用不一致。

## 二、决策规则（定稿）

### 2.1 包结构

```
s-pay-mall-api/src/main/java/cn/fcr/api/
├── dto/
│   ├── admin/
│   │   ├── req/        # 管理端请求体
│   │   └── res/        # 管理端响应体
│   ├── user/
│   │   ├── req/        # 用户端请求体
│   │   └── res/        # 用户端响应体
│   └── common/
│       ├── req/        # 共用请求体（登录、注册、支付入口等）
│       └── res/        # 共用响应体（登录结果、双端共用的商品/分类等）
└── response/
    └── Response.java   # 统一包装类，保留原名，禁止改名
```

- `vo/` 包取消，全部并入 `dto/{side}/{req,res}/`。
- `Response<T>` 是全局统一包装（`code/info/data`），与业务 `Res` 后缀不是一回事，**保留原名**。

### 2.2 命名

- 格式：**`{端前缀}{业务名}{Req|Res}`**。
- 端前缀：`Admin`（管理端）、`User`（用户端）、无前缀（`common`）。
- **共用类不带前缀**：登录、注册、双端共用的商品/分类出线对象。
- 示例：`AdminOrderRes`、`UserOrderRes`、`UserCartAddReq`、`LoginReq`、`LoginRes`。

### 2.3 造类纪律

- **只为真实存在的接口建类，不为对称造类**。例：管理端没有"创建订单"接口，因此 `AdminOrderCreateReq` **不存在，禁止造**。
- 旧的示例/结构图中的 `AdminOrderVO` 一律按新规则写作 `AdminOrderRes`。

### 2.4 不变项（边界）

- **JSON 字段名一个都不变**：含历史遗留 `@JsonProperty` snake_case 的类（迁移后落在 admin/res 下），其 JSON 输出保持原样，前端无感。
- 校验注解（`@NotBlank`/`@NotNull` 等）与字段语义随类迁移，不做任何契约语义变化。
- domain 层读模型（`domain/mall/model/valobj/OrderVO` 等）本次**不改名**，与 api 新类名不再冲突的问题随本决策自然消解一半，剩余归"暂缓项"。

## 三、旧名 → 新名映射表（21 个类，只读盘点结果）

| # | 旧类 | 新类 | 服务端点 | 备注 |
|---|------|------|---------|------|
| 1 | `api.dto.UserLoginRequestDTO` | `api.dto.common.req.LoginReq` | POST /auth/login | 共用：登录 |
| 2 | `api.dto.UserRegisterRequestDTO` | `api.dto.common.req.RegisterReq` | POST /auth/register | 共用：注册 |
| 3 | `api.vo.UserLoginVO` | `api.dto.common.res.LoginRes` | login/register 响应 | 共用 |
| 4 | `api.dto.CreatePayRequestDTO` | `api.dto.common.req.CreatePayReq` | POST /pay-api/v1/alipay/create_pay_order | 无 admin/user 归属，归 common |
| 5 | `api.vo.ProductVO` | `api.dto.common.res.ProductRes` | /products、/admin/products | 双端共用 |
| 6 | `api.vo.CategoryVO` | `api.dto.common.res.CategoryRes` | /categories、/admin/categories | 双端共用 |
| 7 | `api.dto.CartAddRequestDTO` | `api.dto.user.req.UserCartAddReq` | POST/PUT /cart* | 用户端 |
| 8 | `api.dto.OrderCreateRequestDTO` | `api.dto.user.req.UserOrderCreateReq` | POST /orders | 用户端 |
| 9 | `api.dto.CartItemRespDTO` | `api.dto.user.res.UserCartItemRes` | GET /cart | 用户端 |
| 10 | `api.dto.OrderCreateRespDTO` | `api.dto.user.res.UserOrderCreateRes` | POST /orders、continue-pay | 用户端 |
| 11 | `api.dto.OrderListRespDTO` | `api.dto.user.res.UserOrderRes` | GET /orders | 用户端（定稿示例名） |
| 12 | `api.dto.StockCheckRespDTO` | `api.dto.user.res.UserOrderStockCheckRes` | GET /orders/{orderNo}/check-stock | 用户端 |
| 13 | `api.vo.UserProfileVO` | `api.dto.user.res.UserProfileRes` | /auth/profile、/profile | 用户端 |
| 14 | `api.vo.BindStatusVO` | `api.dto.user.res.UserBindStatusRes` | /auth/bind/status | 用户端 |
| 15 | `api.dto.UserSaveRequestDTO` | `api.dto.admin.req.AdminUserSaveReq` | POST /admin/users | 管理端 |
| 16 | `api.dto.CategorySaveRequestDTO` | `api.dto.admin.req.AdminCategorySaveReq` | POST /admin/categories | 管理端 |
| 17 | `api.dto.ProductSaveRequestDTO` | `api.dto.admin.req.AdminProductSaveReq` | POST /admin/products | 管理端 |
| 18 | `api.vo.UserVO` | `api.dto.admin.res.AdminUserRes` | GET /admin/users | 管理端 |
| 19 | `api.vo.OrderVO` | `api.dto.admin.res.AdminOrderRes` | GET /admin/orders | 管理端（定稿示例名） |
| 20 | `api.vo.SalesTrendVO` | `api.dto.admin.res.AdminSalesTrendRes` | /admin/statistics/sales-trend | 管理端 |
| 21 | `api.vo.CategoryRatioVO` | `api.dto.admin.res.AdminCategoryRatioRes` | /admin/statistics/category-ratio | 管理端 |

`cn.fcr.api.response.Response<T>` 不在映射表内：**保留原名**。

## 四、暂缓项（明确不在本次范围）

| 暂缓项 | 说明 |
|--------|------|
| domain 读模型改名 | `domain/mall/model/valobj/` 的 OrderVO 等读模型归位（→ `OrderView` 等）与 valobj 堆放场整治，单独任务 |
| command 孤儿包 | `ProductSaveCommand` 零引用，补完或删除，单独决策 |
| gateway/adapter 双端口包 | domain 内两个端口包的分界标准，需架构讨论 |
| 前端 TS 类型政策 | TS 语义名 vs 后端镜像名，待政策 B 定稿后另行落地 |
| 手写拷贝统一 MapStruct | product/user/category 的手写映射收敛到 converter |
| 前端重复类型清理 | `api/admin.ts`、`AdminUsersPage.vue` 本地重复类型定义 |

## 五、同步清单（必须同一提交完成）

- [ ] 代码：21 个类按映射表迁移重命名，trigger 层引用全部更新
- [ ] DEVELOPMENT_GUIDE §1.1（命名表三行旧规替换）与 §1.3（dto/vo 包描述更新）
- [ ] REVIEW.md 命名检查条目（如"禁止 UserLoginRequest（应为 UserLoginRequestDTO）"）
- [ ] API_CONTRACT §二/§三/§四端点表与 §五映射表中的全部类名引用
