package cn.fcr.domain.mall.model.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 分类实体
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class CategoryEntity {

    private Long id;

    private String name;

    private Integer status;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
