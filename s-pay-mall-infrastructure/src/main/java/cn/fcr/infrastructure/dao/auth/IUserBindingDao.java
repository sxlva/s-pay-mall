package cn.fcr.infrastructure.dao.auth;

import cn.fcr.infrastructure.dao.auth.po.UserBinding;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 用户绑定关系数据访问接口（MyBatis-Plus BaseMapper）
 *
 * @author 傅崇睿
 */
@Mapper
public interface IUserBindingDao extends BaseMapper<UserBinding> {

    /**
     * 根据登录类型和标识查询用户绑定记录
     * 用于第三方登录时查找已绑定的本地用户
     *
     * @param identityType 登录类型（如 weixin、phone）
     * @param identifier 标识值（如微信 openid、手机号）
     * @return 用户绑定记录，若不存在返回 null
     */
    @Select("SELECT * FROM user_binding WHERE identity_type = #{identityType} AND identifier = #{identifier} LIMIT 1")
    UserBinding findByIdentityTypeAndIdentifier(@Param("identityType") String identityType, @Param("identifier") String identifier);

    /**
     * 统计指定登录类型和标识的绑定记录数量
     * 用于校验是否已存在绑定关系
     *
     * @param identityType 登录类型
     * @param identifier 标识值
     * @return 绑定记录数量，0 表示无绑定
     */
    @Select("SELECT COUNT(1) FROM user_binding WHERE identity_type = #{identityType} AND identifier = #{identifier}")
    Integer countByIdentityTypeAndIdentifier(@Param("identityType") String identityType, @Param("identifier") String identifier);

    /**
     * 根据用户ID和登录类型查询绑定记录
     * 用于查询用户已绑定的第三方账号信息
     *
     * @param userId 本地用户ID
     * @param identityType 登录类型
     * @return 用户绑定记录，若不存在返回 null
     */
    @Select("SELECT * FROM user_binding WHERE user_id = #{userId} AND identity_type = #{identityType} LIMIT 1")
    UserBinding findByUserIdAndIdentityType(@Param("userId") Long userId, @Param("identityType") String identityType);

    @Delete("DELETE FROM user_binding WHERE user_id = #{userId}")
    int deleteByUserId(@Param("userId") Long userId);
}
