package cn.fcr.trigger.http.mall;

import cn.fcr.api.dto.common.req.LoginReq;
import cn.fcr.api.dto.common.req.RegisterReq;
import cn.fcr.api.dto.user.req.UserBindConfirmReq;
import cn.fcr.api.dto.user.req.UserPasswordSetReq;
import cn.fcr.api.response.Response;
import cn.fcr.api.dto.user.res.UserBindStatusRes;
import cn.fcr.api.dto.common.res.LoginRes;
import cn.fcr.api.dto.user.res.UserProfileRes;
import cn.fcr.domain.auth.login.service.ILoginService;
import cn.fcr.domain.auth.login.service.WeixinBindService;
import cn.fcr.domain.mall.user.model.valobj.UserLoginVO;
import cn.fcr.domain.mall.user.model.valobj.UserProfile;
import cn.fcr.domain.mall.user.service.IMallUserService;
import cn.fcr.trigger.http.BaseController;
import cn.fcr.trigger.http.assembler.UserProfileAssembler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;

/**
 * 用户认证Controller
 *
 * <p>【DDD 触发层】处理用户注册、登录、微信绑定等认证相关接口。
 * 同时映射 /mall-api/v1/auth 和 /mall-api/v1/mall/user 两个路径。</p>
 *
 * @author 傅崇睿
 */
@Slf4j
@RestController
@CrossOrigin("${app.config.cross-origin}")
@RequestMapping({"/mall-api/${app.config.api-version}/auth", "/mall-api/${app.config.api-version}/mall/user"})
public class MallAuthController extends BaseController {

    /** 商城用户领域服务 */
    @Resource
    private IMallUserService mallUserService;

    /** 微信绑定服务 */
    @Resource
    private WeixinBindService weixinBindService;

    /** 登录领域服务 */
    @Resource
    private ILoginService loginService;

    /**
     * 用户注册
     *
     * <p>统一注册入口：仅传用户名密码为账密注册；同时携带 openId 则为
     * 微信注册（注册并绑定）。注册策略的判定在 Domain 层
     * {@link IMallUserService#register} 内完成（P0-6）。</p>
     *
     * @param request 注册请求，包含用户名、密码和可选的openId
     * @return 登录信息（含JWT token）
     */
    @PostMapping("/register")
    public Response<LoginRes> register(@RequestBody @Valid RegisterReq request) {
        log.info("用户注册请求: username={}, openId={}", request.getUsername(), request.getOpenId());

        UserLoginVO loginVO = mallUserService.register(
                request.getUsername(),
                request.getPassword(),
                request.getOpenId()
        );

        LoginRes result = new LoginRes();
        result.setToken(loginVO.getToken());
        result.setUserId(loginVO.getUserId());
        result.setUsername(loginVO.getUsername());
        result.setRole(loginVO.getRole());
        return success(result);
    }

    /**
     * 用户登录
     *
     * <p>验证用户名密码，成功返回JWT token</p>
     *
     * @param request 登录请求，包含用户名和密码
     * @return 登录信息（含JWT token）
     */
    @PostMapping("/login")
    public Response<LoginRes> login(@RequestBody @Valid LoginReq request) {
        log.info("用户登录请求: username={}", request.getUsername());
        UserLoginVO loginVO = mallUserService.login(request.getUsername(), request.getPassword());

        LoginRes result = new LoginRes();
        result.setToken(loginVO.getToken());
        result.setUserId(loginVO.getUserId());
        result.setUsername(loginVO.getUsername());
        result.setRole(loginVO.getRole());
        return success(result);
    }

    /**
     * 获取用户个人信息
     *
     * <p>【安全修复 2026-10-02】userId 不再从 query 参数获取——原实现存在 IDOR 越权，
     * 任意匿名请求方可通过遍历 userId 读取任意用户资料。现统一从 JWT 解析当前登录用户，
     * 与 ProfileController 行为一致。</p>
     *
     * @param httpRequest HTTP请求（用于提取JWT中的userId）
     * @return 当前登录用户个人信息
     */
    @GetMapping("/profile")
    public Response<UserProfileRes> getProfile(HttpServletRequest httpRequest) {
        Long userId = currentUserId(httpRequest);
        UserProfile profile = mallUserService.getProfile(userId);
        if (profile == null) {
            return fail("用户不存在");
        }
        return success(UserProfileAssembler.toVO(profile));
    }

