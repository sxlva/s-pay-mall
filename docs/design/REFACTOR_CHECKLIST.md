# DDD 规范性重构验收清单

## 项目信息
- **项目名称**: s-pay-mall
- **重构阶段**: 第一阶段（契约命名对齐 + 防腐层加固 + 实体行为化）
- **完成日期**: 2026-06-06

---

## 一、契约命名对齐 ✅

### 1.1 Port → Gateway 迁移
| 序号 | 旧文件 | 新文件 | 状态 |
|:---:|---|---|:---:|
| 1 | `IAuthTokenPort.java` | `IAuthTokenGateway.java` | ✅ 已删除/已创建 |
| 2 | `AuthTokenPortImpl.java` | `AuthTokenGatewayImpl.java` | ✅ 已删除/已创建 |

### 1.2 包结构调整
- ✅ 外部系统交互契约统一放置到 `domain.mall.gateway` 包
- ✅ 数据库交互契约放置到 `domain.mall.adapter.repository` 包

---

## 二、防腐层加固 ✅

### 2.1 消除 Map 泄漏
| 序号 | 修改文件 | 修改内容 | 状态 |
|:---:|---|---|:---:|
| 1 | `IUserRepository.java` | 返回类型从 `Map<String, Object>` 改为 `UserEntity` | ✅ |
| 2 | `UserRepository.java` | 新增 `toUserEntity()` / `toMallUser()` 双向转换方法 | ✅ |
| 3 | `MallUserServiceImpl.java` | 适配新接口签名 | ✅ |
| 4 | `MallAdminController.java` | 适配新返回类型 | ✅ |
| 5 | `AdminApiController.java` | 适配新返回类型 | ✅ |

### 2.2 验证标准
- ✅ Domain 层无 PO 类导入
- ✅ Repository 实现包含双向转换逻辑
- ✅ 所有 Repository 返回值均为强类型实体

---

## 三、实体行为化（充血模型）✅

### 3.1 UserEntity 充血化
| 新增方法 | 功能说明 | 状态 |
|---|---|:---:|
| `validateLoginStatus()` | 验证账户是否可登录（禁用检查） | ✅ |
| `validatePassword(raw, matcher)` | 验证密码是否正确 | ✅ |
| `getRoleOrDefault()` | 获取角色编码，为空返回默认值 | ✅ |
| `PasswordMatcher` | 函数式接口（依赖倒置） | ✅ |

### 3.2 OrderEntity 静态工厂方法
| 新增方法 | 功能说明 | 状态 |
|---|---|:---:|
| `createFromCart(userId, address, cartItems)` | 从购物车创建订单 | ✅ |
| `toPayOrder()` | 转换为支付订单实体 | ✅ |
| `calculateTotalAmount()` | 计算订单总金额 | ✅ |
| `addItem(item)` | 添加订单项 | ✅ |

### 3.3 Service 层职责调整
- ✅ `MallUserServiceImpl`：仅负责编排，业务逻辑已下沉
- ✅ `MallOrderServiceImpl`：移除面条式 set 拼接，使用工厂方法

---

## 四、单元测试 ✅

### 4.1 UserEntityTest（10 个测试用例）
| 测试方法 | 覆盖场景 | 状态 |
|---|---|:---:|
| `testValidateLoginStatus_ActiveUser_ShouldPass` | 正常用户登录 | ✅ |
| `testValidateLoginStatus_DisabledUser_ShouldThrowException` | 禁用用户登录 | ✅ |
| `testValidateLoginStatus_NullStatus_ShouldPass` | 空状态处理 | ✅ |
| `testValidatePassword_MatchingPassword_ShouldReturnTrue` | 密码匹配 | ✅ |
| `testValidatePassword_NonMatchingPassword_ShouldReturnFalse` | 密码不匹配 | ✅ |
| `testValidatePassword_NullEncodedPassword_ShouldReturnFalse` | 空密码处理 | ✅ |
| `testGetRoleOrDefault_HasRoleCode_ShouldReturnRoleCode` | 有角色码 | ✅ |
| `testGetRoleOrDefault_NullRoleCode_ShouldReturnDefaultRole` | 空角色码 | ✅ |
| `testGetRoleOrDefault_EmptyRoleCode_ShouldReturnDefaultRole` | 空字符串角色码 | ✅ |
| `testGetRoleOrDefault_BlankRoleCode_ShouldReturnDefaultRole` | 空白角色码 | ✅ |

### 4.2 OrderEntityTest（29 个测试用例）
| 测试类别 | 覆盖场景 | 数量 | 状态 |
|---|---|:---:|:---:|
| 静态工厂方法 | 空购物车、单商品、多商品、零数量 | 4 | ✅ |
| 金额计算 | 无商品、多商品、精度验证 | 3 | ✅ |
| 状态转换 | 支付、取消、发货、完成 | 4 | ✅ |
| 支付订单转换 | 单商品、多商品拼接 | 2 | ✅ |
| 订单项操作 | 添加、清空、空值检查 | 3 | ✅ |
| 边界情况 | 空值、异常参数 | 13 | ✅ |

### 4.3 测试覆盖率
- ✅ 测试总数：39 个
- ✅ 全部通过：39/39

---

## 五、编译验证 ✅

| 验证项 | 状态 |
|---|:---:|
| Maven 编译 | ✅ 通过 |
| 依赖检查 | ✅ 无循环依赖 |
| 模块间依赖 | ✅ 符合洋葱架构 |

---

## 六、架构边界守卫 ✅

### 6.1 检查策略文档
- ✅ 创建 `ArchitectureGuard.md`，包含：
  - Port 命名禁止规则
  - Map 返回值禁止规则
  - 包结构规范
  - ArchUnit 检查规则示例

### 6.2 CI 集成建议
```bash
# 检查 Port 结尾接口
find . -name "*Port.java" -path "*/domain/*"

# 检查 Map 返回类型
grep -r "Map<String, Object>" domain/src/main/java/*/repository/
```

---

## 七、技术债务清理

### 7.1 删除的文件
- ✅ `IAuthTokenPort.java`
- ✅ `AuthTokenPortImpl.java`

### 7.2 遗留待改进项
| 序号 | 改进项 | 优先级 |
|:---:|---|:---:|
| 1 | 引入 ArchUnit 进行自动化架构检查 | 中 |
| 2 | 完善 OrderEntity 状态转换测试 | 中 |
| 3 | 添加 Mockito 测试 MallUserServiceImpl | 低 |

---

## 八、验收结论

✅ **重构已完成并通过验证**

### 核心改进摘要
1. **契约规范**: 统一 `Gateway` 命名，消除 `Port` 命名
2. **防腐层**: 消除 `Map` 返回值，实现 PO-Entity 双向转换
3. **充血模型**: UserEntity 和 OrderEntity 具备完整业务行为
4. **测试覆盖**: 39 个单元测试覆盖核心业务场景
5. **架构防护**: 提供架构边界检查策略

### 后续建议
- 将架构检查规则集成到 CI/CD 流水线
- 定期进行 DDD 规范性审计
- 持续改进领域模型的行为化程度
