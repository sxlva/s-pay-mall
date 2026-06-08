package cn.fcr.domain.mall.adapter.repository;

import cn.fcr.domain.mall.model.entity.CartEntity;

public interface ICartRepository {

    CartEntity findByUserId(Long userId);

    void saveCart(CartEntity cart);

    void clearCart(Long userId);
}