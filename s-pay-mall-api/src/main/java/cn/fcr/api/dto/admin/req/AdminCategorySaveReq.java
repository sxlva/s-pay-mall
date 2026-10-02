package cn.fcr.api.dto.admin.req;

import javax.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 分类保存请求DTO
 * <p>
 * 用于创建或更新商品分类的请求参数封装
 *
 * @author 傅崇睿
 */
@Data
public class AdminCategorySaveReq {

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
     * 分类状态：0-禁用，1-启用；可选，不传时由服务层按默认启用处理
     * （与管理后台分类表单字段保持一致，该表单仅包含名称）
     */
    private Integer status;
}