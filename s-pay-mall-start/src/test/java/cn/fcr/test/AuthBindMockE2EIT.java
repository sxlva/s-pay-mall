package cn.fcr.test;

import cn.fcr.infrastructure.auth.login.gateway.WeixinGatewayImpl;
import cn.fcr.infrastructure.auth.token.JwtTokenProvider;
import cn.fcr.infrastructure.dao.auth.IMallUserDao;
import cn.fcr.infrastructure.dao.auth.IUserBindingDao;
import cn.fcr.infrastructure.dao.auth.IUserRoleDao;
import cn.fcr.infrastructure.dao.auth.po.UserBinding;
import cn.fcr.types.common.Constants;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.Claims;
import lombok.extern.slf4j.Slf4j;
import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import javax.annotation.Resource;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 账号绑定（微信 ↔ 账密）E2E Mock 测试（无微信服务器方案，TD-10 收尾验收）
 *
 * <p>覆盖 2026-10-07 新增的账号绑定功能完整闭环，与生产链路逐层一致：
 * HTTP 请求 → Spring Security 过滤链 → MallAuthController/WeixinPortalController
 * → Domain（WeixinBindService / IMallUserService）→ MySQL user_binding / mall_user
 * → Redis 绑定票据与登录态。唯一 Mock 的仍是 {@link WeixinGatewayImpl}
 * （对 api.weixin.qq.com 的 ticket 创建与模板消息），与其余 Mock E2E 测试口径一致；
 * 每次调用返回独立随机 ticket，模拟微信侧真实票据。</p>
 *
 * <p>场景 1：账密注册用户绑定微信 → 微信扫码登录回到同一账户 → 微信用户补设密码
 * → 账密登录回到同一账户（双向互通闭环 + UserProfileRes.wechatBound 输出 + 票据销毁）。
 * 场景 2：openId 被其他用户绑定时拒绝；同一用户重复绑定幂等。
 * 场景 3：无效/过期票据确认绑定被拒绝。</p>
 *
 * <p>依赖真实环境：MySQL(127.0.0.1:23306, db=s-pay-mall)、
 * Redis(127.0.0.1:26379, db=1)、RocketMQ(127.0.0.1:9876)。
 * 测试数据通过唯一 username/openid/ticket 隔离，@After 物理清理。</p>
 *
 * @author 傅崇睿
 */
@Slf4j
@RunWith(SpringRunner.class)
@SpringBootTest
@AutoConfigureMockMvc
public class AuthBindMockE2EIT {

    /** 全链路 HTTP 入口（含 Spring Security 过滤链） */
    @Autowired
    private MockMvc mockMvc;

    /** JWT 提供方：解析各登录方式签发的 token，断言指向同一 uid */
    @Resource
    private JwtTokenProvider jwtTokenProvider;

    @Resource
    private IMallUserDao mallUserDao;

    @Resource
    private IUserBindingDao userBindingDao;

    @Resource
    private IUserRoleDao userRoleDao;

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    private final ObjectMapper objectMapper = new ObjectMapper();

    /** 唯一真正调用微信服务器的网关：ticket 由 Mock 每次返回独立随机值，模板消息为空实现 */
    @MockBean
    private WeixinGatewayImpl weixinGatewayImpl;

    /** 本测试创建的 userId，用于 @After 物理清理 */
    private final List<Long> createdUserIds = new ArrayList<>();

    /** 登录态 ticket（Redis 以原始值作 key），用于 @After 兜底清理 */
    private final List<String> usedLoginTickets = new ArrayList<>();

    /** 绑定票据 ticket（Redis 键带 wechat:bind:ticket: 前缀），用于 @After 兜底清理 */
    private final List<String> usedBindTickets = new ArrayList<>();

