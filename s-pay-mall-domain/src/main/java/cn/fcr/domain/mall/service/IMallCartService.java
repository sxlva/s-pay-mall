package cn.fcr.domain.mall.service;

import cn.fcr.domain.mall.model.valobj.CartItemVO;

import java.util.List;

/**
 * 购物车领域服务接口，定义购物车增删改查的抽象。
 *
 * @author 傅崇睿
 */
public interface IMallCartService {

    /**
     * 添加商品到购物车
     *
     * @param userId    用户ID
     * @param productId 商品ID
     * @param quantity  数量
     * @return 购物车商品项数量
     */
    int addCart(Long userId, Long productId, Integer quantity);

    /**
     * 查询购物车列表
     *
     * @param userId 用户ID
     * @return 购物车商品项列表
     */
    List<CartItemVO> listCart(Long userId);

    /**
     * 更新购物车商品数量
     *
     * @param userId    用户ID
     * @param productId 商品ID
     * @param quantity  新数量
     * @return 购物车商品项数量
     */
    int updateQuantity(Long userId, Long productId, Integer quantity);

    /**
     * 删除购物车商品项
     *
     * @param userId     用户ID
     * @param cartItemId 购物车项ID
     * @return 购物车商品项数量
     */
    int deleteCartItem(Long userId, Long cartItemId);

    /**
     * 清空购物车
     *
     * @param userId 用户ID
     * @return 0
     */
    int clearCart(Long userId);
}
