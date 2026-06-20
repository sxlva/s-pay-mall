package cn.fcr.domain.mall.service.impl;

import cn.fcr.domain.mall.adapter.gateway.IDistributedLockService;
import cn.fcr.domain.mall.adapter.repository.ICartRepository;
import cn.fcr.domain.mall.adapter.repository.IProductRepository;
import cn.fcr.domain.mall.model.entity.CartEntity;
import cn.fcr.domain.mall.model.entity.CartItemEntity;
import cn.fcr.domain.mall.model.entity.ProductEntity;
import cn.fcr.domain.mall.model.valobj.CartItemVO;
import cn.fcr.domain.mall.service.IMallCartService;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class MallCartServiceImpl implements IMallCartService {

    private final ICartRepository cartRepository;
    private final IProductRepository productRepository;
    private final IDistributedLockService distributedLockService;

    public MallCartServiceImpl(ICartRepository cartRepository,
                               IProductRepository productRepository,
                               IDistributedLockService distributedLockService) {
        this.cartRepository = cartRepository;
        this.productRepository = productRepository;
        this.distributedLockService = distributedLockService;
    }

    @Override
    public int addCart(Long userId, Long productId, Integer quantity) {
        return distributedLockService.executeWithLock(productId, () -> {
            ProductEntity product = productRepository.findById(productId);
            if (product == null) {
                throw new IllegalArgumentException("商品不存在");
            }

            CartEntity cart = cartRepository.findByUserId(userId);

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
            cartRepository.saveCart(cart);
            return cart.getItems().size();
        });
    }

    @Override
    public List<CartItemVO> listCart(Long userId) {
        CartEntity cart = cartRepository.findByUserId(userId);
        List<Long> productIds = cart.getItems().stream()
                .map(CartItemEntity::getProductId)
                .distinct()
                .collect(Collectors.toList());

        Map<Long, ProductEntity> productMap = productIds.isEmpty()
                ? Map.of()
                : productRepository.findByIds(productIds).stream()
                        .collect(Collectors.toMap(ProductEntity::getId, p -> p));

        return cart.getItems().stream()
                .map(item -> toCartItemVO(item, getStock(productMap.get(item.getProductId()))))
                .collect(Collectors.toList());
    }

    private Integer getStock(ProductEntity product) {
        return product != null ? product.getStock() : 0;
    }

    @Override
    public int updateQuantity(Long userId, Long productId, Integer quantity) {
        return distributedLockService.executeWithLock(productId, () -> {
            ProductEntity product = productRepository.findById(productId);
            if (product == null) {
                throw new IllegalArgumentException("商品不存在");
            }

            if (product.getStock() == null || quantity > product.getStock()) {
                throw new IllegalArgumentException("库存不足");
            }

            CartEntity cart = cartRepository.findByUserId(userId);
            cart.updateQuantity(productId, quantity);
            cartRepository.saveCart(cart);
            return cart.getItems().size();
        });
    }

    @Override
    public int deleteCartItem(Long userId, Long cartItemId) {
        CartEntity cart = cartRepository.findByUserId(userId);
        cart.removeItem(cartItemId);
        cartRepository.saveCart(cart);
        return cart.getItems().size();
    }

    @Override
    public int clearCart(Long userId) {
        cartRepository.clearCart(userId);
        return 0;
    }

    private CartItemVO toCartItemVO(CartItemEntity entity, Integer stock) {
        return CartItemVO.builder()
                .id(entity.getId() != null ? entity.getId() : 0L)
                .productId(entity.getProductId() != null ? entity.getProductId() : 0L)
                .productName(entity.getProductName() != null ? entity.getProductName() : "未知商品")
                .productPrice(entity.getProductPrice() != null ? entity.getProductPrice() : BigDecimal.ZERO)
                .quantity(entity.getQuantity() != null ? entity.getQuantity() : 0)
                .selected(entity.getSelected() != null ? entity.getSelected() : false)
                .itemAmount(entity.calculateItemAmount() != null ? entity.calculateItemAmount() : BigDecimal.ZERO)
                .stock(stock != null ? stock : 0)
                .build();
    }
}