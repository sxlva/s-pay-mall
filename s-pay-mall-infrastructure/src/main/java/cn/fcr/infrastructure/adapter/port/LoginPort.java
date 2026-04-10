package cn.fcr.infrastructure.adapter.port;

import cn.fcr.domain.auth.adapter.port.ILoginPort;
import cn.fcr.infrastructure.gateway.IWeixinApiService;
import cn.fcr.infrastructure.gateway.dto.WeixinQrCodeRequestDTO;
import cn.fcr.infrastructure.gateway.dto.WeixinQrCodeResponseDTO;
import cn.fcr.infrastructure.gateway.dto.WeixinTemplateMessageDTO;
import cn.fcr.infrastructure.gateway.dto.WeixinTokenResponseDTO;
import cn.fcr.infrastructure.redis.IRedisService;
import cn.fcr.types.common.Constants;

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

    @Resource
    private IWeixinApiService weixinApiService;
    @Resource
    private IRedisService redisService;

    /**
     * 生成微信扫码登录 ticket
     * @return ticket 登录凭证
     */
    @Override
    public String createQrCodeTicket() throws IOException {
        String cacheKey = Constants.RedisKey.WEIXIN_ACCESS_TOKEN_KEY_PREFIX + appid;
        // 1. 获取 access_token
        String accessToken = redisService.getValue(cacheKey);
        if (null == accessToken) {
            Call<WeixinTokenResponseDTO> call = weixinApiService.getToken("client_credential", appid, appSecret);
            WeixinTokenResponseDTO weixinTokenRes = call.execute().body();
            assert weixinTokenRes != null;
            accessToken = weixinTokenRes.getAccess_token();
            redisService.setValue(cacheKey, accessToken, Constants.RedisKey.ACCESS_TOKEN_EXPIRE_TIME);
        }
        
        // 2. 生成 ticket
        WeixinQrCodeRequestDTO weixinQrCodeReq = WeixinQrCodeRequestDTO.builder()
                .expire_seconds((int) Constants.RedisKey.ACCESS_TOKEN_EXPIRE_TIME)
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

    /**
     * 发送登录模板消息
     * @param openid 用户ID
     */
    @Override
    public void sendLoginTemplate(String openid) throws IOException {
        String cacheKey = Constants.RedisKey.WEIXIN_ACCESS_TOKEN_KEY_PREFIX + appid;
        
        String accessToken = redisService.getValue(cacheKey);
        if (null == accessToken){
            Call<WeixinTokenResponseDTO> call = weixinApiService.getToken("client_credential", appid, appSecret);
            WeixinTokenResponseDTO weixinTokenRes = call.execute().body();
            assert weixinTokenRes != null;
            accessToken = weixinTokenRes.getAccess_token();
            redisService.setValue(cacheKey, accessToken, Constants.RedisKey.ACCESS_TOKEN_EXPIRE_TIME);
        }

        // 2. 发送模板消息
        Map<String, Map<String, String>> data = new HashMap<>();
        WeixinTemplateMessageDTO.put(data, WeixinTemplateMessageDTO.TemplateKey.USER, openid);

        WeixinTemplateMessageDTO templateMessageDTO = new WeixinTemplateMessageDTO(openid, template_id);
        templateMessageDTO.setUrl("https://fuchongrui.site");
        templateMessageDTO.setData(data);

        Call<Void> call = weixinApiService.sendMessage(accessToken, templateMessageDTO);
        call.execute();
    }

    /**
     * 保存登录状态
     * @param ticket 登录凭证
     * @param openid 用户ID
     */
    @Override
    public void saveLoginState(String ticket, String openid) {
        String cacheKey = Constants.RedisKey.WEIXIN_LOGIN_STATE_KEY_PREFIX + ticket;
        redisService.setValue(cacheKey, openid, Constants.RedisKey.LOGIN_STATE_EXPIRE_TIME);
    }

    /**
     * 检查登录状态
     * @param ticket 登录凭证
     * @return openid 用户ID
     */
    @Override
    public String checkLogin(String ticket) {
        String cacheKey = Constants.RedisKey.WEIXIN_LOGIN_STATE_KEY_PREFIX + ticket;
        return redisService.getValue(cacheKey);
    }

}
