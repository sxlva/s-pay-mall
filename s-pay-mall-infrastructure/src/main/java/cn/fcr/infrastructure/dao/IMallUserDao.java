package cn.fcr.infrastructure.dao;

import cn.fcr.infrastructure.dao.po.MallUser;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 商城用户数据访问接口
 * 继承 MyBatis-Plus BaseMapper，提供基础的 CRUD 操作
 * 自定义方法支持基于用户名的查询和角色关联查询
 */
@Mapper
public interface IMallUserDao extends BaseMapper<MallUser> {

    /**
     * 根据用户名查询用户信息，包含角色编码
     * 使用 LEFT JOIN 关联 user_role 和 role 表获取用户角色
     *
     * @param username 用户名
     * @return 包含角色信息的用户对象，若不存在返回 null
     */
    @Select("SELECT u.id, u.username, u.password, u.status, u.create_time, r.role_code " +
            "FROM mall_user u LEFT JOIN user_role ur ON u.id = ur.user_id " +
            "LEFT JOIN role r ON ur.role_id = r.id " +
            "WHERE u.username = #{username} AND u.status = 1 LIMIT 1")
    MallUser findByUsernameWithRole(@Param("username") String username);

    /**
     * 统计指定用户名的用户数量
     * 用于检查用户名是否已存在（注册时校验）
     *
     * @param username 用户名
     * @return 匹配的用户数量，0 表示不存在
     */
    @Select("SELECT COUNT(1) FROM mall_user WHERE username = #{username}")
    Integer countByUsername(@Param("username") String username);

    /**
     * 查询用户列表，包含角色信息
     * 使用 LEFT JOIN 关联 user_role 和 role 表获取用户角色
     *
     * @param username 用户名（模糊查询，可选）
     * @param status   用户状态（可选）
     * @param roleCode 角色编码（可选）
     * @return 包含角色信息的用户列表
     */
    @Select("<script>" +
            "SELECT u.id, u.username, u.password, u.status, u.create_time, u.update_time, " +
            "       r.role_code as roleCode, r.id as roleId " +
            "FROM mall_user u " +
            "LEFT JOIN user_role ur ON u.id = ur.user_id " +
            "LEFT JOIN role r ON ur.role_id = r.id " +
            "WHERE 1=1 " +
            "<if test='username != null and username != \"\"'>" +
            "AND u.username LIKE CONCAT('%', #{username}, '%') " +
            "</if>" +
            "<if test='status != null'>" +
            "AND u.status = #{status} " +
            "</if>" +
            "<if test='roleCode != null and roleCode != \"\"'>" +
            "AND r.role_code = #{roleCode} " +
            "</if>" +
            "ORDER BY u.id DESC" +
            "</script>")
    List<MallUser> listUsersWithRole(
            @Param("username") String username,
            @Param("status") Integer status,
            @Param("roleCode") String roleCode);
}