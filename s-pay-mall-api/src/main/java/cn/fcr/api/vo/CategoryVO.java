package cn.fcr.api.vo;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 分类视图对象
 */
@Data
public class CategoryVO {
    private Long id;
    private String name;
    private Integer status;

    @JsonProperty("create_time")
    private LocalDateTime createTime;
}
