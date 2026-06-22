package cn.fcr.infrastructure.dao.auth.po;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * 用户角色关系实体类（user_role表）
 *
 * @author 傅崇睿
 */
@Data
@TableName("user_role")
public class UserRole {

    /** 用户ID */
    @TableId(value = "user_id", type = IdType.INPUT)
    private Long userId;

    /** 角色ID */
    @TableField("role_id")
    private Long roleId;
}
