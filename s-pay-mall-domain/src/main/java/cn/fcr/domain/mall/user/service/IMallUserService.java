package cn.fcr.domain.mall.user.service;

import cn.fcr.domain.mall.user.model.entity.UserEntity;
import cn.fcr.domain.mall.user.model.valobj.UserLoginVO;
import cn.fcr.domain.mall.user.model.valobj.UserProfile;

import java.util.List;

/**
 * 用户领域服务接口，定义注册、登录和用户管理的抽象。
 *
 * @author 傅崇睿
 */
public interface IMallUserService {

    /**
     * 用户注册（统一入口，注册策略在领域层判定）
     *
     * <p>【P0-6】openId 为空白走账密注册；openId 有效则注册并绑定微信。
     * 注册方式的路由规则收敛在本接口，Trigger 层不再持有 if-else 判断。</p>
     *
     * @param username 用户名
     * @param password 密码
     * @param openId   微信 OpenID，可为 null/空白（纯账密注册）
     * @return 登录信息（含JWT token）
     */
    UserLoginVO register(String username, String password, String openId);

    /**
     * 微信扫码登录的自动注册（首次扫码建户并绑定）
     *
     * <p>【P0-6】原实现位于 Infrastructure 网关（WeixinLoginGatewayImpl），
     * 与 {@link #register} 双写同一套建户规则；现收敛为本领域唯一入口。
     * 用户名规则：先以临时名落库，再以自增ID固化为 wx_user_{userId}。</p>
     *
     * @param openId 微信 OpenID（调用方已确认未绑定）
     * @return 登录信息（含JWT token，角色 MEMBER）
     */
    UserLoginVO registerWeChatUserByScan(String openId);

    UserLoginVO login(String username, String password);

    List<UserEntity> listUsers(String username, Integer status, String roleCode);

    int saveUser(UserEntity user);

    int updateUserStatus(Long userId, Integer status);

    int deleteUser(Long id);

    UserProfile getProfile(Long userId);
}
