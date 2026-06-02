# S-Pay-Mall RocketMQ 配置优化计划

## 问题分析

### 当前状态
1. **环境变量读取失败**: Docker Compose 执行时显示 `ROCKETMQ_ACCESS_KEY` 和 `ROCKETMQ_SECRET_KEY` 变量未被设置
   - 原因：.env 文件位置不被 docker-compose 自动识别
2. **多份 .env 配置**: 存在两份不同的 .env 文件，导致配置不一致
3. **RocketMQ Dashboard 认证**: 
   - plain_acl.yml 中配置了 `fcradmin/Fcradmin006`
   - Dashboard 登录要求密码和用户名但输入无效
4. **Redis 健康检查失败**: 
   - Redis container 启动时 requirepass 参数未正确传递
   - 健康检查命令 `redis-cli ping` 需要密码认证
5. **缺失 RocketMQ 在 docker-compose-environment.yml 中的定义**
   - 当前只有 NameServer 和 Broker 的配置片段缺失
   - Dashboard 服务 (rmqdashboard-spay) 无法获取认证信息

## 优化方案

### Phase 1: .env 文件统一管理
**目标**: 创建单一 .env 文件作为唯一环境变量来源

#### 1.1 归并现有配置
- 检查是否存在 `/Users/xiaolv/Develop/projects/backend/java/s-pay-mall/docs/dev-ops/.env`
- 整合所有环境变量到根目录 `.env`:
  - MySQL 配置
  - Redis 配置
  - Redis Commander 认证
  - RocketMQ 认证 (AccessKey/SecretKey)
  - RocketMQ Dashboard 默认管理员账户
  - 第三方服务 (WeChat/Alipay)

#### 1.2 环境变量添加
```
# RocketMQ Dashboard 管理员账户 (新增)
ROCKETMQ_DASHBOARD_USER=admin
ROCKETMQ_DASHBOARD_PASSWORD=Fcr006

# RocketMQ 服务认证
ROCKETMQ_ACCESS_KEY=fcradmin
ROCKETMQ_SECRET_KEY=Fcradmin006
```

### Phase 2: Docker Compose 配置修正

#### 2.1 修复 docker-compose-environment.yml

**问题修复**:
1. **Redis 健康检查**: 修改健康检查命令以支持密码认证
   ```yaml
   healthcheck:
     test: ["CMD", "redis-cli", "-a", "${REDIS_PASSWORD}", "ping"]
   ```

2. **添加 RocketMQ 服务定义**:
   - NameServer 配置
   - Broker 配置
   - Dashboard 配置 (含默认管理员认证)

3. **环境变量注入**:
   - Docker Compose 默认读取根目录 `.env`
   - 确保所有服务通过 `${VAR_NAME}` 引用环境变量
   - 添加 `--env-file` 参数备选方案

#### 2.2 RocketMQ Dashboard 配置
Dashboard 需要以下环境变量:
```yaml
environment:
  - ROCKETMQ_NAMESRV_ADDR=rmqnamesrv-spay:9876
  - USER_NAME=${ROCKETMQ_ACCESS_KEY}
  - USER_PASSWORD=${ROCKETMQ_SECRET_KEY}
  - LOGIN_REQUIRED=true
```

### Phase 3: 认证配置

#### 3.1 plain_acl.yml 同步
- 当前: 
  ```yaml
  - accessKey: fcradmin
    secretKey: Fcradmin006
  ```
- 确保与 .env 中的 `ROCKETMQ_ACCESS_KEY` 和 `ROCKETMQ_SECRET_KEY` 一致

#### 3.2 RocketMQ Broker ACL 启用
在 RocketMQ Broker 配置中需要:
```properties
aclEnable=true
accessLogEnable=false
brokerClusterName=DefaultCluster
brokerName=broker-a
brokerId=0
```

### Phase 4: 应用配置集成

#### 4.1 application-dev.yml 补充
添加 RocketMQ 配置:
```yaml
rocketmq:
  producer:
    group: ${ROCKETMQ_PRODUCER_GROUP:pay-producer-group}
    access_key: ${ROCKETMQ_ACCESS_KEY}
    secret_key: ${ROCKETMQ_SECRET_KEY}
  # 如使用阿里云 RocketMQ
  # name-server: ${ROCKETMQ_NAMESRV_ADDR:http://127.0.0.1:8080}
```

#### 4.2 docker-compose-app.yml 环保
将 RocketMQ 认证信息注入应用容器:
```yaml
environment:
  - ROCKETMQ_ACCESS_KEY=${ROCKETMQ_ACCESS_KEY}
  - ROCKETMQ_SECRET_KEY=${ROCKETMQ_SECRET_KEY}
  - ROCKETMQ_NAMESRV_ADDR=rmqnamesrv-spay:9876
```

## 实施步骤

### Step 1: 备份现有配置
```bash
cp /Users/xiaolv/Develop/projects/backend/java/s-pay-mall/.env \
   /Users/xiaolv/Develop/projects/backend/java/s-pay-mall/.env.backup
```

### Step 2: 更新根目录 .env
- 添加 RocketMQ Dashboard 认证信息
- 验证所有变量完整性

### Step 3: 修改 docker-compose-environment.yml
1. 修复 Redis 健康检查
2. 补全 RocketMQ 完整定义 (NameServer/Broker/Dashboard)
3. 所有服务使用环境变量引用

### Step 4: 更新应用配置
- 补充 application-dev.yml 中的 RocketMQ 配置
- 更新 docker-compose-app.yml 环境变量

### Step 5: 验证测试
```bash
# 启动环境
docker-compose -f docker-compose-environment.yml up -d

# 验证环境变量
docker-compose -f docker-compose-environment.yml config | grep ROCKETMQ

# 访问服务
# RocketMQ Dashboard: http://localhost:8162
# Redis Commander: http://localhost:8082
# PHPMyAdmin: http://localhost:8898

# 测试 RocketMQ 认证
# 登录 Dashboard 使用: fcradmin / Fcradmin006
```

## 风险评估

| 风险项 | 概率 | 影响 | 缓解措施 |
|------|------|------|--------|
| 环境变量冲突 | 中 | 中 | 备份现有配置，分步验证 |
| RocketMQ Broker ACL 导致连接失败 | 中 | 高 | 暂时禁用 ACL 测试基础连接 |
| 密码包含特殊字符导致解析失败 | 低 | 中 | 使用引号并转义特殊字符 |

## 验收标准

1. ✓ .env 文件作为唯一配置源
2. ✓ Docker Compose 执行无环境变量警告
3. ✓ Redis 容器正常启动且健康检查通过
4. ✓ RocketMQ Dashboard 可访问并支持认证登录
5. ✓ RocketMQ 认证凭证与 plain_acl.yml 同步
6. ✓ 应用能够正确读取和使用 RocketMQ 配置
7. ✓ 所有服务间通信正常 (应用 ↔ RocketMQ, Redis 等)

