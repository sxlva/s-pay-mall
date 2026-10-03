# API 接口契约

> **版本**: v1.0 | **生效日期**: 2026-07-04
>
> 本文档是 s-pay-mall 前后端接口契约的唯一权威来源。新增或修改接口前必须核对本文件。

---

## 一、通用规范

### 1.1 响应包装

所有 API 返回统一格式：

```json
{
  "code": "0000",
  "info": "调用成功",
  "data": { ... }
}
```

| 字段 | 类型 | 说明 |
|------|------|------|
| code | String | 错误码，详见 §五 |
| info | String | 描述信息 |
| data | T | 业务数据，可能是对象/列表/字符串 |

### 1.2 认证方式

| 场景 | 方式 | Header |
|------|------|--------|
| 微信扫码/微信网关/支付回调（pay-api） | 无认证/公开 | — |
| 商城用户端（mall-api） | JWT Bearer Token | `Authorization: Bearer {token}` |
| 管理后台（admin） | JWT Bearer Token | `Authorization: Bearer {token}` |

### 1.3 基础URL

| 前缀 | 用途 | 版本 |
|------|------|------|
| `/pay-api/v1` | 支付、登录、微信网关 | v1 |
| `/mall-api/v1` | 商城用户端 + 管理后台 | v1 |
| `/orders` | 支付同步跳转（MVC 页面） | — |

---

## 二、支付与认证 API（/pay-api/v1/）

| # | 方法 | 端点 | 请求类 | 响应类 | 说明 |
|---|------|------|-----------|------------|------|
| 1 | POST | `/alipay/alipay_notify_url` | HttpServletRequest (params) | `String` | 支付宝异步回调验签+更新订单状态 |
| 2 | GET | `/login/weixin_qrcode_ticket` | — | `Response<String>` | 获取微信扫码登录二维码ticket |
| 3 | GET | `/login/check_login` | `ticket` (query) | `Response<String>` | 轮询检查扫码登录状态，返回JWT token |
| 4 | GET | `/weixin/portal/receive` | signature/timestamp/nonce/echostr (query) | `String` | 微信URL验证（服务器配置校验） |
| 5 | POST | `/weixin/portal/receive` | XML body + query params | `String` (XML) | 接收微信事件（关注/扫码/消息） |
| 6 | GET | `/orders` | out_trade_no/trade_no/total_amount (query) | ModelAndView (302) | 支付宝同步回调跳转 |

> **2026-10-03 变更**：`POST /alipay/create_pay_order` 端点及 `CreatePayReq` DTO 已随旧链（legacy）下线删除。支付单由商城下单接口（`POST /orders`）统一创建。

---

## 三、商城 API（/mall-api/v1）

### 3.1 用户认证（/auth）

| # | 方法 | 端点 | 请求类 | 响应类 | 说明 |
|---|------|------|-----------|------------|------|
| 7 | POST | `/auth/register` | `RegisterReq` | `Response<LoginRes>` | 用户注册（普通或微信） |
| 8 | POST | `/auth/login` | `LoginReq` | `Response<LoginRes>` | 用户登录 |
| 9 | GET | `/auth/profile` | JWT Header | `Response<UserProfileRes>` | 获取用户资料（2026-10-02 起 userId 取自 JWT，不再接受 query 参数，修复 IDOR 越权） |
| 10 | GET | `/auth/bind/qrcode` | — | `Response<String>` | 获取微信绑定二维码ticket |
| 11 | GET | `/auth/bind/status` | `ticket` (query) | `Response<UserBindStatusRes>` | 轮询微信绑定状态 |

**RegisterReq**:
| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| username | String | ✅ | 用户名 |
| password | String | ✅ | 密码 |
| openId | String | ❌ | 微信openId（微信注册时传入） |

**LoginReq**:
| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| username | String | ✅ | 用户名 |
| password | String | ✅ | 密码 |

**ResponseDTO 说明**:

| DTO | 字段 |
|-----|------|
| LoginRes | `token: String, userId: Long, username: String, role: String` |
| UserProfileRes | `id: Long, username: String, status: Integer, roleCode: String, createTime: LocalDateTime, updateTime: LocalDateTime` |
| UserBindStatusRes | `status: String (BIND_SUCCESS/BINDING_PENDING/INVALID_CODE), openId: String` |

