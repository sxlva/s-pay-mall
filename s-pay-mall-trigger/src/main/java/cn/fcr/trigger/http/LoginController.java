package cn.fcr.trigger.http;

import cn.fcr.api.IAuthService;
import cn.fcr.api.response.Response;
import cn.fcr.domain.auth.service.ILoginService;
import cn.fcr.types.common.Constants;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;

/**
 * @author fcr
 * @description 登录认证控制器
 * 
 * <p>该控制器负责处理用户登录相关的API请求，主要包含两种登录方式：
 * <ul>
 *   <li><strong>传统账号密码登录</strong>：通过 MallApiController 的 /mall-api/v1/auth/login 接口实现</li>
 *   <li><strong>微信扫码登录</strong>：通过本控制器的接口实现，包含获取二维码ticket和轮询登录状态</li>
 * </ul>
 */
@Slf4j
@RestController()
@CrossOrigin("${app.config.cross-origin}")
@RequestMapping("/pay-api/${app.config.api-version}/login/")
public class LoginController implements IAuthService {

    @Resource
    private ILoginService loginService;

    /**
     * 获取微信扫码登录二维码ticket
     * 
     * <p><strong>微信扫码登录流程第一步</strong>：前端调用此接口获取二维码ticket，
     * 然后使用ticket拼接微信二维码图片URL：https://mp.weixin.qq.com/cgi-bin/showqrcode?ticket={ticket}
     * 
     * @return Response 包含二维码ticket的响应对象
     */
    @RequestMapping(value = "weixin_qrcode_ticket", method = RequestMethod.GET)
    @Override
    public Response<String> weixinQrCodeTicket() {
        try {
            // 调用领域服务创建二维码ticket
            String qrCodeTicket = loginService.createQrCodeTicket();
            log.info("生成微信扫码登录 ticket:{}", qrCodeTicket);
            return Response.<String>builder()
                    .code(Constants.ResponseCode.SUCCESS.getCode())
                    .info(Constants.ResponseCode.SUCCESS.getInfo())
                    .data(qrCodeTicket)
                    .build();
        } catch (Exception e) {
            log.error("生成微信扫码登录 ticket 失败", e);
            return Response.<String>builder()
                    .code(Constants.ResponseCode.UN_ERROR.getCode())
                    .info(Constants.ResponseCode.UN_ERROR.getInfo())
                    .build();
        }
    }

    /**
     * 轮询检查微信扫码登录状态
     * 
     * <p><strong>微信扫码登录流程第二步</strong>：前端获取二维码后，每隔3秒调用此接口轮询登录状态。
     * 如果用户已扫码并确认登录，返回openidToken（JWT token）；否则返回未登录状态。
     * 
     * @param ticket 二维码ticket，用于标识当前登录会话
     * @return Response 包含登录状态的响应对象，成功时data字段为JWT token
     */
    @RequestMapping(value = "check_login", method = RequestMethod.GET)
    @Override
    public Response<String> checkLogin(String ticket) {
        try {
            // 调用领域服务检查登录状态
            String openidToken = loginService.checkLogin(ticket);
            log.info("扫码检测登录结果 ticket:{} openidToken:{}", ticket, openidToken);
            
            // 如果获取到token，说明登录成功
            if (StringUtils.isNotBlank(openidToken)) {
                return Response.<String>builder()
                        .code(Constants.ResponseCode.SUCCESS.getCode())
                        .info(Constants.ResponseCode.SUCCESS.getInfo())
                        .data(openidToken)
                        .build();
            } else {
                // 未登录，返回未登录状态码
                return Response.<String>builder()
                        .code(Constants.ResponseCode.NO_LOGIN.getCode())
                        .info(Constants.ResponseCode.NO_LOGIN.getInfo())
                        .build();
            }
        } catch (Exception e) {
            log.error("扫码检测登录结果失败 ticket:{}", ticket, e);
            return Response.<String>builder()
                    .code(Constants.ResponseCode.UN_ERROR.getCode())
                    .info(Constants.ResponseCode.UN_ERROR.getInfo())
                    .build();
        }
    }

}
