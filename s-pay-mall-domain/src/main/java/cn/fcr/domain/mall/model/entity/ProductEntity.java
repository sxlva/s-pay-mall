package cn.fcr.domain.mall.model.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 商品实体，业务逻辑封装在 Entity 内部，包含销售验证、库存校验等核心能力。
 *
 * @author 傅崇睿
 */
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ProductEntity {

    /** 商品ID */
    private Long id;

    /** 分类ID */
    private Long categoryId;

    /** 商品名称 */
    private String name;

    /** 商品描述 */
    private String description;

    /** 商品价格（元） */
    private BigDecimal price;

    /** 库存数量 */
    private Integer stock;

    /** 分类名称 */
    private String category;

    /** 商品状态：0=下架，1=上架 */
    private Integer status;

    /** 创建时间 */
    private LocalDateTime createTime;

    /** 更新时间 */
    private LocalDateTime updateTime;

    /* ==========================================
     * 商品状态守卫逻辑
     * ========================================== */

    /**
     * 检查商品是否可销售（上架状态）
     * @return true=可销售，false=已下架
     */
    public boolean isAvailable() {
        return status != null && status == 1;
    }

    /**
     * 检查商品是否可删除
     * @return true=可删除，false=已上架不可删除
     */
    public boolean canDelete() {
        return status == null || status != 1;
    }

    /* ==========================================
     * 库存验证与操作逻辑
     * ========================================== */

    /**
     * 验证库存是否充足
     * @param quantity 需要的数量
     * @return true=库存充足，false=库存不足
     */
    public boolean validateStock(Integer quantity) {
        if (quantity == null || quantity <= 0) {
            return false;
        }
        return stock != null && stock >= quantity;
    }

    /**
     * 验证价格是否匹配
     * @param orderPrice 订单价格
     * @return true=价格匹配，false=价格异常
     */
    public boolean validatePrice(BigDecimal orderPrice) {
        if (orderPrice == null || price == null) {
            return false;
        }
        return price.compareTo(orderPrice) == 0;
    }

    /**
     * 检查是否可以扣减库存
     * @param quantity 需要扣减的数量
     * @return true=可以扣减，false=库存不足
     */
    public boolean canReduceStock(Integer quantity) {
        return validateStock(quantity);
    }

    /* ==========================================
     * 充血模型核心：销售验证业务逻辑封装
     * ========================================== */

    /**
     * 验证商品是否可销售（完整业务规则）
     * 包含：商品存在性、上架状态、库存充足、价格匹配
     *
     * @param quantity 购买数量
     * @param orderPrice 订单价格
     * @throws IllegalArgumentException 商品不存在
     * @throws IllegalStateException 商品已下架、库存不足或价格异常
     */
    public void validateForSale(Integer quantity, BigDecimal orderPrice) {
        // 1. 商品状态验证
        if (!isAvailable()) {
            throw new IllegalStateException("商品已下架");
        }

        // 2. 库存验证
        if (!validateStock(quantity)) {
            throw new IllegalStateException("商品库存不足");
        }

        // 3. 价格验证
        if (!validatePrice(orderPrice)) {
            throw new IllegalStateException("商品价格异常");
        }
    }

    /**
     * 验证商品是否可销售（简化版，仅验证状态和库存）
     *
     * @param quantity 购买数量
     * @throws IllegalStateException 商品已下架或库存不足
     */
    public void validateForSale(Integer quantity) {
        if (!isAvailable()) {
            throw new IllegalStateException("商品已下架");
        }
        if (!validateStock(quantity)) {
            throw new IllegalStateException("商品库存不足");
        }
    }

    /* ==========================================
     * 库存操作决策逻辑
     * ========================================== */

    /**
     * 计算扣减后的库存数量
     * 注意：此方法仅返回计算结果，不修改内部状态
     * 实际库存扣减由 Infrastructure 层的 Repository 执行
     *
     * @param quantity 扣减数量
     * @return 扣减后的库存数量，若库存不足返回 null
     */
    public Integer calculateStockAfterDeduct(Integer quantity) {
        if (!canReduceStock(quantity)) {
            return null;
        }
        return this.stock - quantity;
    }

    /**
     * 计算恢复后的库存数量
     * 注意：此方法仅返回计算结果，不修改内部状态
     *
     * @param quantity 恢复数量
     * @return 恢复后的库存数量
     */
    public Integer calculateStockAfterRestore(Integer quantity) {
        if (quantity == null || quantity <= 0) {
            return this.stock;
        }
        return this.stock + quantity;
    }
}
