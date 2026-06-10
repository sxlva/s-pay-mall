package cn.fcr.api.dto;

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
    private String name;

    /**
     * 分类状态：0-禁用，1-启用
     */
    private Integer status;
}