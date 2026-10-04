package cn.fcr.test;

import cn.fcr.application.AuthApplicationService;
import cn.fcr.domain.auth.login.service.ILoginService;
import cn.fcr.infrastructure.auth.login.gateway.WeixinGatewayImpl;
import cn.fcr.infrastructure.auth.token.JwtTokenProvider;
import cn.fcr.infrastructure.dao.auth.IMallUserDao;
import cn.fcr.infrastructure.dao.auth.IUserBindingDao;
import cn.fcr.infrastructure.dao.auth.IUserRoleDao;
import cn.fcr.types.common.Constants;
import io.jsonwebtoken.Claims;
import lombok.extern.slf4j.Slf4j;
import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.context.junit4.SpringRunner;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;

/**
 * 微信扫码登录 E2E Mock 测试（无微信服务器方案）
 *
 * <p>微信扫码登录链路的完整流程是：
 * 前端取 ticket → 用户扫码 → 微信服务器回调 /weixin/portal/receive →
 * handleWechatScanLogin 自动注册/绑定 → 签发 JWT → Redis 存登录态 → 前端轮询取 token。
 * 其中只有 {@code WeixinGatewayImpl} 真正发起对 api.weixin.qq.com 的 HTTP 调用
 * （取 ticket、发模板消息），其余环节（MySQL 自动注册、JWT 签发、Redis 登录态）
 * 全部在本系统内完成。</p>
 *
 * <p>本测试用 {@link MockBean} 将 {@code WeixinGatewayImpl}（IWeChatGateway 的唯一实现）
 * 替换为 Mock：ticket 由 Mock 返回固定值，模板消息为空实现。
 * 其余 Bean 全部使用 Spring 真实装配——特别是
 * {@code WeixinLoginService → IAuthTokenGateway(AuthTokenGatewayImpl) → JwtTokenProvider}
 * 这条刚完成接口合并的链路，以及 WeixinLoginGatewayImpl 的
 * MySQL 自动注册与 Redis 登录态。生产代码零改动。</p>
 *
 * <p>【P0-3】扫码登录入口已切换为 {@code AuthApplicationService.handleWechatScanLogin}
 * （事务边界在 Application 层，与 WeixinPortalController SCAN 分支一致）；
 * 场景 3 验证"通知 best-effort"：微信模板通知失败不影响已完成的自动注册与登录
 * （原"中途异常 → 整体回滚"语义已废弃）。</p>
 *
 * <p>依赖真实环境：MySQL(127.0.0.1:23306, db=s-pay-mall)、
 * Redis(127.0.0.1:26379, db=1)、RocketMQ(127.0.0.1:9876)。
 * 测试数据通过唯一 openid / 独立 ticket 隔离，@After 物理清理；
 * Redis 登录态 key 读取后即删，属既有业务行为。</p>
 *
 * @author 傅崇睿
 */
@Slf4j
@RunWith(SpringRunner.class)
@SpringBootTest
public class WeixinScanLoginMockE2ETest {

    /** 登录领域服务（WeixinLoginService，DomainServiceConfig 手动装配），用于取 ticket/轮询 */
    @Resource
    private ILoginService loginService;

    /** 认证应用层服务（P0-3：扫码登录事务边界，生产入口与 WeixinPortalController SCAN 分支一致） */
    @Resource
    private AuthApplicationService authApplicationService;

    /** JWT 提供方：用于验证签发出的 token 是合法 JWT（生产解析代码原样执行） */
    @Resource
    private JwtTokenProvider jwtTokenProvider;

    /** 本测试创建的 userId，用于 @After 物理清理 */
    private final List<Long> createdUserIds = new ArrayList<>();

    /** 本测试使用的 ticket，用于 @After 兜底清理 Redis 登录态 */
    private final List<String> usedTickets = new ArrayList<>();

    @Resource
    private IMallUserDao mallUserDao;

    @Resource
    private IUserBindingDao userBindingDao;

