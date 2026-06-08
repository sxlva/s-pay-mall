package cn.fcr.domain.mall.model.entity;

import cn.fcr.types.common.Constants;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 用户实体
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class UserEntity {

    private Long id;

    private String username;

    private String password;

    private Integer status;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

    private String roleCode;

    private Long roleId;

    /**
     * 验证账户是否可登录
     */
    public void validateLoginStatus() {
        if (this.status != null && this.status.equals(Constants.USER_STATUS_DISABLED)) {
            throw new IllegalStateException("您的账号已被封禁，请联系管理员处理！");
        }
    }

    /**
     * 验证密码是否正确
     * @param rawPassword 明文密码
     * @param passwordMatcher 密码匹配器
     * @return 是否匹配
     */
    public boolean validatePassword(String rawPassword, PasswordMatcher passwordMatcher) {
        return passwordMatcher.matches(rawPassword, this.password);
    }

    /**
     * 获取角色编码，若为空则返回默认成员角色
     */
    public String getRoleOrDefault() {
        if (this.roleCode == null || this.roleCode.isBlank()) {
            return Constants.DEFAULT_ROLE_MEMBER;
        }
        return this.roleCode;
    }

    /**
     * 密码匹配器接口
     */
    @FunctionalInterface
    public interface PasswordMatcher {
        boolean matches(String rawPassword, String encodedPassword);
    }

    /**
     * 密码编码器接口
     */
    @FunctionalInterface
    public interface PasswordEncoder {
        String encode(String rawPassword);
    }
}
