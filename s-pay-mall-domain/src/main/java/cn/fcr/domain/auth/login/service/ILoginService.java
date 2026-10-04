package cn.fcr.domain.auth.login.service;

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
     * 检查登录状态（前端轮询入口）
     *
     * <p>返回票据槽位中最后一次存入的凭证：扫码登录完成前是 openid，
     * 扫码登录完成后是 JWT token；取出后即删除（Redis get 后删的既有行为）。
     * 槽位为空表示未登录。</p>
     *
     * @param ticket 票据
     * @return 槽位中的凭证（openid 或 JWT token），未登录返回 null
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
