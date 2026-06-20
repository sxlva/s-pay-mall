package cn.fcr.infrastructure.mall.repository;

import cn.fcr.domain.mall.adapter.repository.ICartRepository;
import cn.fcr.domain.mall.model.entity.CartEntity;
import cn.fcr.domain.mall.model.entity.CartItemEntity;
import cn.fcr.infrastructure.dao.ICartItemDao;
import cn.fcr.infrastructure.dao.IProductDao;
import cn.fcr.infrastructure.dao.po.CartItem;
import cn.fcr.infrastructure.dao.po.Product;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.stereotype.Repository;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Repository
public class CartRepository implements ICartRepository {

    @Resource
    private ICartItemDao cartItemDao;

    @Resource
    private IProductDao productDao;

    @Override
    public CartEntity findByUserId(Long userId) {
        LambdaQueryWrapper<CartItem> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(CartItem::getUserId, userId)
                   .orderByDesc(CartItem::getId);

        List<CartItem> cartItems = cartItemDao.selectList(queryWrapper);

        List<CartItemEntity> itemEntities = cartItems.stream()
                .map(this::toCartItemEntity)
                .collect(Collectors.toList());

        return CartEntity.builder()
                .userId(userId)
                .items(itemEntities)
                .build();
    }

    @Override
    public void saveCart(CartEntity cart) {
        if (cart == null || cart.getItems() == null) return;

        LambdaQueryWrapper<CartItem> deleteWrapper = new LambdaQueryWrapper<>();
        deleteWrapper.eq(CartItem::getUserId, cart.getUserId());
        cartItemDao.delete(deleteWrapper);

        for (CartItemEntity item : cart.getItems()) {
            cartItemDao.insert(toCartItemPO(item));
        }
    }

    @Override
    public void clearCart(Long userId) {
        LambdaQueryWrapper<CartItem> deleteWrapper = new LambdaQueryWrapper<>();
        deleteWrapper.eq(CartItem::getUserId, userId);
        cartItemDao.delete(deleteWrapper);
    }

    private CartItemEntity toCartItemEntity(CartItem po) {
        Product product = productDao.selectById(po.getProductId());
        return CartItemEntity.builder()
                .id(po.getId())
                .userId(po.getUserId())
                .productId(po.getProductId())
                .productName(product != null ? product.getName() : null)
                .productPrice(product != null ? product.getPrice() : null)
                .quantity(po.getQuantity())
                .selected(true)
                .createTime(po.getCreateTime())
                .updateTime(po.getUpdateTime())
                .build();
    }

    private CartItem toCartItemPO(CartItemEntity entity) {
        CartItem po = new CartItem();
        po.setUserId(entity.getUserId());
        po.setProductId(entity.getProductId());
        po.setQuantity(entity.getQuantity());
        po.setCreateTime(entity.getCreateTime());
        po.setUpdateTime(entity.getUpdateTime());
        return po;
    }
}
