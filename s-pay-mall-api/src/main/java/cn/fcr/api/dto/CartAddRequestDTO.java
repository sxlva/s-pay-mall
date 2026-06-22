package cn.fcr.api.dto;

import javax.validation.constraints.NotNull;
import lombok.Data;

/**
 * 购物车添加请求DTO
 * <p>
 * 用于将商品添加到购物车的请求参数封装
 *
 * @author 傅崇睿
 */
@Data
public class CartAddRequestDTO {

    /**
     * 商品ID
     */
    @NotNull(message = "商品ID不能为空")
    private Long productId;

    /**
     * 商品数量，默认为1
     */
    private Integer quantity = 1;
}
