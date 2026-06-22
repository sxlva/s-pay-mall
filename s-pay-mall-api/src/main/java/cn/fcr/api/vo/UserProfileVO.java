package cn.fcr.api.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 用户个人信息视图对象
 * <p>
 * 字段与 Domain 层 UserProfile 一一对应，由 Assembler 负责转换。
 *
 * @author 傅崇睿
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserProfileVO {

    /** 用户ID */
    private Long id;

    /** 用户名 */
    private String username;

    /** 用户状态：0-禁用，1-启用 */
    private Integer status;

    /** 角色编码 */
    private String roleCode;

    /** 创建时间 */
    private LocalDateTime createTime;

    /** 更新时间 */
    private LocalDateTime updateTime;
}