    /**
     * 场景 1：账号绑定双向互通完整闭环
     *
     * <p>账密注册 → 扫码绑定微信 → 微信扫码登录同一账户 → 补设密码 →
     * 账密登录同一账户。任一步 uid 不一致即失败。</p>
     *
     * @throws Exception 断言失败
     */
    @Test
    public void testBindChain_fullCycle_bothLoginMethodsSameAccount() throws Exception {
        when(weixinGatewayImpl.createQrCodeTicket())
                .thenAnswer(invocation -> "MOCK_TICKET_" + UUID.randomUUID().toString().substring(0, 8));
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        String openid = "e2e_openid_" + suffix;

        // ========== 步骤 1：账密注册，拿到 JWT ==========
        String username = "e2e_bind_" + suffix;
        JsonNode register = postJson("/mall-api/v1/auth/register",
                "{\"username\":\"" + username + "\",\"password\":\"Init123456\"}");
        assertEquals("账密注册应成功", "0000", register.get("code").asText());
        String tokenA = register.get("data").get("token").asText();
        // LoginRes.userId 自 TD-1 修复后按 API_CONTRACT 登记的 camelCase（userId）输出
        Long userIdA = register.get("data").get("userId").asLong();
        createdUserIds.add(userIdA);

        // ========== 步骤 2：获取绑定二维码 ticket（/auth/bind/qrcode） ==========
        JsonNode qrCode = getJson("/mall-api/v1/auth/bind/qrcode", tokenA);
        assertEquals("获取绑定二维码应成功", "0000", qrCode.get("code").asText());
        String bindTicket = qrCode.get("data").asText();
        usedBindTickets.add(bindTicket);

        // ========== 步骤 3：模拟微信服务器回调 SCAN 事件（绑定分支写入 openId） ==========
        mockWechatScanCallback(openid, bindTicket);

        // ========== 步骤 4：轮询绑定状态（/auth/bind/status） ==========
        JsonNode bindStatus = getJson("/mall-api/v1/auth/bind/status?ticket=" + bindTicket, tokenA);
        assertEquals("0000", bindStatus.get("code").asText());
        assertEquals("BIND_SUCCESS", bindStatus.get("data").get("status").asText());
        assertEquals(openid, bindStatus.get("data").get("openId").asText());

        // ========== 步骤 5：确认绑定（/auth/bind/confirm），openId 服务端按 ticket 解析 ==========
        JsonNode confirm = postJson("/mall-api/v1/auth/bind/confirm",
                "{\"ticket\":\"" + bindTicket + "\"}", tokenA);
        assertEquals("确认绑定应成功", "0000", confirm.get("code").asText());

        // ========== 步骤 6：绑定成功后票据应已销毁，重复确认须失败 ==========
        JsonNode reConfirm = postJson("/mall-api/v1/auth/bind/confirm",
                "{\"ticket\":\"" + bindTicket + "\"}", tokenA);
        assertEquals("票据销毁后重复确认应失败", "0001", reConfirm.get("code").asText());

        // ========== 步骤 7：个人资料返回 wechatBound=true，DB 绑定行落到 userIdA ==========
        JsonNode profile = getJson("/mall-api/v1/profile", tokenA);
        assertEquals("0000", profile.get("code").asText());
        assertTrue("wechatBound 应为 true", profile.get("data").get("wechatBound").asBoolean());
        UserBinding binding = userBindingDao.findByIdentityTypeAndIdentifier(
                Constants.IDENTITY_TYPE_WECHAT_MP, openid);
        assertNotNull("user_binding 应有绑定记录", binding);
        assertEquals("绑定应落到账密注册的账户", userIdA, binding.getUserId());

        // ========== 步骤 8：微信扫码登录（新 ticket + 同 openid）应复用同一账户 ==========
        JsonNode loginQr = getJson("/pay-api/v1/login/weixin_qrcode_ticket", null);
        assertEquals("0000", loginQr.get("code").asText());
        String loginTicket = loginQr.get("data").asText();
        usedLoginTickets.add(loginTicket);
        mockWechatScanCallback(openid, loginTicket);

        JsonNode checkLogin = getJson("/pay-api/v1/login/check_login?ticket=" + loginTicket, null);
        assertEquals("0000", checkLogin.get("code").asText());
        String tokenWx = checkLogin.get("data").asText();
        Claims claimsWx = jwtTokenProvider.parse(tokenWx);
        assertEquals("微信扫码登录应回到绑定的同一账户", userIdA, claimsWx.get("uid", Long.class));
        log.info("【测试】微信登录 uid={} 与账密注册 uid={} 一致", claimsWx.get("uid"), userIdA);

        // ========== 步骤 9：微信身份补设密码（/auth/password） ==========
        JsonNode setPwd = postJson("/mall-api/v1/auth/password",
                "{\"password\":\"NewPass123\"}", tokenWx);
        assertEquals("微信用户补设密码应成功", "0000", setPwd.get("code").asText());

        // ========== 步骤 10：设置密码后状态应转为正常（WECHAT 2 → ACTIVE 1） ==========
        Integer status = mallUserDao.selectById(userIdA).getStatus();
        assertEquals("补设密码后用户状态应转为正常", Constants.USER_STATUS_ACTIVE, status);

        // ========== 步骤 11：账密登录应回到同一账户 ==========
        JsonNode login = postJson("/mall-api/v1/auth/login",
                "{\"username\":\"" + username + "\",\"password\":\"NewPass123\"}");
        assertEquals("账密登录应成功", "0000", login.get("code").asText());
        Long loginUid = login.get("data").get("userId").asLong();
        assertEquals("账密登录应回到绑定的同一账户", userIdA, loginUid);
        log.info("【测试】账密登录 uid={} 与注册 uid={} 一致", loginUid, userIdA);
    }