### 3.2 个人信息

| # | 方法 | 端点 | Request | ResponseDTO | 说明 |
|---|------|------|---------|------------|------|
| 12 | GET | `/profile` | JWT Header | `Response<UserProfileRes>` | 获取当前用户信息（JWT识别） |

### 3.3 商品

| # | 方法 | 端点 | Request | ResponseDTO | 说明 |
|---|------|------|---------|------------|------|
| 13 | GET | `/products` | `categoryId?, keyword?, minPrice?, maxPrice?, status?` (query) | `Response<List<ProductRes>>` | 查询商品列表（多条件筛选） |
| 14 | GET | `/categories` | — | `Response<List<CategoryRes>>` | 查询商品分类列表 |

**ProductRes 字段**:
| 字段 | 类型 | JSON输出 | 说明 |
|------|------|---------|------|
| id | Long | `id` | 商品ID |
| categoryId | Long | `category_id` | 分类ID（snake_case输出） |
| name | String | `name` | 商品名称 |
| description | String | `description` | 商品描述 |
| price | BigDecimal | `price` | 商品价格 |
| stock | Integer | `stock` | 库存量 |
| category | String | `category` | 分类对象 |
| categoryName | String | `category_name` | 分类名称 |
| status | Integer | `status` | 1-上架 0-下架 |
| createTime | LocalDateTime | `create_time` | 创建时间 |

**CategoryRes 字段**: `id: Long, name: String, status: Integer, createTime: LocalDateTime`

### 3.4 购物车

| # | 方法 | 端点 | 请求类 | 响应类 | 说明 |
|---|------|------|-----------|------------|------|
| 15 | POST | `/cart` | `UserCartAddReq` | `Response<Integer>` | 添加商品到购物车 |
| 16 | GET | `/cart` | JWT Header | `Response<List<UserCartItemRes>>` | 查询购物车列表 |
| 17 | PUT | `/cart/quantity` | `UserCartAddReq` | `Response<Integer>` | 更新购物车商品数量 |
| 18 | DELETE | `/cart/delete` | `itemId` (query) | `Response<Integer>` | 删除购物车条目 |

**UserCartAddReq**:
| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| productId | Long | ✅ | 商品ID |
| quantity | Integer | ❌ | 数量（默认1） |

**UserCartItemRes 字段**:
| 字段 | 类型 | 说明 |
|------|------|------|
| id | Long | 购物车条目ID |
| productId | Long | 商品ID |
| productName | String | 商品名称 |
| price | BigDecimal | 商品单价 |
| quantity | Integer | 数量 |
| selected | Boolean | 是否选中 |
| itemAmount | BigDecimal | 小计 |
| stock | Integer | 库存 |

### 3.5 订单

| # | 方法 | 端点 | 请求类 | 响应类 | 说明 |
|---|------|------|-----------|------------|------|
| 19 | POST | `/orders` | `UserOrderCreateReq` | `Response<UserOrderCreateRes>` | 从购物车创建订单 |
| 20 | GET | `/orders` | `status?, startTime?, endTime?` (query) | `Response<List<UserOrderRes>>` | 查询用户订单列表 |
| 21 | GET | `/orders/{orderNo}/continue-pay` | `orderNo` (path) | `Response<UserOrderCreateRes>` | 继续支付未完成订单 |
| 22 | GET | `/orders/{orderNo}/check-stock` | `orderNo` (path) | `Response<UserOrderStockCheckRes>` | 检查订单库存 |

**UserOrderCreateReq**:
| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| address | String | ✅ | 收货地址 |
| requestId | String | ⬜ | 幂等键（客户端生成的 UUID，P0-4）：非空时同键 24h 内重发返回首次创建的订单；为空降级为用户级 10s 短锁仅防双击 |

**UserOrderCreateRes**:
| 字段 | 类型 | 说明 |
|------|------|------|
| orderId | String | 订单编号（创建后返回） |
| payUrl | String | 支付URL |
| totalAmount | BigDecimal | 订单总额 |
| html | String | 支付表单HTML |

