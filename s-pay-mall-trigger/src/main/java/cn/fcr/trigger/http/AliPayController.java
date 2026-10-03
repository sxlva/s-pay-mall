package cn.fcr.trigger.http;

import cn.fcr.application.OrderApplicationService;
import cn.fcr.trigger.http.BaseController;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.Map;

/**
 * 支付宝支付Controller
 *
 * @author 傅崇睿
 */
@Slf4j
@RestController()
@CrossOrigin("${app.config.cross-origin}")
@RequestMapping("/pay-api/${app.config.api-version}/alipay/")
public class AliPayController extends BaseController {

    /** 支付宝公钥，用于支付回调验签 */
    @Value("${alipay.alipay_public_key}")
    private String alipayPublicKey;

    /** 订单应用层服务 */
    @Resource
    private OrderApplicationService orderApplicationService;

    /**
     * 支付宝支付异步回调通知
     *
     * <p>接收支付宝服务器发送的支付结果通知。Controller 只做协议适配：
     * 解析参数并委托应用层 {@link OrderApplicationService#handleAlipayCallback}
     * 完成状态判断、验签与履约（P0-6，业务规则已下沉，本类无 trade_status 判断）。</p>
     *
     * @param request HTTP请求，包含支付宝POST过来的回调参数
     * @return "success" 表示处理成功，"false" 表示处理失败
     */
    @RequestMapping(value = "alipay_notify_url", method = RequestMethod.POST)
    public String payNotify(HttpServletRequest request) {
        try {
            request.setCharacterEncoding("UTF-8");
        } catch (Exception e) {
            log.warn("设置请求编码失败", e);
        }

        log.info("支付回调，消息接收 trade_status:{}", request.getParameter("trade_status"));

        Map<String, String> params = extractParams(request);
        boolean accepted = orderApplicationService.handleAlipayCallback(params, alipayPublicKey);
        return accepted ? "success" : "false";
    }

    /**
     * 从 HttpServletRequest 提取所有参数为 Map
     *
     * @param request HTTP请求
     * @return 参数 key-value Map
     */
    private Map<String, String> extractParams(HttpServletRequest request) {
        Map<String, String[]> requestParams = request.getParameterMap();
        Map<String, String> params = new HashMap<>();
        for (String name : requestParams.keySet()) {
            params.put(name, request.getParameter(name));
        }
        return params;
    }
}
