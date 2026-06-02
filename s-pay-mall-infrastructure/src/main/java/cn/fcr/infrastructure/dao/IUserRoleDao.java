package cn.fcr.infrastructure.dao;

import cn.fcr.infrastructure.dao.po.UserRole;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 用户角色关系数据访问接口
 * 负责用户与角色之间的多对多关联关系操作
 * 主要用于权限控制和角色鉴定
 */
@Mapper
public interface IUserRoleDao extends BaseMapper<UserRole> {

    /**
     * 插入用户角色关联记录
     * 在用户注册或分配角色时调用
     *
     * @param userId 用户ID
     * @param roleId 角色ID
     * @return 影响行数
     */
    @Insert("INSERT IGNORE INTO user_role(user_id, role_id) VALUES(#{userId}, #{roleId})")
    int insertUserRole(@Param("userId") Long userId, @Param("roleId") Long roleId);

    /**
     * 根据用户ID查询用户的角色编码
     * 用于用户登录后获取权限标识，如 "ADMIN"、"USER" 等
     *
     * @param userId 用户ID
     * @return 角色编码，若无角色返回 null
     */
    @Select("SELECT r.role_code FROM user_role ur JOIN role r ON ur.role_id = r.id WHERE ur.user_id = #{userId} LIMIT 1")
    String getRoleCodeByUserId(@Param("userId") Long userId);

    /**
     * 根据用户ID删除所有角色关联记录
     * 在删除用户时调用，避免外键约束错误
     *
     * @param userId 用户ID
     * @return 影响行数
     */
    @Delete("DELETE FROM user_role WHERE user_id = #{userId}")
    int deleteByUserId(@Param("userId") Long userId);
}