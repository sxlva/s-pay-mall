package cn.fcr.infrastructure.adapter.service;

import cn.fcr.domain.mall.model.entity.CartEntity;
import cn.fcr.domain.mall.model.entity.CartItemEntity;
import cn.fcr.domain.mall.model.valobj.CartItemVO;
import cn.fcr.domain.mall.service.IMallCartService;
import cn.fcr.infrastructure.dao.ICartItemDao;
import cn.fcr.infrastructure.dao.IProductDao;
import cn.fcr.infrastructure.dao.po.CartItem;
import cn.fcr.infrastructure.dao.po.Product;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class MallCartServiceImpl implements IMallCartService {

    @Resource
    private ICartItemDao cartItemDao;

    @Resource
    private IProductDao productDao;

    @Override
    public int addCart(Long userId, Long productId, Integer quantity) {
        Product product = productDao.selectById(productId);
        if (product == null) {
            throw new IllegalArgumentException("商品不存在");
        }

        CartEntity cart = loadCartFromDB(userId);

        CartItemEntity newItem = CartItemEntity.builder()
                .userId(userId)
                .productId(productId)
                .productName(product.getName())
                .productPrice(product.getPrice())
                .quantity(quantity)
                .createTime(LocalDateTime.now())
                .updateTime(LocalDateTime.now())
                .build();

        cart.addItem(newItem);

        return saveCartToDB(cart);
    }

    @Override
    public List<CartItemVO> listCart(Long userId) {
        CartEntity cart = loadCartFromDB(userId);

        return cart.getItems().stream()
                .map(this::toCartItemVO)
                .collect(Collectors.toList());
    }

    @Override
    public int updateQuantity(Long userId, Long productId, Integer quantity) {
        CartEntity cart = loadCartFromDB(userId);
        cart.updateQuantity(productId, quantity);
        return saveCartToDB(cart);
    }

    @Override
    public int deleteCartItem(Long userId, Long cartItemId) {
        CartEntity cart = loadCartFromDB(userId);
        cart.removeItem(cartItemId);
        return saveCartToDB(cart);
    }

    @Override
    public int clearCart(Long userId) {
        CartEntity cart = loadCartFromDB(userId);
        cart.clear();
        return saveCartToDB(cart);
    }

    private CartEntity loadCartFromDB(Long userId) {
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

    private int saveCartToDB(CartEntity cart) {
        if (cart == null || cart.getItems() == null) return 0;

        LambdaQueryWrapper<CartItem> deleteWrapper = new LambdaQueryWrapper<>();
        deleteWrapper.eq(CartItem::getUserId, cart.getUserId());
        cartItemDao.delete(deleteWrapper);

        int count = 0;
        for (CartItemEntity item : cart.getItems()) {
            CartItem cartItem = toCartItemPO(item);
            count += cartItemDao.insert(cartItem);
        }
        return count;
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

    private CartItemVO toCartItemVO(CartItemEntity entity) {
        // 防御性处理：确保所有字段都有安全默认值
        // 避免前端解析 undefined 时崩溃
        return CartItemVO.builder()
                .id(entity.getId() != null ? entity.getId() : 0L)
                .productId(entity.getProductId() != null ? entity.getProductId() : 0L)
                .productName(entity.getProductName() != null ? entity.getProductName() : "未知商品")
                .productPrice(entity.getProductPrice() != null ? entity.getProductPrice() : BigDecimal.ZERO)
                .quantity(entity.getQuantity() != null ? entity.getQuantity() : 0)
                .selected(entity.getSelected() != null ? entity.getSelected() : false)
                .itemAmount(entity.calculateItemAmount() != null ? entity.calculateItemAmount() : BigDecimal.ZERO)
                .build();
    }
}