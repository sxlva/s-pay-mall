package cn.fcr.domain.auth.gateway;

/**
 * @author 傅崇睿
 * @date 2025/7/26
 * @description 微信网关接口 - 定义微信相关的业务能力
 */
public interface IWeChatGateway {

    /**
     * 创建微信登录二维码票据
     *
     * @return 二维码票据
     */
    String createQrCodeTicket();

    /**
     * 发送登录成功通知
     *
     * @param openid 用户微信 openid
     */
    void sendLoginNotification(String openid);

    /**
     * 发送支付成功通知
     *
     * @param openid      用户微信 openid
     * @param productName 商品名称
     * @param orderId     订单号
     * @param amount      支付金额
     * @param payTime     支付时间
     */
    void sendPaymentSuccessNotification(String openid, String productName, String orderId, String amount, String payTime);
}