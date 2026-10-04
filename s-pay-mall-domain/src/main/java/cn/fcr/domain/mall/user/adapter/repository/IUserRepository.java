package cn.fcr.domain.mall.user.adapter.repository;

import cn.fcr.domain.mall.user.model.entity.UserEntity;

import java.util.List;

/**
 * 用户仓储接口，定义用户持久化操作的抽象。
 *
 * @author 傅崇睿
 */
public interface IUserRepository {

    UserEntity findByUsernameWithRole(String username);

    Integer countByUsername(String username);

    Long insert(String username, String password, Integer status);

    /**
     * 更新用户名（用于扫码自动注册后固化默认用户名，前缀见
     * {@code Constants.WX_USER_USERNAME_PREFIX}）
     *
     * @param userId   用户ID
     * @param username 新用户名
     * @return 影响行数
     */
    int updateUsername(Long userId, String username);

    int updateStatus(Long userId, Integer status);

    int updateUser(UserEntity user);

    int deleteById(Long id);

    UserEntity findById(Long userId);

    List<UserEntity> listUsersWithRole(String username, Integer status, String roleCode);

    int insertUserRole(Long userId, Long roleId);

    int deleteUserRoleByUserId(Long userId);

    int deleteUserBindingByUserId(Long userId);

    int deleteCartItemByUserId(Long userId);

    String getRoleCodeByUserId(Long userId);
}