    /**
     * 场景 2：openId 已被其他用户绑定 → 拒绝；同一用户重复绑定 → 幂等不重复插行
     *
     * @throws Exception 断言失败
     */
    @Test
    public void testBindWeChat_conflictRejectedAndIdempotent() throws Exception {
        when(weixinGatewayImpl.createQrCodeTicket())
                .thenAnswer(invocation -> "MOCK_TICKET_" + UUID.randomUUID().toString().substring(0, 8));
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        String openid = "e2e_openid_conflict_" + suffix;

        // 用户 B 先绑定成功
        String tokenB = registerAndGetToken("e2e_b_" + suffix);
        String bindTicketB = getBindTicket(tokenB);
        mockWechatScanCallback(openid, bindTicketB);
        JsonNode confirmB = postJson("/mall-api/v1/auth/bind/confirm",
                "{\"ticket\":\"" + bindTicketB + "\"}", tokenB);
        assertEquals("首次绑定应成功", "0000", confirmB.get("code").asText());

        // 用户 C 扫同一个微信 → 确认绑定应被拒绝
        String tokenC = registerAndGetToken("e2e_c_" + suffix);
        String bindTicketC = getBindTicket(tokenC);
        mockWechatScanCallback(openid, bindTicketC);
        JsonNode confirmC = postJson("/mall-api/v1/auth/bind/confirm",
                "{\"ticket\":\"" + bindTicketC + "\"}", tokenC);
        assertEquals("openId 已被他人绑定时应拒绝", "0001", confirmC.get("code").asText());
        assertTrue("拒绝信息应说明已被其他用户绑定",
                confirmC.get("info").asText().contains("已被其他用户绑定"));

        // 用户 B 再次绑定同一微信 → 幂等成功，绑定行仍只有 1 条
        String bindTicketB2 = getBindTicket(tokenB);
        mockWechatScanCallback(openid, bindTicketB2);
        JsonNode confirmB2 = postJson("/mall-api/v1/auth/bind/confirm",
                "{\"ticket\":\"" + bindTicketB2 + "\"}", tokenB);
        assertEquals("同一用户重复绑定应幂等成功", "0000", confirmB2.get("code").asText());
        Integer count = userBindingDao.countByIdentityTypeAndIdentifier(
                Constants.IDENTITY_TYPE_WECHAT_MP, openid);
        assertEquals("幂等重复绑定不应产生重复记录", Integer.valueOf(1), count);
    }

    /**
     * 场景 3：无效/过期票据确认绑定 → 拒绝
     *
     * @throws Exception 断言失败
     */
    @Test
    public void testBindConfirm_invalidTicket_rejected() throws Exception {
        String token = registerAndGetToken("e2e_d_" + UUID.randomUUID().toString().substring(0, 8));
        JsonNode confirm = postJson("/mall-api/v1/auth/bind/confirm",
                "{\"ticket\":\"NOT_EXIST_TICKET\"}", token);
        assertEquals("无效票据应拒绝", "0001", confirm.get("code").asText());
        assertTrue("拒绝信息应提示二维码过期或未扫码",
                confirm.get("info").asText().contains("二维码已过期"));
    }

    /**
     * 场景 4：未认证请求统一 401 + 0003 JSON（S-02 验收）
     *
     * <p>匿名或携带非法 token 访问受保护端点，必须由 Security 入口点拦截：
     * HTTP 401 + {@code {"code":"0003","info":"未登录或登录已过期"}}，
     * 不得穿透到 Controller（修复前为默认 403 空响应，前端无法驱动跳登录）。</p>
     *
     * @throws Exception 断言失败
     */
    @Test
    public void testUnauthenticatedRequest_receives401Json() throws Exception {
        // 匿名访问受保护端点
        MvcResult anonymous = mockMvc.perform(get("/mall-api/v1/auth/profile"))
                .andExpect(status().isUnauthorized())
                .andReturn();
        JsonNode anonymousBody = objectMapper.readTree(
                anonymous.getResponse().getContentAsString(java.nio.charset.StandardCharsets.UTF_8));
        assertEquals("匿名访问应返回 0003 未登录", "0003", anonymousBody.get("code").asText());

        // 携带非法 token 访问受保护端点（S-02：解析失败不得静默放行）
        MvcResult badToken = mockMvc.perform(get("/mall-api/v1/auth/profile")
                        .header("Authorization", "Bearer not.a.valid.token"))
                .andExpect(status().isUnauthorized())
                .andReturn();
        JsonNode badTokenBody = objectMapper.readTree(
                badToken.getResponse().getContentAsString(java.nio.charset.StandardCharsets.UTF_8));
        assertEquals("非法 token 应返回 0003 未登录", "0003", badTokenBody.get("code").asText());
    }

