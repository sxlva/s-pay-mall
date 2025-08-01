package cn.bugstack.api.dto;

import lombok.Data;

/**
 * @author xiaolv
 * @date 2025/8/1 08:08
 * @description
 */
@Data
public class CreatePayRequestDTO {

    // 用户ID 【实际产生中会通过登录模块获取，不需要透彻】
    private String userId;
    // 产品编号
    private String productId;

}
