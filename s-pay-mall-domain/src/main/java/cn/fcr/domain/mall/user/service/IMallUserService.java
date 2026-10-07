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
     * 用户名规则：先以临时名落库，再以自增ID固化为
     * {@code Constants.WX_USER_USERNAME_PREFIX} + userId。</p>
     *
     * @param openId 微信 OpenID（调用方已确认未绑定）
     * @return 登录信息（含JWT token，角色 MEMBER）
     */
    UserLoginVO registerWeChatUserByScan(String openId);

    /**
     * 账密登录
     * 校验用户存在性、封禁状态与密码正确性，通过后签发 JWT Token
     *
     * @param username 用户名
     * @param password 明文密码
     * @return 登录信息（含JWT token）
     */
    UserLoginVO login(String username, String password);

    /**
     * 条件查询用户列表（管理后台）
     *
     * @param username 用户名（模糊查询，可选）
     * @param status   用户状态（可选）
     * @param roleCode 角色编码（可选）
     * @return 用户实体列表，包含角色信息
     */
    List<UserEntity> listUsers(String username, Integer status, String roleCode);

    /**
     * 新增或编辑用户（管理后台）
     * 密码字段非空时经 BCrypt 加密后落库
     *
     * @param user 用户实体
     * @return 影响行数
     */
    int saveUser(UserEntity user);

    /**
     * 更新用户状态（封禁/解封）
     *
     * @param userId 用户ID
     * @param status 目标状态：0=禁用，1=正常
     * @return 影响行数
     */
    int updateUserStatus(Long userId, Integer status);

    /**
     * 删除用户（管理后台）
     * 存在关联订单时拒绝删除；删除时级联清理角色、微信绑定与购物车数据
     *
     * @param id 用户ID
     * @return 影响行数
     */
    int deleteUser(Long id);

    /**
     * 查询当前登录用户的个人资料
     *
     * @param userId 用户ID
     * @return 用户资料，用户不存在时返回 null
     */
    UserProfile getProfile(Long userId);

    /**
     * 绑定微信 OpenID 到当前用户（账密用户绑定微信）
     *
     * <p>【TD-10 收尾】绑定规则在领域层判定：openId 已被其他用户绑定时拒绝；
     * 已绑定到本用户时幂等返回（不重复插记录）。绑定成功后该账户同时支持
     * 账密登录与微信扫码登录。</p>
     *
     * @param userId 当前登录用户ID
     * @param openId 微信 OpenID（服务端按绑定票据解析）
     */
    void bindWeChat(Long userId, String openId);

    /**
     * 设置/修改账户密码（微信用户补设密码）
     *
     * <p>密码经 BCrypt 加密后落库；微信扫码注册用户（状态
     * {@code Constants.USER_STATUS_WECHAT}）设置密码后状态转为正常，
     * 之后该账户同时支持微信扫码与账密两种登录方式。</p>
     *
     * @param userId      当前登录用户ID
     * @param rawPassword 明文密码
     */
    void setPassword(Long userId, String rawPassword);
}
