package cn.fcr.api.dto;

import lombok.Data;

/**
 * 购物车添加请求DTO
 * <p>
 * 用于将商品添加到购物车的请求参数封装
 */
@Data
public class CartAddRequestDTO {

    /**
     * 商品ID
     */
    private Long productId;

    /**
     * 商品数量，默认为1
     */
    private Integer quantity = 1;
}
