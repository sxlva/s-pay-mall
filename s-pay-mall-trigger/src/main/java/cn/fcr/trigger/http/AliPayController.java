package cn.fcr.trigger.http;

import cn.fcr.api.dto.CreatePayRequestDTO;
import cn.fcr.api.response.Response;
import cn.fcr.domain.shared.model.entity.PayOrderEntity;
import cn.fcr.trigger.application.OrderApplicationService;
import cn.fcr.types.common.Constants;
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
public class AliPayController {

    /** 支付宝公钥，用于支付回调验签 */
    @Value("${alipay.alipay_public_key}")
    private String alipayPublicKey;

    /** 订单应用层服务 */
    @Resource
    private OrderApplicationService orderApplicationService;

    /**
     * 创建支付订单
     *
     * <p>根据商品ID生成支付宝支付单，返回支付页面的跳转URL</p>
     *
     * @param createPayRequestDTO 创建支付请求，包含 userId 和 productId
     * @return 支付URL
     */
    @RequestMapping(value = "create_pay_order", method = RequestMethod.POST)
    public Response<String> createPayOrder(@RequestBody CreatePayRequestDTO createPayRequestDTO) {
        try {
            log.info("商品下单，根据商品ID创建支付单开始 userId:{} productId:{}",
                    createPayRequestDTO.getUserId(), createPayRequestDTO.getProductId());

            String userId = createPayRequestDTO.getUserId();
            String productId = createPayRequestDTO.getProductId();

            PayOrderEntity payOrderEntity = orderApplicationService.createPayOrder(userId, productId);

            log.info("商品下单，根据商品ID创建支付单完成 userId:{} productId:{} orderNo:{}",
                    userId, productId, payOrderEntity.getOrderNo());

            return Response.<String>builder()
                    .code(Constants.ResponseCode.SUCCESS.getCode())
                    .info(Constants.ResponseCode.SUCCESS.getInfo())
                    .data(payOrderEntity.getPayUrl())
                    .build();
        } catch (Exception e) {
            log.error("商品下单，根据商品ID创建支付单失败 userId:{} productId:{}",
                    createPayRequestDTO.getUserId(), createPayRequestDTO.getProductId(), e);
            return Response.<String>builder()
                    .code(Constants.ResponseCode.UN_ERROR.getCode())
                    .info(Constants.ResponseCode.UN_ERROR.getInfo())
                    .build();
        }
    }

    /**
     * 支付宝支付异步回调通知
     *
     * <p>接收支付宝服务器发送的支付结果通知，验签后更新订单支付状态</p>
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

        if (!"TRADE_SUCCESS".equals(request.getParameter("trade_status"))
                && !"TRADE_FINISHED".equals(request.getParameter("trade_status"))) {
            return "false";
        }

        Map<String, String> params = extractParams(request);

        boolean signVerified = orderApplicationService.verifyPayCallbackSign(params, alipayPublicKey);
        if (!signVerified) {
            log.error("支付回调，签名验证失败。公钥长度: {}, 公钥前100字符: {}, 接收参数: {}",
                    alipayPublicKey != null ? alipayPublicKey.length() : 0,
                    alipayPublicKey != null && alipayPublicKey.length() > 100 ? alipayPublicKey.substring(0, 100) : alipayPublicKey,
                    params);
            return "false";
        }

        String tradeNo = params.get("out_trade_no");
        log.info("支付回调，验签通过，交易名称: {}, 商户订单号: {}, 交易金额: {}",
                params.get("subject"), tradeNo, params.get("total_amount"));

        orderApplicationService.changeOrderPaySuccess(tradeNo);

        return "success";
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
