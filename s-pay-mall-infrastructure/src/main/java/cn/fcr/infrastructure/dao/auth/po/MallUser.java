package cn.fcr.infrastructure.dao.auth.po;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 用户实体类
 *
 * <p>基础用户表，存储核心登录信息
 * 第三方绑定信息（微信、支付宝、手机号）存储在 user_binding 表中
 */
@Data
@TableName("mall_user")
public class MallUser {

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("username")
    private String username;

    @TableField("password")
    private String password;

    @TableField("status")
    private Integer status;

    @JsonProperty("create_time")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @TableField("create_time")
    private LocalDateTime createTime;

    @JsonProperty("update_time")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @TableField("update_time")
    private LocalDateTime updateTime;

    /**
     * 角色编码（非数据库字段，通过 JOIN 查询注入）
     */
    @TableField(exist = false)
    private String roleCode;

    /**
     * 角色ID（非数据库字段，通过 JOIN 查询注入）
     */
    @TableField(exist = false)
    private Long roleId;
}
