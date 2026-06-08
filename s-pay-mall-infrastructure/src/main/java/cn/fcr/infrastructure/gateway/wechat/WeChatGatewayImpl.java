package cn.fcr.infrastructure.gateway.wechat;

import cn.fcr.domain.auth.gateway.IWeChatGateway;
import cn.fcr.infrastructure.gateway.IWeixinApiService;
import cn.fcr.infrastructure.gateway.dto.WeixinQrCodeRequestDTO;
import cn.fcr.infrastructure.gateway.dto.WeixinQrCodeResponseDTO;
import cn.fcr.infrastructure.gateway.dto.WeixinTemplateMessageDTO;
import cn.fcr.infrastructure.gateway.dto.WeixinTokenResponseDTO;
import com.google.common.cache.Cache;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import retrofit2.Call;

import javax.annotation.Resource;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * @author 傅崇睿
 * @date 2025/7/26
 * @description 微信网关实现 - 处理微信 API 调用和技术细节
 */
@Slf4j
@Service
public class WeChatGatewayImpl implements IWeChatGateway {

    @Value("${weixin.config.app-id}")
    private String appid;

    @Value("${weixin.config.app-secret}")
    private String appSecret;

    @Value("${weixin.config.template_id}")
    private String templateId;

    @Value("${weixin.config.template_id_pay_success}")
    private String templateIdPaySuccess;

    @Qualifier("weixinAccessToken")
    @Resource
    private Cache<String, String> weixinAccessToken;

    @Resource
    private IWeixinApiService weixinApiService;

    @Override
    public String createQrCodeTicket() {
        try {
            // 1. 获取 accessToken
            String accessToken = getAccessToken();

            // 2. 生成 ticket
            WeixinQrCodeRequestDTO weixinQrCodeReq = WeixinQrCodeRequestDTO.builder()
                    .expire_seconds(2592000)
                    .action_name(WeixinQrCodeRequestDTO.ActionNameTypeVO.QR_SCENE.getCode())
                    .action_info(WeixinQrCodeRequestDTO.ActionInfo.builder()
                            .scene(WeixinQrCodeRequestDTO.ActionInfo.Scene.builder()
                                    .scene_id(100601)
                                    .build())
                            .build())
                    .build();

            Call<WeixinQrCodeResponseDTO> call = weixinApiService.createQrCode(accessToken, weixinQrCodeReq);
            WeixinQrCodeResponseDTO weixinQrCodeRes = call.execute().body();

            if (weixinQrCodeRes == null) {
                throw new RuntimeException("创建二维码失败：微信 API 返回为空");
            }

            return weixinQrCodeRes.getTicket();
        } catch (IOException e) {
            log.error("创建微信二维码票据失败", e);
            throw new RuntimeException("创建微信二维码票据失败", e);
        }
    }

    @Override
    public void sendLoginNotification(String openid) {
        try {
            // 1. 获取 accessToken
            String accessToken = getAccessToken();

            // 2. 发送模板消息
            Map<String, Map<String, String>> data = new HashMap<>();
            WeixinTemplateMessageDTO.put(data, WeixinTemplateMessageDTO.TemplateKey.USER, openid);

            WeixinTemplateMessageDTO templateMessageDTO = new WeixinTemplateMessageDTO(openid, templateId);
            templateMessageDTO.setUrl("https://fuchongrui.me");
            templateMessageDTO.setData(data);

            Call<Void> call = weixinApiService.sendMessage(accessToken, templateMessageDTO);
            call.execute();

            log.info("发送登录通知成功，openid: {}", openid);
        } catch (IOException e) {
            log.error("发送登录通知失败，openid: {}", openid, e);
            throw new RuntimeException("发送登录通知失败", e);
        }
    }

    @Override
    public void sendPaymentSuccessNotification(String openid, String productName, String orderId, String amount, String payTime) {
        try {
            // 1. 获取 accessToken
            String accessToken = getAccessToken();

            // 2. 发送支付成功模板消息
            Map<String, Map<String, String>> data = new HashMap<>();
            WeixinTemplateMessageDTO.put(data, WeixinTemplateMessageDTO.TemplateKey.PRODUCT, productName);
            WeixinTemplateMessageDTO.put(data, WeixinTemplateMessageDTO.TemplateKey.ORDER_ID, orderId);
            WeixinTemplateMessageDTO.put(data, WeixinTemplateMessageDTO.TemplateKey.AMOUNT, amount);
            WeixinTemplateMessageDTO.put(data, WeixinTemplateMessageDTO.TemplateKey.PAY_TIME, payTime);

            WeixinTemplateMessageDTO templateMessageDTO = new WeixinTemplateMessageDTO(openid, templateIdPaySuccess);
            templateMessageDTO.setData(data);

            Call<Void> call = weixinApiService.sendMessage(accessToken, templateMessageDTO);
            call.execute();

            log.info("发送支付成功通知成功，openid: {}, 商品: {}, 订单号: {}", openid, productName, orderId);
        } catch (IOException e) {
            log.error("发送支付成功通知失败，openid: {}, 订单号: {}", openid, orderId, e);
            throw new RuntimeException("发送支付成功通知失败", e);
        }
    }

    /**
     * 获取微信 access token
     *
     * @return access token
     * @throws IOException IO 异常
     */
    private String getAccessToken() throws IOException {
        String accessToken = weixinAccessToken.getIfPresent(appid);
        if (accessToken == null) {
            Call<WeixinTokenResponseDTO> call = weixinApiService.getToken("client_credential", appid, appSecret);
            WeixinTokenResponseDTO weixinTokenRes = call.execute().body();

            if (weixinTokenRes == null) {
                throw new RuntimeException("获取微信 access token 失败：微信 API 返回为空");
            }

            accessToken = weixinTokenRes.getAccess_token();
            weixinAccessToken.put(appid, accessToken);
        }
        return accessToken;
    }
}