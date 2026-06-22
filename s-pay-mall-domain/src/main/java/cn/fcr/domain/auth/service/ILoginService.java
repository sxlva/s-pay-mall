package cn.fcr.domain.auth.service;

/**
 * 登录服务接口
 *
 * @author 傅崇睿
 */
public interface ILoginService {

    /**
     * 创建微信登录二维码票据
     *
     * @return 二维码票据
     */
    String createQrCodeTicket();

    /**
     * 检查登录状态
     *
     * @param ticket 票据
     * @return openid，如果未登录则返回 null
     */
    String checkLogin(String ticket);

    /**
     * 保存登录状态
     *
     * @param ticket 票据
     * @param openid 用户微信 openid
     */
    void saveLoginState(String ticket, String openid);

    /**
     * 处理微信扫码登录，实现自动注册与绑定
     * 
     * @param ticket 票据
     * @param openid 用户微信 openid
     * @return JWT token
     */
    String handleWechatScanLogin(String ticket, String openid);

}
