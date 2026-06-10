# 微信扫码登录流程（V2.0）

> **领域上下文**：Auth Context  
> **演进说明**：架构保持稳定，新增"技术亮点与面试高频考点"章节

---

## 一、流程总览

登录流程分为两大阶段：

| 阶段 | 同步/异步 | 核心职责 |
|------|----------|----------|
| 获取登录二维码 | 同步 | AccessToken 缓存、生成 ticket |
| 扫码回调与轮询 | 异步 | 微信回调处理、前端轮询检测 |

---

## 二、获取登录二维码

```mermaid
sequenceDiagram
    autonumber
    participant User as 用户浏览器
    participant F as 前端 Vue
    participant C as LoginController
    participant S as WeixinLoginService
    participant A as WeixinApiService
    participant Cache as Guava Cache
    participant WX as 微信API服务器

    User->>F: 访问 /login 页面
    F->>C: GET /api/v1/login/weixin_qrcode_ticket
    C->>S: createQrCodeTicket()
    S->>A: getAccessToken(appId, appSecret)
    A->>Cache: get("wx:token:appid")
    
    alt 缓存命中
        Cache-->>A: accessToken
    else 缓存未命中
        A->>WX: HTTP GET /cgi-bin/token
        WX-->>A: { access_token, expires_in }
        A->>Cache: put("wx:token:appid", token, 7000s)
    end
    
    A->>WX: HTTP POST /cgi-bin/qrcode/create
    WX-->>A: { ticket }
    A-->>S: ticket
    S-->>C: ticket
    C-->>F: Response<String>(ticket)
    F->>User: 渲染二维码
```

---

## 三、扫码回调与轮询

```mermaid
sequenceDiagram
    autonumber
    participant WX as 微信客户端
    participant WXAPI as 微信服务器
    participant Portal as WeixinPortalController
    participant Service as WeixinLoginService
    participant Cache as Guava Cache
    participant F as 前端
    participant C as LoginController

    WX->>WXAPI: 扫描二维码
    WXAPI->>Portal: POST /api/v1/weixin/portal/receive (XML)
    Portal->>Service: saveLoginState(ticket, openid)
    Service->>Cache: put("wx:login:ticket:"+ticket, openid, 180s)
    Portal-->>WXAPI: "success"

    loop 每 3 秒轮询
        F->>C: GET /api/v1/login/check_login?ticket=xxx
        C->>Service: checkLogin(ticket)
        Service->>Cache: getIfPresent("wx:login:ticket:"+ticket)
        alt 已扫码
            Cache-->>Service: openid
            Service-->>C: openid
            C-->>F: Response<String>(openid)
            F->>F: 停止轮询，跳转主页
        else 未扫码
            Cache-->>Service: null
            Service-->>C: null
            C-->>F: Response<String>(未登录)
        end
    end
```

---

## 四、凭证时效控制

| 凭证 | 存储位置 | TTL | 说明 |
|------|----------|-----|------|
| accessToken | Guava Cache | 7000s | 提前于官方 7200s 过期 |
| ticket | 微信服务器 | 1800s | 微信侧强制 |
| ticket→openid | Guava Cache | 180s | 防止轮询无限重试 |

---

## 五、技术亮点与面试高频考点

| 考点 | 标准答案 |
|------|----------|
| **AccessToken 缓存** | 微信 API 有频率限制（2000次/分），缓存可显著降低 RT 与限流风险 |
| **轮询 vs 长连接** | 短时一次性交互，3s 轮询实现简单、容错高；大规模场景可升级 SSE |
| **OpenID vs UnionID** | OpenID 是某公众号下的唯一标识；UnionID 是开放平台下跨应用统一标识 |
| **安全性** | ticket→openid 仅存于服务端缓存，前端只见 ticket；短 TTL 降低重放窗口 |
| **解耦设计** | 微信回调是被动事件，轮询是主动探查，二者解耦保证系统稳定 |

---

> **详细架构请参考**：[module-auth.md](module-auth.md)
