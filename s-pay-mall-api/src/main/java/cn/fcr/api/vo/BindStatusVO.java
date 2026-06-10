package cn.fcr.api.vo;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/**
 * 微信绑定状态视图对象
 */
@Data
public class BindStatusVO {
    private String status;

    @JsonProperty("open_id")
    private String openId;
}
