package cn.fcr.domain.mall.service;

import cn.fcr.domain.mall.model.valobj.CartItemVO;

import java.util.List;

/**
 * 购物车领域服务接口
 */
public interface IMallCartService {
    
    int addCart(Long userId, Long productId, Integer quantity);
    
    List<CartItemVO> listCart(Long userId);
    
    int updateQuantity(Long userId, Long productId, Integer quantity);
    
    int deleteCartItem(Long userId, Long cartItemId);
    
    int clearCart(Long userId);
}
