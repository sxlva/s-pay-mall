package cn.fcr.infrastructure.dao.po;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * 用户角色关系实体类
 */
@Data
@TableName("user_role")
public class UserRole {

    @TableId(value = "user_id", type = IdType.INPUT)
    private Long userId;

    @TableField("role_id")
    private Long roleId;
}
