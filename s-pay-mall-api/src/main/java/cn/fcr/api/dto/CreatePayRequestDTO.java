package cn.fcr.api.dto;

import lombok.Data;

/**
 * 创建支付请求DTO
 * <p>
 * 用于发起支付请求的参数封装
 *
 * @author 傅崇睿
 * @date 2025/8/1 08:08
 */
@Data
public class CreatePayRequestDTO {

    /**
     * 用户ID，实际生产中通过登录模块获取，不需要透传
     */
    private String userId;

    /**
     * 商品ID
     */
    private String productId;

}
