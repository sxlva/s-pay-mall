package cn.fcr.domain.mall.user.service.impl;

import cn.fcr.domain.auth.token.gateway.IAuthTokenGateway;
import cn.fcr.domain.mall.user.adapter.repository.IUserRepository;
import cn.fcr.domain.order.gateway.IOrderQueryGateway;
import cn.fcr.domain.mall.user.gateway.IUserBindingGateway;
import cn.fcr.domain.mall.user.model.entity.UserEntity;
import cn.fcr.domain.mall.user.model.valobj.UserLoginVO;
import cn.fcr.domain.mall.user.model.valobj.UserProfile;
import cn.fcr.domain.mall.user.service.IMallUserService;
import cn.fcr.types.common.Constants;
import cn.fcr.types.exception.AppException;

import java.util.List;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;

/**
 * 用户领域服务实现，负责注册、登录、微信绑定和用户数据管理。
 *
 * @author 傅崇睿
 */
@Slf4j
public class MallUserServiceImpl implements IMallUserService {

    /** 会员角色ID（商城注册用户默认角色） */
    private static final Long MEMBER_ROLE_ID = 2L;

    private final IUserRepository userRepository;
    private final IAuthTokenGateway authTokenGateway;
    private final IOrderQueryGateway orderQueryGateway;
    private final IUserBindingGateway userBindingGateway;

    public MallUserServiceImpl(IUserRepository userRepository,
                               IAuthTokenGateway authTokenGateway,
                               IOrderQueryGateway orderQueryGateway,
                               IUserBindingGateway userBindingGateway) {
        this.userRepository = userRepository;
        this.authTokenGateway = authTokenGateway;
        this.orderQueryGateway = orderQueryGateway;
        this.userBindingGateway = userBindingGateway;
    }

    @Override
    public UserLoginVO register(String username, String password, String openId) {
        if (openId != null && !openId.isBlank()) {
            return registerWithWeChat(username, password, openId);
        }
        return registerWithPassword(username, password);
    }

    /**
     * 账密注册
     *
     * @param username 用户名
     * @param password 明文密码（内部加密存储）
     * @return 登录信息（含JWT token）
     */
    private UserLoginVO registerWithPassword(String username, String password) {
        createUser(username, password, Constants.USER_STATUS_ACTIVE);
        return login(username, password);
    }

    /**
     * 微信注册（注册账号并同时绑定微信）
     *
     * @param username 用户名
     * @param password 明文密码（内部加密存储）
     * @param openId   微信 OpenID
     * @return 登录信息（含JWT token）
     */
    private UserLoginVO registerWithWeChat(String username, String password, String openId) {
        if (userBindingGateway.isWeChatOpenIdBound(openId)) {
            throw new IllegalArgumentException("该微信账号已被其他用户绑定");
        }

        Long userId = createUser(username, password, Constants.USER_STATUS_WECHAT);
        userBindingGateway.bindWeChatOpenId(userId, openId);
        log.info("微信扫码注册并绑定成功: userId={}, username={}, openId={}", userId, username, openId);

        return login(username, password);
    }

    @Override
    public UserLoginVO registerWeChatUserByScan(String openId) {
        // 先以临时名落库拿自增ID，再固化为 Constants.WX_USER_USERNAME_PREFIX + userId
        //（与扫码登录侧的用户名约定一致）
        Long userId = createUser("temp_" + UUID.randomUUID().toString().substring(0, 8), "",
                Constants.USER_STATUS_WECHAT);
        String username = Constants.WX_USER_USERNAME_PREFIX + userId;
        userRepository.updateUsername(userId, username);

        userBindingGateway.bindWeChatOpenId(userId, openId);
        log.info("微信扫码自动注册并绑定成功: userId={}, openId={}", userId, openId);

        String token = authTokenGateway.createToken(userId, username, Constants.DEFAULT_ROLE_MEMBER);
        return UserLoginVO.builder()
                .token(token)
                .userId(userId)
                .username(username)
                .role(Constants.DEFAULT_ROLE_MEMBER)
                .build();
    }

    /**
     * 创建用户（查重 + 落库 + 赋会员角色），注册各路径的公共步骤
     *
     * @param username 用户名
     * @param password 已加密密码
     * @param status   用户状态（Constants.USER_STATUS_*）
     * @return 新用户ID
     */
    private Long createUser(String username, String password, Integer status) {
        Integer count = userRepository.countByUsername(username);
        if (count != null && count > 0) {
            throw new IllegalArgumentException("用户名已存在");
        }

        Long userId = userRepository.insert(username, authTokenGateway.encodePassword(password), status);
        userRepository.insertUserRole(userId, MEMBER_ROLE_ID);
        return userId;
    }

    @Override
    public UserLoginVO login(String username, String password) {
        UserEntity user = userRepository.findByUsernameWithRole(username);

        if (user == null) {
            throw new IllegalArgumentException("账号不存在或已禁用");
        }

        try {
            user.validateLoginStatus();
        } catch (IllegalStateException e) {
            log.warn("【登录拦截】用户 " + username + " 已被封禁");
            throw new AppException(Constants.ResponseCode.BANNED.getCode(), e.getMessage());
        }

        if (!user.validatePassword(password, authTokenGateway::matchesPassword)) {
            throw new IllegalArgumentException("密码错误");
        }

        Long userId = user.getId();
        String role = user.getRoleOrDefault();

        String token = authTokenGateway.createToken(userId, username, role);

        return UserLoginVO.builder()
                .token(token)
                .userId(userId)
                .username(username)
                .role(role)
                .build();
    }

