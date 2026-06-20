package cn.fcr.api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * 购物车商品响应 DTO
 * 替代原 Controller 中手动构造的 Map<String, Object>
 *
 * 字段名使用 camelCase，与前端 CartItemRaw / CartItem 类型对齐：
 * - price 对应 domain 的 productPrice（通过 MapStruct @Mapping 处理）
 * - 其余字段名与 domain CartItemVO 一致
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CartItemRespDTO {

    private Long id;
    private Long productId;
    private String productName;
    private BigDecimal price;
    private Integer quantity;
    private Boolean selected;
    private BigDecimal itemAmount;
    private Integer stock;
}
