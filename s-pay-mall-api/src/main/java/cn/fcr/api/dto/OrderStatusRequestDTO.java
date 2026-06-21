package cn.fcr.api.dto;

import javax.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 订单状态请求DTO
 * <p>
 * 用于更新订单状态的请求参数封装
 */
@Data
public class OrderStatusRequestDTO {

    /**
     * 订单状态：CREATED-已创建，PAID-已支付，CANCELLED-已取消
     */
    @NotBlank(message = "订单状态不能为空")
    private String status;
}