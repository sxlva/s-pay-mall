package cn.fcr.api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * 购物车商品响应 DTO
 * 替代原 Controller 中手动构造的 Map<String, Object>*
 * 字段名使用 camelCase，与前端 CartItemRaw / CartItem 类型对齐：
 * - price 对应 domain 的 productPrice（通过 MapStruct @Mapping 处理）
 * - 其余字段名与 domain CartItemVO 一致
 *
 * @author 傅崇睿
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CartItemRespDTO {

    /** 购物车项ID */
    private Long id;

    /** 商品ID */
    private Long productId;

    /** 商品名称 */
    private String productName;

    /** 商品价格（对应 domain 的 productPrice） */
    private BigDecimal price;

    /** 商品数量 */
    private Integer quantity;

    /** 是否选中 */
    private Boolean selected;

    /** 单项总金额 */
    private BigDecimal itemAmount;

    /** 商品库存 */
    private Integer stock;
}
