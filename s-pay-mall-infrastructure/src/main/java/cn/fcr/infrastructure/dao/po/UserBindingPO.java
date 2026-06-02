package cn.fcr.infrastructure.dao.po;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 用户第三方联合绑定表实体类
 * 
 * <p>用于存储用户与第三方平台（微信公众号、支付宝、手机号等）的绑定关系
 * 采用一对多设计，为后续扩展多种登录方式留出空间
 */
@Data
@TableName("user_binding")
public class UserBindingPO {
    
    @TableId(type = IdType.AUTO)
    private Long id;
    
    @TableField("user_id")
    private Long userId;
    
    @TableField("identity_type")
    private String identityType;
    
    @TableField("identifier")
    private String identifier;
    
    @TableField("credential")
    private String credential;
    
    @TableField("create_time")
    private LocalDateTime createTime;
}