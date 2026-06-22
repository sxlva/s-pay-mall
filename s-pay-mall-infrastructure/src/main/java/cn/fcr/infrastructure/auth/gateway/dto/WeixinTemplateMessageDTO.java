package cn.fcr.infrastructure.auth.gateway.dto;

import lombok.Data;
import lombok.Getter;

import java.io.Serial;
import java.util.HashMap;
import java.util.Map;

/**
 * 微信模板消息DTO
 *
 * @author 傅崇睿
 */
@Data
public class WeixinTemplateMessageDTO {

    /**
     * 接收消息的用户OpenID
     */
    private String touser;

    /**
     * 模板消息ID
     */
    private String template_id;

    /**
     * 点击消息跳转的URL
     */
    private String url = "https://weixin.qq.com";

    /**
     * 模板变量数据
     */
    private Map<String, Map<String, String>> data = new HashMap<>();

    /**
     * 构造函数
     *
     * @param touser 用户OpenID
     * @param template_id 模板消息ID
     */
    public WeixinTemplateMessageDTO(String touser, String template_id) {
        this.touser = touser;
        this.template_id = template_id;
    }

    /**
     * 静态方法：向指定数据Map中添加模板变量
     *
     * @param data 目标数据Map
     * @param key 模板变量键
     * @param value 变量值
     */
    public static void put(Map<String, Map<String, String>> data, TemplateKey key, String value) {
        data.put(key.getCode(), new HashMap<>() {
            @Serial
            private static final long serialVersionUID = 7092338402387318563L;

            {
                put("value", value);
            }
        });
    }

    /**
     * 模板变量键枚举
     */
    @Getter
    public enum TemplateKey {
        USER("user","用户ID"),
        PRODUCT("product","商品名称"),
        ORDER_ID("orderId","订单号"),
        AMOUNT("amount","金额"),
        PAY_TIME("payTime","支付时间")
        ;

        private final String code;
        private final String desc;

        TemplateKey(String code, String desc) {
            this.code = code;
            this.desc = desc;
        }
    }

}