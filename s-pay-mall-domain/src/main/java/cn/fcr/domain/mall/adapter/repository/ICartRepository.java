package cn.fcr.domain.mall.adapter.repository;

import cn.fcr.domain.mall.model.entity.CartEntity;

/**
 * 购物车仓储接口，定义购物车持久化操作的抽象。
 *
 * @author 傅崇睿
 */
public interface ICartRepository {

    /**
     * 根据用户ID查询购物车
     *
     * @param userId 用户ID
     * @return 购物车实体
     */
    CartEntity findByUserId(Long userId);

    /**
     * 保存购物车
     *
     * @param cart 购物车实体
     */
    void saveCart(CartEntity cart);

    /**
     * 清空用户购物车
     *
     * @param userId 用户ID
     */
    void clearCart(Long userId);
}