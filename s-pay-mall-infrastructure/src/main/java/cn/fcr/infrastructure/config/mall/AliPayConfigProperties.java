package cn.fcr.infrastructure.config.mall;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 支付宝配置属性（映射 alipay.* 配置项）
 *
 * @author 傅崇睿
 */
@Data
@ConfigurationProperties(prefix = "alipay", ignoreInvalidFields = true)
public class AliPayConfigProperties {
    /** 支付宝AppID */
    private String app_id;
    /** 商户私钥 */
    private String merchant_private_key;
    /** 支付宝公钥 */
    private String alipay_public_key;
    /** 支付异步通知URL */
    private String notify_url;
    /** 支付同步跳转URL */
    private String return_url;
    /** 支付宝网关地址 */
    private String gatewayUrl;
    /** 签名类型，默认RSA2 */
    private String sign_type = "RSA2";
    /** 字符集，默认UTF-8 */
    private String charset = "utf-8";
    /** 返回格式，默认JSON */
    private String format = "json";
}