    @Resource
    private IUserRoleDao userRoleDao;

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    /**
     * 唯一真正调用微信服务器的网关，整体 Mock 掉：
     * ticket 由 Mock 返回，模板消息通知为空实现（void 方法默认 doNothing）。
     */
    @MockBean
    private WeixinGatewayImpl weixinGatewayImpl;

    /**
     * 场景 1：新 openid 首次扫码 → 自动注册 → 签发合法 JWT → Redis 登录态可轮询取回
     *
     * <p>断言重点：
     * ① token 能被生产 JwtTokenProvider.parse 解析（签名/secret 未变）；
     * ② claims 中 uid/username/role 与自动注册结果一致（username=wx_user_{uid}，role=MEMBER）；
     * ③ checkLogin 第一次取回同一 token、第二次返回空（Redis get 后即删的既有行为）。</p>
     *
     * @throws Exception 断言失败
     */
    @Test
    public void testWechatScanLogin_newOpenid_autoRegisterAndIssueJwt() throws Exception {
        // ========== 模拟微信服务器：返回固定 ticket，通知为空实现 ==========
        String ticket = "MOCK_TICKET_" + UUID.randomUUID().toString().substring(0, 8);
        when(weixinGatewayImpl.createQrCodeTicket()).thenReturn(ticket);
        assertEquals("取 ticket 应走 Mock 的微信网关", ticket, loginService.createQrCodeTicket());

        // ========== 模拟微信扫码回调后服务端处理（WeixinPortalController SCAN 分支同一入口） ==========
        String openid = "mock_openid_" + UUID.randomUUID().toString().substring(0, 8);
        String token = authApplicationService.handleWechatScanLogin(ticket, openid);
        assertNotNull("handleWechatScanLogin 应返回 JWT token", token);
        usedTickets.add(ticket);

        // ========== 断言 1：token 是生产 JwtTokenProvider 签发的合法 JWT ==========
        Claims claims = jwtTokenProvider.parse(token);
        Long userId = claims.get("uid", Long.class);
        assertNotNull("JWT claims 应包含 uid", userId);
        createdUserIds.add(userId);
        log.info("【测试】签发 JWT: uid={}, username={}, role={}",
                userId, claims.get("username"), claims.get("role"));

        // ========== 断言 2：与自动注册结果一致（wx_user_{id} / MEMBER） ==========
        assertEquals("JWT username 应为自动注册的 wx_user_{uid}", "wx_user_" + userId, claims.get("username"));
        assertEquals("JWT role 应为默认成员角色", Constants.DEFAULT_ROLE_MEMBER, claims.get("role"));
        assertEquals("JWT subject 应为 username", "wx_user_" + userId, claims.getSubject());

        // ========== 断言 3：MySQL 侧自动注册数据真实落库 ==========
        assertNotNull("mall_user 应已自动注册", mallUserDao.selectById(userId));
        assertNotNull("user_binding 应已建立 openid 绑定", userBindingDao.findByIdentityTypeAndIdentifier(
                Constants.IDENTITY_TYPE_WECHAT_MP, openid));

        // ========== 断言 4：Redis 登录态可轮询取回（checkLogin），取后删除 ==========
        assertEquals("checkLogin 应取回同一 token", token, loginService.checkLogin(ticket));
        assertNull("checkLogin 第二次应为空（Redis get 后即删）", loginService.checkLogin(ticket));
    }

