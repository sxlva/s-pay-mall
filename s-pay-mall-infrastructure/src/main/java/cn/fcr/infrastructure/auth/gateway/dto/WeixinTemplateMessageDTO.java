package cn.fcr.infrastructure.auth.gateway.dto;

import java.util.HashMap;
import java.util.Map;

/**
 * @description 微信模板消息DTO
 *
 * 【职责说明】
 * - 封装微信公众号模板消息的数据结构
 * - 用于向微信服务器发送模板消息通知
 * - 支持动态填充模板变量（用户ID、商品名称、订单号、金额、支付时间等）
 *
 * 【字段说明】
 * - touser: 接收用户的OpenID
 * - template_id: 模板消息ID（从微信公众平台配置）
 * - url: 点击消息跳转的URL
 * - data: 模板变量数据，key为模板中的变量名，value为具体值
 *
 * 【使用方式】
 * 通过构造函数传入touser和template_id，使用put方法设置模板变量
 */
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
     * 向模板数据中添加变量
     *
     * @param key 模板变量键（来自TemplateKey枚举）
     * @param value 变量值
     */
    public void put(TemplateKey key, String value) {
        data.put(key.getCode(), new HashMap<String, String>() {
            private static final long serialVersionUID = 7092338402387318563L;

            {
                put("value", value);
            }
        });
    }

    /**
     * 静态方法：向指定数据Map中添加模板变量
     *
     * @param data 目标数据Map
     * @param key 模板变量键
     * @param value 变量值
     */
    public static void put(Map<String, Map<String, String>> data, TemplateKey key, String value) {
        data.put(key.getCode(), new HashMap<String, String>() {
            private static final long serialVersionUID = 7092338402387318563L;

            {
                put("value", value);
            }
        });
    }

    /**
     * 模板变量键枚举
     */
    public enum TemplateKey {
        USER("user","用户ID"),
        PRODUCT("product","商品名称"),
        ORDER_ID("orderId","订单号"),
        AMOUNT("amount","金额"),
        PAY_TIME("payTime","支付时间")
        ;

        private String code;
        private String desc;

        TemplateKey(String code, String desc) {
            this.code = code;
            this.desc = desc;
        }

        public String getCode() {
            return code;
        }

        public void setCode(String code) {
            this.code = code;
        }

        public String getDesc() {
            return desc;
        }

        public void setDesc(String desc) {
            this.desc = desc;
        }
    }

    // Getters and Setters
    public String getTouser() {
        return touser;
    }

    public void setTouser(String touser) {
        this.touser = touser;
    }

    public String getTemplate_id() {
        return template_id;
    }

    public void setTemplate_id(String template_id) {
        this.template_id = template_id;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public Map<String, Map<String, String>> getData() {
        return data;
    }

    public void setData(Map<String, Map<String, String>> data) {
        this.data = data;
    }

}
