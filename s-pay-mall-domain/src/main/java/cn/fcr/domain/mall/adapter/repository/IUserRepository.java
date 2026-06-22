package cn.fcr.domain.mall.adapter.repository;

import cn.fcr.domain.mall.model.entity.UserEntity;

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