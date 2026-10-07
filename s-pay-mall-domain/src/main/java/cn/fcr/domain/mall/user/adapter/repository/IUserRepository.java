package cn.fcr.domain.mall.user.adapter.repository;

import cn.fcr.domain.mall.user.model.entity.UserEntity;

import java.util.List;

/**
 * 用户仓储接口，定义用户持久化操作的抽象。
 *
 * @author 傅崇睿
 */
public interface IUserRepository {

    /**
     * 根据用户名查询用户（包含角色信息）
     * 仅返回状态正常的用户
     *
     * @param username 用户名
     * @return 用户实体（含角色编码），不存在或已禁用时返回 null
     */
    UserEntity findByUsernameWithRole(String username);

    /**
     * 统计指定用户名的用户数量
     * 用于注册时校验用户名是否已存在
     *
     * @param username 用户名
     * @return 匹配的用户数量，0 表示不存在
     */
    Integer countByUsername(String username);

    /**
     * 新增用户
     * status 为空时默认正常状态，返回数据库自增主键
     *
     * @param username 用户名
     * @param password 密码密文
     * @param status   用户状态，可为 null（默认正常）
     * @return 新用户的自增ID
     */
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

    /**
     * 更新用户状态（封禁/解封）
     *
     * @param userId 用户ID
     * @param status 目标状态：0=禁用，1=正常
     * @return 影响行数
     */
    int updateStatus(Long userId, Integer status);

    /**
     * 更新用户密码（已 BCrypt 加密的密文）
     *
     * @param userId   用户ID
     * @param password 加密后的密码密文
     * @return 影响行数
     */
    int updatePassword(Long userId, String password);

    /**
     * 更新用户基本信息（管理后台编辑）
     * 仅更新状态与密码字段；密码非空时才覆盖，status 为空默认正常
     *
     * @param user 用户实体（id 必填，password/status 可选）
     * @return 影响行数
     */
    int updateUser(UserEntity user);

    /**
     * 根据用户ID删除用户记录
     * 仅删除 mall_user 主记录，角色/绑定/购物车等关联数据由调用方级联清理
     *
     * @param id 用户ID
     * @return 影响行数
     */
    int deleteById(Long id);

    /**
     * 根据用户ID查询用户
     *
     * @param userId 用户ID
     * @return 用户实体，不存在时返回 null
     */
    UserEntity findById(Long userId);

    /**
     * 条件查询用户列表（包含角色信息）
     * 无角色信息的用户角色编码兜底为默认成员角色
     *
     * @param username 用户名（模糊查询，可选）
     * @param status   用户状态（可选）
     * @param roleCode 角色编码（可选）
     * @return 用户实体列表
     */
    List<UserEntity> listUsersWithRole(String username, Integer status, String roleCode);

    /**
     * 插入用户角色关联记录
     * 在用户注册或分配角色时调用
     *
     * @param userId 用户ID
     * @param roleId 角色ID
     * @return 影响行数
     */
    int insertUserRole(Long userId, Long roleId);

    /**
     * 删除用户的全部角色关联记录
     * 在删除用户或变更角色时级联调用
     *
     * @param userId 用户ID
     * @return 影响行数
     */
    int deleteUserRoleByUserId(Long userId);

    /**
     * 删除用户的全部第三方绑定记录（微信等）
     * 在删除用户时级联调用
     *
     * @param userId 用户ID
     * @return 影响行数
     */
    int deleteUserBindingByUserId(Long userId);

    /**
     * 删除用户的全部购物车记录
     * 在删除用户时级联调用
     *
     * @param userId 用户ID
     * @return 影响行数
     */
    int deleteCartItemByUserId(Long userId);

    /**
     * 查询用户的角色编码
     *
     * @param userId 用户ID
     * @return 角色编码（如 ADMIN、USER），无角色时返回 null
     */
    String getRoleCodeByUserId(Long userId);
}