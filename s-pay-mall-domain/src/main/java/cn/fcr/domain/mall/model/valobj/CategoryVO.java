package cn.fcr.domain.mall.model.valobj;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 分类视图对象，承载前端展示所需数据。
 *
 * @author 傅崇睿
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CategoryVO {

    /** 分类ID */
    private Long id;

    /** 分类名称 */
    private String name;

    /** 分类状态：0=禁用，1=启用 */
    private Integer status;

    /** 创建时间 */
    @JsonProperty("create_time")
    private LocalDateTime createTime;

    /** 更新时间 */
    @JsonProperty("update_time")
    private LocalDateTime updateTime;
}