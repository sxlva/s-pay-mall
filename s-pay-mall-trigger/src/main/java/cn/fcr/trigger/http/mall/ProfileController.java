package cn.fcr.trigger.http.mall;

import cn.fcr.api.response.Response;
import cn.fcr.api.vo.UserProfileVO;
import cn.fcr.domain.mall.model.valobj.UserProfile;
import cn.fcr.domain.mall.service.IMallUserService;
import cn.fcr.trigger.http.BaseController;
import cn.fcr.trigger.http.assembler.UserProfileAssembler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;

@Slf4j
@RestController
@CrossOrigin("${app.config.cross-origin}")
@RequestMapping("/mall-api/${app.config.api-version}")
public class ProfileController extends BaseController {

    @Resource
    private IMallUserService mallUserService;

    @GetMapping("/profile")
    public Response<UserProfileVO> getProfile(HttpServletRequest httpRequest) {
        Long userId = currentUserId(httpRequest);
        log.info("获取用户个人信息: userId={}", userId);
        UserProfile profile = mallUserService.getProfile(userId);
        if (profile == null) {
            return fail("用户不存在");
        }
        return success(UserProfileAssembler.toVO(profile));
    }
}
