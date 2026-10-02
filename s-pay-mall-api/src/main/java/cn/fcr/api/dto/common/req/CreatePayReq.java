package cn.fcr.api.dto.common.req;

import javax.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 创建支付请求DTO
 * <p>
 * 用于发起支付请求的参数封装
 *
 * @author 傅崇睿
 */
@Data
public class CreatePayReq {

    /**
     * 商品ID
     */
    @NotBlank(message = "商品ID不能为空")
    private String productId;

}
