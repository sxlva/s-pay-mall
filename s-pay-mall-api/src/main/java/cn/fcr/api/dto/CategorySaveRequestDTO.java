package cn.fcr.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 分类保存请求DTO
 * <p>
 * 用于创建或更新商品分类的请求参数封装
 */
@Data
public class CategorySaveRequestDTO {

    /**
     * 分类ID，创建时为null，更新时必填
     */
    private Long id;

    /**
     * 分类名称
     */
    @NotBlank(message = "分类名称不能为空")
    private String name;

    /**
     * 分类状态：0-禁用，1-启用
     */
    @NotNull(message = "分类状态不能为空")
    private Integer status;
}