    /**
     * 注册账密用户并返回 token（登记 createdUserIds 供清理）
     */
    private String registerAndGetToken(String username) throws Exception {
        JsonNode register = postJson("/mall-api/v1/auth/register",
                "{\"username\":\"" + username + "\",\"password\":\"Init123456\"}");
        assertEquals("注册应成功", "0000", register.get("code").asText());
        createdUserIds.add(register.get("data").get("userId").asLong());
        return register.get("data").get("token").asText();
    }

    /**
     * 获取绑定二维码 ticket（登记 usedBindTickets 供清理）
     */
    private String getBindTicket(String token) throws Exception {
        JsonNode qrCode = getJson("/mall-api/v1/auth/bind/qrcode", token);
        assertEquals("0000", qrCode.get("code").asText());
        String ticket = qrCode.get("data").asText();
        usedBindTickets.add(ticket);
        return ticket;
    }

    /**
     * 模拟微信服务器扫码回调：向 /pay-api/v1/weixin/portal/receive 发送 SCAN 事件 XML
     * （POST 入口仅接收不验签，与生产微信服务器行为一致；openid 经 query 传递）
     *
     * @param openid 微信用户 OpenID
     * @param ticket 场景票据（绑定票据或登录票据，由 SCAN 分流自动判定）
     */
    private void mockWechatScanCallback(String openid, String ticket) throws Exception {
        String xml = "<xml>"
                + "<MsgType><![CDATA[event]]></MsgType>"
                + "<Event><![CDATA[SCAN]]></Event>"
                + "<Ticket><![CDATA[" + ticket + "]]></Ticket>"
                + "</xml>";
        MvcResult result = mockMvc.perform(post("/pay-api/v1/weixin/portal/receive")
                        .param("openid", openid)
                        .param("signature", "mock_signature")
                        .param("timestamp", "1700000000")
                        .param("nonce", "mock_nonce")
                        .contentType(MediaType.APPLICATION_XML)
                        .content(xml))
                .andExpect(status().isOk())
                .andReturn();
        log.info("【测试】模拟微信 SCAN 回调: ticket={}, openid={}, 响应={}",
                ticket, openid, result.getResponse().getContentAsString());
    }

    /**
     * 发起 GET 请求并解析 JSON 响应
     *
     * @param url   请求地址（可带 query）
     * @param token JWT token，可传 null（公开接口）
     */
    private JsonNode getJson(String url, String token) throws Exception {
        org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder builder = get(url);
        if (token != null) {
            builder.header("Authorization", "Bearer " + token);
        }
        MvcResult result = mockMvc.perform(builder)
                .andExpect(status().isOk())
                .andReturn();
        // 响应无 charset 时 MockMvc 默认按 ISO-8859-1 解码，中文 info 会乱码，显式按 UTF-8 读取
        return objectMapper.readTree(result.getResponse().getContentAsString(java.nio.charset.StandardCharsets.UTF_8));
    }

    /**
     * 发起 POST JSON 请求并解析响应
     *
     * @param url   请求地址
     * @param body  JSON 请求体
     * @param token JWT token，可传 null（公开接口）
     */
    private JsonNode postJson(String url, String body, String token) throws Exception {
        org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder builder = post(url)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body);
        if (token != null) {
            builder.header("Authorization", "Bearer " + token);
        }
        MvcResult result = mockMvc.perform(builder)
                .andExpect(status().isOk())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString(java.nio.charset.StandardCharsets.UTF_8));
    }

    private JsonNode postJson(String url, String body) throws Exception {
        return postJson(url, body, null);
    }

    /**
     * 每个测试方法后物理清理：user_role、user_binding、mall_user 依次删除
     * （与外键约束顺序一致），并兜底清理 Redis 登录态 key 与绑定票据 key。
     */
    @After
    public void cleanup() {
        for (Long userId : createdUserIds) {
            userRoleDao.deleteByUserId(userId);
            userBindingDao.deleteByUserId(userId);
            mallUserDao.deleteById(userId);
            log.info("【测试】清理账号绑定测试数据: userId={}", userId);
        }
        createdUserIds.clear();
        for (String ticket : usedLoginTickets) {
            stringRedisTemplate.delete(ticket);
        }
        usedLoginTickets.clear();
        for (String ticket : usedBindTickets) {
            stringRedisTemplate.delete(Constants.REDIS_WECHAT_BIND_TICKET_PREFIX + ticket);
        }
        usedBindTickets.clear();
    }
}
