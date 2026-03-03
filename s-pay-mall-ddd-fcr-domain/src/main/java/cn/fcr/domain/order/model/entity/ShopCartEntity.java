package cn.fcr.domain.order.model.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author xiaolv
 * @date 2025/7/21 07:09
 * @description 用户下单，买一个商品
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ShopCartEntity {

    private String userId;
    private String productId;

}