    @Override
    public List<UserEntity> listUsers(String username, Integer status, String roleCode) {
        return userRepository.listUsersWithRole(username, status, roleCode);
    }

    @Override
    public int saveUser(UserEntity user) {
        UserEntity userCopy = UserEntity.builder()
                .id(user.getId())
                .username(user.getUsername())
                .password(user.getPassword())
                .status(user.getStatus())
                .roleCode(user.getRoleCode())
                .roleId(user.getRoleId())
                .build();

        if (userCopy.getPassword() != null) {
            userCopy.setPassword(authTokenGateway.encodePassword(userCopy.getPassword()));
        }
        return userRepository.updateUser(userCopy);
    }

    @Override
    public int updateUserStatus(Long userId, Integer status) {
        return userRepository.updateStatus(userId, status);
    }

    @Override
    public int deleteUser(Long id) {
        log.info("【级联删除】开始删除用户: userId=" + id);

        // 检查用户是否存在关联订单，若存在则禁止删除
        long orderCount = orderQueryGateway.countOrdersByUserId(id);
        if (orderCount > 0) {
            log.warn("【级联删除拦截】用户 " + id + " 存在 " + orderCount + " 个关联订单，禁止删除");
            throw new AppException(Constants.ResponseCode.UN_ERROR.getCode(), "该用户存在关联订单，无法删除");
        }

        int deleted = 0;

        deleted += userRepository.deleteUserRoleByUserId(id);
        log.info("【级联删除】已删除 user_role 记录: " + deleted + " 条");

        deleted += userRepository.deleteUserBindingByUserId(id);
        log.info("【级联删除】已删除 user_binding 记录: " + deleted + " 条");

        deleted += userRepository.deleteCartItemByUserId(id);
        log.info("【级联删除】已删除 cart_item 记录: " + deleted + " 条");

        deleted += userRepository.deleteById(id);
        log.info("【级联删除】已删除 mall_user 记录: " + deleted + " 条");

        return deleted;
    }

    @Override
    public UserProfile getProfile(Long userId) {
        UserEntity user = userRepository.findById(userId);
        if (user == null) {
            return null;
        }

        String roleCode = userRepository.getRoleCodeByUserId(userId);
        if (roleCode == null) {
            roleCode = Constants.DEFAULT_ROLE_MEMBER;
        }

        return UserProfile.builder()
                .id(user.getId())
                .username(user.getUsername())
                .status(user.getStatus())
                .roleCode(roleCode)
                .createTime(user.getCreateTime())
                .updateTime(user.getUpdateTime())
                .wechatBound(userBindingGateway.getWeChatOpenIdByUserId(userId) != null)
                .build();
    }

    @Override
    public void bindWeChat(Long userId, String openId) {
        if (openId == null || openId.isBlank()) {
            // S-03：面向用户的业务拒绝统一走 AppException（0001 + 固定文案），不再借 IllegalArgumentException 透传
            throw new AppException(Constants.ResponseCode.UN_ERROR.getCode(), "微信绑定凭证无效，请重新扫码");
        }
        if (userRepository.findById(userId) == null) {
            throw new AppException(Constants.ResponseCode.UN_ERROR.getCode(), "用户不存在");
        }

        // 幂等：本用户已绑定同一微信时直接返回
        String boundOpenId = userBindingGateway.getWeChatOpenIdByUserId(userId);
        if (openId.equals(boundOpenId)) {
            log.info("微信绑定幂等命中，跳过重复绑定: userId={}, openId={}", userId, openId);
            return;
        }

        // 冲突：该微信已被其他用户绑定（user_binding 唯一键 uk_type_identifier 兜底）
        if (userBindingGateway.isWeChatOpenIdBound(openId)) {
            throw new AppException(Constants.ResponseCode.UN_ERROR.getCode(), "该微信账号已被其他用户绑定");
        }

        userBindingGateway.bindWeChatOpenId(userId, openId);
        log.info("账号绑定微信成功: userId={}, openId={}", userId, openId);
    }

    @Override
    public void setPassword(Long userId, String rawPassword) {
        if (rawPassword == null || rawPassword.length() < 6 || rawPassword.length() > 64) {
            // S-03：面向用户的业务拒绝统一走 AppException（0001 + 固定文案）
            throw new AppException(Constants.ResponseCode.UN_ERROR.getCode(), "密码长度需在 6~64 位之间");
        }
        UserEntity user = userRepository.findById(userId);
        if (user == null) {
            throw new AppException(Constants.ResponseCode.UN_ERROR.getCode(), "用户不存在");
        }

        userRepository.updatePassword(userId, authTokenGateway.encodePassword(rawPassword));
        // 微信扫码注册用户补设密码后转为正常状态，账密登录正式可用
        if (Constants.USER_STATUS_WECHAT.equals(user.getStatus())) {
            userRepository.updateStatus(userId, Constants.USER_STATUS_ACTIVE);
        }
        log.info("用户设置密码成功: userId={}, 原状态={}", userId, user.getStatus());
    }
}
