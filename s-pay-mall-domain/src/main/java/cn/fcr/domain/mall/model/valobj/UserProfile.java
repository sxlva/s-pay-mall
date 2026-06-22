package cn.fcr.domain.mall.model.valobj;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

/**
 * 用户个人信息值对象，仅在领域层内部使用，外部 API 通过 Assembler 转换。
 *
 * @author 傅崇睿
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
