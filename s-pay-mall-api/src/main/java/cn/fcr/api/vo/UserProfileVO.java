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
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserProfileVO {
    private Long id;
    private String username;
    private Integer status;
    private String roleCode;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