    /**
     * 获取微信绑定二维码ticket
     *
     * <p>生成用于绑定的微信二维码ticket，前端使用此ticket拼接二维码图片URL：
     * https://mp.weixin.qq.com/cgi-bin/showqrcode?ticket={ticket}</p>
     *
     * @return 二维码ticket
     */
    @GetMapping("/bind/qrcode")
    public Response<String> generateBindQrCode() {
        try {
            String qrCodeTicket = loginService.createQrCodeTicket();
            weixinBindService.initBindStatus(qrCodeTicket);
            log.info("生成微信绑定二维码 ticket:{}", qrCodeTicket);
            return success(qrCodeTicket);
        } catch (Exception e) {
            log.error("生成微信绑定二维码失败", e);
            return fail("生成二维码失败");
        }
    }

    /**
     * 检查微信绑定状态
     *
     * <p>轮询检查用户是否已扫码完成绑定</p>
     *
     * @param ticket 二维码ticket
     * @return 绑定状态（BIND_SUCCESS / BINDING_PENDING / INVALID_CODE）
     */
    @GetMapping("/bind/status")
    public Response<UserBindStatusRes> checkBindStatus(String ticket) {
        String openId = weixinBindService.checkBindStatus(ticket);
        log.info("检查微信绑定状态 ticket:{} openId:{}", ticket, openId);

        UserBindStatusRes result = new UserBindStatusRes();
        if (openId != null) {
            result.setStatus("BIND_SUCCESS");
            result.setOpenId(openId);
        } else {
            String status = weixinBindService.getBindStatusRaw(ticket);
            if ("BINDING_PENDING".equals(status)) {
                result.setStatus("BINDING_PENDING");
            } else {
                result.setStatus("INVALID_CODE");
            }
        }
        return success(result);
    }

    /**
     * 确认微信绑定（账密登录用户绑定微信）
     *
     * <p>【TD-10 收尾】前端扫码轮询到 BIND_SUCCESS 后调用本端点完成落库：
     * userId 取自 JWT，openId 由服务端按 ticket 从缓存解析（不回传前端），
     * 绑定成功后销毁票据防止重复使用。绑定后该账户微信扫码/账密均可登录。</p>
     *
     * @param request     绑定确认请求，包含二维码票据
     * @param httpRequest HTTP请求（用于提取JWT中的userId）
     * @return 绑定结果
     */
    @PostMapping("/bind/confirm")
    public Response<String> confirmBind(@RequestBody @Valid UserBindConfirmReq request,
                                        HttpServletRequest httpRequest) {
        Long userId = currentUserId(httpRequest);
        String openId = weixinBindService.checkBindStatus(request.getTicket());
        if (openId == null) {
            return fail("二维码已过期或尚未完成扫码，请刷新二维码重试");
        }

        mallUserService.bindWeChat(userId, openId);
        // 绑定成功后销毁票据，防止重复使用
        weixinBindService.clearBindStatus(request.getTicket());
        log.info("微信绑定确认成功: userId={}", userId);
        return success("微信绑定成功");
    }

    /**
     * 设置/修改账户密码（微信扫码注册用户补设密码）
     *
     * <p>微信扫码自动注册的用户无账密，通过本端点设置密码后，
     * 该账户同时支持微信扫码与账密两种登录方式（状态转为正常）。</p>
     *
     * @param request     密码设置请求，包含新密码
     * @param httpRequest HTTP请求（用于提取JWT中的userId）
     * @return 设置结果
     */
    @PostMapping("/password")
    public Response<String> setPassword(@RequestBody @Valid UserPasswordSetReq request,
                                        HttpServletRequest httpRequest) {
        Long userId = currentUserId(httpRequest);
        mallUserService.setPassword(userId, request.getPassword());
        log.info("用户设置密码成功: userId={}", userId);
        return success("密码设置成功");
    }
}