**UserOrderRes**:
| 字段 | 类型 | 说明 |
|------|------|------|
| id | Long | 订单ID |
| orderNo | String | 订单号 |
| status | String | 订单状态 |
| statusDesc | String | 状态描述 |
| totalAmount | BigDecimal | 订单总额 |
| createTime | LocalDateTime | 创建时间 |
| address | String | 收货地址 |

**UserOrderStockCheckRes**:
| 字段 | 类型 | 说明 |
|------|------|------|
| success | Boolean | 库存是否充足 |
| message | String | 提示信息（不足时说明） |

---

## 四、管理后台 API

### 4.1 MallAdminController（/mall-api/v1/admin）

| # | 方法 | 端点 | 请求类 | 响应类 | 说明 |
|---|------|------|-----------|------------|------|
| 23 | GET | `/admin/users` | `username?, status?, roleCode?` (query) | `Response<List<AdminUserRes>>` | 查询用户列表 |
| 24 | POST | `/admin/users` | `AdminUserSaveReq` | `Response<Integer>` | 新增/更新用户 |
| 25 | PUT | `/admin/users/{userId}/status` | `userId`(path),`status`(query) | `Response<Integer>` | 更新用户状态 |
| 26 | DELETE | `/admin/users/{userId}` | `userId` (path) | `Response<Integer>` | 删除用户 |
| 27 | GET | `/admin/categories` | — | `Response<List<CategoryRes>>` | 查询分类列表 |
| 28 | POST | `/admin/categories` | `AdminCategorySaveReq` | `Response<Integer>` | 新增/更新分类 |
| 29 | DELETE | `/admin/categories/{categoryId}` | `categoryId` (path) | `Response<Integer>` | 删除分类 |
| 30 | GET | `/admin/products` | `categoryId?, keyword?, minPrice?, maxPrice?, status?` (query) | `Response<List<ProductRes>>` | 查询商品列表 |
| 31 | POST | `/admin/products` | `AdminProductSaveReq` | `Response<Integer>` | 新增/更新商品 |
| 32 | DELETE | `/admin/products/{productId}` | `productId` (path) | `Response<Integer>` | 删除商品 |
| 33 | GET | `/admin/orders` | `userId?, status?, startTime?, endTime?` (query) | `Response<List<AdminOrderRes>>` | 查询订单列表 |
| 34 | PUT | `/admin/orders/{orderId}/deliver` | `orderId` (path) | `Response<Integer>` | 一键发货 |
| 35 | PUT | `/admin/orders/{orderId}/cancel` | `orderId` (path) | `Response<Integer>` | 取消订单 |
| 36 | DELETE | `/admin/orders/{orderId}` | `orderId` (path) | `Response<Integer>` | 删除订单 |
| 37 | GET | `/admin/statistics/sales-trend` | — | `Response<List<AdminSalesTrendRes>>` | 销售趋势 |
| 38 | GET | `/admin/statistics/category-ratio` | — | `Response<List<AdminCategoryRatioRes>>` | 分类销售占比 |

### 4.2 ~~AdminApiController（/pay-api/v1/admin）~~（已删除）

~~功能与 MallAdminController **重复**，加 `@PreAuthorize("hasRole('ADMIN')")` 权限注解。详见 [TECH_DEBT_ROADMAP.md](docs/design_wait/TECH_DEBT_ROADMAP.md) §一 P0-1。~~

> **2026-10-01 变更**：`AdminApiController` 已删除（JV-003 第二批）。该前缀下 16 个管理端端点原本就是 `MallAdminController`（`/mall-api/v1/admin`）的镜像，前端与历史分支均无调用方；SecurityConfig 中 `/pay-api/v1/admin/**` 规则同步移除。管理后台唯一入口为 §4.1。

### 4.3 Admin DTO 字段

**AdminUserSaveReq**: `id: Long, username: String, password: String, status: Integer`
**AdminCategorySaveReq**: `id: Long, name: String, status: Integer`
**AdminProductSaveReq**: `id: Long, categoryId: Long, name: String, description: String, price: BigDecimal, stock: Integer, status: Integer`

**Admin VO 字段**:

