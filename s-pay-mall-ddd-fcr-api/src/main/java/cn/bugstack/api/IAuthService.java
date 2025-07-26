package cn.bugstack.api;

import cn.bugstack.api.response.Response;

/**
 * @author xiaolv
 * @date 2025/7/26 15:47
 * @description 登陆接口
 */
public interface IAuthService {

    Response<String> weixinQrCodeTicket();

    Response<String> checkLogin(String ticket);

}
