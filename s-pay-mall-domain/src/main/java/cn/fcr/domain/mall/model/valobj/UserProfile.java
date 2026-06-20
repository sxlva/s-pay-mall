package cn.fcr.domain.mall.model.valobj;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

/**
 * 用户个人信息值对象
 * <p>
 * 【Domain 值对象】仅在领域层内部使用，不暴露给外部 API。
 * 外部表现层通过 Assembler 或 Controller 转换为 API 层的 UserProfileVO。
 */
@Getter
@Builder
@AllArgsConstructor
public class UserProfile {

    private final Long id;
    private final String username;
    private final Integer status;
    private final String roleCode;
    private final LocalDateTime createTime;
    private final LocalDateTime updateTime;
}