| VO | 字段 |
|----|------|
| AdminUserRes | `id: Long, username: String, status: Integer, roleCode: String, roleName: String, createTime: LocalDateTime, updateTime: LocalDateTime` |
| AdminOrderRes | `id: Long, orderNo: String, userId: Long, status: String, statusDesc: String, totalAmount: BigDecimal, address: String, createTime: LocalDateTime` |
| AdminSalesTrendRes | `date: String, salesAmount: BigDecimal, orderCount: Integer` |
| AdminCategoryRatioRes | `categoryName: String, productCount: Integer, salesAmount: BigDecimal` |

---

## 五、前后端类型映射表

> 后端 DTO/VO 是 Single Source of Truth，前端类型字段不得超出后端定义。

| 后端类（Java） | 前端类型（TypeScript） | 字段一致性 | 备注 |
|---------------|----------------------|-----------|------|
| `ProductRes` (common/res) | `types/domain/product.ts ProductRes` | **`category_id` → `categoryId` 命名不一致** | 后端 `@JsonProperty` 输出snake_case，前端字段名需对齐 |
| `CategoryRes` (common/res) | `types/domain/product.ts CategoryRes` | ✅ 一致 | — |
| `AdminUserRes` (admin/res) | `types/domain/user.ts AdminUserRes` | **前端多了 `email`, `role` 字段** | 后端无对应字段，风险 |
| `UserCartItemRes` (user/res) | `types/domain/cart.ts CartItem` | **`price` vs `productPrice` 命名不一致** | 运行时常一致（映射层中转） |
| `UserOrderCreateRes` (user/res) | `types/domain/order.ts OrderCreateResult` | **`orderId` vs `orderNo` 命名不一致** | `CheckoutPage.vue` 硬编码空串 |
| `UserOrderRes` (user/res) | `types/domain/order.ts Order` | ✅ 基本一致 | — |
| `UserOrderStockCheckRes` (user/res) | `api/order.ts StockCheckResult` | **前端多余 `stockStatus` 字段** | 后端无对应，需删除 |
| `LoginRes` (common/res) | （前端 `useUserStore` 消费） | ✅ 基本一致 | — |
| `RegisterReq` (common/req) | 前端无独立类型 | — | 直接使用 Form 数据 |
| `UserOrderCreateReq` (user/req) | 前端无独立类型 | — | 仅传 `address` |
| `UserCartAddReq` (user/req) | `types/domain/cart.ts CartAddParams` | ✅ 一致 | — |

---

## 六、错误码定义

### 6.1 通用错误码（s-pay-mall-types Constants.ResponseCode）

| code | info | 说明 |
|------|------|------|
| `0000` | 调用成功 | 请求正常处理 |
| `0001` | 调用失败 | 通用业务错误 |
| `0002` | 非法参数 | 参数校验不通过 |
| `0003` | 未登录 | 未提供有效token或token过期 |
| `0403` | 账号已被封禁 | 用户被禁用 |

### 6.2 业务错误码（预期扩展）

| 前缀 | 含义 | 当前状态 |
|------|------|---------|
| `A0xxx` | 认证/授权 | 未实现 |
| `B0xxx` | 业务错误 | 未实现 |
| `C0xxx` | 参数校验 | 未实现 |
| `S0xxx` | 系统错误 | 未实现 |
| `T0xxx` | 第三方错误 | 未实现 |

> **注意**: 项目计划按 6 位扩展错误码重构（参见 FUTURE_FEATURES.md §2.1），目前仅使用上述 5 个基础码。

---

## 七、约束规则

1. **Single Source of Truth**: 后端 DTO/VO 是接口定义的唯一来源，前端类型字段不得超出后端定义。
2. **字段命名一致**: 后端 `@JsonProperty` 输出 snake_case 时，前端类型字段名必须一致使用该 snake_case 名。
3. **禁止 `any` 类型**: 所有前端接口调用必须有明确的 TypeScript 类型定义。
4. **`api/` 层解构**: 组件层不得消费 `Response<T>` 包装，必须在 `api/` 层解构 `res.data`。
5. **修改同步**: 修改任何端点、DTO、VO 后，必须同步更新本文档和对应的前端类型定义。
