package cn.fcr.api.vo;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/**
 * 用户个人信息视图对象
 */
@Data
public class UserProfileVO {
    private Long id;
    private String username;
    private Integer status;

    @JsonProperty("role_code")
    private String roleCode;

    @JsonProperty("order_count")
    private Integer orderCount;

    @JsonProperty("cart_count")
    private Integer cartCount;
}
