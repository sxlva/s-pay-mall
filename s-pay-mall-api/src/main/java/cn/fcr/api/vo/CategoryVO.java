package cn.fcr.api.vo;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 分类视图对象
 *
 * @author 傅崇睿
 */
@Data
public class CategoryVO {

    /** 分类ID */
    private Long id;

    /** 分类名称 */
    private String name;

    /** 分类状态：0-禁用，1-启用 */
    private Integer status;

    /** 创建时间 */
    @JsonProperty("create_time")
    private LocalDateTime createTime;
}
