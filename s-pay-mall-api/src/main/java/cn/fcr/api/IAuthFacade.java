package cn.fcr.api;

import cn.fcr.api.response.Response;

/**
 * 认证外观接口
 * <p>
 * 对外暴露认证登录相关的操作接口，提供微信扫码登录能力
 *
 * @author 傅崇睿
 */
public interface IAuthFacade {

    /**
     * 获取微信登录二维码ticket
     * <p>
     * 生成用于微信扫码登录的二维码ticket，前端使用此ticket获取二维码图片
     *
     * @return Response&lt;String&gt; 返回微信二维码ticket
     */
    Response<String> weixinQrCodeTicket();

    /**
     * 检查登录状态
     * <p>
     * 根据ticket检查用户是否已扫码登录，返回登录成功后的token或相关标识
     *
     * @param ticket 二维码ticket，用于标识当前登录会话
     * @return Response&lt;String&gt; 返回登录token或状态信息
     */
    Response<String> checkLogin(String ticket);

}