package cn.fcr.domain.mall.user.service.impl;

import cn.fcr.domain.auth.token.gateway.IAuthTokenGateway;
import cn.fcr.domain.mall.user.adapter.repository.IUserRepository;
import cn.fcr.domain.mall.user.gateway.IUserBindingGateway;
import cn.fcr.domain.mall.user.model.entity.UserEntity;
import cn.fcr.domain.mall.user.model.valobj.UserProfile;
import cn.fcr.domain.order.gateway.IOrderQueryGateway;
import cn.fcr.domain.order.model.valobj.OrderSummaryVO;
import cn.fcr.types.common.Constants;
import cn.fcr.types.exception.AppException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * MallUserServiceImpl 单元测试
 *
 * <p>覆盖账号绑定相关领域规则：bindWeChat（绑定/冲突/幂等/非法入参）、
 * setPassword（设密/微信用户转正/弱密码拒绝）、getProfile（wechatBound 输出）。
 * 外部依赖（仓储/网关）以手写桩替代，不使用 Mock 框架（domain 模块未引入 Mockito）。</p>
 *
 * @author 傅崇睿
 */
public class MallUserServiceImplTest {

    private StubUserRepository userRepository;
    private StubUserBindingGateway userBindingGateway;
    private MallUserServiceImpl mallUserService;

    @BeforeEach
    public void setUp() {
        userRepository = new StubUserRepository();
        userBindingGateway = new StubUserBindingGateway();
        mallUserService = new MallUserServiceImpl(
                userRepository,
                new StubAuthTokenGateway(),
                new StubOrderQueryGateway(),
                userBindingGateway);
    }

    // ==================== bindWeChat ====================

    @Test
    public void testBindWeChat_NewBinding_ShouldBind() {
        userRepository.user = UserEntity.builder()
                .id(1L).username("dawn").status(Constants.USER_STATUS_ACTIVE).build();

        mallUserService.bindWeChat(1L, "openid_abc");

        assertEquals(Long.valueOf(1L), userBindingGateway.boundUserId);
        assertEquals("openid_abc", userBindingGateway.boundOpenId);
    }

    @Test
    public void testBindWeChat_OpenIdBoundToOtherUser_ShouldThrow() {
        userRepository.user = UserEntity.builder()
                .id(1L).username("dawn").status(Constants.USER_STATUS_ACTIVE).build();
        userBindingGateway.existingBindingUserId = 2L;

        AppException exception = assertThrows(AppException.class,
                () -> mallUserService.bindWeChat(1L, "openid_abc"));
        assertTrue(exception.getInfo().contains("已被其他用户绑定"));
        assertNull(userBindingGateway.boundUserId);
    }

    @Test
    public void testBindWeChat_AlreadyBoundSameOpenId_ShouldBeIdempotent() {
        userRepository.user = UserEntity.builder()
                .id(1L).username("dawn").status(Constants.USER_STATUS_ACTIVE).build();
        userBindingGateway.openIdOfUser = "openid_abc";

        mallUserService.bindWeChat(1L, "openid_abc");

        assertNull(userBindingGateway.boundUserId, "幂等命中时不应重复插入绑定记录");
    }

    @Test
    public void testBindWeChat_BlankOpenId_ShouldThrow() {
        assertThrows(AppException.class, () -> mallUserService.bindWeChat(1L, " "));
        assertNull(userBindingGateway.boundUserId);
    }

    @Test
    public void testBindWeChat_UserNotFound_ShouldThrow() {
        userRepository.user = null;

        AppException exception = assertThrows(AppException.class,
                () -> mallUserService.bindWeChat(1L, "openid_abc"));
        assertTrue(exception.getInfo().contains("用户不存在"));
    }

    // ==================== setPassword ====================

    @Test
    public void testSetPassword_WeChatUser_ShouldUpdatePasswordAndActivate() {
        userRepository.user = UserEntity.builder()
                .id(1L).username("wx_user_1").status(Constants.USER_STATUS_WECHAT).build();

        mallUserService.setPassword(1L, "newPass123");

        assertEquals("encoded_newPass123", userRepository.updatedPassword);
        assertEquals(Constants.USER_STATUS_ACTIVE, userRepository.updatedStatus,
                "微信扫码注册用户补设密码后状态应转为正常");
    }

    @Test
    public void testSetPassword_ActiveUser_ShouldOnlyUpdatePassword() {
        userRepository.user = UserEntity.builder()
                .id(1L).username("dawn").status(Constants.USER_STATUS_ACTIVE).build();

        mallUserService.setPassword(1L, "newPass123");

        assertEquals("encoded_newPass123", userRepository.updatedPassword);
        assertNull(userRepository.updatedStatus, "正常用户改密码不应触发状态变更");
    }

