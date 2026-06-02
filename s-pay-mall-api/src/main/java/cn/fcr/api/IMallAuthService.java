package cn.fcr.api;

import cn.fcr.api.dto.UserLoginRequest;
import cn.fcr.api.dto.UserRegisterRequest;
import cn.fcr.api.response.Response;

import java.util.Map;

/**
 * 商城用户认证服务接口
 * 对外暴露用户注册、登录、微信绑定、个人信息等认证相关接口
 */
public interface IMallAuthService {

    /**
     * 用户注册
     * 支持普通账号注册和微信扫码注册
     *
     * @param request 注册请求（用户名、密码、可选openId）
     * @return 用户登录信息（token、userId、username、role）
     */
    Response<Map<String, Object>> register(UserRegisterRequest request);

    /**
     * 用户登录
     * 验证用户名密码，成功返回JWT token
     *
     * @param request 登录请求（用户名、密码）
     * @return 用户登录信息（token、userId、username、role）
     */
    Response<Map<String, Object>> login(UserLoginRequest request);

    /**
     * 获取用户个人信息
     *
     * @param userId 用户ID（从token中解析）
     * @return 用户信息（包含订单数量、购物车数量等）
     */
    Response<Map<String, Object>> getProfile(Long userId);

    /**
     * 生成微信绑定码
     * 用户在微信公众号发送"绑定 xxx"完成账号绑定
     *
     * @return 绑定码
     */
    Response<Map<String, String>> generateBindCode();

    /**
     * 轮询检查微信绑定状态
     *
     * @param ticket 绑定码
     * @return 绑定状态（成功返回openId）
     */
    Response<Map<String, Object>> checkBindStatus(String ticket);
}