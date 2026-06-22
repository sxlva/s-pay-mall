package cn.fcr.api.vo;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/**
 * 微信绑定状态视图对象
 *
 * @author 傅崇睿
 */
@Data
public class BindStatusVO {

    /** 绑定状态 */
    private String status;

    /** 微信 OpenID */
    @JsonProperty("open_id")
    private String openId;
}
