package cn.fcr.api.dto.user.req;

import javax.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 订单创建请求DTO
 * <p>
 * 用于创建订单的请求参数封装
 *
 * @author 傅崇睿
 */
@Data
public class UserOrderCreateReq {

    /**
     * 收货地址
     */
    @NotBlank(message = "收货地址不能为空")
    private String address;

    /**
     * 幂等键（P0-4）：客户端生成的 UUID，同一值 24 小时内重复提交返回首次创建的订单；
     * 为空时降级为用户级短锁（10 秒），仅防双击/并发重发
     */
    private String requestId;
}