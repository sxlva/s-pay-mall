package cn.fcr.domain.mall.model.entity;

import cn.fcr.domain.mall.model.valobj.ProductVO;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

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
    private Integer status;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    private String category;
    private String categoryName;

    public static ProductEntity fromVO(ProductVO vo) {
        if (vo == null) return null;
        return ProductEntity.builder()
                .id(vo.getId())
                .categoryId(vo.getCategoryId())
                .name(vo.getName())
                .description(vo.getDescription())
                .price(vo.getPrice())
                .stock(vo.getStock())
                .status(vo.getStatus())
                .createTime(vo.getCreateTime())
                .updateTime(vo.getUpdateTime())
                .category(vo.getCategory())
                .categoryName(vo.getCategoryName())
                .build();
    }

    public ProductVO toVO() {
        return ProductVO.builder()
                .id(this.id)
                .categoryId(this.categoryId)
                .name(this.name)
                .description(this.description)
                .price(this.price)
                .stock(this.stock)
                .status(this.status)
                .createTime(this.createTime)
                .updateTime(this.updateTime)
                .category(this.category)
                .categoryName(this.categoryName)
                .build();
    }

    public boolean isAvailable() {
        return this.status != null && this.status == 1;
    }

    public boolean validateStock(int quantity) {
        return this.stock != null && this.stock >= quantity;
    }

    public boolean validatePrice(BigDecimal orderPrice) {
        if (this.price == null || orderPrice == null) return false;
        return this.price.compareTo(orderPrice) >= 0;
    }

    public boolean canReduceStock(int quantity) {
        return isAvailable() && validateStock(quantity);
    }

    public void reduceStock(int quantity) {
        if (!canReduceStock(quantity)) {
            throw new IllegalStateException("商品库存不足或商品已下架");
        }
        this.stock = this.stock - quantity;
    }

    public void restoreStock(int quantity) {
        if (this.stock == null) {
            this.stock = quantity;
        } else {
            this.stock = this.stock + quantity;
        }
    }
}