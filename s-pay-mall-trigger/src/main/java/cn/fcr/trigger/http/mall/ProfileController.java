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

/**
 * 用户个人信息Controller
 *
 * <p>【DDD 触发层】处理用户个人信息查询，通过JWT自动识别当前用户。</p>
 *
 * @author 傅崇睿
 */
@Slf4j
@RestController
@CrossOrigin("${app.config.cross-origin}")
@RequestMapping("/mall-api/${app.config.api-version}")
public class ProfileController extends BaseController {

    /** 商城用户领域服务 */
    @Resource
    private IMallUserService mallUserService;

    /**
     * 获取当前用户个人信息
     *
     * @param httpRequest HTTP请求（用于提取JWT中的userId）
     * @return 用户个人信息
     */
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
