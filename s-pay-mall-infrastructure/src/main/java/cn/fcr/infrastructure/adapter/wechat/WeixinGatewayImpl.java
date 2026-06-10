package cn.fcr.infrastructure.adapter.wechat;

import cn.fcr.domain.auth.gateway.IWeChatGateway;
import cn.fcr.infrastructure.adapter.wechat.dto.WeixinQrCodeRequestDTO;
import cn.fcr.infrastructure.adapter.wechat.dto.WeixinQrCodeResponseDTO;
import cn.fcr.infrastructure.adapter.wechat.dto.WeixinTemplateMessageDTO;
import cn.fcr.infrastructure.adapter.wechat.dto.WeixinTokenResponseDTO;
import cn.fcr.types.common.Constants;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import retrofit2.Call;

import javax.annotation.Resource;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * @description 微信网关实现类
 * 
 * 【职责说明】
 * - 封装微信公众号API的调用逻辑
 * - 实现领域层定义的IWeChatGateway接口
 * - 负责获取Access Token、创建二维码、发送模板消息等操作
 * 
 * 【核心功能】
 * 1. createQrCodeTicket(): 生成微信二维码票据
 * 2. sendLoginNotification(): 发送登录通知模板消息
 * 3. sendPaymentSuccessNotification(): 发送支付成功通知模板消息
 * 
 * 【依赖说明】
 * - IWeixinApiService: Retrofit2声明式接口，用于HTTP调用
 * - Cache: Guava缓存，用于缓存Access Token
 * - 配置文件: 微信app-id、app-secret、template_id等
 */
@Slf4j
@Component
public class WeixinGatewayImpl implements IWeChatGateway {

    @Value("${weixin.config.app-id}")
    private String appid;

    @Value("${weixin.config.app-secret}")
    private String appSecret;

    @Value("${weixin.config.template_id}")
    private String templateId;

    @Value("${weixin.config.template_id_pay_success}")
    private String templateIdPaySuccess;

    @Resource
    private StringRedisTemplate stringRedisTemplate;

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
                                    .scene_id(100601L)
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
        String key = Constants.REDIS_WECHAT_ACCESS_TOKEN_PREFIX + appid;
        String accessToken = stringRedisTemplate.opsForValue().get(key);
        if (accessToken == null) {
            Call<WeixinTokenResponseDTO> call = weixinApiService.getToken("client_credential", appid, appSecret);
            WeixinTokenResponseDTO weixinTokenRes = call.execute().body();

            if (weixinTokenRes == null) {
                throw new RuntimeException("获取微信 access token 失败：微信 API 返回为空");
            }

            accessToken = weixinTokenRes.getAccess_token();
            // 微信 access_token 默认有效期 7200 秒，这里设置 110 分钟提前续期
            stringRedisTemplate.opsForValue().set(key, accessToken, 110, TimeUnit.MINUTES);
            log.info("从微信 API 获取并缓存 access token");
        }
        return accessToken;
    }
}