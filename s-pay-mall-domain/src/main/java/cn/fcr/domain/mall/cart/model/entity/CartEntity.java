package cn.fcr.domain.mall.cart.model.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Optional;

/**
 * 购物车实体，负责购物车商品项管理和金额计算。
 *
 * @author 傅崇睿
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class CartEntity {

    /** 用户ID */
    private Long userId;

    /** 购物车商品项列表 */
    private List<CartItemEntity> items;

    public void addItem(CartItemEntity newItem) {
        if (newItem == null) {
            throw new IllegalArgumentException("购物车项不能为空");
        }

        Optional<CartItemEntity> existingItemOpt = findItemByProductId(newItem.getProductId());
        if (existingItemOpt.isPresent()) {
            CartItemEntity existingItem = existingItemOpt.get();
            existingItem.addQuantity(newItem.getQuantity());
        } else {
            validateItemLimit();
            newItem.setSelected(true);
            this.items.add(newItem);
        }
    }

    public void updateQuantity(Long productId, Integer quantity) {
        CartItemEntity item = findItemByProductId(productId)
                .orElseThrow(() -> new IllegalArgumentException("购物车中未找到该商品"));

        int delta = quantity - item.getQuantity();
        item.addQuantity(delta);
    }

    public void removeItem(Long cartItemId) {
        if (items == null) return;
        items.removeIf(item -> cartItemId.equals(item.getId()));
    }

    private Optional<CartItemEntity> findItemByProductId(Long productId) {
        if (items == null || productId == null) return Optional.empty();
        return items.stream()
                .filter(item -> productId.equals(item.getProductId()))
                .findFirst();
    }

    private Optional<CartItemEntity> findItemById(Long cartItemId) {
        if (items == null || cartItemId == null) return Optional.empty();
        return items.stream()
                .filter(item -> cartItemId.equals(item.getId()))
                .findFirst();
    }

    private void validateItemLimit() {
        if (items != null && items.size() >= 99) {
            throw new IllegalStateException("购物车最多只能添加99件商品");
        }
    }

}
