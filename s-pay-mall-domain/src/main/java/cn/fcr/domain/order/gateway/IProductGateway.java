package cn.fcr.domain.order.gateway;

import cn.fcr.domain.order.model.entity.ProductEntity;

/**
 * @author 傅崇睿
 * @description 产品网关接口
 */
public interface IProductGateway {

    /**
     * 根据产品ID查询产品信息
     *
     * @param productId 产品ID
     * @return 产品实体
     */
    ProductEntity queryProductByProductId(String productId);

    /**
     * 恢复商品库存
     *
     * @param productId 产品ID
     * @param quantity  数量
     */
    void restoreStock(String productId, Integer quantity);
}