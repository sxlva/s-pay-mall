package cn.fcr.api;

import cn.fcr.api.dto.UserLoginRequestDTO;
import cn.fcr.api.dto.UserRegisterRequestDTO;
import cn.fcr.api.response.Response;
import cn.fcr.api.vo.BindStatusVO;
import cn.fcr.api.vo.UserLoginVO;
import cn.fcr.api.vo.UserProfileVO;

/**
 * 商城用户认证外观接口
 * 对外暴露用户注册、登录、微信绑定、个人信息等认证相关接口
 *
 * @author 傅崇睿
 */
public interface IMallAuthFacade {

    /**
     * 用户注册
     * 支持普通账号注册和微信扫码注册
     *
     * @param request 注册请求（用户名、密码、可选openId）
     * @return 用户登录信息（token、userId、username、role）
     */
    Response<UserLoginVO> register(UserRegisterRequestDTO request);

    /**
     * 用户登录
     * 验证用户名密码，成功返回JWT token
     *
     * @param request 登录请求（用户名、密码）
     * @return 用户登录信息（token、userId、username、role）
     */
    Response<UserLoginVO> login(UserLoginRequestDTO request);

    /**
     * 获取用户个人信息
     *
     * @param userId 用户ID（从token中解析）
     * @return 用户信息（包含订单数量、购物车数量等）
     */
    Response<UserProfileVO> getProfile(Long userId);

    /**
     * 生成微信绑定二维码ticket
     * 用户使用微信扫码完成账号绑定
     *
     * @return 二维码ticket
     */
    Response<String> generateBindQrCode();

    /**
     * 轮询检查微信绑定状态
     *
     * @param ticket 绑定码
     * @return 绑定状态（成功返回openId）
     */
    Response<BindStatusVO> checkBindStatus(String ticket);
}