    @Test
    public void testSetPassword_TooShortPassword_ShouldThrow() {
        userRepository.user = UserEntity.builder()
                .id(1L).username("dawn").status(Constants.USER_STATUS_ACTIVE).build();

        assertThrows(AppException.class, () -> mallUserService.setPassword(1L, "123"));
        assertNull(userRepository.updatedPassword);
    }

    @Test
    public void testSetPassword_UserNotFound_ShouldThrow() {
        userRepository.user = null;

        assertThrows(AppException.class, () -> mallUserService.setPassword(1L, "newPass123"));
    }

    // ==================== getProfile ====================

    @Test
    public void testGetProfile_BoundWeChat_ShouldReturnWechatBoundTrue() {
        userRepository.user = UserEntity.builder()
                .id(1L).username("dawn").status(Constants.USER_STATUS_ACTIVE).build();
        userRepository.roleCode = "MEMBER";
        userBindingGateway.openIdOfUser = "openid_abc";

        UserProfile profile = mallUserService.getProfile(1L);

        assertNotNull(profile);
        assertEquals(Boolean.TRUE, profile.getWechatBound());
    }

    @Test
    public void testGetProfile_WeChatUnbound_ShouldReturnWechatBoundFalse() {
        userRepository.user = UserEntity.builder()
                .id(1L).username("dawn").status(Constants.USER_STATUS_ACTIVE).build();
        userRepository.roleCode = "MEMBER";
        userBindingGateway.openIdOfUser = null;

        UserProfile profile = mallUserService.getProfile(1L);

        assertEquals(Boolean.FALSE, profile.getWechatBound());
    }

    // ==================== 手写桩实现 ====================

    /** 用户仓储桩：记录更新动作，支持按用例注入用户 */
    private static class StubUserRepository implements IUserRepository {
        UserEntity user;
        String roleCode;
        String updatedPassword;
        Integer updatedStatus;

        @Override
        public UserEntity findByUsernameWithRole(String username) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Integer countByUsername(String username) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Long insert(String username, String password, Integer status) {
            throw new UnsupportedOperationException();
        }

        @Override
        public int updateUsername(Long userId, String username) {
            throw new UnsupportedOperationException();
        }

        @Override
        public int updateStatus(Long userId, Integer status) {
            this.updatedStatus = status;
            return 1;
        }

        @Override
        public int updatePassword(Long userId, String password) {
            this.updatedPassword = password;
            return 1;
        }

        @Override
        public int updateUser(UserEntity user) {
            throw new UnsupportedOperationException();
        }

        @Override
        public int deleteById(Long id) {
            throw new UnsupportedOperationException();
        }

        @Override
        public UserEntity findById(Long userId) {
            return user;
        }

        @Override
        public List<UserEntity> listUsersWithRole(String username, Integer status, String roleCode) {
            throw new UnsupportedOperationException();
        }

        @Override
        public int insertUserRole(Long userId, Long roleId) {
            throw new UnsupportedOperationException();
        }

        @Override
        public int deleteUserRoleByUserId(Long userId) {
            throw new UnsupportedOperationException();
        }

        @Override
        public int deleteUserBindingByUserId(Long userId) {
            throw new UnsupportedOperationException();
        }

        @Override
        public int deleteCartItemByUserId(Long userId) {
            throw new UnsupportedOperationException();
        }

        @Override
        public String getRoleCodeByUserId(Long userId) {
            return roleCode;
        }
    }

    /** 用户绑定网关桩：记录绑定动作，支持注入已绑定关系 */
    private static class StubUserBindingGateway implements IUserBindingGateway {
        /** 当前用户已绑定的 openId */
        String openIdOfUser;
        /** openId 已被其他用户绑定时的持有者ID */
        Long existingBindingUserId;
        Long boundUserId;
        String boundOpenId;

        @Override
        public String getWeChatOpenIdByUserId(Long userId) {
            return openIdOfUser;
        }

        @Override
        public boolean isWeChatOpenIdBound(String openId) {
            return existingBindingUserId != null;
        }

        @Override
        public void bindWeChatOpenId(Long userId, String openId) {
            this.boundUserId = userId;
            this.boundOpenId = openId;
        }
    }

    /** 令牌网关桩：密码编码前缀固定，便于断言 */
    private static class StubAuthTokenGateway implements IAuthTokenGateway {
        @Override
        public String createToken(Long userId, String username, String role) {
            throw new UnsupportedOperationException();
        }

        @Override
        public String encodePassword(String rawPassword) {
            return "encoded_" + rawPassword;
        }

        @Override
        public boolean matchesPassword(String rawPassword, String encodedPassword) {
            throw new UnsupportedOperationException();
        }
    }

    /** 订单查询网关桩（本测试未触及订单逻辑） */
    private static class StubOrderQueryGateway implements IOrderQueryGateway {
        @Override
        public long countOrdersByUserId(Long userId) {
            throw new UnsupportedOperationException();
        }

        @Override
        public OrderSummaryVO findPayOrderByOrderNo(String orderNo) {
            throw new UnsupportedOperationException();
        }
    }
}
