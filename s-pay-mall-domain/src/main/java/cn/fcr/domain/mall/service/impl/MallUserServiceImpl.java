package cn.fcr.domain.mall.service.impl;

import cn.fcr.domain.mall.adapter.repository.IUserRepository;
import cn.fcr.domain.mall.gateway.IAuthTokenGateway;
import cn.fcr.domain.mall.gateway.IOrderQueryGateway;
import cn.fcr.domain.mall.gateway.IUserBindingGateway;
import cn.fcr.domain.mall.model.entity.UserEntity;
import cn.fcr.domain.mall.model.valobj.UserLoginVO;
import cn.fcr.domain.mall.model.valobj.UserProfile;
import cn.fcr.domain.mall.service.IMallUserService;
import cn.fcr.types.common.Constants;
import cn.fcr.types.exception.AppException;

import java.util.List;
import java.util.logging.Logger;

public class MallUserServiceImpl implements IMallUserService {

    private final Logger logger = Logger.getLogger(MallUserServiceImpl.class.getName());

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
    public UserLoginVO register(String username, String password) {
        Integer count = userRepository.countByUsername(username);
        if (count != null && count > 0) {
            throw new IllegalArgumentException("用户名已存在");
        }

        Long userId = userRepository.insert(username, authTokenGateway.encodePassword(password), Constants.USER_STATUS_ACTIVE);
        userRepository.insertUserRole(userId, 2L);

        return login(username, password);
    }

    @Override
    public UserLoginVO registerWithWeChat(String username, String password, String openId) {
        if (userBindingGateway.isWeChatOpenIdBound(openId)) {
            throw new IllegalArgumentException("该微信账号已被其他用户绑定");
        }

        Integer count = userRepository.countByUsername(username);
        if (count != null && count > 0) {
            throw new IllegalArgumentException("用户名已存在");
        }

        Long userId = userRepository.insert(username, authTokenGateway.encodePassword(password), Constants.USER_STATUS_WECHAT);
        userRepository.insertUserRole(userId, 2L);

        userBindingGateway.bindWeChatOpenId(userId, openId);
        logger.info("微信扫码注册并绑定成功: userId=" + userId + ", username=" + username + ", openId=" + openId);

        return login(username, password);
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
            logger.warning("【登录拦截】用户 " + username + " 已被封禁");
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
        logger.info("【级联删除】开始删除用户: userId=" + id);

        // 检查用户是否存在关联订单，若存在则禁止删除
        long orderCount = orderQueryGateway.countOrdersByUserId(id);
        if (orderCount > 0) {
            logger.warning("【级联删除拦截】用户 " + id + " 存在 " + orderCount + " 个关联订单，禁止删除");
            throw new AppException(Constants.ResponseCode.UN_ERROR.getCode(), "该用户存在关联订单，无法删除");
        }

        int deleted = 0;

        deleted += userRepository.deleteUserRoleByUserId(id);
        logger.info("【级联删除】已删除 user_role 记录: " + deleted + " 条");

        deleted += userRepository.deleteUserBindingByUserId(id);
        logger.info("【级联删除】已删除 user_binding 记录: " + deleted + " 条");

        deleted += userRepository.deleteCartItemByUserId(id);
        logger.info("【级联删除】已删除 cart_item 记录: " + deleted + " 条");

        deleted += userRepository.deleteById(id);
        logger.info("【级联删除】已删除 mall_user 记录: " + deleted + " 条");

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
                .build();
    }
}
