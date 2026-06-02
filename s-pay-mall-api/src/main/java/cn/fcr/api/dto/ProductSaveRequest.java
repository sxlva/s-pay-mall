package cn.fcr.api.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class ProductSaveRequest {

    private Long id;

    private Long categoryId;

    private String name;

    private String description;

    private BigDecimal price;

    private Integer stock;

    private Integer status;
}