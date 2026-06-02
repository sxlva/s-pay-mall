package cn.fcr.infrastructure.adapter.port;

import cn.fcr.domain.auth.adapter.port.ILoginPort;
import cn.fcr.infrastructure.gateway.IWeixinApiService;
import cn.fcr.infrastructure.gateway.dto.WeixinQrCodeRequestDTO;
import cn.fcr.infrastructure.gateway.dto.WeixinQrCodeResponseDTO;
import cn.fcr.infrastructure.gateway.dto.WeixinTemplateMessageDTO;
import cn.fcr.infrastructure.gateway.dto.WeixinTokenResponseDTO;
import com.google.common.cache.Cache;
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
 * @date 2025/7/26 15:11
 * @description 微信登陆外部接口实现类
 */
@Service
public class LoginPort implements ILoginPort {
    @Value("${weixin.config.app-id}")
    private String appid;
    @Value("${weixin.config.app-secret}")
    private String appSecret;
    @Value("${weixin.config.template_id}")
    private String template_id;
    @Value("${weixin.config.template_id_pay_success}")
    private String templateIdPaySuccess;

    @Qualifier("weixinAccessToken")
    @Resource
    private Cache<String, String> weixinAccessToken;
    @Resource
    private IWeixinApiService weixinApiService;

    @Override
    public String createQrCodeTicket() throws IOException {
        // 1. 获取 accessToken
        String accessToken = weixinAccessToken.getIfPresent(appid);
        if (null == accessToken) {
            Call<WeixinTokenResponseDTO> call = weixinApiService.getToken("client_credential", appid, appSecret);
            WeixinTokenResponseDTO weixinTokenRes = call.execute().body();
            assert weixinTokenRes != null;
            accessToken = weixinTokenRes.getAccess_token();
            weixinAccessToken.put(appid, accessToken);
        }

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
        assert null != weixinQrCodeRes;
        return weixinQrCodeRes.getTicket();

    }

    @Override
    public void sendLoginTemplate(String openid) throws IOException {
        // 1. 获取 accessToken 【实际业务场景，按需处理下异常】
        String accessToken = weixinAccessToken.getIfPresent(appid);
        if (null == accessToken){
            Call<WeixinTokenResponseDTO> call = weixinApiService.getToken("client_credential", appid, appSecret);
            WeixinTokenResponseDTO weixinTokenRes = call.execute().body();
            assert weixinTokenRes != null;
            accessToken = weixinTokenRes.getAccess_token();
            weixinAccessToken.put(appid, accessToken);
        }

        // 2. 发送模板消息
        Map<String, Map<String, String>> data = new HashMap<>();
        WeixinTemplateMessageDTO.put(data, WeixinTemplateMessageDTO.TemplateKey.USER, openid);

        WeixinTemplateMessageDTO templateMessageDTO = new WeixinTemplateMessageDTO(openid, template_id);
        templateMessageDTO.setUrl("https://fuchongrui.me");
        templateMessageDTO.setData(data);

        Call<Void> call = weixinApiService.sendMessage(accessToken, templateMessageDTO);
        call.execute();
    }

    public void sendPaySuccessTemplate(String openid, String productName, String orderId, String amount, String payTime) throws IOException {
        String accessToken = weixinAccessToken.getIfPresent(appid);
        if (null == accessToken) {
            Call<WeixinTokenResponseDTO> call = weixinApiService.getToken("client_credential", appid, appSecret);
            WeixinTokenResponseDTO weixinTokenRes = call.execute().body();
            assert weixinTokenRes != null;
            accessToken = weixinTokenRes.getAccess_token();
            weixinAccessToken.put(appid, accessToken);
        }

        Map<String, Map<String, String>> data = new HashMap<>();
        WeixinTemplateMessageDTO.put(data, WeixinTemplateMessageDTO.TemplateKey.PRODUCT, productName);
        WeixinTemplateMessageDTO.put(data, WeixinTemplateMessageDTO.TemplateKey.ORDER_ID, orderId);
        WeixinTemplateMessageDTO.put(data, WeixinTemplateMessageDTO.TemplateKey.AMOUNT, amount);
        WeixinTemplateMessageDTO.put(data, WeixinTemplateMessageDTO.TemplateKey.PAY_TIME, payTime);

        WeixinTemplateMessageDTO templateMessageDTO = new WeixinTemplateMessageDTO(openid, templateIdPaySuccess);
        templateMessageDTO.setData(data);

        Call<Void> call = weixinApiService.sendMessage(accessToken, templateMessageDTO);
        call.execute();
    }
}
