package cn.fcr.trigger.http.mall;

import cn.fcr.api.dto.UserLoginRequestDTO;
import cn.fcr.api.dto.UserRegisterRequestDTO;
import cn.fcr.api.response.Response;
import cn.fcr.api.vo.BindStatusVO;
import cn.fcr.api.vo.UserLoginVO;
import cn.fcr.api.vo.UserProfileVO;
import cn.fcr.domain.auth.service.ILoginService;
import cn.fcr.domain.auth.service.WeixinBindService;
import cn.fcr.domain.mall.model.valobj.UserProfile;
import cn.fcr.domain.mall.service.IMallUserService;
import cn.fcr.trigger.http.BaseController;
import cn.fcr.trigger.http.assembler.UserProfileAssembler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;

/**
 * 用户认证控制器
 * 处理用户注册、登录等认证相关接口
 */
@Slf4j
@RestController
@CrossOrigin("${app.config.cross-origin}")
@RequestMapping({"/mall-api/${app.config.api-version}/auth", "/mall-api/${app.config.api-version}/mall/user"})
public class MallAuthController extends BaseController {

    @Resource
    private IMallUserService mallUserService;

    @Resource
    private WeixinBindService weixinBindService;

    @Resource
    private ILoginService loginService;

    /**
     * 用户注册
     * 支持普通账号注册和微信扫码注册
     */
    @PostMapping("/register")
    public Response<UserLoginVO> register(@RequestBody UserRegisterRequestDTO request) {
        log.info("用户注册请求: username={}, openId={}", request.getUsername(), request.getOpenId());

        cn.fcr.domain.mall.model.valobj.UserLoginVO loginVO;
        if (request.getOpenId() != null && !request.getOpenId().isBlank()) {
            loginVO = mallUserService.registerWithWeChat(
                    request.getUsername(),
                    request.getPassword(),
                    request.getOpenId()
            );
        } else {
            loginVO = mallUserService.register(
                    request.getUsername(),
                    request.getPassword()
            );
        }

        UserLoginVO result = new UserLoginVO();
        result.setToken(loginVO.getToken());
        result.setUserId(loginVO.getUserId());
        result.setUsername(loginVO.getUsername());
        result.setRole(loginVO.getRole());
        return success(result);
    }

    /**
     * 用户登录
     * 验证用户名密码，成功返回JWT token
     */
    @PostMapping("/login")
    public Response<UserLoginVO> login(@RequestBody UserLoginRequestDTO request) {
        log.info("用户登录请求: username={}", request.getUsername());
        cn.fcr.domain.mall.model.valobj.UserLoginVO loginVO = mallUserService.login(request.getUsername(), request.getPassword());

        UserLoginVO result = new UserLoginVO();
        result.setToken(loginVO.getToken());
        result.setUserId(loginVO.getUserId());
        result.setUsername(loginVO.getUsername());
        result.setRole(loginVO.getRole());
        return success(result);
    }

    /**
     * 获取用户个人信息
     */
    @GetMapping("/profile")
    public Response<UserProfileVO> getProfile(Long userId) {
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
     * https://mp.weixin.qq.com/cgi-bin/showqrcode?ticket={ticket}
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
     * <p>轮询检查用户是否已扫码完成绑定
     */
    @GetMapping("/bind/status")
    public Response<BindStatusVO> checkBindStatus(String ticket) {
        String openId = weixinBindService.checkBindStatus(ticket);
        log.info("检查微信绑定状态 ticket:{} openId:{}", ticket, openId);

        BindStatusVO result = new BindStatusVO();
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
}