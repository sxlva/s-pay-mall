package cn.fcr.trigger.http.mall;

import cn.fcr.api.response.Response;
import cn.fcr.domain.mall.service.IMallUserService;
import cn.fcr.trigger.http.BaseController;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.Map;

@Slf4j
@RestController
@CrossOrigin("${app.config.cross-origin}")
@RequestMapping("/mall-api/${app.config.api-version}")
public class ProfileController extends BaseController {

    @Resource
    private IMallUserService mallUserService;

    @GetMapping("/profile")
    public Response<Map<String, Object>> getProfile(HttpServletRequest httpRequest) {
        Long userId = currentUserId(httpRequest);
        log.info("获取用户个人信息: userId={}", userId);
        Map<String, Object> profile = mallUserService.getProfile(userId);
        return success(profile);
    }
}