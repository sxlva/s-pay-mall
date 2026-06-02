package cn.fcr.trigger.http.mall;

import cn.fcr.api.IMallAuthService;
import cn.fcr.api.dto.UserLoginRequest;
import cn.fcr.api.dto.UserRegisterRequest;
import cn.fcr.api.response.Response;
import cn.fcr.domain.auth.service.WeixinBindService;
import cn.fcr.domain.mall.model.valobj.UserLoginVO;
import cn.fcr.domain.mall.service.IMallUserService;
import cn.fcr.trigger.http.BaseController;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 用户认证控制器
 * 实现 IMallAuthService 接口，处理用户注册、登录等认证相关接口
 */
@Slf4j
@RestController
@CrossOrigin("${app.config.cross-origin}")
@RequestMapping({"/mall-api/${app.config.api-version}/auth", "/mall-api/${app.config.api-version}/mall/user"})
public class MallAuthController extends BaseController implements IMallAuthService {

    @Resource
    private IMallUserService mallUserService;

    @Resource
    private WeixinBindService weixinBindService;

    @Override
    @PostMapping("/register")
    public Response<Map<String, Object>> register(@RequestBody UserRegisterRequest request) {
        log.info("用户注册请求: username={}, openId={}", request.getUsername(), request.getOpenId());

        UserLoginVO loginVO;
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

        Map<String, Object> result = new HashMap<>();
        result.put("token", loginVO.getToken());
        result.put("userId", loginVO.getUserId());
        result.put("username", loginVO.getUsername());
        result.put("role", loginVO.getRole());
        return success(result);
    }

    @Override
    @PostMapping("/login")
    public Response<Map<String, Object>> login(@RequestBody UserLoginRequest request) {
        log.info("用户登录请求: username={}", request.getUsername());
        UserLoginVO loginVO = mallUserService.login(request.getUsername(), request.getPassword());

        Map<String, Object> result = new HashMap<>();
        result.put("token", loginVO.getToken());
        result.put("userId", loginVO.getUserId());
        result.put("username", loginVO.getUsername());
        result.put("role", loginVO.getRole());
        return success(result);
    }

    @Override
    @GetMapping("/profile")
    public Response<Map<String, Object>> getProfile(Long userId) {
        Map<String, Object> profile = mallUserService.getProfile(userId);
        return success(profile);
    }

    @GetMapping("/bind/code")
    public Response<Map<String, String>> generateBindCode() {
        String ticket = UUID.randomUUID().toString().replace("-", "").substring(0, 8);
        weixinBindService.initBindStatus(ticket);
        log.info("生成微信绑定码: {}", ticket);

        Map<String, String> result = new HashMap<>();
        result.put("bindCode", ticket);
        return success(result);
    }

    @GetMapping("/bind/status")
    public Response<Map<String, Object>> checkBindStatus(String ticket) {
        String openId = weixinBindService.checkBindStatus(ticket);
        log.info("检查微信绑定状态 ticket:{} openId:{}", ticket, openId);

        Map<String, Object> result = new HashMap<>();
        if (openId != null) {
            result.put("status", "BIND_SUCCESS");
            result.put("openId", openId);
        } else {
            String status = weixinBindService.getBindStatusRaw(ticket);
            if ("BINDING_PENDING".equals(status)) {
                result.put("status", "BINDING_PENDING");
            } else {
                result.put("status", "INVALID_CODE");
            }
        }
        return success(result);
    }
}