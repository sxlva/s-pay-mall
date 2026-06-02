package cn.fcr.api.dto;

import lombok.Data;

@Data
public class CartAddRequest {

    private Long productId;

    private Integer quantity = 1;
}