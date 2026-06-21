package cn.fcr.api.dto;

import javax.validation.constraints.NotBlank;
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
    @NotBlank(message = "收货地址不能为空")
    private String address;
}