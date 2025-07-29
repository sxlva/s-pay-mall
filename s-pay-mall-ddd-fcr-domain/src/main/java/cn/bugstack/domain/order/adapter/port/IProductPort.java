package cn.bugstack.domain.order.adapter.port;

import cn.bugstack.domain.order.model.entity.ProductEntity;

/**
 * @author xiaolv
 * @date 2025/7/28 19:31
 * @description
 */
public interface IProductPort {

    public ProductEntity queryProductByProductId(String productId);

}