    /**
     * 场景 2：同一 openid 再次扫码 → 复用已绑定账号 → uid 不变
     *
     * <p>验证 findUserIdByOpenid 命中已有绑定时不重复注册，
     * 两次签发的 JWT 指向同一 userId。</p>
     *
     * @throws Exception 断言失败
     */
    @Test
    public void testWechatScanLogin_sameOpenid_reuseBoundUser() throws Exception {
        // ========== 首次扫码：建立绑定 ==========
        String openid = "mock_openid_" + UUID.randomUUID().toString().substring(0, 8);
        String ticket1 = "MOCK_TICKET_" + UUID.randomUUID().toString().substring(0, 8);
        String token1 = authApplicationService.handleWechatScanLogin(ticket1, openid);
        usedTickets.add(ticket1);
        Long userId1 = jwtTokenProvider.parse(token1).get("uid", Long.class);
        createdUserIds.add(userId1);

        // ========== 同一 openid 再次扫码：ticket 不同，账号应复用 ==========
        String ticket2 = "MOCK_TICKET_" + UUID.randomUUID().toString().substring(0, 8);
        String token2 = authApplicationService.handleWechatScanLogin(ticket2, openid);
        usedTickets.add(ticket2);
        Long userId2 = jwtTokenProvider.parse(token2).get("uid", Long.class);

        assertEquals("同一 openid 再次扫码应复用同一账号", userId1, userId2);
        assertEquals("两次 token 的 username 应一致",
                jwtTokenProvider.parse(token1).get("username"),
                jwtTokenProvider.parse(token2).get("username"));
        assertTrue("两次扫码只应注册一个用户", createdUserIds.size() == 1);
    }

    /**
     * 场景 3：自动注册后微信模板通知失败 → 注册正常提交，登录态仍可取回（通知 best-effort）
     *
     * <p>【P0-3 验收·通知 best-effort】微信模板消息只是扫码人微信侧的辅助提醒，
     * 前端经 check_login 轮询取 token，不依赖本条消息；因此通知失败不得影响
     * 已完成的自动注册与登录：4 个写操作（插 mall_user → 更新用户名 → 插
     * user_binding → 插 user_role，由
     * {@code IMallUserService.registerWeChatUserByScan} 统一执行）正常提交，
     * check_login 仍能取回 token。
     * （本场景原验收为"通知失败 → 整体回滚"，已按通知 best-effort 业务语义废弃。）</p>
     *
     * @throws Exception 断言失败
     */
    @Test
    public void testWechatScanLogin_notificationFailure_loginStillSucceeds() throws Exception {
        String ticket = "MOCK_TICKET_" + UUID.randomUUID().toString().substring(0, 8);
        usedTickets.add(ticket); // 先登记，@After 兜底清理可能残留的 Redis 登录态
        String openid = "mock_openid_" + UUID.randomUUID().toString().substring(0, 8);

        // 模拟微信模板消息通知失败（流程中最后一个外部动作）
        doThrow(new RuntimeException("模拟微信通知失败")).when(weixinGatewayImpl).sendLoginNotification(anyString());

        // 通知异常不向上传播：自动注册与登录仍应成功
        String token = authApplicationService.handleWechatScanLogin(ticket, openid);
        assertNotNull("通知失败不应影响登录，仍应返回 JWT token", token);
        // 无需手动恢复 Mock：Spring 默认在每个测试方法后重置 @MockBean

        // 验收 1：自动注册 4 个写操作正常提交（openid 绑定必须存在）
        assertNotNull("通知失败后自动注册应正常提交（user_binding 必须存在）",
                userBindingDao.findByIdentityTypeAndIdentifier(Constants.IDENTITY_TYPE_WECHAT_MP, openid));

        // 验收 2：登录态可轮询取回（checkLogin）
        assertEquals("checkLogin 应取回同一 token", token, loginService.checkLogin(ticket));

        // 登记 userId 供 @After 物理清理
        createdUserIds.add(jwtTokenProvider.parse(token).get("uid", Long.class));
    }

    /**
     * 每个测试方法后物理清理：user_role、user_binding、mall_user 依次删除
     * （与 user_role / user_binding 存在外键约束的顺序一致），
     * 并兜底删除可能残留的 Redis 登录态 key。
     */
    @After
    public void cleanup() {
        for (Long userId : createdUserIds) {
            userRoleDao.deleteByUserId(userId);
            userBindingDao.deleteByUserId(userId);
            mallUserDao.deleteById(userId);
            log.info("【测试】清理自动注册数据: userId={}", userId);
        }
        createdUserIds.clear();
        for (String ticket : usedTickets) {
            stringRedisTemplate.delete(ticket);
        }
        usedTickets.clear();
    }
}
