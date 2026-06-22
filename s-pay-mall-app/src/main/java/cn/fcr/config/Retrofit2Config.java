package cn.fcr.config;

import cn.fcr.infrastructure.auth.gateway.IWeixinApiService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import retrofit2.Retrofit;
import retrofit2.converter.jackson.JacksonConverterFactory;

/**
 * Retrofit2 HTTP 客户端配置
 *
 * <p>配置微信 API 的 Retrofit 客户端，baseUrl 为微信 API 域名。</p>
 *
 * @author 傅崇睿
 */
@Slf4j
@Configuration
public class Retrofit2Config {

    /** 微信 API 基础URL */
    private static final String BASE_URL = "https://api.weixin.qq.com/";

    /**
     * 创建 Retrofit 实例
     *
     * @return Retrofit 实例，使用 Jackson 序列化
     */
    @Bean
    public Retrofit retrofit() {
        return new Retrofit.Builder()
                .baseUrl(BASE_URL)
                .addConverterFactory(JacksonConverterFactory.create()).build();
    }

    /**
     * 创建微信 API 服务代理
     *
     * @param retrofit Retrofit 实例
     * @return IWeixinApiService 代理
     */
    @Bean
    public IWeixinApiService wexinApiService(Retrofit retrofit) {
        return retrofit.create(IWeixinApiService.class);
    }

}
