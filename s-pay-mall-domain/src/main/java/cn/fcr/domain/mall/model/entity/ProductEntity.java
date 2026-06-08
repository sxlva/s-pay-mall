package cn.fcr.domain.mall.model.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 商品实体
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ProductEntity {

    private Long id;

    private Long categoryId;

    private String name;

    private String description;

    private BigDecimal price;

    private Integer stock;

    private String category;

    private Integer status;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

    public boolean isAvailable() {
        return status != null && status == 1;
    }

    public boolean validateStock(Integer quantity) {
        if (quantity == null || quantity <= 0) {
            return false;
        }
        return stock != null && stock >= quantity;
    }

    public boolean validatePrice(BigDecimal orderPrice) {
        if (orderPrice == null || price == null) {
            return false;
        }
        return price.compareTo(orderPrice) == 0;
    }

    public boolean canReduceStock(Integer quantity) {
        return validateStock(quantity);
    }
}
