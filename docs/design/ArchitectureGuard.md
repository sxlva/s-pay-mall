# DDD 架构边界守卫说明

## 1. 禁止 Port 结尾接口出现在 Domain 层

### 问题背景
DDD 架构要求外部系统交互契约应定义在 `domain.模块.gateway` 包，并使用 `...Gateway` 命名。`Port` 结尾的接口属于旧命名规范，应予以杜绝。

### 检查规则
- **禁止模式**: `.*Port\.java` 文件出现在 `domain` 包下
- **正确模式**: 外部交互契约应在 `domain.模块.gateway` 包下，命名为 `I...Gateway`

### 检查方法（可集成到 CI）
```bash
# 检查是否有 Port 结尾的接口
find s-pay-mall-domain/src/main/java -name "*Port.java" | grep -v infrastructure
```

### 代码审查检查项
- [ ] 新增接口时检查命名是否以 `Gateway` 结尾
- [ ] 检查接口是否放在正确的 `gateway` 包下
- [ ] 删除旧的 `Port` 接口时同步删除其实现类

---

## 2. 禁止 Map 作为 Repository 返回值

### 问题背景
`Map<String, Object>` 作为 Repository 返回值会导致基础设施层数据泄漏到领域层，破坏防腐层原则。

### 检查规则
- **禁止模式**: Repository 接口方法返回 `Map<String, Object>` 或 `List<Map<String, Object>>`
- **正确模式**: 返回强类型的领域实体（如 `UserEntity`、`OrderEntity` 等）

### 检查方法（可集成到 CI）
```bash
# 检查 Repository 接口中的 Map 返回类型
grep -r "Map<String, Object>" s-pay-mall-domain/src/main/java/cn/fcr/domain/*/adapter/repository/
```

### 代码审查检查项
- [ ] Repository 接口方法返回类型应为领域实体
- [ ] Repository 实现类必须包含 PO 与 Entity 的双向转换
- [ ] Domain 层禁止导入任何 PO 类

---

## 3. 架构边界保护策略

### 3.1 包结构规范
```
domain/
├── mall/
│   ├── gateway/          # 外部系统交互契约（Gateway）
│   ├── adapter/
│   │   └── repository/   # 数据库交互契约（Repository）
│   ├── model/
│   │   ├── entity/       # 领域实体（充血模型）
│   │   └── valobj/       # 值对象
│   └── service/          # 领域服务（编排层）
```

### 3.2 依赖方向（洋葱架构）
```
Trigger (用户接口层)
    ↓
Application (应用层)
    ↓
Domain (领域层) ← 禁止反向依赖
    ↑
Infrastructure (基础设施层)
```

### 3.3 实体行为化检查
- [ ] Entity 不应是纯数据类（贫血模型）
- [ ] 核心业务逻辑应下沉到 Entity 中
- [ ] Service 层应仅负责编排，不应包含业务规则

---

## 4. ArchUnit 检查规则示例（如需）

若项目后续引入 ArchUnit，可添加以下规则：

```java
// 禁止 Domain 层出现 Port 结尾的类
classes().that().resideInAPackage("..domain..")
    .should().notHaveSimpleNameEndingWith("Port")

// 禁止 Repository 返回 Map
methods().that().areDeclaredInClassesThat()
    .resideInAPackage("..domain..repository..")
    .should().notHaveRawReturnType(Map.class)

// 禁止 Domain 层导入 PO 类
classes().that().resideInAPackage("..domain..")
    .should().notDependOnClassesThat()
    .haveSimpleNameEndingWith("PO")
```
