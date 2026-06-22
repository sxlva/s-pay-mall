package cn.fcr.domain.order.model.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * 购物车聚合根，充血模型实体
 *
 * <p>业务逻辑封装在 Entity 内部，购物车状态由内部维护，外部只能通过行为方法修改。
 * 暴露给外部的 List 使用不可变视图，防止破坏聚合根一致性。
 * 此购物车用于订单创建流程，保存用户下单时的商品快照。</p>
 *
 * @author 傅崇睿
 */
@Getter // 【重构】仅允许外部读取，防止状态被外部随意篡改
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ShopCartEntity {

    /**
     * 用户ID
     */
    private String userId;

    /**
     * 【兼容性字段】单个商品ID（从 items[0] 派生）
     * 保留此字段以兼容旧代码，未来可移除
     */
    @Builder.Default
    private String productId = null;

    /**
     * 购物车中的商品项
     * 【防御性编程】使用 unmodifiableList 暴露，防止外部直接修改集合
     */
    @Builder.Default
    private List<CartItem> items = new ArrayList<>();

    /**
     * 获取购物车商品列表（不可变视图）
     * 【DDD 聚合根保护】外部只能读取，不能直接修改集合
     *
     * @return 不可变的商品列表视图
     */
    public List<CartItem> getItems() {
        return Collections.unmodifiableList(items);
    }

    /**
     * 【兼容性方法】获取单个商品ID
     * 优先返回 productId 字段，若为空则从 items[0] 获取
     * 【防御性编程】若无商品则返回 null
     *
     * @return 商品ID或null
     */
    public String getProductId() {
        if (productId != null && !productId.trim().isEmpty()) {
            return productId;
        }
        if (items != null && !items.isEmpty()) {
            return items.get(0).getProductId();
        }
        return null;
    }

    /* ==========================================
     * 行为方法：添加或修改商品
     * ========================================== */

    /**
     * 添加或更新商品到购物车
     * 【防御性编程】
     * - 校验数量必须 > 0
     * - 如果数量为 0，则移除该商品
     * - 如果商品已存在，则累加数量
     * - 如果商品不存在，则添加新商品
     *
     * @param product  商品信息（快照）
     * @param quantity 数量
     * @throws IllegalArgumentException 数量为 null 或 < 0
     */
    public void addOrUpdateItem(ProductEntity product, Integer quantity) {
        // 【守卫校验】数量不能为 null
        if (quantity == null) {
            throw new IllegalArgumentException("商品数量不能为空");
        }

        // 【业务逻辑】数量为 0 则移除商品
        if (quantity <= 0) {
            removeItem(product.getProductId());
            return;
        }

        // 【防御性编程】校验商品信息
        validateProduct(product);

        // 查找是否已存在该商品
        Optional<CartItem> existingItem = findItemByProductId(product.getProductId());

        if (existingItem.isPresent()) {
            // 【业务逻辑】商品已存在，累加数量
            CartItem item = existingItem.get();
            item.increaseQuantity(quantity);
        } else {
            // 【业务逻辑】商品不存在，创建新项
            CartItem newItem = CartItem.builder()
                    .productId(product.getProductId())
                    .productName(product.getProductName())
                    .price(product.getPrice())
                    .quantity(quantity)
                    .build();
            items.add(newItem);
        }
    }

    /* ==========================================
     * 行为方法：移除商品
     * ========================================== */

    /**
     * 从购物车移除商品
     * 【防御性编程】
     * - 如果商品不存在，抛出异常
     * - 使用迭代器安全删除，避免 ConcurrentModificationException
     *
     * @param productId 商品ID
     * @throws IllegalArgumentException 商品不存在于购物车中
     */
    public void removeItem(String productId) {
        if (productId == null || productId.trim().isEmpty()) {
            throw new IllegalArgumentException("商品ID不能为空");
        }

        boolean removed = items.removeIf(item -> productId.equals(item.getProductId()));

        if (!removed) {
            throw new IllegalArgumentException("商品不存在于购物车中: " + productId);
        }
    }

    /* ==========================================
     * 行为方法：计算金额和数量
     * ========================================== */

    /**
     * 计算购物车总金额
     * 【防御性编程】
     * - 使用 BigDecimal 确保精度
     * - 使用 compareTo 而非 equals
     *
     * @return 总金额
     */
    public BigDecimal calculateTotalAmount() {
        return items.stream()
                .map(item -> item.calculateAmount())
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /**
     * 计算购物车商品总数量
     *
     * @return 总数量
     */
    public int calculateTotalCount() {
        return items.stream()
                .mapToInt(CartItem::getQuantity)
                .sum();
    }

    /**
     * 清空购物车
     */
    public void clear() {
        items.clear();
    }

    /**
     * 检查购物车是否为空
     *
     * @return true=空，false=非空
     */
    public boolean isEmpty() {
        return items.isEmpty();
    }

    /**
     * 获取商品项数量
     *
     * @return 商品种类数
     */
    public int getItemCount() {
        return items.size();
    }

    /* ==========================================
     * 内部辅助方法
     * ========================================== */

    /**
     * 校验商品信息
     * @throws IllegalArgumentException 商品信息不完整
     */
    private void validateProduct(ProductEntity product) {
        if (product == null) {
            throw new IllegalArgumentException("商品信息不能为空");
        }
        if (product.getProductId() == null || product.getProductId().trim().isEmpty()) {
            throw new IllegalArgumentException("商品ID不能为空");
        }
        if (product.getPrice() == null) {
            throw new IllegalArgumentException("商品价格不能为空");
        }
        if (product.getPrice().compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("商品价格不能为负数");
        }
    }

    /**
     * 根据商品ID查找购物车项
     *
     * @param productId 商品ID
     * @return 购物车项（如果存在）
     */
    private Optional<CartItem> findItemByProductId(String productId) {
        return items.stream()
                .filter(item -> productId.equals(item.getProductId()))
                .findFirst();
    }

    /* ==========================================
     * 内部类：购物车商品项
     * 【设计说明】作为 ShopCartEntity 的组合对象，非独立实体
     * ========================================== */

    /**
     * 【DDD 组合对象】购物车商品项
     * 负责管理单个商品的快照信息
     */
    @Getter // 购物车项也使用 @Getter，仅允许读取
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class CartItem {

        /**
         * 商品ID
         */
        private String productId;

        /**
         * 商品名称（快照）
         */
        private String productName;

        /**
         * 商品单价（快照）
         */
        private BigDecimal price;

        /**
         * 购买数量
         */
        @Builder.Default
        private Integer quantity = 0;

        /**
         * 增加商品数量
         * 【守卫校验】数量不能超过 999
         *
         * @param delta 增加的数量
         * @throws IllegalArgumentException 数量超过上限
         */
        public void increaseQuantity(Integer delta) {
            if (delta == null || delta <= 0) {
                throw new IllegalArgumentException("增加数量必须大于 0");
            }
            long newQuantity = (long) this.quantity + delta;
            if (newQuantity > 999) {
                throw new IllegalArgumentException("购物车单个商品数量不能超过 999");
            }
            this.quantity = (int) newQuantity;
        }

        /**
         * 计算该项的总金额
         * 【防御性编程】使用 BigDecimal 确保精度
         *
         * @return 商品总价
         */
        public BigDecimal calculateAmount() {
            if (price == null || quantity == null) {
                return BigDecimal.ZERO;
            }
            return price.multiply(BigDecimal.valueOf(quantity));
        }
    }
}
