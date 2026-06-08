# s-pay-mall 作业验收说明

## 1) 页面数量（>=10）

前台页面：
- `/`
- `/login`
- `/register`
- `/products`
- `/products/:id`
- `/cart`
- `/checkout`
- `/orders`
- `/profile`

后台页面：
- `/admin`
- `/admin/users`
- `/admin/categories`
- `/admin/products`
- `/admin/orders`
- `/admin/notices`

合计 15 页。

## 2) 数据库表（>=6）与主从关系（>=2组）

DDL 文件：`docs/dev-ops/mysql/sql/s-pay-mall.sql`

表清单：
- `mall_user`
- `role`
- `permission`
- `user_role`
- `role_permission`
- `category`
- `product`
- `notice`
- `cart_item`
- `order_main`
- `order_item`
- `pay_order`

主从关系：
- `category(1) -> product(N)`
- `order_main(1) -> order_item(N)`
- `mall_user(1) -> cart_item(N)`

## 3) CRUD 与多条件查询

管理员 CRUD：
- 用户：`/pay-api/v1/admin/users`
- 分类：`/pay-api/v1/admin/categories`
- 商品：`/pay-api/v1/admin/products`
- 订单：`/pay-api/v1/admin/orders`
- 公告：`/pay-api/v1/admin/notices`

前台 CRUD：
- 购物车：`/mall-api/v1/cart`
- 订单：`/mall-api/v1/orders`

多条件查询：
- 商品：`categoryId/keyword/minPrice/maxPrice/status`
- 订单：`userId/status/startTime/endTime`
- 用户：`username/status/roleCode`

## 4) 权限控制

实现文件：
- `s-pay-mall-app/src/main/java/cn/fcr/config/security/SecurityConfig.java`
- `s-pay-mall-app/src/main/java/cn/fcr/config/security/JwtAuthenticationFilter.java`
- `s-pay-mall-app/src/main/java/cn/fcr/config/security/JwtTokenProvider.java`

验证点：
- 未登录访问需要登录的页面时，前端路由守卫跳转 `/login`
- 未登录访问受保护后端接口返回鉴权失败
- 非管理员角色无法访问 `/pay-api/v1/admin/**`

## 5) MQ 接入

代码改造：
- `s-pay-mall-infrastructure/.../OrderRepository.java` 新增 `RocketMQTemplate` 发送 `order.paid`
- `s-pay-mall-trigger/.../OrderPaidRocketListener.java` 监听 `order.paid`

环境改造：
- `docs/dev-ops/docker-compose-environment.yml` 新增 `rocketmq-namesrv`、`rocketmq-broker`

## 6) 构建验证

后端：
- `mvn -q -DskipTests compile` 通过

前端：
- `npm install && npm run build` 通过
