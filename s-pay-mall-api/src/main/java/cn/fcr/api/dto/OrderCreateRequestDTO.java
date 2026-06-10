package cn.fcr.api.dto;

import lombok.Data;

/**
 * 订单创建请求DTO
 * <p>
 * 用于创建订单的请求参数封装
 */
@Data
public class OrderCreateRequestDTO {

    /**
     * 收货地址
     */
    private String address;
}